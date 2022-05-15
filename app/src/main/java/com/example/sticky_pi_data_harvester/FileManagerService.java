package com.example.sticky_pi_data_harvester;

import static java.lang.Thread.sleep;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class FileManagerService extends Service {
    static final String TAG = "StickyPiDataHarvester::FileManagerService";

    Hashtable<String, FileHandler> file_handler_dict = new Hashtable<>();
    File storage_dir = null;

    MyBinder binder = new MyBinder();

    public class MyBinder extends Binder {
        public FileManagerService getService() {
            return FileManagerService.this;
        }}

    private void initialise_file_table(){
        File directory = new File(String.valueOf(storage_dir));
        File[] device_dirs = directory.listFiles();
        for (File dir : device_dirs) {
            if(dir.isDirectory()){
                FileHandler file_handler = new FileHandler(dir.getPath());
                file_handler_dict.put(dir.getName(), file_handler);
            }
        }
    }


//    public Hashtable<String, DeviceHandler> get_device_dict() {
//        return device_dict;
//    }


    @Override
    public void onCreate() {
        storage_dir = getApplicationContext().getExternalFilesDir(null);
        Log.e("TODEL", "Created! file service");
        initialise_file_table();
//    while(true){
//        Log.e("TEST", "test");
//        try {
//            sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
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
