package com.thewizrd.shared_resources.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;

public class LocalDateTimeConverter extends TypeAdapter<LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        return LocalDateTime.ofInstant(Instant.from(formatter.parse(in.nextString())), ZoneOffset.UTC);
    }

    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        out.value(value.toInstant(ZoneOffset.UTC).toString());
    }
}
