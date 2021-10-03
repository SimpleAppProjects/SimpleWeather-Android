package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.thewizrd.simpleweather.R

class WearPreferenceCategory @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = R.style.WearPreference_Category
) : PreferenceCategory(context, attrs, defStyleAttr, defStyleRes) {
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val view = holder.itemView as? TextView
        if (view != null) {
            view.text = title
        }
    }
}