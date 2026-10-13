package com.comfortgeek.newbiblegrowth;
import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
 private TextToSpeech tts;
 private TextView verseText;
 private boolean isTtsReady = false;
 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_main);
  verseText = findViewById(R.id.verseText);
  Button btnPlay = findViewById(R.id.btnPlay);
  Button btnPause = findViewById(R.id.btnPause);
  Button btnStop = findViewById(R.id.btnStop);
  tts = new TextToSpeech(this, this);
  btnPlay.setOnClickListener(v -> {
   if(isTtsReady){
    String text = verseText.getText().toString();
    tts.setSpeechRate(0.9f);
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "verse");
   } else { Toast.makeText(this, "Audio loading...", Toast.LENGTH_SHORT).show(); }
  });
  btnPause.setOnClickListener(v -> { if(tts!= null) tts.stop(); });
  btnStop.setOnClickListener(v -> { if(tts!= null) tts.stop(); });
 }
 @Override
 public void onInit(int status) {
  if(status == TextToSpeech.SUCCESS){
   tts.setLanguage(Locale.ENGLISH);
   tts.setSpeechRate(0.9f);
   isTtsReady = true;
  }
 }
 @Override
 protected void onDestroy() {
  if(tts!= null){ tts.stop(); tts.shutdown(); }
  super.onDestroy();
 }
}
