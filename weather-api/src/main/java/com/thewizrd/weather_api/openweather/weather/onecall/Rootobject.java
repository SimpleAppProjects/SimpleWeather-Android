package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class Rootobject {

    @Json(name = "alerts")
    private List<AlertsItem> alerts;

    @Json(name = "current")
    private Current current;

    @Json(name = "timezone")
    private String timezone;

    @Json(name = "timezone_offset")
    private int timezoneOffset;

    @Json(name = "daily")
	private List<DailyItem> daily;

    @Json(name = "lon")
	private float lon;

    @Json(name = "hourly")
	private List<HourlyItem> hourly;

    @Json(name = "minutely")
	private List<MinutelyItem> minutely;

    @Json(name = "lat")
	private float lat;

	public List<AlertsItem> getAlerts() {
		return alerts;
	}

	public void setAlerts(List<AlertsItem> alerts) {
		this.alerts = alerts;
    }

    public Current getCurrent() {
        return current;
    }

    public void setCurrent(Current current) {
        this.current = current;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getTimezoneOffset() {
        return timezoneOffset;
    }

    public void setTimezoneOffset(int timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }

    public List<DailyItem> getDaily() {
        return daily;
    }

    public void setDaily(List<DailyItem> daily) {
        this.daily = daily;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
	}

	public List<HourlyItem> getHourly() {
		return hourly;
	}

	public void setHourly(List<HourlyItem> hourly) {
		this.hourly = hourly;
	}

	public List<MinutelyItem> getMinutely() {
		return minutely;
	}

	public void setMinutely(List<MinutelyItem> minutely) {
		this.minutely = minutely;
	}

	public float getLat() {
		return lat;
	}

	public void setLat(float lat) {
		this.lat = lat;
	}
}