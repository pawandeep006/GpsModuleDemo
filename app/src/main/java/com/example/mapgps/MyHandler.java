package com.example.mapgps;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;

class MyHandler extends Handler {
    private final WeakReference<MapsActivity> mActivity;

    MyHandler(MapsActivity activity) {
        mActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UsbService.MESSAGE_FROM_SERIAL_PORT:
                String data = (String) msg.obj;
                mActivity.get().drawOnMap(data);
                break;
            case UsbService.CTS_CHANGE:
                Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                break;
            case UsbService.DSR_CHANGE:
                Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                break;
        }
    }

//        private int getLength(String data) {
//            return data.split(",").length;
//        }
//
//        private String getLocation(String data) {
//            String[] locationArray = data.split(",");
//            if (locationArray.length != 13)
//                return mActivity.get().locationData.getText().toString();
//
//            if (!"$GPRMC".equalsIgnoreCase(locationArray[0])) {
//                return mActivity.get().locationData.getText().toString();
//            }
//
//            String ttfString = "TTF: " + locationArray[1];
//            String status = getStatus(locationArray[2]);
//            String latString = locationArray[3] + locationArray[4];
//            String longString = locationArray[5] + locationArray[6];
////            String quality = "Fix quality: " + quality(locationArray[6]);
////            String satString = "SAT: " + locationArray[7];
////            String altitudeString = "Alt: " + locationArray[9] + locationArray[10];
//
//
//            return ttfString + "\n" +
//                    status + "\n" +
//                    latString + "," + longString //+ "\n" +
//                    /*quality + "\n" +
//                    satString + "\n" +
//                    altitudeString*/;
//        }
//
//        private String getStatus(String s) {
//            if ("a".equalsIgnoreCase(s)) {
//                return "Active";
//            }
//
//            if ("v".equalsIgnoreCase(s)) {
//                return "Void";
//            }
//            return "NA";
//        }
//
//        private String quality(String s) {
//            if ("".equalsIgnoreCase(s))
//                return "";
//
//            int code = Integer.parseInt(s);
//
//            switch (code) {
//                case 0:
//                    return "invalid";
//                case 1:
//                    return "GPS fix (SPS)";
//                case 2:
//                    return "DGPS fix";
//                case 3:
//                    return "PPS fix";
//                case 4:
//                    return "Real Time Kinematic";
//                case 5:
//                    return "Float RTK";
//                case 6:
//                    return "estimated (dead reckoning) (2.3 feature)";
//                case 7:
//                    return "Manual input mode";
//                case 8:
//                    return "Simulation mode";
//            }
//
//            return "invalid";
//        }
}