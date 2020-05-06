package com.thewizrd.simpleweather.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.controls.DetailCard;
import com.thewizrd.simpleweather.helpers.DarkMode;

import java.util.ArrayList;
import java.util.List;

public class DetailsItemGridAdapter extends BaseAdapter {
    private List<DetailItemViewModel> mDataset;

    @Override
    public int getCount() {
        return mDataset != null ? mDataset.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mDataset != null ? mDataset.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return mDataset != null && mDataset.size() > 0 ? mDataset.get(position).getDetailsType().getValue() : 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DetailCard view = convertView != null ? (DetailCard) convertView : new DetailCard(parent.getContext());
        view.setStrokeWidth((int) ActivityUtils.dpToPx(parent.getContext(), 1));

        view.bindModel(mDataset.get(position));

        switch (getDarkThemeMode()) {
            case OFF:
                view.setBackgroundColor(isLightBackground() ? getItemColor() : ColorUtils.blendARGB(getItemColor(), Colors.WHITE, 0.25f));
                view.setTextColor(isLightBackground() ? Colors.BLACK : Colors.WHITE);
                view.setStrokeColor(ColorUtils.setAlphaComponent(isLightBackground() ? Colors.BLACK : Colors.LIGHTGRAY, 0x40));
                view.setShadowColor(isLightBackground() ? Colors.GRAY : Colors.BLACK);
                break;
            case ON:
                view.setBackgroundColor(ColorUtils.blendARGB(getItemColor(), Colors.BLACK, 0.75f));
                view.setTextColor(isLightBackground() ? Colors.BLACK : Colors.WHITE);
                view.setStrokeColor(ColorUtils.setAlphaComponent(isLightBackground() ? Colors.BLACK : Colors.LIGHTGRAY, 0x40));
                view.setShadowColor(isLightBackground() ? Colors.GRAY : Colors.BLACK);
                break;
            case AMOLED_DARK:
                view.setBackgroundColor(0x90909); // 0x121212 (colorSurface) / 2
                view.setTextColor(Colors.WHITE);
                view.setStrokeColor(ColorUtils.setAlphaComponent(Colors.DARKGRAY, 0x40));
                view.setShadowColor(Colors.BLACK);
                break;
        }

        return view;
    }

    public void updateItems(final List<DetailItemViewModel> dataset) {
        if (mDataset == null || !Iterables.elementsEqual(mDataset, dataset)) {
            mDataset = new ArrayList<>(dataset);
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private DarkMode darkThemeMode;
    private @ColorInt
    int itemColor;
    private boolean isLightBackground;

    public final DarkMode getDarkThemeMode() {
        return darkThemeMode;
    }

    public final boolean isLightBackground() {
        return isLightBackground;
    }

    public final void setDarkThemeMode(DarkMode mode) {
        darkThemeMode = mode;
        notifyDataSetChanged();
    }

    public @ColorInt
    int getItemColor() {
        return itemColor;
    }

    public void setItemColor(@ColorInt int itemColor) {
        this.itemColor = itemColor;
        isLightBackground = ColorsUtils.isSuperLight(this.itemColor);
        notifyDataSetChanged();
    }
}
