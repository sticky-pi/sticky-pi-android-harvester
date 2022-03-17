package com.example.sticky_pi_data_harvester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    AppCompatActivity activity;
    WifiP2pManager.Channel channel;
    WifiP2pManager manager;
    static final String TAG = "StickyPiDataHarvester-BroadcastListener";

    public WiFiDirectBroadcastReceiver(WifiP2pManager manag, WifiP2pManager.Channel chan,
                                       AppCompatActivity activ){
            activity =activ;
            channel = chan;
            manager = manag;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.w(TAG, "Action" + action);
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // The peer list has changed! We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            // Connection state changed! We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            //
        }
    }
}

