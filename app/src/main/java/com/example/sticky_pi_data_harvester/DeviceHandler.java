package com.example.sticky_pi_data_harvester;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.time.Instant;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.util.Log;

//fixme does not work when connected to nordvpn app!!

public class DeviceHandler extends Thread {
    final static double VOLTAGE_DIVIDER = 0.6;
    final static double REF_VOLTAGE = 3.3;
    final static double MAX_LIPO_VOLTAGE = 4.2;
    final static double MIN_LIPO_VOLTAGE = 3.5;

    private Location location;
    String host_address;
    String device_id;
    String target_dir;
    String last_image_path = "";
    String status = "starting"; // syncing, "errored", "done"

    int port;
    boolean is_mock=false;

    long device_datetime;
    long time_created;
    int n_to_download = -1;
    int n_downloaded = 0;
    int n_skipped = 0;
    int n_errored = 0;
    int battery_level=0;

    String version = "";
    float available_disk_space = -1;
    long last_pace;

    URL status_url, images_url,keep_alive_url, metadata_url, clear_disk_url, stop_url, log_url;

    static final int THUMBNAIL_HEIGHT = 96;
    static final int THUMBNAIL_WIDTH = 128;

    public String get_device_id(){
        return device_id;
    }

    public long get_device_datetime(){
        return device_datetime;
    }
    public int get_n_to_download(){
        return n_to_download;
    }

    public int get_n_downloaded(){
        return n_downloaded;
    }

    public int get_n_errored(){
        return n_errored;
    }

    public int get_n_skipped(){
        return n_skipped;
    }

    public int get_battery_level(){
        double voltage = (battery_level / 100.0) * REF_VOLTAGE / VOLTAGE_DIVIDER;
        double out = (voltage - MIN_LIPO_VOLTAGE) / (MAX_LIPO_VOLTAGE - MIN_LIPO_VOLTAGE);
        if(out < 0)
            out = 0;
        if(out > 1)
            out = 1;
        return (int) (100 * out);
    }

    public float get_available_disk_space(){
        return available_disk_space;
    }

    public String get_last_image_path(){
        return last_image_path;
    }

    public long get_last_pace(){
        return last_pace;
    }

    public String get_status(){
        return status;
    }


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
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        }
    }
    public static boolean createDirIfNotExists(String path) {
        boolean ret = true;
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.e(TAG, "Storage is not mounted");
            return false;
        }
        File file = new File(path);
        if (!file.exists()) {
            Log.i(TAG, "Creating: " + file.getAbsolutePath());
            if (!file.mkdirs()) {
                Log.e(TAG, "Problem creating Image folder");
                ret = false;
            }
        }
        else{
            Log.i(TAG, "Already exists: " + file.getAbsolutePath());
        }
        return ret;
    }
