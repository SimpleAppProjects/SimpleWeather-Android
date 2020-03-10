package com.thewizrd.shared_resources.weatherdata;

import android.content.Context;
import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.CustomJsonObject;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.IOException;
import java.io.StringReader;

public class Beaufort extends CustomJsonObject {

    public enum BeaufortScale {
        B0,
        B1,
        B2,
        B3,
        B4,
        B5,
        B6,
        B7,
        B8,
        B9,
        B10,
        B11,
        B12;

        public static BeaufortScale valueOf(int value) {
            switch (value) {
                case 0:
                    return BeaufortScale.B0;
                case 1:
                    return BeaufortScale.B1;
                case 2:
                    return BeaufortScale.B2;
                case 3:
                    return BeaufortScale.B3;
                case 4:
                    return BeaufortScale.B4;
                case 5:
                    return BeaufortScale.B5;
                case 6:
                    return BeaufortScale.B6;
                case 7:
                    return BeaufortScale.B7;
                case 8:
                    return BeaufortScale.B8;
                case 9:
                    return BeaufortScale.B9;
                case 10:
                    return BeaufortScale.B10;
                case 11:
                    return BeaufortScale.B11;
                case 12:
                    return BeaufortScale.B12;
                default:
                    return null;
            }
        }
    }

    @SerializedName("scale")
    private BeaufortScale scale;

    @SerializedName("desc")
    private String desc;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Beaufort() {
        // Needed for deserialization
    }

    public Beaufort(int beaufortScale) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        switch (beaufortScale) {
            case 0:
                scale = BeaufortScale.B0;
                desc = context.getString(R.string.beaufort_0);
                break;
            case 1:
                scale = BeaufortScale.B1;
                desc = context.getString(R.string.beaufort_1);
                break;
            case 2:
                scale = BeaufortScale.B2;
                desc = context.getString(R.string.beaufort_2);
                break;
            case 3:
                scale = BeaufortScale.B3;
                desc = context.getString(R.string.beaufort_3);
                break;
            case 4:
                scale = BeaufortScale.B4;
                desc = context.getString(R.string.beaufort_4);
                break;
            case 5:
                scale = BeaufortScale.B5;
                desc = context.getString(R.string.beaufort_5);
                break;
            case 6:
                scale = BeaufortScale.B6;
                desc = context.getString(R.string.beaufort_6);
                break;
            case 7:
                scale = BeaufortScale.B7;
                desc = context.getString(R.string.beaufort_7);
                break;
            case 8:
                scale = BeaufortScale.B8;
                desc = context.getString(R.string.beaufort_8);
                break;
            case 9:
                scale = BeaufortScale.B9;
                desc = context.getString(R.string.beaufort_9);
                break;
            case 10:
                scale = BeaufortScale.B10;
                desc = context.getString(R.string.beaufort_10);
                break;
            case 11:
                scale = BeaufortScale.B11;
                desc = context.getString(R.string.beaufort_11);
                break;
            case 12:
                scale = BeaufortScale.B12;
                desc = context.getString(R.string.beaufort_12);
                break;
        }
    }

    public Beaufort(int beaufortScale, String description) {
        this(beaufortScale);

        if (!StringUtils.isNullOrWhitespace(description))
            this.desc = description;
    }

    public BeaufortScale getScale() {
        return scale;
    }

    public void setScale(BeaufortScale scale) {
        this.scale = scale;
    }

    public String getDescription() {
        return desc;
    }

    public void setDescription(String description) {
        this.desc = description;
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
                    case "scale":
                        this.scale = BeaufortScale.valueOf(reader.nextInt());
                        break;
                    case "desc":
                        this.desc = reader.nextString();
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

            // "scale" : ""
            writer.name("scale");
            writer.value(scale.ordinal());

            // "desc" : ""
            writer.name("desc");
            writer.value(desc);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "Beaufort: error writing json string");
        }
    }
}
