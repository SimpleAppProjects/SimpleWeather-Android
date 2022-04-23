package com.thewizrd.common.controls

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils.getLocale
import com.thewizrd.shared_resources.utils.getColorFromAlertSeverity
import com.thewizrd.shared_resources.utils.getDrawableFromAlertType
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.floor

class WeatherAlertViewModel(weatherAlert: WeatherAlert) {
    var alertType: WeatherAlertType
    var alertSeverity: WeatherAlertSeverity
    var title: String
    var message: String
    var postDate: String
    var expireDate: String
    var attribution: String?

    init {
        val context = sharedDeps.context

        alertType = weatherAlert.type
        alertSeverity = weatherAlert.severity
        title = weatherAlert.title
        message = weatherAlert.message

        val sincePost = Duration.between(ZonedDateTime.now(ZoneOffset.UTC), weatherAlert.date).abs()

        postDate = when {
            sincePost.toDays() >= 1 -> {
                context.getString(
                    R.string.datetime_day_ago,
                    floor(sincePost.toDays().toDouble()).toInt()
                )
            }
            sincePost.toHours() >= 1 -> {
                context.getString(
                    R.string.datetime_hr_ago,
                    floor(sincePost.toHours().toDouble()).toInt()
                )
            }
            sincePost.toMinutes() >= 1 -> {
                context.getString(
                    R.string.datetime_min_ago,
                    floor(sincePost.toMinutes().toDouble()).toInt()
                )
            }
            else -> {
                context.getString(
                    R.string.datetime_sec_ago,
                    floor(sincePost.seconds.toDouble()).toInt()
                )
            }
        }

        // Displays Thursday, April 10, 2008 6:30 AM
        expireDate = String.format(
            "%s %s %s",
            context.getString(R.string.datetime_validuntil),
            weatherAlert.expiresDate.format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
                    .withLocale(getLocale())
            ),
            weatherAlert.expiresDate.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.TIMEZONE_NAME))
        )

        attribution = weatherAlert.attribution

        if (attribution != null) {
            attribution =
                String.format("%s %s", context.getString(R.string.credit_prefix), attribution)
        }
    }

    val alertBodyMessage: String
        get() = String.format("%s\n\n%s\n\n%s", expireDate, message, attribution)

    @get:ColorInt
    val alertSeverityColor: Int
        get() = alertSeverity.getColorFromAlertSeverity()

    @get:DrawableRes
    val alertDrawable: Int
        get() = alertType.getDrawableFromAlertType()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeatherAlertViewModel

        if (alertType != other.alertType) return false
        if (alertSeverity != other.alertSeverity) return false
        if (title != other.title) return false
        if (message != other.message) return false
        if (postDate != other.postDate) return false
        if (expireDate != other.expireDate) return false
        if (attribution != other.attribution) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alertType.hashCode()
        result = 31 * result + alertSeverity.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + postDate.hashCode()
        result = 31 * result + expireDate.hashCode()
        result = 31 * result + (attribution?.hashCode() ?: 0)
        return result
    }
}