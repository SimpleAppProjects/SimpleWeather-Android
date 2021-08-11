package com.thewizrd.shared_resources.icons

import android.content.Context
import android.graphics.drawable.Drawable

interface LottieIconsProviderInterface {
    fun getLottieIconDrawable(context: Context, icon: String): Drawable?
}