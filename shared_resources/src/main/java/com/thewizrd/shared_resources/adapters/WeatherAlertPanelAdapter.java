package com.thewizrd.shared_resources.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.WeatherAlertPanel;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;

public class WeatherAlertPanelAdapter extends ListAdapter<WeatherAlertViewModel, WeatherAlertPanelAdapter.ViewHolder> {
    public WeatherAlertPanelAdapter() {
        super(diffCallback);
    }

    private static final DiffUtil.ItemCallback<WeatherAlertViewModel> diffCallback = new DiffUtil.ItemCallback<WeatherAlertViewModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull WeatherAlertViewModel oldItem, @NonNull WeatherAlertViewModel newItem) {
            return oldItem.getAlertType() == newItem.getAlertType() && oldItem.getAlertSeverity() == newItem.getAlertSeverity();
        }

        @Override
        public boolean areContentsTheSame(@NonNull WeatherAlertViewModel oldItem, @NonNull WeatherAlertViewModel newItem) {
            return ObjectsCompat.equals(oldItem, newItem);
        }
    };

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final WeatherAlertPanel mAlertPanel;

        public ViewHolder(WeatherAlertPanel v) {
            super(v);
            mAlertPanel = v;
        }

        public void bind(WeatherAlertViewModel model) {
            mAlertPanel.bindModel(model);
        }
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        WeatherAlertPanel v = new WeatherAlertPanel(parent.getContext());
        // set the view's size, margins, paddings and layout parameters
        v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(v);
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.bind(getItem(position));
    }
}
