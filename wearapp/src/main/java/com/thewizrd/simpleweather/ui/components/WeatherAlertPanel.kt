@file:OptIn(ExperimentalHorologistApi::class, ExperimentalWearFoundationApi::class)

package com.thewizrd.simpleweather.ui.components

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.thewizrd.common.controls.WeatherAlertViewModel
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.utils.getColorFromAlertSeverity
import com.thewizrd.shared_resources.utils.getDrawableFromAlertType
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.text.toAnnotatedString

@Composable
fun WeatherAlertPanel(
    model: WeatherAlertViewModel
) {
    WeatherAlertPanel(
        title = model.title,
        alertBodyMessage = model.alertBodyMessage,
        alertSeverityColor = model.alertSeverityColor,
        alertDrawable = model.alertDrawable
    )
}

@Composable
private fun WeatherAlertPanel(
    title: String,
    alertBodyMessage: CharSequence,
    @ColorInt alertSeverityColor: Int,
    @DrawableRes alertDrawable: Int = sharedRes.drawable.ic_error
) {
    var showDialog by remember { mutableStateOf(false) }
    val severityColor = remember(alertSeverityColor) { Color(alertSeverityColor) }

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        label = {
            Text(
                text = title,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            Icon(
                modifier = Modifier.size(24.dp),
                tint = Color.White,
                painter = painterResource(id = alertDrawable),
                contentDescription = null
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = severityColor,
            contentColor = Color.White,
            iconColor = Color.White
        ),
        onClick = { showDialog = true }
    )

    // show alert dialog on click
    AlertDialog(
        visible = showDialog,
        onDismissRequest = {
            showDialog = false
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        },
        icon = {
            Box(
                modifier = Modifier
                    .background(
                        color = severityColor,
                        shape = CircleShape
                    )
                    .padding(2.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                    painter = painterResource(id = alertDrawable),
                    contentDescription = null
                )
            }
        },
        text = {
            Text(text = alertBodyMessage.toAnnotatedString())
        },
    )
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun PreviewWeatherAlertPanel() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        WeatherAlertPanel(
            title = "Winter Weather Advisory",
            alertBodyMessage = "Winter Weather Advisory",
            alertDrawable = WeatherAlertType.WINTERWEATHER.getDrawableFromAlertType(),
            alertSeverityColor = WeatherAlertSeverity.SEVERE.getColorFromAlertSeverity()
        )
    }
}