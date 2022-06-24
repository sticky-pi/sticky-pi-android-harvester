package com.piee.sticky_pi_data_harvester;

import static com.piee.sticky_pi_data_harvester.MainActivity.MY_PERMISSIONS_REQUEST_LOCATION;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.piee.sticky_pi_data_harvester.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.piee.sticky_pi_data_harvester.databinding.FragmentDeviceListBinding;

import java.lang.reflect.Method;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;

public class DeviceListFragment extends Fragment {
    private FragmentDeviceListBinding binding;
    private Handler mHandler = new Handler();
//    MainActivity parent_activity;
    DeviceAdapter device_adapter;
    private static final String TAG = "DeviceListFragment";
    BroadcastReceiver wifi_state_receiver;
    String local_only_pass, local_only_ssid;
    private WifiManager wifiManager;
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    private void update_location_view(){
        MainActivity main_activity = (MainActivity) getActivity();
        TextView location_textview = (TextView) main_activity.findViewById(R.id.location_text_view);
        if(location_textview == null || main_activity == null)
            return;
        String text = "Not available!";
        if(main_activity.device_manager_service != null){
            Location location = main_activity.device_manager_service.get_location();
            if(location != null) {
                text = String.format("%+013.8f", location.getLatitude()) + "\n" +
                        String.format("%+013.8f", location.getLongitude()) + "\n" +
                        String.format("%+04.1f m", location.getAltitude());

            location_textview.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String uri = String.format(Locale.ENGLISH, "geo:%f,%f", location.getLatitude(), location.getLongitude());
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    main_activity.startActivity(intent);
                }
            });
        }}
        location_textview.setText(text);

    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            try {
                update_location_view();
                if(device_adapter != null) {
                    device_adapter.notifyDataSetChanged();

                }
            }
            finally {
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    private void generate_local_only_qr(){
        MainActivity main_activity = (MainActivity) getActivity();
//        ImageView imageCode = main_activity.findViewById(R.id.local_only_ap_qr);
        Button connectivity_button = main_activity.findViewById(R.id.device_conectivity_action);
        // this should also work in localonly hotspo is on the 2.4ghz band

        if( local_only_ssid != null && local_only_pass != null) {

            String qr_code = "WIFI:S:" + local_only_ssid + ";T:WPA;P:" + local_only_pass + ";;F:1;";

            MultiFormatWriter mWriter = new MultiFormatWriter();
            //ImageView for generated QR code

            //        ImageView dialog_image = findViewById(R.id.dialog_ap_qr);

            try {
                //BitMatrix class to encode entered text and set Width & Height
                BitMatrix mMatrix = mWriter.encode(qr_code, BarcodeFormat.QR_CODE, 512, 512);
                BarcodeEncoder mEncoder = new BarcodeEncoder();
                Bitmap mBitmap = mEncoder.createBitmap(mMatrix);//creating bitmap of code
//                imageCode.setImageBitmap(mBitmap);//Setting generated QR code to imageView

                connectivity_button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        APDialogFragment ap_dial_frag = new APDialogFragment(local_only_ssid, local_only_pass, mBitmap);
                        ap_dial_frag.show(getChildFragmentManager(), "ap");
                    }
                });
            } catch (WriterException writerException) {
                writerException.printStackTrace();
            }
        }
        else{
            connectivity_button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
//                    Toast("VOILA");
                    Toast.makeText(main_activity, "Issue with temporary access point!",
                            Toast.LENGTH_LONG).show();
//
                }
            });
        }
//        else{
//            if(local_only_ssid == null  || local_only_pass == null){
//
//                Log.e(TAG, "Localonly wifi not set up properly!");
//                imageCode.setImageDrawable(getResources().getDrawable(R.drawable.deactivate_wifi));
//            }
//            else {
//                imageCode.setImageDrawable(getResources().getDrawable(R.drawable.deactivate_wifi));
//                Log.e(TAG, "Wifi enabled. Might not be able to start access point!");
//                // support for turning wifi off from app is deprecated!
//                imageCode.setOnClickListener(new View.OnClickListener() {
//                    public void onClick(View v) {
//
//                        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
//                        startActivityForResult(intent, 0);
//                    }
//                });
//            }
//        }

    }
