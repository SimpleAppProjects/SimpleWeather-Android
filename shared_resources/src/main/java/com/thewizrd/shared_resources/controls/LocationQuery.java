package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.utils.StringUtils;

public class LocationQuery extends LinearLayout {
    private View viewLayout;
    private TextView locationNameView;
    private TextView locationCountryView;
    private TextView pinIcon;

    public LocationQuery(Context context) {
        super(context);
        initialize(context);
    }

    public LocationQuery(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public LocationQuery(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        viewLayout = inflater.inflate(R.layout.location_query_view, this);

        locationNameView = viewLayout.findViewById(R.id.location_name);
        locationCountryView = viewLayout.findViewById(R.id.location_country);
        pinIcon = viewLayout.findViewById(R.id.pin);
    }

    public void setLocation(LocationQueryViewModel view) {
        locationNameView.setText(view.getLocationName());
        locationCountryView.setText(view.getLocationCountry());
        pinIcon.setVisibility(
                StringUtils.isNullOrWhitespace(view.getLocationQuery()) && StringUtils.isNullOrWhitespace(view.getLocationCountry()) ?
                        View.INVISIBLE : View.VISIBLE);
    }
}
