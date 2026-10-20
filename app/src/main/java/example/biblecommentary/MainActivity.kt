package com.example.biblecommentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private val DB_NAME = "commentary_better.db"
    private val DB_URL = "https://archive.org/download/commentary_better/commentary_better.db"
    
    private val books = listOf("Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians","Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation")
    
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var listView: ListView
    private lateinit var contentView: TextView
    private lateinit var topBar: TextView
    
    private var currentMode = "books" // books, chapters, content
    private var currentBook = ""
    private var currentChapter = 1
    private var chapterList = listOf<Int>()
    private var db: SQLiteDatabase? = null
    private var tableName = "commentary"
    private var bookCol = ""
    private var chapterCol = ""
    private var textCol = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        
        topBar = TextView(this).apply { text="Bible Commentary"; textSize=18f; setPadding(20,40,20,20); setBackgroundColor(Color.parseColor("#0D47A1")); setTextColor(Color.WHITE) }
        statusText = TextView(this).apply { text="Initializing..."; textSize=16f; setPadding(20,20,20,20); visibility=View.GONE }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate=false; max=100; progress=0
            progressDrawable?.setColorFilter(Color.parseColor("#2196F3"), android.graphics.PorterDuff.Mode.SRC_IN)
            visibility=View.GONE
        }
        listView = ListView(this)
        contentView = TextView(this).apply {
            textSize=18f; setPadding(30,30,30,30); visibility=View.GONE
            setTextColor(Color.BLACK); setLineSpacing(10f,1.3f)
        }
        
        root.addView(topBar)
        root.addView(statusText)
        root.addView(progressBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20))
        root.addView(listView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        root.addView(contentView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        
        setContentView(root)
        
        // Swipe detection for next/previous
        val gesture = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener(){
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if(currentMode!="content") return false
                if(abs(e2.x - (e1?.x?:0f)) > 150 && abs(vx) > 500){
                    if(e2.x < (e1?.x?:0f)){ nextChapter() } else { prevChapter() }
                    return true
                }
                return false
            }
        })
        contentView.setOnTouchListener{ _, e -> gesture.onTouchEvent(e); true }
        
        listView.setOnItemClickListener{ _, _, pos, _ ->
            when(currentMode){
                "books" -> { currentBook = books[pos]; openChapters() }
                "chapters" -> { currentChapter = chapterList[pos]; openContent() }
            }
        }
        
        topBar.setOnClickListener{
            when(currentMode){
                "chapters" -> showBooks()
                "content" -> openChapters()
            }
        }

        startDownload()
    }
    
    private fun startDownload(){
        Thread{
            try{
                val dbFile = File(getDatabasePath(DB_NAME).path)
                if(!dbFile.exists() || dbFile.length() < 10000){
                    dbFile.parentFile?.mkdirs()
                    runOnUiThread{
                        statusText.visibility=View.VISIBLE
                        progressBar.visibility=View.VISIBLE
                        statusText.text="Downloading Commentary..."
                    }
                    val conn = URL(DB_URL).openConnection() as HttpURLConnection
                    conn.connect()
                    val total = conn.contentLength
                    val input = conn.inputStream
                    val output = FileOutputStream(dbFile)
                    val buffer = ByteArray(8192)
                    var downloaded = 0
                    var len: Int
                    while(input.read(buffer).also{len=it} != -1){
                        output.write(buffer,0,len)
                        downloaded+=len
                        val pct = if(total>0) (downloaded*100/total) else 0
                        runOnUiThread{ progressBar.progress=pct; statusText.text="Downloading... $pct% (${downloaded/1024/1024} MB)" }
                    }
                    output.close(); input.close()
                }
                openDB()
            }catch(e:Exception){
                runOnUiThread{ statusText.text="Error: ${e.message}"; statusText.visibility=View.VISIBLE }
            }
        }.start()
    }
    
    private fun openDB(){
        try{
            val dbFile = File(getDatabasePath(DB_NAME).path)
            db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db!!.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            while(cursor.moveToNext()){
                val t = cursor.getString(0)
                if(t!="android_metadata"){ tableName=t; break }
            }
            cursor.close()
            val colCur = db!!.rawQuery("SELECT * FROM $tableName LIMIT 1", null)
            for(i in 0 until colCur.columnCount){
                val n = colCur.getColumnName(i).lowercase()
                if(n.contains("book")) bookCol = colCur.getColumnName(i)
                if(n.contains("chapter") || n=="ch") chapterCol = colCur.getColumnName(i)
                if(n.contains("text") || n.contains("comment") || n.contains("content") || n.contains("verse") || n.contains("data")) textCol = colCur.getColumnName(i)
            }
            if(bookCol.isEmpty()) bookCol = colCur.getColumnName(0)
            if(chapterCol.isEmpty() && colCur.columnCount>1) chapterCol = colCur.getColumnName(1)
            if(textCol.isEmpty()) textCol = colCur.getColumnName(colCur.columnCount-1)
            colCur.close()
            
            runOnUiThread{
                statusText.visibility=View.GONE
                progressBar.visibility=View.GONE
                showBooks()
            }
        }catch(e:Exception){
            runOnUiThread{ statusText.text="DB Error: ${e.message}" }
        }
    }
    
    private fun showBooks(){
        currentMode="books"
        topBar.text="Select Book - Genesis to Revelation"
        contentView.visibility=View.GONE
        listView.visibility=View.VISIBLE
        listView.adapter=ArrayAdapter(this, android.R.layout.simple_list_item_1, books)
    }
    
    private fun openChapters(){
        currentMode="chapters"
        topBar.text="$currentBook - Select Chapter (Tap to go back)"
        try{
            val q = if(bookCol.isNotEmpty()) "SELECT DISTINCT $chapterCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' ORDER BY CAST($chapterCol AS INTEGER)" else "SELECT DISTINCT $chapterCol FROM $tableName LIMIT 50"
            val cur = db!!.rawQuery(q, null)
            val chs = mutableListOf<Int>()
            while(cur.moveToNext()){
                try{ chs.add(cur.getString(0).toInt()) }catch(_:Exception){}
            }
            cur.close()
            chapterList = if(chs.isEmpty()) (1..50).toList() else chs.sorted()
            val display = chapterList.map{ "Chapter $it" }
            listView.adapter=ArrayAdapter(this, android.R.layout.simple_list_item_1, display)
        }catch(e:Exception){
            chapterList = (1..50).toList()
            listView.adapter=ArrayAdapter(this, android.R.layout.simple_list_item_1, chapterList.map{ "Chapter $it" })
        }
    }
    
    private fun openContent(){
        currentMode="content"
        topBar.text="$currentBook $currentChapter - Swipe ←→ Next/Prev (Tap top to go back)"
        listView.visibility=View.GONE
        contentView.visibility=View.VISIBLE
        contentView.text="Loading..."
        Thread{
            try{
                val q = "SELECT $textCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' AND $chapterCol='$currentChapter' LIMIT 100"
                val cur = db!!.rawQuery(q, null)
                var fullText = ""
                while(cur.moveToNext()){
                    fullText += cur.getString(0) + "\n\n"
                }
                cur.close()
                if(fullText.isEmpty()){
                    val q2 = "SELECT $textCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' LIMIT 5"
                    val cur2 = db!!.rawQuery(q2, null)
                    while(cur2.moveToNext()){ fullText += cur2.getString(0) + "\n\n" }
                    cur2.close()
                }
                if(fullText.isEmpty()) fullText="No commentary found for $currentBook Chapter $currentChapter in this DB. Table: $tableName Cols: $bookCol, $chapterCol, $textCol"
                val finalText = fullText
                runOnUiThread{ contentView.text = finalText }
            }catch(e:Exception){
                runOnUiThread{ contentView.text="Error: ${e.message}" }
            }
        }.start()
    }
    
    private fun nextChapter(){
        currentChapter++
        if(!chapterList.contains(currentChapter) && chapterList.isNotEmpty() && currentChapter>chapterList.max()){ currentChapter=chapterList.max() }
        openContent()
    }
    private fun prevChapter(){
        currentChapter--
        if(currentChapter<1) currentChapter=1
        openContent()
    }
}