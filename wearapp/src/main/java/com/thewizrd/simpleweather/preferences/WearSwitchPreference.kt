package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.WearChipButton

class WearSwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.WearPreference_SwitchPreference
) : SwitchPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val chipButton = holder.itemView as? WearChipButton
        if (chipButton != null) {
            chipButton.setPrimaryText(title)
            chipButton.setSecondaryText(summary)
            chipButton.setIconDrawable(icon)

            chipButton.findViewById<TextView?>(R.id.wear_chip_secondary_text)?.apply {
                maxLines = 10
            }

            chipButton.isChecked = isChecked
        }
    }
}