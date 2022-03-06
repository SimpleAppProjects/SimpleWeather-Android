package com.thewizrd.simpleweather.controls.graphs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ContextUtils;
import com.thewizrd.simpleweather.BuildConfig;

import java.util.ArrayList;

import kotlin.collections.CollectionsKt;

public class RangeBarGraphView extends BaseGraphHorizontalScrollView<RangeBarGraphData> {
    private static final String TAG = "RangeBarGraphView";

    public RangeBarGraphView(Context context) {
        super(context);
    }

    public RangeBarGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RangeBarGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RangeBarGraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @NonNull
    @Override
    public BaseGraphView<?> createGraphView(@NonNull Context context) {
        return new RangeBarChartGraph(context);
    }

    @NonNull
    @Override
    public RangeBarChartGraph getGraph() {
        return (RangeBarChartGraph) super.getGraph();
    }

    public RangeBarGraphData getData() {
        return getGraph().mData;
    }

    public void setData(RangeBarGraphData data) {
        getGraph().setData(data);
    }

    private class RangeBarChartGraph extends BaseGraphView<RangeBarGraphData> {
        private final ArrayList<Bar> drawDotLists;

        private final Paint linePaint;

        RangeBarChartGraph(Context context) {
            this(context, null);
        }

        RangeBarChartGraph(Context context, AttributeSet attrs) {
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

                RangeBarGraphDataSet set = mData.getDataSet();
                for (RangeBarGraphEntry entry : set.getEntryData()) {
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
                backgroundGridWidth = longestTextWidth + ContextUtils.dpToPx(getContext(), 8f);
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
            final float defaultPadding = ContextUtils.dpToPx(getContext(), 8f);

            final int mParentWidth = getScrollViewer().getMeasuredWidth();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "refreshGridWidth: parent width = " + getScrollViewer().getMeasuredWidth());
                Log.d(TAG, "refreshGridWidth: measure width = " + getMeasuredWidth());
            }

            if (getGraphExtentWidth() < mParentWidth) {
                int freeSpace = mParentWidth - getGraphExtentWidth();
                float availableAdditionalSpace = (float) freeSpace / getMaxEntryCount();

                if (isFillParentWidth()) {
                    if (availableAdditionalSpace > 0) {
                        backgroundGridWidth += availableAdditionalSpace;
                    } else {
                        backgroundGridWidth += defaultPadding;
                    }
                } else {
                    final float requestedPadding = ContextUtils.dpToPx(getContext(), 48f);
                    if (availableAdditionalSpace > 0 && requestedPadding < availableAdditionalSpace) {
                        backgroundGridWidth += requestedPadding;
                    } else {
                        backgroundGridWidth += defaultPadding;
                    }
                }
            } else {
                backgroundGridWidth += defaultPadding;
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

                final float graphHeight = getGraphHeight();
                final float graphTop = getGraphTop();

                int drawDotSize = drawDotLists.isEmpty() ? 0 : drawDotLists.size();

                if (drawDotSize > 0) {
                    drawDotLists.ensureCapacity(mData.getDataSet().getDataCount());
                }

                for (int i = 0; i < mData.getDataSet().getDataCount(); i++) {
                    RangeBarGraphEntry entry = mData.getDataSet().getEntryForIndex(i);
                    float x = xCoordinateList.get(i);
                    Float hiY = null, loY = null;

                    /*
                     * Scaling formula
                     *
                     * ((value - minValue) / (maxValue - minValue)) * (scaleMax - scaleMin) + scaleMin
                     * graphTop is scaleMax & graphHeight is scaleMin due to View coordinate system
                     */
                    if (entry.getHiTempData() != null) {
                        hiY = ((entry.getHiTempData().getY() - minValue) / (maxValue - minValue)) * (graphTop - graphHeight) + graphHeight;
                    }

                    if (entry.getLoTempData() != null) {
                        loY = ((entry.getLoTempData().getY() - minValue) / (maxValue - minValue)) * (graphTop - graphHeight) + graphHeight;
                    }

                    // Skip empty entry
                    if (hiY == null && loY == null) {
                        continue;
                    }

                    if (hiY == null) {
                        hiY = loY;
                    }

                    if (loY == null) {
                        loY = hiY;
                    }

                    if (i > drawDotSize - 1) {
                        drawDotLists.add(new Bar(x, hiY, loY));
                    } else {
                        drawDotLists.set(i, new Bar(x, hiY, loY));
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
                        getScrollViewer().getScrollY() + getScrollViewer().getHeight()
                );
                Log.d("BarGraphView", "onDraw: rect = " + visibleRect);
            }

            if (mData != null) {
                drawTextAndIcons(canvas);
                drawLines(canvas);
            }
        }

