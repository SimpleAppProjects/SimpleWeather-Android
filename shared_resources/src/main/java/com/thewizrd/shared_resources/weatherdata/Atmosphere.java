package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class Atmosphere {

    @SerializedName("humidity")
    private String humidity;

    @SerializedName("pressure_mb")
    private String pressureMb;

    @SerializedName("pressure_in")
    private String pressureIn;

    @SerializedName("pressure_trend")
    private String pressureTrend;

    @SerializedName("visibility_mi")
    private String visibilityMi;

    @SerializedName("visibility_km")
    private String visibilityKm;

    private Atmosphere() {
        // Needed for deserialization
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.weatherunderground.CurrentObservation condition) {
        humidity = condition.getRelativeHumidity();
        pressureMb = condition.getPressureMb();
        pressureIn = condition.getPressureIn();
        pressureTrend = condition.getPressureTrend();
        visibilityMi = condition.getVisibilityMi();
        visibilityKm = condition.getVisibilityKm();
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.weatheryahoo.Atmosphere atmosphere) {
        humidity = atmosphere.getHumidity();
        pressureMb = atmosphere.getPressure();
        pressureIn = ConversionMethods.mbToInHg(pressureMb);
        pressureTrend = atmosphere.getRising();
        visibilityKm = atmosphere.getVisibility();
        visibilityMi = ConversionMethods.kmToMi(visibilityKm);
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.openweather.CurrentRootobject root) {
        humidity = root.getMain().getHumidity();
        pressureMb = Float.toString(root.getMain().getPressure());
        pressureIn = ConversionMethods.mbToInHg(Float.toString(root.getMain().getPressure()));
        pressureTrend = "";
        visibilityMi = ConversionMethods.kmToMi(Integer.toString((root.getVisibility() / 1000)));
        visibilityKm = Integer.toString((root.getVisibility() / 1000));
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.metno.Weatherdata.Time time) {
        humidity = Integer.toString(Math.round(time.getLocation().getHumidity().getValue().floatValue()));
        pressureMb = time.getLocation().getPressure().getValue().toString();
        pressureIn = ConversionMethods.mbToInHg(time.getLocation().getPressure().getValue().toString());
        pressureTrend = "";
        visibilityMi = Weather.NA;
        visibilityKm = Weather.NA;
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.here.ObservationItem observation) {
        humidity = observation.getHumidity();
        pressureMb = ConversionMethods.inHgToMB(observation.getBarometerPressure());
        pressureIn = observation.getBarometerPressure();
        pressureTrend = observation.getBarometerTrend();
        visibilityMi = observation.getVisibility();

        try {
            Float visible_mi = Float.valueOf(observation.getVisibility());
            visibilityKm = ConversionMethods.miToKm(visible_mi.toString());
        } catch (NumberFormatException ex) {
            visibilityKm = observation.getVisibility();
        }
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getPressureMb() {
        return pressureMb;
    }

    public void setPressureMb(String pressureMb) {
        this.pressureMb = pressureMb;
    }

    public String getPressureIn() {
        return pressureIn;
    }

    public void setPressureIn(String pressureIn) {
        this.pressureIn = pressureIn;
    }

    public String getPressureTrend() {
        return pressureTrend;
    }

    public void setPressureTrend(String pressureTrend) {
        this.pressureTrend = pressureTrend;
    }

    public String getVisibilityMi() {
        return visibilityMi;
    }

    public void setVisibilityMi(String visibilityMi) {
        this.visibilityMi = visibilityMi;
    }

    public String getVisibilityKm() {
        return visibilityKm;
    }

    public void setVisibilityKm(String visibilityKm) {
        this.visibilityKm = visibilityKm;
    }

    public static Atmosphere fromJson(JsonReader extReader) {
        Atmosphere obj = null;

        try {
            obj = new Atmosphere();
            JsonReader reader;
            String jsonValue;

            if (extReader.peek() == JsonToken.STRING) {
                jsonValue = extReader.nextString();
            } else {
                jsonValue = null;
            }

            if (jsonValue == null)
                reader = extReader;
            else {
                reader = new JsonReader(new StringReader(jsonValue));
                reader.beginObject(); // StartObject
            }

            while (reader.hasNext() && reader.peek() != JsonToken.END_OBJECT) {
                if (reader.peek() == JsonToken.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    continue;
                }

                switch (property) {
                    case "humidity":
                        obj.humidity = reader.nextString();
                        break;
                    case "pressure_mb":
                        obj.pressureMb = reader.nextString();
                        break;
                    case "pressure_in":
                        obj.pressureIn = reader.nextString();
                        break;
                    case "pressure_trend":
                        obj.pressureTrend = reader.nextString();
                        break;
                    case "visibility_mi":
                        obj.visibilityMi = reader.nextString();
                        break;
                    case "visibility_km":
                        obj.visibilityKm = reader.nextString();
                        break;
                    default:
                        break;
                }
            }

            if (reader.peek() == JsonToken.END_OBJECT)
                reader.endObject();

        } catch (Exception ex) {
            obj = null;
        }

        return obj;
    }

    public String toJson() {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);

        try {
            // {
            writer.beginObject();

            // "humidity" : ""
            writer.name("humidity");
            writer.value(humidity);

            // "pressure_mb" : ""
            writer.name("pressure_mb");
            writer.value(pressureMb);

            // "pressure_in" : ""
            writer.name("pressure_in");
            writer.value(pressureIn);

            // "pressure_trend" : ""
            writer.name("pressure_trend");
            writer.value(pressureTrend);

            // "visibility_mi" : ""
            writer.name("visibility_mi");
            writer.value(visibilityMi);

            // "visibility_km" : ""
            writer.name("visibility_km");
            writer.value(visibilityKm);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Atmosphere: error writing json string");
        }

        return sw.toString();
    }
}