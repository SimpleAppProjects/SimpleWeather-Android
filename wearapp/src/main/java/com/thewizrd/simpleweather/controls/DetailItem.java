package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.simpleweather.R;

public class DetailItem extends LinearLayout {
    private TextView detailLabel;
    private TextView detailIcon;
    private TextView detailValue;

    public DetailItem(Context context) {
        super(context);
        initialize(context);
    }

    public DetailItem(Context context, DetailItemViewModel forecastView) {
        super(context);
        initialize(context);
        setDetails(forecastView);
    }

    public DetailItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public DetailItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.detail_item_panel, this);

        detailLabel = view.findViewById(R.id.detail_label);
        detailIcon = view.findViewById(R.id.detail_icon);
        detailValue = view.findViewById(R.id.detail_value);
    }

    public void setDetails(DetailItemViewModel viewModel) {
        detailLabel.setText(viewModel.getLabel());
        detailIcon.setText(viewModel.getIcon());
        detailValue.setText(viewModel.getValue());
        detailIcon.setRotation(viewModel.getIconRotation());
    }
}
