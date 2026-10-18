package com.bible.commentary

import android.database.sqlite.SQLiteDatabase
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Gravity
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

        // ---- Real UI Built in Code (no R needed) ----
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFAFAFA.toInt())
        }

        titleView = TextView(this).apply {
            text = "Enduring Word Commentary"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(24, 32, 24, 16)
            setBackgroundColor(0xFF1A237E.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        contentView = TextView(this).apply {
            text = "Loading database..."
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
            contentView.text = "Put commentary.db in:\n/data/data/com.bible.commentary/databases/"
            return
        }

        // Clean DB + then show books
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
        // get chapter count from DB
        val chapters = getChapterCount(book)
        val nums = (1..chapters).map { "Chapter $it" }
        titleView.text = "$book - $chapters Chapters"
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nums)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, pos, _ ->
            showCommentary(book, pos+1)
        }
        // back button
        titleView.setOnClickListener { showBooks() }
    }

    private fun showCommentary(book: String, chapter: Int) {
        listView.visibility = ListView.GONE
        contentView.visibility = TextView.VISIBLE
        titleView.text = "$book $chapter"
        titleView.setOnClickListener { showChapters(book) }
        
        val text = getCommentary(book, chapter)
        contentView.text = if (android.os.Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(text)
        }
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
                return t
            }
            c.close()
            return "Commentary not found for $book $chapter"
        } catch(e: Exception) {
            return "Error: ${e.message}"
        } finally { try{db?.close()}catch(e:Exception){} }
    }

    private fun cleanDatabaseOnFirstInstall(file: File): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("db_cleaned_v4", false)) return false
        if (!file.exists()) return false
        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            cursor = db.rawQuery("SELECT rowid, commentary FROM enduring_word", null)
            db.beginTransaction()
            while (cursor.moveToNext()) {
                val rowId = cursor.getLong(0)
                var t = cursor.getString(1) ?: continue
                var prev: String; var loop=0
                do {
                    prev=t
                    try{
                        val b=t.toByteArray(charset("ISO-8859-1"))
                        val d=String(b, charset("UTF-8"))
                        if(d.length <= t.length+20) t=d
                    }catch(e:Exception){break}
                    loop++
                }while(t!=prev && loop<5)
                t=t.replace("\u00E2\u20AC\u2013","\u2013").replace("\u00E2\u20AC\u2014","\u2014").replace("\u00E2\u20AC\u2122","'").replace("\u00C2"," ")
                t=t.replace("&#8217;","'").replace("&#8211;","\u2013").replace("&#8212;","\u2014").replace("&amp;","&")
                t=t.replace(Regex("(?is)<h3[^>]*>.*?Videos for.*?</h3>.*?<p><a.*?</p>"),"")
                t=t.replace(Regex("(?is)High Points.*"),"")
                db.execSQL("UPDATE enduring_word SET commentary=? WHERE rowid=?", arrayOf(t, rowId))
            }
            cursor.close(); db.setTransactionSuccessful(); db.endTransaction()
            prefs.edit().putBoolean("db_cleaned