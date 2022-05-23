package com.example.sticky_pi_data_harvester;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import de.codecrafters.tableview.TableDataAdapter;

public class FileTableAdapter extends TableDataAdapter<FileHandler> {
    final static int TEXT_SIZE = 16;

    public FileTableAdapter(Context context, ArrayList<FileHandler>  data) {
        super(context, data);
    }

    @Override
    public View getCellView(int rowIndex, int columnIndex, ViewGroup parentView) {
        FileHandler fh = getRowData(rowIndex);
        View renderedView = null;

        switch (columnIndex) {
            case 0:
                renderedView = renderString(fh.get_device_id());
                break;
            case 1:
                renderedView = renderString(String.valueOf(fh.get_n_jpg_images() + fh.get_n_trace_images()));
                break;
            case 2:
                int denom =  (fh.get_n_jpg_images() + fh.get_n_trace_images() - fh.get_n_traced_jpg_images());
                if(denom >0)
                    renderedView = renderString(String.valueOf(String.format(
                            "%.01f",
                            (100.0 * fh.get_n_trace_images()) / (float) denom
                    )));
                else
                    renderedView = renderString("NA");
                break;
            case 3:
                double val = ((double) fh.get_disk_use()) / Math.pow(1024, 3);
                renderedView = renderString(String.format("%.02f",val));
                break;

            case 4:
                long deltaT = Instant.now().getEpochSecond() - fh.get_last_seen();
                renderedView = renderString(secs_to_human_durations(deltaT));
                break;
        }

        return renderedView;
    }

    private View renderString(final String value) {
        final TextView textView = new TextView(getContext());
        textView.setText(value);
        textView.setPadding(10, 5, 10, 5);
        textView.setTextSize(TEXT_SIZE);
        return textView;
    }

    private String secs_to_human_durations(long seconds){
        String prefix = "";
        boolean isNegative = seconds < 0;
        if (isNegative) {
            prefix = " - ";
        }
        seconds = Math.abs(seconds);

        int days = (int) Math.floor(seconds / (3600*24));
        int hrs   = (int) Math.floor(seconds / 3600);
        long mins = (long) Math.floor(seconds / 60);

        if(days >= 2){
            return prefix + days + " d ago";
        }
        if(hrs >= 2){
            return prefix + hrs + " h ago";
        }
        if(mins >= 2){
            return prefix + mins + " min ago";
        }
        if(seconds <= 2 ) {
            return prefix + "now";
        }
        return seconds + "s ago";
    }
}