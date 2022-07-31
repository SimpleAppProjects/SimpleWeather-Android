package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.common.utils.glide.CustomRoundedCorners
import com.thewizrd.common.utils.glide.TransparentOverlay
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.GlideApp
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.simpleweather.viewmodels.getImageData
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.WidgetUtils.getMaxBitmapSize
import com.thewizrd.simpleweather.widgets.preferences.KEY_BGCOLOR
import com.thewizrd.simpleweather.widgets.preferences.KEY_BGCOLORCODE
import com.thewizrd.simpleweather.widgets.preferences.KEY_BGSTYLE
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
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ) {
        // Background
        val background = newOptions.getSerializable(KEY_BGCOLOR) as? WidgetUtils.WidgetBackground
            ?: WidgetUtils.getWidgetBackground(appWidgetId)
        var style: WidgetUtils.WidgetBackgroundStyle? = null

        if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            style = (newOptions.getSerializable(KEY_BGSTYLE) as? WidgetUtils.WidgetBackgroundStyle)
                ?: WidgetUtils.getBackgroundStyle(appWidgetId)
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

    private suspend fun setWidgetBackground(
        info: WidgetProviderInfo,
        appWidgetId: Int, updateViews: RemoteViews,
        background: WidgetUtils.WidgetBackground,
        style: WidgetUtils.WidgetBackgroundStyle?,
        newOptions: Bundle,
        weather: WeatherUiModel
    ) {
        val backgroundColor = if (background == WidgetUtils.WidgetBackground.CUSTOM) {
            newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(appWidgetId)
        } else {
            Colors.TRANSPARENT
        }

        if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            var imageData: ImageDataViewModel? = null

            if (loadBackground) {
                imageData = weather.getImageData()
            }

            if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    updateViews.setColorAttr(
                        R.id.panda_container,
                        "setBackgroundColor",
                        R.attr.colorSurface
                    )
                } else {
                    updateViews.addView(
                        R.id.panda_container,
                        RemoteViews(context.packageName, R.layout.layout_panda_bg)
                    )
                }
            } else if (style == WidgetUtils.WidgetBackgroundStyle.LIGHT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    updateViews.setColorInt(
                        R.id.panda_container,
                        "setBackgroundColor",
                        Colors.WHITE,
                        Colors.WHITE
                    )
                } else {
                    updateViews.addView(
                        R.id.panda_container,
                        RemoteViews(context.packageName, R.layout.layout_panda_bg)
                    )
                    updateViews.setImageViewResource(
                        R.id.panda_background,
                        R.drawable.widget_panel_background
                    )
                    updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.WHITE)
                }
            } else if (style == WidgetUtils.WidgetBackgroundStyle.DARK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    updateViews.setColorInt(
                        R.id.panda_container,
                        "setBackgroundColor",
                        Colors.BLACK,
                        Colors.BLACK
                    )
                } else {
                    updateViews.addView(
                        R.id.panda_container,
                        RemoteViews(context.packageName, R.layout.layout_panda_bg)
                    )
                    updateViews.setImageViewResource(
                        R.id.panda_background,
                        R.drawable.widget_panel_background
                    )
                    updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.BLACK)
                }
            }

            if (loadBackground) {
                loadBackgroundImage(
                    context,
                    updateViews,
                    info,
                    appWidgetId,
                    imageData?.imageURI,
                    newOptions
                )
            } else {
                updateViews.setImageViewBitmap(R.id.widgetBackground, null)
            }
        } else if (background == WidgetUtils.WidgetBackground.TRANSPARENT) {
            updateViews.setImageViewBitmap(R.id.widgetBackground, null)
            updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT)
            updateViews.setImageViewBitmap(R.id.panda_background, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                updateViews.setInt(R.id.panda_container, "setBackgroundResource", 0)
            } else {
                updateViews.removeAllViews(R.id.panda_container)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                updateViews.setImageViewBitmap(
                    R.id.widgetBackground,
                    ImageUtils.createColorBitmap(backgroundColor)
                )
                updateViews.setInt(R.id.panda_container, "setBackgroundResource", 0)
            } else {
                val maxBitmapSize = context.getMaxBitmapSize()

                // Widget dimensions
                val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)

                var imgWidth = context.dpToPx(maxWidth.toFloat()).toInt()
                var imgHeight = context.dpToPx(maxHeight.toFloat()).toInt()

                if (imgHeight * imgWidth * 4 * 1.5f >= maxBitmapSize) {
                    imgWidth = context.dpToPx(minWidth.toFloat()).toInt()
                    imgHeight = context.dpToPx(minHeight.toFloat()).toInt()
                }

                updateViews.setImageViewBitmap(
                    R.id.widgetBackground,
                    ImageUtils.fillColorRoundedCornerBitmap(
                        backgroundColor,
                        imgWidth,
                        imgHeight,
                        context.dpToPx(16f)
                    )
                )
            }
            updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT)
            updateViews.setImageViewBitmap(R.id.panda_background, null)
            updateViews.removeAllViews(R.id.panda_container)
        }
    }

    private suspend fun loadBackgroundImage(
        context: Context, updateViews: RemoteViews,
        info: WidgetProviderInfo, appWidgetId: Int,
        backgroundURI: String?, newOptions: Bundle
    ) = withContext(Dispatchers.IO) {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)

        val imgWidth = context.dpToPx(minWidth.toFloat()).toInt()
        val imgHeight = context.dpToPx(minHeight.toFloat()).toInt()

        val cornerRadius = context.dpToPx(16f)

        try {
            WidgetUtils.setBackgroundUri(appWidgetId, backgroundURI)

            val bmp = suspendCancellableCoroutine<Bitmap?> {
                val task = GlideApp.with(context)
                    .asBitmap()
                    .load(backgroundURI)
                    .run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            thumbnail(0.75f)
                        } else {
                            this
                        }
                    }
                    .apply(
                        RequestOptions.noTransformation()
                            .format(DecodeFormat.PREFER_RGB_565)
                            .run {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    transform(
                                        TransparentOverlay(
                                            0x33
                                        ), CenterCrop()
                                    )
                                } else {
                                    override(imgWidth, imgHeight)
                                        .transform(
                                            TransparentOverlay(
                                                0x33
                                            ),
                                            CenterCrop(),
                                            CustomRoundedCorners(
                                                cornerRadius
                                            )
                                        )
                                }
                            }
                    )
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
                            if (it.isActive && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !isFirstResource)) {
                                it.resume(resource)
                            }
                            return true
                        }
                    })
                    .submit()

                it.invokeOnCancellation {
                    task.cancel(true)
                }
            } ?: return@withContext

            updateViews.setImageViewBitmap(R.id.widgetBackground, bmp)
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
        }
    }

    override fun resizeWidgetBackground(
        info: WidgetProviderInfo,
        appWidgetId: Int,
        updateViews: RemoteViews,
        newOptions: Bundle
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return

        // Background
        val background = newOptions.getSerializable(KEY_BGCOLOR) as? WidgetUtils.WidgetBackground
            ?: WidgetUtils.getWidgetBackground(appWidgetId)

        if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            // Widget dimensions
            val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

            val imgWidth = context.dpToPx(minWidth.toFloat()).toInt()
            val imgHeight = context.dpToPx(minHeight.toFloat()).toInt()

            val cornerRadius = context.dpToPx(16f)

            try {
                GlideApp.with(context)
                    .asBitmap()
                    .load(WidgetUtils.getBackgroundUri(appWidgetId))
                    .run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            thumbnail(0.75f)
                        } else {
                            this
                        }
                    }
                    .apply(
                        RequestOptions.noTransformation()
                            .format(DecodeFormat.PREFER_RGB_565)
                            .run {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    transform(
                                        TransparentOverlay(
                                            0x33
                                        ), CenterCrop()
                                    )
                                } else {
                                    override(imgWidth, imgHeight)
                                        .transform(
                                            TransparentOverlay(
                                                0x33
                                            ),
                                            CenterCrop(),
                                            CustomRoundedCorners(
                                                cornerRadius
                                            )
                                        )
                                }
                            }
                    ).into(
                        AppWidgetTarget(
                            context, appWidgetId, updateViews, R.id.widgetBackground
                        )
                    )
            } catch (e: Exception) {
                Logger.writeLine(Log.ERROR, e)
            }
        }
    }
}