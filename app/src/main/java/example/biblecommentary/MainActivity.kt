package com.example.biblecommentary

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var status: TextView
    private lateinit var homeLayout: LinearLayout
    private var currentChapter = 1
    private var startX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        status = findViewById(R.id.status)
        homeLayout = findViewById(R.id.homeLayout)
        webView.settings.javaScriptEnabled = true

        Thread {
            for (i in 0..100) {
                Thread.sleep(30)
                runOnUiThread {
                    progressBar.progress = i
                    status.text = "Downloading $i%"
                    if (i == 100) {
                        progressBar.visibility = View.GONE
                        status.visibility = View.GONE
                        homeLayout.visibility = View.VISIBLE
                        loadChapter(1)
                    }
                }
            }
        }.start()

        webView.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> startX = event.x
                android.view.MotionEvent.ACTION_UP -> {
                    if (event.x - startX > 150 && currentChapter > 1) {
                        currentChapter--
                        loadChapter(currentChapter)
                    } else if (startX - event.x > 150) {
                        currentChapter++
                        loadChapter(currentChapter)
                    }
                }
            }
            false
        }
    }

    private fun loadChapter(num: Int) {
        webView.loadData("<h1>Chapter $num</h1><p>Swipe left/right for next chapter - Full screen</p>", "text/html", "UTF-8")
    }
}