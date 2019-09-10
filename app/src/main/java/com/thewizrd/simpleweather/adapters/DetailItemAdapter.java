package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.controls.DetailCard;
import com.thewizrd.simpleweather.helpers.ActivityUtils;

import java.util.ArrayList;
import java.util.List;

public class DetailItemAdapter extends ColorModeRecyclerViewAdapter<DetailItemAdapter.ViewHolder> {
    private List<DetailItemViewModel> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public DetailCard mDetailCard;

        public ViewHolder(DetailCard v) {
            super(v);
            mDetailCard = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public DetailItemAdapter() {
        mDataset = new ArrayList<>();
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        DetailCard v = new DetailCard(parent.getContext());
        v.setStrokeWidth((int) ActivityUtils.dpToPx(parent.getContext(), 1));
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull ViewHolder vh, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(vh, position, payloads);

        final boolean colorUpdateOnly;
        if (!payloads.isEmpty()) {
            colorUpdateOnly = payloads.get(0).equals(Payload.COLOR_UPDATE);
        } else {
            colorUpdateOnly = false;
        }

        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (!colorUpdateOnly) {
            vh.mDetailCard.setDetails(mDataset.get(position));
        }

        boolean isLightBackground = ColorsUtils.isSuperLight(this.getItemColor());

        switch (getDarkThemeMode()) {
            case OFF:
                vh.mDetailCard.setBackgroundColor(isLightBackground ? getItemColor() : ColorUtils.blendARGB(getItemColor(), Colors.WHITE, 0.25f));
                vh.mDetailCard.setTextColor(isLightBackground ? Colors.BLACK : Colors.WHITE);
                vh.mDetailCard.setStrokeColor(ColorUtils.setAlphaComponent(isLightBackground ? Colors.BLACK : Colors.LIGHTGRAY, 0x40));
                vh.mDetailCard.setShadowColor(isLightBackground ? Colors.GRAY : Colors.BLACK);
                break;
            case ON:
                vh.mDetailCard.setBackgroundColor(ColorUtils.blendARGB(getItemColor(), Colors.BLACK, 0.75f));
                vh.mDetailCard.setTextColor(isLightBackground ? Colors.BLACK : Colors.WHITE);
                vh.mDetailCard.setStrokeColor(ColorUtils.setAlphaComponent(isLightBackground ? Colors.BLACK : Colors.LIGHTGRAY, 0x40));
                vh.mDetailCard.setShadowColor(isLightBackground ? Colors.GRAY : Colors.BLACK);
                break;
            case AMOLED_DARK:
                vh.mDetailCard.setBackgroundColor(0x90909); // 0x121212 (colorSurface) / 2
                vh.mDetailCard.setTextColor(Colors.WHITE);
                vh.mDetailCard.setStrokeColor(ColorUtils.setAlphaComponent(Colors.DARKGRAY, 0x40));
                vh.mDetailCard.setShadowColor(Colors.BLACK);
                break;
        }
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }

    public void updateItems(WeatherNowViewModel weatherNowViewModel) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DetailItemDiffCallback(mDataset, weatherNowViewModel.getWeatherDetails()));
        mDataset.clear();
        mDataset.addAll(weatherNowViewModel.getWeatherDetails());
        diffResult.dispatchUpdatesTo(this);
    }

    private class DetailItemDiffCallback extends DiffUtil.Callback {
        private List<DetailItemViewModel> oldList;
        private List<DetailItemViewModel> newList;

        public DetailItemDiffCallback(@NonNull List<DetailItemViewModel> oldList, @NonNull List<DetailItemViewModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getDetailsType() == newList.get(newItemPosition).getDetailsType();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}
