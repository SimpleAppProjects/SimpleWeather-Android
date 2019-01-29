package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "rss", strict = false)
public class Rss {

    @Element(name = "channel", required = false)
    Channel channel;

    @Attribute(name = "version", required = false)
    String version;

    public Channel getChannel() {
        return this.channel;
    }

    public void setChannel(Channel value) {
        this.channel = value;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String value) {
        this.version = value;
    }

    public static class Atmosphere {

        @Attribute(name = "rising", required = false)
        String rising;

        @Attribute(name = "visibility", required = false)
        String visibility;

        @Attribute(name = "humidity", required = false)
        String humidity;

        @Attribute(name = "pressure", required = false)
        String pressure;

        public String getRising() {
            return this.rising;
        }

        public void setRising(String value) {
            this.rising = value;
        }

        public String getVisibility() {
            return this.visibility;
        }

        public void setVisibility(String value) {
            this.visibility = value;
        }

        public String getHumidity() {
            return this.humidity;
        }

        public void setHumidity(String value) {
            this.humidity = value;
        }

        public String getPressure() {
            return this.pressure;
        }

        public void setPressure(String value) {
            this.pressure = value;
        }

    }

    public static class Image {

        @Element(name = "width", required = false)
        String width;

        @Element(name = "link", required = false)
        String link;

        @Element(name = "title", required = false)
        String title;

        @Element(name = "url", required = false)
        String url;

        @Element(name = "height", required = false)
        String height;

        public String getWidth() {
            return this.width;
        }

        public void setWidth(String value) {
            this.width = value;
        }

        public String getLink() {
            return this.link;
        }

        public void setLink(String value) {
            this.link = value;
        }

        public String getTitle() {
            return this.title;
        }

        public void setTitle(String value) {
            this.title = value;
        }

        public String getUrl() {
            return this.url;
        }

        public void setUrl(String value) {
            this.url = value;
        }

        public String getHeight() {
            return this.height;
        }

        public void setHeight(String value) {
            this.height = value;
        }

    }

    public static class Item {

        @Element(name = "condition", required = false)
        Condition condition;

        @Element(name = "link", required = false)
        String link;

        @Element(name = "description", required = false)
        String description;

        @Element(name = "guid", required = false)
        Guid guid;

        @ElementList(name = "forecast", required = false, entry = "forecast", inline = true)
        List<Forecast> forecast;

        @Element(name = "title", required = false)
        String title;

        @Element(name = "pubDate", required = false)
        String pubDate;

        @Element(name = "lat", required = false)
        String lat;

        @Element(name = "long", required = false)
        String _long;

        public Condition getCondition() {
            return this.condition;
        }

        public void setCondition(Condition value) {
            this.condition = value;
        }

        public String getLink() {
            return this.link;
        }

        public void setLink(String value) {
            this.link = value;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String value) {
            this.description = value;
        }

        public Guid getGuid() {
            return this.guid;
        }

        public void setGuid(Guid value) {
            this.guid = value;
        }

        public List<Forecast> getForecast() {
            return this.forecast;
        }

        public void setForecast(List<Forecast> value) {
            this.forecast = value;
        }

        public String getTitle() {
            return this.title;
        }

        public void setTitle(String value) {
            this.title = value;
        }

        public String getPubDate() {
            return this.pubDate;
        }

        public void setPubDate(String value) {
            this.pubDate = value;
        }

        public String getLat() {
            return this.lat;
        }

        public void setLat(String value) {
            this.lat = value;
        }

        public String getLong() {
            return this._long;
        }

        public void setLong(String value) {
            this._long = value;
        }

    }

    public static class Condition {

        @Attribute(name = "date", required = false)
        String date;

        @Attribute(name = "temp", required = false)
        String temp;

        @Attribute(name = "code", required = false)
        String code;

        @Attribute(name = "text", required = false)
        String text;

        public String getDate() {
            return this.date;
        }

        public void setDate(String value) {
            this.date = value;
        }

        public String getTemp() {
            return this.temp;
        }

        public void setTemp(String value) {
            this.temp = value;
        }

        public String getCode() {
            return this.code;
        }

        public void setCode(String value) {
            this.code = value;
        }

        public String getText() {
            return this.text;
        }

        public void setText(String value) {
            this.text = value;
        }

    }

    public static class Channel {

        @Element(name = "atmosphere", required = false)
        Atmosphere atmosphere;

        @Element(name = "image", required = false)
        Image image;

        @Element(name = "item", required = false)
        Item item;

        @Element(name = "lastBuildDate", required = false)
        String lastBuildDate;

        @Element(name = "link", required = false)
        String link;

        @Element(name = "description", required = false)
        String description;

        @Element(name = "language", required = false)
        String language;

        @Element(name = "units", required = false)
        Units units;

        @Element(name = "title", required = false)
        String title;

        @Element(name = "astronomy", required = false)
        Astronomy astronomy;

        @Element(name = "ttl", required = false)
        String ttl;

        @Element(name = "location", required = false)
        Location location;

        @Element(name = "wind", required = false)
        Wind wind;

