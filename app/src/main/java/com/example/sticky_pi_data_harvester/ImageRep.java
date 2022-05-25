package com.example.sticky_pi_data_harvester;

import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ImageRep {
    private long time;
    private boolean has_trace;
    private boolean has_jpg;
    private boolean has_thumb;
    private boolean has_error;
    static SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    static SimpleDateFormat day_formatter = new SimpleDateFormat("yyyy-MM-dd");

    public ImageRep(String root_directory, String deviceID, long timeStamp) {
        time = timeStamp;
        date_formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        day_formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        File img = new File (getImagePath(root_directory,deviceID));
        has_jpg = img.isFile();
        has_thumb = has_jpg;
        has_trace = !img.exists();
        has_error = has_trace && has_jpg;

    }

    public String getImageDatetime(String root_directory, String deviceID) {
        return date_formatter.format(new Date(time * 1000));
    }

    public String getImageStatus(String root_directory, String deviceID) {
        return "TODO";
    }

    public String getImagePath(String root_directory, String deviceID) {
        String date = date_formatter.format(new Date(time * 1000));
        String day_str = day_formatter.format(new Date(time * 1000));
        String img_path = root_directory + "/" + deviceID + "/" + day_str + "/" +deviceID + "." + date + ".jpg";

        return img_path;
    }

    public String getImgThumbnailPath(String root_directory, String deviceID) {
        return getImagePath(root_directory,deviceID) + ".thumbnail";
    }

}

