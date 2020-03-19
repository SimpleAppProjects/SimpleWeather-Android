package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.DetailItemPanelBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class DetailItemAdapter extends RecyclerView.Adapter {
    private AsyncListDiffer<DetailItemViewModel> mDiffer;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        private DetailItemPanelBinding binding;

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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case 1:
                return new FooterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.locations_header, parent, false));
            default:
                // create a new view
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                DetailItemPanelBinding binding = DetailItemPanelBinding.inflate(inflater);
                // FOR WEAR ONLY
                // set the view's size, margins, paddings and layout parameters
                binding.getRoot().setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                int paddingHoriz = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, parent.getContext().getResources().getDisplayMetrics());
                binding.getRoot().setPaddingRelative(paddingHoriz, 0, paddingHoriz, 0);
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
        new AsyncTask<Void>().await(new Callable<Void>() {
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

    public class FooterViewHolder extends RecyclerView.ViewHolder implements HeaderSetterInterface {
        TextView header;

        FooterViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
            header.setTextColor(Colors.WHITE);
        }

        public void setHeader() {
            String creditPrefix = SimpleLibrary.getInstance().getAppContext().getString(R.string.credit_prefix);
            String weatherAPI = WeatherManager.getInstance().getWeatherAPI();

            if (WeatherAPI.WEATHERUNDERGROUND.equals(weatherAPI))
                header.setText(String.format("%s WeatherUnderground", creditPrefix));
            else if (WeatherAPI.YAHOO.equals(weatherAPI))
                header.setText(String.format("%s Yahoo!", creditPrefix));
            else if (WeatherAPI.OPENWEATHERMAP.equals(weatherAPI))
                header.setText(String.format("%s OpenWeatherMap", creditPrefix));
            else if (WeatherAPI.METNO.equals(weatherAPI))
                header.setText(String.format("%s MET Norway", creditPrefix));
            else if (WeatherAPI.HERE.equals(weatherAPI))
                header.setText(String.format("%s HERE Weather", creditPrefix));
        }
    }
}
