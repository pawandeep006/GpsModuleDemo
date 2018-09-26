package com.example.mapgps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;

public class FusedLocationWrapper {
    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final Context context;
    private final FusedListener listener;
    private FusedLocationProviderClient client;

    public FusedLocationWrapper(Context context, FusedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public FusedLocationWrapper init() {
        if (!isGooglePlayServicesAvailable())
            return this;
        if (client == null)
            client = LocationServices.getFusedLocationProviderClient(context);
        return this;
    }

    public void startLocationUpdates(LocationCallback locationUpdateListener) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (client == null) {
            return;
        }
        client.requestLocationUpdates(getLocationRequest(), locationUpdateListener, null)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        if (listener != null)
                            listener.locationRequestFailure(e);
                    }
                });
    }

    private LocationRequest getLocationRequest() {
        return new LocationRequest()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void stopLocationUpdates(LocationCallback locationUpdateListener) {
        if (client == null) {
            return;
        }
        client.removeLocationUpdates(locationUpdateListener);
        client = null;
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        if (apiAvailability == null) {
            if (listener != null)
                listener.isGooglePlayServicesAvailable(false, -1, null);
            return false;
        }
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (listener != null)
                listener.isGooglePlayServicesAvailable(apiAvailability.isUserResolvableError(resultCode), resultCode, apiAvailability);
            return false;
        }
        return true;
    }

    public interface FusedListener {
        void isGooglePlayServicesAvailable(boolean result, int resultCode, GoogleApiAvailability apiAvailability);

        void locationRequestFailure(Exception e);
    }
}
