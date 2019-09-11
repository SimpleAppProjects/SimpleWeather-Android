package com.thewizrd.simpleweather.helpers;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LocationPanelOffsetDecoration extends RecyclerView.ItemDecoration {
    protected int mItemOffset;
    protected int offsetFlags;

    public LocationPanelOffsetDecoration(@NonNull Context context, float itemOffsetDp) {
        mItemOffset = (int) ActivityUtils.dpToPx(context, itemOffsetDp);
        this.offsetFlags = OffsetMargin.ALL;
    }

    public LocationPanelOffsetDecoration(@NonNull Context context, float itemOffsetDp, int offsetFlags) {
        mItemOffset = (int) ActivityUtils.dpToPx(context, itemOffsetDp);
        this.offsetFlags = offsetFlags;
    }

    public LocationPanelOffsetDecoration(@NonNull Context context, @DimenRes int itemOffsetId) {
        mItemOffset = context.getResources().getDimensionPixelSize(itemOffsetId);
        this.offsetFlags = OffsetMargin.ALL;
    }

    public LocationPanelOffsetDecoration(@NonNull Context context, @DimenRes int itemOffsetId, int offsetFlags) {
        this(context, itemOffsetId);
        this.offsetFlags = offsetFlags;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int left = offsetFlags == 0 || (offsetFlags & OffsetMargin.LEFT) != 0 ? mItemOffset : 0;
        int right = offsetFlags == 0 || (offsetFlags & OffsetMargin.RIGHT) != 0 ? mItemOffset : 0;
        int top = offsetFlags == 0 || (offsetFlags & OffsetMargin.TOP) != 0 ? mItemOffset : 0;
        int bottom = offsetFlags == 0 || (offsetFlags & OffsetMargin.BOTTOM) != 0 ? mItemOffset : 0;
        outRect.set(left, top, right, bottom); // l,t,r,b
    }
}
