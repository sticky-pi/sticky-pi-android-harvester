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
    ArrayList<FileHandler> file_handler_list = new ArrayList<FileHandler>();
    File storage_dir = null;

    APIClient api_client;


    boolean has_internet = false;

    boolean is_host_up = false;
    boolean are_credentials_valid = false;
//    boolean has_write_access = false;
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
                Log.i(TAG, "Adding unhandled device: " + device_id);
                return true;
            }
        }
        return false;
    }


    private void update_device_file_table(){
        File directory = new File(String.valueOf(storage_dir));
        File[] device_dirs = directory.listFiles();
        for (File dir : device_dirs) {
            if(dir.isDirectory()){
                if(! is_device_handled(dir.getName())){
                    FileHandler file_handler = new FileHandler(dir.getPath(), api_client);
                    file_handler.start();
                    file_handler_list.add(file_handler);
                }
            }
        }
    }
    public boolean is_domain_up(String domain) {
        try {
            InetAddress ipAddr = InetAddress.getByName(domain);
            //You can replace it with your name
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }


    private void update_network_status(){

        has_internet = is_domain_up("google.com");
        Log.e("TODEL", "has_internet: " + has_internet);
        is_host_up = is_domain_up(api_client.get_api_host());
        Log.e("TODEL", "is_host_up: " + is_host_up);
        are_credentials_valid = api_client.get_token();
        Log.e("TODEL", "are_credentials_valid:  " + are_credentials_valid);
    }






    class Updater extends Thread{
        Updater(){
            super();
        }

        @Override
        public void run(){
            while(true){
                SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_TAG, Context.MODE_PRIVATE);
                String api_host =  sharedpreferences.getString("preference_api_host", "");
                String user_name =  sharedpreferences.getString("preference_user_name", "");
                String password =  sharedpreferences.getString("preference_password", "");
                // invalidate client if settings have changed

                if (api_client == null){
                    api_client = new APIClient(api_host, user_name, password);
                }
                else if(api_host != api_client.get_api_host() ||
                        user_name != api_client.get_user_name() ||
                        password != api_client.get_password()){
                    api_client =  null;
                    continue;

                }
                update_network_status();
                if (are_credentials_valid && has_internet && is_host_up)
                    update_device_file_table();
                try {
                    sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate() {
        storage_dir = getApplicationContext().getExternalFilesDir(null);
        Log.e("TODEL", "Created! file service");
        updater = new Updater();
        updater.start(); // could also use an observer
        // fixme this is not stopped on close of the ap...?!
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
