package com.bible.commentary

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import android.database.sqlite.SQLiteDatabase
import android.app.AlertDialog
import android.widget.ProgressBar
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val DB_URL = "https://archive.org/download/commentary_202610/commentary.db"
    private val DB_NAME = "commentary.db"
    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        val dbFile = File(filesDir, DB_NAME)
        if (!dbFile.exists()) {
            downloadDB(DB_URL, dbFile)
        } else {
            loadApp()
        }
    }

    fun showProgress() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50,50,50,50)
        progressText = TextView(this)
        progressText!!.text = "Downloading 40MB - First time only..."
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar!!.max = 100
        layout.addView(progressText)
        layout.addView(progressBar)
        progressDialog = AlertDialog.Builder(this)
            .setTitle("Please wait")
            .setView(layout)
            .setCancelable(false)
            .create()
        progressDialog!!.show()
    }

    fun downloadDB(urlStr: String, outFile: File) {
        showProgress()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection()
                conn.connect()
                val length = conn.contentLength
                val input = conn.getInputStream()
                val output = FileOutputStream(outFile)
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (length > 0) {
                        val pct = (total * 100 / length).toInt()
                        withContext(Dispatchers.Main) { 
                            progressBar?.progress = pct
                            progressText?.text = "Downloading $pct%"
                        }
                    }
                    output.write(data, 0, count)
                }
                output.close()
                input.close()
                withContext(Dispatchers.Main) { progressDialog?.dismiss(); loadApp() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(this@MainActivity, "Failed: " + e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun loadApp() {
        val html = """
        <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
        body{margin:0;font-family:system-ui;background:#f6f7fb}
        .hdr{background:#111827;color:#fff;padding:16px;text-align:center;position:sticky;top:0}
        .wrap{max-width:700px;margin:0 auto;padding:12px}
        .card{background:#fff;border-radius:14px;padding:14px;margin:10px 0;box-shadow:0 2px 8px rgba(0,0,0,.06)}
        .grid{display:grid;grid-template-columns:1fr 1fr;gap:8px}
        .btn{display:block;padding:14px;background:#fff;border:1px solid #e5e7eb;border-radius:10px;text-align:center;text-decoration:none;color:#111827;font-weight:600}
        </style></head><body>
        <div class="hdr"><h2 style="margin:0">Bible Commentary</h2><p style="margin:4px 0 0;opacity:.7;font-size:12px">Genesis to Revelation - Offline</p></div>
        <div class="wrap" id="app">Loading...</div>
        <script>
        const BOOKS = [["Genesis",50],["Exodus",40],["Leviticus",27],["Numbers",36],["Deuteronomy",34],["Joshua",24],["Judges",21],["Ruth",4],["1 Samuel",31],["2 Samuel",24],["1 Kings",22],["2 Kings",25],["1 Chronicles",29],["2 Chronicles",36],["Ezra",10],["Nehemiah",13],["Esther",10],["Job",42],["Psalms",150],["Proverbs",31],["Ecclesiastes",12],["Song of Solomon",8],["Isaiah",66],["Jeremiah",52],["Lamentations",5],["Ezekiel",48],["Daniel",12],["Hosea",14],["Joel",3],["Amos",9],["Obadiah",1],["Jonah",4],["Micah",7],["Nahum",3],["Habakkuk",3],["Zephaniah",3],["Haggai",2],["Zechariah",14],["Malachi",4],["Matthew",28],["Mark",16],["Luke",24],["John",21],["Acts",28],["Romans",16],["1 Corinthians",16],["2 Corinthians",13],["Galatians",6],["Ephesians",6],["Philippians",4],["Colossians",4],["1 Thessalonians",5],["2 Thessalonians",3],["1 Timothy",6],["2 Timothy",4],["Titus",3],["Philemon",1],["Hebrews",13],["James",5],["1 Peter",5],["2 Peter",3],["1 John",5],["2 John",1],["3 John",1],["Jude",1],["Revelation",22]];
        let curBook="", curCh=1;
        function renderHome(){let h='<div class="card"><div class="grid">';BOOKS.forEach(b=>{h+=`<a class="btn" href="#" onclick="openBook('${'$'}{b[0]}')">${'$'}{b[0]}<br><small>${'$'}{b[1]} ch</small></a>`});h+='</div></div>';document.getElementById('app').innerHTML=h;}
        function openBook(book){curBook=book;let total=BOOKS.find(x=>x[0]==book)[1];let h=`<div class="card"><a href="#" onclick="renderHome()">Back</a><h2>${'$'}{book}</h2><div class="grid">`;for(let i=1;i<=total;i++){h+=`<a class="btn" href="#" onclick="openChap(${'$'}{i})">${'$'}{i}</a>`}h+='</div></div>';document.getElementById('app').innerHTML=h;}
        function openChap(ch){curCh=ch;Android.getCommentary(curBook, ch);}
        function showCommentary(book,ch,htmlContent){let h=`<div class="card"><a href="#" onclick="openBook('${'$'}{book}')">Back ${'$'}{book}</a><h2>${'$'}{book} ${'$'}{ch}</h2><div style="display:flex;justify-content:space-between;margin:10px 0"><a class="btn" href="#" onclick="openChap(${'$'}{Math.max(1,ch-1)})">Prev</a><a class="btn" href="#" onclick="openChap(${'$'}{ch+1})">Next</a></div><div style="line-height:1.8;font-size:16px">${'$'}{htmlContent}</div></div>`;document.getElementById('app').innerHTML=h;window.scrollTo(0,0);}
        renderHome();
        </script></body></html>
        """
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        webView.addJavascriptInterface(JSInterface(), "Android")
    }

    inner class JSInterface {
        @android.webkit.JavascriptInterface
        fun getCommentary(book: String, ch: Int) {
            runOnUiThread {
                try {
                    val dbFile = File(filesDir, DB_NAME)
                    val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                    val cursor = db.rawQuery("SELECT commentary FROM enduring_word WHERE book=? AND chapter=?", arrayOf(book, ch.toString()))
                    var content = "<p>No commentary found.</p>"
                    if (cursor.moveToFirst()) { content = cursor.getString(0) }
                    cursor.close()
                    db.close()
                    val safe = content.replace("`","'").replace("$","\\$")
                    webView.evaluateJavascript("showCommentary('$book', $ch, `$safe`)", null)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}