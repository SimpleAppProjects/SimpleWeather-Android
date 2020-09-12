package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class NwsAlerts {

    @SerializedName("watch")
    private List<WatchItem> watch;

    @SerializedName("warning")
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