//    public DeviceHandler(InetAddress host, int port, String name, Location loc){
    public DeviceHandler(NsdServiceInfo serviceInfo, Location loc, File storage_dir){
        super("Thread-" + serviceInfo.getServiceName().split("-")[1]);
        time_created = Instant.now().getEpochSecond();
        port = serviceInfo.getPort();
        host_address = serviceInfo.getHost().getHostAddress();
        try {
            status_url = new URL("http", host_address, port, "status");
            images_url = new URL("http", host_address, port, "images");
            log_url = new URL("http", host_address, port, "log");
            // posts
            keep_alive_url = new URL("http", host_address, port, "keep_alive");
            metadata_url = new URL("http", host_address, port, "metadata");
            clear_disk_url = new URL("http", host_address, port, "clear_disk");
            stop_url = new URL("http", host_address, port, "stop");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        device_id =  serviceInfo.getServiceName().split("-")[1];
        location = loc;
        Log.i(TAG, "Registering device " + device_id + " for sync. IP = " + host_address);
        last_pace = Instant.now().getEpochSecond();
        //fixme CHECK permissions/ presence of sd card
        target_dir =  storage_dir + "/" + device_id;
        //fixme delettttttttttttttttttttttttttte
        delete_local_files();
    }

    private void delete_local_files(){
        File dir = new File(target_dir);
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }
    }

    private  boolean get_log() {
        try {
            Log.w(TAG, "URL: " + log_url.toString());
            JSONObject out = readJsonFromUrl(log_url.toString());
            Log.w("TODEL", "OUT: " + out.toString());
//            device_id = out.getString("device_id");
//            version = out.getString("version");
//            device_datetime  = (long) out.getDouble("datetime");
//            available_disk_space = (float) out.getDouble("available_disk_space");
//            battery_level = (int) out.getDouble("battery_level");
//            if(out.has("is_mock_device"))
//                is_mock = out.getInt("is_mock_device") != 0;


        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
        last_pace = Instant.now().getEpochSecond();
        return true;
    }

    private  boolean get_metadata() {
        try {
            Log.w(TAG, "URL: " + status_url.toString());
            JSONObject out = readJsonFromUrl(status_url.toString());
            Log.w(TAG, "OUT: " + out.toString());
            device_id = out.getString("device_id");
            version = out.getString("version");
            device_datetime  = (long) out.getDouble("datetime");
            available_disk_space = (float) out.getDouble("available_disk_space");
            battery_level = (int) out.getDouble("battery_level");
            if(out.has("is_mock_device"))
                is_mock = out.getInt("is_mock_device") != 0;


        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
        Log.i(TAG, "Device status");
        last_pace = Instant.now().getEpochSecond();
        return true;
    }
    private String compute_hash(String path){
        try {
            return String.valueOf(Files.size(Paths.get(path )));
        } catch (IOException e) {
            return "";
        }
    }
    private  boolean get_single_image(URL remote_url, String path, String hash, int retry) {
        Log.i(TAG, "Getting: "+ remote_url.toString());
//
        if(retry > 3){
            Log.e(TAG, "Max retry reached. Failed to get image " + remote_url.toString());
            n_errored ++;
            return false;

        }

        final String tmp_path  = path + ".tmp";

        if (new File(path).exists()){
            if(compute_hash(path).equals(hash)){
                Log.i(TAG, "Skipping preexisting image: " + remote_url);
                n_skipped ++;
                last_pace = Instant.now().getEpochSecond();
                return true;
            }
        }
        URLConnection conn = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            conn = remote_url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.connect();
            InputStream input = conn.getInputStream();
//            OutputStream output = new FileOutputStream(tmp_path);
            byte data[] = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();


            if(is_mock) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return get_single_image(remote_url, path, hash, retry +1);
        }

        try(OutputStream outputStream = new FileOutputStream(tmp_path)) {
            output.writeTo(outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String local_hash = compute_hash(tmp_path);

        if(hash.equals(local_hash)) {
            File tmp_file = new File(tmp_path);
            make_thumbnail(tmp_file, new File(path + ".thumbnail"));
            File out = new File(path);
            tmp_file.renameTo(out);

            //fixme iff > than previous
            File last_image_file = new File(last_image_path);
            if(last_image_file.getName().compareTo(out.getName()) < 0 )
                last_image_path = out.getAbsolutePath();

            n_downloaded++;
            last_pace = Instant.now().getEpochSecond();
            return true;
        }
        else {
            Log.w(TAG, "Wrong hash " + local_hash + " != " + hash + " for image: " + remote_url.toString() + ". Retrying...");
            return  get_single_image(remote_url, path, hash, retry +1);
        }
    }
    private void make_thumbnail(File file, File target){

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true; // obtain the size of the image, without loading it in memory
        BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOptions);

// find the best scaling factor for the desired dimensions
        int desiredWidth = THUMBNAIL_WIDTH;
        int desiredHeight = THUMBNAIL_HEIGHT;
        float widthScale = (float)bitmapOptions.outWidth/desiredWidth;
        float heightScale = (float)bitmapOptions.outHeight/desiredHeight;
        float scale = Math.min(widthScale, heightScale);

        int sampleSize = 1;
        while (sampleSize < scale) {
            sampleSize *= 2;
        }
        bitmapOptions.inSampleSize = sampleSize; // this value must be a power of 2,
        // this is why you can not have an image scaled as you would like
        bitmapOptions.inJustDecodeBounds = false; // now we want to load the image
        Bitmap thumbnail = BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOptions);
        try {
            FileOutputStream fos = new FileOutputStream(target);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        thumbnail.recycle();
    }
    private  boolean get_images() {
//        boolean errored = false;
        try {
            Log.w(TAG, "URL: " + images_url.toString());
            JSONObject out = readJsonFromUrl(images_url.toString());
            Iterator<String> it = out.keys();
            n_to_download = 0;
            // todo images by date sort here
//            so we get the old ones first!
            while (it.hasNext()) {
                n_to_download+=1;
                it.next();
            }
            ThreadPoolExecutor executor =
                    (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
            it = out.keys();
            while (it.hasNext()){
                String k = it.next();
                String hash = out.getString(k);
                String filename = device_id + "." + k + ".jpg";
                URL image_url = new URL("http", host_address, port, "static/" + filename);
                executor.submit( () -> {
                    get_single_image(image_url, target_dir + "/" + filename, hash, 0);
                    keep_alive();

                });
            }

            executor.shutdown();
            executor.awaitTermination(15, TimeUnit.MINUTES);
        } catch (IOException | JSONException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return n_errored == 0;
    }

    private  boolean keep_alive_or_clear_disk(URL url){

        JSONObject json = new JSONObject();

        try {
            json.put("device_id", device_id);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        Log.i(TAG, "POSTING" + json.toString());
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            DataOutputStream out_stream  = new DataOutputStream(conn.getOutputStream());

            out_stream.writeBytes(json.toString());
            out_stream.flush();
            out_stream.close();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            if (200 != conn.getResponseCode()) {
               Log.e(TAG, "POST failed");
               return false;
           }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        conn.disconnect();
        return true;
    }

    private  boolean keep_alive(){return keep_alive_or_clear_disk(keep_alive_url);}
    private  boolean clear_disk(){return keep_alive_or_clear_disk(clear_disk_url);}
    private  boolean stop_server(){return keep_alive_or_clear_disk(stop_url);}

    private  boolean set_metadata() throws IOException {
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
            return false;
        }
        Log.i(TAG, "POSTING" + json.toString());
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) metadata_url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            DataOutputStream out_stream  = new DataOutputStream(conn.getOutputStream());

            out_stream.writeBytes(json.toString());
            out_stream.flush();
            out_stream.close();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (200 != conn.getResponseCode()) {
            Log.e(TAG, "POST failed");
            return false;
        }
        conn.disconnect();
        return true;
    }


    public void run() {
        createDirIfNotExists(target_dir);

        //fixme timeout here
        while (true){
                Log.i(TAG, "Running: " + device_id + ", " + host_address + ":"+ port);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (! get_metadata())
                    continue;
                if (! get_log())
                    continue;
                try {
                    if (! set_metadata())
                        continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                status = "syncing";

                if(get_images()) {
//                if(true) {
                    Log.i(TAG,"Clearing disk");
                    clear_disk();
                    stop_server();
                    status = "done";
                }
                else {
                    status = "errored";
                }
            return;


        }
    }


}
