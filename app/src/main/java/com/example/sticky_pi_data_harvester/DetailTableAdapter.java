package com.example.sticky_pi_data_harvester;

import android.content.Context;
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
    Context m_context;

    public DetailTableAdapter(Context context, ArrayList<ImageRep> data) {
        super(context, data);
        root_img_dir = context.getApplicationContext().getExternalFilesDir(null).getPath();
        m_context = context;
    }

    @Override
    public View getCellView(int rowIndex, int columnIndex, ViewGroup parentView) {
        ImageRep img_rep = getRowData(rowIndex);
        String device_id = "test";
        parentView.findViewById(R.id.device_id);

        View renderedView = null;

        switch (columnIndex) {
            case 0:
                renderedView = renderString("TODO col1");
//                renderedView = renderString(img_rep.getImageDatetime(root_img_dir, device_id));
                break;
            case 1:
                renderedView = renderString("TODO col2");
//                renderedView = renderString(img_rep.getImageStatus(root_img_dir, device_id));
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