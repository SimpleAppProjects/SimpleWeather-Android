package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import kotlinx.coroutines.*
import java.io.InputStreamReader

class OSSCreditsPreference : Preference {
    private var loadJob: Job? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val settingsManager = SettingsManager(context.applicationContext)

        val bg_color = if (settingsManager.getUserThemeMode() != UserThemeMode.AMOLED_DARK) {
            context.getAttrColor(android.R.attr.colorBackground)
        } else {
            Colors.BLACK
        }
        holder.itemView.setBackgroundColor(bg_color)

        val webView = holder.itemView.findViewById<TextView>(R.id.textview)
        val progressBar = holder.itemView.findViewById<ProgressBar>(R.id.progressBar)

        loadJob?.cancel()
        loadJob = GlobalScope.launch(Dispatchers.Main.immediate) {
            supervisorScope {
                progressBar.visibility = View.VISIBLE

                val creditsText = withContext(Dispatchers.IO) {
                    val sBuilder = StringBuilder()

                    InputStreamReader(context.assets.open("credits/licenses.html")).use { sReader ->
                        var c: Int
                        while (sReader.read().also { c = it } != -1) {
                            val char = c.toChar()
                            sBuilder.append(if (char == '\n') "<br/>" else char)
                        }
                    }

                    sBuilder.toString()
                }

                ensureActive()

                webView.text = HtmlCompat.fromHtml(creditsText, HtmlCompat.FROM_HTML_MODE_COMPACT)
                webView.movementMethod = LinkMovementMethod.getInstance()

                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDetached() {
        super.onDetached()
        loadJob?.cancel()
    }

    override fun onPrepareForRemoval() {
        super.onPrepareForRemoval()
        loadJob?.cancel()
    }
}