package com.thewizrd.shared_resources.database;

import androidx.room.TypeConverter;

import com.thewizrd.shared_resources.utils.DateTimeUtils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

public class SortableDateTimeConverters {
    private static final DateTimeFormatter zDTF = DateTimeUtils.ofPatternForInvariantLocale("yyyy-MM-dd HH:mm:ss ZZZZZ");
    private static final DateTimeFormatter lDTF = DateTimeFormatter.ISO_INSTANT.withLocale(Locale.ROOT);

    @TypeConverter
    public static ZonedDateTime zonedDateTimeFromString(String value) {
        return value == null ? null : ZonedDateTime.parse(value, zDTF);
    }

    @TypeConverter
    public static String zonedDateTimetoString(ZonedDateTime value) {
        return value == null ? null : value.format(zDTF);
    }

    @TypeConverter
    public static LocalDateTime localDateTimeFromString(String value) {
        return value == null ? null : LocalDateTime.ofInstant(Instant.from(lDTF.parse(value)), ZoneOffset.UTC);
    }

    @TypeConverter
    public static String localDateTimetoString(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
    }
}
