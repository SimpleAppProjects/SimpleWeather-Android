package com.thewizrd.simpleweather;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_Location;

import java.util.List;

public class LocationQueryAdapter extends RecyclerView.Adapter<LocationQueryAdapter.ViewHolder> {
    private List<AC_Location> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public LocationQueryView mLocView;
        public ViewHolder(LocationQueryView v) {
            super(v);
            mLocView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationQueryAdapter(List<AC_Location> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LocationQueryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        LocationQueryView v = new LocationQueryView(parent.getContext());
        // set the view's size, margins, paddings and layout parameters
        v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mLocView.setLocation(mDataset.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setLocations(List<AC_Location> myDataset) {
        mDataset.clear();
        mDataset.addAll(myDataset);
        notifyDataSetChanged();
    }
}
