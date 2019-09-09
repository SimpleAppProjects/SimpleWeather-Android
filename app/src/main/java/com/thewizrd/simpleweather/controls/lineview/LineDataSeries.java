package com.thewizrd.simpleweather.controls.lineview;

import java.util.List;

public class LineDataSeries {
    private String seriesLabel;
    private List<YEntryData> seriesData;

    public LineDataSeries(List<YEntryData> seriesData) {
        if (seriesData == null || seriesData.size() <= 0) {
            throw new IllegalArgumentException("Series data cannot be empty or null");
        }
        this.seriesData = seriesData;
        this.seriesLabel = null;
    }

    public LineDataSeries(String seriesLabel, List<YEntryData> seriesData) {
        if (seriesData == null || seriesData.size() <= 0) {
            throw new IllegalArgumentException("Series data cannot be empty or null");
        }
        this.seriesData = seriesData;
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
}
