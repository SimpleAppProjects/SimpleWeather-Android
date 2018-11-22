package com.thewizrd.shared_resources.weatherdata.here;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.math.BigDecimal;
import java.util.List;

@Root(name = "query")
public class AutoCompleteQuery {

    @Attribute(name = "created", required = false)
    String created;

    @Attribute(name = "count", required = false)
    byte count;

    @Attribute(name = "lang", required = false)
    String lang;

    @ElementList(name = "results", required = false)
    List<Place> results;

    public String getCreated() {
        return this.created;
    }

    public void setCreated(String value) {
        this.created = value;
    }

    public byte getCount() {
        return this.count;
    }

    public void setCount(byte value) {
        this.count = value;
    }

    public String getLang() {
        return this.lang;
    }

    public void setLang(String value) {
        this.lang = value;
    }

    public List<Place> getResults() {
        return this.results;
    }

    public void setResults(List<Place> value) {
        this.results = value;
    }

    public static class Country {

        @Attribute(name = "code", required = false)
        String code;

        @Text(required = false)
        String textValue;

        @Attribute(name = "woeid", required = false)
        long woeid;

        @Attribute(name = "type", required = false)
        String type;

        public String getCode() {
            return this.code;
        }

        public void setCode(String value) {
            this.code = value;
        }

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

        public long getWoeid() {
            return this.woeid;
        }

        public void setWoeid(long value) {
            this.woeid = value;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String value) {
            this.type = value;
        }

    }

    public static class BoundingBox {

        @Element(name = "southWest", required = false)
        SouthWest southWest;

        @Element(name = "northEast", required = false)
        NorthEast northEast;

        public SouthWest getSouthWest() {
            return this.southWest;
        }

        public void setSouthWest(SouthWest value) {
            this.southWest = value;
        }

        public NorthEast getNorthEast() {
            return this.northEast;
        }

        public void setNorthEast(NorthEast value) {
            this.northEast = value;
        }

    }

    public static class Timezone {

        @Text(required = false)
        String textValue;

        @Attribute(name = "woeid", required = false)
        long woeid;

        @Attribute(name = "type", required = false)
        String type;

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

        public long getWoeid() {
            return this.woeid;
        }

        public void setWoeid(long value) {
            this.woeid = value;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String value) {
            this.type = value;
        }

    }

    public static class SouthWest {

        @Element(name = "latitude", required = false)
        BigDecimal latitude;

        @Element(name = "longitude", required = false)
        BigDecimal longitude;

        public BigDecimal getLatitude() {
            return this.latitude;
        }

        public void setLatitude(BigDecimal value) {
            this.latitude = value;
        }

        public BigDecimal getLongitude() {
            return this.longitude;
        }

        public void setLongitude(BigDecimal value) {
            this.longitude = value;
        }

    }

    public static class PlaceTypeName {

        @Attribute(name = "code", required = false)
        byte code;

        @Text(required = false)
        String textValue;

        public byte getCode() {
            return this.code;
        }

        public void setCode(byte value) {
            this.code = value;
        }

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

    }

    public static class Centroid {

        @Element(name = "latitude", required = false)
        BigDecimal latitude;

        @Element(name = "longitude", required = false)
        BigDecimal longitude;

        public BigDecimal getLatitude() {
            return this.latitude;
        }

        public void setLatitude(BigDecimal value) {
            this.latitude = value;
        }

        public BigDecimal getLongitude() {
            return this.longitude;
        }

        public void setLongitude(BigDecimal value) {
            this.longitude = value;
        }

    }

    public static class Admin1 {

        @Attribute(name = "code", required = false)
        String code;

        @Text(required = false)
        String textValue;

        @Attribute(name = "woeid", required = false)
        long woeid;

        @Attribute(name = "type", required = false)
        String type;

        public String getCode() {
            return this.code;
        }

        public void setCode(String value) {
            this.code = value;
        }

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

        public long getWoeid() {
            return this.woeid;
        }

        public void setWoeid(long value) {
            this.woeid = value;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String value) {
            this.type = value;
        }

    }

    public static class Admin2 {

        @Attribute(name = "code", required = false)
        String code;

        @Text(required = false)
        String textValue;

        @Attribute(name = "woeid", required = false)
        long woeid;

        @Attribute(name = "type", required = false)
        String type;

        public String getCode() {
            return this.code;
        }

        public void setCode(String value) {
            this.code = value;
        }

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

        public long getWoeid() {
            return this.woeid;
        }

        public void setWoeid(long value) {
            this.woeid = value;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String value) {
            this.type = value;
        }

    }

    public static class Place {

        @Element(name = "country", required = false)
        Country country;

        @Element(name = "boundingBox", required = false)
        BoundingBox boundingBox;

        @Element(name = "timezone", required = false)
        Timezone timezone;

        @Element(name = "woeid", required = false)
        String woeid;

