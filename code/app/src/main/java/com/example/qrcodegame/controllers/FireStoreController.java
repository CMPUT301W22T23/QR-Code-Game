package com.example.qrcodegame.controllers;

import com.example.qrcodegame.models.Comment;
import com.example.qrcodegame.models.QRCode;
import com.example.qrcodegame.utils.CurrentUserHelper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FireStoreController {

    private static FireStoreController single_instance = null;

    /**
     * Singleton constructor
     * @return returns the singular object
     */
    public static FireStoreController getInstance() {
        if (single_instance == null) {
            single_instance = new FireStoreController();
        }
        return single_instance;
    }

    // Helper Var
    CurrentUserHelper currentUserHelper = CurrentUserHelper.getInstance();

    // Firestore Variables
    private final DocumentReference userDocument = FirebaseFirestore.getInstance().collection("Users").document(currentUserHelper.getFirebaseId());
    private final CollectionReference userCollectionReference = FirebaseFirestore.getInstance().collection("Users");
    private final CollectionReference qrCollectionReference = FirebaseFirestore.getInstance().collection("Codes");
    private final CollectionReference commentsCollectionReference = FirebaseFirestore.getInstance().collection("Comments");
    final FirebaseStorage storage = FirebaseStorage.getInstance();

    public Task<QuerySnapshot> getAllCodesWithLocation() {
        return qrCollectionReference.whereNotEqualTo("coordinates", new ArrayList<>()).get();
    };

    public Task<QuerySnapshot> getAllPlayers() {
        return userCollectionReference.whereNotEqualTo("isOwner", true).get();
    }

    public Task<QuerySnapshot> getAllCurrentUserCodes() {
        return qrCollectionReference.whereArrayContains("players", currentUserHelper.getUsername()).get();
    }

    public Task<DocumentSnapshot> getSingleQRCode(String qrCodeId) {
        return qrCollectionReference.document(qrCodeId).get();
    }

    public DocumentReference setListenerForSingleQRCodeComments(String id) {
        return commentsCollectionReference.document(id);
    }

    public void storeCommentAtId(String codeID, Comment commentToStore) {
        commentsCollectionReference
                .document(codeID)
                .update("comment", FieldValue.arrayUnion(commentToStore));
    }
}
