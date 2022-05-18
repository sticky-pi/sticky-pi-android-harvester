package com.example.sticky_pi_data_harvester;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;


import com.example.sticky_pi_data_harvester.databinding.FragmentDetailBinding;
import com.example.sticky_pi_data_harvester.databinding.FragmentImageFilesBinding;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Hashtable;

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
        file_handler_list = file_manager_service.get_file_handler_list();
        TextView t = (TextView) inflatedView.findViewById(R.id.device_id);
        t.setText("Current device: " + getArguments().getString("a"));


        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_detail, container, false);
        // TODO :add on to show gridview of single device (need arg passed by from constructor)


        // TODO: add on to show scrollbar view of list of images (need arg passed by from constructor)
        // requires a list of files


        // TODO: add on to show size , download99 status of the latest image of a device

        return inflatedView;
    }
}
