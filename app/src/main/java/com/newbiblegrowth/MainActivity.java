package com.newbiblegrowth;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
public class MainActivity extends AppCompatActivity {
    public void onCreate(Bundle s) {
        super.onCreate(s);
        WebView w = new WebView(this);
        w.getSettings().setJavaScriptEnabled(true);
        w.loadUrl("https://enduringword.com");
        setContentView(w);
    }
}