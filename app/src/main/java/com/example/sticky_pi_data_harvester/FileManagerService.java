package com.example.sticky_pi_data_harvester;

import static java.lang.Thread.sleep;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

public class FileManagerService extends Service {
    static final String TAG = "StickyPiDataHarvester::FileManagerService";
    Updater updater;
    final int DEVICE_TABLE_UPDATE_PERIOD = 10; // s
    ArrayList<FileHandler> file_handler_list = new ArrayList<FileHandler>();

    public File getStorage_dir() {
        return storage_dir;
    }

    File storage_dir = null;
    boolean delete_uploaded_images = false;
    APIClient api_client;

    boolean has_internet = false;

    boolean is_host_up = false;
    boolean are_credentials_valid = false;
//    boolean has_write_access = false;
    long last_device_table_update = 0;
    MyBinder binder = new MyBinder();

    public class MyBinder extends Binder {
        public FileManagerService getService() {
            return FileManagerService.this;
        }}

    ArrayList<FileHandler> get_file_handler_list(){return file_handler_list;}

    public APIClient get_api_client() {
        return api_client;
    }


    private boolean is_device_handled(String device_id){

        if(file_handler_list == null) {
            return false;
        }
        for (FileHandler fh: file_handler_list) {
            if(Objects.equals(fh.get_device_id(), device_id)) {
                return true;
            }
        }
        return false;
    }


    private void update_device_file_table(boolean start_upload, boolean pause_handlers){

        File directory = new File(String.valueOf(storage_dir));
        File[] device_dirs = directory.listFiles();
            for (File dir : device_dirs) {
                if (dir.isDirectory()) {
                    if (!is_device_handled(dir.getName())) {
                        FileHandler file_handler = new FileHandler(dir.getPath(), api_client, delete_uploaded_images);
                        file_handler_list.add(file_handler);
                    }
                }
            }
            // in case user ticked/unticked option, we set it here
            for (FileHandler fh : file_handler_list) {
                fh.set_delete_uploaded_images(delete_uploaded_images);
            }

        if(! pause_handlers) {
            if (file_handler_list != null && start_upload) {
                for (FileHandler fh : file_handler_list) {

                    if (!fh.isAlive()) {
                    fh.start();
                    }
                    if(!fh.isPaused()){
                        fh.resume_run();
                    }
                }
            }

            if (start_upload)
                last_device_table_update = System.currentTimeMillis() / 1000;
        }
        else{
            if (file_handler_list != null) {
                for (FileHandler fh : file_handler_list) {
                    if (fh.isAlive()) {
                        fh.pause();
                    }
                }
            }

        }

    }
    public boolean is_domain_up(String domain) {
        try {
            InetAddress ipAddr = InetAddress.getByName(domain);
            //You can replace it with your name
            return !ipAddr.toString().equals("");

        } catch (Exception e) {
            return false;
        }
    }


    private void update_network_status(){
        has_internet = is_domain_up("google.com");
        if(has_internet) {
            is_host_up = is_domain_up(api_client.get_api_host());
            try {
                are_credentials_valid = api_client.get_token();
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
                are_credentials_valid = false;
            }
        }
    }


    class Updater extends Thread{
        Updater(){
            super();
        }

        @Override
        public void run(){
//            int i = 0;
            while(true){
//                Log.e("ME", "Thread updater: "+ i++ + " " + Thread.currentThread());
                SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_TAG, Context.MODE_PRIVATE);
                String api_host =  sharedpreferences.getString("preference_api_host", "");
                String user_name =  sharedpreferences.getString("preference_user_name", "");
                String password =  sharedpreferences.getString("preference_password", "");
                delete_uploaded_images =  sharedpreferences.getBoolean("preference_delete_uploaded_images", false);
                String protocol= "https";


                // fixed DEV
//                api_host = "192.168.42.86";
//                user_name = "test_wr_user";
//                password = "test";
//                String protocol= "http";

                if (api_client == null){
                    api_client = new APIClient(api_host, user_name, password, protocol);
                }
                else if(!Objects.equals(api_host, api_client.get_api_host()) ||
                        !Objects.equals(user_name, api_client.get_user_name()) ||
                        !Objects.equals(password, api_client.get_password())){
                    Log.i(TAG, "Invalidating API client. Setting changed");
                    api_client =  null;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;

                }

                update_network_status();
                long now = System.currentTimeMillis() / 1000;
                boolean upload_files = (now - last_device_table_update) > DEVICE_TABLE_UPDATE_PERIOD;
                boolean pause_handlers = !(has_internet  && are_credentials_valid  && is_host_up);

                update_device_file_table(upload_files, pause_handlers);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate() {
        storage_dir = getApplicationContext().getExternalFilesDir(null);
        updater = new Updater();
        updater.start(); // could also use an observer
        // todo this is not stopped on close of the ap...?!
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int start_id) {
        return Service.START_STICKY;
    }

}
