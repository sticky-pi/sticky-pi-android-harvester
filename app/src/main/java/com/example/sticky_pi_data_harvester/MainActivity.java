package com.example.sticky_pi_data_harvester;

import static java.lang.Thread.sleep;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.sticky_pi_data_harvester.databinding.ActivityMainBinding;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Hashtable;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity {
    static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    static final String TAG = "StickyPiDataHarvester";
    AppBarConfiguration appBarConfiguration;
    ActivityMainBinding binding;
    DeviceManagerService device_manager_service;
    boolean device_manager_service_bound =false;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            DeviceManagerService.MyBinder binder = (DeviceManagerService.MyBinder) service;
            device_manager_service = binder.getService();
            device_manager_service_bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            device_manager_service_bound = false;
        }
    };

    public Hashtable<String, DeviceHandler> get_device_dict() {
        if(device_manager_service == null) {
            Log.w(TAG, "Service not bound, so returning empty table");
            return new Hashtable<String, DeviceHandler>();
        }
        return device_manager_service.get_device_dict();
    }


    private WifiManager wifiManager;
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    @AfterPermissionGranted(MY_PERMISSIONS_REQUEST_LOCATION)
    public void turnOnHotspot() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {


                super.onStarted(reservation);
                  hotspotReservation = reservation;
                  WifiConfiguration currentConfig = hotspotReservation.getWifiConfiguration();

                  Log.e("DANG", "THE PASSWORD IS: "
                            + reservation.getSoftApConfiguration().getPassphrase()
                        + " \n SSID is : "
                        + reservation.getSoftApConfiguration().getSsid());


            }


            @Override
            public void onStopped() {
                super.onStopped();
                Log.e("DANG", "Local Hotspot Stopped");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.e("DANG", "Local Hotspot failed to start");
            }
        }, new Handler());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestLocationPermission();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //todo show error message ?!
            return;
        }
//

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

//        binding.fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });


//        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
//        WifiInfo info = wifiManager.getConnectionInfo();
//        Method[] methods = wifiManager.getClass().getDeclaredMethods();
//        for (Method m: methods) {
//            if (m.getName().equals("getWifiApConfiguration")) {
//                WifiConfiguration config = null;
//                try {
//                    config = (WifiConfiguration)m.invoke(wifiManager);
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
//                }
//                Log.e("TODEL", config.SSID);
//                // here, the "config" variable holds the info, your SSID is in
//                // config.SSID
//            }
//        }
//          turnOnHotspot();

        // on destroy:
//        mHandler.removeCallbacks(mUpdateTimeTask);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Intent service_intent = new Intent(this, DeviceManagerService.class);
        bindService(service_intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        device_manager_service_bound = false;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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
            EasyPermissions.requestPermissions(this, "Please grant the location permission", MY_PERMISSIONS_REQUEST_LOCATION, perms);
        }
    }

}
