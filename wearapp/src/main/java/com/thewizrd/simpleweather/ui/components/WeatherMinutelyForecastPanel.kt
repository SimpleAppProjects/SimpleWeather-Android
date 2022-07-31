package com.thewizrd.simpleweather.ui.components

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.viewmodels.MinutelyForecastViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun WeatherMinutelyForecastPanel(
    model: MinutelyForecastViewModel
) {
    val ctx = LocalContext.current
    val weatherIconDrawable = remember {
        ContextCompat.getDrawable(
            ctx,
            sharedDeps.weatherIconsManager.getWeatherIconResource(WeatherIcons.RAINDROP)
        )
    }

    WeatherMinutelyForecastPanel(
        date = model.date,
        rainAmount = model.rainAmount,
        raindropDrawable = weatherIconDrawable
    )
}

@Composable
private fun WeatherMinutelyForecastPanel(
    date: String? = null,
    rainAmount: String? = null,
    raindropDrawable: Drawable? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(
                vertical = 4.dp,
                horizontal = dimensionResource(R.dimen.list_item_padding),
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            textAlign = TextAlign.Center,
            text = date ?: WeatherIcons.EM_DASH,
            style = MaterialTheme.typography.body1
        )
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .weight(1f, false),
                textAlign = TextAlign.Center,
                text = rainAmount ?: WeatherIcons.PLACEHOLDER,
                style = MaterialTheme.typography.body1
            )
            Icon(
                modifier = Modifier
                    .weight(1f, false)
                    .size(20.dp),
                painter = rememberDrawablePainter(raindropDrawable),
                contentDescription = null,
                tint = colorResource(id = R.color.colorSecondaryDark)
            )
        }
    }
}

@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_LARGE_ROUND,
    widthDp = 360,
    heightDp = 360,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_SQUARE,
    widthDp = 360,
    heightDp = 360,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Preview(
    apiLevel = 26,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = Devices.WEAR_OS_SMALL_ROUND,
    widthDp = 320,
    heightDp = 320,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun PreviewWeatherMinutelyForecastPanel() {
    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        val ctx = LocalContext.current
        val weatherIconDrawable = remember {
            ContextCompat.getDrawable(ctx, R.drawable.wi_raindrop)
        }

        val fmt = DateTimeFormatter.ofPattern("h:mm a")

        WeatherMinutelyForecastPanel(
            date = ZonedDateTime.now().format(fmt),
            rainAmount = "1.00 mm",
            weatherIconDrawable
        )
    }
}