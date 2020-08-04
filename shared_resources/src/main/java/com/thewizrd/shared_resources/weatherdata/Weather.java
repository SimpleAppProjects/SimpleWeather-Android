package com.thewizrd.shared_resources.weatherdata;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.common.collect.Iterables;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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

    @RestrictTo({RestrictTo.Scope.LIBRARY, RestrictTo.Scope.TESTS})
    public Weather() {
        // Needed for deserialization
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.weatheryahoo.Rootobject root) {
        location = new Location(root.getLocation());
        updateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(root.getCurrentObservation().getPubDate())), ZoneOffset.UTC);
        forecast = new ArrayList<>(root.getForecasts().size());
        for (int i = 0; i < root.getForecasts().size(); i++) {
            forecast.add(new Forecast(root.getForecasts().get(i)));
        }
        condition = new Condition(root.getCurrentObservation());
        atmosphere = new Atmosphere(root.getCurrentObservation().getAtmosphere());
        astronomy = new Astronomy(root.getCurrentObservation().getAstronomy());
        ttl = 120;

        // Set feelslike temp
        if (condition.getTempF() != null && condition.getTempF() > 80 && atmosphere.getHumidity() != null) {
            condition.setFeelslikeF(WeatherUtils.calculateHeatIndex(condition.getTempF(), atmosphere.getHumidity()));
            condition.setFeelslikeC(ConversionMethods.FtoC(condition.getFeelslikeF()));
        }

        if ((condition.getHighF() == null || condition.getHighC() == null) && forecast.size() > 0) {
            condition.setHighF(forecast.get(0).getHighF());
            condition.setHighC(forecast.get(0).getHighC());
            condition.setLowF(forecast.get(0).getLowF());
            condition.setLowC(forecast.get(0).getLowC());
        }

        condition.setObservationTime(updateTime);

        source = WeatherAPI.YAHOO;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.openweather.Rootobject root) {
        location = new Location(root);
        updateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(root.getCurrent().getDt()), ZoneOffset.UTC);

        forecast = new ArrayList<>(root.getDaily().size());
        txtForecast = new ArrayList<>(root.getDaily().size());
        for (com.thewizrd.shared_resources.weatherdata.openweather.DailyItem daily : root.getDaily()) {
            forecast.add(new Forecast(daily));
            txtForecast.add(new TextForecast(daily));
        }
        hrForecast = new ArrayList<>(root.getHourly().size());
        for (com.thewizrd.shared_resources.weatherdata.openweather.HourlyItem hourly : root.getHourly()) {
            hrForecast.add(new HourlyForecast(hourly));
        }

        condition = new Condition(root.getCurrent());
        atmosphere = new Atmosphere(root.getCurrent());
        astronomy = new Astronomy(root.getCurrent());
        precipitation = new Precipitation(root.getCurrent());
        ttl = 120;

        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("#.####");
        query = String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.getLatitude()), location.getLongitude());

        if ((condition.getHighF() == null || condition.getHighC() == null) && forecast.size() > 0) {
            condition.setHighF(forecast.get(0).getHighF());
            condition.setHighC(forecast.get(0).getHighC());
            condition.setLowF(forecast.get(0).getLowF());
            condition.setLowC(forecast.get(0).getLowC());
        }

        source = WeatherAPI.OPENWEATHERMAP;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.metno.Response foreRoot, com.thewizrd.shared_resources.weatherdata.metno.AstroResponse astroRoot) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        location = new Location(foreRoot);
        updateTime = now;

        // 9-day forecast / hrly -> 6hrly forecast
        forecast = new ArrayList<>(10);
        hrForecast = new ArrayList<>(foreRoot.getProperties().getTimeseries().size());

        // Store potential min/max values
        float dayMax = Float.NaN;
        float dayMin = Float.NaN;

        LocalDateTime currentDate = LocalDateTime.MIN;
        Forecast fcast = null;

        // Metno data is troublesome to parse thru
        for (int i = 0; i < foreRoot.getProperties().getTimeseries().size(); i++) {
            com.thewizrd.shared_resources.weatherdata.metno.TimeseriesItem time = foreRoot.getProperties().getTimeseries().get(i);
            LocalDateTime date = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getTime())), ZoneOffset.UTC);

            // Create condition for next 2hrs from data
            if (i == 0) {
                condition = new Condition(time);
                atmosphere = new Atmosphere(time);
                precipitation = new Precipitation(time);
            }

            // Add a new hour
            if (!date.truncatedTo(ChronoUnit.HOURS).isBefore(now.toLocalDateTime().truncatedTo(ChronoUnit.HOURS)))
                hrForecast.add(new HourlyForecast(time));

            // Create new forecast
            if (!currentDate.toLocalDate().isEqual(date.toLocalDate()) &&
                    !date.isBefore(currentDate.plusDays(1))) {
                // Last forecast for day; create forecast
                if (fcast != null) {
                    // condition (set in provider GetWeather method)
                    // date
                    fcast.setDate(currentDate);
                    // high
                    fcast.setHighF(ConversionMethods.CtoF(dayMax));
                    fcast.setHighC((float) Math.round(dayMax));
                    // low
                    fcast.setLowF(ConversionMethods.CtoF(dayMin));
                    fcast.setLowC((float) Math.round(dayMin));

                    forecast.add(fcast);
                }

                currentDate = date;
                fcast = new Forecast(time);
                fcast.setDate(date);

                // Reset
                dayMax = Float.NaN;
                dayMin = Float.NaN;
            }

            // Find max/min for each hour
            float temp = time.getData().getInstant().getDetails().getAirTemperature() != null ?
                    time.getData().getInstant().getDetails().getAirTemperature() : Float.NaN;
            if (!Float.isNaN(temp) && (Float.isNaN(dayMax) || temp > dayMax)) {
                dayMax = temp;
            }
            if (!Float.isNaN(temp) && (Float.isNaN(dayMin) || temp < dayMin)) {
                dayMin = temp;
            }
        }

        fcast = Iterables.getLast(forecast, null);
        if (fcast != null && (fcast.getCondition() == null && fcast.getIcon() == null)) {
            forecast.remove(forecast.size() - 1);
        }

        HourlyForecast hrfcast = Iterables.getLast(hrForecast, null);
        if (hrfcast != null && (hrfcast.getCondition() == null && hrfcast.getIcon() == null)) {
            hrForecast.remove(hrForecast.size() - 1);
        }

        astronomy = new Astronomy(astroRoot);
        ttl = 120;

        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("#.####");
        query = String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.getLatitude()), location.getLongitude());

        if ((condition.getHighF() == null || condition.getHighC() == null) && forecast.size() > 0) {
            condition.setHighF(forecast.get(0).getHighF());
            condition.setHighC(forecast.get(0).getHighC());
            condition.setLowF(forecast.get(0).getLowF());
            condition.setLowC(forecast.get(0).getLowC());
        }

        condition.setObservationTime(ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(foreRoot.getProperties().getMeta().getUpdatedAt())), ZoneOffset.UTC));

        source = WeatherAPI.METNO;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.here.Rootobject root) {
        ZonedDateTime now = ZonedDateTime.parse(root.getFeedCreation());

        location = new Location(root.getObservations().getLocation().get(0));
        updateTime = now;
        forecast = new ArrayList<>(root.getDailyForecasts().getForecastLocation().getForecast().size());
        txtForecast = new ArrayList<>(root.getDailyForecasts().getForecastLocation().getForecast().size());
        for (com.thewizrd.shared_resources.weatherdata.here.ForecastItem fcast : root.getDailyForecasts().getForecastLocation().getForecast()) {
            forecast.add(new Forecast(fcast));
            txtForecast.add(new TextForecast(fcast));
        }
        hrForecast = new ArrayList<>(root.getHourlyForecasts().getForecastLocation().getForecast().size());
        for (com.thewizrd.shared_resources.weatherdata.here.ForecastItem1 forecast1 : root.getHourlyForecasts().getForecastLocation().getForecast()) {
            if (ZonedDateTime.parse(forecast1.getUtcTime()).truncatedTo(ChronoUnit.HOURS).isBefore(now.truncatedTo(ChronoUnit.HOURS)))
                continue;

            hrForecast.add(new HourlyForecast(forecast1));
        }

        com.thewizrd.shared_resources.weatherdata.here.ObservationItem observation = root.getObservations().getLocation().get(0).getObservation().get(0);
        com.thewizrd.shared_resources.weatherdata.here.ForecastItem todaysForecast = root.getDailyForecasts().getForecastLocation().getForecast().get(0);

        condition = new Condition(observation, todaysForecast);
        atmosphere = new Atmosphere(observation);
        astronomy = new Astronomy(root.getAstronomy().getAstronomy());
        precipitation = new Precipitation(todaysForecast);
        ttl = 180;

        source = WeatherAPI.HERE;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.nws.PointsResponse pointsResponse, com.thewizrd.shared_resources.weatherdata.nws.ForecastResponse forecastResponse, com.thewizrd.shared_resources.weatherdata.nws.HourlyForecastResponse hourlyForecastResponse, com.thewizrd.shared_resources.weatherdata.nws.ObservationCurrentResponse obsCurrentResponse) {
        location = new Location(pointsResponse);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        updateTime = now;

        // ~8-day forecast
        forecast = new ArrayList<>(8);
        txtForecast = new ArrayList<>(16);

        for (int i = 0; i < forecastResponse.getPeriods().size(); i++) {
            com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem forecastItem = forecastResponse.getPeriods().get(i);

            if ((forecast.isEmpty() && !forecastItem.getIsDaytime()) ||
                    (forecast.size() == forecastResponse.getPeriods().size() - 1 && forecastItem.getIsDaytime())) {
                forecast.add(new Forecast(forecastItem));
                txtForecast.add(new TextForecast(forecastItem));
            } else if (forecastItem.getIsDaytime() && (i + 1) < forecastResponse.getPeriods().size()) {
                com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem nightForecastItem = forecastResponse.getPeriods().get(i + 1);
                forecast.add(new Forecast(forecastItem, nightForecastItem));
                txtForecast.add(new TextForecast(forecastItem, nightForecastItem));

                i++;
            }
        }
        if (hourlyForecastResponse != null) {
            hrForecast = new ArrayList<>(hourlyForecastResponse.getPeriods().size());
            for (com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem period : hourlyForecastResponse.getPeriods()) {
                if (ZonedDateTime.parse(period.getStartTime(), DateTimeFormatter.ISO_ZONED_DATE_TIME).truncatedTo(ChronoUnit.HOURS).isBefore(now.truncatedTo(ChronoUnit.HOURS)))
                    continue;

                hrForecast.add(new HourlyForecast(period));
            }
        }
        condition = new Condition(obsCurrentResponse);
        atmosphere = new Atmosphere(obsCurrentResponse);
        //astronomy = new Astronomy(obsCurrentResponse);
        precipitation = new Precipitation(obsCurrentResponse);
        ttl = 180;

        if (condition.getHighF() == null && forecast.size() > 0) {
            condition.setHighF(forecast.get(0).getHighF());
            condition.setHighC(forecast.get(0).getHighC());
        }
        if (condition.getLowF() == null && forecast.size() > 0) {
            condition.setLowF(forecast.get(0).getLowF());
            condition.setLowC(forecast.get(0).getLowC());
        }

        source = WeatherAPI.NWS;
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
    public void fromJson(JsonReader reader) {
        try {
            while (reader.hasNext() && reader.peek() != JsonToken.END_OBJECT) {
                if (reader.peek() == JsonToken.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonToken.NULL) {
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

                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING || reader.peek() == JsonToken.BEGIN_OBJECT) {
                                Forecast fcast = new Forecast();
                                fcast.fromJson(reader);
                                forecasts.add(fcast);
                            }
                        }
                        this.forecast = forecasts;

                        if (reader.peek() == JsonToken.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "hr_forecast":
                        // Set initial cap to 90
                        // MetNo contains ~90 items, but HERE contains ~165
                        // If 90+ is needed, let the List impl allocate more
                        List<HourlyForecast> hr_forecasts = new ArrayList<>(90);
                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING || reader.peek() == JsonToken.BEGIN_OBJECT) {
                                HourlyForecast hr_fcast = new HourlyForecast();
                                hr_fcast.fromJson(reader);
                                hr_forecasts.add(hr_fcast);
                            }
                        }
                        this.hrForecast = hr_forecasts;

                        if (reader.peek() == JsonToken.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "txt_forecast":
                        // Set initial cap to 20
                        // Most provider forecasts are <= 10 (x2 for day & nt)
                        List<TextForecast> txt_forecasts = new ArrayList<>(20);
                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING || reader.peek() == JsonToken.BEGIN_OBJECT) {
                                TextForecast txtFcast = new TextForecast();
                                txtFcast.fromJson(reader);
                                txt_forecasts.add(txtFcast);
                            }
                        }
                        this.txtForecast = txt_forecasts;

                        if (reader.peek() == JsonToken.END_ARRAY)
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
                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING || reader.peek() == JsonToken.BEGIN_OBJECT) {
                                @SuppressLint("RestrictedApi") WeatherAlert alert = new WeatherAlert();
                                alert.fromJson(reader);
                                alerts.add(alert);
                            }
                        }
                        this.weather_alerts = alerts;

                        if (reader.peek() == JsonToken.END_ARRAY)
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

        if (location != null ? !location.equals(weather.location) : weather.location != null)
            return false;
        if (updateTime != null ? !updateTime.equals(weather.updateTime) : weather.updateTime != null)
            return false;
        if (forecast != null ? !forecast.equals(weather.forecast) : weather.forecast != null)
            return false;
        if (hrForecast != null ? !hrForecast.equals(weather.hrForecast) : weather.hrForecast != null)
            return false;
        if (txtForecast != null ? !txtForecast.equals(weather.txtForecast) : weather.txtForecast != null)
            return false;
        if (condition != null ? !condition.equals(weather.condition) : weather.condition != null)
            return false;
        if (atmosphere != null ? !atmosphere.equals(weather.atmosphere) : weather.atmosphere != null)
            return false;
        if (astronomy != null ? !astronomy.equals(weather.astronomy) : weather.astronomy != null)
            return false;
        if (precipitation != null ? !precipitation.equals(weather.precipitation) : weather.precipitation != null)
            return false;
        if (weather_alerts != null ? !weather_alerts.equals(weather.weather_alerts) : weather.weather_alerts != null)
            return false;
        if (ttl != weather.ttl) return false;
        if (source != null ? !source.equals(weather.source) : weather.source != null) return false;
        if (!query.equals(weather.query)) return false;
        return locale != null ? locale.equals(weather.locale) : weather.locale == null;
    }

    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + (updateTime != null ? updateTime.hashCode() : 0);
        result = 31 * result + (forecast != null ? forecast.hashCode() : 0);
        result = 31 * result + (hrForecast != null ? hrForecast.hashCode() : 0);
        result = 31 * result + (txtForecast != null ? txtForecast.hashCode() : 0);
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