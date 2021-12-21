package com.thewizrd.simpleweather.controls.graphs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;

import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ContextUtils;

import java.util.ArrayList;

import kotlin.collections.CollectionsKt;

public class BarGraphView extends FrameLayout implements IGraph {

    private ViewGroup mParentLayout;
    private final RectF visibleRect = new RectF();
    private BarChartGraph graph;
    private OnClickListener onClickListener;

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param v          The view whose scroll position has changed.
         * @param scrollX    Current horizontal scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         */
        void onScrollChange(BarGraphView v, int scrollX, int oldScrollX);
    }

    private OnScrollChangeListener mOnScrollChangeListener;

    public interface OnSizeChangedListener {
        void onSizeChanged(BarGraphView v, int canvasWidth);
    }

    private OnSizeChangedListener mOnSizeChangedListener;

    public BarGraphView(Context context) {
        super(context);
        initialize(context);
    }

    public BarGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public BarGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BarGraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnSizeChangedListener(@Nullable OnSizeChangedListener l) {
        mOnSizeChangedListener = l;
    }

    private void initialize(Context context) {
        graph = new BarChartGraph(context);
        graph.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null)
                    onClickListener.onClick(v);
            }
        });

        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.setOverScrollMode(View.OVER_SCROLL_NEVER);

        this.removeAllViews();
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        this.addView(graph, layoutParams);

        mParentLayout = this;
    }

    public void setBottomTextColor(@ColorInt int color) {
        this.graph.BOTTOM_TEXT_COLOR = color;
        if (this.graph.bottomTextPaint != null) {
            this.graph.bottomTextPaint.setColor(this.graph.BOTTOM_TEXT_COLOR);
        }
    }

    public void setBottomTextSize(@Px float textSize) {
        this.graph.BOTTOM_TEXT_SIZE = textSize;
        if (this.graph.bottomTextPaint != null) {
            this.graph.bottomTextPaint.setTextSize(this.graph.BOTTOM_TEXT_SIZE);
        }
    }

    public void setDrawIconLabels(boolean drawIconsLabels) {
        this.graph.drawIconsLabels = drawIconsLabels;
    }

    public void setDrawDataLabels(boolean drawDataLabels) {
        this.graph.drawDataLabels = drawDataLabels;
    }

    public void setGraphMaxWidth(@Px int maxWidth) {
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setOnTouchListener(OnTouchListener l) {
        this.graph.setOnTouchListener(l);
    }

    public final int getItemPositionFromPoint(float xCoordinate) {
        return this.graph.getItemPositionFromPoint(xCoordinate);
    }

    public BarGraphData getData() {
        return this.graph.mData;
    }

    public void setData(BarGraphData data) {
        this.graph.setData(data);
    }

    public void resetData(boolean invalidate) {
        this.graph.resetData(invalidate);
    }

    private class BarChartGraph extends BaseGraphView<BarGraphData> {
        private float drwTextWidth;
        private float bottomTextHeight = 0;

        private final ArrayList<Bar> drawDotLists;

        private final Paint bottomTextPaint;
        private int bottomTextDescent;

        private final float iconHeight;

        private final float iconBottomMargin = ContextUtils.dpToPx(getContext(), 4);
        private final float bottomTextTopMargin = ContextUtils.dpToPx(getContext(), 6);

        private int BOTTOM_TEXT_COLOR = Colors.WHITE;
        private float BOTTOM_TEXT_SIZE = ContextUtils.complexUnitToPx(getContext(), TypedValue.COMPLEX_UNIT_SP, 14);

        private float sideLineLength = 0;
        private float backgroundGridWidth = ContextUtils.dpToPx(getContext(), 45);
        private float longestTextWidth;

        private boolean drawDataLabels = false;
        private boolean drawIconsLabels = false;

        private final Paint linePaint;

        BarChartGraph(Context context) {
            this(context, null);
        }

        BarChartGraph(Context context, AttributeSet attrs) {
            super(context, attrs);

            bottomTextPaint = new TextPaint();
            drawDotLists = new ArrayList<>();

            resetData(false);

            bottomTextPaint.setAntiAlias(true);
            bottomTextPaint.setTextSize(BOTTOM_TEXT_SIZE);
            bottomTextPaint.setTextAlign(Paint.Align.CENTER);
            bottomTextPaint.setStyle(Paint.Style.FILL);
            bottomTextPaint.setColor(BOTTOM_TEXT_COLOR);

            iconHeight = ContextUtils.dpToPx(getContext(), 30);

            linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(ContextUtils.dpToPx(getContext(), 8));
            linePaint.setStrokeCap(Paint.Cap.ROUND);
        }

        private float getGraphTop() {
            int graphTop = getTop();
            graphTop += bottomTextTopMargin + bottomTextHeight * 2f + bottomTextDescent * 2f;

            return graphTop;
        }

        private float getGraphHeight() {
            float graphHeight = mViewHeight - bottomTextTopMargin - bottomTextHeight - bottomTextDescent - linePaint.getStrokeWidth();
            if (drawIconsLabels) graphHeight = graphHeight - iconHeight - iconBottomMargin;
            if (drawDataLabels)
                graphHeight = graphHeight - bottomTextTopMargin - linePaint.getStrokeWidth();

            return graphHeight;
        }

        @Override
        protected void resetData(boolean invalidate) {
            this.drawDotLists.clear();
            bottomTextDescent = 0;
            longestTextWidth = 0;
            horizontalGridNum = MIN_HORIZONTAL_GRID_NUM;
            super.resetData(invalidate);
        }

        @Override
        public void updateGraph() {
            if (!isDataEmpty()) {
                Rect r = new Rect();
                float longestWidth = 0;

                BarGraphDataSet set = mData.getDataSet();
                for (BarGraphEntry entry : set.getEntryData()) {
                    if (entry.getXLabel() != null) {
                        final String s = entry.getXLabel().toString();
                        bottomTextPaint.getTextBounds(s, 0, s.length(), r);
                        if (bottomTextHeight < r.height()) {
                            bottomTextHeight = r.height();
                        }
                        if (longestWidth < r.width()) {
                            longestWidth = r.width();
                        }
                        if (bottomTextDescent < (Math.abs(r.bottom))) {
                            bottomTextDescent = Math.abs(r.bottom);
                        }
                    }
                }

                if (longestTextWidth < longestWidth) {
                    longestTextWidth = longestWidth;
                }
                if (sideLineLength < longestWidth / 1.5f) {
                    sideLineLength = longestWidth / 1.5f;
                }

                backgroundGridWidth = longestTextWidth;
            } else {
                bottomTextDescent = 0;
                longestTextWidth = 0;
            }

            updateHorizontalGridNum();
            refreshXCoordinateList();
            refreshDrawDotList();
            postInvalidate();
        }

        private void refreshGridWidth() {
            // Reset the grid width
            backgroundGridWidth = longestTextWidth;

            final int mParentWidth;
            if (getMaxWidth() > 0) {
                mParentWidth = Math.min(Math.max(mParentLayout.getMeasuredWidth(), getMeasuredWidth()), getMaxWidth());
            } else {
                mParentWidth = Math.max(mParentLayout.getMeasuredWidth(), getMeasuredWidth());
            }

            if (getGraphExtentWidth() < mParentWidth) {
                int freeSpace = mParentWidth - getGraphExtentWidth();
                float additionalSpace = (float) freeSpace / getMaxEntryCount();
                if (additionalSpace > 0) {
                    backgroundGridWidth += additionalSpace;
                }
            }
        }

        private void refreshXCoordinateList() {
            xCoordinateList.clear();
            xCoordinateList.ensureCapacity(getMaxEntryCount());

            for (int i = 0; i < getMaxEntryCount(); i++) {
                float x = sideLineLength + backgroundGridWidth * i;
                xCoordinateList.add(x);
            }
        }

        private void refreshDrawDotList() {
            if (!isDataEmpty()) {
                drawDotLists.clear();

                mData.calcMinMax();
                float maxValue = mData.getYMax();
                float minValue = mData.getYMin();

                final float graphHeight = getGraphHeight();
                final float graphTop = getGraphTop();

                int drawDotSize = drawDotLists.isEmpty() ? 0 : drawDotLists.size();

                if (drawDotSize > 0) {
                    drawDotLists.ensureCapacity(mData.getDataSet().getDataCount());
                }

                for (int i = 0; i < mData.getDataSet().getDataCount(); i++) {
                    BarGraphEntry entry = mData.getDataSet().getEntryForIndex(i);
                    float x = xCoordinateList.get(i);
                    float y = 0;

                    /*
                     * Scaling formula
                     *
                     * ((value - minValue) / (maxValue - minValue)) * (scaleMax - scaleMin) + scaleMin
                     * graphTop is scaleMax & graphHeight is scaleMin due to View coordinate system
                     */
                    if (entry.getEntryData() != null) {
                        y = ((entry.getEntryData().getY() - minValue) / (maxValue - minValue)) * (graphTop - graphHeight) + graphHeight;
                    }

                    if (i > drawDotSize - 1) {
                        drawDotLists.add(new Bar(x, y));
                    } else {
                        drawDotLists.set(i, new Bar(x, y));
                    }
                }
            }
        }

        @Override
        public void invalidate() {
            setMinimumWidth(0);
            visibleRect.setEmpty();
            super.invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (visibleRect.isEmpty()) {
                visibleRect.set(
                        mParentLayout.getScrollX(),
                        mParentLayout.getScrollY(),
                        mParentLayout.getScrollX() + mParentLayout.getWidth(),
                        mParentLayout.getScrollY() + mParentLayout.getHeight());
            }

            if (mData != null) {
                drawTextAndIcons(canvas);
                drawLines(canvas);
            }
        }

        private void drawTextAndIcons(Canvas canvas) {
            // draw bottom text
            if (!isDataEmpty()) {
                BarGraphDataSet set = mData.getDataSet();
                for (int i = 0; i < set.getDataCount(); i++) {
                    float x = xCoordinateList.get(i);
                    float y = mViewHeight - bottomTextDescent;
                    BarGraphEntry entry = set.getEntryForIndex(i);

                    if (entry.getXLabel() != null && !TextUtils.isEmpty(entry.getXLabel())) {
                        drwTextWidth = bottomTextPaint.measureText(entry.getXLabel().toString());
                        drawingRect.set(x, y, x + drwTextWidth, y + bottomTextHeight);

                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawText(entry.getXLabel().toString(), x, y, bottomTextPaint);
                    }

                    if (drawIconsLabels && entry.getXIcon() != null) {
                        final int rotation = entry.getXIconRotation();

                        Rect bounds = new Rect(0, 0, (int) iconHeight, (int) iconHeight);
                        entry.getXIcon().setBounds(bounds);
                        drawingRect.set(x, y, x + bounds.width(), y + bounds.height());

                        if (RectF.intersects(drawingRect, visibleRect)) {
                            if (entry.getXIcon() instanceof Animatable) {
                                addAnimatedDrawable(entry.getXIcon());
                            }

                            canvas.save();
                            canvas.translate(x - bounds.width() / 2f, y - bounds.height() - bottomTextHeight - iconBottomMargin);
                            canvas.rotate(rotation, bounds.width() / 2f, bounds.height() / 2f);
                            entry.getXIcon().draw(canvas);
                            canvas.restore();
                        }
                    }
                }
            }
        }

        private void drawLines(Canvas canvas) {
            if (!drawDotLists.isEmpty() && !isDataEmpty()) {
                BarGraphDataSet set = mData.getDataSet();
                for (int i = 0; i < drawDotLists.size(); i++) {
                    Bar bar = drawDotLists.get(i);
                    BarGraphEntry entry = set.getEntryForIndex(i);

                    linePaint.setColor(entry.getFillColor() != null ? entry.getFillColor() : Colors.ALICEBLUE);

                    drawingRect.set(
                            bar.x - linePaint.getStrokeWidth() / 2f,
                            bar.y - bottomTextHeight - bottomTextDescent,
                            bar.x + drwTextWidth + linePaint.getStrokeWidth() / 2f,
                            mViewHeight - bottomTextDescent - bottomTextHeight - bottomTextTopMargin * 2f
                    );

                    if (RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(bar.x, drawingRect.bottom, bar.x, bar.y, linePaint);

                        if (drawDataLabels) {
                            if (entry.getEntryData() != null)
                                canvas.drawText(entry.getEntryData().getLabel().toString(), bar.x, bar.y - bottomTextHeight - bottomTextDescent, bottomTextPaint);
                        }
                    }
                }
            }
        }

        @Override
        protected int getGraphExtentWidth() {
            return Math.round(longestTextWidth * getMaxEntryCount() + sideLineLength * 2);
        }

        @Override
        protected int getPreferredWidth() {
            if (xCoordinateList.isEmpty())
                return Math.round(backgroundGridWidth * getMaxEntryCount());
            else
                return (int) (CollectionsKt.last(xCoordinateList).intValue() + sideLineLength);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            mViewWidth = Math.min(MeasureSpec.getSize(widthMeasureSpec), getMaxWidth());
            setMeasuredDimension(mViewWidth, mViewHeight);

            refreshGridWidth();
            refreshXCoordinateList();

            mViewWidth = Math.min(getPreferredWidth(), getMaxWidth());
            setMeasuredDimension(mViewWidth, mViewHeight);

            Log.d("BarGraphView", "onMeasure: parent width = " + mParentLayout.getMeasuredWidth());
            Log.d("BarGraphView", "onMeasure: measure width = " + MeasureSpec.getSize(widthMeasureSpec));
            Log.d("BarGraphView", "onMeasure: width = " + mViewWidth);

            refreshDrawDotList();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (mOnSizeChangedListener != null)
                mOnSizeChangedListener.onSizeChanged(BarGraphView.this, xCoordinateList.size() > 0 ? CollectionsKt.last(xCoordinateList).intValue() : 0);
        }

        private class Bar {
            float x;
            float y;

            Bar(float x, float y) {
                this.x = x;
                this.y = y;
            }
        }
    }
}