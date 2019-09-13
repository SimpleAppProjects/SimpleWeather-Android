package com.thewizrd.simpleweather.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.simpleweather.R;

/*
 * Based on ItemDecoration implementation from
 * https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete
 */
public class SwipeToDeleteOffSetItemDecoration extends LocationPanelOffsetDecoration
        implements ItemTouchCallbackListener {
    private Drawable mSwipeBackground;
    private Drawable deleteIcon;
    private int iconMargin;
    private float lastTranslationX;

    public SwipeToDeleteOffSetItemDecoration(@NonNull Context context) {
        this(context, 0);
        initialize(context);
    }

    public SwipeToDeleteOffSetItemDecoration(@NonNull Context context, float itemOffsetDp) {
        super(context, itemOffsetDp);
        initialize(context);
    }

    public SwipeToDeleteOffSetItemDecoration(@NonNull Context context, float itemOffsetDp, int offsetFlags) {
        super(context, itemOffsetDp, offsetFlags);
        initialize(context);
    }

    public SwipeToDeleteOffSetItemDecoration(@NonNull Context context, @DimenRes int itemOffsetId) {
        super(context, itemOffsetId);
        initialize(context);
    }

    public SwipeToDeleteOffSetItemDecoration(@NonNull Context context, @DimenRes int itemOffsetId, int offsetFlags) {
        super(context, itemOffsetId, offsetFlags);
        initialize(context);
    }

    private void initialize(@NonNull Context context) {
        mSwipeBackground = ContextCompat.getDrawable(context, R.drawable.swipe_delete);
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white_24dp);
        iconMargin = context.getResources().getDimensionPixelSize(R.dimen.delete_icon_margin);
    }

    /* ItemTouchHelper.Callback Listener */
    /* Used to keep track of last swipe direction to position the delete icon */
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        switch (direction) {
            case ItemTouchHelper.LEFT:
            case ItemTouchHelper.START:
                lastTranslationX = -1;
                break;
            case ItemTouchHelper.RIGHT:
            case ItemTouchHelper.END:
                lastTranslationX = 1;
                break;
            default:
                lastTranslationX = 0;
                break;
        }
    }

    @Override
    public void onDraw(@NonNull final Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        // Only draw if animation is in progress
        if (parent.getItemAnimator() == null || !parent.getItemAnimator().isRunning()) {
            super.onDraw(c, parent, state);
            lastTranslationX = 0;
            return;
        }

        // Some items might be animating up to close the gap left by the removed item
        // Find first child with translationY > 0
        // we're after a rect that is not covered in recycler-view views at this point in time
        View firstViewComingUp = null;

        // this is fixed
        int left = 0;
        int right = parent.getWidth();

        // this we need to find out
        int top = 0;
        int bottom = 0;

        // find relevant translating views
        int childCount = parent.getLayoutManager() != null ? parent.getLayoutManager().getChildCount() : 0;
        for (int i = 0; i < childCount; i++) {
            View child = parent.getLayoutManager().getChildAt(i);
            if (child != null) {
                if (child.getTranslationY() > 0) {
                    // view is coming up
                    firstViewComingUp = child;
                    break;
                }
            }
        }

        if (firstViewComingUp != null) {
            // views are coming up to fill the void
            top = firstViewComingUp.getTop();
            bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();

            // Add ItemDecorator offsets
            int offsetLeft = offsetFlags == 0 || (offsetFlags & OffsetMargin.LEFT) != 0 ? mItemOffset : 0;
            int offsetTop = offsetFlags == 0 || (offsetFlags & OffsetMargin.TOP) != 0 ? mItemOffset : 0;
            int offsetRight = offsetFlags == 0 || (offsetFlags & OffsetMargin.RIGHT) != 0 ? mItemOffset : 0;
            int offsetBottom = offsetFlags == 0 || (offsetFlags & OffsetMargin.BOTTOM) != 0 ? mItemOffset : 0;

            left += offsetLeft;
            bottom -= offsetTop + offsetBottom;
            right -= offsetRight;

            mSwipeBackground.setBounds(left, top, right, bottom);
            mSwipeBackground.draw(c);

            if (lastTranslationX != 0 && bottom > top) {
                int iconLeft;
                int iconRight;
                int iconTop = top + (bottom - top - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (lastTranslationX > 0) {
                    iconLeft = left + iconMargin;
                    iconRight = left + iconMargin + deleteIcon.getIntrinsicWidth();
                } else {
                    iconLeft = right - iconMargin - deleteIcon.getIntrinsicHeight();
                    iconRight = right - iconMargin;
                }

                c.save();

                // Set clip region for icon drawable,
                // So it doesn't appear outside the bounds
                // of the swipe background
                c.clipRect(left, top, right, bottom);

                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(c);

                c.restore();
            }
        }
    }
}
