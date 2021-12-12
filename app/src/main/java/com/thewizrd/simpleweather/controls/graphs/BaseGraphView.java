package com.thewizrd.simpleweather.controls.graphs;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

import com.thewizrd.shared_resources.utils.ContextUtils;

import java.util.ArrayList;
import java.util.Stack;

public abstract class BaseGraphView<T extends GraphData<? extends GraphDataSet<? extends GraphEntry>>> extends View implements IGraph {
    protected T mData;
    private int mMaxXEntries;

    protected int mViewHeight;
    protected int mViewWidth;
    private int mMaxWidth = -1;

    // Containers to check if we're drawing w/in bounds
    protected final RectF drawingRect = new RectF();

    protected final ArrayList<Float> xCoordinateList;
    protected int horizontalGridNum;
    protected int verticalGridNum;
    protected final int MIN_HORIZONTAL_GRID_NUM = 1;

    protected float sideLineLength = 0;
    protected float backgroundGridWidth = ContextUtils.dpToPx(getContext(), 45);

    public BaseGraphView(Context context) {
        this(context, null);
    }

    public BaseGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        xCoordinateList = new ArrayList<>();
    }

    public final void setData(T data) {
        this.mData = data;
        notifyDataSetChanged();
        updateGraph();
    }

    public final boolean isDataEmpty() {
        return mData == null || mData.isEmpty();
    }

    public final int getDataCount() {
        return (mData != null ? mData.getDataCount() : 0);
    }

    protected int getMaxEntryCount() {
        return mMaxXEntries;
    }

    @CallSuper
    protected void resetData(boolean invalidate) {
        mData = null;
        this.xCoordinateList.clear();
        removeAnimatedDrawables();
        if (invalidate) {
            this.postInvalidate();
        }
    }

    public abstract void updateGraph();

    protected void notifyDataSetChanged() {
        calcMaxX();
    }

    private void calcMaxX() {
        int count = 0;

        if (mData != null && !mData.isEmpty()) {
            for (GraphDataSet set : mData.getDataSets()) {
                if (set.getDataCount() > count) {
                    count = set.getDataCount();
                }
            }
        }

        mMaxXEntries = count;
    }

    protected void updateHorizontalGridNum() {
        horizontalGridNum = Math.max(horizontalGridNum,
                Math.max(MIN_HORIZONTAL_GRID_NUM, getMaxEntryCount() - 1));
    }

    @Override
    public int getItemPositionFromPoint(float xCoordinate) {
        if (horizontalGridNum <= 1) {
            return 0;
        }

        return binarySearchPointIndex(xCoordinate);
    }

    protected int binarySearchPointIndex(float targetXPoint) {
        int l = 0;
        int r = xCoordinateList.size() - 1;
        while (l <= r) {
            int midPt = (int) Math.floor((l + r) / 2f);
            if (targetXPoint > (xCoordinateList.get(midPt) - backgroundGridWidth / 2f) && targetXPoint < (xCoordinateList.get(midPt) + backgroundGridWidth / 2f)) {
                return midPt;
            } else if (targetXPoint <= xCoordinateList.get(midPt) - backgroundGridWidth / 2f) {
                r = midPt - 1;
            } else if (targetXPoint >= xCoordinateList.get(midPt) + backgroundGridWidth / 2f) {
                l = midPt + 1;
            }
        }

        return 0;
    }

    @Override
    @CallSuper
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mViewWidth = measureWidth(widthMeasureSpec);
        mViewHeight = measureHeight(heightMeasureSpec);
        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    protected int getPreferredWidth() {
        return getGraphExtentWidth();
    }

    protected int getGraphExtentWidth() {
        return (int) (backgroundGridWidth * getMaxEntryCount() + (sideLineLength * 2f));
    }

    @Px
    public int getMaxWidth() {
        return mMaxWidth;
    }

    public void setMaxWidth(@Px int maxWidth) {
        this.mMaxWidth = maxWidth;
        requestLayout();
    }

    protected final int measureWidth(int measureSpec) {
        return getMeasurement(measureSpec, getPreferredWidth(), getMaxWidth());
    }

    protected final int measureHeight(int measureSpec) {
        return getDefaultSize(getSuggestedMinimumHeight(), measureSpec);
    }

    protected final int getMeasurement(int measureSpec, int desiredSize, int maxSize) {
        int result = desiredSize;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                // Parent says we can be as big as we want. Just don't be larger
                // than max size imposed on ourselves.
                result = maxSize > 0 ? Math.min(desiredSize, maxSize) : desiredSize;
                break;
            case MeasureSpec.AT_MOST:
                // If there is an upper limit, don't exceed maximum width (explicit or implicit)
                if (maxSize > 0) {
                    result = Math.min(Math.min(desiredSize, specSize), maxSize);
                } else {
                    result = Math.min(desiredSize, specSize);
                }
                break;
            case MeasureSpec.EXACTLY:
                // If an exact width is specified, still don't exceed any specified maximum width
                if (maxSize > 0) {
                    result = Math.min(maxSize, result);
                } else {
                    // No choice. Do what we are told.
                    result = specSize;
                }
                break;
        }

        return result;
    }

    /* Drawables */
    private final Stack<Animatable> animatedDrawables = new Stack<>();

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || (who instanceof Animatable && animatedDrawables.contains(who));
    }

    protected final void removeAnimatedDrawables() {
        // Stop running animations
        while (!animatedDrawables.empty()) {
            Animatable drw = animatedDrawables.pop();
            drw.stop();
            if (drw instanceof Drawable) {
                ((Drawable) drw).setCallback(null);
            }
        }
    }

    protected final void addAnimatedDrawable(@NonNull Drawable drawable) {
        if (drawable instanceof Animatable && !animatedDrawables.contains(drawable)) {
            drawable.setCallback(this);
            Animatable animatable = (Animatable) drawable;
            animatable.start();
            animatedDrawables.add(animatable);
        }
    }
}
