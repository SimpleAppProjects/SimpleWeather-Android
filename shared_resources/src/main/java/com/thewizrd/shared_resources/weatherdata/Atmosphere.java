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

import java.io.IOException;
import java.io.StringReader;

public class Atmosphere extends CustomJsonObject {

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

    @SerializedName("dewpoint_f")
    private String dewpointF;

    @SerializedName("dewpoint_c")
    private String dewpointC;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Atmosphere() {
        // Needed for deserialization
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.weatheryahoo.Atmosphere atmosphere) {
        humidity = atmosphere.getHumidity();
        pressureIn = atmosphere.getPressure();
        pressureMb = ConversionMethods.inHgToMB(atmosphere.getPressure());
        pressureTrend = atmosphere.getRising();
        visibilityMi = atmosphere.getVisibility();
        visibilityKm = ConversionMethods.miToKm(atmosphere.getVisibility());
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

        try {
            float visMi = 10.0f;
            visibilityMi = Float.toString(visMi - (visMi * time.getLocation().getFog().getPercent().floatValue() / 100));
            visibilityKm = ConversionMethods.miToKm(visibilityMi);
        } catch (NumberFormatException ex) {
            visibilityMi = Weather.NA;
            visibilityKm = Weather.NA;
        }

        try {
            dewpointF = ConversionMethods.CtoF(time.getLocation().getDewpointTemperature().getValue().toString());
            dewpointC = Float.toString(time.getLocation().getDewpointTemperature().getValue().floatValue());
        } catch (NumberFormatException ex) {
            dewpointF = null;
            dewpointC = null;
        }
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.here.ObservationItem observation) {
        humidity = observation.getHumidity();
        try {
            pressureIn = observation.getBarometerPressure();
            pressureMb = ConversionMethods.inHgToMB(observation.getBarometerPressure());
        } catch (NumberFormatException ex) {
            pressureIn = null;
            pressureMb = null;
        }
        pressureTrend = observation.getBarometerTrend();

        try {
            visibilityMi = observation.getVisibility();
            visibilityKm = ConversionMethods.miToKm(observation.getVisibility());
        } catch (NumberFormatException ex) {
            visibilityMi = null;
            visibilityKm = null;
        }

        try {
            dewpointF = observation.getDewPoint();
            dewpointC = ConversionMethods.FtoC(observation.getDewPoint());
        } catch (NumberFormatException ex) {
            dewpointF = null;
            dewpointC = null;
        }
    }

    public Atmosphere(com.thewizrd.shared_resources.weatherdata.nws.ObservationCurrentResponse obsCurrentResponse) {
        try {
            humidity = Integer.toString(Math.round(obsCurrentResponse.getRelativeHumidity().getValue()));
        } catch (NullPointerException | NumberFormatException ex) {
            humidity = null;
        }
        try {
            pressureIn = ConversionMethods.paToInHg(
                    Float.toString(obsCurrentResponse.getBarometricPressure().getValue()));
            pressureMb = ConversionMethods.paToMB(
                    Float.toString(obsCurrentResponse.getBarometricPressure().getValue()));
        } catch (NullPointerException | NumberFormatException ex) {
            pressureIn = null;
            pressureMb = null;
        }
        pressureTrend = "";

        try {
            visibilityKm = Float.toString(obsCurrentResponse.getVisibility().getValue() / 1000);
            visibilityMi = ConversionMethods.kmToMi(
                    Float.toString(obsCurrentResponse.getVisibility().getValue() / 1000));
        } catch (NullPointerException | NumberFormatException ex) {
            visibilityKm = null;
            visibilityMi = null;
        }

        try {
            dewpointC = Float.toString(obsCurrentResponse.getDewpoint().getValue());
            dewpointF = ConversionMethods.CtoF(
                    Float.toString(obsCurrentResponse.getDewpoint().getValue()));
        } catch (NullPointerException | NumberFormatException ex) {
            dewpointC = null;
            dewpointF = null;
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

    public String getDewpointF() {
        return dewpointF;
    }

    public void setDewpointF(String dewpointF) {
        this.dewpointF = dewpointF;
    }

    public String getDewpointC() {
        return dewpointC;
    }

    public void setDewpointC(String dewpointC) {
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
                        this.humidity = reader.nextString();
                        break;
                    case "pressure_mb":
                        this.pressureMb = reader.nextString();
                        break;
                    case "pressure_in":
                        this.pressureIn = reader.nextString();
                        break;
                    case "pressure_trend":
                        this.pressureTrend = reader.nextString();
                        break;
                    case "visibility_mi":
                        this.visibilityMi = reader.nextString();
                        break;
                    case "visibility_km":
                        this.visibilityKm = reader.nextString();
                        break;
                    case "dewpoint_f":
                        this.dewpointF = reader.nextString();
                        break;
                    case "dewpoint_c":
                        this.dewpointC = reader.nextString();
                        break;
                    default:
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