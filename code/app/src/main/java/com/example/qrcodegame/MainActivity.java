package com.example.qrcodegame;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qrcodegame.models.QRCode;
import com.example.qrcodegame.utils.CurrentUserHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    TextView welcomeText;
    TextView analyzeText;
    TextView resultText;
    ImageButton scanQRButton;

    Button locationPhotoBtn;
    Button saveQRtoCloudBtn;

    QRCode currentQRCode;
    byte[] locationImage;

    final FirebaseStorage storage = FirebaseStorage.getInstance();
    final CollectionReference userCollectionReference = FirebaseFirestore.getInstance().collection("Users");
    final CollectionReference qrCollectionReference = FirebaseFirestore.getInstance().collection("Codes");

    ActivityResultLauncher<Intent> activityResultLauncher;

    CurrentUserHelper currentUserHelper = CurrentUserHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        // Binding
        welcomeText = findViewById(R.id.welcomeText);
        analyzeText = findViewById(R.id.analyzeText);
        resultText = findViewById(R.id.codeWorthText);
        scanQRButton = findViewById(R.id.scanQRCodeBtn);
        locationPhotoBtn = findViewById(R.id.takeLocationBtn);
        saveQRtoCloudBtn = findViewById(R.id.saveQRtoCloudBtn);

        // Update
        welcomeText.setText("Welcome " + currentUserHelper.getUsername() +"!");
        analyzeText.setVisibility(View.INVISIBLE);
        resultText.setVisibility(View.INVISIBLE);

        // Updating listeners
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Fetching image
                        Bitmap image = (Bitmap) result.getData().getExtras().get("data");
                        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.JPEG, 100, bytesStream);
                        locationImage = bytesStream.toByteArray();
                        // Updating UI
                        locationPhotoBtn.setText("REMOVE IMAGE");
                        locationPhotoBtn.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.black, null));
                    }
                }
        );

        // Listeners
        scanQRButton.setOnClickListener(this);

        locationPhotoBtn.setOnClickListener(view -> {

            if (locationImage == null) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                activityResultLauncher.launch(intent);
                return;
            }

            locationPhotoBtn.setText("TAKE PHOTO");
            locationPhotoBtn.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.purple_200, null));
            Toast.makeText(this, "Image removed!", Toast.LENGTH_SHORT).show();
        });

        saveQRtoCloudBtn.setOnClickListener(v->{
            saveCode();
        });
    }

    private void saveCode() {

        boolean saveLocation = ((CheckBox) findViewById(R.id.saveLocationCheckBox)).isChecked();

        // Check if QR code already exists

            // If so, update QR code,
            // Update player

        // Else

            // Create QR Code
            // Already created

            // Save Image
            StorageReference imageLocationStorage = storage.getReference().child("images").child(currentQRCode.getId() + ".jpg");
            imageLocationStorage
                    .putBytes(locationImage)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageLocationStorage.getDownloadUrl().addOnSuccessListener( uri -> {
                            currentQRCode.setImageUrl(uri.toString());
                        }).addOnFailureListener(e -> {
                            System.err.println(e.getMessage());
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Image could not be saved!", Toast.LENGTH_SHORT).show();
                        System.err.println(e.getMessage());
                    });

            // Save Location
//            if (saveLocation) {
//
//            }

            // Save QR Code

    }

    @Override
    public void onClick(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CaptureAct.class);
        integrator.setOrientationLocked(true);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scanning Code");
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String qrCodeContent =  result.getContents();
                calculateWorth(qrCodeContent);
                return;
            } else {
                Toast.makeText(this, "Scanning cancelled!", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void calculateWorth(String qrCodeContent) {

        if (qrCodeContent == null || qrCodeContent.isEmpty()) {
            return;
        }

        if (qrCodeContent.startsWith("Account-Transfer=")){
            // Transfer account settings
            return;
        }

        if (qrCodeContent.startsWith("View-Profile=")){
            // View someones profile
            return;
        }

        try {

            // calculate sha-256
            // Citation: https://stackoverflow.com/questions/5531455/how-to-hash-some-string-with-sha256-in-java
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(qrCodeContent.getBytes(StandardCharsets.UTF_8));

            final StringBuilder hashStr = new StringBuilder(hash.length);
            for (byte hashByte : hash)
                hashStr.append(Integer.toHexString(255 & hashByte));

            // Hashed string here
            final String hashedContent = hashStr.toString();




            final char[] hashedArray = hashedContent.toCharArray();

            // Calculating String
            char[] codeArray = new char[hashedContent.length()];
            int[] intCodeArray = new int[hashedContent.length()+1];
            int comparer = 0;
            int codeWorth = 0;
            int counter = 0;

            // Copy char by char into array
            for (int i = 0; i < hashedContent.length(); i++) {
                codeArray[i] = hashedContent.charAt(i);
            }

            // hex to int. put this int in the intCodeArray
            // help from https://stackoverflow.com/questions/26839558/hex-char-to-int-conversion
            for (int i = 0; i < hashedContent.length(); i++) {
                if (codeArray[i] >= '0' && codeArray[i] <= '9')
                    intCodeArray[i] = codeArray[i] - '0';
                if (codeArray[i] >= 'A' && codeArray[i] <= 'F')
                    intCodeArray[i] = codeArray[i] - 'A' + 10;
                if (codeArray[i] >= 'a' && codeArray[i] <= 'f')
                    intCodeArray[i] = codeArray[i] - 'a' + 10;
            }

            // used for cases like "1000"; the 16 at the end breaks the counter since its the
                // end of the hash string. And 16 is out of the range of a hex so its safe to use
            intCodeArray[hashedContent.length()] = 16;

            comparer = intCodeArray[0];
            for (int i = 1; i < hashedContent.length() + 1; i++) {
                if (intCodeArray[i] == comparer) {
                    counter += 1;
                }
                else {
                    if (counter != 0)
                    {
                        if (comparer != 0) {
                            codeWorth += Math.pow(comparer, counter);
                        }
                        else {
                            codeWorth += Math.pow(20, counter);
                        }
                        counter = 0;
                    }

                    else if (comparer == 0) {
                        codeWorth += 1;
                    }
                    comparer = intCodeArray[i];
                }
            }




            // Displaying text
            analyzeText.setVisibility(View.VISIBLE);
            resultText.setVisibility(View.VISIBLE);
            resultText.setText("This Hash is worth: " + codeWorth);

            // Updating the QR code Object
            currentQRCode.setId(hashedContent);
            currentQRCode.setWorth(codeWorth);


        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            System.err.println(e.getMessage());
            return;
        }

    }

}