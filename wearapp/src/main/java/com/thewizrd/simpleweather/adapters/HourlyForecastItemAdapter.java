package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.ListDiffUtilCallback;
import com.thewizrd.simpleweather.databinding.WeatherHrforecastPanelBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HourlyForecastItemAdapter extends RecyclerView.Adapter {
    private List<HourlyForecastItemViewModel> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        private WeatherHrforecastPanelBinding binding;

        public ViewHolder(WeatherHrforecastPanelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(HourlyForecastItemViewModel model) {
            binding.setViewModel(model);
            binding.executePendingBindings();
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
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        WeatherHrforecastPanelBinding binding = WeatherHrforecastPanelBinding.inflate(inflater);
        // FOR WEAR ONLY
        // set the view's size, margins, paddings and layout parameters
        binding.getRoot().setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int paddingHoriz = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, parent.getContext().getResources().getDisplayMetrics());
        binding.getRoot().setPaddingRelative(paddingHoriz, 0, paddingHoriz, 0);
        return new ViewHolder(binding);
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ViewHolder vh = (ViewHolder) holder;
        vh.bind(mDataset.get(position));
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
