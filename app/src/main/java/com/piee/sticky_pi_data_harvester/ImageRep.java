package com.piee.sticky_pi_data_harvester;

import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;

public class ImageRep {
    private long time;

    private boolean has_trace;
    private boolean has_jpg;
    private boolean has_thumb;

    public boolean get_has_error() {
        return has_error;
    }

    private boolean has_error;
    static DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public ImageRep(String root_directory, String deviceID, long timeStamp) {
        time = timeStamp;
        String str = getImagePath(root_directory,deviceID);
        has_jpg = new File (str).isFile();
        has_thumb = new File (getImgThumbnailPath(root_directory,deviceID)).isFile();
        has_trace = new File (getImgTracePath(root_directory,deviceID)).isFile();
        has_error = new File (getImgErrorPath(root_directory,deviceID)).isFile();

    }

    public ImageRep(String root_directory, String deviceID, String serialised_line) {
        String[] values = serialised_line.split(",", 5);
        int i = 0;
        time =  Long.parseLong(values[i++]);

        has_jpg   = int_to_bool(Integer.parseInt(values[i++]));
        has_thumb = int_to_bool(Integer.parseInt(values[i++]));
        has_trace = int_to_bool(Integer.parseInt(values[i++]));
        has_error = int_to_bool(Integer.parseInt(values[i++]));
    }
    private int bool_to_int(boolean value){
        return value ? 1 : 0;
    }
    private boolean int_to_bool(int value){
        return value != 0;
    }

    public String serialise(){
        return time + "," + bool_to_int(has_jpg) + "," + bool_to_int(has_thumb) + "," + bool_to_int(has_trace) + "," + bool_to_int(has_error) + "\n";
    }

    public String getImageDatetime() {
        LocalDateTime date = Instant.ofEpochMilli(time * 1000).atZone(ZoneOffset.UTC).toLocalDateTime();
        return date.format(date_formatter);
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
        LocalDateTime date = Instant.ofEpochMilli(time * 1000).atZone(ZoneOffset.UTC).toLocalDateTime();
        String date_str = date.format(date_formatter);
        String day_str = date_str.substring(0, 10);
        String img_path = root_directory  + "/" + day_str + "/" +deviceID + "." + date_str + ".jpg";
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

