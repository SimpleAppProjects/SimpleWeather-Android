package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.simpleweather.controls.WeatherDetailItem;

import java.util.ArrayList;
import java.util.List;

public class WeatherDetailsAdapter<T extends BaseForecastItemViewModel> extends RecyclerView.Adapter {
    private List<T> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        private WeatherDetailItem mDetailPanel;

        public ViewHolder(WeatherDetailItem v) {
            super(v);
            mDetailPanel = v;
        }

        public void bind(BaseForecastItemViewModel model) {
            mDetailPanel.bind(model);
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
        return new ViewHolder(new WeatherDetailItem(parent.getContext()));
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ViewHolder vh = (ViewHolder) holder;
        vh.bind(mDataset.get(position));
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }
}
