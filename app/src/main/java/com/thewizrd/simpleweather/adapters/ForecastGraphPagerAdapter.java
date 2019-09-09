package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.viewpager.widget.PagerAdapter;

import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.lineview.LineDataSeries;
import com.thewizrd.simpleweather.controls.lineview.LineView;
import com.thewizrd.simpleweather.controls.lineview.XLabelData;
import com.thewizrd.simpleweather.controls.lineview.YEntryData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ForecastGraphPagerAdapter<T extends BaseForecastItemViewModel> extends PagerAdapter {
    private Context context;
    private List<T> forecasts;
    private boolean isDarkMode;

    // Event listeners
    private RecyclerOnClickListenerInterface onClickListener;

    public void setOnClickListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public ForecastGraphPagerAdapter(Context context) {
        this.context = context;
        this.forecasts = new ArrayList<>();
    }

    @Override
    public int getCount() {
        if (forecasts != null && forecasts.size() > 0) {
            T first = forecasts.get(0);

            if (first instanceof ForecastItemViewModel) {
                if (!StringUtils.isNullOrWhitespace(first.getWindSpeed()) &&
                        !StringUtils.isNullOrWhitespace(first.getPop().replace("%", ""))) {
                    return 3;
                }
            }

            if (first instanceof HourlyForecastItemViewModel) {
                if (Settings.getAPI().equals(WeatherAPI.OPENWEATHERMAP) || Settings.getAPI().equals(WeatherAPI.METNO)) {
                    return 2;
                } else {
                    return 3;
                }
            }
        }

        return 1;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        final LineView view = new LineView(container.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (onClickListener != null)
                        onClickListener.onClick(v, view.getItemPositionFromPoint(event.getX()));
                    return true;
                }
                return false;
            }
        });

        view.setLineColor(ColorUtils.setAlphaComponent(isDarkMode ? Colors.WHITE : Colors.GRAY, 0x80));
        view.setBackgroundLineColor(ColorUtils.setAlphaComponent(isDarkMode ? Colors.WHITE : Colors.GRAY, 0x80));
        view.setBottomTextColor(isDarkMode ? Colors.WHITE : Colors.BLACK);

        switch (position) {
            case 0:
            default: // Temp
                view.setDrawGridLines(false);
                view.setDrawDotLine(false);
                view.setDrawDataLabels(true);
                view.setDrawIconLabels(true);
                view.setDrawGraphBackground(true);
                view.setDrawDotPoints(false);

                if (forecasts.size() > 0) {
                    List<XLabelData> labelData = new ArrayList<>();
                    List<LineDataSeries> tempDataSeries = new ArrayList<>();
                    List<YEntryData> hiTempSeries = new ArrayList<>();
                    List<YEntryData> loTempSeries = null;

                    if (forecasts.get(0) instanceof ForecastItemViewModel) {
                        loTempSeries = new ArrayList<>();
                        view.setDrawSeriesLabels(true);
                    }

                    for (T forecastItemViewModel : forecasts) {
                        try {
                            float hiTemp = Float.valueOf(StringUtils.removeNonDigitChars(forecastItemViewModel.getHiTemp()));
                            YEntryData hiTempData = new YEntryData(hiTemp, forecastItemViewModel.getHiTemp().trim());
                            hiTempSeries.add(hiTempData);

                            if (loTempSeries != null && forecastItemViewModel instanceof ForecastItemViewModel) {
                                ForecastItemViewModel fVM = (ForecastItemViewModel) forecastItemViewModel;

                                float loTemp = Float.valueOf(StringUtils.removeNonDigitChars(fVM.getLoTemp()));
                                YEntryData loTempData = new YEntryData(loTemp, fVM.getLoTemp().trim());
                                loTempSeries.add(loTempData);
                            }

                            XLabelData xLabelData = new XLabelData(forecastItemViewModel.getDate(), forecastItemViewModel.getWeatherIcon(), 0);
                            labelData.add(xLabelData);
                        } catch (NumberFormatException ex) {
                            Logger.writeLine(Log.DEBUG, ex);
                        }
                    }

                    tempDataSeries.add(new LineDataSeries("High", hiTempSeries));
                    if (loTempSeries != null) {
                        tempDataSeries.add(new LineDataSeries("Low", loTempSeries));
                    }
                    view.setData(labelData, tempDataSeries);
                }
                break;
            case 1: // Wind
                view.setDrawGridLines(false);
                view.setDrawDotLine(false);
                view.setDrawDataLabels(true);
                view.setDrawIconLabels(false);
                view.setDrawGraphBackground(true);
                view.setDrawDotPoints(false);

                if (forecasts.size() > 0) {
                    List<XLabelData> labelData = new ArrayList<>();
                    List<LineDataSeries> windDataList = new ArrayList<>();
                    List<YEntryData> windDataSeries = new ArrayList<>();

                    for (T forecastItemViewModel : forecasts) {
                        try {
                            float wind = Float.valueOf(StringUtils.removeNonDigitChars(forecastItemViewModel.getWindSpeed()));
                            YEntryData windData = new YEntryData(wind, forecastItemViewModel.getWindSpeed());

                            windDataSeries.add(windData);
                            XLabelData xLabelData = new XLabelData(forecastItemViewModel.getDate(), context.getString(R.string.wi_wind_direction), forecastItemViewModel.getWindDirection());
                            labelData.add(xLabelData);
                        } catch (NumberFormatException ex) {
                            Logger.writeLine(Log.DEBUG, ex);
                        }
                    }

                    windDataList.add(new LineDataSeries(windDataSeries));
                    view.setData(labelData, windDataList);
                }
                break;
            case 2: // PoP
                view.setDrawGridLines(false);
                view.setDrawDotLine(false);
                view.setDrawDataLabels(true);
                view.setDrawIconLabels(false);
                view.setDrawGraphBackground(true);
                view.setDrawDotPoints(false);

                if (forecasts.size() > 0) {
                    List<XLabelData> labelData = new ArrayList<>();
                    List<LineDataSeries> popDataList = new ArrayList<>();
                    List<YEntryData> popDataSeries = new ArrayList<>();

                    for (T forecastItemViewModel : forecasts) {
                        try {
                            float pop = Float.valueOf(StringUtils.removeNonDigitChars(forecastItemViewModel.getPop()));
                            YEntryData popData = new YEntryData(pop, forecastItemViewModel.getPop().trim());

                            popDataSeries.add(popData);
                            XLabelData xLabelData = new XLabelData(forecastItemViewModel.getDate(), context.getString(R.string.wi_raindrop), 0);
                            labelData.add(xLabelData);
                        } catch (NumberFormatException ex) {
                            Logger.writeLine(Log.DEBUG, ex);
                        }
                    }

                    popDataList.add(new LineDataSeries(popDataSeries));
                    view.setData(labelData, popDataList);
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

    public void updateDataset(Collection<T> dataset) {
        forecasts.clear();
        forecasts.addAll(dataset);
        notifyDataSetChanged();
    }

    public void updateColors(boolean isDark) {
        isDarkMode = isDark;
        notifyDataSetChanged();
    }
}
