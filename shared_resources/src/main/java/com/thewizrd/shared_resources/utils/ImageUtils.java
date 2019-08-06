package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.thewizrd.shared_resources.R;

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

    public static Bitmap weatherIconToBitmap(Context context, String text, int textSize) {
        return weatherIconToBitmap(context, text, textSize, Color.WHITE);
    }

    public static Bitmap weatherIconToBitmap(Context context, String text, int textSize, boolean addShadow) {
        return weatherIconToBitmap(context, text, textSize, Color.WHITE, addShadow);
    }

    public static Bitmap weatherIconToBitmap(Context context, String text, int textSize, int textColor) {
        return weatherIconToBitmap(context, text, textSize, textColor, false);
    }

    public static Bitmap weatherIconToBitmap(Context context, String text, int textSize, int textColor, boolean addShadow) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface weathericons = ResourcesCompat.getFont(context, R.font.weathericons);
        paint.setSubpixelText(true);
        paint.setTypeface(weathericons);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.LEFT);
        if (addShadow) {
            paint.setShadowLayer(1, 1, 1, Color.parseColor("#000000"));
        }

        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas myCanvas = new Canvas(bmp);
        myCanvas.drawText(text, 0, baseline, paint);

        return bmp;
    }

    public static Bitmap bitmapFromDrawable(Context context, int resDrawable) {
        Drawable drawable = ContextCompat.getDrawable(context, resDrawable);
        return bitmapFromDrawable(drawable);
    }

    public static Bitmap bitmapFromDrawable(Context context, Drawable drawable) {
        return bitmapFromDrawable(drawable);
    }

    public static Bitmap tintedBitmapFromDrawable(Context context, int resDrawable, int color) {
        Drawable drawable = ContextCompat.getDrawable(context, resDrawable);
        Drawable wrapped = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrapped, color);
        return bitmapFromDrawable(wrapped);
    }

    private static Bitmap bitmapFromDrawable(Drawable drawable) {
        Bitmap bitmap;

        if (drawable.getIntrinsicHeight() <= 0 || drawable.getIntrinsicWidth() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
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

    public class CenterCropper {
        private int width = 0;
        private int height = 0;

        /**
         * @param newWidth  destination width
         * @param newHeight destination height
         */
        public CenterCropper(int newWidth, int newHeight) {
            width = newWidth;
            height = newHeight;
        }

        /**
         * Scales and center-crops a bitmap to the size passed in and returns the new bitmap.
         *
         * @param source Bitmap to scale and center-crop
         * @return Bitmap scaled and center-cropped bitmap
         */
        public Bitmap process(Bitmap source) {
            Bitmap dest = null;
            try {
                int sourceWidth = source.getWidth();
                int sourceHeight = source.getHeight();

                // Compute the scaling factors to fit the new height and width, respectively.
                // To cover the final image, the final scaling will be the bigger
                // of these two.
                float xScale = (float) width / sourceWidth;
                float yScale = (float) height / sourceHeight;
                float scale = Math.max(xScale, yScale);

                // Now get the size of the source bitmap when scaled
                float scaledWidth = scale * sourceWidth;
                float scaledHeight = scale * sourceHeight;

                // Let's find out the upper left coordinates if the scaled bitmap
                // should be centered in the new size give by the parameters
                float left = (width - scaledWidth) / 2;
                float top = (height - scaledHeight) / 2;

                // The target rectangle for the new, scaled version of the source bitmap will now
                // be
                RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

                // Finally, we create a new bitmap of the specified size and draw our new,
                // scaled bitmap onto it.
                dest = Bitmap.createBitmap(width, height, source.getConfig());
                Canvas canvas = new Canvas(dest);
                canvas.drawBitmap(source, null, targetRect, null);
            } catch (Exception ex) {
                dest = null;
                Logger.writeLine(Log.ERROR, ex);
            } finally {
                if (dest == null)
                    dest = source;
            }

            return dest;
        }
    }

    public static class GlideBitmapViewTarget extends BitmapImageViewTarget {
        public interface OnLoadListener {
            void onLoad();
        }

        private OnLoadListener onLoadListener;
        private Handler mMainHandler;

        public GlideBitmapViewTarget(ImageView view, OnLoadListener listener) {
            super(view);
            onLoadListener = listener;
            mMainHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onLoadFailed(@Nullable final Drawable errorDrawable) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    GlideBitmapViewTarget.super.onLoadFailed(errorDrawable);
                    if (onLoadListener != null)
                        onLoadListener.onLoad();
                }
            });
        }

        @Override
        public void setDrawable(final Drawable drawable) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    GlideBitmapViewTarget.super.setDrawable(drawable);
                    if (onLoadListener != null)
                        onLoadListener.onLoad();
                }
            });
        }

        @Override
        protected void setResource(final Bitmap resource) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    GlideBitmapViewTarget.super.setResource(resource);
                    if (onLoadListener != null)
                        onLoadListener.onLoad();
                }
            });
        }
    }
}
