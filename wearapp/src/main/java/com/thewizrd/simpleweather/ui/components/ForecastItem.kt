package com.thewizrd.simpleweather.ui.components

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.thewizrd.common.controls.ForecastItemViewModel
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.StringUtils.removeDigitChars
import com.thewizrd.simpleweather.R

@Composable
fun ForecastItem(
    modifier: Modifier = Modifier,
    model: ForecastItemViewModel
) {
    val ctx = LocalContext.current
    val weatherIconDrawable = remember {
        ContextCompat.getDrawable(
            ctx,
            sharedDeps.weatherIconsManager.getWeatherIconResource(model.weatherIcon)
        )
    }

    ForecastItem(
        modifier = modifier,
        date = model.date.removeDigitChars(),
        weatherIconDrawable = weatherIconDrawable,
        hiTemp = model.hiTemp,
        loTemp = model.loTemp
    )
}

@Composable
fun ForecastItem(
    modifier: Modifier = Modifier,
    date: String,
    weatherIconDrawable: Drawable?,
    hiTemp: String,
    loTemp: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(2.dp),
            maxLines = 1,
            text = date,
            style = MaterialTheme.typography.body1
        )
        Icon(
            modifier = Modifier
                .padding(2.dp)
                .size(36.dp),
            painter = rememberDrawablePainter(weatherIconDrawable),
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(2.dp),
            maxLines = 1,
            text = hiTemp,
            style = MaterialTheme.typography.body1
        )
        Text(
            modifier = Modifier.padding(2.dp),
            maxLines = 1,
            text = loTemp,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurfaceVariant
        )
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
fun PreviewForecastItem() {
    Box(
        modifier = Modifier.fillMaxRectangle(),
        contentAlignment = Alignment.Center
    ) {
        val ctx = LocalContext.current
        val weatherIconDrawable = remember {
            ContextCompat.getDrawable(
                ctx,
                R.drawable.wi_day_cloudy
            )
        }

        ForecastItem(
            date = "Fri",
            weatherIconDrawable = weatherIconDrawable,
            hiTemp = "83°",
            loTemp = "64°"
        )
    }
}