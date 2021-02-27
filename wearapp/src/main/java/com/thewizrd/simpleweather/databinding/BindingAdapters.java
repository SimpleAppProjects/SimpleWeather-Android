package com.thewizrd.simpleweather.databinding;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.databinding.BindingAdapter;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherDetailsType;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.ForecastPanel;
import com.thewizrd.simpleweather.controls.HourlyForecastPanel;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Collection;
import java.util.List;

public class BindingAdapters {
    @BindingAdapter("forecasts")
    public static void updateForecasts(final ForecastPanel view, final List<ForecastItemViewModel> models) {
        view.bindModel(models);
    }

    @BindingAdapter("forecasts")
    public static void updateForecasts(final HourlyForecastPanel view, final List<HourlyForecastItemViewModel> models) {
        view.bindModel(models);
    }

    @BindingAdapter(value = {"popData", "hideIfTrue"}, requireAll = false)
    public static void updatePopLayout(@NonNull ViewGroup view, List<DetailItemViewModel> details, boolean hideIfTrue) {
        ImageView popIcon = view.findViewWithTag("popicon");
        TextView pop = view.findViewWithTag("pop");

        if (popIcon == null || pop == null) return;

        if (details != null) {
            DetailItemViewModel chanceModel = Iterables.find(details, new Predicate<DetailItemViewModel>() {
                @Override
                public boolean apply(@NullableDecl DetailItemViewModel input) {
                    return input != null && input.getDetailsType() == WeatherDetailsType.POPCHANCE;
                }
            }, null);

            if (chanceModel != null) {
                popIcon.setImageResource(chanceModel.getIcon());
                pop.setText(chanceModel.getValue());
                popIcon.setVisibility(View.VISIBLE);
                pop.setVisibility(View.VISIBLE);
                view.setVisibility(View.VISIBLE);
            } else {
                popIcon.setVisibility(View.GONE);
                pop.setVisibility(View.GONE);
                if (hideIfTrue) {
                    view.setVisibility(View.GONE);
                }
            }
        } else {
            popIcon.setVisibility(View.GONE);
            pop.setVisibility(View.GONE);
            if (hideIfTrue) {
                view.setVisibility(View.GONE);
            }
        }
    }

    @BindingAdapter(value = {"windData", "hideIfTrue"}, requireAll = false)
    public static void updateWindLayout(@NonNull ViewGroup view, List<DetailItemViewModel> details, boolean hideIfTrue) {
        ImageView windIcon = view.findViewWithTag("windicon");
        TextView windSpeed = view.findViewWithTag("windspeed");

        if (windIcon == null || windSpeed == null) return;

        if (details != null) {
            DetailItemViewModel windModel = Iterables.find(details, new Predicate<DetailItemViewModel>() {
                @Override
                public boolean apply(@NullableDecl DetailItemViewModel input) {
                    return input != null && (input.getDetailsType() == WeatherDetailsType.WINDSPEED);
                }
            }, null);

            if (windModel != null) {
                if (windIcon.getDrawable() == null) {
                    windIcon.setImageResource(windModel.getIcon());
                }
                windIcon.setRotation(windModel.getIconRotation());

                String speed = TextUtils.isEmpty(windModel.getValue()) ? "" : windModel.getValue().toString();
                speed = speed.split(",")[0];

                windSpeed.setText(speed);

                windIcon.setVisibility(View.VISIBLE);
                windSpeed.setVisibility(View.VISIBLE);
                view.setVisibility(View.VISIBLE);
            } else {
                windIcon.setVisibility(View.GONE);
                windSpeed.setVisibility(View.GONE);
                if (hideIfTrue) {
                    view.setVisibility(View.GONE);
                }
            }
        } else {
            windIcon.setVisibility(View.GONE);
            windSpeed.setVisibility(View.GONE);
            if (hideIfTrue) {
                view.setVisibility(View.GONE);
            }
        }
    }

    @BindingAdapter(value = {"tempTextColor", "tempUnit"}, requireAll = false)
    public static void tempTextColor(TextView view, CharSequence temp, @Units.TemperatureUnits String tempUnit) {
        String temp_str = StringUtils.removeNonDigitChars(temp);
        Float temp_f = NumberUtils.tryParseFloat(temp_str);
        if (temp_f != null) {
            if (ObjectsCompat.equals(tempUnit, Units.CELSIUS) || temp.toString().endsWith(Units.CELSIUS)) {
                temp_f = ConversionMethods.CtoF(temp_f);
            }

            view.setTextColor(WeatherUtils.getColorFromTempF(temp_f, Colors.WHITE));
        } else {
            view.setTextColor(ContextCompat.getColor(view.getContext(), R.color.colorTextPrimary));
        }
    }

    @BindingAdapter("watchHideIfEmpty")
    public static <T extends Object> void invisibleIfEmpty(View view, Collection<T> c) {
        boolean isRound = view.getContext().getResources().getConfiguration().isScreenRound();
        view.setVisibility(c == null || c.isEmpty() ? (isRound ? View.INVISIBLE : View.GONE) : View.VISIBLE);
    }
}
