package com.thewizrd.simpleweather.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

/**
 * NestedScrollView which provides listener events for fling events (start, stop)
 * and an individual listener for scroll events started from a touch event (press down and scroll then lift)
 * Based on the FlingListener implementation from https://stackoverflow.com/a/19149908, and
 * a strip down version of the scroll event listener from the AOSP Calendar app
 * (https://android.googlesource.com/platform/packages/apps/Calendar/+/refs/heads/pie-release/src/com/android/calendar/DayView.java)
 */
public class ObservableNestedScrollView extends NestedScrollView {
    private static final String TAG = "ObservableScrollView";

    private OnFlingListener mFlingListener;

    public interface OnFlingListener {
        void onFlingStarted(int scrollY, int velocityY);

        void onFlingStopped(int scrollY);
    }

    private OnTouchScrollChangeListener mTouchScrollListener;
    private boolean mOnFlingCalled;
    private int mInitialScrollY;

    /**
     * The initial state of the touch mode when we enter this view.
     */
    private static final int TOUCH_MODE_INITIAL_STATE = 0;

    /**
     * Indicates we just received the touch event and we are waiting to see if
     * it is a tap or a scroll gesture.
     */
    private static final int TOUCH_MODE_DOWN = 1;

    /**
     * Indicates the touch gesture is a scroll
     */
    private static final int TOUCH_MODE_SCROLL = 2;

    private int mTouchMode = TOUCH_MODE_INITIAL_STATE;

    public interface OnTouchScrollChangeListener {
        void onTouchScrollChange(int scrollY, int oldScrollY);
    }

    public ObservableNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public ObservableNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ObservableNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnFlingListener(OnFlingListener listener) {
        mFlingListener = listener;
    }

    public void setTouchScrollListener(OnTouchScrollChangeListener listener) {
        mTouchScrollListener = listener;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void fling(int velocityY) {
        super.fling(velocityY);

        mOnFlingCalled = true;

        if (mFlingListener != null) {
            mFlingListener.onFlingStarted(computeVerticalScrollOffset(), velocityY);
        }
    }

    /**
     * This method gets called when the fling action has settled
     * The parameter type TYPE_NON_TOUCH is passed when the scroller animation is stopped
     * <p>
     * TYPE_NON_TOUCH
     * Indicates that the input type for the gesture is caused by something which is not a user
     * touching a screen. This is usually from a fling which is settling.
     */
    @SuppressLint("RestrictedApi")
    @Override
    public void stopNestedScroll(int type) {
        super.stopNestedScroll(type);

        if (mFlingListener != null && mOnFlingCalled && type == ViewCompat.TYPE_NON_TOUCH) {
            this.postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    mFlingListener.onFlingStopped(computeVerticalScrollOffset());
                }
            });
            mOnFlingCalled = false;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mOnFlingCalled = false;
                mInitialScrollY = computeVerticalScrollOffset();
                mTouchMode = TOUCH_MODE_DOWN;
                break;
            case MotionEvent.ACTION_UP:
                // Fling was called; Scroll event handled by fling listener
                if (mOnFlingCalled) {
                    break;
                }

                if (mTouchMode == TOUCH_MODE_SCROLL) {
                    if (mTouchScrollListener != null) {
                        int startScrollY = mInitialScrollY;
                        int endScrollY = computeVerticalScrollOffset();
                        mTouchScrollListener.onTouchScrollChange(endScrollY, startScrollY);
                    }

                    mTouchMode = TOUCH_MODE_INITIAL_STATE;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchMode == TOUCH_MODE_DOWN && !mOnFlingCalled) {
                    mTouchMode = TOUCH_MODE_SCROLL;
                }
                break;
            default:
                Log.i(TAG, "Unknown action: " + ev.getActionMasked());
                break;
        }

        return result;
    }
}
