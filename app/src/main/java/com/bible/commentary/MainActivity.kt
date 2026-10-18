package com.bible.commentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val tv = TextView(this)
        tv.text = "Loading..."
        tv.textSize = 16f
        tv.setPadding(32, 32, 32, 32)
        setContentView(tv)

        val dbFile = File(getDatabasePath("commentary.db").path)
        if (dbFile.exists()) {
            tv.text = "Cleaning DB... please wait"
            // run in background to not freeze UI
            Thread {
                val cleaned = cleanDatabaseOnFirstInstall(dbFile)
                runOnUiThread {
                    tv.text = if (cleaned) "DB Cleaned OK - 1189 chapters\n\nGenesis 1 – is now clean\nNo more â€™ or Â" else "DB already cleaned - ready"
                }
            }.start()
        } else {
            tv.text = "DB not found - need download\nPlace commentary.db in /data/data/com.bible.commentary/databases/"
        }
    }

    private fun cleanDatabaseOnFirstInstall(file: File): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("db_cleaned_v4", false)) {
            Log.d("DB_CLEAN", "Already cleaned, skipping")
            return false
        }
        if (!file.exists()) return false

        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            cursor = db.rawQuery("SELECT rowid, commentary FROM enduring_word", null)
            db.beginTransaction()
            var count = 0
            while (cursor.moveToNext()) {
                val rowId = cursor.getLong(0)
                var t = cursor.getString(1) ?: continue

                // 1. mojibake fix - same as pydroid
                var prev: String
                var loop = 0
                do {
                    prev = t
                    try {
                        val b = t.toByteArray(charset("ISO-8859-1"))
                        val d = String(b, charset("UTF-8"))
                        if (d.length <= t.length + 20) t = d
                    } catch (e: Exception) { break }
                    loop++
                } while (t != prev && loop < 5)

                // 2. direct unicode safe replaces
                t = t.replace("\u00E2\u20AC\u2013", "\u2013")
                t = t.replace("\u00E2\u20AC\u2014", "\u2014")
                t = t.replace("\u00E2\u20AC\u2122", "'")
                t = t.replace("\u00E2\u20AC\u0153", "\"")
                t = t.replace("\u00E2\u20AC\u009D", "\"")
                t = t.replace("\u00E2\u20AC\u00A6", "...")
                t = t.replace("\u00C2", " ")
                t = t.replace("\u00E2\u20AC\u201C", "-")

                // 3. html entities
                t = t.replace("&#8217;", "'")
                t = t.replace("&#8211;", "\u2013")
                t = t.replace("&#8212;", "\u2014")
                t = t.replace("&#8220;", "\"")
                t = t.replace("&#8221;", "\"")
                t = t.replace("&#038;", "&")
                t = t.replace("&amp;", "&")
                t = t.replace("&#8216;", "'")

                // 4. remove unwanted blocks
                t = t.replace(Regex("(?is)<h3[^>]*>.*?Videos for.*?</h3>[\\s\\S]*?<p><a[\\s\\S]*?</p>"), "")
                t = t.replace(Regex("(?is)High Points[\\s\\S]*"), "")

                db.execSQL("UPDATE enduring_word SET commentary=? WHERE rowid=?", arrayOf(t, rowId))
                count++
            }
            cursor.close()
            db.setTransactionSuccessful()
            db.endTransaction()
            prefs.edit().putBoolean("db_cleaned_v4", true).apply()
            Log.d("DB_CLEAN", "Cleaned $count chapters OK")
            return true
        } catch (e: Exception) {
            Log.e("DB_CLEAN", "fail", e)
            return false
        } finally {
            try { cursor?.close() } catch (e: Exception) {}
            try { db?.endTransaction() } catch (e: Exception) {}
            try { db?.close() } catch (e: Exception) {}
        }
    }
}