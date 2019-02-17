package com.thewizrd.simpleweather.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Pair;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.LineView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HourlyForecastGraphPagerAdapter extends PagerAdapter {
    private Context context;
    private List<HourlyForecastItemViewModel> hrforecasts;

    public HourlyForecastGraphPagerAdapter(Context context) {
        this.context = context;
        this.hrforecasts = new ArrayList<>();
    }

    @Override
    public int getCount() {
        if (Settings.getAPI().equals(WeatherAPI.OPENWEATHERMAP) || Settings.getAPI().equals(WeatherAPI.METNO)) {
            return 2;
        } else {
            return 3;
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LineView view = new LineView(container.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        switch (position) {
            case 0:
            default: // Temp
                view.setDrawGridLines(false);
                view.setDrawDotLine(false);
                view.setDrawDataLabels(true);
                view.setDrawIconLabels(true);
                view.setDrawGraphBackground(true);
                view.setDrawDotPoints(false);

                if (hrforecasts.size() > 0) {
                    ArrayList<Pair<String, String>> tempLabels = new ArrayList<>();
                    ArrayList<ArrayList<Float>> tempDataList = new ArrayList<>();
                    ArrayList<Pair<String, Integer>> iconLabels = new ArrayList<>();
                    ArrayList<Float> tempData = new ArrayList<>();

                    for (HourlyForecastItemViewModel forecastItemViewModel : hrforecasts) {
                        try {
                            float temp = Float.valueOf(StringUtils.removeNonDigitChars(forecastItemViewModel.getHiTemp()));

                            tempLabels.add(new Pair<>(forecastItemViewModel.getDate(), forecastItemViewModel.getHiTemp().trim()));
                            iconLabels.add(new Pair<>(forecastItemViewModel.getWeatherIcon(), 0));
                            tempData.add(temp);

                        } catch (NumberFormatException ex) {
                            Logger.writeLine(Log.DEBUG, ex);
                        }
                    }

                    tempDataList.add(tempData);
                    view.setDataLabels(iconLabels, tempLabels);
                    view.setDataList(tempDataList);
                }
                break;
            case 1: // Wind
                view.setDrawGridLines(false);
                view.setDrawDotLine(false);
                view.setDrawDataLabels(true);
                view.setDrawIconLabels(false);
                view.setDrawGraphBackground(true);
                view.setDrawDotPoints(false);

                if (hrforecasts.size() > 0) {
                    ArrayList<Pair<String, String>> windLabels = new ArrayList<>();
                    ArrayList<ArrayList<Float>> windDataList = new ArrayList<>();
                    ArrayList<Float> windData = new ArrayList<>();
                    ArrayList<Pair<String, Integer>> iconLabels = new ArrayList<>();

                    for (HourlyForecastItemViewModel forecastItemViewModel : hrforecasts) {
                        try {
                            float wind = Float.valueOf(StringUtils.removeNonDigitChars(forecastItemViewModel.getWindSpeed()));

                            windLabels.add(new Pair<>(forecastItemViewModel.getDate(), forecastItemViewModel.getWindSpeed()));
                            iconLabels.add(new Pair<>(context.getString(R.string.wi_wind_direction), forecastItemViewModel.getWindDirection()));
                            windData.add(wind);
                        } catch (NumberFormatException ex) {
                            Logger.writeLine(Log.DEBUG, ex);
                        }
                    }

                    windDataList.add(windData);
                    view.setDataLabels(iconLabels, windLabels);
                    view.setDataList(windDataList);
                }
                break;
            case 2: // PoP
                view.setDrawGridLines(false);
                view.setDrawDotLine(false);
                view.setDrawDataLabels(true);
                view.setDrawIconLabels(false);
                view.setDrawGraphBackground(true);
                view.setDrawDotPoints(false);

                if (hrforecasts.size() > 0) {
                    ArrayList<Pair<String, String>> popLabels = new ArrayList<>();
                    ArrayList<ArrayList<Float>> popDataList = new ArrayList<>();
                    ArrayList<Float> popData = new ArrayList<>();
                    ArrayList<Pair<String, Integer>> iconLabels = new ArrayList<>(1);

                    for (HourlyForecastItemViewModel forecastItemViewModel : hrforecasts) {
                        try {
                            float pop = Float.valueOf(StringUtils.removeNonDigitChars(forecastItemViewModel.getPop()));

                            popLabels.add(new Pair<>(forecastItemViewModel.getDate(), forecastItemViewModel.getPop().trim()));
                            popData.add(pop);
                        } catch (NumberFormatException ex) {
                            Logger.writeLine(Log.DEBUG, ex);
                        }
                    }

                    iconLabels.add(new Pair<>(context.getString(R.string.wi_raindrop), 0));
                    popDataList.add(popData);
                    view.setDataLabels(iconLabels, popLabels);
                    view.setDataList(popDataList);
                }
                break;
        }

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            default:
            case 0:
                return context.getString(R.string.notificationicon_temperature);
            case 1:
                return context.getString(R.string.label_wind);
            case 2:
                return context.getString(R.string.label_precipitation);
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    public void updateDataset(Collection<HourlyForecastItemViewModel> dataset) {
        hrforecasts.clear();
        hrforecasts.addAll(dataset);
        notifyDataSetChanged();
    }
}
