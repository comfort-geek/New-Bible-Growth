package com.bible.commentary;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String DB_URL = "https://archive.org/download/commentary_better/commentary_better.db";
    private static final String DB_NAME = "commentary_better.db";
    
    ProgressBar progressBar;
    TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);
        status = findViewById(R.id.status);

        File dbFile = getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            // FIRST OPEN - Download from Archive.org cloud
            downloadDB();
        } else {
            openApp();
        }
    }

    void downloadDB() {
        new Thread(() -> {
            try {
                URL url = new URL(DB_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                int total = conn.getContentLength();
                InputStream in = conn.getInputStream();
                File outFile = getDatabasePath(DB_NAME);
                outFile.getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(outFile);
                byte[] buffer = new byte[8192];
                int len, downloaded = 0;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    downloaded += len;
                    int pct = (int) (downloaded * 100.0 / total);
                    runOnUiThread(() -> {
                        progressBar.setProgress(pct);
                        status.setText("Downloading from Archive.org: " + pct + "%");
                    });
                }
                out.close(); in.close();
                runOnUiThread(this::openApp);
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    void openApp() {
        // Your existing code to load commentary
        // db path = getDatabasePath(DB_NAME).getPath()
        // Example: WebView loads HTML with clickable verses using bible_verses table
        status.setText("Ready - Offline");
        // startActivity(new Intent(this, CommentaryActivity.class));
    }
}