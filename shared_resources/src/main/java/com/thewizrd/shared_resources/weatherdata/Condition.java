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
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import java.io.IOException;
import java.io.StringReader;

public class Condition extends CustomJsonObject {

    @SerializedName("weather")
    private String weather;

    @SerializedName("temp_f")
    private double tempF;

    @SerializedName("temp_c")
    private double tempC;

    @SerializedName("wind_degrees")
    private int windDegrees;

    @SerializedName("wind_mph")
    private double windMph;

    @SerializedName("wind_kph")
    private double windKph;

    @SerializedName("feelslike_f")
    private double feelslikeF;

    @SerializedName("feelslike_c")
    private double feelslikeC;

    @SerializedName("icon")
    private String icon;

    @SerializedName("beaufort")
    private Beaufort beaufort;

    @SerializedName("uv")
    private UV uv;

    @SerializedName("high_f")
    private double highF;

    @SerializedName("high_c")
    private double highC;

    @SerializedName("low_f")
    private double lowF;

    @SerializedName("low_c")
    private double lowC;

    @SerializedName("airQuality")
    private AirQuality airQuality;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Condition() {
        // Needed for deserialization
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.weatherunderground.CurrentObservation condition) {
        weather = condition.getWeather();
        tempF = condition.getTempF();
        tempC = condition.getTempC();
        windDegrees = condition.getWindDegrees();
        windMph = condition.getWindMph();
        windKph = condition.getWindKph();
        feelslikeF = condition.getFeelslikeF();
        feelslikeC = condition.getFeelslikeC();
        icon = WeatherManager.getProvider(WeatherAPI.WEATHERUNDERGROUND)
                .getWeatherIcon(condition.getIconUrl().replace("http://icons.wxug.com/i/c/k/", "").replace(".gif", ""));
        try {
            uv = new UV(Float.parseFloat(condition.getUV()));
        } catch (NumberFormatException ex) {
            uv = null;
        }
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.weatheryahoo.CurrentObservation observation) {
        weather = observation.getCondition().getText();
        tempF = Float.parseFloat(observation.getCondition().getTemperature());
        tempC = Float.parseFloat(ConversionMethods.FtoC((observation.getCondition().getTemperature())));
        windDegrees = Integer.parseInt(observation.getWind().getDirection());
        windMph = Float.parseFloat(observation.getWind().getSpeed());
        windKph = Float.parseFloat(ConversionMethods.mphTokph(observation.getWind().getSpeed()));
        feelslikeF = Float.parseFloat(observation.getWind().getChill());
        feelslikeC = Float.parseFloat(ConversionMethods.FtoC(observation.getWind().getChill()));
        icon = WeatherManager.getProvider(WeatherAPI.YAHOO)
                .getWeatherIcon(observation.getCondition().getCode());
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.openweather.CurrentRootobject root) {
        weather = StringUtils.toUpperCase(root.getWeather().get(0).getDescription());
        tempF = Float.parseFloat(ConversionMethods.KtoF(Float.toString(root.getMain().getTemp())));
        tempC = Float.parseFloat(ConversionMethods.KtoC(Float.toString(root.getMain().getTemp())));
        highF = Float.parseFloat(ConversionMethods.KtoF(Float.toString(root.getMain().getTempMax())));
        highC = Float.parseFloat(ConversionMethods.KtoC(Float.toString(root.getMain().getTempMax())));
        lowF = Float.parseFloat(ConversionMethods.KtoF(Float.toString(root.getMain().getTempMin())));
        lowC = Float.parseFloat(ConversionMethods.KtoC(Float.toString(root.getMain().getTempMin())));
        windDegrees = (int) root.getWind().getDeg();
        windMph = Float.parseFloat(ConversionMethods.msecToMph(Float.toString(root.getWind().getSpeed())));
        windKph = Float.parseFloat(ConversionMethods.msecToKph(Float.toString(root.getWind().getSpeed())));
        // This will be calculated after with formula
        feelslikeF = tempF;
        feelslikeC = tempC;

        String ico = root.getWeather().get(0).getIcon();
        String dn = Character.toString(ico.charAt(ico.length() == 0 ? 0 : ico.length() - 1));

        try {
            int x = Integer.parseInt(dn);
            dn = "";
        } catch (NumberFormatException ex) {
            // DO nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(root.getWeather().get(0).getId() + dn);
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.metno.Weatherdata.Time time) {
        // weather
        tempF = Float.parseFloat(ConversionMethods.CtoF(time.getLocation().getTemperature().getValue().toString()));
        tempC = time.getLocation().getTemperature().getValue().floatValue();
        windDegrees = Math.round(time.getLocation().getWindDirection().getDeg().floatValue());
        windMph = (float) Math.round(Double.parseDouble(ConversionMethods.msecToMph(time.getLocation().getWindSpeed().getMps().toString())));
        windKph = (float) Math.round(Double.parseDouble(ConversionMethods.msecToKph(time.getLocation().getWindSpeed().getMps().toString())));
        // This will be calculated after with formula
        feelslikeF = tempF;
        feelslikeC = tempC;
        // icon
        beaufort = new Beaufort(time.getLocation().getWindSpeed().getBeaufort());
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.here.ObservationItem observation,
                     com.thewizrd.shared_resources.weatherdata.here.ForecastItem forecastItem) {
        weather = StringUtils.toPascalCase(observation.getDescription());
        try {
            Float tempF = Float.valueOf(observation.getTemperature());
            this.tempF = tempF;
            tempC = Float.parseFloat(ConversionMethods.FtoC(tempF.toString()));
        } catch (NumberFormatException ex) {
            this.tempF = 0.00f;
            tempC = 0.00f;
        }

        try {
            Float highTempF = Float.valueOf(observation.getHighTemperature());
            Float lowTempF = Float.valueOf(observation.getLowTemperature());
            this.highF = highTempF;
            this.highC = Float.parseFloat(ConversionMethods.FtoC(highTempF.toString()));
            this.lowF = lowTempF;
            this.lowC = Float.parseFloat(ConversionMethods.FtoC(lowTempF.toString()));
        } catch (NumberFormatException ignored) {
            this.highF = 0.00f;
            this.highC = 0.00f;
            this.lowF = 0.00f;
            this.lowC = 0.00f;
        }

        if (highF == 0 && highF == highC && lowF == 0 && lowF == lowC) {
            try {
                Float highTempF = Float.valueOf(forecastItem.getHighTemperature());
                Float lowTempF = Float.valueOf(forecastItem.getLowTemperature());
                this.highF = highTempF;
                this.highC = Float.parseFloat(ConversionMethods.FtoC(highTempF.toString()));
                this.lowF = lowTempF;
                this.lowC = Float.parseFloat(ConversionMethods.FtoC(lowTempF.toString()));
            } catch (NumberFormatException ignored) {
                this.highF = 0.00f;
                this.highC = 0.00f;
                this.lowF = 0.00f;
                this.lowC = 0.00f;
            }
        }

        try {
            this.windDegrees = Integer.parseInt(observation.getWindDirection());
        } catch (NumberFormatException ex) {
            this.windDegrees = 0;
        }

        try {
            windMph = Float.parseFloat(observation.getWindSpeed());
            windKph = Float.parseFloat(ConversionMethods.mphTokph(observation.getWindSpeed()));
        } catch (NumberFormatException ex) {
            windMph = 0.00f;
            windKph = 0.00f;
        }

        try {
            Float comfortTempF = Float.valueOf(observation.getComfort());
            feelslikeF = comfortTempF;
            feelslikeC = Float.parseFloat(ConversionMethods.FtoC(comfortTempF.toString()));
        } catch (NumberFormatException ex) {
            feelslikeF = 0.00f;
            feelslikeC = 0.00f;
        }

        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", observation.getDaylight(), observation.getIconName()));

        try {
            int scale = Integer.parseInt(forecastItem.getBeaufortScale());
            beaufort = new Beaufort(scale, forecastItem.getBeaufortDescription());
        } catch (NumberFormatException ex) {
            beaufort = null;
        }

        try {
            float index = Float.parseFloat(forecastItem.getUvIndex());
            uv = new UV(index, forecastItem.getUvDesc());
        } catch (NumberFormatException ex) {
            uv = null;
        }
    }

    public Condition(com.thewizrd.shared_resources.weatherdata.nws.ObservationCurrentResponse obsCurrentResponse) {
        weather = obsCurrentResponse.getTextDescription();
        try {
            tempC = obsCurrentResponse.getTemperature().getValue();
            tempF = Double.parseDouble(ConversionMethods.CtoF(Double.toString(tempC)));
        } catch (NullPointerException | NumberFormatException ex) {
            tempF = 0.00f;
            tempC = 0.00f;
        }
        try {
            windDegrees = obsCurrentResponse.getWindDirection().getValue().intValue();
        } catch (NullPointerException | NumberFormatException ex) {
            windDegrees = 0;
        }
        try {
            windMph = Double.parseDouble(ConversionMethods.msecToMph(Float.toString(obsCurrentResponse.getWindSpeed().getValue())));
            windKph = Double.parseDouble(ConversionMethods.msecToKph(Float.toString(obsCurrentResponse.getWindSpeed().getValue())));
        } catch (NullPointerException | NumberFormatException ex) {
            windMph = -1.0f;
            windKph = -1.0f;
        }
        float humidity;
        try {
            humidity = obsCurrentResponse.getRelativeHumidity().getValue();
        } catch (NullPointerException | NumberFormatException ex) {
            humidity = -1.0f;
        }
        if (tempF != tempC) {
            feelslikeF = Double.parseDouble(WeatherUtils.getFeelsLikeTemp(Double.toString(tempF),
                    Double.toString(windMph), Float.toString(humidity)));
            feelslikeC = Double.parseDouble(ConversionMethods.FtoC(Double.toString(feelslikeF)));
        } else {
            feelslikeF = -1.0f;
            feelslikeC = -1.0f;
        }
        icon = WeatherManager.getProvider(WeatherAPI.NWS)
                .getWeatherIcon(obsCurrentResponse.getIcon());
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public double getTempF() {
        return tempF;
    }

    public void setTempF(double tempF) {
        this.tempF = tempF;
    }

    public double getTempC() {
        return tempC;
    }

    public void setTempC(double tempC) {
        this.tempC = tempC;
    }

    public int getWindDegrees() {
        return windDegrees;
    }

    public void setWindDegrees(int windDegrees) {
        this.windDegrees = windDegrees;
    }

    public double getWindMph() {
        return windMph;
    }

    public void setWindMph(double windMph) {
        this.windMph = windMph;
    }

    public double getWindKph() {
        return windKph;
    }

    public void setWindKph(double windKph) {
        this.windKph = windKph;
    }

    public double getFeelslikeF() {
        return feelslikeF;
    }

    public void setFeelslikeF(double feelslikeF) {
        this.feelslikeF = feelslikeF;
    }

    public double getFeelslikeC() {
        return feelslikeC;
    }

    public void setFeelslikeC(double feelslikeC) {
        this.feelslikeC = feelslikeC;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Beaufort getBeaufort() {
        return beaufort;
    }

    public void setBeaufort(Beaufort beaufort) {
        this.beaufort = beaufort;
    }

    public UV getUv() {
        return uv;
    }

    public void setUv(UV uv) {
        this.uv = uv;
    }

    public double getHighF() {
        return highF;
    }

    public void setHighF(double highF) {
        this.highF = highF;
    }

    public double getHighC() {
        return highC;
    }

    public void setHighC(double highC) {
        this.highC = highC;
    }

    public double getLowF() {
        return lowF;
    }

    public void setLowF(double lowF) {
        this.lowF = lowF;
    }

    public double getLowC() {
        return lowC;
    }

    public void setLowC(double lowC) {
        this.lowC = lowC;
    }

    public AirQuality getAirQuality() {
        return airQuality;
    }

    public void setAirQuality(AirQuality airQuality) {
        this.airQuality = airQuality;
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
                    case "weather":
                        this.weather = reader.nextString();
                        break;
                    case "temp_f":
                        this.tempF = Float.parseFloat(reader.nextString());
                        break;
                    case "temp_c":
                        this.tempC = Float.parseFloat(reader.nextString());
                        break;
                    case "wind_degrees":
                        this.windDegrees = Integer.parseInt(reader.nextString());
                        break;
                    case "wind_mph":
                        this.windMph = Float.parseFloat(reader.nextString());
                        break;
                    case "wind_kph":
                        this.windKph = Float.parseFloat(reader.nextString());
                        break;
                    case "feelslike_f":
                        this.feelslikeF = Float.parseFloat(reader.nextString());
                        break;
                    case "feelslike_c":
                        this.feelslikeC = Float.parseFloat(reader.nextString());
                        break;
                    case "icon":
                        this.icon = reader.nextString();
                        break;
                    case "beaufort":
                        this.beaufort = new Beaufort();
                        this.beaufort.fromJson(reader);
                        break;
                    case "uv":
                        this.uv = new UV();
                        this.uv.fromJson(reader);
                        break;
                    case "high_f":
                        this.highF = Float.parseFloat(reader.nextString());
                        break;
                    case "high_c":
                        this.highC = Float.parseFloat(reader.nextString());
                        break;
                    case "low_f":
                        this.lowF = Float.parseFloat(reader.nextString());
                        break;
                    case "low_c":
                        this.lowC = Float.parseFloat(reader.nextString());
                        break;
                    case "airQuality":
                        this.airQuality = new AirQuality();
                        this.airQuality.fromJson(reader);
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

            // "weather" : ""
            writer.name("weather");
            writer.value(weather);

            // "temp_f" : ""
            writer.name("temp_f");
            writer.value(tempF);

            // "temp_c" : ""
            writer.name("temp_c");
            writer.value(tempC);

            // "wind_degrees" : ""
            writer.name("wind_degrees");
            writer.value(windDegrees);

            // "wind_mph" : ""
            writer.name("wind_mph");
            writer.value(windMph);

            // "wind_kph" : ""
            writer.name("wind_kph");
            writer.value(windKph);

            // "feelslike_f" : ""
            writer.name("feelslike_f");
            writer.value(feelslikeF);

            // "feelslike_c" : ""
            writer.name("feelslike_c");
            writer.value(feelslikeC);

            // "icon" : ""
            writer.name("icon");
            writer.value(icon);

            // "beaufort" : ""
            if (beaufort != null) {
                writer.name("beaufort");
                if (beaufort == null)
                    writer.nullValue();
                else
                    beaufort.toJson(writer);
            }

            // "uv" : ""
            if (uv != null) {
                writer.name("uv");
                if (uv == null)
                    writer.nullValue();
                else
                    uv.toJson(writer);
            }

            // "high_f" : ""
            writer.name("high_f");
            writer.value(highF);

            // "high_c" : ""
            writer.name("high_c");
            writer.value(highC);

            // "low_f" : ""
            writer.name("low_f");
            writer.value(lowF);

            // "low_c" : ""
            writer.name("low_c");
            writer.value(lowC);

            // "airQuality" : ""
            if (airQuality != null) {
                writer.name("airQuality");
                if (airQuality == null)
                    writer.nullValue();
                else
                    airQuality.toJson(writer);
            }

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Condition: error writing json string");
        }
    }
}