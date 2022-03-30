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
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class DeviceHandler extends Thread {

    // fixme socket factory
    //  https://github.com/square/okhttp/pull/4865
    NsdManager.ResolveListener resolveListener;
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

    private final  static String TAG = "DEV_HANDLE";

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


//    public DeviceHandler(InetAddress host, int port, String name, Location loc){
    public DeviceHandler(NsdServiceInfo serviceInfo, Location loc, NsdManager nsdManager){
        super("Thread-" + serviceInfo.getServiceName().split("-")[1]);


        resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                Log.e(TAG, "FAILED to resolve");
            }
            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                port = nsdServiceInfo.getPort();
                host_address = nsdServiceInfo.getHost().getHostAddress();
                try {
                    status_url = new URL("http", host_address, port, "status");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        };
        nsdManager.resolveService(serviceInfo, resolveListener);

        device_id =  serviceInfo.getServiceName().split("-")[1];


// fixme here we should capture which interface this IP is valid on  (e.g. wlan0),
        // this must be done at discovery time

//        Log.w(TAG, host.getHostAddress());
//        Log.w(TAG, host.getHostName());
//        Log.w(TAG, host.getCanonicalHostName());
//        Log.w(TAG, host.getAddress());
        location = loc;

        Log.i(TAG, "Registering device " + device_id + " for sync. IP = " + host_address);
//        Log.w(TAG,"======================================================");
//        Log.w(TAG,host.getHostName());
//        Log.w(TAG, host.getCanonicalHostName());
//        Log.w(TAG, device_id);
//        Log.w(TAG, base_url);
//        base_url = "http://" + host_address + ":" + String.valueOf(port);
        last_pace = Instant.now().getEpochSecond();

    }


    private  void get_metadata() {

        InputStream stream = null;
//         first get metadata
        try {
            Log.w(TAG, "URL: " + status_url.toString());
            JSONObject out = readJsonFromUrl(status_url.toString());
            Log.w(TAG, "OUT: " + out.toString());
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
        double lat, lng, alt;
        if(location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            alt = location.getAltitude();
        }
        else{
            lat = 0;
            lng = 0;
            alt = 0;
        }
        try {
            json.put("lat",lat);
            json.put("lng",lng);
            json.put("alt",alt);
            json.put("datetime", (long) Instant.now().getEpochSecond());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "POSTING" + json.toString());
    }

    public void run() {
//
        // Second, set metadata
//
//        get_metadata();


        while (true){
            if( host_address != null) {
                Log.i(TAG, "Running: " + device_id + ", " + host_address + ":"+ port);
                get_metadata();
//                set_metadata();

            }
            else {
                Log.w(TAG, "Resolving: " + device_id );
            }
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
