package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.DetailCard;

import java.util.ArrayList;
import java.util.List;

public class DetailItemAdapter extends RecyclerView.Adapter {
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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        DetailCard v = new DetailCard(parent.getContext());
        return new ViewHolder(v);
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ViewHolder vh = (ViewHolder) holder;
        vh.mDetailCard.setDetails(mDataset.get(position));
    }

    @Override
    // Return the size of your dataset (invoked by the layout manager)
    public int getItemCount() {
        return mDataset.size();
    }

    public void updateItems(WeatherNowViewModel weatherNowViewModel) {
        mDataset.clear();

        if (!StringUtils.isNullOrWhitespace(weatherNowViewModel.getExtras().getChance())) {
            if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                mDataset.add(new DetailItemViewModel(R.string.label_cloudiness, R.string.wi_cloudy, weatherNowViewModel.getExtras().getChance()));
            } else {
                mDataset.add(new DetailItemViewModel(R.string.label_chance, R.string.wi_raindrop, weatherNowViewModel.getExtras().getChance()));
            }

            mDataset.add(new DetailItemViewModel(R.string.label_qpf_rain, R.string.wi_raindrops, weatherNowViewModel.getExtras().getQpfRain()));
            mDataset.add(new DetailItemViewModel(R.string.label_qpf_snow, R.string.wi_snowflake_cold, weatherNowViewModel.getExtras().getQpfSnow()));
        }

        mDataset.add(new DetailItemViewModel(R.string.label_humidity, R.string.wi_humidity, weatherNowViewModel.getHumidity()));
        mDataset.add(new DetailItemViewModel(R.string.label_pressure, R.string.wi_barometer,
                weatherNowViewModel.getRisingIcon() + " " + weatherNowViewModel.getPressure()));
        mDataset.add(new DetailItemViewModel(R.string.label_visibility, R.string.wi_fog, weatherNowViewModel.getVisibility()));
        mDataset.add(new DetailItemViewModel(R.string.label_feelslike, R.string.wi_thermometer, weatherNowViewModel.getWindChill()));
        mDataset.add(new DetailItemViewModel(R.string.label_wind, R.string.wi_wind_direction, weatherNowViewModel.getWindSpeed(), weatherNowViewModel.getWindDirection()));
        mDataset.add(new DetailItemViewModel(R.string.label_sunrise, R.string.wi_sunrise, weatherNowViewModel.getSunrise()));
        mDataset.add(new DetailItemViewModel(R.string.label_sunset, R.string.wi_sunset, weatherNowViewModel.getSunset()));

        notifyDataSetChanged();
    }
}
