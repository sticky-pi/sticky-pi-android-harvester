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
                else
                    Log.e("FIXME", "file adapter is NULL!!");
            }

            finally {

                Log.e("FIXME", "updating");
                mHandler.postDelayed(this, 5000);
                update_global_file_summary();
               TextView text = main_activity.findViewById(R.id.global_device_file_info);
                if(text != null) {
                    text.setText(
                            n_global_files + " Local JPG images\n" +
                                    n_global_traces + " Uploaded images\n" +
                                    global_disk_use + " GB used\n"

                    );
                }

                TextView network_status_text = main_activity.findViewById(R.id.network_status);
                if(network_status_text != null) {
                    String status = "";
                    if(file_manager_service.get_api_client().get_api_host().equals("") || file_manager_service.get_api_client().get_user_name().equals("")){
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
        Log.e("adapter", "...");
        if (tableView != null) {

            adapter = new FileTableAdapter(main_activity, file_handler_list);

            Log.e("adapter", "OK");
            tableView.setDataAdapter(adapter);
//            tableView.addDataClickListener(new CarClickListener());
//            tableView.addDataLongClickListener(new CarLongClickListener());
//            carTableView.setSwipeToRefreshEnabled(true);
//            carTableView.setSwipeToRefreshListener(new SwipeToRefreshListener() {
//                @Override
//                public void onRefresh(final RefreshIndicator refreshIndicator) {
//                    carTableView.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            final Car randomCar = getRandomCar();
//                            carTableDataAdapter.getData().add(randomCar);
//                            carTableDataAdapter.notifyDataSetChanged();
//                            refreshIndicator.hide();
//                            Toast.makeText(MainActivity.this, "Added: " + randomCar, Toast.LENGTH_SHORT).show();
//                        }
//                    }, 3000);
//                }
//            });
//        }
    }
    return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("test1", "2->1");
                NavHostFragment.findNavController(FileFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}