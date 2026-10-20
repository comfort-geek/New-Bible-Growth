package com.example.biblecommentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var bar: ProgressBar
    lateinit var status: TextView
    lateinit var homeLayout: LinearLayout
    var db: SQLiteDatabase? = null
    var currentBook = "Genesis"
    var currentChapter = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        bar = findViewById(R.id.progressBar)
        status = findViewById(R.id.status)
        homeLayout = findViewById(R.id.homeLayout)
        
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // SWIPE GESTURE CODE
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || db == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                // Only horizontal swipe
                if (abs(diffX) > abs(diffY) && abs(diffX) > 150 && abs(velocityX) > 100) {
                    if (diffX < 0) {
                        // Swipe LEFT -> Next
                        nextChapter()
                    } else {
                        // Swipe RIGHT -> Back / Prev
                        prevChapter()
                    }
                    return true
                }
                return false
            }
        })

        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        val f = getDatabasePath("commentary_better.db")
        if (f.exists() && f.length() < 5000000) f.delete()
        if (!f.exists()) download() else openDB()
    }

    fun download() {
        Thread {
            try {
                var url = URL("https://archive.org/download/commentary_better/commentary_better.db")
                var conn: HttpURLConnection
                var r = 0
                while (true) {
                    conn = url.openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    conn.connect()
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val loc = conn.getHeaderField("Location") ?: break
                        url = URL(loc)
                        r++
                        if (r > 5) break
                        continue
                    }
                    break
                }
                val total = conn.contentLength
                val input = conn.inputStream
                val outFile = getDatabasePath("commentary_better.db")
                outFile.parentFile?.mkdirs()
                val out = FileOutputStream(outFile)
                val buf = ByteArray(8192)
                var len: Int
                var sum = 0
                while (input.read(buf).also { len = it } != -1) {
                    out.write(buf, 0, len)
                    sum += len
                    val p = if (total > 0) sum * 100 / total else 0
                    runOnUiThread { bar.progress = p; status.text = "Downloading $p%" }
                }
                out.close()
                input.close()
                runOnUiThread { openDB() }
            } catch (e: Exception) {
                getDatabasePath("commentary_better.db").delete()
                runOnUiThread { status.text = "Error ${e.message} Tap to retry"; status.setOnClickListener { download() } }
            }
        }.start()
    }

    fun openDB() {
        try {
            val f = getDatabasePath("commentary_better.db")
            db = SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY)
            runOnUiThread {
                bar.visibility = android.view.View.GONE
                status.visibility = android.view.View.GONE
                homeLayout.visibility = android.view.View.VISIBLE
                loadChapter(currentBook, currentChapter)
            }
        } catch (e: Exception) { runOnUiThread { download() } }
    }

    fun loadChapter(book: String = currentBook, ch: Int = currentChapter) {
        currentBook = book
        currentChapter = ch
        try {
            val c = db!!.rawQuery("SELECT clickable_html, html FROM expanded_verses WHERE book=? AND chapter=? LIMIT 1", arrayOf(book, ch.toString()))
            var html = "<p>Not found for $book $ch</p>"
            if (c.moveToFirst()) {
                val clickable = c.getString(0)
                val plain = c.getString(1)
                html = if (!clickable.isNullOrBlank()) clickable else plain ?: "<p>Empty</p>"
            }
            c.close()
            val page = """
                <html><head><style>
                body{font-family:serif;line-height:1.9;padding:12px;background:#fdfcf8}
                .hint{color:#888;font-size:12px;text-align:center;padding:8px;background:#eee;border-radius:8px;margin-bottom:10px}
                </style></head><body>
                <div class="hint">← Swipe right for Prev | Swipe left for Next →</div>
                <h2>$book $ch</h2><hr>$html
                </body></html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, page, "text/html", "utf-8", null)
        } catch (e: Exception) {
            webView.loadData("<h3>${e.message}</h3>", "text/html", "utf-8")
        }
    }

    fun nextChapter() {
        if (db == null) return
        loadChapter(currentBook, currentChapter + 1)
    }

    fun prevChapter() {
        if (db == null || currentChapter <= 1) return
        loadChapter(currentBook, currentChapter - 1)
    }
}