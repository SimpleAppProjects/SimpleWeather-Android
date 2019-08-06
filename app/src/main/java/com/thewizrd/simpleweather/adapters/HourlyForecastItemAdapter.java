package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.HourlyForecastItem;
import com.thewizrd.simpleweather.helpers.ActivityUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HourlyForecastItemAdapter extends RecyclerView.Adapter {
    private List<HourlyForecastItemViewModel> mDataset;

    // Event listeners
    private RecyclerOnClickListenerInterface onClickListener;

    public void setOnClickListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public HourlyForecastItem mForecastItem;

        public ViewHolder(HourlyForecastItem v) {
            super(v);
            mForecastItem = v;
            mForecastItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener != null)
                        onClickListener.onClick(v, getAdapterPosition());
                }
            });
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public HourlyForecastItemAdapter() {
        mDataset = new ArrayList<>();
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        Context context = parent.getContext();
        HourlyForecastItem v = new HourlyForecastItem(context);

        int itemWidth = context.getResources().getDimensionPixelSize(R.dimen.hrforecast_panel_width);
        int itemHeight = context.getResources().getDimensionPixelSize(R.dimen.hrforecast_panel_height);

        if (ActivityUtils.isLargeTablet(parent.getContext())) {
            DisplayMetrics displayMetrics = parent.getContext().getResources().getDisplayMetrics();
            float screenWidth = displayMetrics.widthPixels;
            float screenHeight = displayMetrics.heightPixels;

            int desiredWidth;
            if (screenWidth <= 600)
                desiredWidth = (int) (screenWidth / 6f);
            else if (screenWidth <= 1200)
                desiredWidth = (int) (screenWidth / 12f);
            else
                desiredWidth = (int) (screenWidth / 6f);

            if (itemWidth < desiredWidth)
                itemWidth = desiredWidth;
        }
        v.setLayoutParams(new RecyclerView.LayoutParams(itemWidth, itemHeight));

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

    public void updateItems(Collection<HourlyForecastItemViewModel> items) {
        mDataset.clear();
        mDataset.addAll(items);
        notifyDataSetChanged();
    }
}
