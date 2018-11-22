package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.thewizrd.simpleweather.R;

public class CustomDropDownPreference extends DropDownPreference {
    public CustomDropDownPreference(Context context) {
        super(context);
    }

    public CustomDropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomDropDownPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomDropDownPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected ArrayAdapter createAdapter() {
        return new ArrayAdapter(getContext(), R.layout.dropdown_item);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        float scale = getContext().getResources().getDisplayMetrics().density;

        // Show spinner under preference title
        Spinner spinner = view.itemView.findViewById(R.id.spinner);
        int titlePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getContext().getResources().getDisplayMetrics());
        int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getContext().getResources().getDisplayMetrics());
        spinner.setDropDownVerticalOffset(titlePx + paddingPx - 1);
    }
}
