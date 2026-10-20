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
    private lateinit var topText: TextView
    private lateinit var progressText: TextView
    private lateinit var contentText: TextView
    private lateinit var scrollView: ScrollView
    private var db: SQLiteDatabase? = null
    private var tableName = ""
    private var bookCol = ""
    private var chapterCol = ""
    private var textCol = ""
    private var currentBook = ""
    private var currentChapter = 1
    private var mode = "books"
    private var chapters = listOf<Int>()
    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        progressText = TextView(this)
        progressText.text = "Checking DB..."
        progressText.setPadding(20, 80, 20, 10)
        progressText.textSize = 14f

        topText = TextView(this)
        topText.text = "Bible Commentary"
        topText.setPadding(20, 10, 20, 20)
        topText.textSize = 16f

        listView = ListView(this)

        contentText = TextView(this)
        contentText.setPadding(20, 20, 20, 20)
        contentText.textSize = 17f

        scrollView = ScrollView(this)
        scrollView.addView(contentText)
        scrollView.visibility = android.view.View.GONE

        root.addView(progressText)
        root.addView(topText)
        root.addView(listView, LinearLayout.LayoutParams(-1, -1, 1f))
        root.addView(scrollView, LinearLayout.LayoutParams(-1, -1, 1f))

        setContentView(root)

        listView.setOnItemClickListener { _, _, pos, _ ->
            if (isDownloading) {
                Toast.makeText(this, "Please wait, downloading...", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            if (mode == "books") {
                currentBook = books[pos]
                showChapters()
            } else if (mode == "chapters") {
                currentChapter = chapters[pos]
                showContent()
            }
        }

        topText.setOnClickListener {
            if (mode == "chapters") showBooks()
            else if (mode == "content") showChapters()
        }

        Thread { download() }.start()
    }

    private fun getDbFile(): File = File(filesDir, "commentary_better.db")

    private fun download() {
        try {
            val f = getDbFile()
            if (!f.exists() || f.length() < 1024 * 1024) {
                isDownloading = true
                runOnUiThread {
                    progressText.visibility = android.view.View.VISIBLE
                    progressText.text = "Downloading commentary_better.db from archive.org - 0 MB"
                    topText.text = "Genesis to Revelation - Please wait..."
                    listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Downloading... please wait"))
                }
                val conn = URL(DB_URL).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 20000
                conn.readTimeout = 20000
                conn.instanceFollowRedirects = true
                conn.connect()
                if (conn.responseCode == 200) {
                    val inp = conn.inputStream
                    val out = FileOutputStream(f)
                    val buf = ByteArray(8192)
                    var r: Int
                    var total = 0L
                    while (inp.read(buf).also { r = it } != -1) {
                        out.write(buf, 0, r)
                        total += r
                        val mb = total / 1024 / 1024
                        val percent = if (conn.contentLength > 0) (total * 100 / conn.contentLength).toInt() else 0
                        runOnUiThread {
                            progressText.text = "Downloading: $mb MB ($percent%) - ${total/1024} KB"
                        }
                    }
                    out.close()
                    inp.close()
                }
                isDownloading = false
            }
            runOnUiThread { openDb() }
        } catch (e: Exception) {
            runOnUiThread {
                isDownloading = false
                progressText.visibility = android.view.View.VISIBLE
                progressText.text = "Download failed: ${e.message}"
                topText.text = "Tap a book to try offline"
                showBooks()
            }
        }
    }

    private fun openDb() {
        try {
            progressText.visibility = android.view.View.GONE
            val dbFile = getDbFile()
            if (!dbFile.exists()) {
                progressText.visibility = android.view.View.VISIBLE
                progressText.text = "No DB file"
                showBooks()
                return
            }
            db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
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
            if (chapterCol.isEmpty() && col.columnCount > 1) chapterCol = col.getColumnName(1)
            if (textCol.isEmpty()) textCol = col.getColumnName(col.columnCount - 1)
            col.close()
            showBooks()
        } catch (e: Exception) {
            progressText.visibility = android.view.View.VISIBLE
            progressText.text = "DB error: ${e.message}"
            showBooks()
        }
    }

    private fun showBooks() {
        mode = "books"
        if (!isDownloading) {
            topText.text = "Select Book - Genesis to Revelation - Tap a book"
        }
        scrollView.visibility = android.view.View.GONE
        listView.visibility = android.view.View.VISIBLE
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, books)
    }

    private fun showChapters() {
        mode = "chapters"
        topText.text = "$currentBook - Select Chapter - Tap here to go back"
        scrollView.visibility = android.view.View.GONE
        listView.visibility = android.view.View.VISIBLE
        try {
            if (db != null) {
                val q = "SELECT DISTINCT $chapterCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' ORDER BY CAST($chapterCol AS INTEGER)"
                val cur = db!!.rawQuery(q, null)
                val list = mutableListOf<Int>()
                while (cur.moveToNext()) {
                    try { list.add(cur.getInt(0)) } catch (_: Exception) {}
                }
                cur.close()
                chapters = if (list.isEmpty()) (1..50).toList() else list
            } else {
                chapters = (1..50).toList()
            }
        } catch (_: Exception) {
            chapters = (1..50).toList()
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chapters.map { "Chapter $it" })
    }

    private fun showContent() {
        mode = "content"
        listView.visibility = android.view.View.GONE
        scrollView.visibility = android.view.View.VISIBLE
        contentText.text = "Loading $currentBook $currentChapter..."
        topText.text = "$currentBook $currentChapter - Tap here to go back"
        Thread {
            try {
                var txt = ""
                if (db != null) {
                    val q = "SELECT $textCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' AND $chapterCol='$currentChapter'"
                    val cur = db!!.rawQuery(q, null)
                    while (cur.moveToNext()) {
                        val s = cur.getString(0)
                        if (s != null) txt += s + "\n\n"
                    }
                    cur.close()
                }
                if (txt.isEmpty()) txt = "No commentary for $currentBook $currentChapter"
                runOnUiThread {
                    contentText.text = txt
                    scrollView.scrollTo(0, 0)
                }
            } catch (e: Exception) {
                runOnUiThread { contentText.text = "Error: ${e.message}" }
            }
        }.start()
    }
}