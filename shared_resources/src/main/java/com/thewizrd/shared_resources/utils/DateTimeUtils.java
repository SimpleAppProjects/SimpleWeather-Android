package com.thewizrd.shared_resources.utils;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Duration;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.TemporalAdjusters;

import java.util.Locale;

public class DateTimeUtils {
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

    public static LocalDateTime getNextWeekday(LocalDateTime start, DayOfWeek dayOfWeek) {
        return start.with(TemporalAdjusters.nextOrSame(dayOfWeek));
    }

    public static LocalDateTime getPrevWeekday(LocalDateTime start, DayOfWeek dayOfWeek) {
        return start.with(TemporalAdjusters.previousOrSame(dayOfWeek));
    }

    public static String offsetToHMSFormat(ZoneOffset offset) {
        int seconds = offset.getTotalSeconds();
        long absSeconds = Math.abs(seconds);
        String hmsString = String.format(Locale.ROOT,
                "%02d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + hmsString : "+" + hmsString;
    }

    public static String durationToHMFormat(Duration duration) {
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
                DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a"));
    }
}
