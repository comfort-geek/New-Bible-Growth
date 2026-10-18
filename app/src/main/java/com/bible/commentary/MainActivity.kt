package com.bible.commentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val otBooks = listOf("Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi")
    private val ntBooks = listOf("Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians","Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation")

    private val bookCh = mapOf(
        "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36, "Deuteronomy" to 34,
        "Joshua" to 24, "Judges" to 21, "Ruth" to 4, "1 Samuel" to 31, "2 Samuel" to 24,
        "1 Kings" to 22, "2 Kings" to 25, "1 Chronicles" to 29, "2 Chronicles" to 36,
        "Ezra" to 10, "Nehemiah" to 13, "Esther" to 10, "Job" to 42, "Psalms" to 150,
        "Proverbs" to 31, "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66,
        "Jeremiah" to 52, "Lamentations" to 5, "Ezekiel" to 48, "Daniel" to 12,
        "Hosea" to 14, "Joel" to 3, "Amos" to 9, "Obadiah" to 1, "Jonah" to 4, "Micah" to 7,
        "Nahum" to 3, "Habakkuk" to 3, "Zephaniah" to 3, "Haggai" to 2, "Zechariah" to 14,
        "Malachi" to 4, "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21,
        "Acts" to 28, "Romans" to 16, "1 Corinthians" to 16, "2 Corinthians" to 13,
        "Galatians" to 6, "Ephesians" to 6, "Philippians" to 4, "Colossians" to 4,
        "1 Thessalonians" to 5, "2 Thessalonians" to 3, "1 Timothy" to 6, "2 Timothy" to 4,
        "Titus" to 3, "Philemon" to 1, "Hebrews" to 13, "James" to 5, "1 Peter" to 5,
        "2 Peter" to 3, "1 John" to 5, "2 John" to 1, "3 John" to 1, "Jude" to 1, "Revelation" to 22
    )

    private var curBook = "Genesis"
    private var curChapter = 1
    private var curChCount = 50
    private var mode = 0
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private lateinit var dbFile: File
    private var allTables: List<String> = emptyList()
    private val DB_URL = "https://archive.org/download/commentary_202610/commentary.db"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFFF8F6F1.toInt())
        scroll = ScrollView(this)
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(12), dp(12), dp(12), dp(12))
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)

        dbFile = getDatabasePath("commentary.db")
        dbFile.parentFile?.mkdirs()

        if (!dbFile.exists() || dbFile.length() < 1000000) {
            downloadDB()
        } else {
            loadDB()
        }
    }

    private fun downloadDB() {
        content.removeAllViews()
        val tv = TextView(this)
        tv.text = "Downloading commentary (40.2 MB)...\nFirst time only.\n\n0%"
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tv.setPadding(dp(16), dp(32), dp(16), dp(16))
        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        pb.max = 100
        content.addView(tv)
        content.addView(pb)
        Thread {
            try {
                val url = URL(DB_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connect()
                val total = conn.contentLength
                val input = conn.inputStream
                val output = FileOutputStream(dbFile)
                val buffer = ByteArray(8192)
                var read: Int
                var downloaded = 0
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    val percent = if (total > 0) (downloaded * 100 / total) else 0
                    runOnUiThread {
                        pb.progress = percent
                        tv.text = "Downloading commentary (40.2 MB)...\nFirst time only.\n\n$percent% - ${downloaded/1024/1024} MB"
                    }
                }
                output.close()
                input.close()
                runOnUiThread { loadDB() }
            } catch (e: Exception) {
                runOnUiThread { tv.text = "Download failed: ${e.message}" }
            }
        }.start()
    }

    private fun loadDB() {
        try {
            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            val cur = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            val list = mutableListOf<String>()
            while (cur.moveToNext()) list.add(cur.getString(0))
            cur.close()
            db.close()
            allTables = list
        } catch (_: Exception) {}
        showBooks()
    }

    private fun sectionTitle(text: String): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        t.setTypeface(t.typeface, android.graphics.Typeface.BOLD)
        t.setTextColor(0xFF111827.toInt())
        t.setPadding(dp(6), dp(22), dp(6), dp(6))
        return t
    }

    private fun makeGrid(books: List<String>): GridLayout {
        val grid = GridLayout(this)
        grid.columnCount = 2
        for (book in books) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setBackgroundColor(0xFFFFFFFF.toInt())
            card.setPadding(dp(14), dp(14), dp(14), dp(14))
            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            lp.setMargins(dp(6), dp(6), dp(6), dp(6))
            card.layoutParams = lp
            card.isClickable = true
            card.setOnClickListener {
                curBook = book
                curChCount = bookCh[book] ?: 50
                showChapters()
            }
            val tv = TextView(this)
            tv.text = "$book ${bookCh[book]} Chapters"
            tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            card.addView(tv)
            grid.addView(card)
        }
        return grid
    }

    private fun showBooks() {
        mode = 0
        content.removeAllViews()
        content.addView(sectionTitle("OLD TESTAMENT"))
        content.addView(makeGrid(otBooks))
        content.addView(sectionTitle("NEW TESTAMENT"))
        content.addView(makeGrid(ntBooks))
    }

    private fun showChapters() {
        mode = 1
        content.removeAllViews()
        val backTitle = TextView(this)
        backTitle.text = curBook
        backTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        backTitle.setTypeface(backTitle.typeface, android.graphics.Typeface.BOLD)
        backTitle.setPadding(0