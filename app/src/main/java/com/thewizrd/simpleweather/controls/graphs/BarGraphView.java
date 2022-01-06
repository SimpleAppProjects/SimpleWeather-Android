package com.thewizrd.simpleweather.controls.graphs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ContextUtils;

import java.util.ArrayList;

import kotlin.collections.CollectionsKt;

public class BarGraphView extends BaseGraphHorizontalScrollView<BarGraphData> {
    private static final String TAG = "BarGraphView";

    public BarGraphView(Context context) {
        super(context);
    }

    public BarGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BarGraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @NonNull
    @Override
    public BaseGraphView<?> createGraphView(@NonNull Context context) {
        return new BarChartGraph(context);
    }

    @NonNull
    @Override
    public BarChartGraph getGraph() {
        return (BarChartGraph) super.getGraph();
    }

    public void setBottomTextColor(@ColorInt int color) {
        getGraph().setBottomTextColor(color);
    }

    public void setBottomTextSize(@Px float textSize) {
        getGraph().setBottomTextSize(textSize);
    }

    public void setDrawIconLabels(boolean drawIconsLabels) {
        getGraph().setDrawIconsLabels(drawIconsLabels);
    }

    public void setDrawDataLabels(boolean drawDataLabels) {
        getGraph().setDrawDataLabels(drawDataLabels);
    }

    public BarGraphData getData() {
        return getGraph().mData;
    }

    public void setData(BarGraphData data) {
        getGraph().setData(data);
    }

    private class BarChartGraph extends BaseGraphView<BarGraphData> {
        private final ArrayList<Bar> drawDotLists;

        private final Paint linePaint;

        BarChartGraph(Context context) {
            this(context, null);
        }

        BarChartGraph(Context context, AttributeSet attrs) {
            super(context, attrs);

            drawDotLists = new ArrayList<>();

            resetData(false);

            linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(ContextUtils.dpToPx(getContext(), 6));
            linePaint.setStrokeCap(Paint.Cap.ROUND);
        }

        private float getGraphTop() {
            int graphTop = getTop();
            graphTop += bottomTextTopMargin + bottomTextHeight * 2f + bottomTextDescent * 2f;

            return graphTop;
        }

        private float getGraphHeight() {
            float graphHeight = mViewHeight - bottomTextTopMargin - bottomTextHeight - bottomTextDescent - linePaint.getStrokeWidth();
            if (isDrawIconsLabels()) graphHeight = graphHeight - iconHeight - iconBottomMargin;
            if (isDrawDataLabels())
                graphHeight = graphHeight - bottomTextTopMargin - linePaint.getStrokeWidth();

            return graphHeight;
        }

        private float getGraphBottom() {
            int graphBottom = mViewHeight;

            graphBottom -= (bottomTextTopMargin + bottomTextHeight + bottomTextDescent);

            if (isDrawIconsLabels())
                graphBottom -= (iconHeight + iconBottomMargin);
            else
                graphBottom -= (linePaint.getStrokeWidth() + iconBottomMargin);

            return graphBottom;
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

                // Add padding
                longestTextWidth += ContextUtils.dpToPx(getContext(), 8f);
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

            final int mParentWidth = getScrollViewer().getMeasuredWidth();

            if (getGraphExtentWidth() < mParentWidth) {
                int freeSpace = mParentWidth - getGraphExtentWidth();
                float additionalSpace = (float) freeSpace / getMaxEntryCount();

                if (isFillParentWidth()) {
                    if (additionalSpace > 0) {
                        backgroundGridWidth += additionalSpace;
                    }
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

                float maxValue = mData.getYMax();
                float minValue = mData.getYMin();

                final float graphBottom = getGraphBottom();
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
                        y = ((entry.getEntryData().getY() - minValue) / (maxValue - minValue)) * (graphTop - graphBottom) + graphBottom;
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
                        getScrollViewer().getScrollX(),
                        getScrollViewer().getScrollY(),
                        getScrollViewer().getScrollX() + getScrollViewer().getWidth(),
                        getScrollViewer().getScrollY() + getScrollViewer().getHeight());
                Log.d(TAG, "onDraw: rect = " + visibleRect);
            }

            if (mData != null) {
                if (!drawDotLists.isEmpty()) {
                    Log.d(TAG, "onDraw: first x = " + CollectionsKt.first(drawDotLists).toString());
                    Log.d(TAG, "onDraw: last x = " + CollectionsKt.last(drawDotLists).toString());
                }
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
                        float drwTextWidth = bottomTextPaint.measureText(entry.getXLabel().toString());
                        drawingRect.set(x, y, x + drwTextWidth, y + bottomTextHeight);

                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawText(entry.getXLabel().toString(), x, y, bottomTextPaint);
                    }

                    if (isDrawIconsLabels() && entry.getXIcon() != null) {
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
                            bar.x + linePaint.getStrokeWidth() / 2f,
                            getGraphBottom()
                    );

                    if (RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(bar.x, drawingRect.bottom, bar.x, bar.y, linePaint);

                        if (isDrawDataLabels()) {
                            if (entry.getEntryData() != null)
                                canvas.drawText(entry.getEntryData().getLabel().toString(), bar.x, bar.y - bottomTextHeight - bottomTextDescent, bottomTextPaint);
                        }
                    }
                }
            }
        }

        @Override
        protected int getGraphExtentWidth() {
            return Math.round(longestTextWidth * getMaxEntryCount() + sideLineLength);
        }

        @Override
        protected int getPreferredWidth() {
            if (xCoordinateList.isEmpty())
                return Math.round(backgroundGridWidth * getMaxEntryCount());
            else
                return (int) (CollectionsKt.last(xCoordinateList).intValue() + sideLineLength);
        }

        @Override
        protected void onPreMeasure() {
            refreshGridWidth();
            refreshXCoordinateList();
        }

        @Override
        protected void onPostMeasure() {
            refreshDrawDotList();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            // Invalidate the visible rect
            visibleRect.setEmpty();

            if (getOnSizeChangedListener() != null)
                getOnSizeChangedListener().onSizeChanged(BarGraphView.this, xCoordinateList.size() > 0 ? CollectionsKt.last(xCoordinateList).intValue() : 0);
        }

        private class Bar {
            float x;
            float y;

            Bar(float x, float y) {
                this.x = x;
                this.y = y;
            }

            @NonNull
            @Override
            public String toString() {
                return "Bar{x=" + x + ", y=" + y + '}';
            }
        }
    }
}