        public Atmosphere getAtmosphere() {
            return this.atmosphere;
        }

        public void setAtmosphere(Atmosphere value) {
            this.atmosphere = value;
        }

        public Image getImage() {
            return this.image;
        }

        public void setImage(Image value) {
            this.image = value;
        }

        public Item getItem() {
            return this.item;
        }

        public void setItem(Item value) {
            this.item = value;
        }

        public String getLastBuildDate() {
            return this.lastBuildDate;
        }

        public void setLastBuildDate(String value) {
            this.lastBuildDate = value;
        }

        public String getLink() {
            return this.link;
        }

        public void setLink(String value) {
            this.link = value;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String value) {
            this.description = value;
        }

        public String getLanguage() {
            return this.language;
        }

        public void setLanguage(String value) {
            this.language = value;
        }

        public Units getUnits() {
            return this.units;
        }

        public void setUnits(Units value) {
            this.units = value;
        }

        public String getTitle() {
            return this.title;
        }

        public void setTitle(String value) {
            this.title = value;
        }

        public Astronomy getAstronomy() {
            return this.astronomy;
        }

        public void setAstronomy(Astronomy value) {
            this.astronomy = value;
        }

        public String getTtl() {
            return this.ttl;
        }

        public void setTtl(String value) {
            this.ttl = value;
        }

        public Location getLocation() {
            return this.location;
        }

        public void setLocation(Location value) {
            this.location = value;
        }

        public Wind getWind() {
            return this.wind;
        }

        public void setWind(Wind value) {
            this.wind = value;
        }

    }

    public static class Guid {

        @Attribute(name = "isPermaLink", required = false)
        String isPermaLink;

        public String getIsPermaLink() {
            return this.isPermaLink;
        }

        public void setIsPermaLink(String value) {
            this.isPermaLink = value;
        }

    }

    public static class Location {

        @Attribute(name = "country", required = false)
        String country;

        @Attribute(name = "city", required = false)
        String city;

        @Attribute(name = "region", required = false)
        String region;

        public String getCountry() {
            return this.country;
        }

        public void setCountry(String value) {
            this.country = value;
        }

        public String getCity() {
            return this.city;
        }

        public void setCity(String value) {
            this.city = value;
        }

        public String getRegion() {
            return this.region;
        }

        public void setRegion(String value) {
            this.region = value;
        }

    }

    public static class Forecast {

        @Attribute(name = "date", required = false)
        String date;

        @Attribute(name = "high", required = false)
        String high;

        @Attribute(name = "code", required = false)
        String code;

        @Attribute(name = "low", required = false)
        String low;

        @Attribute(name = "text", required = false)
        String text;

        @Attribute(name = "day", required = false)
        String day;

        public String getDate() {
            return this.date;
        }

        public void setDate(String value) {
            this.date = value;
        }

        public String getHigh() {
            return this.high;
        }

        public void setHigh(String value) {
            this.high = value;
        }

        public String getCode() {
            return this.code;
        }

        public void setCode(String value) {
            this.code = value;
        }

        public String getLow() {
            return this.low;
        }

        public void setLow(String value) {
            this.low = value;
        }

        public String getText() {
            return this.text;
        }

        public void setText(String value) {
            this.text = value;
        }

        public String getDay() {
            return this.day;
        }

        public void setDay(String value) {
            this.day = value;
        }

    }

    public static class Units {

        @Attribute(name = "distance", required = false)
        String distance;

        @Attribute(name = "temperature", required = false)
        String temperature;

        @Attribute(name = "pressure", required = false)
        String pressure;

        @Attribute(name = "speed", required = false)
        String speed;

        public String getDistance() {
            return this.distance;
        }

        public void setDistance(String value) {
            this.distance = value;
        }

        public String getTemperature() {
            return this.temperature;
        }

        public void setTemperature(String value) {
            this.temperature = value;
        }

        public String getPressure() {
            return this.pressure;
        }

        public void setPressure(String value) {
            this.pressure = value;
        }

        public String getSpeed() {
            return this.speed;
        }

        public void setSpeed(String value) {
            this.speed = value;
        }

    }

    public static class Astronomy {

        @Attribute(name = "sunrise", required = false)
        String sunrise;

        @Attribute(name = "sunset", required = false)
        String sunset;

        public String getSunrise() {
            return this.sunrise;
        }

        public void setSunrise(String value) {
            this.sunrise = value;
        }

        public String getSunset() {
            return this.sunset;
        }

        public void setSunset(String value) {
            this.sunset = value;
        }

    }

    public static class Wind {

        @Attribute(name = "chill", required = false)
        String chill;

        @Attribute(name = "speed", required = false)
        String speed;

        @Attribute(name = "direction", required = false)
        String direction;

        public String getChill() {
            return this.chill;
        }

        public void setChill(String value) {
            this.chill = value;
        }

        public String getSpeed() {
            return this.speed;
        }

        public void setSpeed(String value) {
            this.speed = value;
        }

        public String getDirection() {
            return this.direction;
        }

        public void setDirection(String value) {
            this.direction = value;
        }

    }

}