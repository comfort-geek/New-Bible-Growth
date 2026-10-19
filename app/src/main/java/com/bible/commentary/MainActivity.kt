package com.bible.commentary
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
class MainActivity : AppCompatActivity() {
var curBook="Genesis";var curChapter=1;var curChCount=50;var mode=0
lateinit var content:LinearLayout;lateinit var dbFile:File
var allTables:List<String> = emptyList()
val DB_URL="https://archive.org/download/commentary_202610/commentary.db"
fun chCount(b:String):Int{
if(b=="Genesis") return 50
if(b=="Exodus") return 40
if(b=="Leviticus") return 27
if(b=="Numbers") return 36
if(b=="Deuteronomy") return 34
if(b=="Joshua") return 24
if(b=="Judges") return 21
if(b=="Ruth") return 4
if(b=="1 Samuel") return 31
if(b=="2 Samuel") return 24
if(b=="1 Kings") return 22
if(b=="2 Kings") return 25
if(b=="1 Chronicles") return 29
if(b=="2 Chronicles") return 36
if(b=="Ezra") return 10
if(b=="Nehemiah") return 13
if(b=="Esther") return 10
if(b=="Job") return 42
if(b=="Psalms") return 150
if(b=="Proverbs") return 31
if(b=="Ecclesiastes") return 12
if(b=="Song of Solomon") return 8
if(b=="Isaiah") return 66
if(b=="Jeremiah") return 52
if(b=="Lamentations") return 5
if(b=="Ezekiel") return 48
if(b=="Daniel") return 12
if(b=="Hosea") return 14
if(b=="Joel") return 3
if(b=="Amos") return 9
if(b=="Obadiah") return 1
if(b=="Jonah") return 4
if(b=="Micah") return 7
if(b=="Nahum") return 3
if(b=="Habakkuk") return 3
if(b=="Zephaniah") return 3
if(b=="Haggai") return 2
if(b=="Zechariah") return 14
if(b=="Malachi") return 4
if(b=="Matthew") return 28
if(b=="Mark") return 16
if(b=="Luke") return 24
if(b=="John") return 21
if(b=="Acts") return 28
if(b=="Romans") return 16
if(b=="1 Corinthians") return 16
if(b=="2 Corinthians") return 13
if(b=="Galatians") return 6
if(b=="Ephesians") return 6
if(b=="Philippians") return 4
if(b=="Colossians") return 4
if(b=="1 Thessalonians") return 5
if(b=="2 Thessalonians") return 3
if(b=="1 Timothy") return 6
if(b=="2 Timothy") return 4
if(b=="Titus") return 3
if(b=="Philemon") return 1
if(b=="Hebrews") return 13
if(b=="James") return 5
if(b=="1 Peter") return 5
if(b=="2 Peter") return 3
if(b=="1 John") return 5
if(b=="2 John") return 1
if(b=="3 John") return 1
if(b=="Jude") return 1
if(b=="Revelation") return 22
return 50
}
fun cleanHtml(s:String):String{val sb=StringBuilder();var inside=false;for(c in s){if(c==60.toChar()){inside=true;continue};if(c==62.toChar()){inside=false;continue};if(!inside) sb.append(c)};return sb.toString()}
override fun onCreate(savedInstanceState: Bundle?) {super.onCreate(savedInstanceState);val root=LinearLayout(this);root.orientation=LinearLayout.VERTICAL;root.setBackgroundColor(0xFFF8F6F1.toInt());val scroll=ScrollView(this);content=LinearLayout(this);content.orientation=LinearLayout.VERTICAL;content.setPadding(30,30,30,30);scroll.addView(content);root.addView(scroll);setContentView(root);dbFile=getDatabasePath("commentary.db");dbFile.parentFile?.mkdirs();if(!dbFile.exists()||dbFile.length()<1000000) downloadDB() else loadDB()}
fun downloadDB(){content.removeAllViews();val tv=TextView(this);tv.text="Downloading 40.2 MB First time only";tv.setTextSize(18f);tv.setPadding(40,80,40,40);val pb=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.max=100;content.addView(tv);content.addView(pb);Thread{try{val conn=URL(DB_URL).openConnection() as HttpURLConnection;conn.connect();val total=conn.contentLength;val input=conn.inputStream;val output=FileOutputStream(dbFile);val buffer=ByteArray(8192);var read:Int;var downloaded=0;while(input.read(buffer).also{read=it}!=-1){output.write(buffer,0,read);downloaded+=read;val percent=if(total>0) downloaded*100/total else 0;runOnUiThread{pb.progress=percent;tv.text="Downloading ${downloaded/1024/1024} MB $percent percent"}};output.close();input.close();runOnUiThread{loadDB()}}catch(e:Exception){runOnUiThread{tv.text="Failed ${e.message}"}}}.start()}
fun loadDB(){try{val db=SQLiteDatabase.openDatabase(dbFile.path,null,SQLiteDatabase.OPEN_READONLY);val cur=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'",null);val list=mutableListOf<String>();while(cur.moveToNext()) list.add(cur.getString(0));cur.close();db.close();allTables=list}catch(_:Exception){};showBooks()}
fun makeGrid(books:List<String>):GridLayout{val grid=GridLayout(this);grid.columnCount=2;for(book in books){val card=LinearLayout(this);card.orientation=LinearLayout.VERTICAL;card.setBackgroundColor(0xFFFFFFFF.toInt());card.setPadding(30,30,30,30);val lp=GridLayout.LayoutParams();lp.width=0;lp.height=GridLayout.LayoutParams.WRAP_CONTENT;lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(12,12,12,12);card.layoutParams=lp;card.isClickable=true;val b=book;card.setOnClickListener{curBook=b;curChCount=chCount(b);showChapters()};val tv=TextView(this);tv.text=b+" "+chCount(b)+" Chapters";tv.setTypeface(tv.typeface,android.graphics.Typeface.BOLD);tv.setTextSize(14f);card.addView(tv);grid.addView(card)};return grid}
fun showBooks(){mode=0;content.removeAllViews();val t1=TextView(this);t1.text="OLD TESTAMENT 39";t1.setTypeface(t1.typeface,android.graphics.Typeface.BOLD);t1.setPadding(12,40,12,12);content.addView(t1);content.addView(makeGrid(listOf("Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel","2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job","Psalms","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai","Zechariah","Malachi")));val t2=TextView(this);t2.text="NEW TESTAMENT 27 TOTAL 66";t2.setTypeface(t2.typeface,android.graphics.Typeface.BOLD);t2.setPadding(12,40,12,12);content.addView(t2);content.addView(makeGrid(listOf("Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians","2 Corinthians","Galatians","Ephesians","Philippians","Colossians","1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon","Hebrews","James","1 Peter","2 Peter","1 John","2 John","3 John","Jude","Revelation")))}
fun showChapters(){mode=1;content.removeAllViews();val backTitle=TextView(this);backTitle.text=curBook;backTitle.setTextSize(20f);backTitle.setTypeface(backTitle.typeface,android.graphics.Typeface.BOLD);backTitle.setPadding(0,20,0,30);content.addView(backTitle);val grid=GridLayout(this);grid.columnCount=4;for(i in 1..curChCount){val card=TextView(this);card.text=i.toString();card.gravity=Gravity.CENTER;card.setBackgroundColor(0xFFFFFFFF.toInt());card.setPadding(20,30,20,30);card.setTypeface(card.typeface,android.graphics.Typeface.BOLD);val lp=GridLayout.LayoutParams();lp.width=0;lp.height=GridLayout.LayoutParams.WRAP_CONTENT;lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(12,12,12,12);card.layoutParams=lp;card.isClickable=true;val num=i;card.setOnClickListener{curChapter=num;showReading()};grid.addView(card)};content.addView(grid)}
fun showReading(){mode=2;content.removeAllViews();val title=TextView(this);title.text=curBook+" Chapter "+curChapter;title.setTextSize(22f);title.setTypeface(title.typeface,android.graphics.Typeface.BOLD);title.setPadding(0,30,0,30);val body=TextView(this);body.setTextSize(19f);body.setPadding(10,10,10,200);content.addView(title);content.addView(body);try{val db=SQLiteDatabase.openDatabase(dbFile.path,null,SQLiteDatabase.OPEN_READONLY);var txt:String?=null;for(table in allTables){if(txt!=null) break;try{val colsCur=db.rawQuery("PRAGMA table_info("+table+")",null);val cols=mutableListOf<String>();while(colsCur.moveToNext()) cols.add(colsCur.getString(1).lowercase());colsCur.close();var textCol="";if(cols.contains("commentary")) textCol="commentary" else if(cols.contains("text")) textCol="text" else if(cols.contains("content")) textCol="content" else if(cols.size>0) textCol=cols[0];if(textCol=="") continue;var bookCol="";var chapCol="";for(c in cols){if(c=="book"||c=="book_name") bookCol=c;if(c=="chapter"||c=="ch"||c=="chapter_number") chapCol=c};if(bookCol!=""&&chapCol!=""){val cur=db.rawQuery("SELECT "+textCol+" FROM "+table+" WHERE "+bookCol+"=? AND "+chapCol+"=? LIMIT 1",arrayOf(curBook,curChapter.toString()));if(cur.moveToFirst()) txt=cur.getString(0);cur.close()}}catch(_:Exception){}};db.close();if(txt==null) txt="Not found for "+curBook+" Chapter "+curChapter;body.text=cleanHtml(txt)}catch(e:Exception){body.text="Error "+e.message}}
override fun onBackPressed(){when(mode){2->showChapters();1->showBooks();else->super.onBackPressed()}}
}