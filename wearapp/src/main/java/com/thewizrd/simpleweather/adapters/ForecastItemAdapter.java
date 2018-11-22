package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ViewGroup;

import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.simpleweather.controls.ForecastItem;

import java.util.Collection;
import java.util.List;

public class ForecastItemAdapter extends RecyclerView.Adapter {
    private List<ForecastItemViewModel> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public ForecastItem mForecastItem;

        public ViewHolder(ForecastItem v) {
            super(v);
            mForecastItem = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ForecastItemAdapter(List<ForecastItemViewModel> myDataset) {
        mDataset = myDataset;
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        ForecastItem v = new ForecastItem(parent.getContext());
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

    public void updateItems(Collection<ForecastItemViewModel> items) {
        mDataset.clear();
        mDataset.addAll(items);
        notifyDataSetChanged();
    }
}
