package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Date {

    @SerializedName("tz_short")
    private String tzShort;

    @SerializedName("pretty")
    private String pretty;

    @SerializedName("ampm")
    private String ampm;

    @SerializedName("year")
    private int year;

    @SerializedName("isdst")
    private String isdst;

    @SerializedName("weekday")
    private String weekday;

    @SerializedName("weekday_short")
    private String weekdayShort;

    @SerializedName("epoch")
    private String epoch;

    @SerializedName("sec")
    private int sec;

    @SerializedName("min")
    private String min;

    @SerializedName("month")
    private int month;

    @SerializedName("hour")
    private int hour;

    @SerializedName("monthname_short")
    private String monthnameShort;

    @SerializedName("monthname")
    private String monthname;

    @SerializedName("tz_long")
    private String tzLong;

    @SerializedName("yday")
    private int yday;

    @SerializedName("day")
    private int day;

    public void setTzShort(String tzShort) {
        this.tzShort = tzShort;
    }

    public String getTzShort() {
        return tzShort;
    }

    public void setPretty(String pretty) {
        this.pretty = pretty;
    }

    public String getPretty() {
        return pretty;
    }

    public void setAmpm(String ampm) {
        this.ampm = ampm;
    }

    public String getAmpm() {
        return ampm;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getYear() {
        return year;
    }

    public void setIsdst(String isdst) {
        this.isdst = isdst;
    }

    public String getIsdst() {
        return isdst;
    }

    public void setWeekday(String weekday) {
        this.weekday = weekday;
    }

    public String getWeekday() {
        return weekday;
    }

    public void setWeekdayShort(String weekdayShort) {
        this.weekdayShort = weekdayShort;
    }

    public String getWeekdayShort() {
        return weekdayShort;
    }

    public void setEpoch(String epoch) {
        this.epoch = epoch;
    }

    public String getEpoch() {
        return epoch;
    }

    public void setSec(int sec) {
        this.sec = sec;
    }

    public int getSec() {
        return sec;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMin() {
        return min;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getMonth() {
        return month;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getHour() {
        return hour;
    }

    public void setMonthnameShort(String monthnameShort) {
        this.monthnameShort = monthnameShort;
    }

    public String getMonthnameShort() {
        return monthnameShort;
    }

    public void setMonthname(String monthname) {
        this.monthname = monthname;
    }

    public String getMonthname() {
        return monthname;
    }

    public void setTzLong(String tzLong) {
        this.tzLong = tzLong;
    }

    public String getTzLong() {
        return tzLong;
    }

    public void setYday(int yday) {
        this.yday = yday;
    }

    public int getYday() {
        return yday;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getDay() {
        return day;
    }
}