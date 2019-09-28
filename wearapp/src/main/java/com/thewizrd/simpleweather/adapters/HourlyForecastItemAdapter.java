package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.ListDiffUtilCallback;
import com.thewizrd.simpleweather.controls.HourlyForecastItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HourlyForecastItemAdapter extends RecyclerView.Adapter {
    private List<HourlyForecastItemViewModel> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public HourlyForecastItem mForecastItem;

        public ViewHolder(HourlyForecastItem v) {
            super(v);
            mForecastItem = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public HourlyForecastItemAdapter(List<HourlyForecastItemViewModel> myDataset) {
        mDataset = new ArrayList<>(myDataset);
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        HourlyForecastItem v = new HourlyForecastItem(parent.getContext());
        // FOR WEAR ONLY
        // set the view's size, margins, paddings and layout parameters
        v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int paddingHoriz = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, parent.getContext().getResources().getDisplayMetrics());
        v.setPaddingRelative(paddingHoriz, 0, paddingHoriz, 0);
        return new ViewHolder(v);
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ViewHolder vh = (ViewHolder) holder;
        vh.mForecastItem.setForecast(mDataset.get(position));
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }

    public void updateItems(List<HourlyForecastItemViewModel> items) {
        List<HourlyForecastItemViewModel> oldItems = new ArrayList<>(mDataset);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ListDiffUtilCallback<HourlyForecastItemViewModel>(oldItems, items) {
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(getOldList().get(oldItemPosition).getDate(), getNewList().get(newItemPosition).getDate());
            }
        });
        mDataset.clear();
        mDataset.addAll(items);
        diffResult.dispatchUpdatesTo(this);
        oldItems.clear();
    }
}
