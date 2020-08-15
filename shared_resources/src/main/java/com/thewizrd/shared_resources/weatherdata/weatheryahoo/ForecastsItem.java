package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class ForecastsItem {

	@SerializedName("date")
	private long date;

	@SerializedName("high")
	private int high;

	@SerializedName("code")
	private int code;

	@SerializedName("low")
	private int low;

	@SerializedName("text")
	private String text;

	@SerializedName("day")
	private String day;

	public void setDate(long date) {
		this.date = date;
	}

	public long getDate() {
		return date;
	}

	public void setHigh(int high) {
		this.high = high;
	}

	public int getHigh() {
		return high;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setLow(int low) {
		this.low = low;
	}

	public int getLow() {
		return low;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public String getDay() {
		return day;
	}
}