package com.example.qrcodegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.qrcodegame.adapters.CommentRecycleViewAdapter;
import com.example.qrcodegame.utils.CurrentUserHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QRCodeCommentActivity extends AppCompatActivity {

    TextView setQRCodeWorth;
    EditText addComments;
    Button backButton;
    Button addButton;

    private final CurrentUserHelper currentUserHelper = CurrentUserHelper.getInstance();
    Map<String, Object> comments = new HashMap<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CommentRecycleViewAdapter adapter;
    RecyclerView recyclerView;
    ArrayList<String> getComments = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_comment);

        setQRCodeWorth = findViewById(R.id.textViewDisplayWorth);
        addComments = findViewById(R.id.add_comments);
        backButton = findViewById(R.id.back_button_comments);
        addButton = findViewById(R.id.addButtonComments);

        getData();

        getSupportActionBar().setTitle("Comments for: "+getIntent().getStringExtra("QRCodeCommentActivity"));
        setQRCodeWorth.setText("Worth: "+getIntent().getStringExtra("Worth"));


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public void storeComments(View view){
        comments.put(currentUserHelper.getUsername(), FieldValue.arrayUnion(currentUserHelper.getUsername()+": "+addComments.getText().toString()));
        db.collection("Comments").document(getIntent().getStringExtra("QRCodeCommentActivity"))
                .set(comments, SetOptions.merge());
        getComments.add(currentUserHelper.getUsername()+": "+addComments.getText().toString());
        adapter.notifyDataSetChanged();
        addComments.setText("");
    }

    public void getData(){
        DocumentReference reference = FirebaseFirestore.getInstance().collection("Comments").document(getIntent().getStringExtra("QRCodeCommentActivity"));
        reference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                Map<String, Object> map = new HashMap<>();
                DocumentSnapshot doc = task.getResult();
                map = doc.getData();
                if(map == null)
                {
                    initRecyclerView();
                    return;
                }
                for(Map.Entry<String, Object> e: map.entrySet()){
                    ArrayList<String> n = (ArrayList<String>) e.getValue();
                    getComments.addAll(n);
                }initRecyclerView();
            }
        });


    }

    private void initRecyclerView(){
        recyclerView = findViewById(R.id.recycle_view_comments);
        adapter = new CommentRecycleViewAdapter(getComments, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }


}