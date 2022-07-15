package com.piee.sticky_pi_data_harvester;


import android.content.Context;
import android.util.AttributeSet;

import de.codecrafters.tableview.TableView;
import de.codecrafters.tableview.model.TableColumnWeightModel;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;

public class DetailTable extends TableView<ImageRep> {

    public DetailTable(final Context context) {
        this(context, null);
    }

    public DetailTable(final Context context, final AttributeSet attributes) {
        this(context, attributes, android.R.attr.listViewStyle);
    }

    public DetailTable(final Context context, final AttributeSet attributes, final int styleAttributes) {
        super(context, attributes, styleAttributes);
        final SimpleTableHeaderAdapter simpleTableHeaderAdapter = new SimpleTableHeaderAdapter(context,
                "Date", "Status");
        setHeaderAdapter(simpleTableHeaderAdapter);
        final TableColumnWeightModel tableColumnWeightModel = new TableColumnWeightModel(2);
        tableColumnWeightModel.setColumnWeight(0, 4);
        tableColumnWeightModel.setColumnWeight(1, 4);
        setColumnModel(tableColumnWeightModel);
    }


}