package com.example.sticky_pi_data_harvester;

import static com.example.sticky_pi_data_harvester.MainActivity.MY_PERMISSIONS_REQUEST_LOCATION;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.sticky_pi_data_harvester.databinding.FragmentDeviceListBinding;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;

public class DeviceListFragment extends Fragment {
    private FragmentDeviceListBinding binding;
    private Handler mHandler = new Handler();
    MainActivity parent_activity;
    DeviceAdapter device_adapter;

    BroadcastReceiver wifi_state_receiver;
    String local_only_pass, local_only_ssid;
    private WifiManager wifiManager;
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    public void handleDialogClose(DialogInterface dialog){

        ImageView imageCode = getActivity().findViewById(R.id.local_only_ap_qr);
        if(imageCode != null)
            imageCode.setImageAlpha(255);
       };//or whatever args you want

    private class LocationUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DeviceManagerService.UPDATE_LOCATION_INTENT) && parent_activity.device_manager_service != null) {
                TextView location_textview = (TextView) parent_activity.findViewById(R.id.location_text_view);
                String text = "";
                Location location = parent_activity.device_manager_service.get_location();
                if(location != null) {
                    text += String.format("%+013.8f", location.getLatitude()) + "\n" +
                            String.format("%+013.8f", location.getLongitude()) + "\n" +
                            String.format("%+04.1f m", location.getAltitude());

                    location_textview.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            String uri = String.format(Locale.ENGLISH, "geo:%f,%f", location.getLatitude(), location.getLongitude());
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                            context.startActivity(intent);
                        }
                    });
                }
                else{
                    text += "Not available";
                }
                location_textview.setText(text);

            }
        }
    }

    private LocationUpdateReceiver location_update_receiver;

    private static final String TAG = "StickyPiDataHarvester-framgent1";


    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            try {
                if(device_adapter != null) {
                    device_adapter.notifyDataSetChanged();
                }
                else
                    Log.e(TAG, "device adapter is NULL!!");
            }
            finally {
                mHandler.postDelayed(this, 1000);
            }
        }
    };


    private void generate_local_only_qr(){
        MainActivity main_activity = (MainActivity) getActivity();
        ImageView imageCode = main_activity.findViewById(R.id.local_only_ap_qr);
        // this should also work in localonly hotspo is on the 2.4ghz band

        if(! wifiManager.isWifiEnabled() && local_only_ssid != null && local_only_pass != null) {

            String qr_code = "WIFI:S:" + local_only_ssid + ";T:WPA;P:" + local_only_pass + ";;F:1;";

            MultiFormatWriter mWriter = new MultiFormatWriter();
            //ImageView for generated QR code

            //        ImageView dialog_image = findViewById(R.id.dialog_ap_qr);

            try {
                //BitMatrix class to encode entered text and set Width & Height
                BitMatrix mMatrix = mWriter.encode(qr_code, BarcodeFormat.QR_CODE, 512, 512);
                BarcodeEncoder mEncoder = new BarcodeEncoder();
                Bitmap mBitmap = mEncoder.createBitmap(mMatrix);//creating bitmap of code
                imageCode.setImageBitmap(mBitmap);//Setting generated QR code to imageView

                imageCode.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Log.e("todel", "clieck on dial");
                        Log.e(TAG, "ssid: " + local_only_ssid + "; pass: "+ local_only_pass);
                        APDialogFragment ap_dial_frag = new APDialogFragment(local_only_ssid, local_only_pass, mBitmap);
                        imageCode.setImageAlpha(0);
                        ap_dial_frag.show(getChildFragmentManager(), "ap");
                    }
                });
            } catch (WriterException writerException) {
                writerException.printStackTrace();
            }
        }
        else{
            if(local_only_ssid == null  || local_only_pass == null){

                Log.e(TAG, "Localonly wifi not set up properly!");
                imageCode.setImageDrawable(getResources().getDrawable(R.drawable.deactivate_wifi));
            }
            else {
                imageCode.setImageDrawable(getResources().getDrawable(R.drawable.deactivate_wifi));
                Log.e(TAG, "Wifi enabled. Might not be able to start access point!");
                // support for turning wifi off from app is deprecated!
                imageCode.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                        startActivityForResult(intent, 0);
                    }
                });
            }
        }

    }



    @AfterPermissionGranted(MY_PERMISSIONS_REQUEST_LOCATION)
    public void turnOnHotspot() {

        if(hotspotReservation !=null) {
            hotspotReservation.close();
            hotspotReservation = null;
        }

        wifiManager = (WifiManager) parent_activity.getSystemService(Context.WIFI_SERVICE);

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
                generate_local_only_qr();
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
                Toast.makeText(parent_activity, "Issue with hotspot! Turn airplane mode off and on!",
                                Toast.LENGTH_LONG).show();
                generate_local_only_qr();
            }
        }, new Handler());
        } catch(java.lang.IllegalStateException e){
            Log.e(TAG, "Failed to set local hotpot");
        }
    }

    @Override
    public void onResume() {

        super.onResume();

        if (location_update_receiver == null)
            location_update_receiver = new LocationUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(DeviceManagerService.UPDATE_LOCATION_INTENT);
        parent_activity.registerReceiver(location_update_receiver, intentFilter);
        location_update_receiver.onReceive( parent_activity, new Intent(DeviceManagerService.UPDATE_LOCATION_INTENT));

        wifi_state_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                turnOnHotspot();

            }
        };
        parent_activity.registerReceiver(wifi_state_receiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        turnOnHotspot();


    }
    @Override
    public void onPause() {
        super.onPause();
        if (location_update_receiver != null) parent_activity.unregisterReceiver(location_update_receiver);
        if (wifi_state_receiver != null) parent_activity.unregisterReceiver(wifi_state_receiver);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentDeviceListBinding.inflate(inflater, container, false);
        parent_activity = (MainActivity) getActivity();
        device_adapter = new DeviceAdapter(this.getContext(), parent_activity, this);

        final GridView gridView = (GridView) binding.getRoot().findViewById(R.id.device_grid_view);

        gridView.setAdapter(device_adapter);
        mUpdateTimeTask.run();
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(DeviceListFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        if(hotspotReservation != null)
            hotspotReservation.close();
        super.onDestroyView();
        binding = null;
    }

}