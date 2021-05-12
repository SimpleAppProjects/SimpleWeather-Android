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
import java.util.List;

public class Precipitation extends CustomJsonObject {

    @SerializedName("pop")
    private Integer pop;

    @SerializedName("cloudiness")
    private Integer cloudiness;

    @SerializedName("qpf_rain_in")
    private Float qpfRainIn;

    @SerializedName("qpf_rain_mm")
    private Float qpfRainMm;

    @SerializedName("qpf_snow_in")
    private Float qpfSnowIn;

    @SerializedName("qpf_snow_cm")
    private Float qpfSnowCm;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Precipitation() {
        // Needed for deserialization
    }

    public Precipitation(com.thewizrd.shared_resources.weatherdata.openweather.CurrentRootobject root) {
        // Use cloudiness value here
        cloudiness = root.getClouds().getAll();
        if (root.getRain() != null) {
            if (root.getRain().get_1h() != null) {
                qpfRainIn = ConversionMethods.mmToIn(root.getRain().get_1h());
                qpfRainMm = root.getRain().get_1h();
            } else if (root.getRain().get_3h() != null) {
                qpfRainIn = ConversionMethods.mmToIn(root.getRain().get_3h());
                qpfRainMm = root.getRain().get_3h();
            }
        }
        if (root.getSnow() != null) {
            if (root.getSnow().get_1h() != null) {
                qpfSnowIn = ConversionMethods.mmToIn(root.getSnow().get_1h());
                qpfSnowCm = root.getSnow().get_1h() / 10;
            } else if (root.getSnow().get_3h() != null) {
                qpfSnowIn = ConversionMethods.mmToIn(root.getSnow().get_3h());
                qpfSnowCm = root.getSnow().get_3h() / 10;
            }
        }
    }

    /* OpenWeather OneCall
    public Precipitation(com.thewizrd.shared_resources.weatherdata.openweather.onecall.Current current) {
        // Use cloudiness value here
        cloudiness = current.getClouds();
        if (current.getRain() != null) {
            qpfRainIn = ConversionMethods.mmToIn(current.getRain().get_1h());
            qpfRainMm = current.getRain().get_1h();
        }
        if (current.getSnow() != null) {
            qpfSnowIn = ConversionMethods.mmToIn(current.getSnow().get_1h());
            qpfSnowCm = current.getSnow().get_1h() / 10;
        }
    }
    */

    public Precipitation(com.thewizrd.shared_resources.weatherdata.metno.TimeseriesItem time) {
        // Use cloudiness value here
        cloudiness = Math.round(time.getData().getInstant().getDetails().getCloudAreaFraction());
        // Precipitation
        if (time.getData().getInstant().getDetails() != null && time.getData().getInstant().getDetails().getProbabilityOfPrecipitation() != null) {
            pop = Math.round(time.getData().getInstant().getDetails().getProbabilityOfPrecipitation());
        } else if (time.getData().getNext1Hours() != null && time.getData().getNext1Hours().getDetails() != null && time.getData().getNext1Hours().getDetails().getProbabilityOfPrecipitation() != null) {
            pop = Math.round(time.getData().getNext1Hours().getDetails().getProbabilityOfPrecipitation());
        } else if (time.getData().getNext6Hours() != null && time.getData().getNext6Hours().getDetails() != null && time.getData().getNext6Hours().getDetails().getProbabilityOfPrecipitation() != null) {
            pop = Math.round(time.getData().getNext6Hours().getDetails().getProbabilityOfPrecipitation());
        } else if (time.getData().getNext12Hours() != null && time.getData().getNext12Hours().getDetails() != null && time.getData().getNext12Hours().getDetails().getProbabilityOfPrecipitation() != null) {
            pop = Math.round(time.getData().getNext12Hours().getDetails().getProbabilityOfPrecipitation());
        }
        // The rest DNE
    }

    public Precipitation(com.thewizrd.shared_resources.weatherdata.here.ForecastItem forecast) {
        Integer POP = NumberUtils.tryParseInt(forecast.getPrecipitationProbability());
        if (POP != null) {
            pop = POP;
        }

        Float rain_in = NumberUtils.tryParseFloat(forecast.getRainFall());
        if (rain_in != null) {
            qpfRainIn = rain_in;
            qpfRainMm = ConversionMethods.inToMM(rain_in);
        }

        Float snow_in = NumberUtils.tryParseFloat(forecast.getRainFall());
        if (snow_in != null) {
            qpfSnowIn = snow_in;
            qpfSnowCm = ConversionMethods.inToMM(snow_in) / 10;
        }
    }

    public Precipitation(com.thewizrd.shared_resources.weatherdata.nws.observation.ForecastResponse forecastResponse) {
        // The rest DNE
    }

    public Precipitation(final long dt, List<com.thewizrd.shared_resources.weatherdata.meteofrance.ProbabilityForecastItem> probForecastList) {
        // The rest DNE
    }

    public Integer getPop() {
        return pop;
    }

    public void setPop(Integer pop) {
        this.pop = pop;
    }

    public Integer getCloudiness() {
        return cloudiness;
    }

    public void setCloudiness(Integer cloudiness) {
        this.cloudiness = cloudiness;
    }

    public Float getQpfRainIn() {
        return qpfRainIn;
    }

    public void setQpfRainIn(Float qpfRainIn) {
        this.qpfRainIn = qpfRainIn;
    }

    public Float getQpfRainMm() {
        return qpfRainMm;
    }

    public void setQpfRainMm(Float qpfRainMm) {
        this.qpfRainMm = qpfRainMm;
    }

    public Float getQpfSnowIn() {
        return qpfSnowIn;
    }

    public void setQpfSnowIn(Float qpfSnowIn) {
        this.qpfSnowIn = qpfSnowIn;
    }

    public Float getQpfSnowCm() {
        return qpfSnowCm;
    }

    public void setQpfSnowCm(Float qpfSnowCm) {
        this.qpfSnowCm = qpfSnowCm;
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
                    case "pop":
                        this.pop = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "cloudiness":
                        this.cloudiness = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "qpf_rain_in":
                        this.qpfRainIn = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "qpf_rain_mm":
                        this.qpfRainMm = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "qpf_snow_in":
                        this.qpfSnowIn = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "qpf_snow_cm":
                        this.qpfSnowCm = NumberUtils.tryParseFloat(reader.nextString());
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

            // "pop" : ""
            writer.name("pop");
            writer.value(pop);

            // "cloudiness" : ""
            writer.name("cloudiness");
            writer.value(cloudiness);

            // "qpf_rain_in" : ""
            writer.name("qpf_rain_in");
            writer.value(qpfRainIn);

            // "qpf_rain_mm" : ""
            writer.name("qpf_rain_mm");
            writer.value(qpfRainMm);

            // "qpf_snow_in" : ""
            writer.name("qpf_snow_in");
            writer.value(qpfSnowIn);

            // "qpf_snow_cm" : ""
            writer.name("qpf_snow_cm");
            writer.value(qpfSnowCm);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Precipitation: error writing json string");
        }
    }
}