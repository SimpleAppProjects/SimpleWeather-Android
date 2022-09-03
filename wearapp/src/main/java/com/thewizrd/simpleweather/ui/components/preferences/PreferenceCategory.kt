package com.thewizrd.simpleweather.ui.components.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.layout.fillMaxRectangle
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices

@Composable
fun PreferenceCategory(
    modifier: Modifier = Modifier,
    title: String
) {
    Text(
        text = title,
        modifier = modifier.padding(
            start = 16.dp,
            top = 24.dp,
            end = 16.dp,
            bottom = 8.dp
        ),
        color = MaterialTheme.colors.primary,
        style = MaterialTheme.typography.button
    )
}

@WearPreviewDevices
@Composable
private fun PreferenceCategoryPreview() {
    Box(
        modifier = Modifier.fillMaxRectangle()
    ) {
        PreferenceCategory(title = "Category")
    }
}