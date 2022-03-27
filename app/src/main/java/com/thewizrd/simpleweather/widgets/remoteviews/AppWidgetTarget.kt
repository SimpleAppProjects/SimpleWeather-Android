package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition

class AppWidgetTarget(
    private val context: Context,
    private val widgetId: Int,
    private val updateViews: RemoteViews,
    private val imageViewId: Int,
    private val fullUpdate: Boolean = false,
    width: Int = Target.SIZE_ORIGINAL,
    height: Int = Target.SIZE_ORIGINAL
) : CustomTarget<Bitmap>(width, height) {
    init {
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            throw IllegalArgumentException("Widget id is invalid")
        }

        if (imageViewId == Resources.ID_NULL) {
            throw IllegalArgumentException("View id is invalid")
        }
    }

    /** Updates the AppWidget after the ImageView has loaded the Bitmap.  */
    private fun update() {
        AppWidgetManager.getInstance(context).run {
            if (fullUpdate) {
                updateAppWidget(widgetId, updateViews)
            } else {
                partiallyUpdateAppWidget(widgetId, updateViews)
            }
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        setBitmap(null)
    }

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        setBitmap(resource)
    }

    private fun setBitmap(bitmap: Bitmap?) {
        this.updateViews.setImageViewBitmap(imageViewId, bitmap)
        update()
    }
}