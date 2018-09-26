package com.example.mapgps;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mapgps.parser.NMEA;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private GoogleMap mMap;
    private UsbService usbService;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    private ArrayList<LatLng> latLngsNMEA = new ArrayList<>();
    private ArrayList<LatLng> latLngsGPS = new ArrayList<>();
    private TextView statusView;
    private TextView gpsData;
    private NMEA nmeaParser;
    private FusedLocationWrapper.FusedListener listener = new FusedLocationWrapper.FusedListener() {
        @Override
        public void isGooglePlayServicesAvailable(boolean result, int resultCode, GoogleApiAvailability apiAvailability) {

        }

        @Override
        public void locationRequestFailure(Exception e) {

        }
    };
    private FusedLocationWrapper wrapper;
    private LocationCallback locationUpdate = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
//            super.onLocationResult(locationResult);

            if (locationResult == null)
                return;

            Location location = locationResult.getLastLocation();

            if (location == null) {
                return;
            }

            if (location.getLatitude() == 0 && location.getLongitude() == 0) {
                return;
            }

            statusView.setText("GPS Current location: " + location.getLatitude() + "," + location.getLongitude()
                    + "\n" + "Acc: " + location.getAccuracy());

            latLngsGPS.add(new LatLng(location.getLatitude(), location.getLongitude()));
            drawRoute();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHandler = new MyHandler(this);
        statusView = findViewById(R.id.status);
        gpsData = findViewById(R.id.gps_data);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        nmeaParser = new NMEA();

        wrapper = new FusedLocationWrapper(this, listener).init();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        centerCamera(new LatLng(28.491, 77.0801), 15);
        mMap = googleMap;
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
        wrapper.startLocationUpdates(locationUpdate);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        wrapper.stopLocationUpdates(locationUpdate);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    public void drawOnMap(String data) {
        if (mMap == null)
            return;

        NMEA.GPSPosition location = null;
        try {
            location = nmeaParser.parse(data);
        } catch (Exception ignored) {

        }

        processNmeaLocation(location);
    }

    private void processNmeaLocation(NMEA.GPSPosition location) {
        if (location == null) {
            return;
        }

        if (location.lat == 0 && location.lon == 0) {
            return;
        }

        gpsData.setText("Current location: " + location.lat + "," + location.lon
                + "\n" + "HDOP: " + location.hdop);

//        if (location.hdop > 1) {
//            return;
//        }

        latLngsNMEA.add(new LatLng(location.lat, location.lon));
        drawRoute();
    }

    private void drawRoute() {
        mMap.clear();
        if (!latLngsNMEA.isEmpty()) {
            PolylineOptions optionsNMEA = new PolylineOptions();
            optionsNMEA.width(5f);
            optionsNMEA.color(ContextCompat.getColor(MapsActivity.this, R.color.colorAccent));
            optionsNMEA.addAll(latLngsNMEA);
            mMap.addPolyline(optionsNMEA);
            mMap.addMarker(new MarkerOptions().position(latLngsNMEA.get(latLngsNMEA.size() - 1))
                    .title("Device"));
        }

        if (!latLngsGPS.isEmpty()) {
            PolylineOptions optionsGPS = new PolylineOptions();
            optionsGPS.width(5f);
            optionsGPS.color(ContextCompat.getColor(MapsActivity.this, R.color.colorPrimary));
            optionsGPS.addAll(latLngsGPS);
            mMap.addPolyline(optionsGPS);
            mMap.addMarker(new MarkerOptions().position(latLngsGPS.get(latLngsGPS.size() - 1))
                    .title("GPS"));
        }

        if (!latLngsGPS.isEmpty()) {
            centerCamera(latLngsGPS.get(latLngsGPS.size() - 1), mMap.getCameraPosition().zoom);
        } else {
            if (!latLngsNMEA.isEmpty())
                centerCamera(latLngsNMEA.get(latLngsNMEA.size() - 1), mMap.getCameraPosition().zoom);
        }
    }

    private void centerCamera(LatLng latLng, float zoom) {
        if (mMap == null)
            return;

        mMap.moveCamera(CameraUpdateFactory
                .newCameraPosition(
                        new CameraPosition.Builder()
                                .target(latLng)
                                .zoom(zoom)
                                .build()
                ));
    }

}
