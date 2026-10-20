package com.example.biblecommentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var bar: ProgressBar
    lateinit var status: TextView
    lateinit var homeLayout: LinearLayout
    var db: SQLiteDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        bar = findViewById(R.id.progressBar)
        status = findViewById(R.id.status)
        homeLayout = findViewById(R.id.homeLayout)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
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
                loadChapter()
            }
        } catch (e: Exception) { runOnUiThread { download() } }
    }

    fun loadChapter() {
        try {
            val c = db!!.rawQuery("SELECT clickable_html FROM expanded_verses LIMIT 1", null)
            var html = "DB Loaded"
            if (c.moveToFirst()) html = c.getString(0)
            c.close()
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } catch (e: Exception) {
            webView.loadData("<h3>${e.message}</h3>", "text/html", "utf-8")
        }
    }
}