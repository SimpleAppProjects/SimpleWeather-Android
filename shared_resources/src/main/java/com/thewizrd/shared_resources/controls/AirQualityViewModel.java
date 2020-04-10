package com.thewizrd.shared_resources.controls;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.weatherdata.AirQuality;

public class AirQualityViewModel {
    @NonNull
    private DetailItemViewModel airQuality;
    private int index;
    private String level;
    private String description;
    private int progress;
    private int progressMax;
    private @ColorInt
    int progressColor;

    public AirQualityViewModel(AirQuality aqi) {
        Context context = SimpleLibrary.getInstance().getAppContext();
        this.airQuality = new DetailItemViewModel(aqi);
        this.index = this.progress = aqi.getIndex();
        this.progressMax = 301;

        if (aqi.getIndex() < 51) {
            this.progressColor = Colors.GREEN;
            this.level = context.getString(R.string.aqi_level_0_50);
            this.description = context.getString(R.string.aqi_desc_0_50);
        } else if (aqi.getIndex() < 101) {
            this.progressColor = Colors.YELLOW;
            this.level = context.getString(R.string.aqi_level_51_100);
            this.description = context.getString(R.string.aqi_desc_51_100);
        } else if (aqi.getIndex() < 151) {
            this.progressColor = Colors.ORANGE;
            this.level = context.getString(R.string.aqi_level_101_150);
            this.description = context.getString(R.string.aqi_desc_101_150);
        } else if (aqi.getIndex() < 201) {
            this.progressColor = Colors.RED;
            this.level = context.getString(R.string.aqi_level_151_200);
            this.description = context.getString(R.string.aqi_desc_151_200);
        } else if (aqi.getIndex() < 301) {
            this.progressColor = Colors.PURPLE;
            this.level = context.getString(R.string.aqi_level_201_300);
            this.description = context.getString(R.string.aqi_desc_201_300);
        } else if (aqi.getIndex() >= 301) {
            this.progressColor = Colors.MAROON;
            this.level = context.getString(R.string.aqi_level_300);
            this.description = context.getString(R.string.aqi_desc_300);
        }
    }

    public DetailItemViewModel getAirQuality() {
        return airQuality;
    }

    public void setAirQuality(DetailItemViewModel airQuality) {
        this.airQuality = airQuality;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgressMax() {
        return progressMax;
    }

    public void setProgressMax(int progressMax) {
        this.progressMax = progressMax;
    }

    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(int progressColor) {
        this.progressColor = progressColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AirQualityViewModel that = (AirQualityViewModel) o;

        if (index != that.index) return false;
        if (progress != that.progress) return false;
        if (progressMax != that.progressMax) return false;
        if (progressColor != that.progressColor) return false;
        if (airQuality != null ? !airQuality.equals(that.airQuality) : that.airQuality != null)
            return false;
        if (level != null ? !level.equals(that.level) : that.level != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = airQuality != null ? airQuality.hashCode() : 0;
        result = 31 * result + index;
        result = 31 * result + (level != null ? level.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + progress;
        result = 31 * result + progressMax;
        result = 31 * result + progressColor;
        return result;
    }
}
