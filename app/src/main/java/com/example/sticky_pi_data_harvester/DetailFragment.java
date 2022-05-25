package com.example.sticky_pi_data_harvester;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.Toast;


import com.example.sticky_pi_data_harvester.databinding.FragmentDetailBinding;
import com.example.sticky_pi_data_harvester.databinding.FragmentImageFilesBinding;

import org.w3c.dom.Text;

import java.io.File;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Objects;

import de.codecrafters.tableview.listeners.TableDataClickListener;

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
    private DetailTableAdapter adapter;
    private String root_img_dir;
    private String devId;
    public ViewGroup view_group;

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

        view_group = container;
        View inflatedView = inflater.inflate(R.layout.fragment_detail, container,false);
        binding = FragmentDetailBinding.inflate(inflater, container, false);

        MainActivity main_activity = (MainActivity) getActivity();
        file_manager_service = main_activity.get_file_manager_service();
        TextView device_id = (TextView) binding.getRoot().findViewById(R.id.device_id);


        if (file_manager_service == null) {
            device_id.setText("Issue connecting to file\nservice manager!");
            return inflatedView;
        }

        file_handler_list = file_manager_service.get_file_handler_list();
        devId =  getArguments().getString("a");
        root_img_dir = file_manager_service.getStorage_dir().getPath() + "/" + devId ;

        device_id.setText( devId);
        device_id.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);


        FileHandler fileHandler = null;
        for (FileHandler fh: file_handler_list) {
            if(Objects.equals(fh.get_device_id(), devId)) {
                fileHandler = fh;
            }
        }

        if (fileHandler == null ) {
            device_id.setText("Cannot find file handler for device: "+ devId+ "\n Maybe no images?");
            return inflatedView;
        }

        ArrayList<ImageRep> all_img_reps = new ArrayList<>();
        fileHandler.index_files(all_img_reps);


        TextView n_jpg = (TextView) binding.getRoot().findViewById(R.id.n_jpgs);
        n_jpg.setText("Local images: "+ fileHandler.get_n_jpg_images());

        int denom = fileHandler.get_n_jpg_images() + fileHandler.get_n_trace_images() - fileHandler.get_n_traced_jpg_images();
        String percent_up;
        if(denom >0) {
            percent_up = String.valueOf(String.format(
                    "%.01f",
                    (100.0 * fileHandler.get_n_trace_images()) / (float) denom
            ));
        }
        else {
            percent_up = "NA";
        }
        TextView percent_up_text = (TextView) binding.getRoot().findViewById(R.id.percent_up);
        percent_up_text.setText("% uploaded: "+ percent_up);


        if(all_img_reps.size() == 0)
            return inflatedView;
        all_img_reps.sort(new Comparator<ImageRep>() {
            @Override
            public int compare(ImageRep o1, ImageRep o2) {
                if (o1.getTime() < o2.getTime()) {
                    return 1;
                } else if (o1.getTime() > o2.getTime()) {
                    return -1;
                }
                return 0;
            }
        });


        update_displayed_image(all_img_reps.get(0));



        final DetailTable tableView = (DetailTable) binding.getRoot().findViewById(R.id.detail_file_list);

        if (tableView != null) {
            adapter = new DetailTableAdapter(main_activity, all_img_reps, devId);
            tableView.setDataAdapter(adapter);
            tableView.setClickable(true);


            tableView.addDataClickListener(new TableDataClickListener<ImageRep>() {
                @Override
                public void onDataClicked(final int row_index, final ImageRep rep) {
                    update_displayed_image(rep);
                    ((DetailTableAdapter) tableView.getDataAdapter()).setSelectedRowIdx(row_index);
                    tableView.getDataAdapter().notifyDataSetChanged();
                }
            });

        }

        return binding.getRoot();
    }

    public  void update_displayed_image(ImageRep rep){

        // here, sort
        ImageView displayed_img = (ImageView) binding.getRoot().findViewById(R.id.detail_thumbnail);

        String uri_candidate_thumb = rep.getImgThumbnailPath(root_img_dir, devId);
        String uri_candidate = rep.getImagePath(root_img_dir, devId);
        String file_name =  new File(uri_candidate).getName();

        TextView detail_img_filename  = (TextView) binding.getRoot().findViewById(R.id.detail_img_filename);

        detail_img_filename.setText(file_name);

        if(new File(uri_candidate_thumb).isFile()) {
            Uri thumb_uri=Uri.parse(uri_candidate_thumb);
            displayed_img.setImageURI(thumb_uri);

            displayed_img.setOnClickListener(
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            if(new File(uri_candidate).isFile()){
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);

                                Uri img_uri=Uri.parse(uri_candidate);
                                intent.setDataAndType(img_uri, "image/*");
                                startActivity(intent);
                            }
                            else{
                                MainActivity main_activity = (MainActivity) getActivity();
                                Toast.makeText(main_activity, "No local image for",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );

        }
        else {
            displayed_img.setImageResource(R.drawable.ic_no_thumbnail);
        }

    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
