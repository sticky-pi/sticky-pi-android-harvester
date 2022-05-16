package com.example.sticky_pi_data_harvester;

import java.io.File;
import java.io.FilenameFilter;

public class FileHandler extends Thread{
    long last_img_seen;
    String m_directory;
    String device_id;
    int n_jpg_images = 0;
    int n_trace_images = 0;


    FileHandler(String directory){
        super();
        m_directory = directory;
        //fixme, set an observer to reindex/update on update
        device_id = new File(m_directory).getName();
        index_files();

        // this is a stub only
        last_img_seen = 0;

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
       File directory = new File(m_directory);
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
               }
               if(img.getName().endsWith(".trace")){
                   tmp_n_trace_images +=1;
               }
           }
       }
        n_jpg_images = tmp_n_jpg_images;
        n_trace_images = tmp_n_trace_images;
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

