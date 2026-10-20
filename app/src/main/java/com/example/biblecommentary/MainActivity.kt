package com.example.biblecommentary

import android.app.Activity
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

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
    private lateinit var topText: TextView
    private lateinit var progressText: TextView
    private lateinit var contentText: TextView
    private lateinit var scrollView: ScrollView
    private var db: SQLiteDatabase? = null
    private var tableName = "commentary"
    private var bookCol = "book"
    private var chapterCol = "chapter"
    private var textCol = "commentary"
    private var currentBook = ""
    private var currentChapter = 1
    private var mode = "books"
    private var chapters = listOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val root = LinearLayout(this)
            root.orientation = LinearLayout.VERTICAL

            progressText = TextView(this)
            progressText.text = "Starting download..."
            progressText.setPadding(20, 100, 20, 10)

            topText = TextView(this)
            topText.text = "Bible Commentary"
            topText.setPadding(20, 10, 20, 20)

            listView = ListView(this)
            contentText = TextView(this)
            contentText.setPadding(20, 20, 20, 20)
            contentText.textSize = 18f

            scrollView = ScrollView(this)
            scrollView.addView(contentText)
            scrollView.visibility = ScrollView.GONE

            root.addView(progressText)
            root.addView(topText)
            root.addView(listView, LinearLayout.LayoutParams(-1, -1, 1f))
            root.addView(scrollView, LinearLayout.LayoutParams(-1, -1, 1f))

            setContentView(root)

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
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
            }

            topText.setOnClickListener {
                if (mode == "chapters") showBooks()
                else if (mode == "content") showChapters()
            }

            // STEP 1: DOWNLOAD FIRST
            showBooks()
            Thread { safeDownload() }.start()

        } catch (e: Exception) {
            val tv = TextView(this)
            tv.text = "Start Error: ${e.message}\n${e.stackTraceToString()}"
            setContentView(tv)
        }
    }

    private fun getDbFile(): File = File(filesDir, "commentary_better.db")

    private fun safeDownload() {
        try {
            val f = getDbFile()
            if (!f.exists() || f.length() < 1024) {
                runOnUiThread { progressText.text = "Downloading from archive.org... 0%" }
                val conn = URL(DB_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.instanceFollowRedirects = true
                conn.connect()
                val inp = conn.inputStream
                val out = FileOutputStream(f)
                val buf = ByteArray(8192)
                var r: Int
                var total = 0L
                val len = conn.contentLength
                while (inp.read(buf).also { r = it } != -1) {
                    out.write(buf, 0, r)
                    total += r
                    if (total % (1024 * 256) == 0L) {
                        val pct = if (len > 0) (total * 100 / len) else 0
                        runOnUiThread { progressText.text = "Downloading: ${total/1024/1024}MB $pct% - Genesis to Revelation" }
                    }
                }
                out.close()
                inp.close()
            }
            runOnUiThread {
                // STEP 2: HIDE PROGRESS AND OPEN
                progressText.visibility = ScrollView.GONE
                openDb()
            }
        } catch (e: Exception) {
            runOnUiThread {
                progressText.text = "Download failed: ${e.message} - Using offline list"
                progressText.visibility = ScrollView.GONE
                showBooks()
            }
        }
    }

    private fun openDb() {
        try {
            val f = getDbFile()
            if (!f.exists()) return
            db = SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY)
            val c = db!!.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            if (c.moveToFirst()) tableName = c.getString(0)
            c.close()
            // Auto detect columns
            val col = db!!.rawQuery("SELECT * FROM $tableName LIMIT 1", null)
            if (col.columnCount >= 3) {
                bookCol = col.getColumnName(0)
                chapterCol = col.getColumnName(1)
                textCol = col.getColumnName(col.columnCount - 1)
                // try to find better names
                for (i in 0 until col.columnCount) {
                    val n = col.getColumnName(i).lowercase()
                    if (n.contains("book")) bookCol = col.getColumnName(i)
                    if (n.contains("chapter")) chapterCol = col.getColumnName(i)
                    if (n.contains("comment") || n.contains("text") || n.contains("content")) textCol = col.getColumnName(i)
                }
            }
            col.close()
            Toast.makeText(this, "DB loaded: Genesis to Revelation", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "DB error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showBooks() {
        mode = "books"
        topText.text = "Select Book - Genesis to Revelation"
        scrollView.visibility = ScrollView.GONE
        listView.visibility = ListView.VISIBLE
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, books)
    }

    private fun showChapters() {
        mode = "chapters"
        topText.text = "$currentBook - Select Chapter - Tap to go back"
        scrollView.visibility = ScrollView.GONE
        listView.visibility = ListView.VISIBLE
        try {
            if (db != null) {
                val cur = db!!.rawQuery("SELECT DISTINCT $chapterCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' ORDER BY CAST($chapterCol AS INTEGER)", null)
                val list = mutableListOf<Int>()
                while (cur.moveToNext()) { try { list.add(cur.getInt(0)) } catch (_: Exception) {} }
                cur.close()
                chapters = if (list.isEmpty()) (1..50).toList() else list
            } else chapters = (1..150).toList()
        } catch (_: Exception) { chapters = (1..50).toList() }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chapters.map { "Chapter $it" })
    }

    private fun showContent() {
        mode = "content"
        listView.visibility = ListView.GONE
        scrollView.visibility = ScrollView.VISIBLE
        contentText.text = "Loading $currentBook $currentChapter..."
        topText.text = "$currentBook $currentChapter - Tap to go back"
        Thread {
            try {
                var txt = ""
                if (db != null) {
                    val cur = db!!.rawQuery("SELECT $textCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' AND $chapterCol='$currentChapter'", null)
                    while (cur.moveToNext()) { txt += cur.getString(0) + "\n\n" }
                    cur.close()
                }
                if (txt.isEmpty()) txt = "No commentary found for $currentBook $currentChapter in this DB.\n\nDB has Genesis to Revelation."
                runOnUiThread { contentText.text = txt; scrollView.scrollTo(0,0) }
            } catch (e: Exception) {
                runOnUiThread { contentText.text = "Error: ${e.message}" }
            }
        }.start()
    }
}