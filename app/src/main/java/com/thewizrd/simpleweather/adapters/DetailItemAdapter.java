package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.controls.DetailCard;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class DetailItemAdapter extends ColorModeRecyclerViewAdapter<DetailItemAdapter.ViewHolder> {
    private AsyncListDiffer<DetailItemViewModel> mDiffer;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        private DetailCard mDetailCard;

        public ViewHolder(DetailCard v) {
            super(v);
            mDetailCard = v;
        }

        public void bind(DetailItemViewModel model) {
            mDetailCard.bindModel(model);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public DetailItemAdapter() {
        mDiffer = new AsyncListDiffer<>(this, diffCallback);
        setHasStableIds(true);
    }

    private DiffUtil.ItemCallback<DetailItemViewModel> diffCallback = new DiffUtil.ItemCallback<DetailItemViewModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull DetailItemViewModel oldItem, @NonNull DetailItemViewModel newItem) {
            return oldItem.getDetailsType() == newItem.getDetailsType();
        }

        @Override
        public boolean areContentsTheSame(@NonNull DetailItemViewModel oldItem, @NonNull DetailItemViewModel newItem) {
            return ObjectsCompat.equals(oldItem, newItem);
        }
    };

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
            vh.bind(mDiffer.getCurrentList().get(position));
        }

        switch (getDarkThemeMode()) {
            case OFF:
                vh.mDetailCard.setBackgroundColor(isLightBackground() ? getItemColor() : ColorUtils.blendARGB(getItemColor(), Colors.WHITE, 0.25f));
                vh.mDetailCard.setTextColor(isLightBackground() ? Colors.BLACK : Colors.WHITE);
                vh.mDetailCard.setStrokeColor(ColorUtils.setAlphaComponent(isLightBackground() ? Colors.BLACK : Colors.LIGHTGRAY, 0x40));
                vh.mDetailCard.setShadowColor(isLightBackground() ? Colors.GRAY : Colors.BLACK);
                break;
            case ON:
                vh.mDetailCard.setBackgroundColor(ColorUtils.blendARGB(getItemColor(), Colors.BLACK, 0.75f));
                vh.mDetailCard.setTextColor(isLightBackground() ? Colors.BLACK : Colors.WHITE);
                vh.mDetailCard.setStrokeColor(ColorUtils.setAlphaComponent(isLightBackground() ? Colors.BLACK : Colors.LIGHTGRAY, 0x40));
                vh.mDetailCard.setShadowColor(isLightBackground() ? Colors.GRAY : Colors.BLACK);
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
        return mDiffer.getCurrentList().size();
    }

    @Override
    public long getItemId(int position) {
        return mDiffer.getCurrentList().get(position).getDetailsType().getValue();
    }

    public void updateItems(final List<DetailItemViewModel> dataset) {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                mDiffer.submitList(new ArrayList<>(dataset));
                return null;
            }
        });
    }
}
