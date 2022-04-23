package com.thewizrd.common.utils.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.util.Util;
import com.thewizrd.shared_resources.utils.Colors;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

/**
 * A {@link BitmapTransformation} which shows an overlay over the bitmap image
 */
public final class TransparentOverlay extends BitmapTransformation {
    private static final String ID = TransparentOverlay.class.getName();
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    private final int overlayAlpha;

    /**
     * @param overlayAlpha the alpha attribute of the overlay
     */
    public TransparentOverlay(@IntRange(from = 0x00, to = 0xFF) int overlayAlpha) {
        this.overlayAlpha = overlayAlpha;
    }

    @Override
    protected Bitmap transform(
            @NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        Bitmap bmp = pool.get(toTransform.getWidth(), toTransform.getHeight(), toTransform.getConfig());
        bmp.setHasAlpha(true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Colors.BLACK);
        paint.setAlpha(overlayAlpha);
        canvas.drawBitmap(toTransform, 0, 0, null);
        canvas.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paint);
        return bmp;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TransparentOverlay) {
            TransparentOverlay other = (TransparentOverlay) o;
            return overlayAlpha == other.overlayAlpha;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Util.hashCode(ID.hashCode(),
                Util.hashCode(overlayAlpha));
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);

        byte[] overlayData = ByteBuffer.allocate(4).putInt(overlayAlpha).array();
        messageDigest.update(overlayData);
    }
}
