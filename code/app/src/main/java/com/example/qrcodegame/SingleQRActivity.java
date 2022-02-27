 package com.example.qrcodegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.qrcodegame.adapters.QrUsernameAdapter;
import com.example.qrcodegame.models.QRCode;
import com.example.qrcodegame.utils.CurrentUserHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

 public class SingleQRActivity extends AppCompatActivity implements OnMapReadyCallback {

     private ImageView surroudingImage;
     private TextView worthTxt;
     private RecyclerView usernamesRecyclerView;
     Button commentBtn;

     private CurrentUserHelper currentUserHelper = CurrentUserHelper.getInstance();
     private GoogleMap map;
     private QRCode currentQRcode;
     String codeID;
     String worth;

     /**
      * Binds and adds listeners.
      * Also initalizes the map.
      * @param savedInstanceState
      */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_qractivity);
        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.qrMap);
        mapFragment.getMapAsync(this);

        surroudingImage = findViewById(R.id.surroundingImage);
        usernamesRecyclerView = findViewById(R.id.usernameList);
        worthTxt = findViewById(R.id.worthTxt);
        commentBtn = findViewById(R.id.commentBtn);

        // Malhar's Edit
        commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), QRCodeCommentActivity.class);
                intent.putExtra("QRCodeCommentActivity", codeID);
                intent.putExtra("Worth", worth);
                startActivity(intent);
            }
        });
    }

     /**
      * Sets the app bar title.
      */
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        codeID = intent.getStringExtra("codeID");

        getSupportActionBar().setTitle("QR CODE: " + codeID);

        currentQRcode = new QRCode();
        currentQRcode.setId(codeID);
    }


     /**
      * Once the map is ready, it will get all the QR code locations and plot them.
      * @param googleMap
      */
     @Override
     public void onMapReady(@NonNull GoogleMap googleMap) {

         new Thread(){
             @Override
             public void run() {
                 FirebaseFirestore
                         .getInstance()
                         .collection("Codes")
                         .document(currentQRcode.getId())
                         .get()
                         .addOnSuccessListener(documentSnapshot -> {
                             currentQRcode = documentSnapshot.toObject(QRCode.class);

                             // Worth Text
                             worth = String.valueOf(currentQRcode.getWorth());
                             worthTxt.setText("Worth: " + currentQRcode.getWorth());

                             // QR Code Location
                             if (currentQRcode.getCoordinates().size() == 2) {
                                LatLng qrCodePosition = new LatLng(currentQRcode.getCoordinates().get(0), currentQRcode.getCoordinates().get(1));
                                googleMap.addMarker(new MarkerOptions().position(qrCodePosition).title("Hash Location"));
                                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(qrCodePosition, 18, 45, 0)));
                             }

                             // Image
                             if (currentQRcode.getImageUrl() != null && !currentQRcode.getImageUrl().isEmpty()) {
                                 Picasso.get().load(currentQRcode.getImageUrl()).into(surroudingImage);
                             }

                             // Usernames
                             QrUsernameAdapter qrUsernameAdapter = new QrUsernameAdapter(currentQRcode.getPlayers(), SingleQRActivity.this);
                             usernamesRecyclerView.setAdapter(qrUsernameAdapter);
                             usernamesRecyclerView.setLayoutManager(new LinearLayoutManager(SingleQRActivity.this));

                         })
                         .addOnFailureListener(e->{
                             e.printStackTrace();
                         });
             }
         }.start();
    }
 }