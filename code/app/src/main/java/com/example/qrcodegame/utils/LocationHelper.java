package com.example.qrcodegame.utils;

import static android.content.Context.LOCATION_SERVICE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.qrcodegame.models.QRCode;


import java.util.ArrayList;

import android.location.LocationListener;

public class LocationHelper implements LocationListener {

    private Context context;
    private QRCode currentQRCode;

    public LocationHelper (Context context, QRCode currentQRCode) {
        this.context = context;
        this.currentQRCode = currentQRCode;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        ArrayList<Double> coordinates = new ArrayList<>();
        coordinates.add(location.getLatitude());
        coordinates.add(location.getLongitude());
        if (currentQRCode != null)
            currentQRCode.setCoordinates(coordinates);
        System.out.println("HERE " + currentQRCode);
    }

    public void getCurrentLocation() {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
