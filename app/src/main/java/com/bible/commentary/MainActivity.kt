package com.enduringword.app

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val prefs by lazy { getSharedPreferences("bible_prefs", MODE_PRIVATE) }
    private var currentBook = "Genesis"
    private var currentChapter = 1

    private val books = listOf(
        "Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth",
        "1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah",
        "Esther","Job","Psalm","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah",
        "Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum",
        "Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John",
        "Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians","Philippians",
        "Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus",
        "Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation"
    )

    private val chapterCount = mapOf(
        "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36, "Deuteronomy" to 34,
        "Joshua" to 24, "Judges" to 21, "Ruth" to 4, "1 Samuel" to 31, "2 Samuel" to 24,
        "1 Kings" to 22, "2 Kings" to 25, "1 Chronicles" to 29, "2 Chronicles" to 36,
        "Ezra" to 10, "Nehemiah" to 13, "Esther" to 10, "Job" to 42, "Psalm" to 150,
        "Proverbs" to 31, "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66,
        "Jeremiah" to 52, "Lamentations" to 5, "Ezekiel" to 48, "Daniel" to 12, "Hosea" to 14,
        "Joel" to 3, "Amos" to 9, "Obadiah" to 1, "Jonah" to 4, "Micah" to 7, "Nahum" to 3,
        "Habakkuk" to 3, "Zephaniah" to 3, "Haggai" to 2, "Zechariah" to 14, "Malachi" to 4,
        "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21, "Acts" to 28, "Romans" to 16,
        "1 Corinthians" to 16, "2 Corinthians" to 13, "Galatians" to 6, "Ephesians" to 6,
        "Philippians" to 4, "Colossians" to 4, "1 Thessalonians" to 5, "2 Thessalonians" to 3,
        "1 Timothy" to 6, "2 Timothy" to 4, "Titus" to 3, "Philemon" to 1, "Hebrews" to 13,
        "James" to 5, "1 Peter" to 5, "2 Peter" to 3, "1 John" to 5, "2 John" to 1, "3 John" to 1,
        "Jude" to 1, "Revelation" to 22
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(JSBridge(), "Android")
        setupSwipe()
        ensureDbAndStart()
    }

    // FIXED: Uses YOUR Internet Archive link
    private fun ensureDbAndStart() {
        val dbFile = File(filesDir, "commentary.db")
        if (dbFile.exists() && dbFile.length() > 1000000) {
            startApp()
            return
        }
        showLoading("Downloading 42MB commentary... First launch only (from Internet Archive)")
        thread {
            try {
                val url = "https://archive.org/download/commentary_202610/commentary.db"
                URL(url).openStream().use { input ->
                    FileOutputStream(dbFile).use { output -> input.copyTo(output) }
                }
                runOnUiThread { startApp() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun startApp() {
        val savedBook = prefs.getString("last_book", null)
        val savedChapter = prefs.getInt("last_chapter", 0)
        if (savedBook != null && savedChapter > 0) {
            openChapterView(savedBook, savedChapter)
        } else {
            showBookList()
        }
    }

    private fun showBookList() {
        val htmlBooks = books.joinToString("") { "<a class='book' onclick=\"Android.openBook('$it')\">$it</a>" }
        val page = """
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body{font-family:sans-serif;background:#f6f7f9;margin:0;padding:16px}
                h1{text-align:center;color:#1a1a1a}
                .grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;max-width:600px;margin:0 auto}
                .book{background:white;padding:14px;border-radius:12px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.08);font-weight:600;color:#2a6db0;cursor:pointer}
                .resume{background:#2a6db0;color:white;padding:14px;border-radius:12px;text-align:center;max-width:600px;margin:0 auto 16px;cursor:pointer;font-weight:700}
            </style></head><body>
            <h1>Enduring Word</h1>
            ${if(prefs.contains("last_book")) "<div class='resume' onclick=\"Android.resume()\">Resume: ${prefs.getString("last_book")} ${prefs.getInt("last_chapter",1)}</div>" else ""}
            <div class="grid">$htmlBooks</div></body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null)
    }

    private fun showChapterList(book: String) {
        currentBook = book
        val count