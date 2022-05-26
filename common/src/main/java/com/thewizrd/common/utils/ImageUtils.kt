package com.thewizrd.common.utils

import android.content.Context
import android.content.res.AssetManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.Logger
import java.io.IOException
import java.io.InputStream

object ImageUtils {
    @JvmStatic
    fun bitmapFromAssets(am: AssetManager, path: String): Bitmap? {
        val bmp: Bitmap? = try {
            val stream = am.open(path)
            BitmapFactory.decodeStream(stream)
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex)
            null
        }

        return bmp
    }

    @JvmStatic
    fun fontTextToBitmap(
        @NonNull context: Context,
        @NonNull text: CharSequence,
        @FontRes fontId: Int,
        textSizeSp: Float,
        textColor: Int,
        shadowRadius: Float
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSp, metrics)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        val font = ResourcesCompat.getFont(context, fontId)
        paint.isSubpixelText = true
        paint.typeface = font
        paint.style = Paint.Style.FILL
        paint.color = textColor
        paint.textSize = textSizePx
        paint.textAlign = Paint.Align.LEFT

        val shadowLayerRadius =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, shadowRadius, metrics)

        if (shadowRadius > 0) {
            paint.setShadowLayer(shadowLayerRadius, 1f, 1f, Colors.BLACK)
        }

        val width = paint.measureText(text, 0, text.length).toInt()

        val mTextLayout = StaticLayout(
            text,
            paint,
            (width + shadowLayerRadius * 2f).toInt(),
            Layout.Alignment.ALIGN_CENTER,
            1.0f,
            0.0f,
            false
        )

        val bmp =
            Bitmap.createBitmap(mTextLayout.width, mTextLayout.height, Bitmap.Config.ARGB_8888)
        val myCanvas = Canvas(bmp)
        mTextLayout.draw(myCanvas)

        return bmp
    }

    @JvmStatic
    fun adaptiveBitmapFromDrawable(context: Context, @DrawableRes resDrawable: Int): Bitmap {
        val drawable =
            ContextCompat.getDrawable(context.getThemeContextOverride(true), resDrawable)!!

        val canvasSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            108f,
            context.resources.displayMetrics
        ).toInt()
        val iconSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            72f,
            context.resources.displayMetrics
        ).toInt()
        val point = canvasSize - iconSize

        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Colors.WHITESMOKE)

        val canvas = Canvas(bitmap)
        drawable.setBounds(point, point, iconSize, iconSize)
        drawable.draw(canvas)
        return bitmap
    }

    @JvmStatic
    fun bitmapFromDrawable(context: Context, @DrawableRes resDrawable: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resDrawable)!!
        return bitmapFromDrawable(drawable)
    }

    @JvmStatic
    fun bitmapFromDrawable(
        context: Context,
        @DrawableRes resDrawable: Int,
        destWidth: Float,
        destHeight: Float
    ): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resDrawable)!!
        return bitmapFromDrawable(drawable, destWidth, destHeight)
    }

    @JvmStatic
    fun tintedBitmapFromDrawable(
        context: Context,
        @DrawableRes resDrawable: Int,
        @ColorInt color: Int
    ): Bitmap {
        val wrapped = tintedDrawable(context, resDrawable, color)
        return bitmapFromDrawable(wrapped)
    }

    @JvmStatic
    fun tintedBitmapFromDrawable(
        context: Context,
        @DrawableRes resDrawable: Int,
        @ColorInt color: Int,
        destWidth: Float,
        destHeight: Float
    ): Bitmap {
        val wrapped = tintedDrawable(context, resDrawable, color)
        return bitmapFromDrawable(wrapped, destWidth, destHeight)
    }

    private fun tintedDrawable(
        context: Context,
        @DrawableRes resDrawable: Int,
        @ColorInt color: Int
    ): Drawable {
        val drawable = ContextCompat.getDrawable(context, resDrawable)
        val wrapped = DrawableCompat.wrap(drawable!!).mutate()
        DrawableCompat.setTint(wrapped, color)
        return wrapped
    }

    @JvmStatic
    @JvmOverloads
    fun bitmapFromDrawable(
        drawable: Drawable,
        destWidth: Float = drawable.intrinsicWidth.toFloat(),
        destHeight: Float = drawable.intrinsicHeight.toFloat(),
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ): Bitmap {
        val bitmap: Bitmap = if (destWidth <= 0 || destHeight <= 0) {
            Bitmap.createBitmap(1, 1, config)
        } else {
            Bitmap.createBitmap(destWidth.toInt(), destHeight.toInt(), config)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun Drawable.toBitmap(
        destWidth: Float = this.intrinsicWidth.toFloat(),
        destHeight: Float = this.intrinsicHeight.toFloat(),
        config: Bitmap.Config = Bitmap.Config.ARGB_8888
    ): Bitmap {
        return bitmapFromDrawable(this, destWidth, destHeight, config)
    }

    @JvmStatic
    fun tintBitmap(bitmap: Bitmap, @ColorInt color: Int): Bitmap {
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        val bitmapResult = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(bitmapResult)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bitmapResult
    }

    fun Bitmap.tint(@ColorInt color: Int): Bitmap {
        return tintBitmap(this, color)
    }

    @JvmStatic
    fun rotateBitmap(source: Bitmap, @FloatRange(from = 0.0, to = 360.0) angle: Float): Bitmap {
        return if (angle != 0f || angle != 360f) {
            val matrix = Matrix()
            matrix.postRotate(angle)
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }
    }

    fun Bitmap.rotate(@FloatRange(from = 0.0, to = 360.0) angle: Float): Bitmap {
        return rotateBitmap(this, angle)
    }

    @JvmStatic
    fun createColorBitmap(@ColorInt color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(color)
        return bmp
    }

    enum class ImageType {
        JPEG, PNG, GIF, BMP, WEBP, UNKNOWN
    }

    /**
     * Guess image file type from file header
     * <p>
     * Checks for GIF, JPEG, PNG, WEBP and BMP file formats
     * <p>
     * Sources:
     * <p>
     * URLConnection.guessContentTypeFromStream
     * <p>
     * https://stackoverflow.com/questions/670546/determine-if-file-is-an-image
     *
     * @see java.net.URLConnection#guessContentTypeFromStream(InputStream)
     * @see com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser
     */
    @JvmStatic
    @Throws(IOException::class)
    fun guessImageType(`is`: InputStream): ImageType {
        // If we can't read ahead safely, just give up on guessing
        if (!`is`.markSupported())
            return ImageType.UNKNOWN

        `is`.mark(16)
        val c1 = `is`.read()
        val c2 = `is`.read()
        val c3 = `is`.read()
        val c4 = `is`.read()
        val c5 = `is`.read()
        val c6 = `is`.read()
        val c7 = `is`.read()
        val c8 = `is`.read()
        val c9 = `is`.read()
        val c10 = `is`.read()
        val c11 = `is`.read()
        val c12 = `is`.read()
        `is`.reset()

        if (c1 == 0xFF && c2 == 0xD8 && c3 == 0xFF) {
            if (c4 == 0xE0 || c4 == 0xEE) {
                return ImageType.JPEG;
            }

            /*
             * File format used by digital cameras to store images.
             * Exif Format can be read by any application supporting
             * JPEG. Exif Spec can be found at:
             * http://www.pima.net/standards/it10/PIMA15740/Exif_2-1.PDF
             */
            if ((c4 == 0xE1) &&
                (c7 == 'E'.code && c8 == 'x'.code && c9 == 'i'.code && c10 == 'f'.code &&
                        c11 == 0)
            ) {
                return ImageType.JPEG;
            }
        }

        if (c1 == 0x89 && c2 == 0x50 && c3 == 0x4e &&
            c4 == 0x47 && c5 == 0x0d && c6 == 0x0a &&
            c7 == 0x1a && c8 == 0x0a
        ) {
            return ImageType.PNG;
        }

        // 0x47 0x49 0x46
        if (c1 == 'G'.code && c2 == 'I'.code && c3 == 'F'.code && c4 == '8'.code) {
            return ImageType.GIF;
        }

        // WebP - "RIFF"
        if (c1 == 0x52 && c2 == 0x49 && c3 == 0x46 && c4 == 0x46) {
            return ImageType.WEBP;
        }

        // "BM"
        if (c1 == 0x42 && c2 == 0x4D) {
            return ImageType.BMP;
        }

        return ImageType.UNKNOWN;
    }

    fun roundedCornerBitmap(inBitmap: Bitmap, cornerRadius: Float): Bitmap {
        return roundedCornerBitmap(inBitmap, cornerRadius, cornerRadius, cornerRadius, cornerRadius)
    }

    fun roundedCornerBitmap(
        inBitmap: Bitmap,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float
    ): Bitmap {
        val result = Bitmap.createBitmap(inBitmap.width, inBitmap.height, Bitmap.Config.ARGB_8888)
        result.setHasAlpha(true)

        val shader = BitmapShader(inBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = shader
        val rect = RectF(
            0f, 0f,
            result.width.toFloat(), result.height.toFloat()
        )

        val canvas = Canvas(result)
        canvas.drawColor(Colors.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val clipPath = Path()
        /*
         * The corners are ordered top-left, top-right,
         * bottom-right, bottom-left
         */
        clipPath.addRoundRect(
            rect, floatArrayOf(
                topLeft, topLeft,
                topRight, topRight,
                bottomRight, bottomRight,
                bottomLeft, bottomLeft
            ), Path.Direction.CW
        )
        canvas.drawPath(clipPath, paint)

        canvas.setBitmap(null)

        return result
    }

    fun fillColorRoundedCornerBitmap(
        @ColorInt fillColor: Int,
        width: Int,
        height: Int,
        cornerRadius: Float
    ): Bitmap {
        return fillColorRoundedCornerBitmap(
            fillColor, width, height,
            cornerRadius, cornerRadius, cornerRadius, cornerRadius
        )
    }

    fun fillColorRoundedCornerBitmap(
        @ColorInt fillColor: Int,
        width: Int,
        height: Int,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float
    ): Bitmap {
        val bmpWidth = if (width <= 0) 1 else width
        val bmpHeight = if (height <= 0) 1 else height

        val result = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        result.setHasAlpha(true)

        val paint = Paint().apply {
            isAntiAlias = true
            color = fillColor
        }

        val rect = RectF(
            0f, 0f,
            result.width.toFloat(), result.height.toFloat()
        )

        val canvas = Canvas(result)
        canvas.drawColor(Colors.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val clipPath = Path()
        /*
         * The corners are ordered top-left, top-right,
         * bottom-right, bottom-left
         */
        clipPath.addRoundRect(
            rect, floatArrayOf(
                topLeft, topLeft,
                topRight, topRight,
                bottomRight, bottomRight,
                bottomLeft, bottomLeft
            ), Path.Direction.CW
        )
        canvas.drawPath(clipPath, paint)

        canvas.setBitmap(null)

        return result
    }
}