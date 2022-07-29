package com.thewizrd.simpleweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Dialog
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.thewizrd.common.controls.WeatherAlertViewModel
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import com.thewizrd.simpleweather.R
import java.time.ZonedDateTime

@Composable
fun WeatherAlertPanel(
    model: WeatherAlertViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    val severityColor = remember { Color(model.alertSeverityColor) }

    Chip(
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = model.title)
        },
        icon = {
            Icon(
                modifier = Modifier.size(24.dp),
                tint = Color.White,
                painter = painterResource(id = model.alertDrawable),
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
    Dialog(
        showDialog = showDialog,
        onDismissRequest = {
            showDialog = false
        }
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxRectangle()
                .verticalScroll(scrollState)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.background(
                        color = severityColor,
                        shape = CircleShape
                    )
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp),
                        painter = painterResource(id = model.alertDrawable),
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = model.title,
                    maxLines = 2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Text(text = model.alertBodyMessage)
            }
        }
    }
}

@Preview
@Composable
fun PreviewWeatherAlertPanel() {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = dimensionResource(id = R.dimen.list_item_padding))
            .verticalScroll(scrollState),
        contentAlignment = Alignment.Center
    ) {
        WeatherAlertPanel(
            WeatherAlertViewModel(
                WeatherAlert().apply {
                    type = WeatherAlertType.WINTERWEATHER
                    severity = WeatherAlertSeverity.SEVERE
                    title = "Winter Weather Advisory"
                    message = "Winter Weather Advisory"
                    date = ZonedDateTime.now()
                    expiresDate = ZonedDateTime.now().plusDays(1)
                }
            )
        )
    }
}