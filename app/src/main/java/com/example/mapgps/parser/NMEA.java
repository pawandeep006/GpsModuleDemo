package com.example.mapgps.parser;

import java.util.HashMap;
import java.util.Map;


public class NMEA {

    private static final Map<String, SentenceParser> sentenceParsers = new HashMap<>();
    GPSPosition position = new GPSPosition();

    public NMEA() {
        sentenceParsers.put("GPGGA", new GPGGA());
        sentenceParsers.put("GPGGL", new GPGGL());
        sentenceParsers.put("GPRMC", new GPRMC());
        sentenceParsers.put("GPRMZ", new GPRMZ());
        //only really good GPS devices have this sentence but ...
        sentenceParsers.put("GPVTG", new GPVTG());
    }

    // utils
    static float Latitude2Decimal(String lat, String NS) {
        float med = Float.parseFloat(lat.substring(2)) / 60.0f;
        med += Float.parseFloat(lat.substring(0, 2));
        if (NS.startsWith("S")) {
            med = -med;
        }
        return med;
    }

    static float Longitude2Decimal(String lon, String WE) {
        float med = Float.parseFloat(lon.substring(3)) / 60.0f;
        med += Float.parseFloat(lon.substring(0, 3));
        if (WE.startsWith("W")) {
            med = -med;
        }
        return med;
    }

    public GPSPosition parse(String line) {

        if (line.startsWith("$")) {
            String nmea = line.substring(1);
            String[] tokens = nmea.split(",");
            String type = tokens[0];
            //TODO check crc
            if (sentenceParsers.containsKey(type)) {
                sentenceParsers.get(type).parse(tokens, position);
            }
            position.updatefix();
        }

        return position;
    }

    // fucking java interfaces
    interface SentenceParser {
        public boolean parse(String[] tokens, GPSPosition position);
    }

    // parsers
    class GPGGA implements SentenceParser {
        public boolean parse(String[] tokens, GPSPosition position) {
            position.time = Float.parseFloat(tokens[1]);
            position.lat = Latitude2Decimal(tokens[2], tokens[3]);
            position.lon = Longitude2Decimal(tokens[4], tokens[5]);
            position.quality = Integer.parseInt(tokens[6]);
            position.hdop = Float.parseFloat(tokens[8]);
            position.altitude = Float.parseFloat(tokens[9]);
            return true;
        }
    }

    class GPGGL implements SentenceParser {
        public boolean parse(String[] tokens, GPSPosition position) {
            position.lat = Latitude2Decimal(tokens[1], tokens[2]);
            position.lon = Longitude2Decimal(tokens[3], tokens[4]);
            position.time = Float.parseFloat(tokens[5]);
            return true;
        }
    }

    class GPRMC implements SentenceParser {
        public boolean parse(String[] tokens, GPSPosition position) {
            position.time = Float.parseFloat(tokens[1]);
            position.lat = Latitude2Decimal(tokens[3], tokens[4]);
            position.lon = Longitude2Decimal(tokens[5], tokens[6]);
            position.velocity = Float.parseFloat(tokens[7]);
            position.dir = Float.parseFloat(tokens[8]);
            return true;
        }
    }

    class GPVTG implements SentenceParser {
        public boolean parse(String[] tokens, GPSPosition position) {
            position.dir = Float.parseFloat(tokens[3]);
            return true;
        }
    }

    class GPRMZ implements SentenceParser {
        public boolean parse(String[] tokens, GPSPosition position) {
            position.altitude = Float.parseFloat(tokens[1]);
            return true;
        }
    }

    public class GPSPosition {
        public float time = 0.0f;
        public float lat = 0.0f;
        public float lon = 0.0f;
        public boolean fixed = false;
        public int quality = 0;
        public float dir = 0.0f;
        public float altitude = 0.0f;
        public float velocity = 0.0f;
        public float hdop;

        public void updatefix() {
            fixed = quality > 0;
        }

        public String toString() {
            return String.format("POSITION: lat: %f, lon: %f, time: %f, Q: %d, acc: %f, dir: %f, alt: %f, vel: %f", lat, lon, time, quality, hdop, dir, altitude, velocity);
        }
    }
}