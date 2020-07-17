package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.simpleweather.controls.DetailCard;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class DetailItemAdapter extends RecyclerView.Adapter<DetailItemAdapter.ViewHolder> {
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
        holder.bind(mDiffer.getCurrentList().get(position));
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
