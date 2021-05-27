package com.thewizrd.shared_resources.controls

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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
        return when (pollenCount) {
            Pollen.PollenCount.LOW -> SpannableString("Low").apply {
                setSpan(ForegroundColorSpan(Colors.LIMEGREEN), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            Pollen.PollenCount.MODERATE -> SpannableString("Moderate").apply {
                setSpan(ForegroundColorSpan(Colors.ORANGE), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            Pollen.PollenCount.HIGH -> SpannableString("High").apply {
                setSpan(ForegroundColorSpan(Colors.ORANGERED), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            Pollen.PollenCount.VERY_HIGH -> SpannableString("Very High").apply {
                setSpan(ForegroundColorSpan(Colors.RED), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            else -> WeatherIcons.EM_DASH
        }
    }
}