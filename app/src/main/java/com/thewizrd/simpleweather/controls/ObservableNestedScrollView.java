package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.widget.NestedScrollView;

/**
 * NestedScrollView which provides listener events for fling events (start, stop)
 * and an individual listener for scroll events started from a touch event (press down and scroll then lift)
 * Based on the FlingListener implementation from https://stackoverflow.com/a/19149908, and
 * a strip down version of the scroll event listener from the AOSP Calendar app
 * (https://android.googlesource.com/platform/packages/apps/Calendar/+/refs/heads/pie-release/src/com/android/calendar/DayView.java)
 */
public class ObservableNestedScrollView extends NestedScrollView {
    private GestureDetectorCompat mGestureDetector;

    private OnFlingListener mFlingListener;
    private Runnable mFlingChecker;

    public interface OnFlingListener {
        void onFlingStarted(int scrollY, int velocityY);

        void onFlingStopped(int scrollY);
    }

    private OnTouchScrollChangeListener mTouchScrollListener;
    private boolean mStartingScroll;
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
        initialize(context);
    }

    public ObservableNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ObservableNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        mFlingChecker = new Runnable() {
            private int mPreviousPosition;

            @Override
            public void run() {
                int position = computeVerticalScrollOffset();
                if (mPreviousPosition - position == 0) {
                    mFlingListener.onFlingStopped(position);
                    removeCallbacks(mFlingChecker);
                } else {
                    mPreviousPosition = computeVerticalScrollOffset();
                    postOnAnimation(mFlingChecker);
                }
            }
        };

        mGestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Vertical fling.
                mTouchMode = TOUCH_MODE_INITIAL_STATE;
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {
                boolean result = super.onScroll(e1, e2, deltaX, deltaY);

                if (mStartingScroll) {
                    mInitialScrollY = computeVerticalScrollOffset();
                    mStartingScroll = false;
                }

                if (mTouchMode == TOUCH_MODE_DOWN) {
                    mTouchMode = TOUCH_MODE_SCROLL;
                }

                return result;
            }
        });
    }

    public void setOnFlingListener(OnFlingListener listener) {
        mFlingListener = listener;
    }

    public void setTouchScrollListener(OnTouchScrollChangeListener listener) {
        mTouchScrollListener = listener;
    }

    @Override
    public void fling(int velocityY) {
        super.fling(velocityY);

        mOnFlingCalled = true;

        if (mFlingListener != null) {
            mFlingListener.onFlingStarted(computeVerticalScrollOffset(), velocityY);
            postOnAnimation(mFlingChecker);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mOnFlingCalled = false;
                mStartingScroll = true;
                mTouchMode = TOUCH_MODE_DOWN;
                break;
            case MotionEvent.ACTION_UP:
                mStartingScroll = false;

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
                Log.i("ObservableScrollView", "Unknown action: " + ev.getActionMasked());
                break;
        }

        return result;
    }
}
