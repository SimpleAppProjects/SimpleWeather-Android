package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.TextForecast;

public class TextForecastItemViewModel {
    private String title;
    private String fctText;
    private String weatherIcon;
    private String pop;

    public TextForecastItemViewModel() {
    }

    public TextForecastItemViewModel(TextForecast txtForecast) {
        title = txtForecast.getTitle();
        weatherIcon = txtForecast.getIcon();
        fctText = Settings.isFahrenheit() ? txtForecast.getFcttext() : txtForecast.getFcttextMetric();
        pop = txtForecast.getPop() + "%";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFctText() {
        return fctText;
    }

    public void setFctText(String fctText) {
        this.fctText = fctText;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public void setWeatherIcon(String weatherIcon) {
        this.weatherIcon = weatherIcon;
    }

    public String getPop() {
        return pop;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }
}
