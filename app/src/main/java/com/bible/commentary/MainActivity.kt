package com.bible.commentary

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

    private fun ensureDbAndStart() {
        val dbFile = File(filesDir, "commentary.db")
        if (dbFile.exists() && dbFile.length() > 1000000) {
            startApp()
            return
        }
        showLoading("Downloading 42MB commentary... First launch only")
        thread {
            try {
                val url = "https://archive.org/download/commentary_202610/commentary.db"
                URL(url).openStream().use { input ->
                    FileOutputStream(dbFile).use { output -> input.copyTo(output) }
                }
                runOnUiThread { startApp() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Download failed: " + e.message, Toast.LENGTH_LONG).show() }
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
        val sb = StringBuilder()
        for (b in books) {
            sb.append("<a class='book' onclick=\"Android.openBook('" + b + "')\">" + b + "</a>")
        }
        var resumeDiv = ""
        if (prefs.contains("last_book")) {
            val rb = prefs.getString("last_book", "Genesis")
            val rc = prefs.getInt("last_chapter", 1)
            resumeDiv = "<div class='resume' onclick=\"Android.resume()\">Resume: " + rb + " " + rc + "</div>"
        }
        val page = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1'>" +
                "<style>body{font-family:sans-serif;background:#f6f7f9;margin:0;padding:16px} h1{text-align:center} .grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;max-width:600px;margin:0 auto} .book{background:white;padding:14px;border-radius:12px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,0.08);font-weight:600;color:#2a6db0} .resume{background:#2a6db0;color:white;padding:14px;border-radius:12px;text-align:center;max-width:600px;margin:0 auto 16px;font-weight:700}</style></head><body>" +
                "<h1>Enduring Word</h1>" + resumeDiv + "<div class='grid'>" + sb.toString() + "</div></body></html>"
        webView.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null)
    }

    private fun showChapterList(book: String) {
        currentBook = book
        val count = chapterCount[book] ?: 50
        val sb = StringBuilder()
        for (i in 1..count) {
            sb.append("<a class='ch' onclick=\"Android.openChapter('" + book + "'," + i + ")\">" + i + "</a>")
        }
        val page = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1'>" +
                "<style>body{font-family:sans-serif;background:#f6f7f9;margin:0;padding:16px} .top{display:flex;justify-content:space-between;align-items:center;max-width:600px;margin:0 auto 16px} .btn{background:white;padding:10px 16px;border-radius:10px;color:#333;box-shadow:0 2px 6px rgba(0,0,0,0.1)} .grid{display:grid;grid-template-columns:repeat(5,1fr);gap:10px;max-width:600px;margin:0 auto} .ch{background:white;padding:16px 0;border-radius:10px;text-align:center;font-weight:700;color:#2a6db0;box-shadow:0 2px 6px rgba(0,0,0,0.08)}</style></head><body>" +
                "<div class='top'><a class='btn' onclick=\"Android.showBooks()\">Books</a><h2 style='margin:0'>" + book + "</h2><span></span></div>" +
                "<div class='grid'>" + sb.toString() + "</div></body></html>"
        webView.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null)
    }

    private fun openChapterView(book: String, chapter: Int) {
        currentBook = book
        currentChapter = chapter
        prefs.edit().putString("last_book", book).putInt("last_chapter", chapter).apply()
        thread {
            try {
                val dbFile = File(filesDir, "commentary.db")
                val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery("SELECT html FROM commentary WHERE book=? AND chapter=?", arrayOf(book, chapter.toString()))
                var raw = "Not found: " + book + " " + chapter
                if (cursor.moveToFirst()) raw = cursor.getString(0)
                cursor.close()
                db.close()

                var html = raw.replace("â€™", "'").replace("â€œ", "\"").replace("â€", "\"").replace("â€“", "-").replace("â€”", "-")
                html = html.replace(Regex("(?i)AI Summary:?"), "")
                for (m in listOf("Related Commentary", "Referenced By", "Cited Authors", "These passages are drawn")) {
                    val idx = html.indexOf(m)
                    if (idx != -1) { html = html.substring(0, idx); break }
                }

                val page = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1'>" +
                        "<style>body{font-family:Georgia,serif;line-height:1.85;font-size:17px;color:#222;background:#fefefe;margin:0} .top{position:sticky;top:0;background:white;display:flex;justify-content:space-between;align-items:center;padding:12px 16px;box-shadow:0 2px 10px rgba(0,0,0,0.08)} .btn{padding:8px 14px;background:#f1f2f4;border-radius:8px;color:#333;font-family:sans-serif;font-size:14px} .content{max-width:720px;margin:0 auto;padding:20px 18px 40px} h2{border-bottom:2px solid #eee;padding-bottom:10px} blockquote{border-left:4px solid #2a6db0;margin:18px 0;padding:10px 16px;background:#f7f9ff}</style></head><body>" +
                        "<div class='top'><a class='btn' onclick=\"Android.openBook('" + book + "')\">" + book + "</a><span style='font-family:sans-serif;font-weight:700'>" + book + " " + chapter + "</span><a class='btn' onclick=\"Android.nextChapter()\">Next</a></div>" +
                        "<div class='content'><h2>" + book + " " + chapter + "</h2>" + html +
                        "<div style='display:flex;gap:12px;margin-top:30px'><a class='btn' style='flex:1;text-align:center' onclick=\"Android.prevChapter()\">Prev</a><a class='btn' style='flex:1;text-align:center' onclick=\"Android.nextChapter()\">Next</a></div>" +
                        "<div style='text-align:center;color:#888;font-family:sans-serif;font-size:13px;margin-top:30px'>Swipe left/right to navigate - Auto-saves</div></div></body></html>"

                runOnUiThread {
                    webView.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null)
                    webView.postDelayed({
                        val s = prefs.getInt("scroll_" + book + "_" + chapter, 0)
                        if (s > 0) webView.scrollTo(0, s)
                    }, 300)
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error: " + e.message, Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun setupSwipe() {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                if (kotlin.math.abs(diffX) > 120 && kotlin.math.abs(vx) > 200) {
                    if (diffX > 0) {
                        val max = chapterCount[currentBook] ?: 150
                        if (currentChapter < max) openChapterView(currentBook, currentChapter + 1)
                    } else {
                        if (currentChapter > 1) openChapterView(currentBook, currentChapter - 1) else showChapterList(currentBook)
                    }
                    return true
                }
                return false
            }
        })
        webView.setOnTouchListener { _, event -> detector.onTouchEvent(event); false }
    }

    private fun showLoading(msg: String) {
        webView.loadData("<html><body style='font-family:sans-serif;padding:40px;text-align:center'><h2>" + msg + "</h2></body></html>", "text/html", "UTF-8")
    }

    inner class JSBridge {
        @android.webkit.JavascriptInterface fun showBooks() { runOnUiThread { showBookList() } }
        @android.webkit.JavascriptInterface fun openBook(book: String) { runOnUiThread { showChapterList(book) } }
        @android.webkit.JavascriptInterface fun openChapter(book: String, ch: Int) { runOnUiThread { openChapterView(book, ch) } }
        @android.webkit.JavascriptInterface fun prevChapter() { runOnUiThread { if (currentChapter > 1) openChapterView(currentBook, currentChapter - 1) else showChapterList(currentBook) } }
        @android.webkit.JavascriptInterface fun nextChapter() { runOnUiThread { val max = chapterCount[currentBook] ?: 150; if (currentChapter < max) openChapterView(currentBook, currentChapter + 1) } }
        @android.webkit.JavascriptInterface fun resume() { val b = prefs.getString("last_book", "Genesis")!!; val c = prefs.getInt("last_chapter", 1); runOnUiThread { openChapterView(b, c) } }
    }

    override fun onPause() {
        super.onPause()
        prefs.edit().putInt("scroll_" + currentBook + "_" + currentChapter, webView.scrollY).apply()
    }
}