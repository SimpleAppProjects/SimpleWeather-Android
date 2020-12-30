package com.thewizrd.shared_resources.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ZonedDateTimeConverter extends TypeAdapter<ZonedDateTime> {
    private static final DateTimeFormatter formatter = DateTimeUtils.getZonedDateTimeFormatter();

    @Override
    public ZonedDateTime read(JsonReader in) throws IOException {
        return ZonedDateTime.parse(in.nextString(), formatter);
    }

    @Override
    public void write(JsonWriter out, ZonedDateTime value) throws IOException {
        out.value(formatter.format(value));
    }
}
