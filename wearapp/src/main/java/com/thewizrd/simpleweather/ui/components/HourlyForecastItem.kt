package com.thewizrd.simpleweather.ui.components

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.thewizrd.common.controls.HourlyForecastItemViewModel
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.DateTimeUtils
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HourlyForecastItem(
    model: HourlyForecastItemViewModel
) {
    val ctx = LocalContext.current
    val dateStr = remember(ctx, model.forecast.date) {
        buildAnnotatedStringDate(model.forecast.date, DateFormat.is24HourFormat(ctx))
    }

    HourlyForecastItem(
        date = dateStr,
        weatherIcon = model.weatherIcon,
        hiTemp = model.hiTemp
    )
}

@Composable
fun HourlyForecastItem(
    date: AnnotatedString,
    weatherIcon: String?,
    hiTemp: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(4.dp)
                .weight(1f),
            maxLines = 1,
            text = date,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
        WeatherIcon(
            modifier = Modifier
                .padding(2.dp)
                .size(36.dp)
                .weight(1f),
            weatherIcon = weatherIcon,
            shouldAnimate = true
        )
        Text(
            modifier = Modifier
                .padding(4.dp)
                .weight(1f),
            maxLines = 1,
            text = hiTemp,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
    }
}

private fun buildAnnotatedStringDate(
    date: ZonedDateTime,
    is24Hour: Boolean = false
): AnnotatedString {
    return buildAnnotatedString {
        if (is24Hour) {
            append(
                date.format(
                    DateTimeUtils.ofPatternForUserLocale(
                        DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_24HR)
                    )
                )
            )
        } else {
            append(
                date.format(DateTimeUtils.ofPatternForUserLocale("h"))
            )
            pushStyle(SpanStyle(fontSize = 0.8.em))
            append(
                date.format(DateTimeUtils.ofPatternForUserLocale("a"))
            )
        }
    }
}

private fun buildAnnotatedStringDatePreview(
    date: ZonedDateTime,
    is24Hour: Boolean = false
): AnnotatedString {
    return buildAnnotatedString {
        if (is24Hour) {
            append(
                date.format(DateTimeFormatter.ofPattern("HH:mm"))
            )
        } else {
            append(
                date.format(DateTimeFormatter.ofPattern("h"))
            )
            pushStyle(SpanStyle(fontSize = 0.8.em))
            append(
                date.format(DateTimeFormatter.ofPattern("a"))
            )
        }
    }
}

@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_LARGE_ROUND,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_SQUARE,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_SMALL_ROUND,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun PreviewHourlyForecastItem() {
    Box(
        modifier = Modifier.fillMaxRectangle(),
        contentAlignment = Alignment.Center
    ) {
        val ctx = LocalContext.current
        val dateStr = remember(ctx) {
            buildAnnotatedStringDatePreview(ZonedDateTime.now())
        }

        HourlyForecastItem(
            date = dateStr,
            weatherIcon = WeatherIcons.DAY_CLOUDY,
            hiTemp = "82°"
        )
    }
}