        private void drawTextAndIcons(Canvas canvas) {
            // draw bottom text
            if (!isDataEmpty()) {
                RangeBarGraphDataSet set = mData.getDataSet();
                for (int i = 0; i < set.getDataCount(); i++) {
                    float x = xCoordinateList.get(i);
                    float y = mViewHeight - bottomTextDescent;
                    RangeBarGraphEntry entry = set.getEntryForIndex(i);

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
                final int hiTempColor = Colors.ORANGERED;
                final int loTempColor = Colors.LIGHTSKYBLUE;

                RangeBarGraphDataSet set = mData.getDataSet();
                for (int i = 0; i < drawDotLists.size(); i++) {
                    Bar bar = drawDotLists.get(i);
                    RangeBarGraphEntry entry = set.getEntryForIndex(i);
                    boolean drawLine = true;

                    if (entry.getHiTempData() != null && entry.getLoTempData() != null) {
                        LinearGradient shader = new LinearGradient(0, bar.hiY, 0, bar.loY, hiTempColor, loTempColor, Shader.TileMode.CLAMP);
                        linePaint.setShader(shader);
                    } else if (entry.getHiTempData() != null) {
                        linePaint.setShader(null);
                        linePaint.setColor(hiTempColor);
                        drawLine = false;
                    } else if (entry.getLoTempData() != null) {
                        linePaint.setShader(null);
                        linePaint.setColor(loTempColor);
                        drawLine = false;
                    }

                    drawingRect.set(bar.x - linePaint.getStrokeWidth() / 2f,
                            bar.hiY - bottomTextHeight - bottomTextDescent,
                            bar.x + linePaint.getStrokeWidth() / 2f,
                            bar.loY + bottomTextHeight + bottomTextDescent);

                    if (RectF.intersects(drawingRect, visibleRect)) {
                        if (drawLine) {
                            canvas.drawLine(bar.x, bar.hiY, bar.x, bar.loY, linePaint);
                        } else {
                            canvas.drawLine(bar.x, bar.hiY - linePaint.getStrokeWidth() / 4f, bar.x, bar.hiY, linePaint);
                        }

                        if (isDrawDataLabels()) {
                            if (entry.getHiTempData() != null)
                                canvas.drawText(entry.getHiTempData().getLabel().toString(), bar.x, bar.hiY - bottomTextHeight - bottomTextDescent, bottomTextPaint);
                            if (entry.getLoTempData() != null)
                                canvas.drawText(entry.getLoTempData().getLabel().toString(), bar.x, bar.loY + bottomTextHeight + bottomTextDescent + linePaint.getStrokeWidth(), bottomTextPaint);
                        }
                    }
                }
            }
        }

        @Override
        protected int getGraphExtentWidth() {
            return Math.round(longestTextWidth * getMaxEntryCount());
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
            if (getOnSizeChangedListener() != null)
                getOnSizeChangedListener().onSizeChanged(RangeBarGraphView.this, xCoordinateList.size() > 0 ? CollectionsKt.last(xCoordinateList).intValue() : 0);
        }

        private class Bar {
            float x;
            float hiY;
            float loY;

            Bar(float x, float hiY, float loY) {
                this.x = x;
                this.hiY = hiY;
                this.loY = loY;
            }
        }
    }
}