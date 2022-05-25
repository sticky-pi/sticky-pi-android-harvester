package com.example.sticky_pi_data_harvester;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.sticky_pi_data_harvester.databinding.FragmentDetailBinding;
import com.example.sticky_pi_data_harvester.databinding.FragmentImageFilesBinding;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DetailFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private @NonNull FragmentDetailBinding binding;
    FileManagerService file_manager_service;
    ArrayList<FileHandler> file_handler_list = null;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Hashtable<String, DeviceHandler> dev_list;

    public DetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DetailFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DetailFragment newInstance(String param1, String param2) {
        DetailFragment fragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View inflatedView = inflater.inflate(R.layout.fragment_detail, container,false);

        MainActivity main_activity = (MainActivity) getActivity();
        file_manager_service = main_activity.get_file_manager_service();
        TextView t = (TextView) inflatedView.findViewById(R.id.device_id);
        if (file_manager_service == null) {
            t.setText("No file manager found");
            return inflatedView;
        }
        file_handler_list = file_manager_service.get_file_handler_list();
        String devId =  getArguments().getString("a");
        t.setText("Current device: " + devId);

        FileHandler fileHandler = null;
        for (FileHandler fh: file_handler_list) {
            if(Objects.equals(fh.get_device_id(), devId)) {
                fileHandler = fh;
            }
        }
        if (fileHandler == null ) {
            t.setText("No file handler for device " + devId);
            return inflatedView;
        }
        fileHandler.index_files();
        int n_jpg_images = fileHandler.get_n_jpg_images();
        int n_traced_jpg_images = fileHandler.get_n_traced_jpg_images();

        TextView n_jpg = (TextView) inflatedView.findViewById(R.id.n_jpgs);
        n_jpg.setText("Number of local images: "+ n_jpg_images);
        TextView n_traced = (TextView) inflatedView.findViewById(R.id.n_traces);
        n_traced.setText("Number of traced images: " + n_traced_jpg_images);

        ImageView latest_image = (ImageView) inflatedView.findViewById(R.id.detailed_image);


        File root_dir = file_manager_service.getApplicationContext().getExternalFilesDir(null);
        ImageRep img = new ImageRep(root_dir.getPath(), fileHandler.get_device_id(), fileHandler.get_last_seen());
        String path = img.getImagePath(root_dir.getPath(),fileHandler.get_device_id());
        if(path.compareTo("") != 0) {
            Uri img_uri=Uri.parse(path);
            latest_image.setImageURI(img_uri);
        }

        return inflatedView;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
