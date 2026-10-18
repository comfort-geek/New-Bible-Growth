package com.bible.commentary

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        setContentView(webView)
        val html = "<html><body style='font-family:sans-serif;text-align:center;padding:40px'><h1>Bible Commentary</h1><p>App is working!</p></body></html>"
        webView.loadData(html, "text/html", "utf-8")
    }
}