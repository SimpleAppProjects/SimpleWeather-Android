package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.here.AlertsItem;
import com.thewizrd.shared_resources.weatherdata.here.TimeSegmentItem;
import com.thewizrd.shared_resources.weatherdata.nws.GraphItem;
import com.thewizrd.shared_resources.weatherdata.weatherunderground.Alert;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

public class WeatherAlert {
    @SerializedName("Type")
    private WeatherAlertType type = WeatherAlertType.SPECIALWEATHERALERT;
    @SerializedName("Severity")
    private WeatherAlertSeverity severity = WeatherAlertSeverity.UNKNOWN;
    @SerializedName("Title")
    private String title;
    @SerializedName("Message")
    private String message;
    @SerializedName("Attribution")
    private String attribution;
    @SerializedName("Date")
    private ZonedDateTime date;
    @SerializedName("ExpiresDate")
    private ZonedDateTime expiresDate;
    @SerializedName("Notified")
    private boolean notified;

    private WeatherAlert() {

    }

    public WeatherAlert(Alert alert) {
        // Alert Type
        switch (alert.getType()) {
            case "HUR": // Hurricane Local Statement
                type = WeatherAlertType.HURRICANELOCALSTATEMENT;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "HWW": // Hurricane Wind Warning
                type = WeatherAlertType.HURRICANEWINDWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "TOR": // Tornado Warning
                type = WeatherAlertType.TORNADOWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "TOW": // Tornado Watch
                type = WeatherAlertType.TORNADOWATCH;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "WRN": // Severe Thunderstorm Warning
                type = WeatherAlertType.SEVERETHUNDERSTORMWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "SEW": // Severe Thunderstorm Watch
                type = WeatherAlertType.SEVERETHUNDERSTORMWATCH;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "WIN": // Winter Weather Advisory
                type = WeatherAlertType.WINTERWEATHER;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "FLO": // Flood Warning
                type = WeatherAlertType.FLOODWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "WAT": // Flood Watch
                type = WeatherAlertType.FLOODWATCH;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "WND": // High Wind Advisory
                type = WeatherAlertType.HIGHWIND;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "HEA": // Heat Advisory
                type = WeatherAlertType.HEAT;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "FOG": // Dense Fog Advisory
                type = WeatherAlertType.DENSEFOG;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "FIR": // Fire Weather Advisory
                type = WeatherAlertType.FIRE;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "VOL": // Volcanic Activity Statement
                type = WeatherAlertType.VOLCANO;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "SVR": // Severe Weather Statement
                type = WeatherAlertType.SEVEREWEATHER;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "SPE": // Special Weather Statement
                type = WeatherAlertType.SPECIALWEATHERALERT;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            default:
                type = WeatherAlertType.SPECIALWEATHERALERT;
                severity = WeatherAlertSeverity.SEVERE;
                break;
        }

        if (StringUtils.isNullOrWhitespace(alert.getWtype_meteoalarm_name())) {
            // NWS Alerts
            title = alert.getDescription();
            message = alert.getMessage();

            date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(alert.getDate_epoch())), ZoneOffset.UTC);
            expiresDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(alert.getExpires_epoch())), ZoneOffset.UTC);

            attribution = "Information provided by the U.S. National Weather Service";
        } else {
            // Meteoalarm.eu Alerts
            title = alert.getWtype_meteoalarm_name();
            message = alert.getDescription();
            attribution = alert.getAttribution();

            try {
                long date_epoch = Long.parseLong(alert.getDate_epoch());
                date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(date_epoch), ZoneOffset.UTC);
            } catch (NumberFormatException ex) {
                date = ZonedDateTime.parse(alert.getDate());
            }

            try {
                long expires_epoch = Long.parseLong(alert.getDate_epoch());
                date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(expires_epoch), ZoneOffset.UTC);
            } catch (NumberFormatException ex) {
                date = ZonedDateTime.parse(alert.getExpires());
            }
        }
    }

    public WeatherAlert(GraphItem alert) {
        // Alert Type
        switch (alert.getEvent()) {
            case "Hurricane Local Statement":
            case "Hurricane Force Wind Watch":
            case "Hurricane Watch":
            case "Hurricane Force Wind Warning":
            case "Hurricane Warning":
                type = WeatherAlertType.HURRICANEWINDWARNING;
                break;
            case "Tornado Warning":
                type = WeatherAlertType.TORNADOWARNING;
                break;
            case "Tornado Watch":
                type = WeatherAlertType.TORNADOWATCH;
                break;
            case "Severe Thunderstorm Warning":
                type = WeatherAlertType.SEVERETHUNDERSTORMWARNING;
                break;
            case "Severe Thunderstorm Watch":
                type = WeatherAlertType.SEVERETHUNDERSTORMWATCH;
                break;
            case "Excessive Heat Warning":
            case "Excessive Heat Watch":
            case "Heat Advisory":
                type = WeatherAlertType.HEAT;
                break;
            case "Dense Fog Advisory":
                type = WeatherAlertType.DENSEFOG;
                break;
            case "Dense Smoke Advisory":
                type = WeatherAlertType.DENSESMOKE;
                break;
            case "Extreme Fire Danger":
            case "Fire Warning":
            case "Fire Weather Watch":
                type = WeatherAlertType.FIRE;
                break;
            case "Volcano Warning":
                type = WeatherAlertType.VOLCANO;
                break;
            case "Extreme Wind Warning":
            case "High Wind Warning":
            case "High Wind Watch":
            case "Lake Wind Advisory":
            case "Wind Advisory":
                type = WeatherAlertType.HIGHWIND;
                break;
            case "Lake Effect Snow Advisory":
            case "Lake Effect Snow Warning":
            case "Lake Effect Snow Watch":
            case "Snow Squall Warning":
            case "Ice Storm Warning":
            case "Winter Storm Warning":
            case "Winter Storm Watch":
            case "Winter Weather Advisory":
                type = WeatherAlertType.WINTERWEATHER;
                break;
            case "Earthquake Warning":
                type = WeatherAlertType.EARTHQUAKEWARNING;
                break;
            case "Gale Warning":
            case "Gale Watch":
                type = WeatherAlertType.GALEWARNING;
                break;
            default:
                if (alert.getEvent().contains("Flood Warning"))
                    type = WeatherAlertType.FLOODWARNING;
                else if (alert.getEvent().contains("Flood"))
                    type = WeatherAlertType.FLOODWATCH;
                else if (alert.getEvent().contains("Snow") || alert.getEvent().contains("Blizzard") ||
                        alert.getEvent().contains("Winter") || alert.getEvent().contains("Ice") ||
                        alert.getEvent().contains("Avalanche") || alert.getEvent().contains("Cold") ||
                        alert.getEvent().contains("Freez") || alert.getEvent().contains("Frost") ||
                        alert.getEvent().contains("Chill")) {
                    type = WeatherAlertType.WINTERWEATHER;
                } else if (alert.getEvent().contains("Dust"))
                    type = WeatherAlertType.DUSTADVISORY;
                else if (alert.getEvent().contains("Small Craft"))
                    type = WeatherAlertType.SMALLCRAFT;
                else if (alert.getEvent().contains("Storm"))
                    type = WeatherAlertType.STORMWARNING;
                else if (alert.getEvent().contains("Tsunami"))
                    type = WeatherAlertType.TSUNAMIWARNING;
                else
                    type = WeatherAlertType.SPECIALWEATHERALERT;
                break;
        }

        switch (alert.getSeverity()) {
            case "Minor":
                severity = WeatherAlertSeverity.MINOR;
                break;
            case "Moderate":
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "Severe":
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "Extreme":
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "Unknown":
            default:
                severity = WeatherAlertSeverity.UNKNOWN;
                break;
        }

        title = alert.getEvent();
        message = String.format("%s\n%s", alert.getDescription(), alert.getInstruction());

        date = ZonedDateTime.parse(alert.getSent());
        expiresDate = ZonedDateTime.parse(alert.getExpires());

        attribution = "Information provided by the U.S. National Weather Service";
    }

    public WeatherAlert(AlertsItem alert) {
        // Alert Type
        switch (alert.getType()) {
            case "2": // Coastal Flood Warning, Watch, or Statement
            case "3": // Flash Flood Watch
            case "4": // Flash Flood Statement
            case "6": // Flood Statement
                type = WeatherAlertType.FLOODWATCH;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "5": // Flash Flood Warning
            case "7": // Flood Warning
            case "8": // Urban and Small Stream Flood Advisory
                type = WeatherAlertType.FLOODWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "9": // Hurricane Local Statement
                type = WeatherAlertType.HURRICANELOCALSTATEMENT;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "13": // Public Severe Weather Alert
            case "29": // Severe Weather Statement
                type = WeatherAlertType.SEVEREWEATHER;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "14": // Red Flag Warning
                type = WeatherAlertType.SPECIALWEATHERALERT;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "15": // River Ice Statement
            case "18": // Snow Avalanche Bulletin
            case "37": // Winter Weather Advisory
                type = WeatherAlertType.WINTERWEATHER;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "21": // Severe Local Storm Watch or Watch Cancellation
            case "23": // Severe Local Storm Watch and Areal Outline
            case "24": // Marine Subtropical Storm Advisory
                type = WeatherAlertType.STORMWARNING;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "27": // Special Weather Statement
                type = WeatherAlertType.SPECIALWEATHERALERT;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "28": // Severe Thunderstorm Warning
                type = WeatherAlertType.SEVERETHUNDERSTORMWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "30": // Tropical Cyclone Advisory
            case "31": // Tropical Cyclone Advisory for Marine and Aviation Interests
            case "32": // Public Tropical Cyclone Advisory
            case "33": // Tropical Cyclone Update
                type = WeatherAlertType.HURRICANEWINDWARNING;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "34": // Tornado Warning
                type = WeatherAlertType.TORNADOWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "35": // Tsunami Watch or Warning
                type = WeatherAlertType.TSUNAMIWARNING;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "36": // Volcanic Activity Statement
                type = WeatherAlertType.VOLCANO;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            default:
                type = WeatherAlertType.SPECIALWEATHERALERT;
                severity = WeatherAlertSeverity.SEVERE;
                break;
        }

        title = alert.getDescription();
        message = alert.getDescription();

        setDateTimeFromSegment(alert.getTimeSegment());

        attribution = "Information provided by HERE Weather";
    }

    private DayOfWeek getDayOfWeekFromSegment(String dayofweek) {
        switch (dayofweek) {
            case "1": // Sun
            default:
                return DayOfWeek.SUNDAY;
            case "2": // Mon
                return DayOfWeek.MONDAY;
            case "3": // Tue
                return DayOfWeek.TUESDAY;
            case "4": // Wed
                return DayOfWeek.WEDNESDAY;
            case "5": // Thu
                return DayOfWeek.THURSDAY;
            case "6": // Fri
                return DayOfWeek.FRIDAY;
            case "7": // Sat
                return DayOfWeek.SATURDAY;
        }
    }

    private void setDateTimeFromSegment(List<TimeSegmentItem> timeSegment) {
        if (timeSegment.size() > 1) {
            int last = timeSegment.size() - 1;
            LocalDateTime startDate = DateTimeUtils.getClosestWeekday(getDayOfWeekFromSegment(timeSegment.get(0).getDayOfWeek()));
            LocalDateTime endDate = DateTimeUtils.getClosestWeekday(getDayOfWeekFromSegment(timeSegment.get(last).getDayOfWeek()));

            date = ZonedDateTime.of(startDate.plusSeconds(
                    getTimeFromSegment(timeSegment.get(0).getSegment()).toSecondOfDay()), ZoneOffset.UTC);
            expiresDate = ZonedDateTime.of(endDate.plusSeconds(
                    getTimeFromSegment(timeSegment.get(last).getSegment()).toSecondOfDay()), ZoneOffset.UTC);
        } else {
            LocalDateTime today = DateTimeUtils.getClosestWeekday(getDayOfWeekFromSegment(timeSegment.get(0).getDayOfWeek()));

            switch (timeSegment.get(0).getSegment()) {
                case "M": // Morning
                default:
                    date = ZonedDateTime.of(today.plusSeconds(
                            getTimeFromSegment("M").toSecondOfDay()), ZoneOffset.UTC);
                    expiresDate = ZonedDateTime.of(today.plusSeconds(
                            getTimeFromSegment("A").toSecondOfDay()), ZoneOffset.UTC);
                    break;
                case "A": // Afternoon
                    date = ZonedDateTime.of(today.plusSeconds(
                            getTimeFromSegment("A").toSecondOfDay()), ZoneOffset.UTC);
                    expiresDate = ZonedDateTime.of(today.plusSeconds(
                            getTimeFromSegment("E").toSecondOfDay()), ZoneOffset.UTC);
                    break;
                case "E": // Evening
                    date = ZonedDateTime.of(today.plusSeconds(
                            getTimeFromSegment("E").toSecondOfDay()), ZoneOffset.UTC);
                    expiresDate = ZonedDateTime.of(today.plusSeconds(
                            getTimeFromSegment("N").toSecondOfDay()), ZoneOffset.UTC);
                    break;
                case "N": // Night
                    date = ZonedDateTime.of(today.plusSeconds(
                            getTimeFromSegment("N").toSecondOfDay()), ZoneOffset.UTC);
                    expiresDate = ZonedDateTime.of(today.plusDays(1).plusSeconds(
                            getTimeFromSegment("M").toSecondOfDay()), ZoneOffset.UTC); // The next morning
                    break;
            }
        }
    }

    private LocalTime getTimeFromSegment(String segment) {
        LocalTime span = LocalTime.MIN;

        switch (segment) {
            case "M": // Morning
                span = LocalTime.of(5, 0, 0); // hh:mm:ss
                break;
            case "A": // Afternoon
                span = LocalTime.of(12, 0, 0); // hh:mm:ss
                break;
            case "E": // Evening
                span = LocalTime.of(17, 0, 0); // hh:mm:ss
                break;
            case "N": // Night
                span = LocalTime.of(21, 0, 0); // hh:mm:ss
                break;
            default:
                break;
        }

        return span;
    }

    public WeatherAlertType getType() {
        return type;
    }

    public void setType(WeatherAlertType type) {
        this.type = type;
    }

    public WeatherAlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(WeatherAlertSeverity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public ZonedDateTime getExpiresDate() {
        return expiresDate;
    }

    public void setExpiresDate(ZonedDateTime expiresDate) {
        this.expiresDate = expiresDate;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public static WeatherAlert fromJson(JsonReader extReader) {
        WeatherAlert obj = null;

        try {
            obj = new WeatherAlert();
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
                    case "Type":
                        obj.type = WeatherAlertType.valueOf(Integer.valueOf(reader.nextString()));
                        break;
                    case "Severity":
                        obj.severity = WeatherAlertSeverity.valueOf(Integer.valueOf(reader.nextString()));
                        break;
                    case "Title":
                        obj.title = reader.nextString();
                        break;
                    case "Message":
                        obj.message = reader.nextString();
                        break;
                    case "Attribution":
                        obj.attribution = reader.nextString();
                        break;
                    case "Date":
                        String json = reader.nextString();
                        ZonedDateTime result = null;
                        try {
                            result = ZonedDateTime.parse(json, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ", Locale.ROOT));
                        } catch (Exception e) {
                            // If we can't parse as DateTimeOffset try DateTime (data could be old)
                            result = ZonedDateTime.parse(json);
                        }
                        obj.date = result;
                        break;
                    case "ExpiresDate":
                        json = reader.nextString();
                        result = null;
                        try {
                            result = ZonedDateTime.parse(json, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ", Locale.ROOT));
                        } catch (Exception e) {
                            // If we can't parse as DateTimeOffset try DateTime (data could be old)
                            result = ZonedDateTime.parse(json);
                        }
                        obj.expiresDate = result;
                        break;
                    case "Notified":
                        obj.notified = reader.nextBoolean();
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

            // "Type" : ""
            writer.name("Type");
            writer.value(type.getValue());

            // "Severity" : ""
            writer.name("Severity");
            writer.value(severity.getValue());

            // "Title" : ""
            writer.name("Title");
            writer.value(title);

            // "Message" : ""
            writer.name("Message");
            writer.value(message);

            // "Attribution" : ""
            writer.name("Attribution");
            writer.value(attribution);

            // "Date" : ""
            writer.name("Date");
            writer.value(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ")));

            // "ExpiresDate" : ""
            writer.name("ExpiresDate");
            writer.value(expiresDate.format(DateTimeFormatter.ofPattern(("dd.MM.yyyy HH:mm:ss ZZZZZ"))));

            // "Notified" : ""
            writer.name("Notified");
            writer.value(notified);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "WeatherAlert: error writing json string");
        }

        return sw.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        WeatherAlert that = (WeatherAlert) o;

        return type == that.type &&
                severity == that.severity &&
                title.equals(that.title) &&
                message.equals(that.message) &&
                attribution.equals(that.attribution) &&
                date.equals(that.date) &&
                expiresDate.equals(that.expiresDate);
    }

    @Override
    public int hashCode() {
        int hashCode = 68217818;
        hashCode = hashCode * -1521134295 + type.hashCode();
        hashCode = hashCode * -1521134295 + severity.hashCode();
        hashCode = hashCode * -1521134295 + (title != null ? title.hashCode() : 0);
        hashCode = hashCode * -1521134295 + (message != null ? message.hashCode() : 0);
        hashCode = hashCode * -1521134295 + (attribution != null ? attribution.hashCode() : 0);
        hashCode = hashCode * -1521134295 + (date != null ? date.hashCode() : 0);
        hashCode = hashCode * -1521134295 + (expiresDate != null ? expiresDate.hashCode() : 0);
        return hashCode;
    }
}
