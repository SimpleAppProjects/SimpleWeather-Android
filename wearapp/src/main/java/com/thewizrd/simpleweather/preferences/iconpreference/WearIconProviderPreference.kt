package com.thewizrd.simpleweather.preferences.iconpreference

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.WearChipButton

class WearIconProviderPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.WearPreference_RadioPreference
) : IconProviderPreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        val chipButton = holder.itemView as? WearChipButton
        if (chipButton != null) {
            chipButton.setPrimaryText(title)
            chipButton.setSecondaryText(summary)
            chipButton.setIconDrawable(icon)

            chipButton.findViewById<TextView?>(R.id.wear_chip_secondary_text)?.apply {
                maxLines = 10
            }

            chipButton.isChecked = isChecked

            if (chipButton.getContentView() == null) {
                chipButton.setContentView(
                    LinearLayout(chipButton.context).apply {
                        id = R.id.icons_container
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                )
            }
        }

        super.onBindViewHolder(holder)
    }
}