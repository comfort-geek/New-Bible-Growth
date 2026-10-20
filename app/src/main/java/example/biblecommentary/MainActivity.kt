package com.example.biblecommentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var bar: ProgressBar
    lateinit var status: TextView
    lateinit var gridBooks: LinearLayout
    lateinit var downloadLayout: LinearLayout
    lateinit var homeLayout: LinearLayout
    var db: SQLiteDatabase? = null
    val DB_URL = "https://archive.org/download/commentary_better/commentary_better.db"
    var currentBook = "Genesis"
    var currentChapter = 1
    val BOOKS = arrayOf("Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","Song","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1 Cor","2 Cor","Galatians","Ephesians","Philippians","Colossians","1 Thess","2 Thess","1 Tim","2 Tim","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        bar = findViewById(R.id.progressBar)
        status = findViewById(R.id.status)
        gridBooks = findViewById(R.id.gridBooks)
        downloadLayout = findViewById(R.id.downloadLayout)
        homeLayout = findViewById(R.id.homeLayout)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(this, "Android")

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (db == null) return false
                val diff = e2.x - (e1?.x ?: 0f)
                if (kotlin.math.abs(diff) > 120) {
                    if (diff < 0) nextChapter() else prevChapter()
                    return true
                }
                return false
            }
        })
        webView.setOnTouchListener { _, e -> detector.onTouchEvent(e); false }

        val f = getDatabasePath("commentary_better.db")
        if (f.exists() && f.length() < 10*1024*1024) f.delete()
        if (!f.exists()) download() else openDB()
    }

    fun download() {
        Thread {
            try {
                var url = URL(DB_URL)
                var c: HttpURLConnection
                var redirects = 0
                while (true) {
                    c = url.openConnection() as HttpURLConnection
                    c.instanceFollowRedirects = false
                    c.connectTimeout = 15000
                    c.readTimeout = 15000
                    c.setRequestProperty("User-Agent", "BibleCommentaryApp")
                    c.connect()
                    val code = c.responseCode
                    if (code in 300..399) {
                        val newUrl = c.getHeaderField("Location") ?: break
                        url = URL(newUrl)
                        redirects++
                        if (redirects > 5) break
                        continue
                    }
                    break
                }
                if (c.responseCode !in 200..299) throw Exception("HTTP ${c.responseCode}")
                val total = c.contentLength
                val input = c.inputStream
                val outFile = getDatabasePath("commentary_better.db")
                outFile.parentFile?.mkdirs()
                val out = FileOutputStream(outFile)
                val b = ByteArray(8192)
                var len: Int; var dl = 0
                while (input.read(b).also { len = it } != -1) {
                    out.write(b, 0, len); dl += len
                    val p = if (total>0) (dl*100/total).toInt() else 0
                    runOnUiThread { try { bar.progress = p; status.text = "Downloading $p%" } catch (_: Exception) {} }
                }
                out.close(); input.close()
                runOnUiThread { openDB() }
            } catch (e: Exception) {
                getDatabasePath("commentary_better.db").delete()
                runOnUiThread { status.text = "Error: ${e.message}\nTap to retry"; status.setOnClickListener { download() } }
            }
        }.start()
    }

    fun openDB() {
        try {
            val f = getDatabasePath("commentary_better.db")
            db = SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY)
            runOnUiThread {
                downloadLayout.visibility = android.view.View.GONE
                homeLayout.visibility = android.view.View.VISIBLE
                setupBooks()
                loadChapter("Genesis", 1)
            }
        } catch (e: Exception) {
            getDatabasePath("commentary_better.db").delete()
            runOnUiThread { download() }
        }
    }

    fun setupBooks() {
        gridBooks.removeAllViews()
        var row: LinearLayout? = null
        for (i in BOOKS.indices) {
            if (i % 3 == 0) {
                row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 3f }
                gridBooks.addView(row)
            }
            val btn = Button(this).apply {
                text = BOOKS[i]; textSize = 10f; isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(2,2,2,2) }
            }
            val book = BOOKS[i]
            btn.setOnClickListener { loadChapter(book, 1) }
            row?.addView(btn)
        }
    }

    fun loadChapter(book: String, ch: Int) {
        if (db == null) return
        currentBook = book; currentChapter = ch
        try {
            val cur = db!!.rawQuery("SELECT clickable_html, html FROM expanded_verses WHERE book=? AND chapter=? LIMIT 1", arrayOf(book, ch.toString()))
            var html = "<p>Not found for $book $ch</p>"
            if (cur.moveToFirst()) {
                val clickable = cur.getString(0)
                val plain = cur.getString(1)
                html = if (!clickable.isNullOrBlank()) clickable else plain ?: "<p>Empty</p>"
            }
            cur.close()
            val page = """
                <style>body{font-family:serif;line-height:1.9;padding:12px;background:#fdfcf8;color:#222}.vref{color:#0d5bd6;background:#e8f0fe;padding:2px 6px;border-radius:6px;font-weight:700;cursor:pointer}.vref:active{background:#0d5bd6;color:#fff}h2{border-bottom:2px solid #ddd}button{padding:8px 12px;margin:4px;border-radius:8px;border:1px solid #ccc;background:#fff}</style>
                <h2>$book $ch</h2><button onclick="Android.prev()">◀ Prev</button> <button onclick="Android.next()">Next ▶</button><hr>$html<hr><button onclick="Android.prev()">◀ Prev</button> <button onclick="Android.next()">Next ▶</button>
                <script>document.addEventListener('click', function(e){ if(e.target.classList.contains('vref')){ Android.showVerse(e.target.innerText) } });</script>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, page, "text/html", "utf-8", null)
        } catch (e: Exception) {
            webView.loadDataWithBaseURL(null, "<h3>Error: ${e.message}</h3>", "text/html","utf-8",null)
        }
    }

    fun nextChapter() { if (db != null) loadChapter(currentBook, currentChapter+1) }
    fun prevChapter() { if (db != null && currentChapter>1) loadChapter(currentBook, currentChapter-1) }

    @JavascriptInterface
    fun showVerse(ref: String) {
        runOnUiThread {
            try {
                val c = db!!.rawQuery("SELECT text FROM bible_verses WHERE ref=?", arrayOf(ref.trim()))
                val t = if (c.moveToFirst()) c.getString(0) else "Verse not found: $ref"
                c.close()
                AlertDialog.Builder(this).setTitle(ref).setMessage(t).setPositiveButton("Close", null).show()
            } catch (e: Exception) {
                AlertDialog.Builder(this).setTitle(ref).setMessage("Error: ${e.message}").setPositiveButton("Close", null).show()
            }
        }
    }
    @JavascriptInterface fun next() { runOnUiThread { nextChapter() } }
    @JavascriptInterface fun prev() { runOnUiThread { prevChapter() } }
}