package com.thewizrd.simpleweather.controls.graphs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;

public abstract class BaseGraphHorizontalScrollView<T extends GraphData<? extends GraphDataSet<? extends GraphEntry>>> extends HorizontalScrollView implements IGraph {
    private HorizontalScrollView mScrollViewer;
    protected final RectF visibleRect = new RectF();
    private BaseGraphView<?> graph;
    private OnClickListener onClickListener;
    private boolean mScrollEnabled = true;

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param v          The view whose scroll position has changed.
         * @param scrollX    Current horizontal scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         */
        void onScrollChange(BaseGraphHorizontalScrollView v, int scrollX, int oldScrollX);
    }

    private OnScrollChangeListener mOnScrollChangeListener;

    public interface OnSizeChangedListener {
        void onSizeChanged(BaseGraphHorizontalScrollView v, int canvasWidth);
    }

    private OnSizeChangedListener mOnSizeChangedListener;

    public BaseGraphHorizontalScrollView(Context context) {
        super(context);
        initialize(context);
    }

    public BaseGraphHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public BaseGraphHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BaseGraphHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        graph = createGraphView(context);
        graph.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null)
                    onClickListener.onClick(v);
            }
        });

        this.setFillViewport(false);
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.setOverScrollMode(View.OVER_SCROLL_NEVER);

        this.removeAllViews();
        final ViewGroup innerLayout = new LinearLayout(getContext());
        LayoutParams innerParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        this.addView(innerLayout, innerParams);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        innerLayout.addView(graph, layoutParams);

        mScrollViewer = this;
    }

    @NonNull
    public abstract BaseGraphView<?> createGraphView(@NonNull Context context);

    @NonNull
    public BaseGraphView<?> getGraph() {
        return graph;
    }

    @NonNull
    protected final HorizontalScrollView getScrollViewer() {
        return mScrollViewer;
    }

    @Nullable
    public OnClickListener getOnClickListener() {
        return onClickListener;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Nullable
    public OnSizeChangedListener getOnSizeChangedListener() {
        return mOnSizeChangedListener;
    }

    public void setOnSizeChangedListener(@Nullable OnSizeChangedListener l) {
        mOnSizeChangedListener = l;
    }

    @Nullable
    public OnScrollChangeListener getOnScrollChangeListener() {
        return mOnScrollChangeListener;
    }

    /**
     * Register a callback to be invoked when the scroll X of
     * this view change.
     * <p>This version of the method works on all versions of Android, back to API v4.</p>
     *
     * @param l The listener to notify when the scroll X position changes.
     * @see android.view.View#getScrollX()
     */
    public void setOnScrollChangedListener(@Nullable OnScrollChangeListener l) {
        mOnScrollChangeListener = l;
    }

    public final void setGraphMaxWidth(@Px int maxWidth) {
        this.graph.setMaxWidth(maxWidth);
    }

    @Override
    protected void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY);
        visibleRect.set(scrollX, scrollY, scrollX + this.getWidth(), scrollY + this.getHeight());
        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener.onScrollChange(this, scrollX, oldScrollX);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        visibleRect.setEmpty();
        this.graph.invalidate();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Invalidate the visible rect
        visibleRect.setEmpty();
    }

    public int getExtentWidth() {
        return computeHorizontalScrollRange();
    }

    public int getViewportWidth() {
        return getMeasuredWidth();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setOnTouchListener(OnTouchListener l) {
        this.graph.setOnTouchListener(l);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            return mScrollEnabled && super.onTouchEvent(ev);
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mScrollEnabled && super.onInterceptTouchEvent(ev);
    }

    public boolean isScrollingEnabled() {
        return mScrollEnabled;
    }

    public void setScrollingEnabled(boolean enabled) {
        mScrollEnabled = enabled;
    }

    public final int getItemPositionFromPoint(float xCoordinate) {
        return this.graph.getItemPositionFromPoint(xCoordinate);
    }

    public GraphData<? extends GraphDataSet<? extends GraphEntry>> getData() {
        return getGraph().mData;
    }

    public abstract void setData(T data);

    public final void resetData(boolean invalidate) {
        getGraph().resetData(invalidate);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            if (child.getVisibility() != GONE) {
                final int parentLeft = getPaddingLeft();
                final int parentRight = r - l - getPaddingRight();

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                if (width < getMeasuredWidth()) {
                    int childLeft;
                    int childTop = child.getTop();

                    // Adjust child to center
                    childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                            lp.leftMargin - lp.rightMargin;

                    child.layout(childLeft, childTop, childLeft + width, childTop + height);
                }
            }
        }
    }
}
