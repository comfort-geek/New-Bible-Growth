package com.bible.commentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dbFile = File(getDatabasePath("commentary.db").path)

        // if first time, download will happen in your existing code
        // For now we ensure DB exists then clean it
        if (!dbFile.exists()) {
            // If you download from internet, put your download code here
            // downloadDatabase(dbFile)
            Log.d("DB", "DB not found - you need to download first")
        } else {
            cleanDatabaseOnFirstInstall(dbFile)
        }

        openChapterView("Genesis", 1)
    }

    private fun openChapterView(book: String, chapter: Int) {
        // your existing code to open chapter - keep it
        // example:
        // val intent = Intent(this, ChapterActivity::class.java)
        // intent.putExtra("book", book)
        // intent.putExtra("chapter", chapter)
        // startActivity(intent)
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

                // fix mojibake - same logic as your pydroid script
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

                // safe replaces - unicode escapes only
                t = t.replace("\u00E2\u20AC\u2013", "\u2013")
                t = t.replace("\u00E2\u20AC\u2014", "\u2014")
                t = t.replace("\u00E2\u20AC\u2122", "'")
                t = t.replace("\u00E2\u20AC\u0153", "\"")
                t = t.replace("\u00E2\u20AC\u009D", "\"")
                t = t.replace("\u00E2\u20AC\u00A6", "...")
                t = t.replace("\u00C2", " ")

                t = t.replace("&#8217;", "'")
                t = t.replace("&#8211;", "\u2013")
                t = t.replace("&#8212;", "\u2014")
                t = t.replace("&#8220;", "\"")
                t = t.replace("&#8221;", "\"")
                t = t.replace("&amp;", "&")
                t = t.replace("&#038;", "&")
                t = t.replace("&#8216;", "'")

                // remove Videos block and High Points
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