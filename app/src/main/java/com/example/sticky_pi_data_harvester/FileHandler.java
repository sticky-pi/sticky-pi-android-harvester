package com.example.sticky_pi_data_harvester;

import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.List;

public class FileHandler extends Thread{
    static final String TAG = "FileHandler";
    long last_img_seen;
    String m_directory;
    String device_id;
    APIClient m_api_client;
    String m_api_host;
    int n_jpg_images = 0;
    int n_trace_images = 0;


    FileHandler(String directory, APIClient api_client){
        super();
        m_directory = directory;
        //fixme, set an observer to reindex/update on update
        device_id = new File(m_directory).getName();
        index_files();
        // this is a stub only
        last_img_seen = 0;
        m_api_client = api_client;
    }

    String get_device_id(){return device_id;}
    long get_last_seen(){return last_img_seen;}
    int get_n_jpg_images(){return n_jpg_images;}
    int get_n_trace_images(){return n_trace_images;}
    long get_disk_use(){
        // fixme
        return 0 ;
    }

    void index_files(){
        int tmp_n_jpg_images = 0;
        int tmp_n_trace_images = 0;
       File directory = new File(m_directory);
       File[] images = directory.listFiles(new FilenameFilter() {
                                         @Override
                                         public boolean accept(File dir, String name) {
                                             return name.matches("^.*(\\.jpg)|(\\.trace)$");
                                         }
                                     }
       );

       if(images != null){
           for (File img : images){
               if(img.getName().endsWith(".jpg")){
                   tmp_n_jpg_images +=1;
               }
               if(img.getName().endsWith(".trace")){
                   tmp_n_trace_images +=1;
               }
           }
       }
        n_jpg_images = tmp_n_jpg_images;
        n_trace_images = tmp_n_trace_images;
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

    private void upload_all_jpg(){
        File directory = new File(m_directory);
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

        SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        if(images != null){
            for (File img : images){
                String fields[] = img.getName().split("\\.");
                if(fields.length > 2 ){
                    try {
                        Date date = date_formatter.parse(fields[1]);
                        image_timestamps.add(date.getTime());
                    } catch (ParseException e) {
                        Log.e(TAG,"Cannot parse date in: " + img.getName() + " (" + fields[1]+ ")");
                        e.printStackTrace();
                    }
                }
                else{
                    Log.e(TAG,"Unexpected image name: " + img.getName());
                }
            }
        }

        Collections.sort(image_timestamps);
        for(long t: image_timestamps){

            String date = date_formatter.format(new Date(t ));
            String img_path = m_directory + "/" + device_id + "." + date + ".jpg";
            File image = new File(img_path);
            //fixme, here we want some sort of file lock ?!
            if(! image.isFile()){
                Log.e(TAG, "Cannot find image to upload: " + img_path);
                continue;
            }
            String query_str = String.format( "[{\"device\": \"%s\", \"datetime\": \"%s\"}]", device_id, date);
            try {

                JSONArray payload = new JSONArray(query_str);
                JSONArray response = (JSONArray)  m_api_client.api_call((Object) payload, "get_images/metadata");
//                Log.e("TODEL", response.toString());
                boolean to_upload = false;
                if(response.length() == 0){
                    to_upload = true;
                }

                String md5 = calculateMD5(image);

                if(to_upload) {
                    List<File> all_images = new ArrayList<>();
                    all_images.add(image);
//                    JSONArray get_img_resp = (JSONArray) m_api_client.put_images(all_images);
                }

            } catch (JSONException e) {
                Log.e(TAG, String.valueOf(e));
                e.printStackTrace();
            }
        }

    }


    @Override
    public void run() {
//        while (true){
            upload_all_jpg();
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//        }
    }
}

