package com.thewizrd.simpleweather.helpers;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class LocationPanelOffsetDecoration extends RecyclerView.ItemDecoration {
    private int mItemOffset;

    public LocationPanelOffsetDecoration(int itemOffset) {
        mItemOffset = itemOffset;
    }

    public LocationPanelOffsetDecoration(@NonNull Context context, @DimenRes int itemOffsetId) {
        this(context.getResources().getDimensionPixelSize(itemOffsetId));
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(mItemOffset / 2, mItemOffset / 2, mItemOffset / 2, mItemOffset / 2); // l,t,r,b
    }
}
