package com.example.sticky_pi_data_harvester;

import static java.lang.Thread.sleep;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class DeviceManagerService extends Service {

    static final String TAG = "StickyPiDataHarvester::DeviceManagerService";
    static final String SERVICE_TYPE = "_http._tcp.";
    static final String SERVICE_NAME_PREFIX = "StickyPi-";
    static final String UPDATE_LOCATION_INTENT = "UPDATE_LOCATION_INTENT";
    int debug_i = 0;

    String location_provider;
    Location location;
    LocationManager locationManager;
    LocationListener locationListener;
    NsdManager mNsdManager;
    NsdManager.DiscoveryListener spiDiscoveryListener;
    Hashtable<String, DeviceHandler> device_dict = new Hashtable<>();
    Context gps_context;
    File storage_dir = null;


    public Location get_location() {
        return location;
    }
    MyBinder binder = new MyBinder();

    public class MyBinder extends Binder {
        public DeviceManagerService getService() {
            return DeviceManagerService.this;
        }
    }

    private void initialise_device_table(){
        Log.d("Files", "Path: " + storage_dir);
        File directory = new File(String.valueOf(storage_dir));
        File[] files = directory.listFiles();
        for (File file : files) {
            if(file.isDirectory()){

                File status_file = new File(file.getPath() + "/status.json");
                if(status_file.isFile()){

                    FileInputStream fi = null;
                    try {
                        fi = new FileInputStream(status_file);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fi));
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        reader.close();
                        JSONObject json = new JSONObject(sb.toString());
                        DeviceGhostHandler dev_handler = new DeviceGhostHandler(json);
                        if(dev_handler.get_device_id() != "")
                            device_dict.put(dev_handler.get_device_id(), dev_handler);

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Log.e(TAG, "No ghost status file in " + file.getPath());
                }
            }
        }
    }

    public void initializeDiscoveryListener() {
        spiDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery FAILED");
                mNsdManager.stopServiceDiscovery(this);
            }
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }
            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.w(TAG, "Discovery started");
            }
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.w(TAG, "Discovery Stopped");
            }

            private void startResolveService(NsdServiceInfo serviceInfo) {
                NsdManager.ResolveListener newResolveListener = new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        switch (errorCode) {
                            case NsdManager.FAILURE_ALREADY_ACTIVE:
                                Log.w(TAG, "Failed to resolve " + serviceInfo.getServiceName() + ". Retrying.");
                                // Just try again...
                                try {
                                    sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                startResolveService(serviceInfo);
                                break;
                            case NsdManager.FAILURE_INTERNAL_ERROR:
                                Log.e(TAG, "FAILURE_INTERNAL_ERROR");
                                break;
                            case NsdManager.FAILURE_MAX_LIMIT:
                                Log.e(TAG, "FAILURE_MAX_LIMIT");
                                break;
                        }
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        Log.i(TAG, "Service Resolved: " + serviceInfo);

                        DeviceHandler dev_handl = new DeviceHandler(serviceInfo, location, storage_dir);

                        if (device_dict.containsKey(dev_handl.get_device_id())) {

                            if (device_dict.get(dev_handl.get_device_id()).isAlive()) {
                                Log.w(TAG, "Device " + dev_handl.get_device_id() + " already registered and running.");
                                return;
                            } else {
                                Log.w(TAG, "Device handler for " + dev_handl.get_device_id() + " is dead. restarting.");
                                device_dict.remove(dev_handl.get_device_id());
                            }
                        }

                        device_dict.put(dev_handl.get_device_id(), dev_handl);
                        device_dict.get(dev_handl.get_device_id()).start();
                    }
                };
                mNsdManager.resolveService(serviceInfo, newResolveListener);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {

                String name = serviceInfo.getServiceName();
                String type = serviceInfo.getServiceType();
                Log.e(TAG, "Found: " + name + " " + type);
                if (type.equals(SERVICE_TYPE) && name.startsWith(SERVICE_NAME_PREFIX)) {
//                    DeviceHandler dev_handl = new DeviceHandler(serviceInfo, location, mNsdManager);
                    startResolveService(serviceInfo);

                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Service lost");
            }
        };
    }

    public void initializeLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location loc) {
                Log.i(TAG, "Updating location");
                location = loc;
                sendBroadcast(new Intent(DeviceManagerService.UPDATE_LOCATION_INTENT));
            }


            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
    }

    public Hashtable<String, DeviceHandler> get_device_dict() {
        return device_dict;
    }

    @Override
    public void onCreate() {
        storage_dir = getApplicationContext().getExternalFilesDir(null);
        initializeLocationListener();
        initialise_device_table();


        gps_context = this;
        mNsdManager = (NsdManager) (getApplicationContext().getSystemService(Context.NSD_SERVICE));
        locationManager = (LocationManager) gps_context.getSystemService(Context.LOCATION_SERVICE);
        location_provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //todo
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                2000,
                10, locationListener);

        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, spiDiscoveryListener);

    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("TODEL", "unbinding device manager");
        stopSelf();
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int start_id) {
        return Service.START_STICKY;
    }
}
