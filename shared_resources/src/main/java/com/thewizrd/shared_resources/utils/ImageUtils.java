package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.FontRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.thewizrd.shared_resources.helpers.ContextUtils;

import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    public static Bitmap bitmapFromAssets(AssetManager am, String path) {
        Bitmap bmp = null;

        try {
            InputStream stream = am.open(path);
            bmp = BitmapFactory.decodeStream(stream);
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex);
            bmp = null;
        }

        return bmp;
    }

    public static Bitmap fontTextToBitmap(@NonNull Context context, @NonNull CharSequence text, @FontRes int fontId, int textSize, int textColor, float shadowRadius) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, metrics);

        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Typeface font = ResourcesCompat.getFont(context, fontId);
        paint.setSubpixelText(true);
        paint.setTypeface(font);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setTextSize(textSizePx);
        paint.setTextAlign(Paint.Align.LEFT);

        float shadowLayerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, shadowRadius, metrics);

        if (shadowRadius > 0) {
            paint.setShadowLayer(shadowLayerRadius, 1, 1, Colors.BLACK);
        }

        int width = (int) paint.measureText(text, 0, text.length());

        StaticLayout mTextLayout = new StaticLayout(
                text, paint, (int) (width + shadowLayerRadius * 2f), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        Bitmap bmp = Bitmap.createBitmap(mTextLayout.getWidth(), mTextLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas myCanvas = new Canvas(bmp);
        mTextLayout.draw(myCanvas);

        return bmp;
    }

    public static Bitmap adaptiveBitmapFromDrawable(@NonNull Context context, @DrawableRes int resDrawable) {
        Drawable drawable = ContextCompat.getDrawable(ContextUtils.getThemeContextOverride(context, true), resDrawable);

        final int canvasSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108, context.getResources().getDisplayMetrics());
        final int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, context.getResources().getDisplayMetrics());
        final int point = canvasSize - iconSize;

        Bitmap bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Colors.WHITESMOKE);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(point, point, iconSize, iconSize);
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap bitmapFromDrawable(@NonNull Context context, @DrawableRes int resDrawable) {
        Drawable drawable = ContextCompat.getDrawable(context, resDrawable);
        return bitmapFromDrawable(drawable);
    }

    public static Bitmap bitmapFromDrawable(@NonNull Context context, @DrawableRes int resDrawable, float destWidth, float destHeight) {
        Drawable drawable = ContextCompat.getDrawable(context, resDrawable);
        return bitmapFromDrawable(drawable, destWidth, destHeight);
    }

    public static Bitmap tintedBitmapFromDrawable(@NonNull Context context, @DrawableRes int resDrawable, int color) {
        Drawable wrapped = tintedDrawable(context, resDrawable, color);
        return bitmapFromDrawable(wrapped);
    }

    public static Bitmap tintedBitmapFromDrawable(@NonNull Context context, @DrawableRes int resDrawable, int color, float destWidth, float destHeight) {
        Drawable wrapped = tintedDrawable(context, resDrawable, color);
        return bitmapFromDrawable(wrapped, destWidth, destHeight);
    }

    private static Drawable tintedDrawable(@NonNull Context context, @DrawableRes int resDrawable, int color) {
        Drawable drawable = ContextCompat.getDrawable(context, resDrawable);
        Drawable wrapped = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(wrapped, color);
        return wrapped;
    }

    public static Bitmap bitmapFromDrawable(@NonNull Drawable drawable) {
        return bitmapFromDrawable(drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }

    public static Bitmap bitmapFromDrawable(@NonNull Drawable drawable, float destWidth, float destHeight) {
        Bitmap bitmap;

        if (destWidth <= 0 || destHeight <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap((int) destWidth, (int) destHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap tintBitmap(Bitmap bitmap, int color) {
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        Bitmap bitmapResult = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bitmapResult);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmapResult;
    }

    public static Bitmap rotateBitmap(Bitmap source, @FloatRange(from = 0, to = 360) float angle) {
        if (angle != 0 || angle != 360) {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        } else {
            return source;
        }
    }

    public static Bitmap createColorBitmap(@ColorInt int color) {
        Bitmap bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(color);
        return bmp;
    }

    public enum ImageType {
        JPEG,
        PNG,
        GIF,
        BMP,
        WEBP,
        UNKNOWN
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
    public static ImageType guessImageType(@NonNull InputStream is) throws IOException {
        // If we can't read ahead safely, just give up on guessing
        if (!is.markSupported())
            return ImageType.UNKNOWN;

        is.mark(16);
        int c1 = is.read();
        int c2 = is.read();
        int c3 = is.read();
        int c4 = is.read();
        int c5 = is.read();
        int c6 = is.read();
        int c7 = is.read();
        int c8 = is.read();
        int c9 = is.read();
        int c10 = is.read();
        int c11 = is.read();
        int c12 = is.read();
        is.reset();

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
                    (c7 == 'E' && c8 == 'x' && c9 == 'i' && c10 == 'f' &&
                            c11 == 0)) {
                return ImageType.JPEG;
            }
        }

        if (c1 == 0x89 && c2 == 0x50 && c3 == 0x4e &&
                c4 == 0x47 && c5 == 0x0d && c6 == 0x0a &&
                c7 == 0x1a && c8 == 0x0a) {
            return ImageType.PNG;
        }

        // 0x47 0x49 0x46
        if (c1 == 'G' && c2 == 'I' && c3 == 'F' && c4 == '8') {
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
}
