package com.thewizrd.simpleweather.radar.rainviewer;

import androidx.annotation.NonNull;

public final class RadarFrame {
    private final String host;
    private final String path;
    private final long timeStamp;

    public RadarFrame(long timeStamp, @NonNull String host, @NonNull String path) {
        this.host = host;
        this.path = path;
        this.timeStamp = timeStamp;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
