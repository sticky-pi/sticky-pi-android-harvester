package com.example.sticky_pi_data_harvester;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;


public class DeviceAdapter extends BaseAdapter {

    private Hashtable<String, DeviceHandler> device_dict;
    private LayoutInflater layoutInflater;
    private Context context;

    public DeviceAdapter(Context aContext,  Hashtable<String, DeviceHandler> dev_dict) {
        this.context = aContext;
        this.device_dict = dev_dict;
        layoutInflater = LayoutInflater.from(aContext);
    }
    public String position_to_key(int position){
        Enumeration<String> e = device_dict.keys();
        ArrayList<Integer> map = new ArrayList();
        List <Long> device_times = new ArrayList();
        String [] device_keys = new String[device_dict.size()];
        int i =0;
        while(e.hasMoreElements()){
            String k = e.nextElement();
            Long time = Objects.requireNonNull(device_dict.get(k)).time_created;
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
        return device_dict.size();
    }


    @Override
    public Object getItem(int position) {
         return device_dict.get(position_to_key(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.grid_item_layout, null);
            holder = new ViewHolder();
//            holder.flagView = (ImageView) convertView.findViewById(R.id.imageView_flag);
//            holder.countryNameView = (TextView) convertView.findViewById(R.id.textView_countryName);
                holder.device_id = (TextView) convertView.findViewById(R.id.text_view_device_id);
                holder.downloaded_files = (TextView) convertView.findViewById(R.id.text_view_downloaded_files);
                holder.battery_level = (TextView) convertView.findViewById(R.id.text_view_battery_level);
                holder.available_disk_space = (TextView) convertView.findViewById(R.id.text_view_available_disk_space);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DeviceHandler device_handler = this.device_dict.get(position_to_key(position));
//        holder.countryNameView.setText(country.getCountryName());
        assert device_handler != null;

        holder.device_id.setText(device_handler.get_device_id());
        //fixme ifelse status is not starting:
        String txt =
                device_handler.get_n_downloaded() + "/" + device_handler.get_n_to_download() +
                        "(" + device_handler.get_n_skipped()+ ")";

        holder.downloaded_files.setText(txt);
        holder.battery_level.setText(String.format("%.2f", device_handler.get_battery_level()));
        holder.available_disk_space.setText(String.format("%.2f", device_handler.get_available_disk_space()));
//        int imageId = this.getMipmapResIdByName(country.getFlagName());

//        holder.flagView.setImageResource(imageId);

        return convertView;
    }

    // Find Image ID corresponding to the name of the image (in the directory mipmap).
//    public int getMipmapResIdByName(String resName)  {
//        String pkgName = context.getPackageName();
//
//        // Return 0 if not found.
//        int resID = context.getResources().getIdentifier(resName , "mipmap", pkgName);
//        Log.i("CustomGridView", "Res Name: "+ resName+"==> Res ID = "+ resID);
//        return resID;
//    }

    static class ViewHolder {

//        ImageView flagView;
        TextView device_id;
        TextView battery_level;
        TextView available_disk_space;
        TextView downloaded_files;
//        TextView countryNameView;
//        TextView populationView;
    }

}
