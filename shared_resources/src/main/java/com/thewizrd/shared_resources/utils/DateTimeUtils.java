package com.thewizrd.shared_resources.utils;

import androidx.annotation.NonNull;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    public static final String ZONED_DATETIME_FORMAT = "dd.MM.yyyy HH:mm:ss ZZZZZ";

    public static Duration maxTimeOfDay() {
        return Duration.ofHours(23).plusMinutes(59).plusSeconds(59).plusMillis(999);
    }

    public static LocalDateTime getClosestWeekday(DayOfWeek dayOfWeek) {
        LocalDateTime today = LocalDate.now(ZoneOffset.UTC).atStartOfDay();

        LocalDateTime nextWeekday = getNextWeekday(today, dayOfWeek);
        LocalDateTime prevWeekday = getPrevWeekday(today, dayOfWeek);

        if (Duration.between(nextWeekday, today).abs().toMillis() < Duration.between(today, prevWeekday).abs().toMillis())
            return nextWeekday;
        else
            return prevWeekday;
    }

    public static LocalDateTime getNextWeekday(@NonNull LocalDateTime start, DayOfWeek dayOfWeek) {
        return start.with(TemporalAdjusters.nextOrSame(dayOfWeek));
    }

    public static LocalDateTime getPrevWeekday(@NonNull LocalDateTime start, DayOfWeek dayOfWeek) {
        return start.with(TemporalAdjusters.previousOrSame(dayOfWeek));
    }

    @NonNull
    public static String offsetToHMSFormat(@NonNull ZoneOffset offset) {
        int seconds = offset.getTotalSeconds();
        long absSeconds = Math.abs(seconds);
        String hmsString = String.format(Locale.ROOT,
                "%02d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + hmsString : "+" + hmsString;
    }

    @NonNull
    public static String durationToHMFormat(@NonNull Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String hmString = String.format(Locale.ROOT,
                "%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60);
        return seconds < 0 ? "-" + hmString : "+" + hmString;
    }

    public static LocalDateTime getLocalDateTimeMIN() {
        return LocalDateTime.parse("1/1/1900 12:00:00 AM",
                DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.ROOT));
    }

    public static DateTimeFormatter getZonedDateTimeFormatter() {
        return DateTimeFormatter.ofPattern(ZONED_DATETIME_FORMAT, Locale.ROOT);
    }

    @NonNull
    public static String formatDateTimeForSkeleton(@NonNull Instant instant, @NonNull String skeleton) {
        return formatDateTimeForSkeleton(instant, skeleton, LocaleUtils.getLocale());
    }

    @NonNull
    public static String formatDateTimeForSkeleton(@NonNull Instant instant, @NonNull String skeleton, @NonNull Locale locale) {
        Date date = Date.from(instant);
        return DateFormat.getInstanceForSkeleton(skeleton, locale).format(date);
    }

    public static String getBestPatternForSkeleton(@NonNull String skeleton) {
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(LocaleUtils.getLocale());
        return generator.getBestPattern(skeleton);
    }

    public static String getBestPatternForSkeleton(@NonNull String skeleton, @NonNull Locale locale) {
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        return generator.getBestPattern(skeleton);
    }

    public static DateTimeFormatter ofPatternForUserLocale(@NonNull String pattern) {
        return DateTimeFormatter.ofPattern(pattern, LocaleUtils.getLocale());
    }

    public static DateTimeFormatter ofPatternForInvariantLocale(@NonNull String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.ROOT);
    }
}
