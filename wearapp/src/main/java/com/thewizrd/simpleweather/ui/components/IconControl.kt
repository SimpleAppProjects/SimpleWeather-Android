package com.thewizrd.simpleweather.ui.components

import android.content.res.ColorStateList
import android.widget.ImageView.ScaleType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.ImageViewCompat
import com.thewizrd.common.controls.IconControl
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider

@Composable
fun WeatherIcon(
    modifier: Modifier = Modifier,
    weatherIcon: String? = WeatherIcons.NA,
    showAsMonochrome: Boolean = false,
    shouldAnimate: Boolean = false,
    forceDarkMode: Boolean = false,
    useDefaultIconProvider: Boolean = false,
    iconProvider: String? = if (useDefaultIconProvider) WeatherIconsEFProvider.KEY else null,
    contentDescription: String? = null,
    tint: Color = Color.Unspecified,
    alignment: IconAlignment = IconAlignment.Center
) {
    AndroidView(
        modifier = modifier,
        factory = {
            IconControl(it).apply {
                this.scaleType = alignment.scaleType
                this.weatherIcon = weatherIcon
                this.showAsMonochrome = showAsMonochrome
                this.shouldAnimate = shouldAnimate
                this.forceDarkMode = forceDarkMode
                this.iconProvider = iconProvider
                if (tint != Color.Unspecified) {
                    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tint.toArgb()))
                }
                if (useDefaultIconProvider) {
                    useDefaultIconProvider()
                }

                this.contentDescription = contentDescription
            }
        },
        update = {}
    )
}

interface IconAlignment {
    val scaleType: ScaleType

    data class ScaleTypeAlignment(
        override val scaleType: ScaleType
    ) : IconAlignment

    companion object {
        val Center = ScaleTypeAlignment(ScaleType.FIT_CENTER)
        val Start = ScaleTypeAlignment(ScaleType.FIT_START)
        val End = ScaleTypeAlignment(ScaleType.FIT_END)
    }
}