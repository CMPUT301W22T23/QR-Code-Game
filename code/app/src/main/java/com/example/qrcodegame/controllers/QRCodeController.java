package com.example.qrcodegame.controllers;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.qrcodegame.SplashScreenActivity;
import com.example.qrcodegame.ViewProfileActivity;
import com.example.qrcodegame.interfaces.CodeSavedListener;
import com.example.qrcodegame.models.QRCode;
import com.example.qrcodegame.utils.CurrentUserHelper;
import com.example.qrcodegame.utils.LocationHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class QRCodeController {

    // Variables we use
    private LocationHelper locationHelper;
    private QRCode currentQrCode;
    private Context context;
    public byte[] locationImage;
    private CodeSavedListener codeSavedListener;

    // Helper Var
    CurrentUserHelper currentUserHelper = CurrentUserHelper.getInstance();

    // Firestore Variables
    private final DocumentReference userDocument = FirebaseFirestore.getInstance().collection("Users").document(currentUserHelper.getFirebaseId());
    private final CollectionReference qrCollectionReference = FirebaseFirestore.getInstance().collection("Codes");
    final FirebaseStorage storage = FirebaseStorage.getInstance();

    private void initNewCode(){
        currentQrCode = new QRCode();
        locationImage = null;
    }

    public QRCodeController(Context context, CodeSavedListener codeSavedListener) {
        this.context = context;
        currentQrCode = new QRCode();
        locationHelper = new LocationHelper(context);
        locationHelper.startLocationUpdates();
        this.codeSavedListener = codeSavedListener;
    }


    /**
     * This method handles the hash content and takes the correct next steps if needed
     * @param qrCodeContent The String stored within the QR code
     * @return status code
     */
    public int processHash(String qrCodeContent) {
        CollectionReference userCollection = FirebaseFirestore.getInstance().collection("Users");

        if (qrCodeContent == null || qrCodeContent.isEmpty()) {
            return 0;
        }

        if (qrCodeContent.startsWith("View-Profile=")) {
            // View someones profile
            String usernameToView = qrCodeContent.split("=")[1];
            Intent intent = new Intent(context, ViewProfileActivity.class);
            intent.putExtra("username", usernameToView);
            context.startActivity(intent);
            return 0;
        }

        if (qrCodeContent.startsWith("Transfer-Profile=")) {
            AtomicInteger success = new AtomicInteger();

            HashMap<String, Object> updates = new HashMap<>();
            updates.put("devices", FieldValue.arrayUnion(CurrentUserHelper.getInstance().getUniqueID()));
            // Transfer
            String usernameToTransferTo = qrCodeContent.split("=")[1];

            userCollection
                    .whereEqualTo("username",usernameToTransferTo)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            return;
                        }
                        String idToUpdate = queryDocumentSnapshots.getDocuments().get(0).getId();

                        userCollection
                                .document(idToUpdate)
                                .update(updates)
                                .addOnSuccessListener(v ->
                                        userCollection
                                                .document(CurrentUserHelper.getInstance().getFirebaseId())
                                                .update("devices", FieldValue.arrayRemove(CurrentUserHelper.getInstance().getUniqueID()))
                                                .addOnSuccessListener(v1 -> {
                                                    Intent intent = new Intent(context, SplashScreenActivity.class);
                                                    context.startActivity(intent);
                                                    success.set(2);
                                                }))
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "User not found!", Toast.LENGTH_SHORT).show();
                                });
                    });
            return success.get();
        }
        calculateWorth(qrCodeContent);
        return 1;
    }

    /**
     * Calculates the worth of the code.
     * @param qrCodeContent String content of the hash scanned
     */
    public  void calculateWorth(String qrCodeContent) {
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
            int[] intCodeArray = new int[hashedContent.length() + 1];
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
                } else {
                    if (counter != 0) {
                        if (comparer != 0) {
                            codeWorth += Math.pow(comparer, counter);
                        } else {
                            codeWorth += Math.pow(20, counter);
                        }
                        counter = 0;
                    } else if (comparer == 0) {
                        codeWorth += 1;
                    }
                    comparer = intCodeArray[i];
                }
            }

            // Updating the QR code Object
            currentQrCode.setId(hashedContent);
            currentQrCode.setWorth(codeWorth);

        } catch (Exception e) {
            Toast.makeText(context, "Something went wrong!", Toast.LENGTH_SHORT).show();
            System.err.println(e.getMessage());
        }
    }

    /**
     * Initial step in saving code. It checks if the hash already exists.
     * If it does, it will update the info, else, creates the code.
     */
    public void saveCode(Boolean locationIsChecked) {
        // Check if QR code already exists
        qrCollectionReference.whereEqualTo("id",currentQrCode.getId()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.getDocuments().size() > 0) {
                        updateExistingCode();
                    } else {
                        createNewCode(locationIsChecked);
                    }
                });
    }

    /**
     * Create a new Hash object in db
     * Also stores the image if set.
     */
    private void createNewCode(Boolean locationIsChecked) {
        // Add location if checked and location is ready
        if (locationIsChecked && !Objects.isNull(currentUserHelper.getCurrentLocation()) && currentUserHelper.getCurrentLocation().size() > 0 ) {
            currentQrCode.setCoordinates(currentUserHelper.getCurrentLocation());
        };

        if (currentQrCode.getId() == null || currentQrCode.getId().isEmpty() || currentQrCode.getWorth() == 0) {
            /*Toast.makeText(this, "Scan a QR Code first!", Toast.LENGTH_SHORT).show();
            return;*/
            // TODO
            // REMOVE
            currentQrCode.setId( "TEST"  + UUID.randomUUID().toString());
            currentQrCode.setWorth((int) Math.floor(Math.random()*1000));
        };

        if (locationImage != null) {
            // Save Image
            StorageReference imageLocationStorage = storage.getReference().child("images").child(currentQrCode.getId() + ".jpg");
            imageLocationStorage
                    .putBytes(locationImage)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageLocationStorage.getDownloadUrl().addOnSuccessListener(uri -> {
                            currentQrCode.setImageUrl(uri.toString());
                            saveCodeFireStore();
                        }).addOnFailureListener(e -> {
                            System.err.println(e.getMessage());
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Image could not be saved!", Toast.LENGTH_SHORT).show();
                        System.err.println(e.getMessage());
                    });
        } else {
            saveCodeFireStore();
        }
    }

    /**
     * Updates the existing hash in DB
     */
    private void updateExistingCode() {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("collectedCodes", FieldValue.arrayUnion(currentQrCode.getId()));
        updates.put("totalScore", FieldValue.increment(currentQrCode.getWorth()));
        userDocument
                .update(updates);
        qrCollectionReference
                .document(currentQrCode.getId())
                .update("players", FieldValue.arrayUnion(currentUserHelper.getUsername()));
        initNewCode();
        Toast.makeText(context, "Stored!", Toast.LENGTH_SHORT).show();
        codeSavedListener.resetUI();
    }

    /**
     * Saves the code to the DB, then updates the player who saved it.
     */
    private void saveCodeFireStore() {
        currentQrCode.getPlayers().add(currentUserHelper.getUsername());
        qrCollectionReference
            .document(currentQrCode.getId())
            .set(currentQrCode)
            .addOnSuccessListener(v -> {
                HashMap<String, Object> updates = new HashMap<>();
                updates.put("collectedCodes", FieldValue.arrayUnion(currentQrCode.getId()));
                updates.put("totalScore", FieldValue.increment(currentQrCode.getWorth()));
                userDocument.update(updates);
                initNewCode();
                Toast.makeText(context, "Created!", Toast.LENGTH_SHORT).show();
                codeSavedListener.resetUI();
            });
    }

    public QRCode getCurrentQrCode() {
        return currentQrCode;
    }

    public void setCurrentQrCode(QRCode currentQrCode) {
        this.currentQrCode = currentQrCode;
    }

    public byte[] getLocationImage() {
        return locationImage;
    }

    public void setLocationImage(byte[] locationImage) {
        this.locationImage = locationImage;
    }
}
