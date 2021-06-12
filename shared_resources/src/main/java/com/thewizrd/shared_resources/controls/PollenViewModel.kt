package com.thewizrd.shared_resources.controls

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.model.Pollen

class PollenViewModel(pollenData: Pollen) {
    var treePollenDesc: CharSequence
        private set

    var grassPollenDesc: CharSequence
        private set

    var ragweedPollenDesc: CharSequence
        private set

    init {
        treePollenDesc = getPollenCountDescription(pollenData.treePollenCount)
        grassPollenDesc = getPollenCountDescription(pollenData.grassPollenCount)
        ragweedPollenDesc = getPollenCountDescription(pollenData.ragweedPollenCount)
    }

    private fun getPollenCountDescription(pollenCount: Pollen.PollenCount): CharSequence {
        val context = SimpleLibrary.instance.appContext

        return when (pollenCount) {
            Pollen.PollenCount.LOW -> SpannableString(context.getString(R.string.label_count_low)).apply {
                setSpan(
                    ForegroundColorSpan(Colors.LIMEGREEN),
                    0,
                    this.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            Pollen.PollenCount.MODERATE -> SpannableString(context.getString(R.string.label_count_moderate)).apply {
                setSpan(
                    ForegroundColorSpan(Colors.ORANGE),
                    0,
                    this.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            Pollen.PollenCount.HIGH -> SpannableString(context.getString(R.string.label_count_high)).apply {
                setSpan(
                    ForegroundColorSpan(Colors.ORANGERED),
                    0,
                    this.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            Pollen.PollenCount.VERY_HIGH -> SpannableString(context.getString(R.string.label_count_veryhigh)).apply {
                setSpan(
                    ForegroundColorSpan(Colors.RED),
                    0,
                    this.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            else -> WeatherIcons.EM_DASH
        }
    }
}