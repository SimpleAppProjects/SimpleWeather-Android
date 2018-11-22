package com.thewizrd.shared_resources.adapters;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.thewizrd.shared_resources.controls.LocationQuery;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;

import java.util.ArrayList;
import java.util.List;

public class LocationQueryAdapter extends RecyclerView.Adapter {
    private List<LocationQueryViewModel> mDataset;
    private RecyclerOnClickListenerInterface onClickListener;

    public void setOnClickListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public List<LocationQueryViewModel> getDataset() {
        return mDataset;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public LocationQuery mLocView;

        public ViewHolder(View itemView) {
            super(itemView);
            mLocView = (LocationQuery) itemView;
            mLocView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener != null)
                        onClickListener.onClick(v, getAdapterPosition());
                }
            });
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationQueryAdapter(List<LocationQueryViewModel> myDataset) {
        if (myDataset != null)
            mDataset = myDataset;
        else
            mDataset = new ArrayList<>(0);
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        LocationQuery v = new LocationQuery(parent.getContext());
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
        vh.mLocView.setLocation(mDataset.get(position));
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }

    public void setLocations(List<LocationQueryViewModel> myDataset) {
        mDataset.clear();
        mDataset.addAll(myDataset);
        notifyDataSetChanged();
    }
}
