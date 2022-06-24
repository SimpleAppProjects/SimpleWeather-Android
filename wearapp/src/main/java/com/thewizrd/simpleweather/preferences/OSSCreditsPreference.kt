package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.thewizrd.shared_resources.appLib
import com.thewizrd.simpleweather.R
import kotlinx.coroutines.*
import java.io.InputStreamReader

class OSSCreditsPreference : Preference {
    private var loadJob: Job? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val textView = holder.findViewById(R.id.textview) as TextView
        val progressBar = holder.findViewById(R.id.progressBar)

        loadJob?.cancel()
        loadJob = appLib.appScope.launch(Dispatchers.Main.immediate) {
            supervisorScope {
                progressBar.visibility = View.VISIBLE

                val creditsText = withContext(Dispatchers.IO) {
                    val sBuilder = StringBuilder()

                    InputStreamReader(context.assets.open("credits/licenses.txt")).use { sReader ->
                        var c: Int
                        while (sReader.read().also { c = it } != -1) {
                            sBuilder.append(c.toChar())
                        }
                    }

                    sBuilder.toString()
                }

                ensureActive()

                textView.text = creditsText
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onPrepareForRemoval() {
        super.onPrepareForRemoval()
        loadJob?.cancel()
    }
}