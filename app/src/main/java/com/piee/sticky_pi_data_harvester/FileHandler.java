package com.piee.sticky_pi_data_harvester;

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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FileHandler extends Thread{
    static final String TAG = "FileHandler";
    static final String INDEX_FILENAME = "file_index.csv";
    static DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    long last_img_seen;
    String m_directory;
    String device_id;
    APIClient m_api_client;


    int n_jpg_images = 0;
    int n_traced_jpg_images = 0;
    int n_trace_images = 0;
    int n_errored_jpg_images = 0;

    AtomicInteger n_uploaded_progress;
    AtomicBoolean is_reading_index_file;
    long disk_used = 0;
    boolean m_delete_uploaded_images = false;
    boolean paused = false;

    String upload_status = "starting";

    public void pause() {
        this.paused = true;
    }
    public void resume_run() {
        this.paused = false;
    }

    boolean isPaused() {return this.paused;}

    // private DeviceHandler devH = new DeviceHandler();

    FileHandler(String directory, APIClient api_client, boolean delete_uploaded_images){
        super();
        is_reading_index_file = new AtomicBoolean(false);
        m_directory = directory;
        //todo, set an observer to reindex/update on update
        device_id = new File(m_directory).getName();
        index_files(null, true);
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
    int get_n_errored_jpg_images(){return n_errored_jpg_images;}
    int get_n_trace_images(){return n_trace_images;}
    String get_upload_status(){
        if(n_uploaded_progress == null)
            return "starting";
        return String.format( "%d/%d", n_uploaded_progress.get() - n_traced_jpg_images , n_jpg_images - n_traced_jpg_images);

    }
    long get_disk_use(){
        return disk_used ;
    }

    void index_files(){
        index_files(null, false);
    }
    void index_files(ArrayList<ImageRep> out, boolean from_index_file){
        File index_path = new File(new File(m_directory).getPath() + "/" + INDEX_FILENAME);

        if(from_index_file && index_path.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(index_path))) {
                is_reading_index_file.set(true);
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.replace("\n", "").replace("\r", "");
                    if(line.startsWith("#")) {
                        line = line.substring(1);

                        String[] values = line.split(",", 6);
                        int i=0;
                        disk_used = Long.parseLong(values[i++]);
                        n_jpg_images = Integer.parseInt(values[i++]);
                        n_traced_jpg_images = Integer.parseInt(values[i++]);
                        n_errored_jpg_images = Integer.parseInt(values[i++]);
                        n_trace_images = Integer.parseInt(values[i++]);
                        last_img_seen = Long.parseLong(values[i++]);

                    }
                    else{
                        if(out != null) {
                            out.add(new ImageRep(m_directory, device_id, line));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                is_reading_index_file.set(false);
            }
        }
        else {
            int tmp_n_jpg_images = 0;
            int tmp_n_traced_jpg_images = 0;
            int tmp_n_errored_jpg_images = 0;
            int tmp_n_trace_images = 0;
            long tmp_disk_used = 0;
            long most_recent_seen = 0;
            int n_indexed = 0;

            File tmp_file = new File(index_path.getPath() + "~");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmp_file))) {
                //        ArrayList<ImageRep> out = new ArrayList<>();

                File directory = new File(m_directory);

                File[] day_dirs = directory.listFiles();
                if (day_dirs != null) {
                    for (File day_dir : day_dirs) {
                        if (!day_dir.isDirectory()) {
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
                                    long datetime = parse_date(img_or_trace.getName());

                                    if (new File(img_or_trace.getPath() + ".trace").isFile()) {
                                        tmp_n_traced_jpg_images += 1;

                                    } else {
                                        // untraced jpg.
                                        //can use for unique timestamp
                                        ImageRep  im_rep = new ImageRep(m_directory, device_id, datetime);
                                        if (out != null)
                                            out.add(im_rep);
                                        writer.write(im_rep.serialise());
                                        n_indexed ++;

                                        if (im_rep.get_has_error()) {
                                            tmp_n_errored_jpg_images += 1;
                                        }
                                    }
                                    if (datetime > most_recent_seen) {
                                        most_recent_seen = datetime;
                                    }
                                    tmp_disk_used += img_or_trace.length();
                                }
                                if (img_or_trace.getName().endsWith(".trace")) {
                                    tmp_n_trace_images += 1;
                                    long datetime = parse_date(img_or_trace.getName());
                                    ImageRep  im_rep = new ImageRep(m_directory, device_id, datetime);
                                    if (out != null)
                                        out.add(im_rep);
                                    writer.write(im_rep.serialise());
                                    n_indexed ++;
                                }
                            }
                        }
                    }
                }

                Log.i(TAG,"Serialised " + n_indexed +" image representations");
                writer.write("#" + tmp_disk_used  + "," + tmp_n_jpg_images+ "," + tmp_n_traced_jpg_images + "," +
                        tmp_n_errored_jpg_images + "," + tmp_n_trace_images + "," + most_recent_seen + "\n");

                writer.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }


            disk_used = tmp_disk_used;
            n_jpg_images = tmp_n_jpg_images;
            n_traced_jpg_images = tmp_n_traced_jpg_images;
            n_errored_jpg_images = tmp_n_errored_jpg_images;
            n_trace_images = tmp_n_trace_images;
            last_img_seen = most_recent_seen;


            while (is_reading_index_file.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            tmp_file.renameTo(index_path);
        }

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
    void writeErrorForFile(File error, String message){
        if(error.isFile())
            return;
        File tmp_file = new File(error.getPath() + "~");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmp_file))) {
            writer.write(message + "\n");
            writer.flush();
            writer.close();
            tmp_file.renameTo(error);
        } catch (IOException e) {
            e.printStackTrace();
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
//
//        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
//        SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
//        date_formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

//        String date = date_formatter.format(new Date(timestamp));
        LocalDateTime date = Instant.ofEpochMilli(timestamp * 1000).atZone(ZoneOffset.UTC).toLocalDateTime();
        String date_str = date.format(date_formatter);

        String day_str = date_str.substring(0, 10);
        String img_path = m_directory + "/" + day_str + "/" +device_id + "." + date_str + ".jpg";
        String trace_img_path = img_path + ".trace";
        String error_img_path = img_path + ".error";
        File image = new File(img_path);
        File trace = new File(trace_img_path);
        File error_file = new File(error_img_path);

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
                if(error_file.isFile())
                    error_file.delete();
                return;
            }
            else {
                Log.e(TAG, "Deleting erroneous trace: " + trace.getName());
                trace.delete();
            }
        }

        if (!image.exists()) {
            Log.e(TAG, "Cannot find image to upload: " + img_path + ". Timestamp: " + timestamp);
            return;
        }
        String query_str = String.format("[{\"device\": \"%s\", \"datetime\": \"%s\"}]", device_id, date_str);
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
                    if(error_file.isFile())
                        error_file.delete();
                }
            }

            if (to_upload) {
                Log.i(TAG, "Uploading " + image.getName() + " (" + md5 + ")");
                if(m_api_client.put_image(image, md5)){
                    writeTraceForFile(trace, image, hash, delete_parent);
                    if(error_file.isFile())
                        error_file.delete();
                }
                else {
                    Log.e(TAG, "Failed to upload: " + image.getName());
                    writeErrorForFile(error_file, "Failed to upload");
                }

            }

        } catch (JSONException e) {
            Log.e(TAG, String.valueOf(e));
            e.printStackTrace();
        }
    }


    private void upload_all_jpg(){
        upload_status = "starting";
        n_uploaded_progress = new AtomicInteger(0);
        //    o make some sort of status here!
        File directory = new File(m_directory);

        File[] day_dir = directory.listFiles();
        if (day_dir != null) {
            for(File d: day_dir){
                if(!d.isDirectory()) {
                    continue;
                }
                upload_all_jpg_in_day_dir(d);
            }
        }
        upload_status = "done";

    }
    private void upload_all_jpg_in_day_dir(File directory) {
//        SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
//        date_formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
//


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
                    long timestamp = parse_date(img.getName());
                    image_timestamps.add(timestamp);
                } else {
                    Log.e(TAG, "Unexpected image name: " + img.getName());
                }
            }
        }

        Collections.sort(image_timestamps);

        for (long t : image_timestamps) {
                upload_one_jpg(t);
                n_uploaded_progress.incrementAndGet();
            }
    }

    // Adds on here to get updated parsed date of img name
    private long parse_date(String name) {
        long timeInSeconds = 0;
        String[] arrSplit = name.split("\\.");
        if (arrSplit.length > 2) {
            String date = arrSplit[1];
            LocalDateTime ldt = LocalDateTime.parse(date, date_formatter);
            timeInSeconds = ldt.toEpochSecond(ZoneOffset.UTC);
        }
        return timeInSeconds;
    }


    class Uploader extends Thread {
        Uploader() {
            super();
        }

        @Override
        public void run() {
            while (true){
                try {
                    sleep(10000);
                    if(paused){
                        sleep(1000);
                        continue;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                upload_all_jpg();
            }
        }
    }
    @Override
    public void run() {
        Uploader uploader = new Uploader();
        uploader.start();
        while (true){
            try {

                index_files();
                sleep(10000);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}

