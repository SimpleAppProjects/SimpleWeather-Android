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
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.IOException;
import java.io.StringReader;

public class UV extends CustomJsonObject {

    @SerializedName("index")
    private Float index;

    @SerializedName("desc")
    private String desc;

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public UV() {
        // Needed for deserialization
    }

    public UV(float index) {
        this.index = index;

        Context context = SimpleLibrary.getInstance().getAppContext();

        if (index >= 0 && index < 3) {
            desc = context.getString(R.string.uv_0);
        } else if (index >= 3 && index < 6) {
            desc = context.getString(R.string.uv_3);
        } else if (index >= 6 && index < 8) {
            desc = context.getString(R.string.uv_6);
        } else if (index >= 8 && index < 11) {
            desc = context.getString(R.string.uv_8);
        } else if (index >= 11) {
            desc = context.getString(R.string.uv_11);
        }
    }

    public UV(float index, String description) {
        this(index);

        if (!StringUtils.isNullOrWhitespace(description))
            this.desc = description;
    }

    public Float getIndex() {
        return index;
    }

    public void setIndex(Float index) {
        this.index = index;
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
                    case "index":
                        this.index = NumberUtils.tryParseFloat(reader.nextString());
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

            // "index" : ""
            writer.name("index");
            writer.value(index);

            // "desc" : ""
            writer.name("desc");
            writer.value(desc);

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "UV: error writing json string");
        }
    }
}
