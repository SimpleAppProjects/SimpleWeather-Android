package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.metno.Weatherdata;

import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "weatherdata")
public class Weather {
    @Ignore
    public static final transient String NA = "N/A";

    @ColumnInfo(name = "locationblob")
    private Location location;
    @ColumnInfo(name = "update_time")
    private ZonedDateTime updateTime;
    @ColumnInfo(name = "forecastblob")
    private Forecast[] forecast;
    @ColumnInfo(name = "hrforecastblob")
    private HourlyForecast[] hrForecast;
    @ColumnInfo(name = "txtforecastblob")
    private TextForecast[] txtForecast;
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
    private transient List<WeatherAlert> weather_alerts;
    private String ttl;
    private String source;
    @PrimaryKey
    @NonNull
    private String query;
    private String locale;

    public Weather() {
        // Needed for deserialization
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.weatheryahoo.Rootobject root) {
        location = new Location(root.getLocation());
        updateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(root.getCurrentObservation().getPubDate())), ZoneOffset.UTC);
        forecast = new Forecast[root.getForecasts().size()];
        for (int i = 0; i < forecast.length; i++) {
            forecast[i] = new Forecast(root.getForecasts().get(i));
        }
        condition = new Condition(root.getCurrentObservation());
        atmosphere = new Atmosphere(root.getCurrentObservation().getAtmosphere());
        astronomy = new Astronomy(root.getCurrentObservation().getAstronomy());
        ttl = "120";

        // Set feelslike temp
        if (condition.getTempF() > 80) {
            condition.setFeelslikeF(WeatherUtils.calculateHeatIndex((float) condition.getTempF(), Integer.valueOf(atmosphere.getHumidity().replace("%", ""))));
            condition.setFeelslikeC(Double.valueOf(ConversionMethods.FtoC(Double.toString(condition.getFeelslikeF()))));
        }

        source = WeatherAPI.YAHOO;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.weatherunderground.Rootobject root) throws WeatherException {
        location = new Location(root.getCurrentObservation());
        updateTime = ZonedDateTime.parse(root.getCurrentObservation().getLocalTimeRfc822(), DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH));
        forecast = new Forecast[root.getForecast().getSimpleforecast().getForecastday().size()];
        for (int i = 0; i < forecast.length; i++) {
            forecast[i] = new Forecast(root.getForecast().getSimpleforecast().getForecastday().get(i));

            if (i == 0) {
                // Note: WUnderground API bug
                // Data sometimes returns forecast from some date in the past
                // If we come across this invalidate the data
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                LocalDateTime curr = forecast[i].getDate().atOffset(ZoneOffset.UTC).toLocalDateTime();
                Duration diffSpan = Duration.between(now, curr).abs();
                if (curr.compareTo(now) < 0 && diffSpan.toDays() > 2)
                    throw new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);
            }
        }
        hrForecast = new HourlyForecast[root.getHourlyForecast().size()];
        for (int i = 0; i < hrForecast.length; i++) {
            hrForecast[i] = new HourlyForecast(root.getHourlyForecast().get(i));
        }
        txtForecast = new TextForecast[root.getForecast().getTxtForecast().getForecastday().size()];
        for (int i = 0; i < txtForecast.length; i++) {
            txtForecast[i] = new TextForecast(root.getForecast().getTxtForecast().getForecastday().get(i));

            // Note: WUnderground API bug
            // If array is not null and we're expecting data
            // and that data is invalid, invalidate weather data
            if (StringUtils.isNullOrWhitespace(txtForecast[i].getTitle()) &&
                    StringUtils.isNullOrWhitespace(txtForecast[i].getFcttext()) &&
                    StringUtils.isNullOrWhitespace(txtForecast[i].getFcttextMetric()))
                throw new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);
        }
        condition = new Condition(root.getCurrentObservation());
        atmosphere = new Atmosphere(root.getCurrentObservation());
        astronomy = new Astronomy(root.getSunPhase(), root.getMoonPhase());
        precipitation = new Precipitation(root.getForecast().getSimpleforecast().getForecastday().get(0));
        ttl = "60";

        source = WeatherAPI.WEATHERUNDERGROUND;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.openweather.CurrentRootobject currRoot,
                   com.thewizrd.shared_resources.weatherdata.openweather.ForecastRootobject foreRoot) {
        location = new Location(foreRoot);
        updateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currRoot.getDt()), ZoneOffset.UTC);

        // 5-day forecast / 3-hr forecast
        // 24hr / 3hr = 8items for each day
        forecast = new Forecast[5];
        hrForecast = new HourlyForecast[foreRoot.getList().size()];

        // Store potential min/max values
        float dayMax = Float.NaN;
        float dayMin = Float.NaN;
        int lastDay = 0;

        for (int i = 0; i < foreRoot.getList().size(); i++) {
            hrForecast[i] = new HourlyForecast(foreRoot.getList().get(i));

            float max = foreRoot.getList().get(i).getMain().getTempMax();
            if (!Float.isNaN(max) && (Float.isNaN(dayMax) || max > dayMax)) {
                dayMax = max;
            }

            float min = foreRoot.getList().get(i).getMain().getTempMin();
            if (!Float.isNaN(min) && (Float.isNaN(dayMin) || min < dayMin)) {
                dayMin = min;
            }

            // Get every 8th item for daily forecast
            if (i % 8 == 0) {
                lastDay = i / 8;

                forecast[i / 8] = new Forecast(foreRoot.getList().get(i));
            }

            // This is possibly the last forecast for the day (3-hrly forecast)
            // Set the min / max temp here and reset
            if (hrForecast[i].getDate().getHour() >= 21) {
                if (!Float.isNaN(dayMax)) {
                    forecast[lastDay].setHighF(ConversionMethods.KtoF(Float.toString(dayMax)));
                    forecast[lastDay].setHighC(ConversionMethods.KtoC(Float.toString(dayMax)));
                }
                if (!Float.isNaN(dayMin)) {
                    forecast[lastDay].setLowF(ConversionMethods.KtoF(Float.toString(dayMin)));
                    forecast[lastDay].setLowC(ConversionMethods.KtoC(Float.toString(dayMin)));
                }

                dayMax = Float.NaN;
                dayMin = Float.NaN;
            }
        }
        condition = new Condition(currRoot);
        atmosphere = new Atmosphere(currRoot);
        astronomy = new Astronomy(currRoot);
        precipitation = new Precipitation(currRoot);
        ttl = "120";

        query = Integer.toString(currRoot.getId());

        // Set feelslike temp
        condition.setFeelslikeF(Float.valueOf(WeatherUtils.getFeelsLikeTemp(Double.toString(condition.getTempF()), Double.toString(condition.getWindMph()), atmosphere.getHumidity())));
        condition.setFeelslikeC(Float.valueOf(ConversionMethods.FtoC(Double.toString(condition.getFeelslikeF()))));

        source = WeatherAPI.OPENWEATHERMAP;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.metno.Weatherdata foreRoot, com.thewizrd.shared_resources.weatherdata.metno.Astrodata astroRoot) {
        location = new Location(foreRoot);
        updateTime = ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse((foreRoot.getCreated()))), ZoneOffset.UTC);

        // 9-day forecast / hrly -> 6hrly forecast
        List<Forecast> forecastL = new ArrayList<>();
        List<HourlyForecast> hr_forecastL = new ArrayList<>();

        // Store potential min/max values
        float dayMax = Float.NaN;
        float dayMin = Float.NaN;

        // Flag values
        boolean end = false;
        boolean conditionSet = false;
        int fcastCount = 0;

        LocalDateTime startDate = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                foreRoot.getMeta().getModel().get(0).getFrom())), ZoneOffset.UTC);
        LocalDateTime endDate = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                foreRoot.getMeta().getModel().get(foreRoot.getMeta().getModel().size() <= 0 ? 0 : foreRoot.getMeta().getModel().size() - 1).getTo())),
                ZoneOffset.UTC).minusSeconds(LocalTime.of(6, 0, 0).toSecondOfDay());
        Forecast fcast = null;

        // Metno data is troublesome to parse thru
        for (int i = 0; i < foreRoot.getProduct().getTime().size(); i++) {
            Weatherdata.Time time = foreRoot.getProduct().getTime().get(i);
            LocalDateTime date = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC);

            // Create condition for next 2hrs from data
            if (i == 0 && date.equals(startDate)) {
                condition = new Condition(time);
                atmosphere = new Atmosphere(time);
                precipitation = new Precipitation(time);
            }

            // This contains all weather details
            if (!end && Duration.between(
                    LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC),
                    LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getTo())), ZoneOffset.UTC)).toMillis() == 0) {
                // Find max/min for each hour
                float temp = time.getLocation().getTemperature().getValue().floatValue();
                if (!Float.isNaN(temp) && (Float.isNaN(dayMax) || temp > dayMax)) {
                    dayMax = temp;
                }
                if (!Float.isNaN(temp) && (Float.isNaN(dayMin) || temp < dayMin)) {
                    dayMin = temp;
                }

                // Add a new hour
                hr_forecastL.add(new HourlyForecast(time));

                // Create new forecast
                if (date.getHour() == 0 || date.equals(startDate)) {
                    fcastCount++;

                    // Oops, we missed one
                    if (fcast != null && fcastCount != forecastL.size()) {
                        // Set forecast properties here:
                        // condition (set in provider GetWeather method)
                        // date
                        fcast.setDate(date);
                        // high
                        fcast.setHighF(ConversionMethods.CtoF(Double.toString(dayMax)));
                        fcast.setHighC(Integer.toString(Math.round(dayMax)));
                        // low
                        fcast.setLowF(ConversionMethods.CtoF(Double.toString(dayMin)));
                        fcast.setLowC(Integer.toString(Math.round(dayMin)));
                        // icon
                        forecastL.add(fcast);

                        // Reset
                        dayMax = Float.NaN;
                        dayMin = Float.NaN;
                    }

                    fcast = new Forecast(time);
                }
                // Last forecast for day; create forecast
                if (date.getHour() == 23 || date.equals(endDate)) {
                    // condition (set in provider GetWeather method)
                    // date
                    fcast.setDate(date);
                    // high
                    fcast.setHighF(ConversionMethods.CtoF(Double.toString(dayMax)));
                    fcast.setHighC(Integer.toString(Math.round(dayMax)));
                    // low
                    fcast.setLowF(ConversionMethods.CtoF(Double.toString(dayMin)));
                    fcast.setLowC(Integer.toString(Math.round(dayMin)));
                    // icon
                    forecastL.add(fcast);

                    if (date.equals(endDate))
                        end = true;

                    // Reset
                    dayMax = Float.NaN;
                    dayMin = Float.NaN;
                    fcast = null;
                }
            }

            // Get conditions for hour if available
            if (hr_forecastL.size() > 1 &&
                    hr_forecastL.get(hr_forecastL.size() - 2).getDate().equals(
                            ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC))) {
                // Set condition from id
                HourlyForecast hr = hr_forecastL.get(hr_forecastL.size() - 2);
                if (StringUtils.isNullOrEmpty(hr.getIcon())) {
                    if (time.getLocation().getSymbol() != null) {
                        hr.setCondition(time.getLocation().getSymbol().getId());
                        hr.setIcon(Byte.toString(time.getLocation().getSymbol().getNumber()));
                    }
                }
            } else if (end && hr_forecastL.get(hr_forecastL.size() <= 0 ? 0 : hr_forecastL.size() - 1).getDate().equals(
                    ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC))) {
                // Set condition from id
                HourlyForecast hr = hr_forecastL.get(hr_forecastL.size() <= 0 ? 0 : hr_forecastL.size() - 1);
                if (StringUtils.isNullOrEmpty(hr.getIcon())) {
                    if (time.getLocation().getSymbol() != null) {
                        hr.setCondition(time.getLocation().getSymbol().getId());
                        hr.setIcon(Byte.toString(time.getLocation().getSymbol().getNumber()));
                    }
                }
            }

            if (fcast != null && fcast.getDate().equals(LocalDateTime.ofInstant(
                    Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC)) &&
                    Duration.between(
                            LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC),
                            LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getTo())), ZoneOffset.UTC)).toHours() >= 1) {
                if (time.getLocation().getSymbol() != null) {
                    fcast.setCondition(time.getLocation().getSymbol().getId());
                    fcast.setIcon(Byte.toString(time.getLocation().getSymbol().getNumber()));
                }
            } else if (forecastL.size() > 0 && forecastL.get(forecastL.size() <= 0 ? 0 : forecastL.size() - 1).getDate().equals(
                    LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC)) &&
                    Duration.between(
                            LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC),
                            LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getTo())), ZoneOffset.UTC)).toHours() >= 1) {
                int last = forecastL.size() <= 0 ? 0 : forecastL.size() - 1;
                if (StringUtils.isNullOrEmpty(forecastL.get(last).getIcon())) {
                    if (time.getLocation().getSymbol() != null) {
                        forecastL.get(last).setCondition(time.getLocation().getSymbol().getId());
                        forecastL.get(last).setIcon(Byte.toString(time.getLocation().getSymbol().getNumber()));
                    }
                }
            }

            if (!conditionSet && condition != null && date.equals(startDate) &&
                    Duration.between(
                            LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC),
                            LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getTo())), ZoneOffset.UTC)).toHours() >= 2) {
                // Set condition from id
                if (time.getLocation().getSymbol() != null) {
                    condition.setIcon(Byte.toString(time.getLocation().getSymbol().getNumber()));
                    condition.setWeather(time.getLocation().getSymbol().getId());
                }

                conditionSet = true;
            }
        }

        forecast = forecastL.toArray(new Forecast[0]);
        hrForecast = hr_forecastL.toArray(new HourlyForecast[0]);
        astronomy = new Astronomy(astroRoot);
        ttl = "120";

        query = String.format("lat=%s&lon=%s", location.getLatitude(), location.getLongitude());

        // Set feelslike temp
        condition.setFeelslikeF(Float.valueOf(WeatherUtils.getFeelsLikeTemp(Double.toString(condition.getTempF()), Double.toString(condition.getWindMph()), atmosphere.getHumidity())));
        condition.setFeelslikeC(Float.valueOf(ConversionMethods.FtoC(Double.toString(condition.getFeelslikeF()))));

        source = WeatherAPI.METNO;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.here.Rootobject root) {
        ZonedDateTime now = ZonedDateTime.parse(root.getFeedCreation());

        location = new Location(root.getObservations().getLocation().get(0));
        updateTime = now;
        forecast = new Forecast[root.getDailyForecasts().getForecastLocation().getForecast().size()];
        for (int i = 0; i < forecast.length; i++) {
            forecast[i] = new Forecast(root.getDailyForecasts().getForecastLocation().getForecast().get(i));
        }
        List<HourlyForecast> tmp_hr_forecast = new ArrayList<>(root.getHourlyForecasts().getForecastLocation().getForecast().size());
        for (com.thewizrd.shared_resources.weatherdata.here.ForecastItem1 forecast1 : root.getHourlyForecasts().getForecastLocation().getForecast()) {
            if (ZonedDateTime.parse(forecast1.getUtcTime()).compareTo(now.withZoneSameInstant(ZoneOffset.UTC)) < 0)
                continue;

            tmp_hr_forecast.add(new HourlyForecast(forecast1));
        }
        hrForecast = tmp_hr_forecast.toArray(new HourlyForecast[0]);
        txtForecast = new TextForecast[root.getDailyForecasts().getForecastLocation().getForecast().size()];
        for (int i = 0; i < txtForecast.length; i++) {
            txtForecast[i] = new TextForecast(root.getDailyForecasts().getForecastLocation().getForecast().get(i));
        }
        condition = new Condition(root.getObservations().getLocation().get(0).getObservation().get(0),
                root.getDailyForecasts().getForecastLocation().getForecast().get(0));
        atmosphere = new Atmosphere(root.getObservations().getLocation().get(0).getObservation().get(0));
        astronomy = new Astronomy(root.getAstronomy().getAstronomy());
        precipitation = new Precipitation(root.getDailyForecasts().getForecastLocation().getForecast().get(0));
        ttl = "180";

        source = WeatherAPI.HERE;
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

    public Forecast[] getForecast() {
        return forecast;
    }

    public void setForecast(Forecast[] forecast) {
        this.forecast = forecast;
    }

    public HourlyForecast[] getHrForecast() {
        return hrForecast;
    }

    public void setHrForecast(HourlyForecast[] hr_forecast) {
        this.hrForecast = hr_forecast;
    }

    public TextForecast[] getTxtForecast() {
        return txtForecast;
    }

    public void setTxtForecast(TextForecast[] txt_forecast) {
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

    public List<WeatherAlert> getWeatherAlerts() {
        return weather_alerts;
    }

    public void setWeatherAlerts(List<WeatherAlert> weather_alerts) {
        this.weather_alerts = weather_alerts;
    }

    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public static Weather fromJson(JsonReader reader) {
        Weather obj = null;

        try {
            obj = new Weather();

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
                        obj.location = Location.fromJson(reader);
                        break;
                    case "update_time":
                        String json = reader.nextString();
                        ZonedDateTime result = null;
                        try {
                            result = ZonedDateTime.parse(json, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ"));
                            if (obj.getLocation().getTzOffset() != null && result.getOffset().getTotalSeconds() == 0) {
                                result = ZonedDateTime.ofInstant(result.toInstant(), obj.getLocation().getTzOffset());
                            }
                        } catch (Exception e) {
                            // If we can't parse as DateTimeOffset try DateTime (data could be old)
                            result = ZonedDateTime.parse(json);
                        }

                        obj.setUpdateTime(result);
                        break;
                    case "forecast":
                        ArrayList<Forecast> forecasts = new ArrayList<>();

                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING)
                                forecasts.add(Forecast.fromJson(reader));
                        }
                        obj.forecast = forecasts.toArray(new Forecast[0]);

                        if (reader.peek() == JsonToken.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "hr_forecast":
                        ArrayList<HourlyForecast> hr_forecasts = new ArrayList<>();
                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING)
                                hr_forecasts.add(HourlyForecast.fromJson(reader));
                        }
                        obj.hrForecast = hr_forecasts.toArray(new HourlyForecast[0]);

                        if (reader.peek() == JsonToken.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "txt_forecast":
                        ArrayList<TextForecast> txt_forecasts = new ArrayList<>();
                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING)
                                txt_forecasts.add(TextForecast.fromJson(reader));
                        }
                        obj.txtForecast = txt_forecasts.toArray(new TextForecast[0]);

                        if (reader.peek() == JsonToken.END_ARRAY)
                            reader.endArray(); // EndArray

                        break;
                    case "condition":
                        obj.condition = Condition.fromJson(reader);
                        break;
                    case "atmosphere":
                        obj.atmosphere = Atmosphere.fromJson(reader);
                        break;
                    case "astronomy":
                        obj.astronomy = Astronomy.fromJson(reader);
                        break;
                    case "precipitation":
                        obj.precipitation = Precipitation.fromJson(reader);
                        break;
                    case "ttl":
                        obj.ttl = reader.nextString();
                        break;
                    case "source":
                        obj.source = reader.nextString();
                        break;
                    case "query":
                        obj.query = reader.nextString();
                        break;
                    case "locale":
                        obj.locale = reader.nextString();
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

            // "location" : ""
            writer.name("location");
            if (location == null)
                writer.nullValue();
            else
                writer.value(location.toJson());

            // "update_time" : ""
            writer.name("update_time");
            writer.value(updateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ", Locale.ROOT)));

            // "forecast" : ""
            if (forecast != null) {
                writer.name("forecast");
                writer.beginArray();
                for (Forecast cast : forecast) {
                    if (cast == null)
                        writer.nullValue();
                    else
                        writer.value(cast.toJson());
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
                        writer.value(hr_cast.toJson());
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
                        writer.value(txt_cast.toJson());
                }
                writer.endArray();
            }

            // "condition" : ""
            writer.name("condition");
            if (condition == null)
                writer.nullValue();
            else
                writer.value(condition.toJson());

            // "atmosphere" : ""
            writer.name("atmosphere");
            if (atmosphere == null)
                writer.nullValue();
            else
                writer.value(atmosphere.toJson());

            // "astronomy" : ""
            writer.name("astronomy");
            if (astronomy == null)
                writer.nullValue();
            else
                writer.value(astronomy.toJson());

            // "precipitation" : ""
            if (precipitation != null) {
                writer.name("precipitation");
                if (precipitation == null)
                    writer.nullValue();
                else
                    writer.value(precipitation.toJson());
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
        return sw.toString();
    }

    public boolean isValid() {
        if (location == null || (forecast == null || forecast.length == 0)
                || condition == null || atmosphere == null || astronomy == null)
            return false;
        else
            return true;
    }
}