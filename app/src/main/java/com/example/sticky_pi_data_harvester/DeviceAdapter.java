package com.example.sticky_pi_data_harvester;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;


public class DeviceAdapter extends BaseAdapter {

    MainActivity main_activity;
    private LayoutInflater layoutInflater;
    private Context context;
    private DeviceListFragment m_parentFragment;

    public Hashtable<String, DeviceHandler> get_device_dict() {
        return main_activity.get_device_dict();
    }
    public DeviceAdapter(Context aContext,  MainActivity mainActivity, DeviceListFragment parentFragment) {

        this.context = aContext;
        main_activity = mainActivity;
        layoutInflater = LayoutInflater.from(aContext);
        m_parentFragment = parentFragment;
    }


    private String secs_to_human_durations(long seconds){

        int days = (int) Math.floor(seconds / (3600*24));
        int hrs   = (int) Math.floor(seconds / 3600);
        long mins = (long) Math.floor(seconds / 60);

        if(days >= 2){
            return days + " d ago";
        }
        if(hrs >= 2){
            return hrs + " h ago";
        }
        if(mins >= 2){
            return mins + " min ago";
        }
        if(seconds <= 2 ) {
            return "now";
        }
        return seconds + "s ago";
    }

    public String position_to_key(int position){
        Enumeration<String> e = get_device_dict().keys();
        ArrayList<Integer> map = new ArrayList();
        List <Long> device_times = new ArrayList();
        String [] device_keys = new String[get_device_dict().size()];
        int i =0;
        while(e.hasMoreElements()){
            String k = e.nextElement();
            Long time = Objects.requireNonNull(get_device_dict().get(k)).time_created;
            device_times.add(time);
            device_keys[i] = k;
            map.add(i);
            i ++;
        }

        Collections.sort(map, (c1, c2) -> Long.compare(device_times.get((Integer) c2), device_times.get((Integer) c1)));
        String key = device_keys[map.get(position)];
        return key;
    }
    @Override
    public int getCount() {
        return get_device_dict().size();
    }

    public static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public Object getItem(int position) {
         return get_device_dict().get(position_to_key(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        DeviceHandler device_handler = this.get_device_dict().get(position_to_key(position));
        assert device_handler != null;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.grid_item_layout, null);
            holder = new ViewHolder();
                holder.last_image = (ImageView) convertView.findViewById(R.id.image_view_last_image);
                holder.device_id = (TextView) convertView.findViewById(R.id.text_view_device_id);
                holder.downloaded_files = (TextView) convertView.findViewById(R.id.text_view_downloaded_files);
                holder.battery_level = (TextView) convertView.findViewById(R.id.text_view_battery_level);
                holder.last_pace = (TextView) convertView.findViewById(R.id.last_pace);
                holder.available_disk_space = (TextView) convertView.findViewById(R.id.text_view_available_disk_space);
                holder.rectangle = (View) convertView.findViewById(R.id.myRectangleView);

                //
                holder.rectangle.setClickable(true);
                holder.rectangle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i("info", "clicked");
                        Bundle bundle = new Bundle();
                        // First string means the key to retrieve the second string
                        bundle.putString("a", device_handler.get_device_id());
                        NavHostFragment.findNavController(m_parentFragment)
                                .navigate(R.id.action_DeviceFragment_to_DetailFragment, bundle);
                    }
                });
                //
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }



        if(device_handler.get_is_ghost()){
            holder.rectangle.setBackgroundResource(R.drawable.rectangle_ghost);
        }
        else {
            if(device_handler.get_status().equals("starting"))
                holder.rectangle.setBackgroundColor(Color.parseColor("#8888bb"));
            else  if(device_handler.get_status().equals("syncing"))
                holder.rectangle.setBackgroundColor(Color.parseColor("#88bbbb"));
            else if(device_handler.get_status().equals("done"))
                holder.rectangle.setBackgroundColor(Color.parseColor("#88bb88"));
            else
                holder.rectangle.setBackgroundColor(Color.parseColor("#bbbbbb"));
        }

        holder.device_id.setText(device_handler.get_device_id());
        //todo ifelse status is not starting:
        String txt =
                device_handler.get_n_downloaded() + device_handler.get_n_skipped() + "/" + device_handler.get_n_to_download() +
                        "(" + device_handler.get_n_skipped()+ ")";

        holder.downloaded_files.setText(txt);

        if(device_handler.get_n_errored() > 0)
            holder.downloaded_files.setTextColor(Color.parseColor("#FF0000"));
        else
            holder.downloaded_files.setTextColor(Color.parseColor("#000000"));

        int battery_icon_num = (int ) (1 + 6.0 * device_handler.get_battery_level() /100.0);
        if (battery_icon_num < 1)
            battery_icon_num = 1;
        if (battery_icon_num > 7)
            battery_icon_num = 7;

        String battery_icon_name = "ic_baseline_battery_" + battery_icon_num + "_bar_12";
        int battery_icon_id = context.getResources().getIdentifier("drawable/" + battery_icon_name, null, context.getPackageName());
        holder.battery_level.setText(String.format("%02d", device_handler.get_battery_level())+ "%");
        holder.battery_level.setCompoundDrawablesWithIntrinsicBounds(battery_icon_id, 0,0, 0);


        long delta_t =   Instant.now().getEpochSecond() - device_handler.get_last_pace();

        holder.last_pace.setText(secs_to_human_durations(delta_t));
        if(device_handler.get_status().equals("done")) {
            holder.device_id.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_check_12, 0);
        }else if(device_handler.get_status().equals("syncing")){
            holder.device_id.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_sync_12, 0);

        }else if((device_handler.get_status().equals("starting"))){
            holder.device_id.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_sync_12, 0);
        }else{
            holder.device_id.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_error_12, 0);
        }
        holder.available_disk_space.setText(String.format("%.2f", device_handler.get_available_disk_space()) + "%" );
        String img_path = device_handler.get_last_image_path() + ".thumbnail";

        if(new File(img_path).isFile()) {
            Uri img_uri=Uri.parse(img_path);
            holder.last_image.setImageURI(img_uri);
        }
        else{
            if(device_handler.get_status().equals("errored")){

                holder.last_image.setImageResource(R.drawable.ic_thumbnail_errored_sync);
            }
            else {
                holder.last_image.setImageResource(R.drawable.ic_thumbnail_syncing);
            }
        }
        return convertView;
    }


    static class ViewHolder {

        ImageView last_image;
        TextView device_id;
        TextView battery_level;
        TextView available_disk_space;
        TextView downloaded_files;
        TextView last_pace;
        View rectangle;
    }

}
