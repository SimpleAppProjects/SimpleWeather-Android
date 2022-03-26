package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.TransparentOverlay
import com.thewizrd.simpleweather.GlideApp
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.simpleweather.controls.getImageData
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetType
import com.thewizrd.simpleweather.widgets.WidgetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

abstract class CustomBackgroundWidgetRemoteViewCreator(
    context: Context,
    var loadBackground: Boolean
) : WidgetRemoteViewCreator(context) {
    override suspend fun buildExtras(
        appWidgetId: Int,
        updateViews: RemoteViews,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ) {
        // Background
        val background = WidgetUtils.getWidgetBackground(appWidgetId)
        var style: WidgetUtils.WidgetBackgroundStyle? = null

        if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            style = WidgetUtils.getBackgroundStyle(appWidgetId)
        }

        setWidgetBackground(
            info,
            appWidgetId,
            updateViews,
            background,
            style,
            newOptions,
            weather
        )
    }

    protected suspend fun setWidgetBackground(
        info: WidgetProviderInfo,
        appWidgetId: Int, updateViews: RemoteViews,
        background: WidgetUtils.WidgetBackground,
        style: WidgetUtils.WidgetBackgroundStyle?,
        newOptions: Bundle,
        weather: WeatherNowViewModel
    ) {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val backgroundColor = WidgetUtils.getBackgroundColor(appWidgetId, background)

        if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            var imageData: ImageDataViewModel? = null

            if (loadBackground) {
                imageData = weather.getImageData()
            }

            updateViews.removeAllViews(R.id.panda_container)
            updateViews.addView(
                R.id.panda_container,
                RemoteViews(context.packageName, R.layout.layout_panda_bg)
            )

            if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                // No-op
            } else if (style == WidgetUtils.WidgetBackgroundStyle.LIGHT) {
                updateViews.setImageViewResource(
                    R.id.panda_background,
                    R.drawable.widget_background
                )
                updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.WHITE)
            } else if (style == WidgetUtils.WidgetBackgroundStyle.DARK) {
                updateViews.setImageViewResource(
                    R.id.panda_background,
                    R.drawable.widget_background
                )
                updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.BLACK)
            } else {
                updateViews.removeAllViews(R.id.panda_container)
            }

            updateViews.setInt(R.id.widgetBackground, "setColorFilter", backgroundColor)
            updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF)

            if (loadBackground) {
                loadBackgroundImage(
                    context,
                    updateViews,
                    info,
                    appWidgetId,
                    imageData?.imageURI,
                    cellWidth,
                    cellHeight
                )
            } else {
                updateViews.setImageViewBitmap(R.id.widgetBackground, null)
            }
        } else if (background == WidgetUtils.WidgetBackground.TRANSPARENT) {
            updateViews.setImageViewResource(R.id.widgetBackground, R.drawable.widget_background)
            updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.BLACK)
            updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0x00)
            updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT)
            updateViews.setImageViewBitmap(R.id.panda_background, null)
        } else {
            updateViews.setImageViewBitmap(
                R.id.widgetBackground,
                ImageUtils.createColorBitmap(backgroundColor)
            )
            updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.TRANSPARENT)
            updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF)
            updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT)
            updateViews.setImageViewBitmap(R.id.panda_background, null)
        }
    }

    protected suspend fun loadBackgroundImage(
        context: Context, updateViews: RemoteViews,
        info: WidgetProviderInfo, appWidgetId: Int,
        backgroundURI: String?, cellWidth: Int, cellHeight: Int
    ) = withContext(Dispatchers.IO) {
        /*
         * The total Bitmap memory used by the RemoteViews object cannot exceed
         * that required to fill the screen 1.5 times,
         * ie. (screen width x screen height x 4 x 1.5) bytes.
         */
        val maxBitmapSize = context.getMaxBitmapSize()

        var imgWidth = 200 * cellWidth
        var imgHeight = 200 * cellHeight

        /*
         * Ensure width and height are both > 0
         * To avoid IllegalArgumentException
         */
        if (imgWidth == 0 || imgHeight == 0) {
            when (info.widgetType) {
                WidgetType.Widget1x1 -> {
                    imgHeight = 200
                    imgWidth = imgHeight
                }
                WidgetType.Widget2x2 -> {
                    imgHeight = 200 * 2
                    imgWidth = imgHeight
                }
                WidgetType.Widget4x1, WidgetType.Widget4x1Google -> {
                    imgWidth = 200 * 4
                    imgHeight = 200
                }
                WidgetType.Widget4x2 -> {
                    imgWidth = 200 * 4
                    imgHeight = 200 * 2
                }
                else -> {
                    imgHeight = 200
                    imgWidth = imgHeight
                }
            }
        }

        /*
         * The total Bitmap memory used by the RemoteViews object cannot exceed
         * that required to fill the screen 1.5 times,
         * ie. (screen width x screen height x 4 x 1.5) bytes.
         */
        if (maxBitmapSize < 3840000) { // (200 * 4) * (200 * 4) * 4 * 1.5f
            imgHeight = 200
            imgWidth = imgHeight
        } else if (imgHeight * imgWidth * 4 * 0.75f > maxBitmapSize) {
            when (info.widgetType) {
                WidgetType.Widget1x1 -> {
                    imgHeight = 200
                    imgWidth = imgHeight
                }
                WidgetType.Widget2x2 -> {
                    imgHeight = 200 * 2
                    imgWidth = imgHeight
                }
                WidgetType.Widget4x1, WidgetType.Widget4x1Google -> {
                    imgWidth = 200 * 4
                    imgHeight = 200
                }
                WidgetType.Widget4x2 -> {
                    imgWidth = 200 * 4
                    imgHeight = 200 * 2
                }
                else -> {
                    imgHeight = 200
                    imgWidth = imgHeight
                }
            }
        }

        try {
            val bmp = suspendCancellableCoroutine<Bitmap?> {
                val task = GlideApp.with(context)
                    .asBitmap()
                    .load(backgroundURI)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .centerCrop()
                    .transform(TransparentOverlay(0x33))
                    .thumbnail(0.75f)
                    .addListener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (it.isActive) {
                                it.resume(null)
                            }
                            return true
                        }

                        override fun onResourceReady(
                            resource: Bitmap?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            // Original image -> firstResource
                            // Thumbnail -> second resource
                            // Resume on the second call
                            if (it.isActive && !isFirstResource) it.resume(resource)
                            return true
                        }
                    })
                    .submit(imgWidth, imgHeight)

                it.invokeOnCancellation {
                    task.cancel(true)
                }
            } ?: return@withContext

            updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.TRANSPARENT)
            updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF)
            updateViews.setImageViewBitmap(R.id.widgetBackground, bmp)
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
        }
    }

    private fun Context.getMaxBitmapSize(): Float {
        val metrics = this.resources.displayMetrics
        return metrics.heightPixels * metrics.widthPixels * 4 * 0.75f
    }
}