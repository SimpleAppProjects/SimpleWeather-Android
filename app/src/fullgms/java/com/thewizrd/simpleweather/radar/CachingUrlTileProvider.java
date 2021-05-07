package com.thewizrd.simpleweather.radar;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.thewizrd.shared_resources.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;

public abstract class CachingUrlTileProvider implements TileProvider {
    private final int mTileWidth;
    private final int mTileHeight;
    private final RequestManager mGlide;

    public CachingUrlTileProvider(Context ctx, int mTileWidth, int mTileHeight) {
        this.mTileWidth = mTileWidth;
        this.mTileHeight = mTileHeight;

        mGlide = Glide.with(ctx)
                .setDefaultRequestOptions(
                        RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                                .dontTransform().skipMemoryCache(false));
    }

    @Override
    public Tile getTile(int x, int y, int z) {
        byte[] tileImage = getTileImage(x, y, z);
        if (tileImage != null) {
            return new Tile(mTileWidth / 2, mTileHeight / 2, tileImage);
        }
        return NO_TILE;
    }

    /**
     * Synchronously loads the requested Tile image either from cache or from the web.</p>
     * Background threading/pooling is done by the google maps api so we can do it all synchronously.
     *
     * @param x x coordinate of the tile
     * @param y y coordinate of the tile
     * @param z the zoom level
     * @return byte data of the image or <i>null</i> if the image could not be loaded.
     */
    private byte[] getTileImage(int x, int y, int z) {
        FutureTarget<Bitmap> bmpRequest = mGlide.asBitmap()
                .load(getTileUrl(x, y, z))
                .submit();

        Bitmap bitmap = null;
        try {
            bitmap = bmpRequest.get();
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        if (bitmap == null) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * Return the url to your tiles. For example:
     * <pre>
     * public String getTileUrl(int x, int y, int z) {
     * return String.format("https://a.tile.openstreetmap.org/%3$s/%1$s/%2$s.png",x,y,z);
     * }
     * </pre>
     * See <a href="http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames">http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames</a> for more details
     *
     * @param x x coordinate of the tile
     * @param y y coordinate of the tile
     * @param z the zoom level
     * @return the url to the tile specified by the parameters
     */
    public abstract String getTileUrl(int x, int y, int z);
}
