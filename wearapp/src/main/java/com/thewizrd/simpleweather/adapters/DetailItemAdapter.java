package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ProviderEntry;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.DetailItemPanelBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

public class DetailItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final AsyncListDiffer<DetailItemViewModel> mDiffer;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final DetailItemPanelBinding binding;

        public ViewHolder(DetailItemPanelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DetailItemViewModel model) {
            binding.setViewModel(model);
            binding.executePendingBindings();
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public DetailItemAdapter() {
        mDiffer = new AsyncListDiffer<>(this, diffCallback);
    }

    private final DiffUtil.ItemCallback<DetailItemViewModel> diffCallback = new DiffUtil.ItemCallback<DetailItemViewModel>() {
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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case 1:
                return new FooterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.locations_header, parent, false));
            default:
                // create a new view
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                DetailItemPanelBinding binding = DetailItemPanelBinding.inflate(inflater);
                // set the view's size, margins, paddings and layout parameters
                binding.getRoot().setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new ViewHolder(binding);
        }
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderSetterInterface) {
            ((HeaderSetterInterface) holder).setHeader();
        } else {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            ViewHolder vh = (ViewHolder) holder;
            vh.bind(mDiffer.getCurrentList().get(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return 1;

        return 0;
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        if (getDataCount() == 0)
            return 0;

        return getDataCount() + 1;
    }

    public int getDataCount() {
        return mDiffer.getCurrentList().size();
    }

    public void updateItems(final List<DetailItemViewModel> dataset) {
        AsyncTask.await(new Callable<Void>() {
            @Override
            public Void call() {
                mDiffer.submitList(new ArrayList<>(dataset));
                return null;
            }
        });
    }

    public interface HeaderSetterInterface {
        void setHeader();
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
        TextView header;

        FooterViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
            header.setTextColor(Colors.WHITE);
        }

        public void setHeader() {
            final Context context = header.getContext();
            final String creditPrefix = context.getString(R.string.credit_prefix);
            final String weatherAPI = WeatherManager.getInstance().getWeatherAPI();

            ProviderEntry entry = Iterables.find(WeatherAPI.APIs, wapi -> wapi != null && Objects.equals(weatherAPI, wapi.getValue()), null);
            String credit = String.format("%s %s",
                    creditPrefix,
                    entry != null ? entry.toString() : WeatherIcons.EM_DASH);

            header.setText(credit);
        }
    }
}
