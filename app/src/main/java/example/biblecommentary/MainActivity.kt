package com.example.biblecommentary

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "Bible Commentary\n\nApp is working!\nVersion 1.1.0"
        textView.textSize = 22f
        textView.setPadding(60, 300, 60, 60)
        textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        
        setContentView(textView)
    }
}