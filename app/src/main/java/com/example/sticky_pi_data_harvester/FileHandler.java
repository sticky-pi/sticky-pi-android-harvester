package com.example.sticky_pi_data_harvester;

import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FileHandler extends Thread{
    static final String TAG = "FileHandler";
    long last_img_seen;
    String m_directory;
    String device_id;
    APIClient m_api_client;
    String m_api_host;

    int n_jpg_images = 0;
    int n_traced_jpg_images = 0;
    int n_trace_images = 0;
    long disk_used = 0;
    boolean m_delete_uploaded_images = false;

    SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    SimpleDateFormat day_formatter = new SimpleDateFormat("yyyy-MM-dd");

    // private DeviceHandler devH = new DeviceHandler();

    FileHandler(String directory, APIClient api_client, boolean delete_uploaded_images){
        super();
        m_directory = directory;
        //todo, set an observer to reindex/update on update
        device_id = new File(m_directory).getName();
        index_files();
        // this is a stub only
        last_img_seen = 0;
        m_api_client = api_client;
        m_delete_uploaded_images = delete_uploaded_images;
    }

    void delete_all_traces() {
        File directory = new File(m_directory);

        File[] day_dirs = directory.listFiles();

        if (day_dirs != null) {
            for (File day_dir : day_dirs) {
                if(!day_dir.isDirectory())
                    continue;
                File[] images = day_dir.listFiles(new FilenameFilter() {
                                                      @Override
                                                      public boolean accept(File dir, String name) {
                                                          return name.matches("^.*\\.trace$");
                                                      }
                                                  }
                );
                if (images != null) {
                    for (File img_or_trace : images) {
                        img_or_trace.delete();
                    }
                }
            }
        }
    }

    String get_device_id(){return device_id;}
    long get_last_seen(){return last_img_seen;}
    int get_n_jpg_images(){return n_jpg_images;}
    int get_n_traced_jpg_images(){return n_traced_jpg_images;}
    int get_n_trace_images(){return n_trace_images;}
    long get_disk_use(){
        return disk_used ;
    }

    void index_files(){
        int tmp_n_jpg_images = 0;
        int tmp_n_traced_jpg_images = 0;
        int tmp_n_trace_images = 0;
        long tmp_disk_used = 0;
        long most_recent_seen = 0;

        File directory = new File(m_directory);
//        List<File> imgs = new ArrayList<>();


        File[] day_dirs = directory.listFiles();
        if(day_dirs != null) {
            for(File day_dir: day_dirs){
                if(!day_dir.isDirectory()){
                    continue;
                }
                File[] images = day_dir.listFiles(new FilenameFilter() {
                                                        @Override
                                                        public boolean accept(File dir, String name) {
                                                            return name.matches("^(.*\\.jpg)|(.*\\.trace)$");
                                                        }
                                                    }
                );
                if (images != null) {
                    for (File img_or_trace : images) {
                        if (img_or_trace.getName().endsWith(".jpg")) {
                            tmp_n_jpg_images += 1;
                            if (new File(img_or_trace.getPath() + ".trace").isFile()) {
                                tmp_n_traced_jpg_images += 1;

                            }
                            long latest_seen = parse_date(img_or_trace.getName());
                            if (latest_seen > most_recent_seen) {
                                most_recent_seen = latest_seen;
                            }
                            tmp_disk_used += img_or_trace.length();
                        }
                        if (img_or_trace.getName().endsWith(".trace")) {
                            tmp_n_trace_images += 1;
                        }
                    }
                }
            }
        }
        disk_used = tmp_disk_used;
        n_jpg_images = tmp_n_jpg_images;
        n_traced_jpg_images = tmp_n_traced_jpg_images;
        n_trace_images = tmp_n_trace_images;
        last_img_seen = most_recent_seen;
    }


    public static String calculateMD5(File updateFile) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Exception while getting digest", e);
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }

    void writeTraceForFile(File trace, File image, String hash, boolean delete_parent){
        File tmp_file = new File(trace.getPath() + "~");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmp_file))) {
            writer.write(hash+ "\n");
            writer.flush();
            writer.close();
            if(tmp_file.renameTo(trace) && delete_parent){
                Log.i(TAG, "Successfully written trace file: " +  trace.getPath() +". Deleting parent image: "+ image.getName());
                if(! image.delete()) {
                    Log.i(TAG, "Could not delete "+ image.getName());
                    }
                new File(image.getPath() + ".thumbnail").delete();
            }

        } catch (IOException e){
            Log.e(TAG, "Error writing trace file: " + trace.getPath());
        }

    }

    public void set_delete_uploaded_images(boolean delete_uploaded_images) {
        m_delete_uploaded_images = delete_uploaded_images;
    }


    private boolean should_delete_parent(){
        return m_delete_uploaded_images;
    }


    private void upload_one_jpg(long timestamp){
        String date = date_formatter.format(new Date(timestamp));
        String day_str = day_formatter.format(new Date(timestamp));
        String img_path = m_directory + "/" + day_str + "/" +device_id + "." + date + ".jpg";
        String trace_img_path = img_path + ".trace";
        File image = new File(img_path);
        File trace = new File(trace_img_path);
        //todo, here we want some sort of file lock ?!
        String hash = DeviceHandler.compute_hash(trace_img_path);
        boolean delete_parent = should_delete_parent();

        if(trace.exists()) {
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
            if(trace_hash.equals(hash)){
                Log.i(TAG, "Skipping file with valid trace: " + image.getName());
                if(delete_parent){
                    image.delete();
                }
                return;
            }
            else {
                Log.e(TAG, "Deleting erroneous trace: " + trace.getName());
                trace.delete();
            }
        }

        if (!image.isFile()) {
            Log.e(TAG, "Cannot find image to upload: " + img_path);
            return;
        }
        String query_str = String.format("[{\"device\": \"%s\", \"datetime\": \"%s\"}]", device_id, date);
        try {
            JSONArray payload = new JSONArray(query_str);
            JSONArray response = (JSONArray) m_api_client.api_call((Object) payload, "get_images/metadata");
            boolean to_upload = false;
            String md5 = calculateMD5(image);


            if (response.length() == 0) {
                to_upload = true;
            }
            else {
                String server_md5;
                server_md5 = ((JSONObject) response.get(0)).getString("md5");
                if(!server_md5.equals(md5)){
                    Log.e(TAG, "Local file with different md5 than same name already on the server: " + image.getName());
                    to_upload = true;
                }
                else {
                    Log.i(TAG, "Skipping file that exists on server: " + image.getName());
                    writeTraceForFile(trace, image, hash, delete_parent );
                }
            }

            if (to_upload) {
                Log.i(TAG, "Uploading " + image.getName() + " (" + md5 + ")");
                if(m_api_client.put_image(image, md5)){
                    writeTraceForFile(trace, image, hash, delete_parent);
                }
                else {
                    Log.e(TAG, "Failed to upload: " + image.getName());
                }

            }

        } catch (JSONException e) {
            Log.e(TAG, String.valueOf(e));
            e.printStackTrace();
        }
    }


    private void upload_all_jpg(){
        File directory = new File(m_directory);

        File[] day_dir = directory.listFiles();
        if (day_dir != null) {
            for(File d: day_dir){
                if(!d.isDirectory())
                    continue;
                upload_all_jpg_in_day_dir(d);
            }
        }

    }
    private void upload_all_jpg_in_day_dir(File directory) {
        File[] images = directory.listFiles(new FilenameFilter() {
                                                @Override
                                                public boolean accept(File dir, String name) {
                                                    return name.matches("^.*(\\.jpg)$");
                                                }
                                            }
        );

        // we get all datetimes and sort by timestamp.
        /// we don't store filenames for memory efficiency
        List<Long> image_timestamps = new ArrayList<Long>();

        if (images != null) {
            for (File img : images) {
                String fields[] = img.getName().split("\\.");
                if (fields.length > 2) {
                    try {
                        Date date = date_formatter.parse(fields[1]);
                        Log.e("FIXME", date + "");
                    } catch (ParseException e) {
                        Log.e(TAG, "Cannot parse date in: " + img.getName() + " (" + fields[1] + ")");
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "Unexpected image name: " + img.getName());
                }
            }
        }

        Collections.sort(image_timestamps);
        ThreadPoolExecutor executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        for (long t : image_timestamps) {
            executor.submit( () -> {
                upload_one_jpg(t);

            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    // Adds on here to get updated parsed date of img name
    private long parse_date(String name) {
        long timeInSeconds = 0;
        String[] arrSplit = name.split("\\.");
        if (arrSplit.length > 2) {
            String date = arrSplit[1];
            LocalDateTime ldt = LocalDateTime.parse(date,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            timeInSeconds = ldt.toEpochSecond(ZoneOffset.UTC);
        }
        return timeInSeconds;
    }


    @Override
    public void run() {
        while (true){
            upload_all_jpg();
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

