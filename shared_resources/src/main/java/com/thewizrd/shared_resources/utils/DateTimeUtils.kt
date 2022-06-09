package com.thewizrd.shared_resources.utils

import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.DateTimePatternGenerator
import com.thewizrd.shared_resources.utils.LocaleUtils.getLocale
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.math.absoluteValue

object DateTimeUtils {
    const val ZONED_DATETIME_FORMAT = "dd.MM.yyyy HH:mm:ss ZZZZZ"

    @JvmStatic
    fun maxTimeOfDay(): Duration {
        LocalTime.MIN
        return Duration.ofHours(23).plusMinutes(59).plusSeconds(59).plusNanos(999_999_999)
    }

    @JvmStatic
    fun getClosestWeekday(dayOfWeek: DayOfWeek): LocalDateTime {
        val today = LocalDate.now(ZoneOffset.UTC).atStartOfDay()
        val nextWeekday = getNextWeekday(today, dayOfWeek)
        val prevWeekday = getPrevWeekday(today, dayOfWeek)
        return if (Duration.between(nextWeekday, today).abs().toMillis() < Duration.between(
                today,
                prevWeekday
            ).abs().toMillis()
        ) {
            nextWeekday
        } else {
            prevWeekday
        }
    }

    @JvmStatic
    fun getNextWeekday(start: LocalDateTime, dayOfWeek: DayOfWeek): LocalDateTime {
        return start.with(TemporalAdjusters.nextOrSame(dayOfWeek))
    }

    @JvmStatic
    fun getPrevWeekday(start: LocalDateTime, dayOfWeek: DayOfWeek): LocalDateTime {
        return start.with(TemporalAdjusters.previousOrSame(dayOfWeek))
    }

    @JvmStatic
    fun offsetToHMSFormat(offset: ZoneOffset): String {
        val seconds = offset.totalSeconds
        val absSeconds = seconds.absoluteValue.toLong()
        val hmsString = String.format(
            Locale.ROOT,
            "%02d:%02d:%02d",
            absSeconds / 3600,
            (absSeconds % 3600) / 60,
            (absSeconds % 60)
        )

        return if (seconds < 0) "-$hmsString" else "+$hmsString"
    }

    @JvmStatic
    fun durationToHMFormat(duration: Duration): String {
        val seconds = duration.seconds
        val absSeconds = seconds.absoluteValue.toLong()
        val hmString = String.format(
            Locale.ROOT,
            "%02d:%02d",
            absSeconds / 3600,
            (absSeconds % 3600) / 60
        )

        return if (seconds < 0) "-$hmString" else "+$hmString"
    }

    const val LOCAL_DATE_TIME_MIN = "1/1/1900 12:00:00 AM"
    val LOCAL_DATE_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.ROOT)

    @JvmStatic
    val LOCALDATETIME_MIN: LocalDateTime by lazy {
        LocalDateTime.parse(LOCAL_DATE_TIME_MIN, LOCAL_DATE_TIME_FORMATTER)
    }

    @JvmStatic
    fun getZonedDateTimeFormatter(): DateTimeFormatter {
        return DateTimeFormatter.ofPattern(ZONED_DATETIME_FORMAT, Locale.ROOT)
    }

    @JvmStatic
    fun formatDateTimeForSkeleton(instant: Instant, skeleton: String): String {
        return formatDateTimeForSkeleton(instant, skeleton, getLocale())
    }

    @JvmStatic
    fun formatDateTimeForSkeleton(instant: Instant, skeleton: String, locale: Locale): String {
        val date = Date.from(instant)
        return DateFormat.getInstanceForSkeleton(skeleton, locale).format(date)
    }

    @JvmStatic
    fun getBestPatternForSkeleton(skeleton: String): String {
        val generator = DateTimePatternGenerator.getInstance(getLocale())
        return generator.getBestPattern(skeleton)
    }

    @JvmStatic
    fun getBestPatternForSkeleton(skeleton: String, locale: Locale): String {
        val generator = DateTimePatternGenerator.getInstance(locale)
        return generator.getBestPattern(skeleton)
    }

    @JvmStatic
    fun ofPatternForUserLocale(pattern: String): DateTimeFormatter {
        return DateTimeFormatter.ofPattern(pattern, getLocale())
    }

    @JvmStatic
    fun ofPatternForInvariantLocale(pattern: String): DateTimeFormatter {
        return DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
    }
}