package com.example.biblecommentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val DB_URL = "https://archive.org/download/commentary_better/commentary_better.db"
    private val books = listOf(
        "Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth",
        "1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job",
        "Psalms","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel",
        "Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi",
        "Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians",
        "Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon",
        "Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation"
    )

    private lateinit var listView: ListView
    private lateinit var tv: TextView
    private var db: SQLiteDatabase? = null
    private var tableName = ""
    private var bookCol = ""
    private var chapterCol = ""
    private var textCol = ""
    private var currentBook = ""
    private var currentChapter = 1
    private var mode = "books"
    private var chapters = listOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val lay = LinearLayout(this)
            lay.orientation = LinearLayout.VERTICAL

            tv = TextView(this)
            tv.text = "Bible Commentary - Loading..."
            tv.setPadding(20, 80, 20, 20)
            tv.textSize = 16f

            listView = ListView(this)

            lay.addView(tv)
            lay.addView(listView)

            setContentView(lay)

            listView.setOnItemClickListener { _, _, pos, _ ->
                try {
                    if (mode == "books") {
                        currentBook = books[pos]
                        showChapters()
                    } else if (mode == "chapters") {
                        currentChapter = chapters[pos]
                        showContent()
                    }
                } catch (e: Exception) {
                    tv.text = e.message
                }
            }

            tv.setOnClickListener {
                if (mode == "chapters") showBooks()
                else if (mode == "content") showChapters()
            }

            Thread { download() }.start()

        } catch (e: Exception) {
            val t = TextView(this)
            t.text = "Startup Error: ${e.message}"
            setContentView(t)
        }
    }

    private fun getDbFile(): File {
        return File(filesDir, "commentary_better.db")
    }

    private fun download() {
        try {
            val f = getDbFile()
            if (!f.exists() || f.length() < 1024 * 1024) {
                f.parentFile?.mkdirs()
                runOnUiThread { tv.text = "Downloading DB from archive.org..." }
                val conn = URL(DB_URL).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.instanceFollowRedirects = true
                conn.connect()
                val inp = conn.inputStream
                val out = FileOutputStream(f)
                val buf = ByteArray(8192)
                var r: Int
                var total = 0
                while (inp.read(buf).also { r = it } != -1) {
                    out.write(buf, 0, r)
                    total += r
                    val mb = total / 1024 / 1024
                    runOnUiThread { tv.text = "Downloading... $mb MB" }
                }
                out.close()
                inp.close()
            }
            runOnUiThread { openDb() }
        } catch (e: Exception) {
            runOnUiThread { tv.text = "Download failed: ${e.message}" }
        }
    }

    private fun openDb() {
        try {
            val f = getDbFile()
            tv.text = "Opening ${f.length() / 1024 / 1024} MB"
            db = SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY)
            val c = db!!.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%'", null)
            if (c.moveToFirst()) tableName = c.getString(0)
            c.close()
            val col = db!!.rawQuery("SELECT * FROM $tableName LIMIT 1", null)
            for (i in 0 until col.columnCount) {
                val n = col.getColumnName(i).lowercase()
                if (n.contains("book")) bookCol = col.getColumnName(i)
                if (n.contains("chapter") || n == "ch") chapterCol = col.getColumnName(i)
                if (n.contains("comment") || n.contains("text")) textCol = col.getColumnName(i)
            }
            if (bookCol.isEmpty()) bookCol = col.getColumnName(0)
            if (chapterCol.isEmpty()) chapterCol = col.getColumnName(1)
            if (textCol.isEmpty()) textCol = col.getColumnName(col.columnCount - 1)
            col.close()
            showBooks()
        } catch (e: Exception) {
            tv.text = "DB error: ${e.message}"
        }
    }

    private fun showBooks() {
        mode = "books"
        tv.text = "Select Book - Genesis to Revelation"
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, books)
    }

    private fun showChapters() {
        mode = "chapters"
        tv.text = "$currentBook - Select Chapter - Tap text to go back"
        try {
            val q = "SELECT DISTINCT $chapterCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' ORDER BY CAST($chapterCol AS INTEGER)"
            val cur = db!!.rawQuery(q, null)
            val list = mutableListOf<Int>()
            while (cur.moveToNext()) {
                try { list.add(cur.getInt(0)) } catch (_: Exception) {}
            }
            cur.close()
            chapters = if (list.isEmpty()) (1..50).toList() else list
        } catch (_: Exception) {
            chapters = (1..50).toList()
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chapters.map { "Chapter $it" })
    }

    private fun showContent() {
        mode = "content"
        tv.text = "$currentBook $currentChapter - Loading... Tap to go back"
        Thread {
            try {
                val q = "SELECT $textCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' AND $chapterCol='$currentChapter'"
                val cur = db!!.rawQuery(q, null)
                var txt = ""
                while (cur.moveToNext()) {
                    txt += cur.getString(0) + "\n\n"
                }
                cur.close()
                if (txt.isEmpty()) txt = "No commentary for $currentBook $currentChapter"
                runOnUiThread {
                    tv.text = "$currentBook $currentChapter - Tap to go back"
                    listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf(txt))
                }
            } catch (e: Exception) {
                runOnUiThread { tv.text = "Error: ${e.message}" }
            }
        }.start()
    }
}