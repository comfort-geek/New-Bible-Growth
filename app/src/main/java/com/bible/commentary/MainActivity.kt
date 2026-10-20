package com.example.biblecommentary;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    WebView webView;
    ProgressBar bar;
    TextView status;
    LinearLayout gridBooks;
    SQLiteDatabase db;
    final String DB_URL = "https://archive.org/download/commentary_better/commentary_better.db";
    String currentBook = "Genesis";
    int currentChapter = 1;
    GestureDetector gestureDetector;

    String[] BOOKS = {"Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","Song","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1 Cor","2 Cor","Galatians","Ephesians","Philippians","Colossians","1 Thess","2 Thess","1 Tim","2 Tim","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);
        bar = findViewById(R.id.progressBar);
        status = findViewById(R.id.status);
        gridBooks = findViewById(R.id.gridBooks);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "Android");

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                float diff = e2.getX() - e1.getX();
                if (Math.abs(diff) > 120) {
                    if (diff < 0) nextChapter(); else prevChapter();
                    return true;
                }
                return false;
            }
        });
        webView.setOnTouchListener((v, e) -> { gestureDetector.onTouchEvent(e); return false; });

        File f = getDatabasePath("commentary_better.db");
        if (!f.exists()) download();
        else openDB();
    }

    void download() {
        new Thread(() -> {
            try {
                URL url = new URL(DB_URL);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.connect();
                int total = c.getContentLength();
                InputStream in = c.getInputStream();
                File outFile = getDatabasePath("commentary_better.db");
                outFile.getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(outFile);
                byte[] b = new byte[8192];
                int len, dl = 0;
                while ((len = in.read(b)) != -1) {
                    out.write(b, 0, len);
                    dl += len;
                    int p = total > 0 ? (int)(dl*100.0/total) : 0;
                    runOnUiThread(() -> { bar.setProgress(p); status.setText(p+"%"); });
                }
                out.close(); in.close();
                runOnUiThread(this::openDB);
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Error:"+e.getMessage()));
            }
        }).start();
    }

    void openDB() {
        File f = getDatabasePath("commentary_better.db");
        db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        bar.setVisibility(android.view.View.GONE);
        status.setVisibility(android.view.View.GONE);
        setupBooks();
        loadChapter("Genesis", 1);
    }

    void setupBooks() {
        LinearLayout row = null;
        for (int i = 0; i < BOOKS.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setWeightSum(3);
                gridBooks.addView(row);
            }
            Button btn = new Button(this);
            btn.setText(BOOKS[i]);
            btn.setTextSize(10);
            btn.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(2,2,2,2);
            btn.setLayoutParams(lp);
            String book = BOOKS[i];
            btn.setOnClickListener(v -> loadChapter(book, 1));
            row.addView(btn);
        }
    }

    void loadChapter(String book, int ch) {
        currentBook = book;
        currentChapter = ch;
        try {
            Cursor cur = db.rawQuery("SELECT clickable_html, html FROM expanded_verses WHERE book=? AND chapter=? LIMIT 1", new String[]{book, String.valueOf(ch)});
            String html = "<p>Not found for " + book + " " + ch + "</p>";
            if (cur.moveToFirst()) {
                String clickable = cur.getString(0);
                String plain = cur.getString(1);
                html = (clickable != null && !clickable.trim().isEmpty()) ? clickable : plain;
                if (html == null || html.trim().isEmpty()) html = "<p>Empty chapter</p>";
            }
            cur.close();
            String page = "<style>body{font-family:serif;line-height:1.9;padding:12px;background:#fdfcf8} .vref{color:#0d5bd6;background:#e8f0fe;padding:2px 6px;border-radius:6px;font-weight:700;cursor:pointer} h2{border-bottom:2px solid #ddd} button{padding:8px 12px;margin:4px}</style><h2>"+book+" "+ch+"</h2><button onclick=\"Android.prev()\">◀ Prev</button> <button onclick=\"Android.next()\">Next ▶</button><hr>"+html+"<hr><button onclick=\"Android.prev()\">◀ Prev</button> <button onclick=\"Android.next()\">Next ▶</button><script>document.addEventListener('click',e=>{if(e.target.classList.contains('vref')) Android.showVerse(e.target.innerText)})</script>";
            webView.loadDataWithBaseURL(null, page, "text/html", "utf-8", null);
        } catch (Exception e) {
            webView.loadDataWithBaseURL(null, "<h3>Error: "+e.getMessage()+"</h3>", "text/html","utf-8",null);
        }
    }

    void nextChapter() { loadChapter(currentBook, currentChapter+1); }
    void prevChapter() { if(currentChapter>1) loadChapter(currentBook, currentChapter-1); }

    @JavascriptInterface
    public void showVerse(String ref) {
        runOnUiThread(() -> {
            try {
                Cursor c = db.rawQuery("SELECT text FROM bible_verses WHERE ref=?", new String[]{ref.trim()});
                String t = c.moveToFirst() ? c.getString(0) : "Verse not found: " + ref;
                c.close();
                new AlertDialog.Builder(this).setTitle(ref).setMessage(t).setPositiveButton("Close", null).show();
            } catch (Exception e) {
                new AlertDialog.Builder(this).setTitle(ref).setMessage("Error: "+e.getMessage()).setPositiveButton("Close", null).show();
            }
        });
    }

    @JavascriptInterface
    public void next() { runOnUiThread(this::nextChapter); }

    @JavascriptInterface
    public void prev() { runOnUiThread(this::prevChapter); }
}