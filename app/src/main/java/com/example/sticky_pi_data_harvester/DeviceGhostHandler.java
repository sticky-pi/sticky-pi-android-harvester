package com.example.sticky_pi_data_harvester;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceGhostHandler extends com.example.sticky_pi_data_harvester.DeviceHandler {
    JSONObject m_properties = null;
    public DeviceGhostHandler(JSONObject properties){
        super();
        m_properties = properties;
        is_ghost = true;
    }



    int safe_get_int_prop(String key){
        try {
            return m_properties.getInt(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }
    String safe_get_string_prop(String key){
        try {
            return m_properties.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }


    public int get_n_to_download(){return safe_get_int_prop("n_to_download");}

    public int get_n_downloaded(){return safe_get_int_prop("n_downloaded");}

    public int get_n_errored(){return safe_get_int_prop("n_errored");}

    public int get_n_skipped(){return safe_get_int_prop("n_skipped");}

    public int get_battery_level(){return safe_get_int_prop("battery_level");}

    public String get_device_id(){return safe_get_string_prop("device_id");}

    public String get_last_image_path(){return safe_get_string_prop("last_image_path");}

    public String get_status(){return safe_get_string_prop("status");}

    public long get_last_pace(){
        try {
            return m_properties.getLong("last_pace");
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public float get_available_disk_space(){
        try {
            return (float) m_properties.getDouble("available_disk_space");
        } catch (JSONException e) {
            e.printStackTrace();
            return 0.0F;
        }
    }


}