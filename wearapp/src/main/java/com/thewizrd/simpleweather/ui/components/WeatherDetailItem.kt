package com.thewizrd.simpleweather.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices
import org.jetbrains.annotations.TestOnly

@Composable
fun WeatherDetailItem(
    model: DetailItemViewModel
) {
    val isPreview = LocalInspectionMode.current

    Chip(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
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
            if (isPreview) {
                Image(
                    modifier = Modifier
                        .size(ChipDefaults.IconSize)
                        .wrapContentSize(align = Alignment.Center),
                    painter = painterResource(id = R.drawable.ic_error),
                    contentDescription = ""
                )
            } else {
                WeatherIcon(
                    modifier = Modifier
                        .size(ChipDefaults.IconSize)
                        .wrapContentSize(align = Alignment.Center)
                        .rotate(model.iconRotation.toFloat()),
                    weatherIcon = model.icon
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