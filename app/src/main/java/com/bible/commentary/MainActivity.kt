package com.bible.commentary

import android.app.AlertDialog
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.URL

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
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(JSInterface(), "Android")
        setContentView(webView)
        
        val dbFile = File(filesDir, DB_NAME)
        if (!dbFile.exists()) {
            downloadDB(dbFile)
        } else {
            loadApp()
        }
    }

    private fun showProgress() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50,50,50,50)
        progressText = TextView(this)
        progressText!!.text = "Downloading - First time only..."
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar!!.max = 100
        layout.addView(progressText)
        layout.addView(progressBar)
        progressDialog = AlertDialog.Builder(this).setTitle("Bible Commentary").setView(layout).setCancelable(false).create()
        progressDialog!!.show()
    }

    private fun downloadDB(outFile: File) {
        showProgress()
        Thread {
            try {
                val url = URL(DB_URL)
                val conn = url.openConnection()
                conn.connect()
                val length = conn.contentLength
                val input = conn.getInputStream()
                val output = FileOutputStream(outFile)
                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (length > 0) {
                        val pct = (total * 100 / length).toInt()
                        runOnUiThread { 
                            progressBar?.progress = pct
                            progressText?.text = "Downloading " + pct + "% - Please wait"
                        }
                    }
                    output.write(data, 0, count)
                }
                output.close()
                input.close()
                runOnUiThread { progressDialog?.dismiss(); loadApp() }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog?.dismiss()
                    Toast.makeText(this@MainActivity, "Download failed: " + e.message, Toast.LENGTH_LONG).show()
                    loadApp()
                }
            }
        }.start()
    }

    private fun loadApp() {
        val html = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
        "<style>body{margin:0;font-family:system-ui;background:#f6f7fb}.hdr{background:#111827;color:#fff;padding:16px;text-align:center;position:sticky;top:0}.wrap{max-width:700px;margin:0 auto;padding:12px}.card{background:#fff;border-radius:14px;padding:14px;margin:10px 0;box-shadow:0 2px 8px rgba(0,0,0,.06)}.grid{display:grid;grid-template-columns:1fr 1fr;gap:8px}.btn{display:block;padding:14px;background:#fff;border:1px solid #e5e7eb;border-radius:10px;text-align:center;text-decoration:none;color:#111827;font-weight:600}</style></head><body>" +
        "<div class=\"hdr\"><h2 style=\"margin:0\">Bible Commentary</h2><p style=\"margin:4px 0 0;opacity:.7;font-size:12px\">Genesis to Revelation - Offline</p></div>" +
        "<div class=\"wrap\" id=\"app\">Loading...</div>" +
        "<script>" +
        "const BOOKS = [[\"Genesis\",50],[\"Exodus\",40],[\"Leviticus\",27],[\"Numbers\",36],[\"Deuteronomy\",34],[\"Joshua\",24],[\"Judges\",21],[\"Ruth\",4],[\"1 Samuel\",31],[\"2 Samuel\",24],[\"1 Kings\",22],[\"2 Kings\",25],[\"1 Chronicles\",29],[\"2 Chronicles\",36],[\"Ezra\",10],[\"Nehemiah\",13],[\"Esther\",10],[\"Job\",42],[\"Psalms\",150],[\"Proverbs\",31],[\"Ecclesiastes\",12],[\"Song of Solomon\",8],[\"Isaiah\",66],[\"Jeremiah\",52],[\"Lamentations\",5],[\"Ezekiel\",48],[\"Daniel\",12],[\"Hosea\",14],[\"Joel\",3],[\"Amos\",9],[\"Obadiah\",1],[\"Jonah\",4],[\"Micah\",7],[\"Nahum\",3],[\"Habakkuk\",3],[\"Zephaniah\",3],[\"Haggai\",2],[\"Zechariah\",14],[\"Malachi\",4],[\"Matthew\",28],[\"Mark\",16],[\"Luke\",24],[\"John\",21],[\"Acts\",28],[\"Romans\",16],[\"1 Corinthians\",16],[\"2 Corinthians\",13],[\"Galatians\",6],[\"Ephesians\",6],[\"Philippians\",4],[\"Colossians\",4],[\"1 Thessalonians\",5],[\"2 Thessalonians\",3],[\"1 Timothy\",6],[\"2 Timothy\",4],[\"Titus\",3],[\"Philemon\",1],[\"Hebrews\",13],[\"James\",5],[\"1 Peter\",5],[\"2 Peter\",3],[\"1 John\",5],[\"2 John\",1],[\"3 John\",1],[\"Jude\",1],[\"Revelation\",22]];" +
        "let curBook=\"\"; let curCh=1;" +
        "function renderHome(){let h='<div class=\"card\"><div class=\"grid\">';BOOKS.forEach(b=>{h+='<a class=\"btn\" href=\"#\" onclick=\"openBook(\\''+b[0]+'\\')\">