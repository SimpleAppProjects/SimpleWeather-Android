package com.thewizrd.common.helpers;

import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

public class SimpleGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(@Nullable MotionEvent e) {
        return super.onSingleTapUp(e);
    }

    @Override
    public void onLongPress(@Nullable MotionEvent e) {
        super.onLongPress(e);
    }

    @Override
    public boolean onScroll(@Nullable MotionEvent e1, @Nullable MotionEvent e2, float distanceX, float distanceY) {
        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public boolean onFling(@Nullable MotionEvent e1, @Nullable MotionEvent e2, float velocityX, float velocityY) {
        return super.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public void onShowPress(@Nullable MotionEvent e) {
        super.onShowPress(e);
    }

    @Override
    public boolean onDown(@Nullable MotionEvent e) {
        return super.onDown(e);
    }

    @Override
    public boolean onDoubleTap(@Nullable MotionEvent e) {
        return super.onDoubleTap(e);
    }

    @Override
    public boolean onDoubleTapEvent(@Nullable MotionEvent e) {
        return super.onDoubleTapEvent(e);
    }

    @Override
    public boolean onSingleTapConfirmed(@Nullable MotionEvent e) {
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onContextClick(@Nullable MotionEvent e) {
        return super.onContextClick(e);
    }
}
