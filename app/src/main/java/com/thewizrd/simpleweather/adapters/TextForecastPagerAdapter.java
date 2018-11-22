package com.thewizrd.simpleweather.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.thewizrd.shared_resources.controls.TextForecastItemViewModel;
import com.thewizrd.simpleweather.controls.TextForecastItem;

import java.util.Collection;
import java.util.List;

public class TextForecastPagerAdapter extends PagerAdapter {
    private Context context;
    private List<TextForecastItemViewModel> txt_forecast;

    public TextForecastPagerAdapter(Context context, List<TextForecastItemViewModel> txt_forecast) {
        this.context = context;
        this.txt_forecast = txt_forecast;
    }

    @Override
    public int getCount() {
        return txt_forecast.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        TextForecastItem fctItem = new TextForecastItem(context);
        fctItem.setForecast(txt_forecast.get(position));
        container.addView(fctItem);
        return fctItem;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return txt_forecast.get(position).getTitle();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    public void updateDataset(Collection<TextForecastItemViewModel> dataset) {
        txt_forecast.clear();
        txt_forecast.addAll(dataset);
        notifyDataSetChanged();
    }
}
