package com.piee.sticky_pi_data_harvester;

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
        has_jpg = new File (getImagePath(root_directory,deviceID)).isFile();
        has_thumb = new File (getImgThumbnailPath(root_directory,deviceID)).isFile();
        has_trace = new File (getImgTracePath(root_directory,deviceID)).isFile();
        has_error = new File (getImgErrorPath(root_directory,deviceID)).isFile();

    }

    public String getImageDatetime() {
        return date_formatter.format(new Date(time * 1000));
    }

    public String getImageStatus() {
        if(has_jpg){
            if(has_trace)
                return  "Uploaded";
           else if(has_error)
                return  "Upload error!";
           else
               return "On phone";
        }
        else{
            if(has_trace)
                return  "Archived";
        }
        return  "Exception!";
    }

    public String getImagePath(String root_directory, String deviceID) {
        String date = date_formatter.format(new Date(time * 1000));
        String day_str = day_formatter.format(new Date(time * 1000));
        String img_path = root_directory  + "/" + day_str + "/" +deviceID + "." + date + ".jpg";

        return img_path;
    }

    public String getImgThumbnailPath(String root_directory, String deviceID) {
        return getImagePath(root_directory,deviceID) + ".thumbnail";
    }


    public String getImgErrorPath(String root_directory, String deviceID) {
        return getImagePath(root_directory,deviceID) + ".error";
    }

    public String getImgTracePath(String root_directory, String deviceID) {
        return getImagePath(root_directory,deviceID) + ".trace";
    }

    public long getTime() {
        return time;
    }
}

