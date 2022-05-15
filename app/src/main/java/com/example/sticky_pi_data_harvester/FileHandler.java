package com.example.sticky_pi_data_harvester;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

public class FileHandler {
    String m_directory;
    int n_jpg_images = 0;
    int n_trace_images = 0;


    FileHandler(String directory){
        m_directory = directory;
        //fixme, set an observer to reindex/update on update
        index_files();
    }
   void index_files(){
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
                   n_jpg_images +=1;
               }
               if(img.getName().endsWith(".trace")){
                   n_trace_images +=1;
               }
           }
       }

   }

}
