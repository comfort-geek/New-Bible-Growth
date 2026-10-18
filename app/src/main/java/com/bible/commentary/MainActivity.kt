package com.bible.commentary

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.webkit.JavascriptInterface
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
    private lateinit var prefs: SharedPreferences
    private var currentBook = "Genesis"
    private var currentChapter = 1

    private val booksOrder = listOf(
        "Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel",
        "1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs",
        "Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos",
        "Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi",
        "Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians",
        "Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon",
        "Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("bible_prefs", Context.MODE_PRIVATE)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(this, "Android")

        val dbFile = File(filesDir, "commentary.db")
        if (!dbFile.exists()) {
            showDownloading()
            thread {
                try {
                    val dbUrl = "https://github.com/YOUR_USERNAME/YOUR_REPO/releases/download/v1.0/commentary.db"
                    URL(dbUrl).openStream().use { input ->
                        FileOutputStream(dbFile).use { output -> input.copyTo(output) }
                    }
                    runOnUiThread { openBooksList() }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, "DB download failed: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            }
        } else {
            val lastBook = prefs.getString("last_book", null)
            if (lastBook != null) openChapterView(lastBook, prefs.getInt("last_chapter", 1))
            else openBooksList()
        }
    }

    private fun showDownloading() {
        webView.loadData("<html><body style='font-family:sans-serif;text-align:center;padding:50px'><h2>Downloading Bible Commentary...</h2><p>First time only</p></body></html>", "text/html", "UTF-8")
    }

    @JavascriptInterface fun openBooks() { runOnUiThread { openBooksList() } }
    @JavascriptInterface fun openBook(book: String) { runOnUiThread { openChaptersList(book) } }
    @JavascriptInterface fun openChapter(book: String, chapter: Int) { runOnUiThread { openChapterView(book, chapter) } }

    @JavascriptInterface
    fun nextChapter() {
        runOnUiThread {
            webView.evaluateJavascript("window.scrollY") { y ->
                prefs.edit().putInt("scroll_${currentBook}_$currentChapter", y.toIntOrNull() ?: 0).apply()
            }
            thread {
                try {
                    val db = SQLiteDatabase.openDatabase(File(filesDir, "commentary.db").path, null, SQLiteDatabase.OPEN_READONLY)
                    val cur = db.rawQuery("SELECT MAX(chapter) FROM enduring_word WHERE book=?", arrayOf(currentBook))
                    cur.moveToFirst(); val maxChap = cur.getInt(0); cur.close(); db.close()
                    runOnUiThread {
                        if (currentChapter < maxChap) openChapterView(currentBook, currentChapter + 1)
                        else {
                            val idx = booksOrder.indexOf(currentBook)
                            if (idx < booksOrder.size - 1) openChapterView(booksOrder[idx + 1], 1)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    @JavascriptInterface
    fun prevChapter() {
        runOnUiThread {
            if (currentChapter > 1) openChapterView(currentBook, currentChapter - 1)
            else {
                val idx = booksOrder.indexOf(currentBook)
                if (idx > 0) {
                    val prevBook = booksOrder[idx - 1]
                    thread {
                        val db = SQLiteDatabase.openDatabase(File(filesDir, "commentary.db").path, null, SQLiteDatabase.OPEN_READONLY)
                        val cur = db.rawQuery("SELECT MAX(chapter) FROM enduring_word WHERE book=?", arrayOf(prevBook))
                        cur.moveToFirst(); val maxChap = cur.getInt(0); cur.close(); db.close()
                        runOnUiThread { openChapterView(prevBook, maxChap) }
                    }
                }
            }
        }
    }

    private fun openBooksList() {
        thread {
            try {
                val db = SQLiteDatabase.openDatabase(File(filesDir, "commentary.db").path, null, SQLiteDatabase.OPEN_READONLY)
                val cur = db.rawQuery("SELECT DISTINCT book FROM enduring_word", null)
                val found = mutableListOf<String>()
                while (cur.moveToNext()) found.add(cur.getString(0))
                cur.close(); db.close()
                val ordered = booksOrder.filter { it in found }
                val html = buildString {
                    append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{font-family:sans-serif;margin:0;background:#f8f9fa}.header{padding:16px;background:white;box-shadow:0 2px 8px rgba(0,0,0,.08);position:sticky;top:0} .grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;padding:16px} .book{padding:18px;background:white;border-radius:12px;text-align:center;box-shadow:0 1px 4px rgba(0,0,0,.08);font-weight:600}</style></head><body><div class='header'><h2 style='margin:0'>Bible Commentary - 66 Books</h2></div><div class='grid'>")
                    for (b in ordered) append("<div class='book' onclick=\"Android.openBook('$b')\">$b</div>")
                    append("</div></body></html>")
                }
                runOnUiThread { webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun openChaptersList(book: String) {
        currentBook = book
        thread {
            val db = SQLiteDatabase.openDatabase(File(filesDir, "commentary.db").path, null, SQLiteDatabase.OPEN_READONLY)
            val cur = db.rawQuery("SELECT chapter FROM enduring_word WHERE book=? ORDER BY CAST(chapter AS INTEGER)", arrayOf(book))
            val chapters = mutableListOf<Int>()
            while (cur.moveToNext()) chapters.add(cur.getInt(0))
            cur.close(); db.close()
            val html = buildString {
                append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{font-family:sans-serif;margin:0;background:#f8f9fa}.top{position:sticky;top:0;background:white;display:flex;gap:12px;align-items:center;padding:12px 16px;box-shadow:0 2px 8px rgba(0,0,0,.08)} .btn{padding:8px 14px;background:#f1f2f4;border-radius:8px} .grid{display:grid;grid-template-columns:repeat(5,1fr);gap:10px;padding:16px} .ch{padding:16px;background:white;border-radius:10px;text-align:center;box-shadow:0 1px 3px rgba(0,0,0,.08)}</style></head><body>")
                append("<div class='top'><span class='btn' onclick=\"Android.openBooks()\">Books</span><b>$book</b></div><div class='grid'>")
                for (ch in chapters) append("<div class='ch' onclick=\"Android.openChapter('$book',$ch)\">$ch</div>")
                append("</div></body></html>")
            }
            runOnUiThread { webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
        }
    }

    private fun openChapterView(book: String, chapter: Int) {
        currentBook = book; currentChapter = chapter
        prefs.edit().putString("last_book", book).putInt("last_chapter", chapter).apply()
        thread {
            try {
                val dbFile = File(filesDir, "commentary.db")
                val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery("SELECT commentary FROM enduring_word WHERE book=? AND chapter=?", arrayOf(book, chapter.toString()))
                var raw = "Not found: $book $chapter"
                if (cursor.moveToFirst()) raw = cursor.getString(0)
                cursor.close(); db.close()

                var html = raw
                // FIX MOJIBAKE - simple safe replaces, no broken quotes
                html = html.replace("\u00e2\u20ac\u2122", "'")
                html = html.replace("\u00e2\u20ac\u201c", "\"")
                html = html.replace("\u00e2\u20ac", "\"")
                html = html.replace("\u00e2\u20ac\u201d", "\"")
                html = html.replace("\u00e2\u20ac\u201c", "\"")
                html = html.replace("\u00c2", "")
                html = html.replace("&#8217;", "'").replace("&#8220;", "\"").replace("&#8221;", "\"").replace("&#8211;", "-").replace("&#8212;", "-")

                // REMOVE JUNK - your rules
                html = html.replace(Regex("(?is)High Points.*?(?=<h1|<h2|<h3)"), "")
                html = html.replace(Regex("(?is)<h3[^>]*>.*?Videos for.*?</h3>.*?(<p><a href=.*?</p>)+"), "")
                html = html.replace(Regex("(?is)<h[2-4][^>]*>.*?AI Summary.*"), "")
                html = html.replace(Regex("(?i)AI generated summary of David Guzik.*"), "")

                for (m in listOf("Related Commentary", "Referenced By", "Cited Authors")) {
                    val idx = html.indexOf(m, ignoreCase = true)
                    if (idx != -1) { html = html.substring(0, idx); break }
                }

                val page = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{font-family:Georgia,serif;line-height:1.85;font-size:17px;color:#222;background:#fefefe;margin:0} .top{position:sticky;top:0;background:white;display:flex;justify-content:space-between;align-items:center;padding:12px 16px;box-shadow:0 2px 10px rgba(0,0,0,0.08)} .btn{padding:8px 14px;background:#f1f2f4;border-radius:8px;color:#333;font-family:sans-serif;font-size:14px} .content{max-width:720px;margin:0 auto;padding:20px 18px 40px} h2{border-bottom:2px solid #eee;padding-bottom:10px} blockquote{border-left:4px solid #2a6db0;margin:18px 0;padding:10px 16px;background:#f7f9ff}</style></head><body><div class='top'><span class='btn' onclick=\"Android.openBook('$book')\">$book</span><span style='font-family:sans-serif;font-weight:700'>$book $chapter</span><span class='btn' onclick=\"Android.nextChapter()\">Next</span></div><div class='content'><h2>$book $chapter</h2>" + html + "<div style='display:flex;gap:12px;margin-top:30px'><span class='btn' style='flex:1;text-align:center' onclick=\"Android.prevChapter()\">Prev</span><span class='btn' style='flex:1;text-align:center' onclick=\"Android.nextChapter()\">Next</span></div></div></body></html>"

                runOnUiThread {
                    webView.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null)
                    webView.postDelayed({
                        val s = prefs.getInt("scroll_${book}_$chapter", 0)
                        if (s > 0) webView.scrollTo(0, s)
                    }, 350)
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            webView.evaluateJavascript("window.scrollY") { y ->
                prefs.edit().putInt("scroll_${currentBook}_$currentChapter", y.toIntOrNull() ?: 0).apply()
            }
        } catch (_: Exception) {}
    }
}