package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;

import java.io.IOException;
import java.io.StringReader;

public class Atmosphere extends CustomJsonObject {

    @SerializedName("humidity")
    private Integer humidity;

    @SerializedName("pressure_mb")
    private Float pressureMb;

    @SerializedName("pressure_in")
    private Float pressureIn;

    @SerializedName("pressure_trend")
    private String pressureTrend;

    @SerializedName("visibility_mi")
    private Float visibilityMi;

    @SerializedName("visibility_km")
    private Float visibilityKm;

    @SerializedName("dewpoint_f")
    private Float dewpointF;

    @SerializedName("dewpoint_c")
    private Float dewpointC;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Atmosphere() {
        // Needed for deserialization
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.weatheryahoo.Atmosphere atmosphere) {
        humidity = atmosphere.getHumidity();
        pressureIn = atmosphere.getPressure();
        pressureMb = ConversionMethods.inHgToMB(pressureIn);
        pressureTrend = Integer.toString(atmosphere.getRising());
        visibilityMi = atmosphere.getVisibility();
        visibilityKm = ConversionMethods.miToKm(visibilityMi);
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.openweather.CurrentRootobject root) {
        humidity = root.getMain().getHumidity();
        // 1hPa = 1mbar
        pressureMb = root.getMain().getPressure();
        pressureIn = ConversionMethods.mbToInHg(root.getMain().getPressure());
        pressureTrend = "";
        visibilityKm = root.getVisibility() / 1000f;
        visibilityMi = ConversionMethods.kmToMi(visibilityKm);
    }

    /* OpenWeather OneCall
    public Atmosphere(com.thewizrd.shared_resources.weatherdata.openweather.onecall.Current current) {
        humidity = current.getHumidity();
        // 1hPa = 1mbar
        pressureMb = current.getPressure();
        pressureIn = ConversionMethods.mbToInHg(pressureMb);
        pressureTrend = "";
        visibilityKm = current.getVisibility() / 1000f;
        visibilityMi = ConversionMethods.kmToMi(visibilityKm);
        dewpointF = ConversionMethods.KtoF(current.getDewPoint());
        dewpointC = ConversionMethods.KtoC(current.getDewPoint());
    }
    */

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.metno.TimeseriesItem time) {
        humidity = Math.round(time.getData().getInstant().getDetails().getRelativeHumidity());
        pressureMb = time.getData().getInstant().getDetails().getAirPressureAtSeaLevel();
        pressureIn = ConversionMethods.mbToInHg(time.getData().getInstant().getDetails().getAirPressureAtSeaLevel());
        pressureTrend = "";

        if (time.getData().getInstant().getDetails().getFogAreaFraction() != null) {
            float visMi = 10.0f;
            visibilityMi = (visMi - (visMi * time.getData().getInstant().getDetails().getFogAreaFraction() / 100));
            visibilityKm = ConversionMethods.miToKm(visibilityMi);
        }

        if (time.getData().getInstant().getDetails().getDewPointTemperature() != null) {
            dewpointF = ConversionMethods.CtoF(time.getData().getInstant().getDetails().getDewPointTemperature());
            dewpointC = time.getData().getInstant().getDetails().getDewPointTemperature();
        }
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.here.ObservationItem observation) {
        Integer Humidity = NumberUtils.tryParseInt(observation.getHumidity());
        if (Humidity != null) {
            humidity = Humidity;
        }

        Float pressureIN = NumberUtils.tryParseFloat(observation.getBarometerPressure());
        if (pressureIN != null) {
            pressureIn = pressureIN;
            pressureMb = ConversionMethods.inHgToMB(pressureIN);
        }
        pressureTrend = observation.getBarometerTrend();

        Float visibilityMI = NumberUtils.tryParseFloat(observation.getVisibility());
        if (visibilityMI != null) {
            visibilityMi = visibilityMI;
            visibilityKm = ConversionMethods.miToKm(visibilityMI);
        }

        Float dewpoint_f = NumberUtils.tryParseFloat(observation.getDewPoint());
        if (dewpoint_f != null) {
            dewpointF = dewpoint_f;
            dewpointC = ConversionMethods.FtoC(dewpoint_f);
        }
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.nws.ObservationCurrentResponse obsCurrentResponse) {
        if (obsCurrentResponse.getRelativeHumidity().getValue() != null) {
            humidity = Math.round(obsCurrentResponse.getRelativeHumidity().getValue());
        }
        if (obsCurrentResponse.getBarometricPressure().getValue() != null) {
            float pressure_pa = obsCurrentResponse.getBarometricPressure().getValue();
            pressureIn = ConversionMethods.paToInHg(pressure_pa);
            pressureMb = ConversionMethods.paToMB(pressure_pa);
        }
        pressureTrend = "";

        if (obsCurrentResponse.getVisibility().getValue() != null) {
            visibilityKm = obsCurrentResponse.getVisibility().getValue() / 1000;
            visibilityMi = ConversionMethods.kmToMi(obsCurrentResponse.getVisibility().getValue() / 1000);
        }

        if (obsCurrentResponse.getDewpoint().getValue() != null) {
            dewpointC = obsCurrentResponse.getDewpoint().getValue();
            dewpointF = ConversionMethods.CtoF(obsCurrentResponse.getDewpoint().getValue());
        }
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Float getPressureMb() {
        return pressureMb;
    }

    public void setPressureMb(Float pressureMb) {
        this.pressureMb = pressureMb;
    }

    public Float getPressureIn() {
        return pressureIn;
    }

    public void setPressureIn(Float pressureIn) {
        this.pressureIn = pressureIn;
    }

    public String getPressureTrend() {
        return pressureTrend;
    }

    public void setPressureTrend(String pressureTrend) {
        this.pressureTrend = pressureTrend;
    }

    public Float getVisibilityMi() {
        return visibilityMi;
    }

    public void setVisibilityMi(Float visibilityMi) {
        this.visibilityMi = visibilityMi;
    }

    public Float getVisibilityKm() {
        return visibilityKm;
    }

    public void setVisibilityKm(Float visibilityKm) {
        this.visibilityKm = visibilityKm;
    }

    public Float getDewpointF() {
        return dewpointF;
    }

    public void setDewpointF(Float dewpointF) {
        this.dewpointF = dewpointF;
    }

    public Float getDewpointC() {
        return dewpointC;
    }

    public void setDewpointC(Float dewpointC) {
        this.dewpointC = dewpointC;
    }

    @Override
    public void fromJson(JsonReader extReader) {
        try {
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
                        this.humidity = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "pressure_mb":
                        this.pressureMb = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "pressure_in":
                        this.pressureIn = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "pressure_trend":
                        this.pressureTrend = reader.nextString();
                        break;
                    case "visibility_mi":
                        this.visibilityMi = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "visibility_km":
                        this.visibilityKm = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "dewpoint_f":
                        this.dewpointF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "dewpoint_c":
                        this.dewpointC = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }

            if (reader.peek() == JsonToken.END_OBJECT)
                reader.endObject();

        } catch (Exception ignored) {
        }
    }

    @Override
    public void toJson(JsonWriter writer) {
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

            // "dewpoint_f" : ""
            writer.name("dewpoint_f");
            writer.value(dewpointF);

            // "dewpoint_c" : ""
            writer.name("dewpoint_c");
            writer.value(dewpointC);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Atmosphere: error writing json string");
        }
    }
}