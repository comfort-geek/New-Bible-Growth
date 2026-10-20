package com.example.biblecommentary

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val DB_URL = "https://archive.org/download/commentary_better/commentary_better.db"
    private val books = listOf(
        "Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth",
        "1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job",
        "Psalms","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel",
        "Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi",
        "Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians",
        "Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon",
        "Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation"
    )

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var listView: ListView
    private lateinit var contentView: TextView
    private lateinit var topBar: TextView
    
    private var currentBook = "Genesis"
    private var currentChapter = 1
    private var chapterList = listOf<Int>()
    private var db: SQLiteDatabase? = null
    private var tableName = ""
    private var bookCol = ""
    private var chapterCol = ""
    private var textCol = ""
    private var currentMode = "books"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        topBar = TextView(this).apply {
            text = "Bible Commentary"
            textSize = 18f
            setPadding(20, 80, 20, 20)
            setBackgroundColor(Color.parseColor("#0D47A1"))
            setTextColor(Color.WHITE)
        }
        statusText = TextView(this).apply { setPadding(20,20,20,20); textSize=16f }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressDrawable?.setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        listView = ListView(this)
        contentView = TextView(this).apply {
            textSize=18f; setPadding(30,30,30,30); setTextColor(Color.BLACK)
            visibility = android.view.View.GONE
        }
        val scroll = ScrollView(this).apply { addView(contentView) }

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val prev = Button(this).apply { text="Prev" }
        val next = Button(this).apply { text="Next" }
        prev.setOnClickListener{ if(currentChapter>1){ currentChapter--; loadChapter() } }
        next.setOnClickListener{ currentChapter++; loadChapter() }
        btnRow.addView(prev, LinearLayout.LayoutParams(0, -2, 1f))
        btnRow.addView(next, LinearLayout.LayoutParams(0, -2, 1f))

        root.addView(topBar)
        root.addView(statusText)
        root.addView(progressBar, LinearLayout.LayoutParams(-1, 30))
        root.addView(listView, LinearLayout.LayoutParams(-1, -1, 1f))
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1, 1f))
        root.addView(btnRow)

        setContentView(root)

        listView.setOnItemClickListener { _, _, i, _ ->
            if(currentMode=="books"){
                currentBook = books[i]
                showChapters()
            } else {
                currentChapter = chapterList[i]
                loadChapter()
            }
        }
        topBar.setOnClickListener{
            if(currentMode=="chapters") showBooks()
            else if(currentMode=="content") showChapters()
        }

        downloadIfNeeded()
    }

    private fun getDbFile(): File {
        return File(filesDir, "commentary_better.db")
    }

    private fun downloadIfNeeded(){
        Thread{
            try{
                val f = getDbFile()
                if(!f.exists() || f.length() < 1024*1024){
                    f.parentFile?.mkdirs()
                    runOnUiThread{ statusText.text = "Downloading with blue bar..." }
                    val conn = URL(DB_URL).openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    conn.instanceFollowRedirects = true
                    conn.connect()
                    val total = conn.contentLength
                    val inp = conn.inputStream
                    val out = FileOutputStream(f)
                    val buf = ByteArray(8192)
                    var read: Int
                    var done = 0
                    while(inp.read(buf).also{read=it} != -1){
                        out.write(buf,0,read)
                        done+=read
                        val p = if(total>0) done*100/total else 0
                        runOnUiThread{
                            progressBar.progress = p
                            statusText.text = "Downloading with blue bar: $p% ${done/1024/1024}MB"
                        }
                    }
                    out.close(); inp.close()
                }
                runOnUiThread{ openDb() }
            }catch(e:Exception){
                runOnUiThread{ statusText.text = "Download error: ${e.message}" }
            }
        }.start()
    }

    private fun openDb(){
        try{
            val f = getDbFile()
            statusText.text = "Opening DB: ${f.length()/1024/1024}MB"
            db = SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY)
            val c = db!!.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%'", null)
            if(c.moveToFirst()) tableName = c.getString(0)
            c.close()
            val col = db!!.rawQuery("SELECT * FROM $tableName LIMIT 1", null)
            for(i in 0 until col.columnCount){
                val n = col.getColumnName(i).lowercase()
                if(n.contains("book")) bookCol = col.getColumnName(i)
                if(n.contains("chapter") || n=="ch") chapterCol = col.getColumnName(i)
                if(n.contains("comment") || n.contains("text") || n.contains("content")) textCol = col.getColumnName(i)
            }
            if(bookCol.isEmpty()) bookCol = col.getColumnName(0)
            if(chapterCol.isEmpty()) chapterCol = col.getColumnName(1)
            if(textCol.isEmpty()) textCol = col.getColumnName(col.columnCount-1)
            col.close()
            statusText.visibility = android.view.View.GONE
            progressBar.visibility = android.view.View.GONE
            showBooks()
        }catch(e:Exception){
            statusText.text = "DB open error: ${e.message}"
        }
    }

    private fun showBooks(){
        currentMode="books"
        topBar.text="Genesis to Revelation - Select Book"
        listView.visibility = android.view.View.VISIBLE
        contentView.visibility = android.view.View.GONE
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, books)
    }

    private fun showChapters(){
        currentMode="chapters"
        topBar.text="$currentBook - Chapters - Tap top to go back"
        listView.visibility = android.view.View.VISIBLE
        contentView.visibility = android.view.View.GONE
        try{
            val q = "SELECT DISTINCT $chapterCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' ORDER BY CAST($chapterCol AS INTEGER)"
            val cur = db!!.rawQuery(q, null)
            val list = mutableListOf<Int>()
            while(cur.moveToNext()){ try{list.add(cur.getInt(0))}catch(_:Exception){} }
            cur.close()
            chapterList = if(list.isEmpty()) (1..50).toList() else list
        }catch(_:Exception){ chapterList = (1..50).toList() }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chapterList.map{"Chapter $it"})
    }

    private fun loadChapter(){
        currentMode="content"
        topBar.text="$currentBook $currentChapter - Full Screen - Tap top to go back"
        listView.visibility = android.view.View.GONE
        contentView.visibility = android.view.View.VISIBLE
        contentView.text = "Loading $currentBook $currentChapter..."
        Thread{
            try{
                val q = "SELECT $textCol FROM $tableName WHERE $bookCol LIKE '%$currentBook%' AND $chapterCol='$currentChapter'"
                val cur = db!!.rawQuery(q, null)
                var txt = ""
                while(cur.moveToNext()){ txt+=cur.getString(0)+"\n\n" }
                cur.close()
                if(txt.isEmpty()) txt="No commentary for $currentBook $currentChapter"
                runOnUiThread{ contentView.text = txt }
            }catch(e:Exception){ runOnUiThread{ contentView.text = e.message } }
        }.start()
    }
}