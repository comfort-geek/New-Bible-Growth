package com.bible.commentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

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

    private var curBook = "Genesis"; private var curChapter = 1; private var curChCount = 50; private var mode = 0
    private lateinit var header: TextView; private lateinit var root: LinearLayout; private lateinit var scroll: ScrollView
    private lateinit var dbFile: File; private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFF8F6F1.toInt()) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF111827.toInt()); setPadding(dp(16), dp(28), dp(16), dp(16)) }
        header = TextView(this).apply { text = "Bible Commentary"; setTextColor(0xFFFFFFFF.toInt()); setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f); gravity = Gravity.CENTER; setTypeface(typeface, android.graphics.Typeface.BOLD) }
        top.addView(header)
        scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)) }
        scroll.addView(content)
        root.addView(top); root.addView(scroll); setContentView(root)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false; val dx = e2.x - e1.x
                if (abs(dx) > 100 && abs(vx) > 100) { if (dx < 0) nextChapter() else prevChapter(); return true }
                return false
            }
        })
        scroll.setOnTouchListener { _, e -> gestureDetector.onTouchEvent(e); false }
        content.setOnTouchListener { _, e -> gestureDetector.onTouchEvent(e); false }

        dbFile = getDatabasePath("commentary.db")
        dbFile.parentFile?.mkdirs()
        if (!dbFile.exists()) copyDbFromAssets()
        if (!dbFile.exists()) { header.text = "DB not found"; showBooks(content); return }
        cleanDatabaseOnFirstInstall(dbFile)
        showBooks(content)
    }

    private fun copyDbFromAssets() {
        try {
            val possible = listOf("commentary.db", "databases/commentary.db")
            for (name in possible) {
                try {
                    assets.open(name).use { input -> FileOutputStream(dbFile).use { output -> input.copyTo(output) } }
                    if (dbFile.exists() && dbFile.length() > 0) return
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun cleanDatabaseOnFirstInstall(file: File) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("db_cleaned_v4", false)) return
        try {
            val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
            val cur = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            while (cur.moveToNext()) {
                val table = cur.getString(0)
                try {
                    db.execSQL("UPDATE $table SET commentary = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(commentary,'â€“','–'),'â€™','''),'â€œ','\"'),'â€','\"'),'â€˜','''')")
                    db.execSQL("UPDATE $table SET commentary = REPLACE(REPLACE(REPLACE(commentary,'&#8217;',''''),'&#8220;','\"'),'&#8221;','\"')")
                } catch (_: Exception) {}
            }
            cur.close(); db.close()
            prefs.edit().putBoolean("db_cleaned_v4", true).apply()
        } catch (_: Exception) {}
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF111827.toInt()); setPadding(dp(6), dp(18), dp(6), dp(6))
        }
    }

    private fun makeGrid(books: List<String>): GridLayout {
        val grid = GridLayout(this).apply { columnCount = 2 }
        books.forEach { book ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(dp(12), dp(12), dp(12), dp(12))
                val lp = GridLayout.LayoutParams().apply { width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(dp(6), dp(6), dp(6), dp(6)) }
                layoutParams = lp; isClickable = true; setOnClickListener { curBook = book; curChCount = bookCh[book] ?: 50; showChapters() }
            }
            card.addView(TextView(this).apply { text = book; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) })
            card.addView(TextView(this).apply { text = "${bookCh[book]} ch"; setTextColor(0xFF6B7280.toInt()); setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f) })
            grid.addView(card)
        }
        return grid
    }

    private fun showBooks(container: LinearLayout? = null) {
        mode = 0; val c = container ?: (scroll.getChildAt(0) as LinearLayout); c.removeAllViews(); header.text = "Bible Commentary"
        c.addView(sectionTitle("OLD TESTAMENT"))
        c.addView(makeGrid(otBooks))
        c.addView(sectionTitle("NEW TESTAMENT"))
        c.addView(makeGrid(ntBooks))
    }

    private fun showChapters() {
        mode = 1; val c = scroll.getChildAt(0) as LinearLayout; c.removeAllViews(); header.text = curBook
        val grid = GridLayout(this).apply { columnCount = 4 }
        for (i in 1..curChCount) {
            val card = TextView(this).apply {
                text = i.toString(); gravity = Gravity.CENTER; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(dp(12), dp(16), dp(12), dp(16))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                val lp = GridLayout.LayoutParams().apply { width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(dp(6), dp(6), dp(6), dp(6)) }
                layoutParams = lp; isClickable = true; setOnClickListener { curChapter = i; showReading() }
            }
            grid.addView(card)
        }
        c.addView(grid)
    }

    private fun showReading() {
        mode = 2; val c = scroll.getChildAt(0) as LinearLayout; c.removeAllViews()
        header.text = "$curBook $curChapter"
        val title = TextView(this).apply { text = "$curBook $curChapter"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f); setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, dp(8), 0, dp(12)) }
        val body = TextView(this).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); setLineSpacing(dp(4).toFloat(), 1.2f) }
        c.addView(title); c.addView(body)
        try {
            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            var txt = "Commentary not found for $curBook $curChapter"
            val tables = listOf(curBook.replace(" ", ""), curBook, curBook.lowercase(), "commentary")
            for (t in tables) {
                try {
                    val cur = db.rawQuery("SELECT commentary FROM $t WHERE chapter=? LIMIT 1", arrayOf(curChapter.toString()))
                    if (cur.moveToFirst()) { txt = cur.getString(0); cur.close(); break }
                    cur.close()
                } catch (_: Exception) {}
            }
            db.close()
            body.text = txt.replace(Regex("<[^>]*>"), "")
        } catch (e: Exception) { body.text = "Error: ${e.message}" }
    }

    private fun nextChapter() { if (mode != 2) return; if (curChapter < curChCount) { curChapter++; showReading() } }
    private fun prevChapter() { if (mode != 2) return; if (curChapter > 1) { curChapter--; showReading() } }
    override fun onBackPressed() { when (mode) { 2 -> showChapters(); 1 -> showBooks(); else -> super.onBackPressed() } }
    private fun dp(v: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
}