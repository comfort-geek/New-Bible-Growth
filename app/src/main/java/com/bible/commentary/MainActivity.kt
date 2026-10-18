package com.bible.commentary

import android.database.sqlite.SQLiteDatabase
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private var dbFile: File? = null
    private var mode = "BOOKS" // BOOKS, CHAPTERS, READING
    private var curBook = "Genesis"
    private var curChapter = 1
    private var curChCount = 50

    private val bookCh = mapOf(
        "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36,
        "Deuteronomy" to 34, "Joshua" to 24, "Judges" to 21, "Ruth" to 4,
        "1 Samuel" to 31, "2 Samuel" to 24, "1 Kings" to 22, "2 Kings" to 25,
        "1 Chronicles" to 29, "2 Chronicles" to 36, "Ezra" to 10, "Nehemiah" to 13,
        "Esther" to 10, "Job" to 42, "Psalm" to 150, "Proverbs" to 31,
        "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66, "Jeremiah" to 52,
        "Lamentations" to 5, "Ezekiel" to 48, "Daniel" to 12, "Hosea" to 14,
        "Joel" to 3, "Amos" to 9, "Obadiah" to 1, "Jonah" to 4, "Micah" to 7,
        "Nahum" to 3, "Habakkuk" to 3, "Zephaniah" to 3, "Haggai" to 2,
        "Zechariah" to 14, "Malachi" to 4, "Matthew" to 28, "Mark" to 16,
        "Luke" to 24, "John" to 21, "Acts" to 28, "Romans" to 16,
        "1 Corinthians" to 16, "2 Corinthians" to 13, "Galatians" to 6,
        "Ephesians" to 6, "Philippians" to 4, "Colossians" to 4,
        "1 Thessalonians" to 5, "2 Thessalonians" to 3, "1 Timothy" to 6,
        "2 Timothy" to 4, "Titus" to 3, "Philemon" to 1, "Hebrews" to 13,
        "James" to 5, "1 Peter" to 5, "2 Peter" to 3, "1 John" to 5,
        "2 John" to 1, "3 John" to 1, "Jude" to 1, "Revelation" to 22
    )

    private lateinit var root: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var contentScroll: ScrollView
    private lateinit var gridLayout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        dbFile = File(getDatabasePath("commentary.db").path)
        if (dbFile!!.exists()) {
            Thread { cleanDatabaseOnFirstInstall(dbFile!!) }.start()
        }

        // Root
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        // Header - like your screenshot
        header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF101828.toInt())
            setPadding(0, 50, 0, 30)
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = "Bible Commentary"
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "Genesis to Revelation - Offline"
            textSize = 14f
            setTextColor(0xFF9AA0B0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        header.addView(title)
        header.addView(sub)

        contentScroll = ScrollView(this)
        gridLayout = GridLayout(this).apply {
            columnCount = 2
            setPadding(16, 16, 16, 16)
        }

        contentScroll.addView(gridLayout)
        root.addView(header)
        root.addView(contentScroll)
        setContentView(root)

        showBooks()
    }

    private fun showBooks() {
        mode = "BOOKS"
        gridLayout.removeAllViews()
        for ((book, ch) in bookCh) {
            val card = createCard(book, "$ch ch")
            card.setOnClickListener {
                curBook = book
                curChCount = ch
                showChapters()
            }
            gridLayout.addView(card)
        }
    }

    private fun showChapters() {
        mode = "CHAPTERS"
        gridLayout.removeAllViews()
        for (i in 1..curChCount) {
            val card = createCard("Chapter $i", curBook)
            card.setOnClickListener {
                curChapter = i
                showReading()
            }
            gridLayout.addView(card)
        }
    }

    private fun showReading() {
        mode = "READING"
        gridLayout.removeAllViews()
        
        // Swipe listener
        var x1 = 0f
        gridLayout.setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> x1 = event.x
                MotionEvent.ACTION_UP -> {
                    val x2 = event.x
                    if (x2 < x1 - 100) nextChapter()
                    if (x2 > x1 + 100) prevChapter()
                }
            }
            true
        }

        val tv = TextView(this).apply {
            val html = getCommentary(curBook, curChapter)
            text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            textSize = 16f
            setPadding(24, 24, 24, 100)
            setTextColor(0xFF111111.toInt())
        }
        val params = GridLayout.LayoutParams()
        params.columnSpec = GridLayout.spec(0, 2)
        params.width = GridLayout.LayoutParams.MATCH_PARENT
        gridLayout.addView(tv, params)
        
        // update header
        (header.getChildAt(0) as TextView).text = "$curBook $curChapter"
        (header.getChildAt(1) as TextView).text = "Swipe left / right for next / prev"
    }

    private fun nextChapter() {
        if (curChapter < curChCount) {
            curChapter++
            showReading()
        }
    }
    private fun prevChapter() {
        if (curChapter > 1) {
            curChapter--
            showReading()
        }
    }

    private fun createCard(top: String, bottom: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            setPadding(16, 24, 16, 24)
            gravity = Gravity.CENTER
        }
        val tv1 = TextView(this).apply {
            text = top
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF111827.toInt())
            gravity = Gravity.CENTER
        }
        val tv2 = TextView(this).apply {
            text = bottom
            textSize = 14f
            setTextColor(0xFF374151.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
        }
        card.addView(tv1)
        card.addView(tv2)
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(8, 8, 8, 8)
        card.layoutParams = params
        return card
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
            return "Not found"
        } catch(e: Exception) { return "Error ${e.message}" }
        finally { try{db?.close()}catch(e:Exception){} }
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
                t = t.replace("&amp;", "&")
                db.execSQL("UPDATE enduring_word SET commentary=? WHERE rowid=?", arrayOf(t, rowId))
            }
            cursor.close()
            db.setTransactionSuccessful()
            db.endTransaction()
            prefs.edit().putBoolean("db_cleaned_v4", true).apply()
            Log.d("DB_CLEAN", "Cleaned OK")
            return true
        } catch (e: Exception) { return false }
        finally {
            try { cursor?.close() } catch (e: Exception) {}
            try { db?.endTransaction() } catch (e: Exception) {}
            try { db?.close() } catch (e: Exception) {}
        }
    }

    override fun onBackPressed() {
        if (mode == "READING") showChapters()
        else if (mode == "CHAPTERS") showBooks()
        else super.onBackPressed()
    }
}