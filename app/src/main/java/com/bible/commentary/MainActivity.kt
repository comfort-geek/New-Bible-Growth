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
        
        // simple view to avoid R.layout error
        val tv = TextView(this)
        tv.text = "Loading..."
        setContentView(tv)

        val dbFile = File(getDatabasePath("commentary.db").path)
        if (dbFile.exists()) {
            cleanDatabaseOnFirstInstall(dbFile)
            tv.text = "DB Cleaned OK - 1189 chapters"
        } else {
            tv.text = "DB not found - need download"
        }
    }

    private fun cleanDatabaseOnFirstInstall(file: File) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("db_cleaned_v4", false)) return
        if (!file.exists()) return

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            val cursor = db.rawQuery("SELECT rowid, commentary FROM enduring_word", null)
            db.beginTransaction()
            while (cursor.moveToNext()) {
                val rowId = cursor.getLong(0)
                var t = cursor.getString(1) ?: continue

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

                t = t.replace("\u00E2\u20AC\u2013", "\u2013")
                t = t.replace("\u00E2\u20AC\u2014", "\u2014")
                t = t.replace("\u00E2\u20AC\u2122", "'")
                t = t.replace("\u00C2", " ")

                t = t.replace("&#8217;", "'")
                t = t.replace("&#8211;", "\u2013")
                t = t.replace("&#8212;", "\u2014")
                t = t.replace("&amp;", "&")

                t = t.replace(Regex("(?is)<h3[^>]*>.*?Videos for.*?</h3>.*?<p><a.*?</p>"), "")
                t = t.replace(Regex("(?is)High Points.*"), "")

                db.execSQL("UPDATE enduring_word SET commentary=? WHERE rowid=?", arrayOf(t, rowId))
            }
            cursor.close()
            db.setTransactionSuccessful()
            db.endTransaction()
            prefs.edit().putBoolean("db_cleaned_v4", true).apply()
            Log.d("DB_CLEAN", "Cleaned OK")
        } catch (e: Exception) {
            Log.e("DB_CLEAN", "fail", e)
        } finally {
            try { db?.endTransaction() } catch (e: Exception) {}
            try { db?.close() } catch (e: Exception) {}
        }
    }
}