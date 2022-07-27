package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class NwsAlerts {

    @Json(name = "watch")
    private List<WatchItem> watch;

    @Json(name = "warning")
    private List<WarningItem> warning;

    public void setWatch(List<WatchItem> watch) {
        this.watch = watch;
    }

    public List<WatchItem> getWatch() {
        return watch;
    }

    public void setWarning(List<WarningItem> warning) {
        this.warning = warning;
    }

    public List<WarningItem> getWarning() {
        return warning;
    }
}