        @Element(name = "popRank", required = false)
        byte popRank;

        @Attribute(name = "uri", required = false)
        String uri;

        @Element(name = "placeTypeName", required = false)
        PlaceTypeName placeTypeName;

        @Element(name = "centroid", required = false)
        Centroid centroid;

        @Element(name = "areaRank", required = false)
        byte areaRank;

        @Element(name = "name", required = false)
        String name;

        @Element(name = "admin1", required = false)
        Admin1 admin1;

        @Element(name = "admin2", required = false)
        Admin2 admin2;

        @Element(name = "postal", required = false)
        Postal postal;

        @Attribute(name = "lang", required = false)
        String lang;

        @Element(name = "locality2", required = false)
        Locality2 locality2;

        @Element(name = "locality1", required = false)
        Locality1 locality1;

        @Element(name = "admin3", required = false)
        Object admin3;

        public Country getCountry() {
            return this.country;
        }

        public void setCountry(Country value) {
            this.country = value;
        }

        public BoundingBox getBoundingBox() {
            return this.boundingBox;
        }

        public void setBoundingBox(BoundingBox value) {
            this.boundingBox = value;
        }

        public Timezone getTimezone() {
            return this.timezone;
        }

        public void setTimezone(Timezone value) {
            this.timezone = value;
        }

        public String getWoeid() {
            return this.woeid;
        }

        public void setWoeid(String value) {
            this.woeid = value;
        }

        public byte getPopRank() {
            return this.popRank;
        }

        public void setPopRank(byte value) {
            this.popRank = value;
        }

        public String getUri() {
            return this.uri;
        }

        public void setUri(String value) {
            this.uri = value;
        }

        public PlaceTypeName getPlaceTypeName() {
            return this.placeTypeName;
        }

        public void setPlaceTypeName(PlaceTypeName value) {
            this.placeTypeName = value;
        }

        public Centroid getCentroid() {
            return this.centroid;
        }

        public void setCentroid(Centroid value) {
            this.centroid = value;
        }

        public byte getAreaRank() {
            return this.areaRank;
        }

        public void setAreaRank(byte value) {
            this.areaRank = value;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String value) {
            this.name = value;
        }

        public Admin1 getAdmin1() {
            return this.admin1;
        }

        public void setAdmin1(Admin1 value) {
            this.admin1 = value;
        }

        public Admin2 getAdmin2() {
            return this.admin2;
        }

        public void setAdmin2(Admin2 value) {
            this.admin2 = value;
        }

        public Postal getPostal() {
            return this.postal;
        }

        public void setPostal(Postal value) {
            this.postal = value;
        }

        public String getLang() {
            return this.lang;
        }

        public void setLang(String value) {
            this.lang = value;
        }

        public Locality2 getLocality2() {
            return this.locality2;
        }

        public void setLocality2(Locality2 value) {
            this.locality2 = value;
        }

        public Locality1 getLocality1() {
            return this.locality1;
        }

        public void setLocality1(Locality1 value) {
            this.locality1 = value;
        }

        public Object getAdmin3() {
            return this.admin3;
        }

        public void setAdmin3(Object value) {
            this.admin3 = value;
        }

    }

    public static class Postal {

        @Text(required = false)
        String textValue;

        @Attribute(name = "woeid", required = false)
        long woeid;

        @Attribute(name = "type", required = false)
        String type;

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

        public long getWoeid() {
            return this.woeid;
        }

        public void setWoeid(long value) {
            this.woeid = value;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String value) {
            this.type = value;
        }

    }

    public static class Locality2 {

        @Text(required = false)
        String textValue;

        @Attribute(name = "woeid", required = false)
        long woeid;

        @Attribute(name = "type", required = false)
        String type;

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

        public long getWoeid() {
            return this.woeid;
        }

        public void setWoeid(long value) {
            this.woeid = value;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String value) {
            this.type = value;
        }

    }

    public static class Locality1 {

        @Text(required = false)
        String textValue;

        @Attribute(name = "woeid", required = false)
        long woeid;

        @Attribute(name = "type", required = false)
        String type;

        public String getTextValue() {
            return this.textValue;
        }

        public void setTextValue(String value) {
            this.textValue = value;
        }

        public long getWoeid() {
            return this.woeid;
        }

        public void setWoeid(long value) {
            this.woeid = value;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String value) {
            this.type = value;
        }

    }

    public static class NorthEast {

        @Element(name = "latitude", required = false)
        BigDecimal latitude;

        @Element(name = "longitude", required = false)
        BigDecimal longitude;

        public BigDecimal getLatitude() {
            return this.latitude;
        }

        public void setLatitude(BigDecimal value) {
            this.latitude = value;
        }

        public BigDecimal getLongitude() {
            return this.longitude;
        }

        public void setLongitude(BigDecimal value) {
            this.longitude = value;
        }

    }

}