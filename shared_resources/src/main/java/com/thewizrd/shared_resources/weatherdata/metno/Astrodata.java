package com.thewizrd.shared_resources.weatherdata.metno;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.math.BigDecimal;

@Root(name = "astrodata", strict = false)
public class Astrodata {

    @Element(name = "meta", required = false)
    Meta meta;

    @Element(name = "time", required = false)
    Time time;

    public Meta getMeta() {
        return this.meta;
    }

    public void setMeta(Meta value) {
        this.meta = value;
    }

    public Time getTime() {
        return this.time;
    }

    public void setTime(Time value) {
        this.time = value;
    }

    public static class Moon {

        @Attribute(name = "phase", required = false)
        String phase;

        @Attribute(name = "set", required = false)
        String set;

        @Attribute(name = "rise", required = false)
        String rise;

        @Attribute(name = "never_rise", required = false)
        boolean never_rise;

        public String getPhase() {
            return this.phase;
        }

        public void setPhase(String value) {
            this.phase = value;
        }

        public String getSet() {
            return this.set;
        }

        public void setSet(String value) {
            this.set = value;
        }

        public String getRise() {
            return this.rise;
        }

        public void setRise(String value) {
            this.rise = value;
        }

        public boolean isNever_rise() {
            return never_rise;
        }

        public void setNever_rise(boolean never_rise) {
            this.never_rise = never_rise;
        }
    }

    public static class Noon {

        @Attribute(name = "altitude", required = false)
        BigDecimal altitude;

        public BigDecimal getAltitude() {
            return this.altitude;
        }

        public void setAltitude(BigDecimal value) {
            this.altitude = value;
        }

    }

    public static class Meta {

        @Attribute(name = "licenseurl", required = false)
        String licenseurl;

        public String getLicenseurl() {
            return this.licenseurl;
        }

        public void setLicenseurl(String value) {
            this.licenseurl = value;
        }

    }

    public static class Location {

        @Element(name = "moon", required = false)
        Moon moon;

        @Attribute(name = "latitude", required = false)
        BigDecimal latitude;

        @Element(name = "sun", required = false)
        Sun sun;

        @Attribute(name = "longitude", required = false)
        BigDecimal longitude;

        public Moon getMoon() {
            return this.moon;
        }

        public void setMoon(Moon value) {
            this.moon = value;
        }

        public BigDecimal getLatitude() {
            return this.latitude;
        }

        public void setLatitude(BigDecimal value) {
            this.latitude = value;
        }

        public Sun getSun() {
            return this.sun;
        }

        public void setSun(Sun value) {
            this.sun = value;
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

        @Element(name = "location", required = false)
        Location location;

        public String getDate() {
            return this.date;
        }

        public void setDate(String value) {
            this.date = value;
        }

        public Location getLocation() {
            return this.location;
        }

        public void setLocation(Location value) {
            this.location = value;
        }

    }

    public static class Sun {

        @Attribute(name = "set", required = false)
        String set;

        @Element(name = "noon", required = false)
        Noon noon;

        @Attribute(name = "rise", required = false)
        String rise;

        @Attribute(name = "never_rise", required = false)
        boolean never_rise;

        @Attribute(name = "never_set", required = false)
        boolean never_set;

        public String getSet() {
            return this.set;
        }

        public void setSet(String value) {
            this.set = value;
        }

        public Noon getNoon() {
            return this.noon;
        }

        public void setNoon(Noon value) {
            this.noon = value;
        }

        public String getRise() {
            return this.rise;
        }

        public void setRise(String value) {
            this.rise = value;
        }

        public boolean isNever_rise() {
            return never_rise;
        }

        public void setNever_rise(boolean never_rise) {
            this.never_rise = never_rise;
        }

        public boolean isNever_set() {
            return never_set;
        }

        public void setNever_set(boolean never_set) {
            this.never_set = never_set;
        }
    }

}