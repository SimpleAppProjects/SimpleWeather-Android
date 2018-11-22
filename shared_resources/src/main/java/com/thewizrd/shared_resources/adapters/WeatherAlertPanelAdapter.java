package com.thewizrd.shared_resources.adapters;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.thewizrd.shared_resources.controls.WeatherAlertPanel;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;

import java.util.ArrayList;
import java.util.List;

public class WeatherAlertPanelAdapter extends RecyclerView.Adapter {
    private List<WeatherAlertViewModel> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public WeatherAlertPanel mAlertPanel;

        public ViewHolder(WeatherAlertPanel v) {
            super(v);
            mAlertPanel = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public WeatherAlertPanelAdapter(List<WeatherAlertViewModel> myDataset) {
        if (myDataset != null) {
            mDataset = myDataset;
        } else
            mDataset = new ArrayList<>(0);
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        WeatherAlertPanel v = new WeatherAlertPanel(parent.getContext());
        // set the view's size, margins, paddings and layout parameters
        v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(v);
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ViewHolder vh = (ViewHolder) holder;
        vh.mAlertPanel.setAlert(mDataset.get(position));
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }
}