//
//    public String get_hostspot_name(){
//        String ssid = "";
//        MainActivity main_activity = (MainActivity) getActivity();
//        wifiManager = (WifiManager) main_activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
//                if (config != null) {
//                    ssid = config.SSID;
////                    String bssid = config.BSSID;
//                }
//            }
//        }
//        return ssid;
//    }
//
    @AfterPermissionGranted(MY_PERMISSIONS_REQUEST_LOCATION)
    public void turnOnHotspot() {

        if(hotspotReservation !=null) {
            hotspotReservation.close();
            hotspotReservation = null;
        }
//        get_hostspot_name();
//        wifiManager = (WifiManager) parent_activity.getSystemService(Context.WIFI_SERVICE);

        MainActivity main_activity = (MainActivity) getActivity();


        if (ActivityCompat.checkSelfPermission(main_activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try{
        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                hotspotReservation = reservation;
                WifiConfiguration currentConfig = hotspotReservation.getWifiConfiguration();
                local_only_pass = currentConfig.preSharedKey;
                local_only_ssid = currentConfig.SSID;
//                generate_local_only_qr();
            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.e(TAG, "Local Hotspot Stopped");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.e(TAG, "Local Hotspot failed to start. Reason: " + reason);
                Toast.makeText(main_activity, "Issue with hotspot! Turn airplane mode off and on!",
                                Toast.LENGTH_LONG).show();
//                generate_local_only_qr();
            }
        }, new Handler());
        } catch(java.lang.IllegalStateException e){
            Log.e(TAG, "Failed to set local hotpot");
        }
    }

    @Override
    public void onResume() {

        super.onResume();

        MainActivity main_activity = (MainActivity) getActivity();
//        if (location_update_receiver == null)
//            location_update_receiver = new LocationUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(DeviceManagerService.UPDATE_LOCATION_INTENT);

        if (wifi_state_receiver == null)
            wifi_state_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    update_device_connectivity();}
        };

        IntentFilter wifi_change = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifi_change.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");

        main_activity.registerReceiver(wifi_state_receiver, wifi_change);
        }

    private void update_device_connectivity() {
        boolean ap_enabled;
        boolean wifi_enabled;
        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifi_enabled = wifiManager.isWifiEnabled();
        try {
            final Method method =
                    wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            ap_enabled = (Boolean) method.invoke(wifiManager);

        } catch (Exception e) {
            ap_enabled = false;
        }

        Log.i(TAG, "Connectivity update. ap_enabled: " + ap_enabled);


        TextView device_conectivity_status = getActivity().findViewById(R.id.device_conectivity_status);
        Button device_conectivity_action = getActivity().findViewById(R.id.device_conectivity_action);

        if(wifi_enabled){
            device_conectivity_status.setText("Wifi in use");
            device_conectivity_action.setText("Turn off wifi.\nToggle airplane\nmode");
            device_conectivity_action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                    startActivityForResult(intent, 0);
                }
            });

        }
        else if(ap_enabled){
            device_conectivity_status.setText("Hotspot on");
            device_conectivity_action.setText("Show QR code\nto devices\nto pair");

            device_conectivity_action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                    final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                    intent.setComponent(cn);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity( intent);
//                    startActivityForResult(intent, 0);
                }
            });

        }
//        else{
//            device_conectivity_status.setText("One-off pairing");
//            device_conectivity_action.setText("Display\ntemporary\nQR code");
//            if(hotspotReservation == null) {
//                turnOnHotspot();
//            }
//            generate_local_only_qr();
//        }
        if((ap_enabled || wifi_enabled) && hotspotReservation != null) {
                hotspotReservation.close();
                hotspotReservation = null;
            }
    }

    @Override
    public void onPause() {
        super.onPause();

        MainActivity main_activity = (MainActivity) getActivity();
        if (wifi_state_receiver != null) main_activity.unregisterReceiver(wifi_state_receiver);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item=menu.findItem(R.id.action_devices);
        if(item!=null)
            item.setVisible(false);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {


        MainActivity main_activity = (MainActivity) getActivity();
        binding = FragmentDeviceListBinding.inflate(inflater, container, false);

        main_activity = (MainActivity) getActivity();
        device_adapter = new DeviceAdapter(this.getContext(), main_activity, this);


        final GridView gridView = (GridView) binding.getRoot().findViewById(R.id.device_grid_view);

        gridView.setAdapter(device_adapter);
        mUpdateTimeTask.run();
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        if(hotspotReservation != null)
            hotspotReservation.close();
        super.onDestroyView();
        binding = null;
    }

}