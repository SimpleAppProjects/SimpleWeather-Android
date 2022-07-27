package com.thewizrd.shared_resources.weatherdata.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

import okio.Buffer;

public class AirQuality extends CustomJsonObject {

    @Json(name = "index")
    private Integer index;

    @Json(name = "attribution")
    private String attribution;

    @Json(name = "no2")
    private Integer no2;

    @Json(name = "o3")
    private Integer o3;

    @Json(name = "so2")
    private Integer so2;

    @Json(name = "pm25")
    private Integer pm25;

    @Json(name = "pm10")
    private Integer pm10;

    @Json(name = "co")
    private Integer co;

    @Json(name = "date")
    private LocalDate date;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public AirQuality() {
        // Needed for deserialization
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public Integer getNo2() {
        return no2;
    }

    public void setNo2(Integer no2) {
        this.no2 = no2;
    }

    public Integer getO3() {
        return o3;
    }

    public void setO3(Integer o3) {
        this.o3 = o3;
    }

    public Integer getSo2() {
        return so2;
    }

    public void setSo2(Integer so2) {
        this.so2 = so2;
    }

    public Integer getPm25() {
        return pm25;
    }

    public void setPm25(Integer pm25) {
        this.pm25 = pm25;
    }

    public Integer getPm10() {
        return pm10;
    }

    public void setPm10(Integer pm10) {
        this.pm10 = pm10;
    }

    public Integer getCo() {
        return co;
    }

    public void setCo(Integer co) {
        this.co = co;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AirQuality that = (AirQuality) o;
        return Objects.equals(index, that.index) &&
                Objects.equals(attribution, that.attribution) &&
                Objects.equals(no2, that.no2) &&
                Objects.equals(o3, that.o3) &&
                Objects.equals(so2, that.so2) &&
                Objects.equals(pm25, that.pm25) &&
                Objects.equals(pm10, that.pm10) &&
                Objects.equals(co, that.co) &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                index,
                attribution,
                no2,
                o3,
                so2,
                pm25,
                pm10,
                co,
                date
        );
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
                    case "index":
                        this.index = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "attribution":
                        this.attribution = reader.nextString();
                        break;
                    case "no2":
                        this.no2 = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "o3":
                        this.o3 = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "so2":
                        this.so2 = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "pm25":
                        this.pm25 = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "pm10":
                        this.pm10 = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "co":
                        this.co = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "date":
                        this.date = LocalDate.parse(reader.nextString());
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

            // "index" : ""
            writer.name("index");
            writer.value(index);

            // "attribution" : ""
            writer.name("attribution");
            writer.value(attribution);

            // "no2" : ""
            writer.name("no2");
            writer.value(no2);

            // "o3" : ""
            writer.name("o3");
            writer.value(o3);

            // "so2" : ""
            writer.name("so2");
            writer.value(so2);

            // "pm25" : ""
            writer.name("pm25");
            writer.value(pm25);

            // "pm10" : ""
            writer.name("pm10");
            writer.value(pm10);

            // "co" : ""
            writer.name("co");
            writer.value(co);

            if (date != null) {
                // "date" : ""
                writer.name("date");
                writer.value(date.toString());
            }

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "AirQuality: error writing json string");
        }
    }
}