package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.simpleweather.controls.WeatherDetailItem;

import java.util.ArrayList;
import java.util.List;

public class WeatherDetailsAdapter<T> extends RecyclerView.Adapter {
    private List<T> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public WeatherDetailItem mDetailPanel;

        public ViewHolder(WeatherDetailItem v) {
            super(v);
            mDetailPanel = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public WeatherDetailsAdapter(List<T> myDataset) {
        mDataset = new ArrayList<>(myDataset);
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        WeatherDetailItem v = new WeatherDetailItem(parent.getContext());
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
        if (mDataset.size() > 0 && mDataset.get(position) instanceof ForecastItemViewModel) {
            vh.mDetailPanel.setForecast((ForecastItemViewModel) mDataset.get(position));
        } else if (mDataset.size() > 0 && mDataset.get(position) instanceof HourlyForecastItemViewModel) {
            vh.mDetailPanel.setForecast((HourlyForecastItemViewModel) mDataset.get(position));
        }
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }
}
