package com.thewizrd.simpleweather.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString
import org.jetbrains.annotations.TestOnly
import com.thewizrd.shared_resources.R as sharedRes

@Composable
fun WeatherDetailItem(
    model: DetailItemViewModel,
    iconProvider: String? = null
) {
    val wim = remember {
        sharedDeps.weatherIconsManager
    }

    WeatherDetailItem(
        label = model.label,
        value = model.value,
        icon = model.icon,
        iconRotation = model.iconRotation,
        iconProvider = iconProvider,
        showAsMonochrome = wim.shouldUseMonochrome(),
        shouldAnimate = true
    )
}

@Composable
private fun WeatherDetailItem(
    label: CharSequence,
    value: CharSequence,
    icon: String,
    iconProvider: String? = null,
    iconRotation: Int = 0,
    showAsMonochrome: Boolean = false,
    shouldAnimate: Boolean = true,
) {
    val isPreview = LocalInspectionMode.current

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        label = {
            Text(
                text = spannableStringToAnnotatedString(label)
            )
        },
        secondaryLabel = {
            Text(
                text = spannableStringToAnnotatedString(value)
            )
        },
        onClick = {},
        colors = ButtonDefaults.filledTonalButtonColors(),
        icon = {
            if (isPreview) {
                Image(
                    modifier = Modifier
                        .size(ButtonDefaults.IconSize)
                        .wrapContentSize(align = Alignment.Center),
                    painter = painterResource(id = sharedRes.drawable.ic_error),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(LocalContentColor.current)
                )
            } else {
                WeatherIcon(
                    modifier = Modifier
                        .size(ButtonDefaults.IconSize)
                        .wrapContentSize(align = Alignment.Center)
                        .rotate(iconRotation.toFloat()),
                    weatherIcon = icon,
                    iconProvider = iconProvider,
                    tint = LocalContentColor.current,
                    shouldAnimate = shouldAnimate,
                    showAsMonochrome = showAsMonochrome
                )
            }
        }
    )
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
@TestOnly
fun PreviewWeatherDetailItem() {
    WeatherDetailItem(
        label = "Feels like",
        value = "70°",
        icon = WeatherIcons.THERMOMETER
    )
}