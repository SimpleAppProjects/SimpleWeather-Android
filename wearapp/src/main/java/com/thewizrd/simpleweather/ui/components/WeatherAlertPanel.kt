package com.thewizrd.simpleweather.ui.components

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import com.google.android.horologist.compose.navscaffold.scrollableColumn
import com.thewizrd.common.controls.WeatherAlertViewModel
import com.thewizrd.shared_resources.utils.getColorFromAlertSeverity
import com.thewizrd.shared_resources.utils.getDrawableFromAlertType
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.dialog.Alert
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices

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
    alertBodyMessage: String,
    @ColorInt alertSeverityColor: Int,
    @DrawableRes alertDrawable: Int = R.drawable.ic_error
) {
    var showDialog by remember { mutableStateOf(false) }
    val severityColor = remember(alertSeverityColor) { Color(alertSeverityColor) }

    Chip(
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = title)
        },
        icon = {
            Icon(
                modifier = Modifier.size(24.dp),
                tint = Color.White,
                painter = painterResource(id = alertDrawable),
                contentDescription = null
            )
        },
        colors = ChipDefaults.gradientBackgroundChipColors(
            startBackgroundColor = severityColor,
            contentColor = Color.White,
            iconColor = Color.White
        ),
        onClick = { showDialog = true }
    )

    // show alert dialog on click
    val dialogScrollState = rememberScalingLazyListState()
    Dialog(
        showDialog = showDialog,
        onDismissRequest = {
            showDialog = false
        },
        scrollState = dialogScrollState
    ) {
        val focusRequester = remember { FocusRequester() }

        Alert(
            modifier = Modifier.scrollableColumn(focusRequester, dialogScrollState),
            scrollState = dialogScrollState,
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
            message = {
                Text(text = alertBodyMessage)
            }
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@WearPreviewDevices
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