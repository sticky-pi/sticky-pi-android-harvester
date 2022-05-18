package com.example.sticky_pi_data_harvester;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileHandler extends Thread{
    long last_img_seen;
    String m_directory;
    String device_id;
    int n_jpg_images = 0;
    int n_trace_images = 0;
    // private DeviceHandler devH = new DeviceHandler();


    FileHandler(String directory){
        super();
        m_directory = directory;
        //fixme, set an observer to reindex/update on update
        device_id = new File(m_directory).getName();
        index_files();

        // this is a stub only
        // last_img_seen = devH.time_created;

    }

    String get_device_id(){return device_id;}
    long get_last_seen(){return last_img_seen;}
    int get_n_jpg_images(){return n_jpg_images;}
    int get_n_trace_images(){return n_trace_images;}
    long get_disk_use(){
        // fixme
        return 0 ;
    }

    void index_files(){
        int tmp_n_jpg_images = 0;
        int tmp_n_trace_images = 0;
        long most_recent_seen = 0;

       File directory = new File(m_directory);
       List<File> imgs = new ArrayList<>();

       File[] images = directory.listFiles(new FilenameFilter() {
                                         @Override
                                         public boolean accept(File dir, String name) {
                                             return name.matches("^.*(\\.jpg)|(\\.trace)$");
                                         }
                                     }
       );

       if(images != null){
           for (File img : images){
               if(img.getName().endsWith(".jpg")){
                   tmp_n_jpg_images +=1;
                   long latest_seen = parse_date(img.getName());
                   if (latest_seen > most_recent_seen) {
                       most_recent_seen = latest_seen;
                   }

               }
               if(img.getName().endsWith(".trace")){
                   tmp_n_trace_images +=1;
               }


           }
       }
        n_jpg_images = tmp_n_jpg_images;
        n_trace_images = tmp_n_trace_images;
        last_img_seen = most_recent_seen;
    }

    // Adds on here to get updated parsed date of img name
    private long parse_date(String name) {
        long timeInSeconds = 0;
        String[] arrSplit = name.split("\\.");
        if (arrSplit.length > 2) {
            String date = arrSplit[1];
            LocalDateTime ldt = LocalDateTime.parse(date,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            timeInSeconds = ldt.toEpochSecond(ZoneOffset.UTC);
        }


        return timeInSeconds;
    }


    @Override
    public void run() {
        while (true){
//            Log.e("TODEL", "HERE WE WOULD INDEX AND UPLOAD");
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

