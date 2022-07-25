package com.thewizrd.simpleweather.locale

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.thewizrd.simpleweather.extras.attachToBaseContext

abstract class UserLocaleActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        attachToBaseContext()
    }
}