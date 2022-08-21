package com.thewizrd.simpleweather.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.thewizrd.common.controls.HourlyForecastItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.StringUtils
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString

@Composable
fun WeatherHourlyForecastPanel(
    model: HourlyForecastItemViewModel
) {
    val ctx = LocalContext.current
    val defaultIconProvider =
        sharedDeps.weatherIconsManager.getIconProvider(WeatherIconsEFProvider.KEY)

    val weatherIconDrawable = remember {
        ContextCompat.getDrawable(
            ctx,
            sharedDeps.weatherIconsManager.getWeatherIconResource(model.weatherIcon)
        )
    }
    val popData = remember {
        model.extras?.get(WeatherDetailsType.POPCHANCE)
    }
    val popDrawable = popData?.let {
        ContextCompat.getDrawable(ctx, defaultIconProvider.getWeatherIconResource(it.icon))
    }
    val windData = remember {
        model.extras?.get(WeatherDetailsType.WINDSPEED)
    }
    val windDrawable = windData?.let {
        ContextCompat.getDrawable(ctx, defaultIconProvider.getWeatherIconResource(it.icon))
    }
    val annotatedDateStr = remember(model.forecast) {
        val is24hr = DateFormat.is24HourFormat(ctx)
        val dayOfWeek =
            model.forecast.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK))
        val time: String
        val timeSuffix: String

        if (is24hr) {
            time = model.forecast.date.format(
                DateTimeUtils.ofPatternForUserLocale(
                    DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_24HR)
                )
            )
            timeSuffix = ""
        } else {
            time = model.forecast.date.format(DateTimeUtils.ofPatternForUserLocale("h"))
            timeSuffix = model.forecast.date.format(DateTimeUtils.ofPatternForUserLocale("a"))
        }

        buildAnnotatedString {
            append(time)

            pushStyle(SpanStyle(fontSize = 0.8.em))
            append(timeSuffix)
            pop()

            append(StringUtils.lineSeparator())
            pushStyle(
                SpanStyle(
                    fontSize = 0.8.em,
                    color = Color(0xB3FFFFFF)
                )
            )
            append(dayOfWeek)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.list_item_padding),
                end = dimensionResource(R.dimen.list_item_padding),
                top = 4.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                textAlign = TextAlign.Center,
                text = annotatedDateStr,
                style = MaterialTheme.typography.body1
            )
            Image(
                modifier = Modifier.size(
                    width = 32.dp, height = 36.dp
                ),
                painter = rememberDrawablePainter(weatherIconDrawable),
                contentDescription = null
            )
            Text(
                modifier = Modifier.weight(1f),
                text = model.hiTemp ?: WeatherIcons.PLACEHOLDER,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (popData != null) {
                Row {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 2.dp),
                        painter = rememberDrawablePainter(drawable = popDrawable),
                        tint = colorResource(R.color.colorPrimaryLight),
                        contentDescription = null
                    )
                    Text(
                        text = spannableStringToAnnotatedString(popData.value),
                        style = MaterialTheme.typography.body1.copy(fontSize = 14.sp),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = colorResource(R.color.colorPrimaryLight)
                    )
                }
            }
            if (popData != null && windData != null) {
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (windData != null) {
                val windSpeed = remember(windData.value) {
                    if (!windData.value.isNullOrEmpty()) {
                        windData.value.split(",")[0]
                    } else {
                        ""
                    }
                }

                Row {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 2.dp)
                            .rotate(windData.iconRotation.toFloat()),
                        painter = rememberDrawablePainter(drawable = windDrawable),
                        tint = Color(0xFF20B2AA),
                        contentDescription = null
                    )
                    Text(
                        text = spannableStringToAnnotatedString(windSpeed),
                        style = MaterialTheme.typography.body1.copy(fontSize = 14.sp),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = Color(0xFF20B2AA)
                    )
                }
            }
        }
    }
}