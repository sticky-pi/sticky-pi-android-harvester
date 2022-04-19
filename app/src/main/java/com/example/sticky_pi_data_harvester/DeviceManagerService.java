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
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.sticky_pi_data_harvester.databinding.ActivityMainBinding;

import java.util.Hashtable;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class DeviceManagerService extends Service {

    static final String TAG = "StickyPiDataHarvester:: DeviceManagerService";
    static final String SERVICE_TYPE = "_http._tcp.";
    static final String SERVICE_NAME_PREFIX = "StickyPi-";
    static final String UPDATE_LOCATION_INTENT = "UPDATE_LOCATION_INTENT";

    String location_provider;
    Location location;
    LocationManager locationManager;
    LocationListener locationListener;
    NsdManager mNsdManager;
    NsdManager.DiscoveryListener spiDiscoveryListener;
    Hashtable<String, DeviceHandler> device_dict = new Hashtable<>();
    Context gps_context;
    String hello = "voila";
    public Location get_location(){return location;}


    MyBinder binder=new MyBinder();

    public class MyBinder extends Binder
    {
        public DeviceManagerService getService()
        {
            return DeviceManagerService.this;
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

                        DeviceHandler dev_handl = new DeviceHandler(serviceInfo, location, getApplicationContext().getExternalFilesDir(null));

                        if (device_dict.containsKey(dev_handl.device_id)) {
                            if (device_dict.get(dev_handl.device_id).isAlive()) {
                                Log.w(TAG, "Device " + dev_handl.device_id + " already registered and running.");
                                return;
                            } else {
                                Log.w(TAG, "Device handler for " + dev_handl.device_id + " is dead. restarting.");
                                device_dict.remove(dev_handl.device_id);
                            }
                        }

                        device_dict.put(dev_handl.device_id, dev_handl);
                        device_dict.get(dev_handl.device_id).start();
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
        initializeLocationListener();
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
    public int onStartCommand(Intent intent, int flag, int start_id) {
        return Service.START_STICKY;
    }

}
