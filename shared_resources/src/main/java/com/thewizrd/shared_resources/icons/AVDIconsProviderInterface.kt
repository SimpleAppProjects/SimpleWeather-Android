package com.thewizrd.shared_resources.icons

import android.content.Context
import android.graphics.drawable.Drawable

interface AVDIconsProviderInterface {
    fun getAnimatedDrawable(context: Context, icon: String): Drawable?
}