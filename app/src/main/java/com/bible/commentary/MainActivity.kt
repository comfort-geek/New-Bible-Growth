package com.bible.commentary

import android.database.sqlite.SQLiteDatabase
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var mainLayout: LinearLayout
    private lateinit var titleView: TextView
    private lateinit var listView: ListView
    private lateinit var contentView: TextView
    private var dbFile: File? = null
    private var currentBook = "Genesis"
    
    private val books = listOf("Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalm","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians","Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleView = TextView(this).apply {
            text = "Enduring Word Commentary"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(24, 32, 24, 16)
        }
        contentView = TextView(this).apply {
            text = "Loading..."
            textSize = 16f
            setPadding(24, 24, 24, 24)
            visibility = TextView.GONE
        }
        listView = ListView(this)
        mainLayout.addView(titleView)
        mainLayout.addView(contentView)
        mainLayout.addView(listView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        setContentView(mainLayout)

        dbFile = File(getDatabasePath("commentary.db").path)
        val file = dbFile!!

        if (!file.exists()) {
            titleView.text = "DB not found"
            contentView.visibility = TextView.VISIBLE
            contentView.text = "Put commentary.db in databases folder"
            return
        }

        Thread {
            val isNewClean = cleanDatabaseOnFirstInstall(file)
            runOnUiThread {
                titleView.text = if(isNewClean) "DB Cleaned - 1189 chapters" else "Enduring Word - Select Book"
                showBooks()
            }
        }.start()
    }

    private fun showBooks() {
        contentView.visibility = TextView.GONE
        listView.visibility = ListView.VISIBLE
        titleView.text = "Select Book (66)"
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, books)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, pos, _ ->
            currentBook = books[pos]
            showChapters(currentBook)
        }
    }

    private fun showChapters(book: String) {
        val chapters = getChapterCount(book)
        val nums = (1..chapters).map { "Chapter $it" }
        titleView.text = "$book - $chapters Chapters"
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nums)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, pos, _ ->
            showCommentary(book, pos+1)
        }
        titleView.setOnClickListener { showBooks() }
    }

    private fun showCommentary(book: String, chapter: Int) {
        listView.visibility = ListView.GONE
        contentView.visibility = TextView.VISIBLE
        titleView.text = "$book $chapter"
        titleView.setOnClickListener { showChapters(book) }
        val text = getCommentary(book, chapter)
        contentView.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun getChapterCount(book: String): Int {
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile!!.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val c = db.rawQuery("SELECT COUNT(*) FROM enduring_word WHERE book=?", arrayOf(book))
            c.moveToFirst()
            val count = c.getInt(0)
            c.close()
            return if(count==0) 50 else count
        } catch(e: Exception) { return 50 } 
        finally { try{db?.close()}catch(e:Exception){} }
    }

    private fun getCommentary(book: String, chapter: Int): String {
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile!!.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val c = db.rawQuery("SELECT commentary FROM enduring_word WHERE book=? AND chapter=?", arrayOf(book, chapter.toString()))
            if(c.moveToFirst()) {
                val t = c.getString(0)
                c.close()
               