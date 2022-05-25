package com.example.sticky_pi_data_harvester;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.util.Log;

//todo does not work when connected to nordvpn app!!


public class DeviceHandler extends Thread {
    final static double VOLTAGE_DIVIDER = 0.6;
    final static double REF_VOLTAGE = 3.3;
    final static double MAX_LIPO_VOLTAGE = 4.2;
    final static double MIN_LIPO_VOLTAGE = 3.5;
    final static long KEEP_ALIVE_TIMEOUT = 30;
    final static long INTERNAL_TIMEOUT = 60; // error if device not seen in this time in seconds
    final static long SHORT_TIMEOUT = 5000; // error if device not seen in this time in seconds

    final static long DOWNLOAD_ALL_IMG_TIMEOUT = 60 * 15; // fail to dowload after that many minutes

    // ghosts are persistent device status used to populate the initial view
    protected boolean is_ghost = false;

    long last_keep_alive = 0;
    long internal_pacemaker = 0;
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
    AtomicInteger n_downloaded = new AtomicInteger(0);
    AtomicInteger n_skipped = new AtomicInteger( 0);
    AtomicInteger n_errored = new AtomicInteger(0);
    int battery_level=0;

    String version = "";
    float available_disk_space = -1;
    long last_pace;
    boolean first_boot = false;

    URL status_url, images_url,keep_alive_url, metadata_url, clear_disk_url, stop_url, log_url;

    static final int THUMBNAIL_HEIGHT = 96;
    static final int THUMBNAIL_WIDTH = 128;

    public DeviceHandler() {
    }

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

