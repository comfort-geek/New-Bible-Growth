package com.newbiblegrowth;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
public class MainActivity extends Activity {
    public void onCreate(Bundle s) {
        super.onCreate(s);
        WebView w = new WebView(this);
        w.getSettings().setJavaScriptEnabled(true);
        w.loadUrl("https://enduringword.com");
        setContentView(w);
    }
}