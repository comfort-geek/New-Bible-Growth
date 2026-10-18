package com.yourapp.biblecommentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var dbFile: File

    // CHANGE THIS TO YOUR RELEASE URL
    private val DB_URL = "https://github.com/YOURNAME/YOURREPO/releases/download/v1.0/commentary.db"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        dbFile = File(getDatabasePath("commentary.db").path)

        // If DB doesn't exist, download then clean it
        if (!dbFile.exists()) {
            Thread {
                try {
                    downloadDatabase()
                    cleanDatabaseOnFirstInstall(dbFile) // <-- CLEAN HERE
                    runOnUiThread { openChapterView("Genesis", 1) }
                } catch (e: Exception) {
                    Log.e("APP", "Download failed", e)
                }
            }.start()
        } else {
            // Already exists, but ensure it was cleaned once
            cleanDatabaseOnFirstInstall(dbFile)
            openChapterView("Genesis", 1)
        }
    }

    private fun downloadDatabase() {
        dbFile.parentFile?.mkdirs()
        URL(DB_URL).openStream().use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    // === THIS CLEANS THE WHOLE DB ON FIRST INSTALL - 1189 CHAPTERS ===
    private fun cleanDatabaseOnFirstInstall(file: File) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("db_cleaned_v3", false)) return
        if (!file.exists()) return

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            val cursor = db.rawQuery("SELECT rowid, commentary FROM enduring_word", null)
            db.beginTransaction()

            while (cursor.moveToNext()) {
                val rowId = cursor.getLong(0)
                var text = cursor.getString(1) ?: continue

                // 1. Fix double UTF-8  Genesis 1 â€“ -> Genesis 1 –
                repeat(5) {
                    if (!text.contains('â') && !text.contains('Â')) return@repeat
                    try {
                        val decoded = String(text.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                        if (decoded.count { c -> c == 'â' } < text.count { c -> c == 'â' }) {
                            text = decoded
                        }
                    } catch (_: Exception) {}
                }

                // 2. Manual replaces
                text = text.replace("â€“", "–")
                    .replace("â—", "—")
                    .replace("â€”", "—")
                    .replace("â€™", "'")
                    .replace("â€˜", "'")
                    .replace("â€œ", "\"")
                    .replace("â€", "\"")
                    .replace("â€¦", "...")
                    .replace("Â", " ")

                // 3. Decode entities  God&#8217;s -> God's
                text = text.replace("&#8217;", "'")
                    .replace("&#8211;", "–")
                    .replace("&#8212;", "—")
                    .replace("&#8220;", "\"")
                    .replace("&#8221;", "\"")
                    .replace("&amp;", "&")
                    .replace("&#038;", "&")

                // 4. Remove Videos for Genesis and High Points
                text = text.replace(Regex("(?is)<h3[^>]*>.*?Videos for.*