package com.newbiblegrowth;
import android.content.Intent; import android.database.Cursor; import android.os.Bundle; import android.widget.EditText; import androidx.appcompat.app.AppCompatActivity; import androidx.recyclerview.widget.*;
public class MainActivity extends AppCompatActivity {
    DatabaseHelper db;
    protected void onCreate(Bundle s){
        super.onCreate(s); setContentView(R.layout.activity_main);
        db=new DatabaseHelper(this);
        RecyclerView list=findViewById(R.id.bookList); list.setLayoutManager(new LinearLayoutManager(this));
        // 66 books
        RecyclerView.Adapter adapter=new RecyclerView.Adapter(){
            public int getItemCount(){return 66;}
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup p,int t){return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.item_book,p,false)){ }{};}
            public void onBindViewHolder(RecyclerView.ViewHolder h,int pos){int bookId=pos+1; android.widget.TextView tv=h.itemView.findViewById(R.id.bookName); tv.setText(bookId+". "+db.getBookName(bookId)); h.itemView.setOnClickListener(v->{Intent i=new Intent(MainActivity.this, CommentaryActivity.class); i.putExtra("book_id",bookId); i.putExtra("chapter",1); startActivity(i);});}
        };
        list.setAdapter(adapter);
        EditText search=findViewById(R.id.search);
        search.setOnEditorActionListener((v,a,e)->{String q=v.getText().toString(); if(!q.isEmpty()){Cursor c=db.search(q); if(c.moveToFirst()){Intent i=new Intent(MainActivity.this, CommentaryActivity.class); i.putExtra("book_id",c.getInt(0)); i.putExtra("chapter",c.getInt(1)); startActivity(i);}} return true;});
    }
}