        //todo CHECK permissions/ presence of sd card
        target_dir =  storage_dir + "/" + device_id;
    }


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
        return n_downloaded.get();
    }

    public int get_n_errored(){
        return n_errored.get();
    }

    public int get_n_skipped(){
        return n_skipped.get();
    }
    public long get_time_created(){
        return time_created;
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
    boolean get_is_ghost(){return is_ghost;}
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

    public static JSONObject readJsonFromUrl(String url, int timeout) throws IOException, JSONException {

        URLConnection connection = new URL(url).openConnection();
        JSONObject json  = null;
        try  {
            connection.setConnectTimeout(timeout);
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            json = new JSONObject(jsonText);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"IO exception" + e);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"JSON exception" );
        }
        return json;
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

    private void delete_local_files(){
        File dir = new File(target_dir);
        Log.e(TAG, "Deleting local files. likely for development");
        if (dir.isDirectory())
        {

            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                File file = new File(dir, children[i]);
                if (file.isDirectory()) {
                    String[] grand_children = file.list();
                    for (int j = 0; j < grand_children.length; j++){
                        File grand_file = new File(file, grand_children[j]);
                        if(grand_file.isFile()){
                            grand_file.delete();
                        }
                    }
                }
                else
                    file.delete();
            }
        }
    }

    private  boolean get_log() {
        try {
            JSONObject json = readJsonFromUrl(log_url.toString(), (int) INTERNAL_TIMEOUT);
            if(json== null)
                return false;
            String log = target_dir + "/" + device_id +".log";
            String tmp_log = log + "~";
            File tmp_file = new File(tmp_log);

            try (PrintStream out = new PrintStream(new FileOutputStream(tmp_log))) {
                out.print(json.toString());
                out.close();
                boolean success = tmp_file.renameTo(new File(log));
            }
            catch (Exception e){
                Log.e(TAG, String.valueOf(e));
            }
            finally {
                if(tmp_file.isFile()){
                    boolean _tmp = tmp_file.delete();
                }
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
        last_pace = Instant.now().getEpochSecond();
        return true;
    }

    private  boolean get_metadata() {
        try {
            JSONObject out = readJsonFromUrl(status_url.toString(), (int) INTERNAL_TIMEOUT);
            if(out== null)
                return false;
            device_id = out.getString("device_id");
            version = out.getString("version");
            device_datetime  = (long) out.getDouble("datetime");
            first_boot  = (boolean) out.getBoolean("first_boot");
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
    static public String compute_hash(String path){
        try {
            return String.valueOf(Files.size(Paths.get(path )));
        } catch (IOException e) {
            return "";
        }
    }
    // 0 = success, 1 = skipped, 2= error
    private  int get_single_image(URL remote_url, String path, String hash, int retry) {
        Log.i(TAG, "Getting: "+ remote_url.toString());

        String day_dir_str = (new File (path)).getParent();
        assert day_dir_str != null;
        File day_dir = new File(day_dir_str);
        if (!(day_dir).isDirectory()){
            boolean success = day_dir.mkdirs();
            if(!success){
                // we recheck in case another thread created the dir just now
                if(!(day_dir).isDirectory()) {
                    Log.e(TAG, "Could not create subdirectory: " + day_dir_str);
                    return 2;
                }
            }
        }

        if(retry > 3){
            Log.e(TAG, "Max retry reached. Failed to get image " + remote_url.toString());
            return 2;
        }

        final String tmp_path  = path + ".tmp";

        File trace = new File(path + ".trace");
        if(trace.exists()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(trace));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String trace_hash = null;
            try {
                trace_hash = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(!Objects.equals(trace_hash, hash)){
                if(trace.delete()) {
                    Log.e(TAG, "Local trace" + trace.getName() + " has a different hash than device hash. Trace deleted!");
                }
            }
            else {
                Log.i(TAG, "Skipping trace image: " + trace.getName());
                return 1;
            }

        }

        if (new File(path).exists()){
            if(compute_hash(path).equals(hash)){
                Log.i(TAG, "Skipping preexisting image: " + remote_url);
//                n_skipped ++;
                last_pace = Instant.now().getEpochSecond();
                return 1;
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
            Log.e(TAG, "FileNotFoundException in image download");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "IOException in image download");
        }

        String local_hash = compute_hash(tmp_path);

        if(hash.equals(local_hash)) {
            File tmp_file = new File(tmp_path);
            make_thumbnail(tmp_file, new File(path + ".thumbnail"));
            File out = new File(path);
            tmp_file.renameTo(out);

            File last_image_file = new File(last_image_path);
            if(last_image_file.getName().compareTo(out.getName()) < 0 )
                last_image_path = out.getAbsolutePath();

            last_pace = Instant.now().getEpochSecond();
            return 0;
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
            JSONObject out = readJsonFromUrl(images_url.toString(), (int) INTERNAL_TIMEOUT);
            if(out== null)
                return false;
            Iterator<String> it = out.keys();

            // todo images by date sort here
//            so we get the old ones first!
            List<String> keys = new ArrayList<String>();
            while (it.hasNext()) {
                keys.add(it.next());
            }

            ThreadPoolExecutor executor =
                    (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
            Collections.sort(keys);
            n_to_download = keys.size();
            for(String k : keys){

                String hash = out.getString(k);
                String filename = device_id + "." + k + ".jpg";
                String day_str = k.split("_")[0] ;
                final URL image_url;

                if(version.compareTo("3.0.1") >= 0)
                    image_url = new URL("http", host_address, port, "static/"  + device_id +  "/" + day_str + "/"+ filename);
                else
                    image_url = new URL("http", host_address, port, "static/"  + device_id  + "/"+ filename);


                executor.submit( () -> {
                    int status = get_single_image(image_url, target_dir + "/" + day_str + "/" + filename, hash, 0);
                    if(status == 0) n_downloaded.getAndIncrement();
                    else if(status == 1) n_skipped.getAndIncrement();
                    else if(status == 2) n_errored.getAndIncrement();
                    else Log.e(TAG, "Unexpected return status for " + filename);
                    write_persistent_device_status();
                    long now =  Instant.now().getEpochSecond();
                    internal_pacemaker = Instant.now().getEpochSecond();

                    if (now - last_keep_alive > KEEP_ALIVE_TIMEOUT) {
                        last_keep_alive = now;
                        keep_alive();
                    }
//                    fixme.getAndIncrement();
                }
                );

            }
            executor.shutdown();
            executor.awaitTermination(DOWNLOAD_ALL_IMG_TIMEOUT, TimeUnit.SECONDS);
        } catch (IOException | JSONException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return n_errored.get() == 0;
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

            conn.setConnectTimeout((int) SHORT_TIMEOUT);
            conn.setReadTimeout((int) SHORT_TIMEOUT);
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

            conn.setConnectTimeout((int) SHORT_TIMEOUT);
            conn.setReadTimeout((int) 5000);
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
    // we create a small file that saves information about a device
    // in <data_dir>/<device_name>/status.json
    public void write_persistent_device_status(){
        JSONObject out = new JSONObject();
        try {
            out.put("device_id", device_id);
            out.put("time_created", time_created);
            out.put("n_errored", n_errored);
            out.put("n_downloaded", n_downloaded);
            out.put("n_to_download", n_to_download);
            out.put("n_skipped", n_skipped);
            out.put("status", status);
            out.put("battery_level", battery_level);
            out.put("available_disk_space", available_disk_space);
            out.put("last_pace", last_pace);
            out.put("last_image_path", last_image_path);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Writer output = null;
        File file = new File(target_dir +"/status.json");
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(out.toString());
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        createDirIfNotExists(target_dir);

        internal_pacemaker = Instant.now().getEpochSecond();
        while (Instant.now().getEpochSecond() - internal_pacemaker < INTERNAL_TIMEOUT){
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (! get_metadata())
                    continue;
                internal_pacemaker = Instant.now().getEpochSecond();
                // delete local files on mock devices

//                if(is_mock || device_id.equals("512858f4"))
                if(is_mock)
                    delete_local_files();

                write_persistent_device_status();
                if (! get_log())
                    continue;
                write_persistent_device_status();
                try {
                    if (! set_metadata())
                        continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                status = "syncing";

                if(get_images()) {
                    Log.i(TAG,"Clearing disk");
                    write_persistent_device_status();
                    clear_disk();
                    stop_server();
                    status = "done";
                    write_persistent_device_status();
                    return;
                }
                else {
                    status = "errored";
                    Log.e(TAG, "Device "+ device_id + " errored");
                    write_persistent_device_status();
                }
        }
        status = "errored";
        write_persistent_device_status();
        Log.e(TAG, "Device "+ device_id + " timeout");
    }


}
