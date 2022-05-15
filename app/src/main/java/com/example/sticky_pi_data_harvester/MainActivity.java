package com.example.sticky_pi_data_harvester;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.sticky_pi_data_harvester.databinding.ActivityMainBinding;

import java.util.Hashtable;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity {
    static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    static final String TAG = "StickyPiDataHarvester";
    AppBarConfiguration appBarConfiguration;
    ActivityMainBinding binding;
    DeviceManagerService device_manager_service;
    FileManagerService file_manager_service;
    boolean device_manager_service_bound =false;
    boolean file_manager_service_bound =false;


    private ServiceConnection device_manager_service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            Log.e("TODEL", "device service connecteing");
            DeviceManagerService.MyBinder binder = (DeviceManagerService.MyBinder) service;
            device_manager_service = binder.getService();
            device_manager_service_bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            device_manager_service_bound = false;
        }
    };


    private ServiceConnection file_manager_service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.e("TODEL", "file service connecteing");
            FileManagerService.MyBinder binder = (FileManagerService.MyBinder) service;
            file_manager_service = binder.getService();
            file_manager_service_bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            file_manager_service_bound = false;

            Log.e("TODEL", "file disconnected");
        }
    };


    public Hashtable<String, DeviceHandler> get_device_dict() {
        if(device_manager_service == null) {
            Log.w(TAG, "Service not bound, so returning empty table");
            return new Hashtable<String, DeviceHandler>();
        }
        return device_manager_service.get_device_dict();
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

        Intent service_intent_fm = new Intent(this, FileManagerService.class);
        bindService(service_intent_fm, file_manager_service_connection, Context.BIND_AUTO_CREATE);

        Intent service_intent_dm = new Intent(this, DeviceManagerService.class);
        bindService(service_intent_dm, device_manager_service_connection, Context.BIND_AUTO_CREATE);
    }
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(device_manager_service_connection);
        device_manager_service_bound = false;
        unbindService(file_manager_service_connection);
        file_manager_service_bound = false;
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
