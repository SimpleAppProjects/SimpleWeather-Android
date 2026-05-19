package com.thewizrd.simpleweather.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
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
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString
import org.jetbrains.annotations.TestOnly

@Composable
fun WeatherDetailItem(
    model: DetailItemViewModel
) {
    val isPreview = LocalInspectionMode.current

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
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
                        .rotate(model.iconRotation.toFloat()),
                    weatherIcon = model.icon,
                    tint = LocalContentColor.current
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
        model = DetailItemViewModel(WeatherDetailsType.FEELSLIKE).apply {
            value = "70°"
            label = "Feels like"
        }
    )
}