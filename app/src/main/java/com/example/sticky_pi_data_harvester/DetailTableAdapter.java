package com.example.sticky_pi_data_harvester;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import de.codecrafters.tableview.TableDataAdapter;

public class DetailTableAdapter extends TableDataAdapter<ImageRep> {
    final static int TEXT_SIZE = 14;
    String root_img_dir;
    String m_device_id;
    Context m_context;
    int selected_row = 0;

    public DetailTableAdapter(Context context, ArrayList<ImageRep> data, String device_id) {
        super(context, data);
        root_img_dir = context.getApplicationContext().getExternalFilesDir(null).getPath();
        m_context = context;
        String m_device_id = device_id;
    }

    @Override
    public View getCellView(int rowIndex, int columnIndex, ViewGroup parentView) {
        ImageRep img_rep = getRowData(rowIndex);

        View renderedView = null;

        switch (columnIndex) {
            case 0:
//                renderedView = renderString("TODO col1");
                renderedView = renderString(img_rep.getImageDatetime());
                break;
            case 1:
//                renderedView = renderString("TODO col2");
                renderedView = renderString(img_rep.getImageStatus());
                break;
        }
        if (selected_row == rowIndex)
            if (renderedView != null)
                renderedView.setBackgroundColor(Color.parseColor("#8899ee"));
        return renderedView;
    }

    void setSelectedRowIdx(int selectedRowIdx) {
        this.selected_row = selectedRowIdx;
    }

    private View renderString(final String value) {
        final TextView textView = new TextView(getContext());
        textView.setText(value);
        textView.setPadding(10, 5, 10, 5);
        textView.setTextSize(TEXT_SIZE);
        return textView;
    }
}