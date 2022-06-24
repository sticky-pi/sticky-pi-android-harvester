package com.piee.sticky_pi_data_harvester;


import android.content.Context;
import android.util.AttributeSet;

import de.codecrafters.tableview.SortableTableView;
import de.codecrafters.tableview.model.TableColumnWeightModel;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;
import de.codecrafters.tableview.toolkit.SortStateViewProviders;

public class FileSortableTable extends SortableTableView<FileHandler> {

    public FileSortableTable(final Context context) {

        this(context, null);

    }

    public FileSortableTable(final Context context, final AttributeSet attributes) {
        this(context, attributes, android.R.attr.listViewStyle);
    }

    public FileSortableTable(final Context context, final AttributeSet attributes, final int styleAttributes) {
        super(context, attributes, styleAttributes);

        final SimpleTableHeaderAdapter simpleTableHeaderAdapter = new SimpleTableHeaderAdapter(context, "Device", "Images", "GB", "Seen");
        //simpleTableHeaderAdapter.setTextColor(ContextCompat.getColor(context, R.color.table_header_text));
        setHeaderAdapter(simpleTableHeaderAdapter);

//        final int rowColorEven = ContextCompat.getColor(context, R.color.table_data_row_even);
//        final int rowColorOdd = ContextCompat.getColor(context, R.color.table_data_row_odd);
//        setDataRowBackgroundProvider(TableDataRowBackgroundProviders.alternatingRowColors(rowColorEven, rowColorOdd));
        setHeaderSortStateViewProvider(SortStateViewProviders.brightArrows());

        final TableColumnWeightModel tableColumnWeightModel = new TableColumnWeightModel(4);
        tableColumnWeightModel.setColumnWeight(0, 4);
        tableColumnWeightModel.setColumnWeight(1, 4);
        tableColumnWeightModel.setColumnWeight(2, 3);
        tableColumnWeightModel.setColumnWeight(3, 4);
//        tableColumnWeightModel.setColumnWeight(4,3);

//        tableColumnWeightModel.setColumnWeight(3, 2);
        setColumnModel(tableColumnWeightModel);

        setColumnComparator(0, FileComparators.getDeviceIDComparator());
        setColumnComparator(1, FileComparators.getImagesComparator());
        // setColumnComparator(2, FileComparators.getGBusedComparator());
        setColumnComparator(3, FileComparators.getLastSeenComparator());
//        setColumnComparator(2, FileComparators.getPercentUploadedComparator());

    }


}