package com.thewizrd.simpleweather.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewGroupCompat;

import com.google.android.material.tabs.TabLayout;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.lineview.LineDataSeries;
import com.thewizrd.simpleweather.controls.lineview.LineView;
import com.thewizrd.simpleweather.controls.lineview.XLabelData;
import com.thewizrd.simpleweather.controls.lineview.YEntryData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ForecastGraphPanel extends LinearLayout {
    private Context context;
    private LineView view;
    private TabLayout tabLayout;
    private List<GraphItemViewModel> forecasts;

    private Configuration currentConfig;

    private static final int MAX_FETCH_SIZE = 24; // 24hrs

    private enum GraphType {
        FORECASTS,
        WIND,
        PRECIPITATION
    }

    private GraphType mGraphType = GraphType.FORECASTS;

    // Event listeners
    private RecyclerOnClickListenerInterface onClickListener;

    public void setOnClickPositionListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public ForecastGraphPanel(Context context) {
        super(context);
        initialize(context);
    }

    public ForecastGraphPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ForecastGraphPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ForecastGraphPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentConfig = new Configuration(newConfig);

        updateTabColors();
        updateLineViewColors();

        view.postInvalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize(Context context) {
        this.context = context;
        this.currentConfig = new Configuration(context.getResources().getConfiguration());
        setOrientation(LinearLayout.VERTICAL);
        view = new LineView(context);
        tabLayout = new TabLayout(context);

        int lineViewHeight = context.getResources().getDimensionPixelSize(R.dimen.forecast_panel_height);
        view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, lineViewHeight));
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

        int tabHeight = (int) ActivityUtils.dpToPx(context, 48.f);
        int tabLayoutPadding = (int) ActivityUtils.dpToPx(context, 12.f);
        tabLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, tabHeight));
        tabLayout.setBackgroundColor(Colors.TRANSPARENT);
        tabLayout.setTabMode(TabLayout.MODE_AUTO);
        tabLayout.setTabIndicatorFullWidth(true);
        tabLayout.setSelectedTabIndicatorColor(Color.WHITE);
        tabLayout.setInlineLabel(true);
        ((MarginLayoutParams) tabLayout.getLayoutParams()).topMargin = tabLayoutPadding;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mGraphType = (GraphType) tab.getTag();
                resetLineView();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                view.smoothScrollTo(0, 0);
            }
        });

        this.removeAllViews();
        this.addView(view);
        this.addView(tabLayout);
        // Individual transitions on the view can cause
        // OpenGLRenderer: GL error:  GL_INVALID_VALUE
        ViewGroupCompat.setTransitionGroup(this, true);

        resetLineView();
    }

    private void updateTabs() {
        boolean isForecast = false, isWind = false, isChance = false;
        GraphItemViewModel first = forecasts != null && forecasts.size() > 0 ? forecasts.get(0) : null;

        if (first != null) {
            if (first.getTempEntryData() != null) {
                isForecast = true;
            }
            if (first.getWindEntryData() != null) {
                isWind = true;
            }
            if (first.getChanceEntryData() != null) {
                isChance = true;
            }
        }

        if (tabLayout.getTabCount() == 0) {
            TabLayout.Tab forecastTab = tabLayout.newTab();
            forecastTab.setCustomView(R.layout.forecast_graph_panel_tablayout);
            forecastTab.setText(R.string.notificationicon_temperature);
            TextView forecastIconView = forecastTab.view.findViewById(R.id.icon);
            forecastIconView.setText(R.string.wi_thermometer);
            forecastTab.setTag(GraphType.FORECASTS);
            tabLayout.addTab(forecastTab, 0, false);

            TabLayout.Tab windTab = tabLayout.newTab();
            windTab.setCustomView(R.layout.forecast_graph_panel_tablayout);
            windTab.setText(R.string.label_wind);
            TextView windIconView = windTab.view.findViewById(R.id.icon);
            windIconView.setText(R.string.wi_strong_wind);
            windTab.setTag(GraphType.WIND);
            tabLayout.addTab(windTab, 1, false);

            TabLayout.Tab precipTab = tabLayout.newTab();
            precipTab.setCustomView(R.layout.forecast_graph_panel_tablayout);
            precipTab.setText(R.string.label_precipitation);
            TextView precipIconView = precipTab.view.findViewById(R.id.icon);
            precipIconView.setText(R.string.wi_raindrop);
            precipTab.setTag(GraphType.PRECIPITATION);
            tabLayout.addTab(precipTab, 2, false);
        }

        updateTabColors();

        tabLayout.getTabAt(0).view.setVisibility(isForecast ? VISIBLE : GONE);
        tabLayout.getTabAt(1).view.setVisibility(isWind ? VISIBLE : GONE);
        tabLayout.getTabAt(2).view.setVisibility(isChance ? VISIBLE : GONE);

        if (!isForecast || !isWind || !isChance) {
            switch (mGraphType) {
                case FORECASTS:
                    if (!isForecast) {
                        if (isWind) {
                            mGraphType = GraphType.WIND;
                        } else if (isChance) {
                            mGraphType = GraphType.PRECIPITATION;
                        }
                    }
                    break;
                case WIND:
                    if (!isWind) {
                        if (isForecast) {
                            mGraphType = GraphType.FORECASTS;
                        } else if (isChance) {
                            mGraphType = GraphType.PRECIPITATION;
                        }
                    }
                    break;
                case PRECIPITATION:
                    if (!isChance) {
                        if (isForecast) {
                            mGraphType = GraphType.FORECASTS;
                        } else if (isWind) {
                            mGraphType = GraphType.WIND;
                        }
                    }
                    break;
            }
        }

        switch (mGraphType) {
            case FORECASTS:
                if (!tabLayout.getTabAt(0).isSelected())
                    tabLayout.getTabAt(0).select();
                break;
            case WIND:
                if (!tabLayout.getTabAt(1).isSelected())
                    tabLayout.getTabAt(1).select();
                break;
            case PRECIPITATION:
                if (!tabLayout.getTabAt(2).isSelected())
                    tabLayout.getTabAt(2).select();
                break;
        }
    }

    private void updateTabColors() {
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;

        TabLayout.Tab forecastTab = tabLayout.getTabAt(0);
        ((TextView) forecastTab.view.findViewById(R.id.icon)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
        ((TextView) forecastTab.view.findViewById(android.R.id.text1)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);

        TabLayout.Tab windTab = tabLayout.getTabAt(1);
        ((TextView) windTab.view.findViewById(R.id.icon)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
        ((TextView) windTab.view.findViewById(android.R.id.text1)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);

        TabLayout.Tab precipTab = tabLayout.getTabAt(2);
        ((TextView) precipTab.view.findViewById(R.id.icon)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
        ((TextView) precipTab.view.findViewById(android.R.id.text1)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);

        tabLayout.setSelectedTabIndicatorColor(isNightMode ? Colors.WHITE : Colors.BLACK);
    }

    private void updateLineViewColors() {
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;

        view.setLineColor(ColorUtils.setAlphaComponent(isNightMode ? Colors.WHITE : Colors.GRAY, 0x80));
        view.setBackgroundLineColor(ColorUtils.setAlphaComponent(isNightMode ? Colors.WHITE : Colors.GRAY, 0x80));
        view.setBottomTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
    }

    private void resetLineView() {
        updateLineViewColors();
        updateTabs();

        view.resetData(false);

        AsyncTask.await(new Callable<Void>() {
            @Override
            public Void call() {
                switch (mGraphType) {
                    case FORECASTS:
                    default: // Temp
                        updateForecastGraph();
                        break;
                    case WIND: // Wind
                        updateWindForecastGraph();
                        break;
                    case PRECIPITATION: // PoP
                        updatePrecipicationGraph();
                        break;
                }
                return null;
            }
        });

        view.smoothScrollTo(0, 0);
    }

    private void updateForecastGraph() {
        if (forecasts != null && forecasts.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(true);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);

            List<XLabelData> labelDataset = new ArrayList<>(forecasts.size());
            List<YEntryData> hiTempDataset = new ArrayList<>(forecasts.size());
            List<YEntryData> loTempDataset = null;

            if (forecasts.get(0) != null && forecasts.get(0).getTempEntryData().getEntryData().getLoTempData() != null) {
                loTempDataset = new ArrayList<>(forecasts.size());
                view.setDrawSeriesLabels(true);
            } else {
                view.setDrawSeriesLabels(false);
            }

            for (int i = 0; i < forecasts.size(); i++) {
                GraphItemViewModel graphModel = forecasts.get(i);

                if (graphModel.getTempEntryData().getEntryData().getHiTempData() != null) {
                    hiTempDataset.add(graphModel.getTempEntryData().getEntryData().getHiTempData());
                } else if (i == 0 && i + 1 < forecasts.size()) { // For NWS, which contains bi-daily forecasts
                    GraphItemViewModel nextVM = forecasts.get(i + 1);
                    if (nextVM.getTempEntryData().getEntryData().getHiTempData() != null)
                        hiTempDataset.add(nextVM.getTempEntryData().getEntryData().getHiTempData());
                } else if (i == forecasts.size() - 1) { // For NWS, which contains bi-daily forecasts
                    GraphItemViewModel prevVM = forecasts.get(i - 1);
                    if (prevVM.getTempEntryData().getEntryData().getHiTempData() != null)
                        hiTempDataset.add(prevVM.getTempEntryData().getEntryData().getHiTempData());
                }

                if (loTempDataset != null) {
                    if (graphModel.getTempEntryData().getEntryData().getLoTempData() != null) {
                        loTempDataset.add(graphModel.getTempEntryData().getEntryData().getLoTempData());
                    } else if (i == 0 && i + 1 < forecasts.size()) { // For NWS, which contains bi-daily forecasts
                        GraphItemViewModel nextVM = forecasts.get(i + 1);
                        if (nextVM.getTempEntryData().getEntryData().getLoTempData() != null)
                            loTempDataset.add(nextVM.getTempEntryData().getEntryData().getLoTempData());
                    } else if (i == forecasts.size() - 1) { // For NWS, which contains bi-daily forecasts
                        GraphItemViewModel prevVM = forecasts.get(i - 1);
                        if (prevVM.getTempEntryData().getEntryData().getLoTempData() != null)
                            loTempDataset.add(prevVM.getTempEntryData().getEntryData().getLoTempData());
                    }
                }

                labelDataset.add(graphModel.getTempEntryData().getLabelData());
            }

            view.getDataLabels().addAll(labelDataset);

            if (hiTempDataset.size() > 0) {
                final String hiTempSeriesLabel = context.getString(R.string.label_high);
                view.getDataLists().add(new LineDataSeries(hiTempSeriesLabel, hiTempDataset));
            }

            if (loTempDataset != null) {
                if (loTempDataset.size() > 0) {
                    final String loTempSeriesLabel = context.getString(R.string.label_low);
                    view.getDataLists().add(new LineDataSeries(loTempSeriesLabel, loTempDataset));
                }
            }
        }
    }

    private void updateWindForecastGraph() {
        if (forecasts != null && forecasts.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(false);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);
            view.setDrawSeriesLabels(false);

            List<XLabelData> labelData = new ArrayList<>(forecasts.size());
            List<YEntryData> windDataSet = new ArrayList<>(forecasts.size());

            for (int i = 0; i < forecasts.size(); i++) {
                GraphItemViewModel graphModel = forecasts.get(i);
                if (graphModel.getWindEntryData() != null) {
                    windDataSet.add(graphModel.getWindEntryData().getEntryData());
                    labelData.add(graphModel.getWindEntryData().getLabelData());
                }
            }

            view.getDataLabels().addAll(labelData);

            if (windDataSet.size() > 0) {
                view.getDataLists().add(new LineDataSeries(windDataSet));
            }
        }
    }

    private void updatePrecipicationGraph() {
        if (forecasts != null && forecasts.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(false);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);
            view.setDrawSeriesLabels(false);

            List<XLabelData> labelData = new ArrayList<>(forecasts.size());
            List<YEntryData> popDataSet = new ArrayList<>(forecasts.size());

            for (int i = 0; i < forecasts.size(); i++) {
                GraphItemViewModel graphModel = forecasts.get(i);
                if (graphModel.getChanceEntryData() != null) {
                    popDataSet.add(graphModel.getChanceEntryData().getEntryData());
                    labelData.add(graphModel.getChanceEntryData().getLabelData());
                }
            }

            view.getDataLabels().addAll(labelData);

            if (popDataSet.size() > 0) {
                view.getDataLists().add(new LineDataSeries(popDataSet));
            }
        }
    }

    public void updateForecasts(@NonNull final List<GraphItemViewModel> dataset) {
        if (forecasts != dataset) {
            forecasts = dataset;
            resetLineView();
        }
    }
}