package com.thewizrd.shared_resources.weatherdata;

import android.content.Context;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class UV {

    @SerializedName("index")
    private float index = -1;

    @SerializedName("desc")
    private String desc;

    private UV() {
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

    public float getIndex() {
        return index;
    }

    public void setIndex(float index) {
        this.index = index;
    }

    public String getDescription() {
        return desc;
    }

    public void setDescription(String description) {
        this.desc = description;
    }

    public static UV fromJson(JsonReader extReader) {
        UV obj = null;

        try {
            obj = new UV();
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
                        obj.index = (float) reader.nextDouble();
                        break;
                    case "desc":
                        obj.desc = reader.nextString();
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

        return sw.toString();
    }
}
