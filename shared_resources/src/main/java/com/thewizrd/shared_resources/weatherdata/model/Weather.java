package com.thewizrd.shared_resources.weatherdata.model;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Entity(tableName = "weatherdata")
public class Weather extends CustomJsonObject {
    @Ignore
    public static final transient String NA = "N/A";

    @ColumnInfo(name = "locationblob")
    private Location location;
    @ColumnInfo(name = "update_time")
    private ZonedDateTime updateTime;
    @Ignore
    private List<Forecast> forecast;
    @Ignore
    private List<HourlyForecast> hrForecast;
    @Ignore
    private List<TextForecast> txtForecast;
    @Ignore
    private List<MinutelyForecast> minForecast;
    @Ignore
    private List<AirQuality> aqiForecast;
    @ColumnInfo(name = "conditionblob")
    private Condition condition;
    @ColumnInfo(name = "atmosphereblob")
    private Atmosphere atmosphere;
    @ColumnInfo(name = "astronomyblob")
    private Astronomy astronomy;
    @ColumnInfo(name = "precipitationblob")
    private Precipitation precipitation;
    @Ignore
    // Just for passing along to where its needed
    private Collection<WeatherAlert> weather_alerts;
    private int ttl;
    private String source;
    @PrimaryKey
    @NonNull
    private String query;
    private String locale;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    public Weather() {
        // Needed for deserialization
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(ZonedDateTime value) {
        updateTime = value;
    }

    public List<Forecast> getForecast() {
        return forecast;
    }

    public void setForecast(List<Forecast> forecast) {
        this.forecast = forecast;
    }

    public List<HourlyForecast> getHrForecast() {
        return hrForecast;
    }

    public void setHrForecast(List<HourlyForecast> hr_forecast) {
        this.hrForecast = hr_forecast;
    }

    public List<TextForecast> getTxtForecast() {
        return txtForecast;
    }

    public void setTxtForecast(List<TextForecast> txt_forecast) {
        this.txtForecast = txt_forecast;
    }

    public List<MinutelyForecast> getMinForecast() {
        return minForecast;
    }

    public void setMinForecast(List<MinutelyForecast> minForecast) {
        this.minForecast = minForecast;
    }

    public List<AirQuality> getAqiForecast() {
        return aqiForecast;
    }

    public void setAqiForecast(List<AirQuality> aqiForecast) {
        this.aqiForecast = aqiForecast;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(Atmosphere atmosphere) {
        this.atmosphere = atmosphere;
    }

    public Astronomy getAstronomy() {
        return astronomy;
    }

    public void setAstronomy(Astronomy astronomy) {
        this.astronomy = astronomy;
    }

    public Precipitation getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(Precipitation precipitation) {
        this.precipitation = precipitation;
    }

    public Collection<WeatherAlert> getWeatherAlerts() {
        return weather_alerts;
    }

    public void setWeatherAlerts(Collection<WeatherAlert> weather_alerts) {
        this.weather_alerts = weather_alerts;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    public void setQuery(@NonNull String query) {
        this.query = query;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public void fromJson(@NonNull JsonReader reader) {
        try {
            while (reader.hasNext() && reader.peek() != JsonReader.Token.END_OBJECT) {
                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonReader.Token.NULL) {
                    reader.nextNull();
                    continue;
                }

                switch (property) {
                    case "location":
                        this.location = new Location();
                        this.location.fromJson(reader);
                        break;
                    case "update_time":
                        String json = reader.nextString();
                        ZonedDateTime result = null;
                        try {
                            result = ZonedDateTime.parse(json, DateTimeUtils.getZonedDateTimeFormatter());
                            if (this.getLocation().getTzOffset() != null && result.getOffset().getTotalSeconds() == 0) {
                                result = ZonedDateTime.ofInstant(result.toInstant(), this.getLocation().getTzOffset());
                            }
                        } catch (Exception e) {
                            // If we can't parse as DateTimeOffset try DateTime (data could be old)
                            result = ZonedDateTime.parse(json);
                        }

                        this.setUpdateTime(result);
                        break;
                    case "forecast":
                        // Set initial cap to 10
                        // Most provider forecasts are <= 10
                        List<Forecast> forecasts = new ArrayList<>(10);

                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonReader.Token.END_ARRAY) {
                            if (reader.peek() == JsonReader.Token.STRING || reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                Forecast fcast = new Forecast();
                                fcast.fromJson(reader);
                                forecasts.add(fcast);
                            }
                        }
                        this.forecast = forecasts;

                        if (reader.peek() == JsonReader.Token.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "hr_forecast":
                        // Set initial cap to 90
                        // MetNo contains ~90 items, but HERE contains ~165
                        // If 90+ is needed, let the List impl allocate more
                        List<HourlyForecast> hr_forecasts = new ArrayList<>(90);
                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonReader.Token.END_ARRAY) {
                            if (reader.peek() == JsonReader.Token.STRING || reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                HourlyForecast hr_fcast = new HourlyForecast();
                                hr_fcast.fromJson(reader);
                                hr_forecasts.add(hr_fcast);
                            }
                        }
                        this.hrForecast = hr_forecasts;

                        if (reader.peek() == JsonReader.Token.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "txt_forecast":
                        // Set initial cap to 20
                        // Most provider forecasts are <= 10 (x2 for day & nt)
                        List<TextForecast> txt_forecasts = new ArrayList<>(20);
                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonReader.Token.END_ARRAY) {
                            if (reader.peek() == JsonReader.Token.STRING || reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                TextForecast txtFcast = new TextForecast();
                                txtFcast.fromJson(reader);
                                txt_forecasts.add(txtFcast);
                            }
                        }
                        this.txtForecast = txt_forecasts;

                        if (reader.peek() == JsonReader.Token.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "minForecast":
                        // Set initial cap to 60
                        // Minutely forecasts are usually only for an hour
                        List<MinutelyForecast> minForecasts = new ArrayList<>(60);

                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonReader.Token.END_ARRAY) {
                            if (reader.peek() == JsonReader.Token.STRING || reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                MinutelyForecast fcast = new MinutelyForecast();
                                fcast.fromJson(reader);
                                minForecasts.add(fcast);
                            }
                        }
                        this.minForecast = minForecasts;

                        if (reader.peek() == JsonReader.Token.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "aqiForecast":
                        // Set initial cap to 60
                        // Minutely forecasts are usually only for an hour
                        List<AirQuality> aqiForecasts = new ArrayList<>(10);

                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonReader.Token.END_ARRAY) {
                            if (reader.peek() == JsonReader.Token.STRING || reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                AirQuality fcast = new AirQuality();
                                fcast.fromJson(reader);
                                aqiForecasts.add(fcast);
                            }
                        }
                        this.aqiForecast = aqiForecasts;

                        if (reader.peek() == JsonReader.Token.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "condition":
                        this.condition = new Condition();
                        this.condition.fromJson(reader);
                        break;
                    case "atmosphere":
                        this.atmosphere = new Atmosphere();
                        this.atmosphere.fromJson(reader);
                        break;
                    case "astronomy":
                        this.astronomy = new Astronomy();
                        this.astronomy.fromJson(reader);
                        break;
                    case "precipitation":
                        this.precipitation = new Precipitation();
                        this.precipitation.fromJson(reader);
                        break;
                    case "weather_alerts":
                        List<WeatherAlert> alerts = new ArrayList<>();
                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonReader.Token.END_ARRAY) {
                            if (reader.peek() == JsonReader.Token.STRING || reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                @SuppressLint({"RestrictedApi", "VisibleForTests"})
                                WeatherAlert alert = new WeatherAlert();
                                alert.fromJson(reader);
                                alerts.add(alert);
                            }
                        }
                        this.weather_alerts = alerts;

                        if (reader.peek() == JsonReader.Token.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "ttl":
                        this.ttl = NumberUtils.tryParseInt(reader.nextString(), 120);
                        break;
                    case "source":
                        this.source = reader.nextString();
                        break;
                    case "query":
                        this.query = reader.nextString();
                        break;
                    case "locale":
                        this.locale = reader.nextString();
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }

            if (reader.peek() == JsonReader.Token.END_OBJECT)
                reader.endObject();

        } catch (Exception ignored) {
        }
    }

    @Override
    public void toJson(@NonNull JsonWriter writer) {
        try {
            // {
            writer.beginObject();

            // "location" : ""
            writer.name("location");
            if (location == null)
                writer.nullValue();
            else
                location.toJson(writer);

            // "update_time" : ""
            writer.name("update_time");
            writer.value(updateTime.format(DateTimeUtils.getZonedDateTimeFormatter()));

            // "forecast" : ""
            if (forecast != null) {
                writer.name("forecast");
                writer.beginArray();
                for (Forecast cast : forecast) {
                    if (cast == null)
                        writer.nullValue();
                    else
                        cast.toJson(writer);
                }
                writer.endArray();
            }

            // "hr_forecast" : ""
            if (hrForecast != null) {
                writer.name("hr_forecast");
                writer.beginArray();
                for (HourlyForecast hr_cast : hrForecast) {
                    if (hr_cast == null)
                        writer.nullValue();
                    else
                        hr_cast.toJson(writer);
                }
                writer.endArray();
            }

            // "txt_forecast" : ""
            if (txtForecast != null) {
                writer.name("txt_forecast");
                writer.beginArray();
                for (TextForecast txt_cast : txtForecast) {
                    if (txt_cast == null)
                        writer.nullValue();
                    else
                        txt_cast.toJson(writer);
                }
                writer.endArray();
            }

            // "minForecast" : ""
            if (minForecast != null) {
                writer.name("minForecast");
                writer.beginArray();
                for (MinutelyForecast min_cast : minForecast) {
                    if (min_cast == null)
                        writer.nullValue();
                    else
                        min_cast.toJson(writer);
                }
                writer.endArray();
            }

            // "aqiForecast" : ""
            if (aqiForecast != null) {
                writer.name("aqiForecast");
                writer.beginArray();
                for (AirQuality aqi_cast : aqiForecast) {
                    if (aqi_cast == null)
                        writer.nullValue();
                    else
                        aqi_cast.toJson(writer);
                }
                writer.endArray();
            }

            // "condition" : ""
            writer.name("condition");
            if (condition == null)
                writer.nullValue();
            else
                condition.toJson(writer);

            // "atmosphere" : ""
            writer.name("atmosphere");
            if (atmosphere == null)
                writer.nullValue();
            else
                atmosphere.toJson(writer);

            // "astronomy" : ""
            writer.name("astronomy");
            if (astronomy == null)
                writer.nullValue();
            else
                astronomy.toJson(writer);

            // "precipitation" : ""
            if (precipitation != null) {
                writer.name("precipitation");
                if (precipitation == null)
                    writer.nullValue();
                else
                    precipitation.toJson(writer);
            }

            // "weather_alerts" : ""
            if (weather_alerts != null) {
                writer.name("weather_alerts");
                writer.beginArray();
                for (WeatherAlert alert : weather_alerts) {
                    if (alert == null)
                        writer.nullValue();
                    else
                        alert.toJson(writer);
                }
                writer.endArray();
            }

            // "ttl" : ""
            writer.name("ttl");
            writer.value(ttl);

            // "source" : ""
            writer.name("source");
            writer.value(source);

            // "query" : ""
            writer.name("query");
            writer.value(query);

            // "locale" : ""
            writer.name("locale");
            writer.value(locale);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "LocationData: error writing json string");
        }
    }

    public boolean isValid() {
        if (location == null || condition == null || atmosphere == null)
            return false;
        else
            return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Weather weather = (Weather) o;

        if (!Objects.equals(location, weather.location))
            return false;
        if (!Objects.equals(updateTime, weather.updateTime))
            return false;
        if (!Objects.equals(forecast, weather.forecast))
            return false;
        if (!Objects.equals(hrForecast, weather.hrForecast))
            return false;
        if (!Objects.equals(txtForecast, weather.txtForecast))
            return false;
        if (!Objects.equals(minForecast, weather.minForecast))
            return false;
        if (!Objects.equals(aqiForecast, weather.aqiForecast))
            return false;
        if (!Objects.equals(condition, weather.condition))
            return false;
        if (!Objects.equals(atmosphere, weather.atmosphere))
            return false;
        if (!Objects.equals(astronomy, weather.astronomy))
            return false;
        if (!Objects.equals(precipitation, weather.precipitation))
            return false;
        if (!Objects.equals(weather_alerts, weather.weather_alerts))
            return false;
        if (ttl != weather.ttl) return false;
        if (!Objects.equals(source, weather.source)) return false;
        if (!query.equals(weather.query)) return false;
        return Objects.equals(locale, weather.locale);
    }

    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + (updateTime != null ? updateTime.hashCode() : 0);
        result = 31 * result + (forecast != null ? forecast.hashCode() : 0);
        result = 31 * result + (hrForecast != null ? hrForecast.hashCode() : 0);
        result = 31 * result + (txtForecast != null ? txtForecast.hashCode() : 0);
        result = 31 * result + (minForecast != null ? minForecast.hashCode() : 0);
        result = 31 * result + (aqiForecast != null ? aqiForecast.hashCode() : 0);
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        result = 31 * result + (atmosphere != null ? atmosphere.hashCode() : 0);
        result = 31 * result + (astronomy != null ? astronomy.hashCode() : 0);
        result = 31 * result + (precipitation != null ? precipitation.hashCode() : 0);
        result = 31 * result + (weather_alerts != null ? weather_alerts.hashCode() : 0);
        result = 31 * result + ttl;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + query.hashCode();
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        return result;
    }
}