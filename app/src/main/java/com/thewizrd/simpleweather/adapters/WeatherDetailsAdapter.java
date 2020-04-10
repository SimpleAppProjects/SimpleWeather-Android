package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.simpleweather.controls.WeatherDetailItem;

public class WeatherDetailsAdapter<T extends BaseForecastItemViewModel>
        extends PagedListAdapter<T, WeatherDetailsAdapter.ViewHolder> {
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
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
    public WeatherDetailsAdapter() {
        super((DiffUtil.ItemCallback<T>) diffCallback);
    }

    private static DiffUtil.ItemCallback<BaseForecastItemViewModel> diffCallback = new DiffUtil.ItemCallback<BaseForecastItemViewModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull BaseForecastItemViewModel oldItem, @NonNull BaseForecastItemViewModel newItem) {
            return ObjectsCompat.equals(oldItem.getDate(), newItem.getDate());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BaseForecastItemViewModel oldItem, @NonNull BaseForecastItemViewModel newItem) {
            return ObjectsCompat.equals(oldItem, newItem);
        }
    };

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(new WeatherDetailItem(parent.getContext()));
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.bind(getItem(position));
    }
}
