package com.thewizrd.simpleweather;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerTouchListener extends RecyclerView.SimpleOnItemTouchListener {
    private View.OnClickListener clickListener;
    private GestureDetector gestureDetector;

    public RecyclerTouchListener(Context context, View.OnClickListener listener) {
        clickListener = listener;
        gestureDetector = new GestureDetector(context, new GestureDetect());
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View child = rv.findChildViewUnder(e.getX(), e.getY());

        if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
            clickListener.onClick(child);
            return true;
        }
        return false;
    }

    private class GestureDetect extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    }
}
