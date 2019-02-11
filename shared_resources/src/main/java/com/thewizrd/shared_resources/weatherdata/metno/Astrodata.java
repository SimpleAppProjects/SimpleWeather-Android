package com.thewizrd.shared_resources.weatherdata.metno;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.math.BigDecimal;
import java.net.URL;

@Root(name = "astrodata", strict = false)
public class Astrodata {

    @Element(name = "meta", required = false)
    Meta meta;

    @Element(name = "location", required = false)
    Location location;

    public Meta getMeta() {
        return this.meta;
    }

    public void setMeta(Meta value) {
        this.meta = value;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location value) {
        this.location = value;
    }

    public static class Solarnoon {

        @Attribute(name = "elevation", required = false)
        BigDecimal elevation;

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public BigDecimal getElevation() {
            return this.elevation;
        }

        public void setElevation(BigDecimal value) {
            this.elevation = value;
        }

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Moonset {

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Sunrise {

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Moonshadow {

        @Attribute(name = "elevation", required = false)
        BigDecimal elevation;

        @Attribute(name = "azimuth", required = false)
        BigDecimal azimuth;

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public BigDecimal getElevation() {
            return this.elevation;
        }

        public void setElevation(BigDecimal value) {
            this.elevation = value;
        }

        public BigDecimal getAzimuth() {
            return this.azimuth;
        }

        public void setAzimuth(BigDecimal value) {
            this.azimuth = value;
        }

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Moonrise {

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Solarmidnight {

        @Attribute(name = "elevation", required = false)
        BigDecimal elevation;

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public BigDecimal getElevation() {
            return this.elevation;
        }

        public void setElevation(BigDecimal value) {
            this.elevation = value;
        }

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class HighMoon {

        @Attribute(name = "elevation", required = false)
        BigDecimal elevation;

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public BigDecimal getElevation() {
            return this.elevation;
        }

        public void setElevation(BigDecimal value) {
            this.elevation = value;
        }

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Moonphase {

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "value", required = false)
        BigDecimal value;

        @Attribute(name = "desc", required = false)
        String desc;

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public BigDecimal getValue() {
            return this.value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Moonposition {

        @Attribute(name = "elevation", required = false)
        BigDecimal elevation;

        @Attribute(name = "range", required = false)
        BigDecimal range;

        @Attribute(name = "azimuth", required = false)
        BigDecimal azimuth;

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public BigDecimal getElevation() {
            return this.elevation;
        }

        public void setElevation(BigDecimal value) {
            this.elevation = value;
        }

        public BigDecimal getRange() {
            return this.range;
        }

        public void setRange(BigDecimal value) {
            this.range = value;
        }

        public BigDecimal getAzimuth() {
            return this.azimuth;
        }

        public void setAzimuth(BigDecimal value) {
            this.azimuth = value;
        }

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Meta {

        @Attribute(name = "licenseurl", required = false)
        URL licenseurl;

        public URL getLicenseurl() {
            return this.licenseurl;
        }

        public void setLicenseurl(URL value) {
            this.licenseurl = value;
        }

    }

    public static class Sunset {

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

    public static class Location {

        @Attribute(name = "latitude", required = false)
        BigDecimal latitude;

        @Element(name = "time", required = false)
        Time time;

        @Attribute(name = "height", required = false)
        String height;

        @Attribute(name = "longitude", required = false)
        BigDecimal longitude;

        public BigDecimal getLatitude() {
            return this.latitude;
        }

        public void setLatitude(BigDecimal value) {
            this.latitude = value;
        }

        public Time getTime() {
            return this.time;
        }

        public void setTime(Time value) {
            this.time = value;
        }

        public String getHeight() {
            return this.height;
        }

        public void setHeight(String value) {
            this.height = value;
        }

        public BigDecimal getLongitude() {
            return this.longitude;
        }

        public void setLongitude(BigDecimal value) {
            this.longitude = value;
        }

    }

    public static class Time {

        @Attribute(name = "date", required = false)
        String date;

        @Element(name = "solarnoon", required = false)
        Solarnoon solarnoon;

        @Element(name = "moonset", required = false)
        Moonset moonset;

        @Element(name = "sunrise", required = false)
        Sunrise sunrise;

        @Element(name = "moonphase", required = false)
        Moonphase moonphase;

        @Element(name = "moonshadow", required = false)
        Moonshadow moonshadow;

        @Element(name = "moonposition", required = false)
        Moonposition moonposition;

        @Element(name = "sunset", required = false)
        Sunset sunset;

        @Element(name = "moonrise", required = false)
        Moonrise moonrise;

        @Element(name = "solarmidnight", required = false)
        Solarmidnight solarmidnight;

        @Element(name = "low_moon", required = false)
        LowMoon lowMoon;

        @Element(name = "high_moon", required = false)
        HighMoon highMoon;

        public String getDate() {
            return this.date;
        }

        public void setDate(String value) {
            this.date = value;
        }

        public Solarnoon getSolarnoon() {
            return this.solarnoon;
        }

        public void setSolarnoon(Solarnoon value) {
            this.solarnoon = value;
        }

        public Moonset getMoonset() {
            return this.moonset;
        }

        public void setMoonset(Moonset value) {
            this.moonset = value;
        }

        public Sunrise getSunrise() {
            return this.sunrise;
        }

        public void setSunrise(Sunrise value) {
            this.sunrise = value;
        }

        public Moonphase getMoonphase() {
            return this.moonphase;
        }

        public void setMoonphase(Moonphase value) {
            this.moonphase = value;
        }

        public Moonshadow getMoonshadow() {
            return this.moonshadow;
        }

        public void setMoonshadow(Moonshadow value) {
            this.moonshadow = value;
        }

        public Moonposition getMoonposition() {
            return this.moonposition;
        }

        public void setMoonposition(Moonposition value) {
            this.moonposition = value;
        }

        public Sunset getSunset() {
            return this.sunset;
        }

        public void setSunset(Sunset value) {
            this.sunset = value;
        }

        public Moonrise getMoonrise() {
            return this.moonrise;
        }

        public void setMoonrise(Moonrise value) {
            this.moonrise = value;
        }

        public Solarmidnight getSolarmidnight() {
            return this.solarmidnight;
        }

        public void setSolarmidnight(Solarmidnight value) {
            this.solarmidnight = value;
        }

        public LowMoon getLowMoon() {
            return this.lowMoon;
        }

        public void setLowMoon(LowMoon value) {
            this.lowMoon = value;
        }

        public HighMoon getHighMoon() {
            return this.highMoon;
        }

        public void setHighMoon(HighMoon value) {
            this.highMoon = value;
        }

    }

    public static class LowMoon {

        @Attribute(name = "elevation", required = false)
        BigDecimal elevation;

        @Attribute(name = "time", required = false)
        String time;

        @Attribute(name = "desc", required = false)
        String desc;

        public BigDecimal getElevation() {
            return this.elevation;
        }

        public void setElevation(BigDecimal value) {
            this.elevation = value;
        }

        public String getTime() {
            return this.time;
        }

        public void setTime(String value) {
            this.time = value;
        }

        public String getDesc() {
            return this.desc;
        }

        public void setDesc(String value) {
            this.desc = value;
        }

    }

}