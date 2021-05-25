package com.thewizrd.shared_resources.weatherdata.openweather.onecall;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Rootobject {

    @SerializedName("alerts")
    private List<AlertsItem> alerts;

    @SerializedName("current")
    private Current current;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("timezone_offset")
    private int timezoneOffset;

	@SerializedName("daily")
	private List<DailyItem> daily;

	@SerializedName("lon")
	private float lon;

	@SerializedName("hourly")
	private List<HourlyItem> hourly;

	@SerializedName("minutely")
	private List<MinutelyItem> minutely;

	@SerializedName("lat")
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