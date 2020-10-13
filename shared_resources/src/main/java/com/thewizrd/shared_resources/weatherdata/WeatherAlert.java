package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.weatherdata.here.AlertsItem;
import com.thewizrd.shared_resources.weatherdata.here.TimeSegmentItem;
import com.thewizrd.shared_resources.weatherdata.here.WarningItem;
import com.thewizrd.shared_resources.weatherdata.here.WatchItem;
import com.thewizrd.shared_resources.weatherdata.nws.alerts.GraphItem;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;

public class WeatherAlert extends CustomJsonObject {
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

    @RestrictTo({RestrictTo.Scope.LIBRARY, RestrictTo.Scope.TESTS})
    public WeatherAlert() {

    }

    // NWS Alerts
    public WeatherAlert(GraphItem alert) {
        // Alert Type
        switch (alert.getEvent()) {
            case "Hurricane Local Statement":
                type = WeatherAlertType.HURRICANELOCALSTATEMENT;
                break;
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

        attribution = "U.S. National Weather Service";
    }

    // HERE GlobalAlerts
    public WeatherAlert(AlertsItem alert) {
        // Alert Type
        switch (alert.getType()) {
            case "1": // Strong Thunderstorms Anticipated
                type = WeatherAlertType.SEVERETHUNDERSTORMWATCH;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "2": // Severe Thunderstorms Anticipated
                type = WeatherAlertType.SEVERETHUNDERSTORMWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "3": // Tornadoes Possible
                type = WeatherAlertType.TORNADOWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "4": // Heavy Rain Anticipated
                type = WeatherAlertType.FLOODWATCH;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "5": // Floods Anticipated
            case "6": // Flash Floods Anticipated
                type = WeatherAlertType.FLOODWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "7": // High Winds Anticipated
                type = WeatherAlertType.HIGHWIND;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "8": // Heavy Snow Anticipated
            case "11": // Freezing Rain Anticipated
            case "12": // Ice Storm Anticipated
                type = WeatherAlertType.WINTERWEATHER;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "9": // Blizzard Conditions Anticipated
            case "10": // Blowing Snow Anticipated
                type = WeatherAlertType.WINTERWEATHER;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "13": // Snow Advisory
            case "14": // Winter Weather Advisory

            case "17": // Wind Chill Alert
            case "18": // Frost Advisory
            case "19": // Freeze Advisory

                type = WeatherAlertType.WINTERWEATHER;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "15": // Heat Advisory
                type = WeatherAlertType.HEAT;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "16": // Excessive Heat Alert
                type = WeatherAlertType.HEAT;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "20": // Fog Anticipated
            case "22": // Smog Anticipated
                type = WeatherAlertType.DENSEFOG;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "21": // Dense Fog Anticipated
                type = WeatherAlertType.DENSEFOG;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "30": // Tropical Cyclone Conditions Anticipated
                type = WeatherAlertType.HURRICANEWINDWARNING;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "31": // Hurricane Conditions Anticipated
                type = WeatherAlertType.HURRICANEWINDWARNING;
                severity = WeatherAlertSeverity.EXTREME;
                break;
            case "32": // Small Craft Advisory Anticipated
                type = WeatherAlertType.SMALLCRAFT;
                severity = WeatherAlertSeverity.MODERATE;
                break;
            case "33": // Gale Warning Anticipated
                type = WeatherAlertType.GALEWARNING;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "34": // High Winds Anticipated (Winds greater than 35 || 50 mph anticipated)
                type = WeatherAlertType.HIGHWIND;
                severity = WeatherAlertSeverity.SEVERE;
                break;
            case "23": // Unknown
            case "24": // Unknown
            case "25": // Unknown
            case "26": // Unknown
            case "27": // Unknown
            case "28": // Unknown
            case "29": // Unknown
            case "35": // Heavy Surf Advisory
            case "36": // Beach Erosion Advisory
            default:
                type = WeatherAlertType.SPECIALWEATHERALERT;
                severity = WeatherAlertSeverity.SEVERE;
                break;
        }

        title = alert.getDescription();
        message = alert.getDescription();

        setDateTimeFromSegment(alert.getTimeSegment());

        attribution = "HERE Weather";
    }

    // HERE NWS Alerts
    public WeatherAlert(WatchItem alert) {
        type = getAlertType(NumberUtils.tryParseInt(alert.getType(), -1), alert.getDescription());
        severity = getAlertSeverity(alert.getSeverity());

        title = alert.getDescription();
        message = alert.getMessage();

        date = ZonedDateTime.parse(alert.getValidFromTimeLocal());
        expiresDate = ZonedDateTime.parse(alert.getValidUntilTimeLocal());

        attribution = "U.S. National Weather Service";
    }

    public WeatherAlert(WarningItem alert) {
        type = getAlertType(NumberUtils.tryParseInt(alert.getType(), -1), alert.getDescription());
        severity = getAlertSeverity(alert.getSeverity());

        title = alert.getDescription();
        message = alert.getMessage();

        date = ZonedDateTime.parse(alert.getValidFromTimeLocal());
        expiresDate = ZonedDateTime.parse(alert.getValidUntilTimeLocal());

        attribution = "U.S. National Weather Service";
    }

    private WeatherAlertType getAlertType(@IntRange(from = 0, to = 38) int type, String alertDescription) {
        switch (type) {
            case 0: // Aviation Weather Warning
            case 1: // Civil Emergency Message
            case 10: // Lakeshore Warning or Statement
            case 11: // Marine Weather Statement
            case 12: // Non Precipitation Warning, Watch, or Statement
            case 13: // Public Severe Weather Alert
            case 14: // Red Flag Warning
            case 16: // River Recreation Statement
            case 17: // River Statement
            case 19: // Preliminary Notice of Watch Cancellation - Aviation Message
            case 20: // Special Dispersion Statement
            case 22: // SPC Watch Point Information Message
            case 25: // Special Marine Warning
            case 27: // Special Weather Statement
            case 38: // Air Stagnation Advisory
            default: {
                // Try to get a more detailed alert type
                if (alertDescription.contains("Hurricane".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.HURRICANEWINDWARNING;
                } else if (alertDescription.contains("Tornado".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.TORNADOWARNING;
                } else if (alertDescription.contains("Heat".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.HEAT;
                } else if (alertDescription.contains("Dense Fog".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.DENSEFOG;
                } else if (alertDescription.contains("Dense Smoke".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.DENSESMOKE;
                } else if (alertDescription.contains("Fire".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.FIRE;
                } else if (alertDescription.contains("Wind".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.HIGHWIND;
                } else if (alertDescription.contains("Snow".toLowerCase(Locale.ROOT)) || alertDescription.contains("Blizzard".toLowerCase(Locale.ROOT)) ||
                        alertDescription.contains("Winter".toLowerCase(Locale.ROOT)) || alertDescription.contains("Ice".toLowerCase(Locale.ROOT)) ||
                        alertDescription.contains("Ice".toLowerCase(Locale.ROOT)) || alertDescription.contains("Ice".toLowerCase(Locale.ROOT)) ||
                        alertDescription.contains("Avalanche".toLowerCase(Locale.ROOT)) || alertDescription.contains("Cold".toLowerCase(Locale.ROOT)) ||
                        alertDescription.contains("Freez".toLowerCase(Locale.ROOT)) || alertDescription.contains("Frost".toLowerCase(Locale.ROOT)) ||
                        alertDescription.contains("Chill".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.WINTERWEATHER;
                } else if (alertDescription.contains("Earthquake".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.EARTHQUAKEWARNING;
                } else if (alertDescription.contains("Gale".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.GALEWARNING;
                } else if (alertDescription.contains("Dust".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.DUSTADVISORY;
                } else if (alertDescription.contains("Small Craft".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.SMALLCRAFT;
                } else if (alertDescription.contains("Storm".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.STORMWARNING;
                } else if (alertDescription.contains("Tsunami".toLowerCase(Locale.ROOT))) {
                    return WeatherAlertType.TSUNAMIWARNING;
                }

                return WeatherAlertType.SPECIALWEATHERALERT;
            }
            case 2: // Coastal Flood Warning, Watch, or Statement
            case 5: // Flash Flood Warning
            case 7: // Flood Warning
            case 8: // Urban and Small Stream Flood Advisory
                return WeatherAlertType.FLOODWARNING;
            case 3: // Flash Flood Watch
            case 4: // Flash Flood Statement
            case 6: // Flood Statement
                return WeatherAlertType.FLOODWATCH;
            case 9: // Hurricane Local Statement
                return WeatherAlertType.HURRICANELOCALSTATEMENT;
            case 15: // River Ice Statement
            case 18: // Snow Avalanche Bulletin
            case 37: // Winter Weather Warning, Watch, or Advisory
                return WeatherAlertType.WINTERWEATHER;
            case 21: // Severe Local Storm Watch or Watch Cancellation
            case 23: // Severe Local Storm Watch and Areal Outline
            case 26: // Storm Strike Probability Bulletin from the TPC
                return WeatherAlertType.SEVERETHUNDERSTORMWATCH;
            case 24: // Marine Subtropical Storm Advisory
                return WeatherAlertType.STORMWARNING;
            case 28: // Severe Thunderstorm Warning
                return WeatherAlertType.SEVERETHUNDERSTORMWARNING;
            case 29: // Severe Weather Statement
                return WeatherAlertType.SEVEREWEATHER;
            case 30: // Tropical Cyclone Advisory
            case 31: // Tropical Cyclone Advisory for Marine and Aviation Interests
            case 32: // Public Tropical Cyclone Advisory
            case 33: // Tropical Cyclone Update
                return WeatherAlertType.HURRICANEWINDWARNING;
            case 34: // Tornado Warning
                return WeatherAlertType.TORNADOWARNING;
            case 35: // Tsunami Watch or Warning
                return WeatherAlertType.TSUNAMIWARNING;
            case 36: // Volcanic Activity Advisory
                return WeatherAlertType.VOLCANO;
        }
    }

    private WeatherAlertSeverity getAlertSeverity(@IntRange(from = 0, to = 100) int severity) {
        if (severity >= 75) {
            return WeatherAlertSeverity.EXTREME;
        } else if (severity >= 50) {
            return WeatherAlertSeverity.SEVERE;
        } else if (severity >= 25) {
            return WeatherAlertSeverity.MODERATE;
        } else {
            return WeatherAlertSeverity.MINOR;
        }
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
                    case "Type":
                        this.type = WeatherAlertType.valueOf(Integer.parseInt(reader.nextString()));
                        break;
                    case "Severity":
                        this.severity = WeatherAlertSeverity.valueOf(Integer.parseInt(reader.nextString()));
                        break;
                    case "Title":
                        this.title = reader.nextString();
                        break;
                    case "Message":
                        this.message = reader.nextString();
                        break;
                    case "Attribution":
                        this.attribution = reader.nextString();
                        break;
                    case "Date":
                        String json = reader.nextString();
                        ZonedDateTime result = null;
                        try {
                            result = ZonedDateTime.parse(json, DateTimeUtils.getZonedDateTimeFormatter());
                        } catch (Exception e) {
                            // If we can't parse as DateTimeOffset try DateTime (data could be old)
                            result = ZonedDateTime.parse(json);
                        }
                        this.date = result;
                        break;
                    case "ExpiresDate":
                        json = reader.nextString();
                        result = null;
                        try {
                            result = ZonedDateTime.parse(json, DateTimeUtils.getZonedDateTimeFormatter());
                        } catch (Exception e) {
                            // If we can't parse as DateTimeOffset try DateTime (data could be old)
                            result = ZonedDateTime.parse(json);
                        }
                        this.expiresDate = result;
                        break;
                    case "Notified":
                        this.notified = reader.nextBoolean();
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
            writer.value(date.format(DateTimeUtils.getZonedDateTimeFormatter()));

            // "ExpiresDate" : ""
            writer.name("ExpiresDate");
            writer.value(expiresDate.format(DateTimeUtils.getZonedDateTimeFormatter()));

            // "Notified" : ""
            writer.name("Notified");
            writer.value(notified);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "WeatherAlert: error writing json string");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        WeatherAlert that = (WeatherAlert) o;

        return type == that.type &&
                severity == that.severity &&
                title.equals(that.title) &&
                //message.equals(that.message) &&
                attribution.equals(that.attribution) &&
                //date.equals(that.date) &&
                expiresDate.equals(that.expiresDate);
    }

    @Override
    public int hashCode() {
        int hashCode = 68217818;
        hashCode = hashCode * -1521134295 + type.hashCode();
        hashCode = hashCode * -1521134295 + severity.hashCode();
        hashCode = hashCode * -1521134295 + (title != null ? title.hashCode() : 0);
        //hashCode = hashCode * -1521134295 + (message != null ? message.hashCode() : 0);
        hashCode = hashCode * -1521134295 + (attribution != null ? attribution.hashCode() : 0);
        //hashCode = hashCode * -1521134295 + (date != null ? date.hashCode() : 0);
        hashCode = hashCode * -1521134295 + (expiresDate != null ? expiresDate.hashCode() : 0);
        return hashCode;
    }
}
