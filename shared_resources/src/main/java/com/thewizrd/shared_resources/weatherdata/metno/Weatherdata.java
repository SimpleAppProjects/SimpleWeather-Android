package com.thewizrd.shared_resources.weatherdata.metno;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Root(name = "weatherdata", strict = false)
public class Weatherdata {

    @Element(name = "product", required = false)
    Product product;

    @Attribute(name = "created", required = false)
    String created;

    @Element(name = "meta", required = false)
    Meta meta;

    @Attribute(name = "noNamespaceSchemaLocation", required = false)
    String noNamespaceSchemaLocation;

    public Product getProduct() {
        return this.product;
    }

    public void setProduct(Product value) {
        this.product = value;
    }

    public String getCreated() {
        return this.created;
    }

    public void setCreated(String value) {
        this.created = value;
    }

    public Meta getMeta() {
        return this.meta;
    }

    public void setMeta(Meta value) {
        this.meta = value;
    }

    //public String getNoNamespaceSchemaLocation() {return this.noNamespaceSchemaLocation;}
    //public void setNoNamespaceSchemaLocation(String value) {this.noNamespaceSchemaLocation = value;}

    public static class MaxTemperature {

        @Attribute(name = "unit", required = false)
        String unit;

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "value", required = false)
        BigDecimal value;

        public String getUnit() {
            return this.unit;
        }

