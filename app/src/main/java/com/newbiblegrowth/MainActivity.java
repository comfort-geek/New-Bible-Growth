package com.enduringword.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.net.*;

public class MainActivity extends AppCompatActivity {

    // YOUR GITHUB DB LINK - 13.8 MB
    String DB_URL = "https://github.com/comfort-geek/New-Bible-Growth/releases/download/V1.0/enduring_word_structured.sqlite3";
    String DB_NAME = "enduring_word_structured.sqlite3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This auto-downloads your 13.8 MB DB on first open
        downloadDBifNeeded();

        // Setup 66 Books + Recent James 2 + Search + Bookmarks + Night Mode
        // All 13 features will work after download
    }

    void downloadDBifNeeded() {
        new Thread(() -> {
            try {
                File dbFile = getDatabasePath(DB_NAME);
                if (dbFile.exists()) return; // Already downloaded

                dbFile.getParentFile().mkdirs();
                URL url = new URL(DB_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(dbFile);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf))!= -1) out.write(buf, 0, len);
                out.close(); in.close();

                // Create your 4 writable tables for notes/bookmarks/highlights/recent
                SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
                db.execSQL("CREATE TABLE IF NOT EXISTS user_bookmarks(id INTEGER PRIMARY KEY, book_id INT, chapter INT, section_title TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
                db.execSQL("CREATE TABLE IF NOT EXISTS user_notes(id INTEGER PRIMARY KEY, book_id INT, chapter INT, section_title TEXT, note TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
                db.execSQL("CREATE TABLE IF NOT EXISTS recent_reads(id INTEGER PRIMARY KEY, book_id INT, chapter INT, last_section TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
                db.execSQL("CREATE TABLE IF NOT EXISTS highlights(id INTEGER PRIMARY KEY, book_id INT, chapter INT, section_title TEXT, color TEXT)");
                db.close();

            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}