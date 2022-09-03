package com.thewizrd.simpleweather.ui.components.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.thewizrd.shared_resources.icons.WeatherIconProvider
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.simpleweather.ui.components.WeatherIcon
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices

@Composable
fun WearIconPreference(
    modifier: Modifier = Modifier,
    iconProvider: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val wip = remember(iconProvider) {
        sharedDeps.weatherIconsManager.getIconProvider(iconProvider)
    }

    WearIconPreference(
        modifier = modifier,
        iconProvider = wip,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Composable
private fun WearIconPreference(
    modifier: Modifier = Modifier,
    iconProvider: WeatherIconProvider,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val previewIcons = listOf(WeatherIcons.DAY_SUNNY, WeatherIcons.NIGHT_CLEAR, WeatherIcons.RAIN)

    TitleCard(
        modifier = modifier,
        title = {
            Text(text = iconProvider.displayName)
        },
        onClick = {
            onCheckedChange(!checked)
        },
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = MaterialTheme.colors.surface,
            endBackgroundColor = MaterialTheme.colors.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row {
                    previewIcons.forEach { icon ->
                        WeatherIcon(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(4.dp),
                            weatherIcon = icon,
                            shouldAnimate = true,
                            iconProvider = iconProvider.key,
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                }
            }
            Column {
                Icon(
                    imageVector = ToggleChipDefaults.radioIcon(checked = checked),
                    contentDescription = if (checked) "Checked" else "Unchecked"
                )
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun WearIconPreferencePreview() {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        var checked by remember { mutableStateOf(false) }

        WearIconPreference(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .wrapContentHeight(),
            iconProvider = remember {
                WeatherIconsEFProvider()
            },
            checked = checked,
            onCheckedChange = {
                checked = it
            }
        )
    }
}