package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
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
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.lineview.LineDataSeries;
import com.thewizrd.simpleweather.controls.lineview.LineView;
import com.thewizrd.simpleweather.controls.lineview.XLabelData;
import com.thewizrd.simpleweather.controls.lineview.YEntryData;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ForecastGraphPagerAdapter<T extends BaseForecastItemViewModel> extends PagerAdapter {
    private List<T> forecasts;
    private boolean isDarkMode;
    private int itemCount = 1;

    private Handler mMainHandler;

    // Cache collection for views
    private Stack<LineView> mLineViewCache;

    // Event listeners
    private RecyclerOnClickListenerInterface onClickListener;

    public void setOnClickListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public ForecastGraphPagerAdapter() {
        this.forecasts = new ArrayList<>();
        this.mLineViewCache = new Stack<>();
        this.mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int getCount() {
        int count = 1;

        if (forecasts != null && forecasts.size() > 0) {
            T first = forecasts.get(0);

            if (first instanceof ForecastItemViewModel) {
                if (!StringUtils.isNullOrWhitespace(first.getWindSpeed()) &&
                        !StringUtils.isNullOrWhitespace(first.getPop().replace("%", ""))) {
                    count = 3;
                }
            }

            if (first instanceof HourlyForecastItemViewModel) {
                if (Settings.getAPI().equals(WeatherAPI.OPENWEATHERMAP) ||
                        Settings.getAPI().equals(WeatherAPI.METNO) ||
                        Settings.getAPI().equals(WeatherAPI.NWS)) {
                    count = 2;
                } else {
                    count = 3;
                }
            }
        }

        if (itemCount != count) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
            itemCount = count;
        }

        return itemCount;
    }

    @SuppressLint("ClickableViewAccessibility")
    private LineView createOrRecycleView(Context context) {
        final LineView view;

        if (mLineViewCache.isEmpty()) {
            view = new LineView(context);
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
        } else {
            view = mLineViewCache.pop();
        }

        return view;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        Context context = container.getContext();
        LineView view = createOrRecycleView(container.getContext());

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
                    } else {
                        view.setDrawSeriesLabels(false);
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

                    tempDataSeries.add(new LineDataSeries(context.getString(R.string.label_high), hiTempSeries));
                    if (loTempSeries != null) {
                        tempDataSeries.add(new LineDataSeries(context.getString(R.string.label_low), loTempSeries));
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
                view.setDrawSeriesLabels(false);

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
                view.setDrawSeriesLabels(false);

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
        LineView view = (LineView) object;
        container.removeView(view);
        mLineViewCache.push(view);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            default:
            case 0:
                return App.getInstance().getAppContext().getString(R.string.notificationicon_temperature);
            case 1:
                return App.getInstance().getAppContext().getString(R.string.label_wind);
            case 2:
                return App.getInstance().getAppContext().getString(R.string.label_precipitation);
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    public void updateDataset(@NonNull final List<T> dataset) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ForecastItemDiffCallBack(forecasts, dataset), false);
        diffResult.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                forecasts.clear();
                forecasts.addAll(dataset);
                notifyDataSetChanged();
            }

            @Override
            public void onRemoved(int position, int count) {
                forecasts.clear();
                forecasts.addAll(dataset);
                notifyDataSetChanged();
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                forecasts.clear();
                forecasts.addAll(dataset);
                notifyDataSetChanged();
            }

            @Override
            public void onChanged(int position, int count, @Nullable Object payload) {
                forecasts.clear();
                forecasts.addAll(dataset);
                notifyDataSetChanged();
            }
        });
    }

    public void updateColors(boolean isDark) {
        isDarkMode = isDark;
        notifyDataSetChanged();
    }

    private class ForecastItemDiffCallBack extends DiffUtil.Callback {

        private List<T> oldList;
        private List<T> newList;

        public ForecastItemDiffCallBack(@NonNull List<T> oldList, @NonNull List<T> newList) {
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
            return oldList.get(oldItemPosition).getDate().equals(newList.get(newItemPosition).getDate());
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
