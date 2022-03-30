package com.example.sticky_pi_data_harvester;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.text.format.Formatter;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.sticky_pi_data_harvester.databinding.ActivityMainBinding;

import java.util.Enumeration;
import java.util.Hashtable;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


//public class MainActivity extends AppCompatActivity{
public class MainActivity extends AppCompatActivity {

    static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    static final String TAG = "StickyPiDataHarvester";
    static final String SERVICE_TYPE = "_http._tcp.";
    static final String SERVICE_NAME_PREFIX = "StickyPi-";
//    static final String SPI_HARVESTER_NAME_PATTERN = "spi-harvester";
    String location_provider;
    AppBarConfiguration appBarConfiguration;
    Location location;
    LocationManager locationManager;
    LocationListener locationListener;
    ActivityMainBinding binding;
    NsdManager mNsdManager;
    NsdManager.DiscoveryListener spiDiscoveryListener;

    Context gps_context;
    final IntentFilter intentFilter = new IntentFilter();
//    CountDownLatch latch = new CountDownLatch(1);
//    WifiP2pManager.Channel channel;
//    WifiP2pManager manager;
//    WiFiDirectBroadcastReceiver receiver;

    Hashtable<String, DeviceHandler> device_dict = new Hashtable<>();

    public void initializeDiscoveryListener() {
        Log.w(TAG, "Init Discovery");

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

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {

                String name = serviceInfo.getServiceName();
                String type = serviceInfo.getServiceType();
                Log.e(TAG, "Found: "+ name + " " + type);
                if (type.equals(SERVICE_TYPE) && name.startsWith(SERVICE_NAME_PREFIX)) {

                    DeviceHandler dev_handl = new DeviceHandler(serviceInfo, location, mNsdManager);

                    if(device_dict.containsKey(dev_handl.device_id)) {
                        //fixme here we stop if we already have a device with the same name
                        Log.w(TAG, "Device " + dev_handl.device_id + " already registered.");
                        //
                        return;
                    }

                    device_dict.put(dev_handl.device_id, dev_handl);
                    Log.e(TAG,"MAP: " + dev_handl.device_id);
                    device_dict.get(dev_handl.device_id).start();
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Service lost");
            }
        };
    }

    public void initializeLocationListener() {
        Log.w(TAG, "Init Location listener");
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location loc) {
                Log.i(TAG, "updating location");
                location = loc;
                TextView location_textview = (TextView) findViewById(R.id.location_text_view);
                String text = "";
                text += location.getLatitude() + ", "+ location.getLongitude() + ", " + location.getAltitude();
                location_textview.setText(text);
            }


            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // wifi direct https://developer.android.com/training/connect-devices-wirelessly/wifi-direct
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


//        // fixme changing the name of the group fails. setting device name by hand for now
//        try {
//            manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//            channel = manager.initialize(this, getMainLooper(),
//                    new WifiP2pManager.ChannelListener() {
//                @Override
//                public void onChannelDisconnected() {
//                    manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//                }
//            });
//            Class[] paramTypes = new Class[3];
//            paramTypes[0] = WifiP2pManager.Channel.class;
//            paramTypes[1] = String.class;
//            paramTypes[2] = WifiP2pManager.ActionListener.class;
//            Method setDeviceName = manager.getClass().getMethod(
//                    "setDeviceName", paramTypes);
//            setDeviceName.setAccessible(true);
//
//            Object arglist[] = new Object[3];
//            arglist[0] = channel;
//            arglist[1] = SPI_HARVESTER_NAME_PATTERN;
//            arglist[2] = new WifiP2pManager.ActionListener() {
//                @Override
//                public void onSuccess() {
//                    Log.d("setDeviceName succeeded", "true");
//                }
//
//                @Override
//                public void onFailure(int reason) {
//                    Log.w("Device name change FAILED", String.valueOf(reason));
//                }
//            };
//            setDeviceName.invoke(manager, arglist);
//
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }




        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        gps_context = this;
        mNsdManager = (NsdManager) (getApplicationContext().getSystemService(Context.NSD_SERVICE));
        requestLocationPermission();
        locationManager = (LocationManager) gps_context.getSystemService(Context.LOCATION_SERVICE);
        location_provider = locationManager.getBestProvider(new Criteria(), false);

        initializeLocationListener();

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                2000,
                10, locationListener);
//
//
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, spiDiscoveryListener);


//        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//        channel = manager.initialize(this, getMainLooper(), null);
//
//
//        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
//            @Override
//            public void onSuccess() {
//                // Device is ready to accept incoming connections from peers.
//                Log.i(TAG, "P2P group created!");
//            }
//            @Override
//            public void onFailure(int reason) {
////                Toast.makeText(WiFiDirectActivity.this, "P2P group creation failed. Retry.",
////                        Toast.LENGTH_SHORT).show();
//                // TODO OK if group already exists
//                Log.e(TAG, "P2P group creation FAILED");
//            }
//        });


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });



    }

    @Override
    public void onResume() {
        super.onResume();
//        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
//        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
//        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @AfterPermissionGranted(MY_PERMISSIONS_REQUEST_LOCATION)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            Log.w(TAG, "permission window");
            EasyPermissions.requestPermissions(this, "Please grant the location permission", MY_PERMISSIONS_REQUEST_LOCATION, perms);
        }
    }

}
