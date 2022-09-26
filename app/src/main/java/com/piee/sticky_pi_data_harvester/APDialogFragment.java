package com.piee.sticky_pi_data_harvester;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.piee.sticky_pi_data_harvester.R;

import java.util.Objects;



public class APDialogFragment extends DialogFragment {
    String m_ssid;
    String m_pass;
    Bitmap m_img;
    boolean dismissed = false;

//    public void onDismiss(DialogInterface dialog)
//    {
//        super.onDismiss(dialog);
//        DeviceListFragment parent =  (DeviceListFragment) getParentFragment();
//        dismissed = true;
//        parent.handleDialogClose(dialog);
//    }

    public  APDialogFragment(String ssid, String pass, Bitmap img) {
        super();
        m_ssid = ssid;
        m_pass = pass;
        m_img = img;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout to use as dialog or embedded fragment
        View out = inflater.inflate(R.layout.dialog_ap_qr, container, false);

        ImageView dialog_image =out.findViewById(R.id.dialog_ap_qr);

        dialog_image.setImageBitmap(m_img);//Setting generated QR code to imageView

        TextView  ssid_view = out.findViewById(R.id.ap_ssid);
        ssid_view.setText(m_ssid);
        TextView  pass_view = out.findViewById(R.id.ap_pass);
        pass_view.setText(m_pass);
        //TODO SET BRIGHTNESS TO HIGHHHHHHHHH
        return out;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        Dialog out = super.onCreateDialog(savedInstanceState);
        return  out;
    }
}