        public void setUnit(String value) {
            this.unit = value;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getValue() {
            return this.value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

    }

    public static class Symbol {

        @Attribute(name = "number", required = false)
        byte number;

        @Attribute(name = "id", required = false)
        String id;

        public byte getNumber() {
            return this.number;
        }

        public void setNumber(byte value) {
            this.number = value;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

    }

    public static class SymbolProbability {

        @Attribute(name = "value", required = false)
        byte value;

        @Attribute(name = "unit", required = false)
        String unit;

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public byte getValue() {
            return value;
        }

        public void setValue(byte value) {
            this.value = value;
        }
    }

    public static class Product {

        @ElementList(name = "time", required = false, entry = "time", inline = true)
        List<Time> time;

        @Attribute(name = "class", required = false)
        String _class;

        public List<Time> getTime() {
            return this.time;
        }

        public void setTime(List<Time> value) {
            this.time = value;
        }

        public String get_class() {
            return this._class;
        }

        public void set_class(String value) {
            this._class = value;
        }

    }

    public static class HighClouds {

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "percent", required = false)
        BigDecimal percent;

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getPercent() {
            return this.percent;
        }

        public void setPercent(BigDecimal value) {
            this.percent = value;
        }

    }

    public static class Pressure {

        @Attribute(name = "unit", required = false)
        String unit;

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "value", required = false)
        BigDecimal value;

        public String getUnit() {
            return this.unit;
        }

        public void setUnit(String value) {
            this.unit = value;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getValue() {
            return this.value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

    }

    public static class Cloudiness {

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "percent", required = false)
        BigDecimal percent;

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getPercent() {
            return this.percent;
        }

        public void setPercent(BigDecimal value) {
            this.percent = value;
        }

    }

    public static class Precipitation {

        @Attribute(name = "unit", required = false)
        String unit;

        @Attribute(name = "value", required = false)
        BigDecimal value;

        public String getUnit() {
            return this.unit;
        }

        public void setUnit(String value) {
            this.unit = value;
        }

        public BigDecimal getValue() {
            return this.value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

    }

    public static class LowClouds {

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "percent", required = false)
        BigDecimal percent;

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getPercent() {
            return this.percent;
        }

        public void setPercent(BigDecimal value) {
            this.percent = value;
        }

    }

    public static class MinTemperature {

        @Attribute(name = "unit", required = false)
        String unit;

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "value", required = false)
        BigDecimal value;

        public String getUnit() {
            return this.unit;
        }

        public void setUnit(String value) {
            this.unit = value;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getValue() {
            return this.value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

    }

    public static class Meta {

        @Element(name = "model", required = false)
        @Convert(MetaConvertor.class)
        List<Model> model;

        public List<Model> getModel() {
            return this.model;
        }

        public void setModel(List<Model> value) {
            this.model = value;
        }

    }

    public static class MetaConvertor implements Converter<List<Model>> {

        @Override
        public List<Model> read(InputNode node) throws Exception {
            List<Model> model = new ArrayList<>();

            InputNode node1 = node;
            NodeMap<InputNode> map = node.getAttributes();

            while (node1 != null) {
                Model mod = new Model();
                mod.name = node1.getAttribute("name").getValue();
                mod.termin = node1.getAttribute("termin").getValue();
                mod.runended = node1.getAttribute("runended").getValue();
                mod.nextrun = node1.getAttribute("nextrun").getValue();
                mod.from = node1.getAttribute("from").getValue();
                mod.to = node1.getAttribute("to").getValue();

                model.add(mod);
                node1 = node.getNext("model");
            }

            return model;
        }

        @Override
        public void write(OutputNode node, List<Model> value) throws Exception {
            for (Model mod : value) {
                OutputNode nod = node.getChild("model");
                nod.setAttribute("name", mod.name);
                nod.setAttribute("termin", mod.termin);
                nod.setAttribute("runended", mod.runended);
                nod.setAttribute("nextrun", mod.nextrun);
                nod.setAttribute("from", mod.from);
                nod.setAttribute("to", mod.to);
            }
        }
    }

    public static class Temperature {

        @Attribute(name = "unit", required = false)
        String unit;

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "value", required = false)
        BigDecimal value;

        public String getUnit() {
            return this.unit;
        }

        public void setUnit(String value) {
            this.unit = value;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getValue() {
            return this.value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

    }

    public static class MediumClouds {

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "percent", required = false)
        BigDecimal percent;

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getPercent() {
            return this.percent;
        }

        public void setPercent(BigDecimal value) {
            this.percent = value;
        }

    }

    public static class Humidity {

        @Attribute(name = "unit", required = false)
        String unit;

        @Attribute(name = "value", required = false)
        BigDecimal value;

        public String getUnit() {
            return this.unit;
        }

        public void setUnit(String value) {
            this.unit = value;
        }

        public BigDecimal getValue() {
            return this.value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

    }

    public static class Model {

        @Attribute(name = "nextrun", required = false)
        String nextrun;

        @Attribute(name = "termin", required = false)
        String termin;

        @Attribute(name = "name", required = false)
        String name;

        @Attribute(name = "runended", required = false)
        String runended;

        @Attribute(name = "from", required = false)
        String from;

        @Attribute(name = "to", required = false)
        String to;

        public String getNextrun() {
            return this.nextrun;
        }

        public void setNextrun(String value) {
            this.nextrun = value;
        }

        public String getTermin() {
            return this.termin;
        }

        public void setTermin(String value) {
            this.termin = value;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String value) {
            this.name = value;
        }

        public String getRunended() {
            return this.runended;
        }

        public void setRunended(String value) {
            this.runended = value;
        }

        public String getFrom() {
            return this.from;
        }

        public void setFrom(String value) {
            this.from = value;
        }

        public String getTo() {
            return this.to;
        }

        public void setTo(String value) {
            this.to = value;
        }

    }

    public static class Location {

        @Element(name = "maxTemperature", required = false)
        MaxTemperature maxTemperature;

        @Attribute(name = "altitude", required = false)
        short altitude;

        @Element(name = "symbol", required = false)
        Symbol symbol;

        @Element(name = "symbolProbability", required = false)
        SymbolProbability symbolProbability;

        @Element(name = "highClouds", required = false)
        HighClouds highClouds;

        @Attribute(name = "latitude", required = false)
        BigDecimal latitude;

        @Element(name = "pressure", required = false)
        Pressure pressure;

        @Element(name = "cloudiness", required = false)
        Cloudiness cloudiness;

        @Element(name = "precipitation", required = false)
        Precipitation precipitation;

        @Element(name = "lowClouds", required = false)
        LowClouds lowClouds;

        @Element(name = "minTemperature", required = false)
        MinTemperature minTemperature;

        @Element(name = "temperature", required = false)
        Temperature temperature;

        @Element(name = "temperatureProbability", required = false)
        TemperatureProbability temperatureProbability;

        @Element(name = "mediumClouds", required = false)
        MediumClouds mediumClouds;

        @Element(name = "humidity", required = false)
        Humidity humidity;

        @Element(name = "dewpointTemperature", required = false)
        DewpointTemperature dewpointTemperature;

        @Element(name = "windDirection", required = false)
        WindDirection windDirection;

        @Element(name = "windSpeed", required = false)
        WindSpeed windSpeed;

        @Element(name = "windGust", required = false)
        WindGust windGust;

        @Element(name = "areaMaxWindSpeed", required = false)
        AreaMaxWindSpeed areaMaxWindSpeed;

        @Element(name = "windProbability", required = false)
        WindProbability windProbability;

        @Attribute(name = "longitude", required = false)
        BigDecimal longitude;

        @Element(name = "fog", required = false)
        Fog fog;

        public MaxTemperature getMaxTemperature() {
            return this.maxTemperature;
        }

        public void setMaxTemperature(MaxTemperature value) {
            this.maxTemperature = value;
        }

        public short getAltitude() {
            return this.altitude;
        }

        public void setAltitude(short value) {
            this.altitude = value;
        }

        public Symbol getSymbol() {
            return this.symbol;
        }

        public void setSymbol(Symbol value) {
            this.symbol = value;
        }

        public HighClouds getHighClouds() {
            return this.highClouds;
        }

        public void setHighClouds(HighClouds value) {
            this.highClouds = value;
        }

        public BigDecimal getLatitude() {
            return this.latitude;
        }

        public void setLatitude(BigDecimal value) {
            this.latitude = value;
        }

        public Pressure getPressure() {
            return this.pressure;
        }

        public void setPressure(Pressure value) {
            this.pressure = value;
        }

        public Cloudiness getCloudiness() {
            return this.cloudiness;
        }

        public void setCloudiness(Cloudiness value) {
            this.cloudiness = value;
        }

        public Precipitation getPrecipitation() {
            return this.precipitation;
        }

        public void setPrecipitation(Precipitation value) {
            this.precipitation = value;
        }

        public LowClouds getLowClouds() {
            return this.lowClouds;
        }

        public void setLowClouds(LowClouds value) {
            this.lowClouds = value;
        }

        public MinTemperature getMinTemperature() {
            return this.minTemperature;
        }

        public void setMinTemperature(MinTemperature value) {
            this.minTemperature = value;
        }

        public Temperature getTemperature() {
            return this.temperature;
        }

        public void setTemperature(Temperature value) {
            this.temperature = value;
        }

        public MediumClouds getMediumClouds() {
            return this.mediumClouds;
        }

        public void setMediumClouds(MediumClouds value) {
            this.mediumClouds = value;
        }

        public Humidity getHumidity() {
            return this.humidity;
        }

        public void setHumidity(Humidity value) {
            this.humidity = value;
        }

        public DewpointTemperature getDewpointTemperature() {
            return this.dewpointTemperature;
        }

        public void setDewpointTemperature(DewpointTemperature value) {
            this.dewpointTemperature = value;
        }

        public WindDirection getWindDirection() {
            return this.windDirection;
        }

        public void setWindDirection(WindDirection value) {
            this.windDirection = value;
        }

        public WindSpeed getWindSpeed() {
            return this.windSpeed;
        }

        public void setWindSpeed(WindSpeed value) {
            this.windSpeed = value;
        }

        public BigDecimal getLongitude() {
            return this.longitude;
        }

        public void setLongitude(BigDecimal value) {
            this.longitude = value;
        }

        public Fog getFog() {
            return this.fog;
        }

        public void setFog(Fog value) {
            this.fog = value;
        }

        public SymbolProbability getSymbolProbability() {
            return symbolProbability;
        }

        public void setSymbolProbability(SymbolProbability symbolProbability) {
            this.symbolProbability = symbolProbability;
        }

        public TemperatureProbability getTemperatureProbability() {
            return temperatureProbability;
        }

        public void setTemperatureProbability(TemperatureProbability temperatureProbability) {
            this.temperatureProbability = temperatureProbability;
        }

        public WindGust getWindGust() {
            return windGust;
        }

        public void setWindGust(WindGust windGust) {
            this.windGust = windGust;
        }

        public AreaMaxWindSpeed getAreaMaxWindSpeed() {
            return areaMaxWindSpeed;
        }

        public void setAreaMaxWindSpeed(AreaMaxWindSpeed areaMaxWindSpeed) {
            this.areaMaxWindSpeed = areaMaxWindSpeed;
        }

        public WindProbability getWindProbability() {
            return windProbability;
        }

        public void setWindProbability(WindProbability windProbability) {
            this.windProbability = windProbability;
        }
    }

    public static class DewpointTemperature {

        @Attribute(name = "unit", required = false)
        String unit;

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "value", required = false)
        BigDecimal value;

        public String getUnit() {
            return this.unit;
        }

        public void setUnit(String value) {
            this.unit = value;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getValue() {
            return this.value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

    }

    public static class Time {

        @Attribute(name = "datatype", required = false)
        String datatype;

        @Attribute(name = "from", required = false)
        String from;

        @Element(name = "location", required = false)
        Location location;

        @Attribute(name = "to", required = false)
        String to;

        public String getDatatype() {
            return this.datatype;
        }

        public void setDatatype(String value) {
            this.datatype = value;
        }

        public String getFrom() {
            return this.from;
        }

        public void setFrom(String value) {
            this.from = value;
        }

        public Location getLocation() {
            return this.location;
        }

        public void setLocation(Location value) {
            this.location = value;
        }

        public String getTo() {
            return this.to;
        }

        public void setTo(String value) {
            this.to = value;
        }

    }

    public static class WindDirection {

        @Attribute(name = "deg", required = false)
        BigDecimal deg;

        @Attribute(name = "name", required = false)
        String name;

        @Attribute(name = "id", required = false)
        String id;

        public BigDecimal getDeg() {
            return this.deg;
        }

        public void setDeg(BigDecimal value) {
            this.deg = value;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String value) {
            this.name = value;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

    }

    public static class WindSpeed {

        @Attribute(name = "mps", required = false)
        BigDecimal mps;

        @Attribute(name = "name", required = false)
        String name;

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "beaufort", required = false)
        byte beaufort;

        public BigDecimal getMps() {
            return this.mps;
        }

        public void setMps(BigDecimal value) {
            this.mps = value;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String value) {
            this.name = value;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public byte getBeaufort() {
            return this.beaufort;
        }

        public void setBeaufort(byte value) {
            this.beaufort = value;
        }

    }

    public static class Fog {

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "percent", required = false)
        BigDecimal percent;

        public String getId() {
            return this.id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public BigDecimal getPercent() {
            return this.percent;
        }

        public void setPercent(BigDecimal value) {
            this.percent = value;
        }

    }

    public static class WindGust {

        @Attribute(name = "id", required = false)
        String id;

        @Attribute(name = "mps", required = false)
        BigDecimal mps;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public BigDecimal getMps() {
            return mps;
        }

        public void setMps(BigDecimal mps) {
            this.mps = mps;
        }
    }

    public static class AreaMaxWindSpeed {

        @Attribute(name = "mps", required = false)
        BigDecimal mps;

        public BigDecimal getMps() {
            return mps;
        }

        public void setMps(BigDecimal mps) {
            this.mps = mps;
        }
    }

    public static class TemperatureProbability {

        @Attribute(name = "value", required = false)
        byte value;

        @Attribute(name = "unit", required = false)
        String unit;

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public byte getValue() {
            return value;
        }

        public void setValue(byte value) {
            this.value = value;
        }
    }

    public static class WindProbability {

        @Attribute(name = "value", required = false)
        byte value;

        @Attribute(name = "unit", required = false)
        String unit;

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public byte getValue() {
            return value;
        }

        public void setValue(byte value) {
            this.value = value;
        }
    }
}