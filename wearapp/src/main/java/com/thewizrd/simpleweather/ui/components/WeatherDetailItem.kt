package com.thewizrd.simpleweather.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString

@Composable
fun WeatherDetailItem(
    model: DetailItemViewModel
) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = spannableStringToAnnotatedString(model.label)
            )
        },
        secondaryLabel = {
            Text(
                text = spannableStringToAnnotatedString(model.value)
            )
        },
        onClick = {},
        colors = ChipDefaults.secondaryChipColors(),
        icon = {
            WeatherIcon(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(model.iconRotation.toFloat()),
                weatherIcon = model.icon
            )
        }
    )
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
fun PreviewWeatherDetailItem() {
    WeatherDetailItem(
        model = DetailItemViewModel(
            WeatherDetailsType.FEELSLIKE,
            "70°"
        )
    )
}