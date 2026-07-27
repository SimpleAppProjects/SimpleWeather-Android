/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thewizrd.simpleweather.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ListViewCompat;

import com.google.android.material.loadingindicator.LoadingIndicator;
import com.thewizrd.shared_resources.utils.ContextUtils;
import com.thewizrd.simpleweather.R;

/**
 * Copy of SwipeRefreshLayout which uses Material LoadingIndicator as the progress drawable
 */
public class LoadingIndicatorRefreshLayout extends ViewGroup implements NestedScrollingParent3,
        NestedScrollingParent2, NestedScrollingChild3, NestedScrollingChild2, NestedScrollingParent,
        NestedScrollingChild {

    @VisibleForTesting
    private static final int CIRCLE_DIAMETER = 48;

    private static final String LOG_TAG = LoadingIndicatorRefreshLayout.class.getSimpleName();

    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 64;

    private View mTarget; // the target of the gesture
    private OnRefreshListener mListener;
    private boolean mRefreshing = false;
    private final int mTouchSlop;
    private float mTotalDragDistance = -1;

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];

    // Used for calls from old versions of onNestedScroll to v3 version of onNestedScroll. This only
    // exists to prevent GC costs that are present before API 21.
    private final int[] mNestedScrollingV2ConsumedCompat = new int[2];
    private boolean mNestedScrollInProgress;

    private final int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;

    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    // Whether this item is scaled up rather than clipped
    private boolean mScale = true;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };

    private AnimationFrameLayout mCircleView;
    private LoadingIndicator mLoadingIndicator;
    private ImageView mStaticLoadingView;

    private int mShowDelay = 0;
    private int mHideDelay = 0;

    private int mCircleViewIndex = -1;

    protected int mFrom;

    private float mStartingScale;

    protected int mOriginalOffsetTop;

    private final int mSpinnerOffsetEnd;

    private Animation mScaleAnimation;

    private Animation mScaleDownAnimation;

    private Animation mAlphaStartAnimation;

    private Animation mAlphaMaxAnimation;

    private Animation mScaleDownToStartAnimation;

    private boolean mNotify;

    private final int mCircleDiameter;

    private final AnimationListener mRefreshListener = new AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                setProgressAlpha(MAX_ALPHA);
                showLoadingIndicator();
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
                mCurrentTargetOffsetTop = mCircleView.getTop();
            } else {
                reset();
            }
        }
    };

    private void showLoadingIndicator() {
        mStaticLoadingView.setVisibility(View.INVISIBLE);
        mLoadingIndicator.show();
    }

    private void hideLoadingIndicator() {
        mStaticLoadingView.setVisibility(View.VISIBLE);
        mLoadingIndicator.hide();
    }

    void reset() {
        mCircleView.clearAnimation();
        hideLoadingIndicator();
        mCircleView.setVisibility(View.GONE);
        setColorViewAlpha(MAX_ALPHA);
        // Return the circle to its start position
        if (mScale) {
            setAnimationProgress(0 /* animation complete and view is hidden */);
        } else {
            setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop);
        }
        mCurrentTargetOffsetTop = mCircleView.getTop();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
        }
    }

    static class SavedState extends BaseSavedState {
        final boolean mRefreshing;

        /**
         * Constructor called from {@link LoadingIndicatorRefreshLayout#onSaveInstanceState()}
         */
        SavedState(Parcelable superState, boolean refreshing) {
            super(superState);
            this.mRefreshing = refreshing;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        SavedState(Parcel in) {
            super(in);
            mRefreshing = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte(mRefreshing ? (byte) 1 : (byte) 0);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mRefreshing);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setRefreshing(savedState.mRefreshing);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }

    private void setColorViewAlpha(int targetAlpha) {
        mCircleView.getBackground().setAlpha(targetAlpha);
        setProgressAlpha(targetAlpha);
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public LoadingIndicatorRefreshLayout(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    @SuppressLint("RestrictedApi")
    public LoadingIndicatorRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);

        createProgressView();
        setChildrenDrawingOrderEnabled(true);
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerOffsetEnd = (int) (DEFAULT_CIRCLE_TARGET * metrics.density);
        mTotalDragDistance = mSpinnerOffsetEnd;
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        mOriginalOffsetTop = mCurrentTargetOffsetTop = -mCircleDiameter;
        moveToStart(1.0f);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mCircleViewIndex;
        } else if (i >= mCircleViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    private void createProgressView() {
        final int containerWidth = (int) ContextUtils.dpToPx(getContext(), 48);
        final int containerHeight = (int) ContextUtils.dpToPx(getContext(), 48);
        final int indicatorSize = (int) ContextUtils.dpToPx(getContext(), 34);

        mCircleView = new AnimationFrameLayout(getContext());
        mCircleView.setLayoutParams(new LayoutParams(containerWidth, containerHeight));
        mCircleView.setBackground(new ShapeDrawable(new OvalShape()));
        mCircleView.setBackgroundTintList(ColorStateList.valueOf(ContextUtils.getAttrColor(getContext(), com.google.android.material.R.attr.colorPrimaryContainer)));
        mCircleView.setClipToOutline(true);

        mLoadingIndicator = new LoadingIndicator(getContext());
        mLoadingIndicator.setIndicatorColor(ContextUtils.getAttrColor(getContext(), androidx.appcompat.R.attr.colorPrimary));
        mLoadingIndicator.setContainerWidth(containerWidth);
        mLoadingIndicator.setContainerHeight(containerHeight);
        mLoadingIndicator.setIndicatorSize(indicatorSize);
        mLoadingIndicator.hide();
        mLoadingIndicator.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        mStaticLoadingView = new AppCompatImageView(getContext());
        mStaticLoadingView.setImageResource(R.drawable.loading_indicator);
        mStaticLoadingView.setImageTintList(ColorStateList.valueOf(ContextUtils.getAttrColor(getContext(), androidx.appcompat.R.attr.colorPrimary)));
        mStaticLoadingView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mStaticLoadingView.setAdjustViewBounds(true);
        mStaticLoadingView.setLayoutParams(new FrameLayout.LayoutParams(indicatorSize, indicatorSize, Gravity.CENTER));

        mCircleView.addView(mLoadingIndicator);
        mCircleView.addView(mStaticLoadingView);

        addView(mCircleView);
    }

    public void setContainerColor(@ColorInt int containerColor) {
        mCircleView.setBackgroundTintList(ColorStateList.valueOf(containerColor));
    }

    public void setIndicatorColor(@ColorInt int indicatorColor) {
        mLoadingIndicator.setIndicatorColor(indicatorColor);
        mStaticLoadingView.setImageTintList(ColorStateList.valueOf(indicatorColor));
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(@Nullable OnRefreshListener listener) {
        mListener = listener;
    }

    private Runnable mRefreshRunnable = null;

    public void setShowDelay(@IntRange(from = 0, to = 1000) int delay) {
        mShowDelay = Math.min(delay, 1000);
    }

    public void setHideDelay(@IntRange(from = 0, to = 1000) int delay) {
        mHideDelay = Math.min(delay, 1000);
    }

    public void setRefreshingDelay(@IntRange(from = 0, to = 1000) int delay) {
        mShowDelay = mHideDelay = Math.min(delay, 1000);
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (mRefreshRunnable != null) {
            removeCallbacks(mRefreshRunnable);
        }

        int delay = refreshing ? mShowDelay : mHideDelay;

        Runnable runnable = () -> {
            if (refreshing && mRefreshing != refreshing) {
                // scale and show
                mRefreshing = refreshing;
                int endTarget = mSpinnerOffsetEnd + mOriginalOffsetTop;
                setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop);
                mNotify = false;
                startScaleUpAnimation(mRefreshListener);
            } else {
                setRefreshing(refreshing, false /* notify */);
            }
        };

        if (delay > 0) {
            postDelayed(runnable, delay);
            mRefreshRunnable = runnable;
        } else {
            runnable.run();
            mRefreshRunnable = null;
        }
    }

    private void startScaleUpAnimation(AnimationListener listener) {
        mCircleView.setVisibility(View.VISIBLE);
        setProgressAlpha(MAX_ALPHA);
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     *
     * @param progress
     */
    void setAnimationProgress(float progress) {
        mCircleView.setScaleX(progress);
        mCircleView.setScaleY(progress);
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
            } else {
                startScaleDownAnimation(mRefreshListener);
            }
        }
    }

    void startScaleDownAnimation(AnimationListener listener) {
        mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mCircleView.setAnimationListener(listener);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownAnimation);
    }

    private void startProgressAlphaStartAnimation() {
        mAlphaStartAnimation = startAlphaAnimation((int) (mCircleView.getAlpha() * 255), STARTING_PROGRESS_ALPHA);
    }

    private int progressAlpha = 255;

    private void setProgressAlpha(int alpha) {
        progressAlpha = alpha;
        mStaticLoadingView.setImageAlpha(alpha);
        mLoadingIndicator.setAlpha(alpha / 255f);
    }

    private void startProgressAlphaMaxAnimation() {
        mAlphaMaxAnimation = startAlphaAnimation(progressAlpha, MAX_ALPHA);
    }

    private Animation startAlphaAnimation(final int startingAlpha, final int endingAlpha) {
        Animation alpha = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setProgressAlpha((int) (startingAlpha + ((endingAlpha - startingAlpha) * interpolatedTime)));
            }
        };
        alpha.setDuration(ALPHA_ANIMATION_DURATION);
        // Clear out the previous animation listeners.
        mCircleView.setAnimationListener(null);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(alpha);
        return alpha;
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mCircleView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        int circleWidth = mCircleView.getMeasuredWidth();
        int circleHeight = mCircleView.getMeasuredHeight();
        mCircleView.layout((width / 2 - circleWidth / 2), mCurrentTargetOffsetTop,
                (width / 2 + circleWidth / 2), mCurrentTargetOffsetTop + circleHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        mCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleDiameter, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleDiameter, MeasureSpec.EXACTLY));
        mCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mCircleView) {
                mCircleViewIndex = index;
                break;
            }
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (mTarget instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) mTarget, -1);
        }
        return mTarget.canScrollVertically(-1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = ev.getActionMasked();
        int pointerIndex;

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp()
                || mRefreshing || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCircleView.getTop());
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialDownY = ev.getY(pointerIndex);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                startDragging(y);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    // NestedScrollingParent 3

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, @ViewCompat.NestedScrollType int type,
                               @NonNull int[] consumed) {
        if (type != ViewCompat.TYPE_TOUCH) {
            return;
        }

        // This is a bit of a hack. onNestedScroll is typically called up the hierarchy of nested
        // scrolling parents/children, where each consumes distances before passing the remainder
        // to parents.  In our case, we want to try to run after children, and after parents, so we
        // first pass scroll distances to parents and consume after everything else has.
        int consumedBeforeParents = consumed[1];
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow, type, consumed);
        int consumedByParents = consumed[1] - consumedBeforeParents;
        int unconsumedAfterParents = dyUnconsumed - consumedByParents;

        // There are two reasons why scroll distance may be totally consumed.  1) All of the nested
        // scrolling parents up the hierarchy implement NestedScrolling3 and consumed all of the
        // distance or 2) at least 1 nested scrolling parent doesn't implement NestedScrolling3 and
        // for comparability reasons, we are supposed to act like they have.
        //
        // We must assume 2) is the case because we have no way of determining that it isn't, and
        // therefore must fallback to a previous hack that was done before nested scrolling 3
        // existed.
        int remainingDistanceToScroll;
        if (unconsumedAfterParents == 0) {
            // The previously implemented hack is to see how far we were offset and assume that that
            // distance is equal to how much all of our parents consumed.
            remainingDistanceToScroll = dyUnconsumed + mParentOffsetInWindow[1];
        } else {
            remainingDistanceToScroll = unconsumedAfterParents;
        }

        // Not sure why we have to make sure the child can't scroll up... but seems dangerous to
        // remove.
        if (remainingDistanceToScroll < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(remainingDistanceToScroll);
            moveSpinner(mTotalUnconsumed);

            // If we've gotten here, we need to consume whatever is left to consume, which at this
            // point is either equal to 0, or remainingDistanceToScroll.
            consumed[1] += unconsumedAfterParents;
        }
    }

    // NestedScrollingParent 2

    @Override
    public boolean onStartNestedScroll(View child, View target, int axes, int type) {
        if (type == ViewCompat.TYPE_TOUCH) {
            return onStartNestedScroll(child, target, axes);
        } else {
            return false;
        }
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes, int type) {
        // Should always be true because onStartNestedScroll returns false for all type !=
        // ViewCompat.TYPE_TOUCH, but check just in case.
        if (type == ViewCompat.TYPE_TOUCH) {
            onNestedScrollAccepted(child, target, axes);
        }
    }

    @Override
    public void onStopNestedScroll(View target, int type) {
        // Should always be true because onStartNestedScroll returns false for all type !=
        // ViewCompat.TYPE_TOUCH, but check just in case.
        if (type == ViewCompat.TYPE_TOUCH) {
            onStopNestedScroll(target);
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed, int type) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type,
                mNestedScrollingV2ConsumedCompat);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
        // Should always be true because onStartNestedScroll returns false for all type !=
        // ViewCompat.TYPE_TOUCH, but check just in case.
        if (type == ViewCompat.TYPE_TOUCH) {
            onNestedPreScroll(target, dx, dy, consumed);
        }
    }

    // NestedScrollingParent 1

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && !mReturningToStart && !mRefreshing
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveSpinner(mTotalUnconsumed);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                ViewCompat.TYPE_TOUCH, mNestedScrollingV2ConsumedCompat);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    // NestedScrollingChild 3

    @Override
    public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                     int dyUnconsumed, @Nullable int[] offsetInWindow, @ViewCompat.NestedScrollType int type,
                                     @NonNull int[] consumed) {
        if (type == ViewCompat.TYPE_TOUCH) {
            mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
                    dyUnconsumed, offsetInWindow, type, consumed);
        }
    }

    // NestedScrollingChild 2

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return type == ViewCompat.TYPE_TOUCH && startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll(int type) {
        if (type == ViewCompat.TYPE_TOUCH) {
            stopNestedScroll();
        }
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return type == ViewCompat.TYPE_TOUCH && hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow, int type) {
        return type == ViewCompat.TYPE_TOUCH && mNestedScrollingChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow,
                                           int type) {
        return type == ViewCompat.TYPE_TOUCH && dispatchNestedPreScroll(dx, dy, consumed,
                offsetInWindow);
    }

    // NestedScrollingChild 1

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    private void moveSpinner(float overscrollTop) {
        float originalDragPercent = overscrollTop / mTotalDragDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
        float slingshotDist = mSpinnerOffsetEnd;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = slingshotDist * tensionPercent * 2;

        int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
        // where 1.0f is a full circle
        if (!mCircleView.isShown()) {
            mCircleView.setVisibility(View.VISIBLE);
        }
        if (!mScale) {
            mCircleView.setScaleX(1f);
            mCircleView.setScaleY(1f);
        }

        if (mScale) {
            setAnimationProgress(Math.min(1f, overscrollTop / mTotalDragDistance));
        }
        if (overscrollTop < mTotalDragDistance) {
            if (progressAlpha > STARTING_PROGRESS_ALPHA
                    && !isAnimationRunning(mAlphaStartAnimation)) {
                // Animate the alpha
                startProgressAlphaStartAnimation();
            }
        } else {
            if (progressAlpha < MAX_ALPHA && !isAnimationRunning(mAlphaMaxAnimation)) {
                // Animate the alpha
                startProgressAlphaMaxAnimation();
            }
        }
        float strokeStart = adjustedPercent * .8f;
        //mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
        //mProgress.setArrowScale(Math.min(1f, adjustedPercent));

        float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
        mStaticLoadingView.setRotation(rotation * 360f);
        setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop);
    }

    private void finishSpinner(float overscrollTop) {
        if (overscrollTop > mTotalDragDistance) {
            setRefreshing(true, true /* notify */);
        } else {
            // cancel refresh
            mRefreshing = false;
            //mProgress.setStartEndTrim(0f, 0f);
            AnimationListener listener = null;
            if (!mScale) {
                listener = new AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (!mScale) {
                            startScaleDownAnimation(null);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                };
            }
            animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
            //mProgress.setArrowEnabled(false);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        int pointerIndex = -1;

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp()
                || mRefreshing || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                startDragging(y);

                if (mIsBeingDragged) {
                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                    if (overscrollTop > 0) {
                        // While the spinner is being dragged down, our parent shouldn't try
                        // to intercept touch events. It will stop the drag gesture abruptly.
                        getParent().requestDisallowInterceptTouchEvent(true);
                        moveSpinner(overscrollTop);
                    } else {
                        return false;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                pointerIndex = ev.getActionIndex();
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG,
                            "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }

                if (mIsBeingDragged) {
                    final float y = ev.getY(pointerIndex);
                    final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                    mIsBeingDragged = false;
                    finishSpinner(overscrollTop);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return true;
    }

    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        if (yDiff > mTouchSlop && !mIsBeingDragged) {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;
            setProgressAlpha(STARTING_PROGRESS_ALPHA);
        }
    }

    private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mCircleView.setAnimationListener(listener);
            }
            mCircleView.clearAnimation();
            mCircleView.startAnimation(mAnimateToStartPosition);
        }
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int endTarget = mSpinnerOffsetEnd - Math.abs(mOriginalOffsetTop);
            int targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mCircleView.getTop();
            setTargetOffsetTopAndBottom(offset);
            //mProgress.setArrowScale(1 - interpolatedTime);
        }
    };

    void moveToStart(float interpolatedTime) {
        int targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - mCircleView.getTop();
        setTargetOffsetTopAndBottom(offset);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(int from,
                                                      AnimationListener listener) {
        mFrom = from;
        mStartingScale = mCircleView.getScaleX();
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownToStartAnimation);
    }

    void setTargetOffsetTopAndBottom(int offset) {
        mCircleView.bringToFront();
        ViewCompat.offsetTopAndBottom(mCircleView, offset);
        mCurrentTargetOffsetTop = mCircleView.getTop();
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        /**
         * Called when a swipe gesture triggers a refresh.
         */
        void onRefresh();
    }
}
