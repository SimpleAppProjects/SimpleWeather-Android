package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.weatherdata.model.UV;

public class UVIndexViewModel {
    private CharSequence icon;
    private int index;
    private CharSequence description;
    private int progress;
    private int progressMax;
    private @ColorInt
    int progressColor;

    public UVIndexViewModel(UV uvIdx) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        icon = WeatherIcons.DAY_SUNNY;
        index = uvIdx.getIndex().intValue();
        progressMax = 11;
        progress = Math.min(index, progressMax);

        if (uvIdx.getIndex() < 3) {
            description = context.getString(R.string.uv_0);
            progressColor = Colors.LIMEGREEN;
        } else if (uvIdx.getIndex() < 6) {
            description = context.getString(R.string.uv_3);
            progressColor = Colors.YELLOW;
        } else if (uvIdx.getIndex() < 8) {
            description = context.getString(R.string.uv_6);
            progressColor = Colors.ORANGE;
        } else if (uvIdx.getIndex() < 11) {
            description = context.getString(R.string.uv_8);
            progressColor = Color.rgb(0xBD, 0x00, 0x35); // Maroon
        } else if (uvIdx.getIndex() >= 11) {
            description = context.getString(R.string.uv_11);
            progressColor = Color.rgb(0xAA, 0x00, 0xFF); // Purple
        }
    }

    public CharSequence getIcon() {
        return icon;
    }

    public void setIcon(CharSequence icon) {
        this.icon = icon;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public CharSequence getDescription() {
        return description;
    }

    public void setDescription(CharSequence description) {
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

    @ColorInt
    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(@ColorInt int progressColor) {
        this.progressColor = progressColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UVIndexViewModel that = (UVIndexViewModel) o;

        if (index != that.index) return false;
        if (progress != that.progress) return false;
        if (progressMax != that.progressMax) return false;
        if (progressColor != that.progressColor) return false;
        if (icon != null ? !icon.equals(that.icon) : that.icon != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = icon != null ? icon.hashCode() : 0;
        result = 31 * result + index;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + progress;
        result = 31 * result + progressMax;
        result = 31 * result + progressColor;
        return result;
    }
}
