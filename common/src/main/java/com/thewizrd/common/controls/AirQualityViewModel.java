package com.thewizrd.common.controls;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.weatherdata.model.AirQuality;

import java.util.Objects;

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
    private String attribution;
    private String date;

    private Integer no2Index;
    private Integer o3Index;
    private Integer so2Index;
    private Integer pm25Index;
    private Integer pm10Index;
    private Integer coIndex;

    public AirQualityViewModel(AirQuality aqi) {
        final Context context = SharedModuleKt.getSharedDeps().getContext();
        this.airQuality = new DetailItemViewModel(aqi);
        this.index = this.progress = NumberUtils.getValueOrDefault(aqi.getIndex(), 0);
        this.progressMax = 301;

        if (aqi.getIndex() < 51) {
            this.progressColor = Colors.LIMEGREEN;
            this.level = context.getString(R.string.aqi_level_0_50);
            this.description = context.getString(R.string.aqi_desc_0_50);
        } else if (aqi.getIndex() < 101) {
            this.progressColor = Color.rgb(0xff, 0xde, 0x33);
            this.level = context.getString(R.string.aqi_level_51_100);
            this.description = context.getString(R.string.aqi_desc_51_100);
        } else if (aqi.getIndex() < 151) {
            this.progressColor = Color.rgb(0xff, 0x99, 0x33);
            this.level = context.getString(R.string.aqi_level_101_150);
            this.description = context.getString(R.string.aqi_desc_101_150);
        } else if (aqi.getIndex() < 201) {
            this.progressColor = Color.rgb(0xcc, 0x00, 0x33);
            this.level = context.getString(R.string.aqi_level_151_200);
            this.description = context.getString(R.string.aqi_desc_151_200);
        } else if (aqi.getIndex() < 301) {
            this.progressColor = Color.rgb(0xaa, 0x00, 0xff); // 0xff660099
            this.level = context.getString(R.string.aqi_level_201_300);
            this.description = context.getString(R.string.aqi_desc_201_300);
        } else if (aqi.getIndex() >= 301) {
            this.progressColor = Color.rgb(0xbd, 0x00, 0x35); // 0xff7e0023
            this.level = context.getString(R.string.aqi_level_300);
            this.description = context.getString(R.string.aqi_desc_300);
        }

        this.attribution = aqi.getAttribution();

        this.no2Index = aqi.getNo2();
        this.o3Index = aqi.getO3();
        this.so2Index = aqi.getSo2();
        this.pm25Index = aqi.getPm25();
        this.pm10Index = aqi.getPm10();
        this.coIndex = aqi.getCo();

        if (aqi.getDate() != null && !aqi.getDate().isEqual(DateTimeUtils.getLOCALDATETIME_MIN().toLocalDate())) {
            this.date = aqi.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.DAY_OF_THE_WEEK));
        }
    }

    @NonNull
    public DetailItemViewModel getAirQuality() {
        return airQuality;
    }

    public void setAirQuality(@NonNull DetailItemViewModel airQuality) {
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

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public Integer getNo2Index() {
        return no2Index;
    }

    public void setNo2Index(Integer no2Index) {
        this.no2Index = no2Index;
    }

    public Integer getO3Index() {
        return o3Index;
    }

    public void setO3Index(Integer o3Index) {
        this.o3Index = o3Index;
    }

    public Integer getSo2Index() {
        return so2Index;
    }

    public void setSo2Index(Integer so2Index) {
        this.so2Index = so2Index;
    }

    public Integer getPm25Index() {
        return pm25Index;
    }

    public void setPm25Index(Integer pm25Index) {
        this.pm25Index = pm25Index;
    }

    public Integer getPm10Index() {
        return pm10Index;
    }

    public void setPm10Index(Integer pm10Index) {
        this.pm10Index = pm10Index;
    }

    public Integer getCoIndex() {
        return coIndex;
    }

    public void setCoIndex(Integer coIndex) {
        this.coIndex = coIndex;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AirQualityViewModel that = (AirQualityViewModel) o;
        return index == that.index &&
                progress == that.progress &&
                progressMax == that.progressMax &&
                progressColor == that.progressColor &&
                airQuality.equals(that.airQuality) &&
                Objects.equals(level, that.level) &&
                Objects.equals(description, that.description) &&
                Objects.equals(attribution, that.attribution) &&
                Objects.equals(no2Index, that.no2Index) &&
                Objects.equals(o3Index, that.o3Index) &&
                Objects.equals(so2Index, that.so2Index) &&
                Objects.equals(pm25Index, that.pm25Index) &&
                Objects.equals(pm10Index, that.pm10Index) &&
                Objects.equals(coIndex, that.coIndex) &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                airQuality,
                index,
                level,
                description,
                progress,
                progressMax,
                progressColor,
                attribution,
                no2Index,
                o3Index,
                so2Index,
                pm25Index,
                pm10Index,
                coIndex,
                date
        );
    }
}
