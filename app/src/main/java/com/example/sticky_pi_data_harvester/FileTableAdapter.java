package com.example.sticky_pi_data_harvester;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
                int denom =  fh.get_n_jpg_images() + fh.get_n_trace_images();
                if(denom >0)
                    renderedView = renderString(String.valueOf(fh.get_n_trace_images() / denom));
                else
                    renderedView = renderString("NA");
                break;
            case 3:
                renderedView = renderString(String.valueOf(fh.get_disk_use() / (1024 * 1024)));
                break;

            case 4:
                renderedView = renderString(String.valueOf(fh.get_last_seen()));
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
}