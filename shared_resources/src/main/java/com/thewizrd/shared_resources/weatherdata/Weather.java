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

import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
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
    private transient Collection<WeatherAlert> weather_alerts;
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
        forecast = new ArrayList<>(root.getForecasts().size());
        for (int i = 0; i < root.getForecasts().size(); i++) {
            forecast.add(new Forecast(root.getForecasts().get(i)));
        }
        condition = new Condition(root.getCurrentObservation());
        atmosphere = new Atmosphere(root.getCurrentObservation().getAtmosphere());
        astronomy = new Astronomy(root.getCurrentObservation().getAstronomy());
        ttl = "120";

        // Set feelslike temp
        if (condition.getTempF() > 80) {
            condition.setFeelslikeF(WeatherUtils.calculateHeatIndex((float) condition.getTempF(), Integer.parseInt(atmosphere.getHumidity().replace("%", ""))));
            condition.setFeelslikeC(Double.parseDouble(ConversionMethods.FtoC(Double.toString(condition.getFeelslikeF()))));
        }

        source = WeatherAPI.YAHOO;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.weatherunderground.Rootobject root) throws WeatherException {
        location = new Location(root.getCurrentObservation());
        updateTime = ZonedDateTime.parse(root.getCurrentObservation().getLocalTimeRfc822(), DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH));
        forecast = new ArrayList<>(root.getForecast().getSimpleforecast().getForecastday().size());
        for (int i = 0; i < root.getForecast().getSimpleforecast().getForecastday().size(); i++) {
            forecast.add(new Forecast(root.getForecast().getSimpleforecast().getForecastday().get(i)));

            if (i == 0) {
                // Note: WUnderground API bug
                // Data sometimes returns forecast from some date in the past
                // If we come across this invalidate the data
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                LocalDateTime curr = forecast.get(i).getDate().atOffset(ZoneOffset.UTC).toLocalDateTime();
                Duration diffSpan = Duration.between(now, curr).abs();
                if (curr.compareTo(now) < 0 && diffSpan.toDays() > 2)
                    throw new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);
            }
        }
        hrForecast = new ArrayList<>(root.getHourlyForecast().size());
        for (int i = 0; i < root.getHourlyForecast().size(); i++) {
            hrForecast.add(new HourlyForecast(root.getHourlyForecast().get(i)));
        }
        txtForecast = new ArrayList<>(root.getForecast().getTxtForecast().getForecastday().size());
        for (int i = 0; i < root.getForecast().getTxtForecast().getForecastday().size(); i++) {
            txtForecast.add(new TextForecast(root.getForecast().getTxtForecast().getForecastday().get(i)));

            // Note: WUnderground API bug
            // If array is not null and we're expecting data
            // and that data is invalid, invalidate weather data
            if (StringUtils.isNullOrWhitespace(txtForecast.get(i).getTitle()) &&
                    StringUtils.isNullOrWhitespace(txtForecast.get(i).getFcttext()) &&
                    StringUtils.isNullOrWhitespace(txtForecast.get(i).getFcttextMetric()))
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
        forecast = new ArrayList<>(5);
        hrForecast = new ArrayList<>(foreRoot.getList().size());

        // Store potential min/max values
        float dayMax = Float.NaN;
        float dayMin = Float.NaN;
        int lastDay = 0;

        for (int i = 0; i < foreRoot.getList().size(); i++) {
            hrForecast.add(new HourlyForecast(foreRoot.getList().get(i)));

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

                forecast.add(i / 8, new Forecast(foreRoot.getList().get(i)));
            }

            // This is possibly the last forecast for the day (3-hrly forecast)
            // Set the min / max temp here and reset
            if (hrForecast.get(i).getDate().getHour() >= 21) {
                if (!Float.isNaN(dayMax)) {
                    forecast.get(lastDay).setHighF(ConversionMethods.KtoF(Float.toString(dayMax)));
                    forecast.get(lastDay).setHighC(ConversionMethods.KtoC(Float.toString(dayMax)));
                }
                if (!Float.isNaN(dayMin)) {
                    forecast.get(lastDay).setLowF(ConversionMethods.KtoF(Float.toString(dayMin)));
                    forecast.get(lastDay).setLowC(ConversionMethods.KtoC(Float.toString(dayMin)));
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
        condition.setFeelslikeF(Float.parseFloat(WeatherUtils.getFeelsLikeTemp(Double.toString(condition.getTempF()), Double.toString(condition.getWindMph()), atmosphere.getHumidity())));
        condition.setFeelslikeC(Float.parseFloat(ConversionMethods.FtoC(Double.toString(condition.getFeelslikeF()))));

        source = WeatherAPI.OPENWEATHERMAP;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.metno.Weatherdata foreRoot, com.thewizrd.shared_resources.weatherdata.metno.Astrodata astroRoot) {
        location = new Location(foreRoot);
        updateTime = ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse((foreRoot.getCreated()))), ZoneOffset.UTC);

        // 9-day forecast / hrly -> 6hrly forecast
        forecast = new ArrayList<>(10);
        hrForecast = new ArrayList<>(90);

        // Store potential min/max values
        float dayMax = Float.NaN;
        float dayMin = Float.NaN;

        // Flag values
        boolean end = false;
        boolean conditionSet = false;
        int fcastCount = 0;

        LocalDateTime startDate = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                foreRoot.getMeta().getModel().getFrom())), ZoneOffset.UTC);
        LocalDateTime endDate = LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(
                foreRoot.getMeta().getModel().getTo())), ZoneOffset.UTC);
        Forecast fcast = null;

        // Metno data is troublesome to parse thru
        for (int i = 0; i < foreRoot.getProduct().getTime().size(); i++) {
            com.thewizrd.shared_resources.weatherdata.metno.Weatherdata.Time time = foreRoot.getProduct().getTime().get(i);
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
                hrForecast.add(new HourlyForecast(time));

                // Create new forecast
                if (date.getHour() == 0 || date.equals(startDate)) {
                    fcastCount++;

                    // Oops, we missed one
                    if (fcast != null && fcastCount != forecast.size()) {
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
                        forecast.add(fcast);

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
                    forecast.add(fcast);

                    if (date.equals(endDate))
                        end = true;

                    // Reset
                    dayMax = Float.NaN;
                    dayMin = Float.NaN;
                    fcast = null;
                }
            }

            // Get conditions for hour if available
            if (hrForecast.size() > 1 &&
                    hrForecast.get(hrForecast.size() - 2).getDate().equals(
                            ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC))) {
                // Set condition from id
                HourlyForecast hr = hrForecast.get(hrForecast.size() - 2);
                if (StringUtils.isNullOrEmpty(hr.getIcon())) {
                    if (time.getLocation().getSymbol() != null) {
                        hr.setCondition(time.getLocation().getSymbol().getId());
                        hr.setIcon(Byte.toString(time.getLocation().getSymbol().getNumber()));
                    }
                }
            } else if (end && hrForecast.get(hrForecast.size() <= 0 ? 0 : hrForecast.size() - 1).getDate().equals(
                    ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC))) {
                // Set condition from id
                HourlyForecast hr = hrForecast.get(hrForecast.size() <= 0 ? 0 : hrForecast.size() - 1);
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
            } else if (forecast.size() > 0 && forecast.get(forecast.size() <= 0 ? 0 : forecast.size() - 1).getDate().equals(
                    LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC)) &&
                    Duration.between(
                            LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getFrom())), ZoneOffset.UTC),
                            LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(time.getTo())), ZoneOffset.UTC)).toHours() >= 1) {
                int last = forecast.size() <= 0 ? 0 : forecast.size() - 1;
                if (StringUtils.isNullOrEmpty(forecast.get(last).getIcon())) {
                    if (time.getLocation().getSymbol() != null) {
                        forecast.get(last).setCondition(time.getLocation().getSymbol().getId());
                        forecast.get(last).setIcon(Byte.toString(time.getLocation().getSymbol().getNumber()));
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

        fcast = forecast.size() <= 0 ? null : forecast.get(forecast.size() - 1);
        if (fcast != null && (fcast.getCondition() == null && fcast.getIcon() == null)) {
            forecast.remove(forecast.size() - 1);
        }

        astronomy = new Astronomy(astroRoot);
        ttl = "120";

        query = String.format("lat=%s&lon=%s", location.getLatitude(), location.getLongitude());

        // Set feelslike temp
        condition.setFeelslikeF(Float.parseFloat(WeatherUtils.getFeelsLikeTemp(Double.toString(condition.getTempF()), Double.toString(condition.getWindMph()), atmosphere.getHumidity())));
        condition.setFeelslikeC(Float.parseFloat(ConversionMethods.FtoC(Double.toString(condition.getFeelslikeF()))));

        source = WeatherAPI.METNO;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.here.Rootobject root) {
        ZonedDateTime now = ZonedDateTime.parse(root.getFeedCreation());

        location = new Location(root.getObservations().getLocation().get(0));
        updateTime = now;
        forecast = new ArrayList<>(root.getDailyForecasts().getForecastLocation().getForecast().size());
        txtForecast = new ArrayList<>(root.getDailyForecasts().getForecastLocation().getForecast().size());
        for (int i = 0; i < root.getDailyForecasts().getForecastLocation().getForecast().size(); i++) {
            forecast.add(new Forecast(root.getDailyForecasts().getForecastLocation().getForecast().get(i)));
            txtForecast.add(new TextForecast(root.getDailyForecasts().getForecastLocation().getForecast().get(i)));
        }
        hrForecast = new ArrayList<>(root.getHourlyForecasts().getForecastLocation().getForecast().size());
        for (com.thewizrd.shared_resources.weatherdata.here.ForecastItem1 forecast1 : root.getHourlyForecasts().getForecastLocation().getForecast()) {
            if (ZonedDateTime.parse(forecast1.getUtcTime()).compareTo(now.withZoneSameInstant(ZoneOffset.UTC)) < 0)
                continue;

            hrForecast.add(new HourlyForecast(forecast1));
        }
        condition = new Condition(root.getObservations().getLocation().get(0).getObservation().get(0),
                root.getDailyForecasts().getForecastLocation().getForecast().get(0));
        atmosphere = new Atmosphere(root.getObservations().getLocation().get(0).getObservation().get(0));
        astronomy = new Astronomy(root.getAstronomy().getAstronomy());
        precipitation = new Precipitation(root.getDailyForecasts().getForecastLocation().getForecast().get(0));
        ttl = "180";

        source = WeatherAPI.HERE;
    }

    public Weather(com.thewizrd.shared_resources.weatherdata.nws.PointsResponse pointsResponse, com.thewizrd.shared_resources.weatherdata.nws.ForecastResponse forecastResponse, com.thewizrd.shared_resources.weatherdata.nws.HourlyForecastResponse hourlyForecastResponse, com.thewizrd.shared_resources.weatherdata.nws.ObservationCurrentResponse obsCurrentResponse) {
        location = new Location(pointsResponse);
        updateTime = ZonedDateTime.now();

        // ~8-day forecast
        forecast = new ArrayList<>(8);
        txtForecast = new ArrayList<>(16);

        for (int i = 0; i < forecastResponse.getPeriods().size(); i++) {
            com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem forecastItem = forecastResponse.getPeriods().get(i);

            if (forecast.isEmpty() && !forecastItem.isIsDaytime())
                continue;

            if (forecastItem.isIsDaytime() && (i + 1) < forecastResponse.getPeriods().size()) {
                com.thewizrd.shared_resources.weatherdata.nws.PeriodsItem nightForecastItem = forecastResponse.getPeriods().get(i + 1);
                forecast.add(new Forecast(forecastItem, nightForecastItem));

                txtForecast.add(new TextForecast(forecastItem));
                txtForecast.add(new TextForecast(nightForecastItem));

                i++;
            }
        }
        if (hourlyForecastResponse != null) {
            hrForecast = new ArrayList<>(hourlyForecastResponse.getPeriods().size());
            for (int i = 0; i < hourlyForecastResponse.getPeriods().size(); i++) {
                hrForecast.add(new HourlyForecast(hourlyForecastResponse.getPeriods().get(i)));
            }
        }
        condition = new Condition(obsCurrentResponse);
        atmosphere = new Atmosphere(obsCurrentResponse);
        //astronomy = new Astronomy(obsCurrentResponse);
        //precipitation = new Precipitation(obsCurrentResponse);
        ttl = "180";

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
                        // Set initial cap to 10
                        // Most provider forecasts are <= 10
                        List<Forecast> forecasts = new ArrayList<>(10);

                        if (reader.peek() == JsonToken.BEGIN_ARRAY)
                            reader.beginArray(); // StartArray

                        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
                            if (reader.peek() == JsonToken.STRING)
                                forecasts.add(Forecast.fromJson(reader));
                        }
                        obj.forecast = forecasts;

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
                            if (reader.peek() == JsonToken.STRING)
                                hr_forecasts.add(HourlyForecast.fromJson(reader));
                        }
                        obj.hrForecast = hr_forecasts;

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
                            if (reader.peek() == JsonToken.STRING)
                                txt_forecasts.add(TextForecast.fromJson(reader));
                        }
                        obj.txtForecast = txt_forecasts;

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
        if (ttl != null ? !ttl.equals(weather.ttl) : weather.ttl != null) return false;
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
        result = 31 * result + (ttl != null ? ttl.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + query.hashCode();
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        return result;
    }
}