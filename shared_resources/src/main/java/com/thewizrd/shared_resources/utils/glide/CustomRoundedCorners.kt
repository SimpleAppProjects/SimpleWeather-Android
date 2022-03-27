package com.thewizrd.shared_resources.utils.glide

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.bumptech.glide.util.Util
import java.nio.ByteBuffer
import java.security.MessageDigest

class CustomRoundedCorners(private val cornerRadius: Float) : BitmapTransformation() {
    companion object {
        private val ID = CustomRoundedCorners::class.java.name
        private val ID_BYTES = ID.toByteArray(CHARSET)
    }

    override fun transform(
        pool: BitmapPool,
        inBitmap: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return TransformationUtils.roundedCorners(
            pool, inBitmap, cornerRadius, cornerRadius, cornerRadius, cornerRadius
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other is CustomRoundedCorners) {
            return cornerRadius == other.cornerRadius
        }
        return false
    }

    override fun hashCode(): Int {
        return Util.hashCode(ID.hashCode(), cornerRadius.hashCode())
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)

        val radiusData = ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(cornerRadius).array()
        messageDigest.update(radiusData)
    }
}