package com.thewizrd.simpleweather.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.common.controls.ForecastItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.designer.initializeDependencies
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString

@Composable
fun WeatherForecastPanel(
    model: ForecastItemViewModel
) {
    val context = LocalContext.current
    val isLargeHeight = LocalConfiguration.current.screenHeightDp >= 225
    val isLargeWidth = LocalConfiguration.current.screenWidthDp >= 225

    val popData = remember(model.extras) {
        model.extras?.get(WeatherDetailsType.POPCHANCE)
    }
    val windData = remember(model.extras) {
        model.extras?.get(WeatherDetailsType.WINDSPEED)
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
                text = model.date ?: WeatherIcons.EM_DASH,
                style = MaterialTheme.typography.bodyLarge
            )
            WeatherIcon(
                modifier = Modifier.size(
                    width = 32.dp, height = 36.dp
                ),
                weatherIcon = model.weatherIcon,
                shouldAnimate = true
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = model.hiTemp ?: WeatherIcons.PLACEHOLDER,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(id = sharedRes.drawable.wi_direction_up),
                        tint = Color(0xFFFF4500),
                        contentDescription = null
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = model.loTemp ?: WeatherIcons.PLACEHOLDER,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(id = sharedRes.drawable.wi_direction_down),
                        tint = Color(0xFF87CEFA),
                        contentDescription = null
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (popData != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 2.dp),
                        painter = painterResource(sharedRes.drawable.wi_umbrella),
                        tint = colorResource(sharedRes.color.colorPrimaryLight),
                        contentDescription = null
                    )
                    Text(
                        text = spannableStringToAnnotatedString(popData.value),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = colorResource(sharedRes.color.colorPrimaryLight)
                    )
                }
            }
            if (popData != null && windData != null) {
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (windData != null) {
                val windSpeed = remember(windData.value) {
                    if (isLargeWidth) {
                        windData.value
                    } else {
                        windData.value.split(",")[0]
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 2.dp)
                            .rotate(windData.iconRotation.toFloat())
                            .align(Alignment.CenterVertically),
                        painter = painterResource(sharedRes.drawable.wi_wind_direction),
                        tint = Color(0xFF20B2AA),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = spannableStringToAnnotatedString(windSpeed),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = Color(0xFF20B2AA)
                    )
                }
            }
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
fun PreviewWeatherForecastPanel() {
    val context = LocalContext.current.also {
        it.initializeDependencies(isPhone = false)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        WeatherForecastPanel(
            model = ForecastItemViewModel().apply {
                weatherIcon = WeatherIcons.DAY_SUNNY
                date = "Mon 07"
                shortDate = "Mon 07"
                longDate = "Monday"
                condition = "Sunny"
                hiTemp = "70°"
                loTemp = "65°"
                windDirection = 180
                windSpeed = "7 mph"
                windDirLabel = "S"
                extras.put(
                    WeatherDetailsType.POPCHANCE,
                    DetailItemViewModel(WeatherDetailsType.POPCHANCE, "70%")
                )
                extras.put(
                    WeatherDetailsType.WINDSPEED,
                    DetailItemViewModel(WeatherDetailsType.WINDSPEED, "7 mph, S")
                )
            }
        )
    }
}