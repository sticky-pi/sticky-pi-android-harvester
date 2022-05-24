package com.example.sticky_pi_data_harvester;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.ContentInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PreferenceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PreferenceFragment extends Fragment {


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public static final  String[] keys = {"preference_api_host", "preference_user_name", "preference_password", "preference_delete_uploaded_images"};


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PreferenceFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PreferenceFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PreferenceFragment newInstance(String param1, String param2) {
        PreferenceFragment fragment = new PreferenceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    SharedPreferences sharedpreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        setHasOptionsMenu(true);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item=menu.findItem(R.id.action_settings);
        Log.e("TODEL", "HIDING SETTINGS");
        if(item!=null)
            item.setVisible(false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState){
        super.onViewCreated(v, savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        activity.invalidateOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_preference, container, false);
        MainActivity activity = (MainActivity) getActivity();

        // populate view with existing preference
        sharedpreferences = activity.getSharedPreferences(MainActivity.APP_TAG, Context.MODE_PRIVATE);
        Resources res = getResources();

        for(String k: keys){
            if(sharedpreferences.contains(k)){
                int id = res.getIdentifier(k, "id", getContext().getPackageName());

                if(!k.equals("preference_delete_uploaded_images")) {
                    EditText target =  view.findViewById(id);
                    if(target != null)
                        target.setText(sharedpreferences.getString(k,""));
                }
                else{
                    CheckBox target = view.findViewById(id);
                    if (target != null) {
                        target.setChecked(sharedpreferences.getBoolean(k,false));

                    }
                }
            }
        }


        Button save_button = (Button) view.findViewById(R.id.preference_save);
        save_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                Resources res = getResources();
                for(String k: keys){
                    int id = res.getIdentifier(k, "id", requireContext().getPackageName());
                    if(!k.equals("preference_delete_uploaded_images")) {
                        EditText target = view.findViewById(id);
                        if (target != null) {
                            String text = target.getText().toString();
                            if (!text.equals("")) {
                                editor.putString(k, text);
                            }
                        }
                    }
                    else {
                        CheckBox target = view.findViewById(id);
                        if (target != null) {
                            editor.putBoolean(k, target.isChecked());
                        }
                    }
                }
                editor.commit();
                NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_content_main);
                navController.navigateUp();
            }
        });

        Button cancel_button = (Button) view.findViewById(R.id.preference_cancel);
        cancel_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                if(activity != null) {
                    NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_content_main);
                    navController.navigateUp();
                }
            }
        });
        return view;

    }
    public void cancel(View view){
        Log.e("TODEL","cancelling");
    }
}