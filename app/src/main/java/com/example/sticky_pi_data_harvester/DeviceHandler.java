package com.example.sticky_pi_data_harvester;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;

import java.io.BufferedReader;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

public class DeviceHandler extends Thread {
    private Location location;
    String host_address;
    int port;
    String base_url;
    String device_id;
    int n_to_download;
    int n_downloaded = 0;
    int n_skipped = 0;
    int n_errored = 0;

    long datetime;
    String version;
    int battery_level;
    float available_disk_space;
    long last_pace;
    URL status_url;

    private static String TAG;

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }


    public DeviceHandler(InetAddress host, int port, String name, Location loc){
        super("Thread-" + name.split("-")[1]);
        device_id = name.split("-")[1];

        TAG = "StickyPiDataHarvester-DeviceHandler" + "-" + device_id;

        host_address = host.getHostAddress();
        location = loc;

        Log.i(TAG, "Registering device" + device_id + "for sync. IP = " + host.getHostAddress());
//        Log.w(TAG,"======================================================");
//        Log.w(TAG,host.getHostName());
//        Log.w(TAG, host.getCanonicalHostName());
//        Log.w(TAG, device_id);
//        Log.w(TAG, base_url);
//        base_url = "http://" + host_address + ":" + String.valueOf(port);
        last_pace = Instant.now().getEpochSecond();
        try {
            status_url = new URL("http", host_address, port, "status");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    private  void get_metadata() {
        InputStream stream = null;
        // first get metadata
        try {
            JSONObject out = readJsonFromUrl(status_url.toString());
            Log.i(TAG, out.toString());
            device_id = out.getString("device_id");
            version = out.getString("version");
            datetime  = (long) out.getDouble("datetime");
            available_disk_space = (float) out.getDouble("available_disk_space");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Device status");
    }
    private  void set_metadata() {
        JSONObject json = new JSONObject();

        try {
            json.put("lat",location.getLatitude());
            json.put("lng",location.getLongitude());
            json.put("alt",location.getAltitude());
            json.put("datetime", (long) Instant.now().getEpochSecond());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "POSTING" + json.toString());
    }

    public void run() {
        get_metadata();
        // Second, set metadata
        set_metadata();
        get_metadata();


        while (true){
            Log.i(TAG, "running!");
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
