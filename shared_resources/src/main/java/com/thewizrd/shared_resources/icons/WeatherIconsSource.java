package com.thewizrd.shared_resources.icons;

import androidx.annotation.StringDef;

import com.thewizrd.shared_resources.controls.ComboBoxItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static java.util.Arrays.asList;

public class WeatherIconsSource {
    public static final String WeatherIconsEF = "wi-erik-flowers";

    @StringDef({
            WeatherIconsEF
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface WeatherIconSource {
    }

    public static List<ComboBoxItem> WeatherIconSources = asList(
            new ComboBoxItem("Weather Icons", WeatherIconsEF)
    );
}
