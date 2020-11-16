package com.thewizrd.simpleweather.controls.graphs;

import androidx.annotation.ColorInt;

import com.google.common.primitives.Ints;
import com.thewizrd.shared_resources.utils.Colors;

import java.util.ArrayList;
import java.util.List;

public class LineDataSeries {
    private static final int[] DEFAULT_COLORS = {Colors.SIMPLEBLUE, Colors.LIGHTSEAGREEN, Colors.YELLOWGREEN};

    private String seriesLabel;
    private List<YEntryData> seriesData;
    private List<Integer> seriesColors;

    public LineDataSeries(List<YEntryData> seriesData) {
        if (seriesData == null || seriesData.size() <= 0) {
            throw new IllegalArgumentException("Series data cannot be empty or null");
        }
        this.seriesData = seriesData;
        this.seriesLabel = null;
        this.seriesColors = Ints.asList(DEFAULT_COLORS);
    }

    public LineDataSeries(String seriesLabel, List<YEntryData> seriesData) {
        if (seriesData == null || seriesData.size() <= 0) {
            throw new IllegalArgumentException("Series data cannot be empty or null");
        }
        this.seriesData = seriesData;
        this.seriesLabel = seriesLabel;
        this.seriesColors = Ints.asList(DEFAULT_COLORS);
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
}
