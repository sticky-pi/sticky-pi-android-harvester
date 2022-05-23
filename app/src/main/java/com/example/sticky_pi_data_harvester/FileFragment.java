package com.example.sticky_pi_data_harvester;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.sticky_pi_data_harvester.databinding.FragmentImageFilesBinding;

import java.util.ArrayList;

import de.codecrafters.tableview.listeners.TableDataClickListener;

public class FileFragment extends Fragment {

    private FragmentImageFilesBinding binding;
    FileTableAdapter adapter;
    FileManagerService file_manager_service;
    ArrayList<FileHandler> file_handler_list = null;

    private Handler mHandler = new Handler();

    private Runnable mUpdateTimeTask = new Runnable() {
        int n_global_files = 0;
        int n_global_traces = 0;
        long global_disk_use ;
        MainActivity main_activity;



        public void update_global_file_summary(){
            int n_files = 0;
            int n_traces = 0;
            long disk_use = 0;
            ArrayList<FileHandler> file_handlers = file_handler_list;
            if(file_handlers != null)
                for (FileHandler fh: file_handlers) {
                    fh.index_files();
                    n_files += fh.get_n_jpg_images();
                    n_traces += fh.get_n_trace_images();
                    disk_use += fh.get_disk_use();
                }
            global_disk_use = disk_use;
            n_global_files = n_files;
            n_global_traces = n_traces;
        }

        public void run() {
            if(main_activity == null) {
                main_activity = (MainActivity) getActivity();
                mHandler.postDelayed(this, 500);
                return;
            }

            try {
                if(adapter != null) {
                    adapter.notifyDataSetChanged();
                }
             }

            finally {

                mHandler.postDelayed(this, 5000);
                update_global_file_summary();
               TextView text = main_activity.findViewById(R.id.global_device_file_info);
                if(text != null) {
                    String disk_use_str =  String.format( "%.02f", (double) global_disk_use / (Math.pow(1024, 3)));
                    text.setText(
                            n_global_files + " Local JPG images\n" +
                                    n_global_traces + " Uploaded images\n" +
                                    disk_use_str + " GB used\n"

                    );
                }

                TextView network_status_text = main_activity.findViewById(R.id.network_status);
                if(network_status_text != null) {
                    String status = "";
                    if (file_manager_service.get_api_client() == null || file_manager_service.get_api_client().get_api_host() == null){
                        network_status_text.setText("Waiting for API client");
                    }
                    else if (file_manager_service.get_api_client().get_api_host().equals("") || file_manager_service.get_api_client().get_user_name().equals("")){
                        network_status_text.setText("You need a host and a user name/.\nCheck your settings!");
                    }

                    else if(!file_manager_service.has_internet){
                        network_status_text.setText("No internet");
                    }
                    else if(!file_manager_service.is_host_up){
                        network_status_text.setText("Internet, but cannot reach\n"+ file_manager_service.get_api_client().get_api_host() );
                    }

                    else if(!file_manager_service.are_credentials_valid){
                        network_status_text.setText("Can reach host,\nbut authentication as `" + file_manager_service.get_api_client().get_user_name()+ "` fails!\nCheck password");
                    }
                    else{
                        network_status_text.setText("Connected to \n"+ file_manager_service.get_api_client().get_api_host());
                    }


                }

            }
        }
    };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {


        binding = FragmentImageFilesBinding.inflate(inflater, container, false);

        MainActivity main_activity = (MainActivity) getActivity();
        file_manager_service = main_activity.get_file_manager_service();
        if (file_manager_service != null) {
            file_handler_list = file_manager_service.get_file_handler_list();
            mUpdateTimeTask.run();

        }
        final FileSortableTable tableView = (FileSortableTable) binding.getRoot().findViewById(R.id.file_list_view);
        if (tableView != null) {
            adapter = new FileTableAdapter(main_activity, file_handler_list);
            tableView.setDataAdapter(adapter);
            tableView.setClickable(true);
            tableView.addDataClickListener(new TableDataClickListener<FileHandler>() {
                @Override
                public void onDataClicked(final int row_index, final FileHandler fh) {
                    Bundle bundle = new Bundle();
                    bundle.putString("a", fh.get_device_id());
                    NavHostFragment.findNavController(FileFragment.this)
                            .navigate(R.id.action_DeviceFragment_to_DetailFragment, bundle);
                }
            });
        }
    return binding.getRoot();

    }



@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}