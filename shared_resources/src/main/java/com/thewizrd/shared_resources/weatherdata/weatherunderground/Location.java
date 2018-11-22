package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.List;

@Root(name = "location")
public class Location {

    @Element(name = "zip", required = false)
    private String zip;

    @Element(name = "magic", required = false)
    private String magic;

    @Element(name = "nearby_weather_stations", required = false)
    private NearbyWeatherStations nearbyWeatherStations;

    @Element(name = "country", required = false)
    private String country;

    @Element(name = "tz_short", required = false)
    private String tzShort;

    @Element(name = "termsofservice", required = false)
    private Termsofservice termsofservice;

    @Element(name = "city", required = false)
    private String city;

    @Element(name = "lon", required = false)
    private String lon;

    @Attribute(name = "type", required = false)
    private String type;

    @Element(name = "tz_unix", required = false)
    private String tzUnix;

    @Element(name = "radar", required = false)
    private Radar radar;

    @Element(name = "requesturl", required = false)
    private String requesturl;

    @Element(name = "wuiurl", required = false)
    private String wuiurl;

    @Element(name = "wmo", required = false)
    private String wmo;

    @Element(name = "state", required = false)
    private String state;

    @Element(name = "lat", required = false)
    private String lat;

    public String getQuery() {
        return String.format("/q/zmw:%s.%s.%s", zip, magic, wmo);
    }

    public String getZip() {
        return this.zip;
    }

    public void setZip(String value) {
        this.zip = value;
    }

    public String getMagic() {
        return this.magic;
    }

    public void setMagic(String value) {
        this.magic = value;
    }

    public NearbyWeatherStations getNearbyWeatherStations() {
        return this.nearbyWeatherStations;
    }

    public void setNearbyWeatherStations(NearbyWeatherStations value) {
        this.nearbyWeatherStations = value;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String value) {
        this.country = value;
    }

    public String getTzShort() {
        return this.tzShort;
    }

    public void setTzShort(String value) {
        this.tzShort = value;
    }

    public Termsofservice getTermsofservice() {
        return this.termsofservice;
    }

    public void setTermsofservice(Termsofservice value) {
        this.termsofservice = value;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String value) {
        this.city = value;
    }

    public String getLon() {
        return this.lon;
    }

    public void setLon(String value) {
        this.lon = value;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public String getTzUnix() {
        return this.tzUnix;
    }

    public void setTzUnix(String value) {
        this.tzUnix = value;
    }

    public Radar getRadar() {
        return this.radar;
    }

    public void setRadar(Radar value) {
        this.radar = value;
    }

    public String getRequesturl() {
        return this.requesturl;
    }

    public void setRequesturl(String value) {
        this.requesturl = value;
    }

    public String getWuiurl() {
        return this.wuiurl;
    }

    public void setWuiurl(String value) {
        this.wuiurl = value;
    }

    public String getWmo() {
        return this.wmo;
    }

    public void setWmo(String value) {
        this.wmo = value;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String value) {
        this.state = value;
    }

    public String getLat() {
        return this.lat;
    }

    public void setLat(String value) {
        this.lat = value;
    }

    public static class NearbyWeatherStations {

        @ElementList(name = "pws", required = false)
        private List<Station> pws;

        @ElementList(name = "airport", required = false)
        private List<Station> airport;

        public List<Station> getPws() {
            return this.pws;
        }

        public void setPws(List<Station> value) {
            this.pws = value;
        }

        public List<Station> getAirport() {
            return this.airport;
        }

        public void setAirport(List<Station> value) {
            this.airport = value;
        }

    }

    public static class Country {

        @Element(name = "#cdata-section", required = false)
        private String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

    public static class DistanceKm {

        @Element(name = "#cdata-section", required = false)
        private String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

    public static class Termsofservice {

        @Text(required = false)
        private String textValue;

        @Attribute(name = "link", required = false)
        private String link;

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

        public String getLink() {
            return this.link;
        }

        public void setLink(String value) {
            this.link = value;
        }

    }

    public static class City {

        @Element(name = "#cdata-section", required = false)
        private String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

    public static class ImageUrl {

        @Element(name = "#cdata-section", required = false)
        private String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

    public static class DistanceMi {

        @Element(name = "#cdata-section", required = false)
        String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

    public static class Url {

        @Element(name = "#cdata-section", required = false)
        private String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

    public static class Radar {

        @Element(name = "image_url", required = false)
        private ImageUrl imageUrl;

        @Element(name = "url", required = false)
        private Url url;

        public ImageUrl getImageUrl() {
            return this.imageUrl;
        }

        public void setImageUrl(ImageUrl value) {
            this.imageUrl = value;
        }

        public Url getUrl() {
            return this.url;
        }

        public void setUrl(Url value) {
            this.url = value;
        }

    }

    public static class Station {

        @Element(name = "country", required = false)
        private Country country;

        @Element(name = "distance_km", required = false)
        private DistanceKm distanceKm;

        @Element(name = "city", required = false)
        private City city;

        @Element(name = "icao", required = false)
        private String icao;

        @Element(name = "distance_mi", required = false)
        private DistanceMi distanceMi;

        @Element(name = "lon", required = false)
        private String lon;

        @Element(name = "state", required = false)
        private State state;

        @Element(name = "neighborhood", required = false)
        private Neighborhood neighborhood;

        @Element(name = "id", required = false)
        private Id id;

        @Element(name = "lat", required = false)
        private String lat;

        public Country getCountry() {
            return this.country;
        }

        public void setCountry(Country value) {
            this.country = value;
        }

        public DistanceKm getDistanceKm() {
            return this.distanceKm;
        }

        public void setDistanceKm(DistanceKm value) {
            this.distanceKm = value;
        }

        public City getCity() {
            return this.city;
        }

        public void setCity(City value) {
            this.city = value;
        }

        public String getIcao() {
            return this.icao;
        }

        public void setIcao(String value) {
            this.icao = value;
        }

        public DistanceMi getDistanceMi() {
            return this.distanceMi;
        }

        public void setDistanceMi(DistanceMi value) {
            this.distanceMi = value;
        }

        public String getLon() {
            return this.lon;
        }

        public void setLon(String value) {
            this.lon = value;
        }

        public State getState() {
            return this.state;
        }

        public void setState(State value) {
            this.state = value;
        }

        public Neighborhood getNeighborhood() {
            return this.neighborhood;
        }

        public void setNeighborhood(Neighborhood value) {
            this.neighborhood = value;
        }

        public Id getId() {
            return this.id;
        }

        public void setId(Id value) {
            this.id = value;
        }

        public String getLat() {
            return this.lat;
        }

        public void setLat(String value) {
            this.lat = value;
        }

    }

    public static class Neighborhood {

        @Element(name = "#cdata-section", required = false)
        private String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

    public static class State {

        @Element(name = "#cdata-section", required = false)
        private String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

    public static class Id {

        @Element(name = "#cdata-section", required = false)
        private String cdataSection;

        public String getCdataSection() {
            return this.cdataSection;
        }

        public void setCdataSection(String value) {
            this.cdataSection = value;
        }

    }

}