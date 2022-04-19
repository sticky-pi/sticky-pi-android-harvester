package com.example.sticky_pi_data_harvester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.sticky_pi_data_harvester.databinding.FragmentDeviceListBinding;

public class DeviceListFragment extends Fragment {

    private FragmentDeviceListBinding binding;
    private Handler mHandler = new Handler();
    MainActivity parent_activity;
    DeviceAdapter device_adapter;


    private class LocationUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("TODEL", "test");
            if (intent.getAction().equals(DeviceManagerService.UPDATE_LOCATION_INTENT) && parent_activity.device_manager_service != null) {
                TextView location_textview = (TextView) parent_activity.findViewById(R.id.location_text_view);
                String text = "";
                Location location = parent_activity.device_manager_service.get_location();
                text += String.format("%.8f", location.getLatitude()) +
                        ", " + String.format("%.8f", location.getLongitude()) +
                        ", " + String.format("%.1f", location.getAltitude());
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


    @Override
    public void onResume() {

        super.onResume();

        if (location_update_receiver == null)
            location_update_receiver = new LocationUpdateReceiver();


        IntentFilter intentFilter = new IntentFilter(DeviceManagerService.UPDATE_LOCATION_INTENT);
        parent_activity.registerReceiver(location_update_receiver, intentFilter);
        location_update_receiver.onReceive( parent_activity, new Intent(DeviceManagerService.UPDATE_LOCATION_INTENT));
    }
    @Override
    public void onPause() {
        super.onPause();
            if (location_update_receiver != null) parent_activity.unregisterReceiver(location_update_receiver);

    }
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentDeviceListBinding.inflate(inflater, container, false);
        parent_activity = (MainActivity) getActivity();
        device_adapter = new DeviceAdapter(this.getContext(), parent_activity);

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
        super.onDestroyView();
        binding = null;
    }

}