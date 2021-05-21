package com.thewizrd.simpleweather.controls.graphs;

import androidx.annotation.ColorInt;

import com.thewizrd.shared_resources.utils.Colors;

import java.util.ArrayList;
import java.util.List;

import kotlin.collections.ArraysKt;

public class LineDataSeries {
    private static final int[] DEFAULT_COLORS = {Colors.SIMPLEBLUE, Colors.LIGHTSEAGREEN, Colors.YELLOWGREEN};

    private String seriesLabel;
    private List<YEntryData> seriesData;
    private List<Integer> seriesColors;

    private Float seriesMin = null, seriesMax = null;

    public LineDataSeries(List<YEntryData> seriesData) {
        this.seriesData = seriesData;

        if (this.seriesData == null) {
            this.seriesData = new ArrayList<>();
        }

        this.seriesLabel = null;
        this.seriesColors = ArraysKt.asList(DEFAULT_COLORS);
    }

    public LineDataSeries(String seriesLabel, List<YEntryData> seriesData) {
        this(seriesData);
        this.seriesLabel = seriesLabel;
    }

    public String getSeriesLabel() {
        return seriesLabel;
    }

    public void setSeriesLabel(String seriesLabel) {
        this.seriesLabel = seriesLabel;
    }

    public List<YEntryData> getSeriesData() {
        return seriesData;
    }

    public void setSeriesData(List<YEntryData> seriesData) {
        this.seriesData = seriesData;
    }

    @ColorInt
    public List<Integer> getSeriesColors() {
        return seriesColors;
    }

    @ColorInt
    public int getColor(int idx) {
        return seriesColors.get(idx % seriesColors.size());
    }

    public void setSeriesColors(@ColorInt List<Integer> colors) {
        this.seriesColors = colors;
    }

    public void setSeriesColors(@ColorInt int... colors) {
        this.seriesColors = new ArrayList<>(colors.length);

        for (int color : colors) {
            this.seriesColors.add(color);
        }
    }

    public Float getSeriesMin() {
        return seriesMin;
    }

    public void setSeriesMin(Float seriesMin) {
        this.seriesMin = seriesMin;
    }

    public Float getSeriesMax() {
        return seriesMax;
    }

    public void setSeriesMax(Float seriesMax) {
        this.seriesMax = seriesMax;
    }

    public void setSeriesMinMax(Float seriesMin, Float seriesMax) {
        setSeriesMin(seriesMin);
        setSeriesMax(seriesMax);
    }
}
