package com.thewizrd.simpleweather.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.viewmodels.MinutelyForecastViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.thewizrd.shared_resources.R as sharedRes

@Composable
fun WeatherMinutelyForecastPanel(
    model: MinutelyForecastViewModel
) {
    WeatherMinutelyForecastPanel(
        date = model.date,
        precipAmount = model.precipAmount,
        isSnow = model.isSnow
    )
}

@Composable
private fun WeatherMinutelyForecastPanel(
    date: String? = null,
    precipAmount: String? = null,
    isSnow: Boolean = false
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
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = date ?: WeatherIcons.EM_DASH,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 20.dp),
                textAlign = TextAlign.Center,
                text = precipAmount ?: WeatherIcons.PLACEHOLDER,
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                modifier = Modifier.size(20.dp),
                painter = if (isSnow) {
                    painterResource(sharedRes.drawable.wi_snowflake_cold)
                } else {
                    painterResource(sharedRes.drawable.wi_raindrop)
                },
                contentDescription = null,
                tint = if (isSnow) {
                    colorResource(id = sharedRes.color.colorSecondaryLight)
                } else {
                    colorResource(id = sharedRes.color.colorSecondaryDark)
                }
            )
        }
    }
}

@Preview(
    apiLevel = 34,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = WearDevices.LARGE_ROUND,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Preview(
    apiLevel = 34,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = WearDevices.SQUARE,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Preview(
    apiLevel = 34,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = WearDevices.SMALL_ROUND,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Preview(
    apiLevel = 34,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
    showSystemUi = true,
    device = WearDevices.RECT,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun PreviewWeatherMinutelyForecastPanel() {
    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        val fmt = remember {
            DateTimeFormatter.ofPattern("h:mm a")
        }

        WeatherMinutelyForecastPanel(
            date = ZonedDateTime.now().format(fmt),
            precipAmount = "1.00 mm"
        )
    }
}