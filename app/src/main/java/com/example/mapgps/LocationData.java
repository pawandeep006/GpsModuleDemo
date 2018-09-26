package com.example.mapgps;

import com.google.android.gms.maps.model.LatLng;

class LocationData {
    private final String status;
    private final LatLng latLng;

    public LocationData(String latString, String longString, String status) {
        this.status = status;
        double lat = 0, lng = 0;
        if (!"".equalsIgnoreCase(latString)) {
            lat = Double.parseDouble(latString) / 100;
        }

        if (!"".equalsIgnoreCase(longString)) {
            lng = Double.parseDouble(longString) / 100;
        }

        latLng = new LatLng(lat, lng);
    }

    public String getStatus() {
        return status;
    }

    public LatLng getLatLng() {
        return latLng;
    }
}
