package com.thewizrd.shared_resources.weatherdata.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Objects;

import okio.Buffer;

public class WeatherAlert extends CustomJsonObject {
    @Json(name = "Type")
    private WeatherAlertType type = WeatherAlertType.SPECIALWEATHERALERT;
    @Json(name = "Severity")
    private WeatherAlertSeverity severity = WeatherAlertSeverity.UNKNOWN;
    @Json(name = "Title")
    private String title;
    @Json(name = "Message")
    private String message;
    @Json(name = "Attribution")
    private String attribution;
    @Json(name = "Date")
    private ZonedDateTime date;
    @Json(name = "ExpiresDate")
    private ZonedDateTime expiresDate;
    @Json(name = "Notified")
    private boolean notified;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    public WeatherAlert() {

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
    public void fromJson(@NonNull JsonReader extReader) {
        try {
            JsonReader reader;
            String jsonValue;

            if (extReader.peek() == JsonReader.Token.STRING) {
                jsonValue = extReader.nextString();
            } else {
                jsonValue = null;
            }

            if (jsonValue == null)
                reader = extReader;
            else {
                reader = JsonReader.of(new Buffer().writeUtf8(jsonValue));
                reader.beginObject(); // StartObject
            }

            while (reader.hasNext() && reader.peek() != JsonReader.Token.END_OBJECT) {
                if (reader.peek() == JsonReader.Token.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonReader.Token.NULL) {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeatherAlert that = (WeatherAlert) o;

        return type == that.type &&
                severity == that.severity &&
                Objects.equals(title, that.title) &&
                //Objects.equals(message, that.message) &&
                Objects.equals(attribution, that.attribution) &&
                //Objects.equals(date, that.date) &&
                Objects.equals(expiresDate, that.expiresDate);
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
