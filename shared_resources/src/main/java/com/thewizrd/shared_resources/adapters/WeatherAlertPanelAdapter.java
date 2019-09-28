package com.thewizrd.shared_resources.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.WeatherAlertPanel;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.helpers.ListDiffUtilCallback;

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
        mDataset = new ArrayList<>(myDataset);
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

    public void updateItems(List<WeatherAlertViewModel> items) {
        List<WeatherAlertViewModel> oldItems = new ArrayList<>(mDataset);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ListDiffUtilCallback<WeatherAlertViewModel>(oldItems, items) {
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                WeatherAlertViewModel oldItem = getOldList().get(oldItemPosition);
                WeatherAlertViewModel newItem = getNewList().get(newItemPosition);

                return oldItem.getAlertType() == newItem.getAlertType() && oldItem.getAlertSeverity() == newItem.getAlertSeverity();
            }
        });
        mDataset.clear();
        mDataset.addAll(items);
        diffResult.dispatchUpdatesTo(this);
        oldItems.clear();
    }
}
