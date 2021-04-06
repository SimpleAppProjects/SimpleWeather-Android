package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.thewizrd.simpleweather.R
import kotlinx.coroutines.*
import java.io.InputStreamReader

class OSSCreditsPreference : Preference {
    private var loadJob: Job? = null

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    override fun onBindView(view: View) {
        super.onBindView(view)
        val textView = view.findViewById<TextView>(R.id.textview)

        loadJob?.cancel()
        loadJob = GlobalScope.launch(Dispatchers.Main.immediate) {
            supervisorScope {
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
            }
        }
    }

    override fun onPrepareForRemoval() {
        super.onPrepareForRemoval()
        loadJob?.cancel()
    }
}