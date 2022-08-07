package com.thewizrd.simpleweather.controls.graphs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ContextUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LineView extends BaseGraphHorizontalScrollView<LineViewData> {
    private static final String TAG = "LineView";

    public LineView(Context context) {
        super(context);
    }

    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LineView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @NonNull
    @Override
    public BaseGraphView<?> createGraphView(@NonNull Context context) {
        return new LineViewGraph(context);
    }

    @NonNull
    @Override
    public LineViewGraph getGraph() {
        return (LineViewGraph) super.getGraph();
    }

    public void setDrawGridLines(boolean drawGridLines) {
        getGraph().drawGridLines = drawGridLines;
    }

    public void setDrawDotLine(boolean drawDotLine) {
        getGraph().drawDotLine = drawDotLine;
    }

    public void setDrawDotPoints(boolean drawDotPoints) {
        getGraph().drawDotPoints = drawDotPoints;
    }

    public void setDrawGraphBackground(boolean drawGraphBackground) {
        getGraph().drawGraphBackground = drawGraphBackground;
    }

    public void setDrawSeriesLabels(boolean drawSeriesLabels) {
        getGraph().drawSeriesLabels = drawSeriesLabels;
    }

    public void setBackgroundLineColor(@ColorInt int color) {
        getGraph().BACKGROUND_LINE_COLOR = color;
        if (getGraph().bgLinesPaint != null) {
            getGraph().bgLinesPaint.setColor(getGraph().BACKGROUND_LINE_COLOR);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setOnTouchListener(OnTouchListener l) {
        getGraph().setOnTouchListener(l);
    }

    @Override
    public LineViewData getData() {
        return getGraph().mData;
    }

    @Override
    public void setData(LineViewData data) {
        getGraph().setData(data);
    }

    /*
     *  Multi-series line graph
     *  Based on LineView from http://www.androidtrainee.com/draw-android-line-chart-with-animation/
     *  Graph background (under line) based on - https://github.com/jjoe64/GraphView (LineGraphSeries)
     */
    private class LineViewGraph extends BaseGraphView<LineViewData> {
        private float drwTextWidth;
        private int dataOfAGird = 10;

        private final ArrayList<Float> yCoordinateList;

        private final List<ArrayList<Dot>> drawDotLists;

        private final float DOT_INNER_CIR_RADIUS = ContextUtils.dpToPx(getContext(), 2);
        private final float DOT_OUTER_CIR_RADIUS = ContextUtils.dpToPx(getContext(), 5);
        private final float LINE_CORNER_RADIUS = ContextUtils.dpToPx(getContext(), 16);
        private final int MIN_VERTICAL_GRID_NUM = 4;

        private int BACKGROUND_LINE_COLOR = Colors.WHITESMOKE;

        private boolean drawGridLines = false;
        private boolean drawDotLine = false;
        private boolean drawDotPoints = false;
        private boolean drawGraphBackground = false;
        private boolean drawSeriesLabels = false;

        private final Paint bigCirPaint;
        private final Paint smallCirPaint;
        private final Paint linePaint;
        private final Path mLinePath;
        private final Path mPathBackground;
        private final Paint mPaintBackground;
        private final Paint bgLinesPaint;
        private final PathEffect dashEffects;
        private final Paint seriesRectPaint;

        LineViewGraph(Context context) {
            this(context, null);
        }

        LineViewGraph(Context context, AttributeSet attrs) {
            super(context, attrs);

            bigCirPaint = new Paint();
            yCoordinateList = new ArrayList<>();
            drawDotLists = new ArrayList<>();

            resetData(false);

            bigCirPaint.setAntiAlias(true);
            smallCirPaint = new Paint(bigCirPaint);

            linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setDither(true);
            linePaint.setPathEffect(new CornerPathEffect(LINE_CORNER_RADIUS));
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(ContextUtils.dpToPx(getContext(), 2));
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setStrokeJoin(Paint.Join.ROUND);
            mLinePath = new Path();

            mPathBackground = new Path();
            mPaintBackground = new Paint();
            mPaintBackground.setAntiAlias(true);
            mPaintBackground.setDither(true);
            mPaintBackground.setPathEffect(new CornerPathEffect(LINE_CORNER_RADIUS));
            mPaintBackground.setStyle(Paint.Style.FILL_AND_STROKE);

            bgLinesPaint = new Paint();
            bgLinesPaint.setStyle(Paint.Style.STROKE);
            bgLinesPaint.setStrokeWidth(ContextUtils.dpToPx(getContext(), 1f));
            bgLinesPaint.setColor(BACKGROUND_LINE_COLOR);
            dashEffects = new DashPathEffect(new float[]{10, 5, 10, 5}, 1);

            seriesRectPaint = new Paint();
            seriesRectPaint.setAntiAlias(true);
            seriesRectPaint.setStyle(Paint.Style.FILL);
        }

        private float getGraphTop() {
            int graphTop = getTop();
            if (drawSeriesLabels) graphTop += getLegendHeight();
            graphTop += bottomTextTopMargin + bottomTextHeight * 2f + bottomTextDescent * 2f;

            return graphTop;
        }

        private float getGraphHeight() {
            float graphHeight = mViewHeight - bottomTextTopMargin - bottomTextHeight - bottomTextDescent;
            if (isDrawIconsLabels()) graphHeight -= iconHeight;

            return graphHeight;
        }

        private float getLegendHeight() {
            return bottomTextTopMargin + bottomTextHeight * 2f + bottomTextDescent * 2f;
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
            this.yCoordinateList.clear();
            this.drawDotLists.clear();
            bottomTextDescent = 0;
            longestTextWidth = 0;
            horizontalGridNum = MIN_HORIZONTAL_GRID_NUM;
            verticalGridNum = MIN_VERTICAL_GRID_NUM;
            super.resetData(invalidate);
        }

        @Override
        public void updateGraph() {
            if (!isDataEmpty()) {
                Rect r = new Rect();
                float longestWidth = 0;

                float biggestData = 0;

                for (LineDataSeries series : mData.getDataSets()) {
                    for (LineGraphEntry entry : series.getEntryData()) {
                        if (biggestData < entry.getYEntryData().getY()) {
                            biggestData = entry.getYEntryData().getY();
                        }

                        String s;
                        // Measure Y label
                        s = entry.getYEntryData().getLabel().toString();
                        bottomTextPaint.getTextBounds(s, 0, s.length(), r);
                        if (longestWidth < r.width()) {
                            longestWidth = r.width();
                        }
                        if (longestTextWidth < longestWidth) {
                            longestTextWidth = longestWidth;
                        }
                        if (sideLineLength < longestWidth / 1.5f) {
                            sideLineLength = longestWidth / 1.5f;
                        }

                        // Measure X label
                        if (entry.getXLabel() != null) {
                            s = entry.getXLabel().toString();
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
                    dataOfAGird = 1;
                    while (biggestData / 10 > dataOfAGird) {
                        dataOfAGird *= 10;
                    }
                }

                if (longestTextWidth < longestWidth) {
                    longestTextWidth = longestWidth;
                }
                if (sideLineLength < longestWidth / 1.5f) {
                    sideLineLength = longestWidth / 1.5f;
                }

                // Add adequate spacing between labels
                longestTextWidth += ContextUtils.dpToPx(getContext(), 16f);
                backgroundGridWidth = longestTextWidth;
            } else {
                bottomTextDescent = 0;
                longestTextWidth = 0;
                verticalGridNum = MIN_VERTICAL_GRID_NUM;
            }

            updateHorizontalGridNum();
            refreshXCoordinateList();
            refreshAfterDataChanged();
            postInvalidate();
        }

        private void refreshGridWidth() {
            // Reset the grid width
            backgroundGridWidth = longestTextWidth;

            final int mParentWidth = getScrollViewer().getMeasuredWidth();

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "refreshGridWidth: parent width = " + getScrollViewer().getMeasuredWidth());
                Log.d(TAG, "refreshGridWidth: measure width = " + getMeasuredWidth());
            }

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

        private void refreshAfterDataChanged() {
            updateVerticalGridNum(mData != null ? mData.getDataSets() : null);
            refreshYCoordinateList();
            refreshDrawDotList();
        }

        private void updateVerticalGridNum(List<LineDataSeries> dataSeriesList) {
            if (dataSeriesList != null && !dataSeriesList.isEmpty()) {
                for (LineDataSeries series : dataSeriesList) {
                    for (LineGraphEntry entry : series.getEntryData()) {
                        verticalGridNum = Math.max(verticalGridNum, Math.max(MIN_VERTICAL_GRID_NUM, (int) Math.ceil(entry.getYEntryData().getY()) + 1));
                    }
                }
            } else {
                verticalGridNum = MIN_VERTICAL_GRID_NUM;
            }
        }

        private void refreshXCoordinateList() {
            xCoordinateList.clear();
            xCoordinateList.ensureCapacity(getMaxEntryCount());
            for (int i = 0; i < (getMaxEntryCount() + 1); i++) {
                xCoordinateList.add(sideLineLength + backgroundGridWidth * i);
            }
        }

        private void refreshYCoordinateList() {
            yCoordinateList.clear();
            yCoordinateList.ensureCapacity(verticalGridNum);
            for (int i = 0; i < (verticalGridNum + 1); i++) {
                /*
                 * Scaling formula
                 *
                 * ((value - minValue) / (maxValue - minValue)) * (scaleMax - scaleMin) + scaleMin
                 * minValue = 0; maxValue = verticalGridNum; value = i
                 */
                yCoordinateList.add(((float) i / (verticalGridNum)) * (getGraphBottom() - getGraphTop()) + getGraphTop());
            }
        }

        private void refreshDrawDotList() {
            if (!isDataEmpty()) {
                if (drawDotLists.size() == 0 || drawDotLists.size() != mData.getDataCount()) {
                    drawDotLists.clear();
                    for (int k = 0; k < mData.getDataCount(); k++) {
                        drawDotLists.add(new ArrayList<>());
                    }
                }

                final float maxValue = mData.getYMax();
                final float minValue = mData.getYMin();

                final float graphBottom = getGraphBottom();
                final float graphTop = getGraphTop();

                for (int k = 0; k < mData.getDataCount(); k++) {
                    int drawDotSize = drawDotLists.get(k).isEmpty() ? 0 : drawDotLists.get(k).size();

                    LineDataSeries series = mData.getDataSetByIndex(k);

                    if (drawDotSize > 0) {
                        drawDotLists.get(k).ensureCapacity(series.getDataCount());
                    }

                    for (int i = 0; i < series.getDataCount(); i++) {
                        float x = xCoordinateList.get(i);
                        float y;
                        if (maxValue == minValue) {
                            if (maxValue == 0) {
                                y = graphBottom;
                            } else if (maxValue == 100) {
                                y = graphTop;
                            } else {
                                y = (graphBottom - graphTop) / 2f;
                            }
                        } else {
                            /*
                             * Scaling formula
                             *
                             * ((value - minValue) / (maxValue - minValue)) * (scaleMax - scaleMin) + scaleMin
                             * graphTop is scaleMax & graphHeight is scaleMin due to View coordinate system
                             */
                            y = ((series.getEntryForIndex(i).getYEntryData().getY() - minValue) / (maxValue - minValue)) * (graphTop - graphBottom) + graphBottom;
                        }

                        if (i > drawDotSize - 1) {
                            drawDotLists.get(k).add(new Dot(x, y));
                        } else {
                            drawDotLists.get(k).set(i, new Dot(x, y));
                        }
                    }

                    int temp = drawDotLists.get(k).size() - series.getDataCount();
                    for (int i = 0; i < temp; i++) {
                        drawDotLists.get(k).remove(drawDotLists.get(k).size() - 1);
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
            }

            if (!isDataEmpty()) {
                drawBackgroundLines(canvas);
                drawTextAndIcons(canvas);
                drawLines(canvas);
                drawDots(canvas);
                drawSeriesLegend(canvas);
            }
        }

        private void drawDots(Canvas canvas) {
            if (drawDotPoints && drawDotLists != null && !drawDotLists.isEmpty()) {
                for (int k = 0; k < drawDotLists.size(); k++) {
                    LineDataSeries series = mData.getDataSetByIndex(k);
                    int color = series.getColor(k);

                    bigCirPaint.setColor(color);
                    smallCirPaint.setColor(ColorUtils.setAlphaComponent(color, 0x99));

                    for (Dot dot : drawDotLists.get(k)) {
                        if (visibleRect.contains(dot.x, dot.y)) {
                            canvas.drawCircle(dot.x, dot.y, DOT_OUTER_CIR_RADIUS, bigCirPaint);
                            canvas.drawCircle(dot.x, dot.y, DOT_INNER_CIR_RADIUS, smallCirPaint);
                        }
                    }
                }
            }
        }

        private void drawLines(Canvas canvas) {
            if (!drawDotLists.isEmpty()) {
                float graphHeight = getGraphHeight();

                for (int k = 0; k < drawDotLists.size(); k++) {
                    LineDataSeries series = mData.getDataSetByIndex(k);
                    List<TextEntry> textEntries = new LinkedList<>();

                    float firstX = -1;
                    // needed to end the path for background
                    Dot currentDot = null;

                    mLinePath.rewind();
                    mPathBackground.rewind();
                    linePaint.setColor(series.getColor(k));
                    mPaintBackground.setColor(ColorUtils.setAlphaComponent(series.getColor(k), 0x99));

                    if (drawGraphBackground) {
                        mPathBackground.moveTo(visibleRect.left - LINE_CORNER_RADIUS, graphHeight);
                    }

                    for (int i = 0; i < drawDotLists.get(k).size() - 1; i++) {
                        Dot dot = drawDotLists.get(k).get(i);
                        Dot nextDot = drawDotLists.get(k).get(i + 1);
                        YEntryData entry = series.getEntryForIndex(i).getYEntryData();
                        YEntryData nextEntry = series.getEntryForIndex(i + 1).getYEntryData();

                        float startX = dot.x;
                        float startY = dot.y;
                        float endX = nextDot.x;
                        float endY = nextDot.y;

                        drawingRect.set(visibleRect.left, dot.y, dot.x, dot.y);
                        if (firstX == -1) {
                            mLinePath.moveTo(visibleRect.left, dot.y);
                            mLinePath.lineTo(dot.x, dot.y);
                        }

                        drawingRect.set(dot.x, dot.y, nextDot.x, nextDot.y);
                        if (RectF.intersects(drawingRect, visibleRect))
                            mLinePath.lineTo(nextDot.x, nextDot.y);

                        if (isDrawDataLabels()) {
                            // Draw top label
                            drwTextWidth = bottomTextPaint.measureText(entry.getLabel().toString());
                            drawingRect.set(dot.x, dot.y, dot.x + drwTextWidth, dot.y + bottomTextHeight);
                            if (RectF.intersects(drawingRect, visibleRect))
                                textEntries.add(new TextEntry(entry.getLabel().toString(), dot.x, dot.y - bottomTextHeight - bottomTextDescent));
                        }

                        if (firstX == -1) {
                            firstX = visibleRect.left;
                            if (drawGraphBackground)
                                mPathBackground.lineTo(firstX - LINE_CORNER_RADIUS, startY);
                        }

                        if (drawGraphBackground) {
                            mPathBackground.lineTo(startX, startY);
                        }

                        currentDot = dot;

                        // Draw last items
                        if (i + 1 == drawDotLists.get(k).size() - 1) {
                            if (isDrawDataLabels()) {
                                // Draw top label
                                drwTextWidth = bottomTextPaint.measureText(nextEntry.getLabel().toString());
                                drawingRect.set(nextDot.x, nextDot.y, nextDot.x + drwTextWidth, nextDot.y + bottomTextHeight);

                                if (RectF.intersects(drawingRect, visibleRect))
                                    textEntries.add(new TextEntry(nextEntry.getLabel().toString(), nextDot.x, nextDot.y - bottomTextHeight - bottomTextDescent));
                            }

                            drawingRect.set(nextDot.x, nextDot.y, visibleRect.right, nextDot.y);

                            if (RectF.intersects(drawingRect, visibleRect))
                                mLinePath.lineTo(visibleRect.right, nextDot.y);

                            currentDot = nextDot;

                            if (drawGraphBackground) {
                                mPathBackground.lineTo(endX, endY);
                            }
                        }
                    }

                    canvas.drawPath(mLinePath, linePaint);

                    if (drawGraphBackground) {
                        if (currentDot != null) {
                            mPathBackground.lineTo(visibleRect.right + LINE_CORNER_RADIUS, currentDot.y);
                        }
                        if (firstX != -1) {
                            mPathBackground.lineTo(visibleRect.right + LINE_CORNER_RADIUS, graphHeight);
                        }

                        mPathBackground.close();

                        canvas.drawPath(mPathBackground, mPaintBackground);
                    }

                    if (isDrawDataLabels()) {
                        for (TextEntry entry : textEntries) {
                            canvas.drawText(entry.text, entry.x, entry.y, bottomTextPaint);
                        }
                    }
                }
            }
        }

        private void drawBackgroundLines(Canvas canvas) {
            if (drawGridLines) {
                // draw vertical lines
                for (int i = 0; i < xCoordinateList.size(); i++) {
                    drawingRect.set(xCoordinateList.get(i), getGraphTop(), xCoordinateList.get(i), getGraphBottom());

                    if (RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(xCoordinateList.get(i),
                                getGraphTop(),
                                xCoordinateList.get(i),
                                getGraphBottom(),
                                bgLinesPaint);
                    }
                }

                // draw dotted lines
                bgLinesPaint.setPathEffect(drawDotLine ? dashEffects : null);

                // draw horizontal lines
                for (int i = 0; i < yCoordinateList.size(); i++) {
                    if ((yCoordinateList.size() - 1 - i) % dataOfAGird == 0) {
                        final float y = yCoordinateList.get(i);

                        if (y <= visibleRect.bottom && y >= visibleRect.top) {
                            canvas.drawLine(visibleRect.left, y, visibleRect.right, y, bgLinesPaint);
                        }
                    }
                }
            }
        }

        private void drawTextAndIcons(Canvas canvas) {
            // draw bottom text
            if (!isDataEmpty()) {
                List<GraphEntry> dataLabels = mData.getDataLabels();
                for (int i = 0; i < dataLabels.size(); i++) {
                    float x = xCoordinateList.get(i);
                    float y = mViewHeight - bottomTextDescent;
                    GraphEntry entry = dataLabels.get(i);

                    if (entry.getXLabel() != null && !TextUtils.isEmpty(entry.getXLabel())) {
                        drwTextWidth = bottomTextPaint.measureText(entry.getXLabel().toString());
                        drawingRect.set(x, y, x + drwTextWidth, y + bottomTextHeight);

                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawText(entry.getXLabel().toString(), x, y, bottomTextPaint);
                    }

                    if (isDrawIconsLabels() && entry.getXIcon() != null) {
                        final int rotation = entry.getXIconRotation();

                        final Rect bounds = new Rect(0, 0, (int) iconHeight, (int) iconHeight);
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

        private void drawSeriesLegend(Canvas canvas) {
            if (drawSeriesLabels && !isDataEmpty()) {
                int seriesSize = mData.getDataCount();

                Rect r = new Rect();
                int longestWidth = 0;
                String longestStr = "";
                float textWidth = 0;
                float paddingLength = 0;
                float textHeight = 0;
                float textDescent = 0;
                for (int i = 0; i < seriesSize; i++) {
                    String title = mData.getDataSetByIndex(i).getSeriesLabel();
                    if (StringUtils.isNullOrWhitespace(title)) {
                        title = "Series " + i;
                    }

                    bottomTextPaint.getTextBounds(title, 0, title.length(), r);
                    if (textHeight < r.height()) {
                        textHeight = r.height();
                    }
                    if (longestWidth < r.width()) {
                        longestWidth = r.width();
                        longestStr = title;
                    }
                    if (textDescent < (Math.abs(r.bottom))) {
                        textDescent = Math.abs(r.bottom);
                    }
                }

                if (textWidth < longestWidth) {
                    textWidth = longestWidth + (int) bottomTextPaint.measureText(longestStr, 0, 1);
                }
                if (paddingLength < longestWidth / 2f) {
                    paddingLength = longestWidth / 2f;
                }
                textWidth += ContextUtils.dpToPx(getContext(), 4);

                float rectSize = textHeight;
                float rect2textPadding = ContextUtils.dpToPx(getContext(), 8);

                for (int i = 0; i < seriesSize; i++) {
                    LineDataSeries series = mData.getDataSetByIndex(i);
                    int seriesColor = series.getColor(i);
                    String title = series.getSeriesLabel();
                    if (StringUtils.isNullOrWhitespace(title)) {
                        title = String.format(LocaleUtils.getLocale(), "%s %d", getContext().getString(R.string.label_series), i);
                    }

                    Rect textBounds = new Rect();
                    bottomTextPaint.getTextBounds(title, 0, title.length(), textBounds);

                    float xRectStart = paddingLength + textWidth * i + ((rectSize + rect2textPadding) * i);
                    float xTextStart = paddingLength + textWidth * i + rectSize + ((rectSize + rect2textPadding) * i);

                    RectF rectF = new RectF(xRectStart, bottomTextTopMargin + textDescent, xRectStart + rectSize, rectSize + bottomTextTopMargin + textDescent);
                    seriesRectPaint.setColor(seriesColor);

                    canvas.drawRect(rectF, seriesRectPaint);
                    canvas.drawText(title, xTextStart + textWidth / 2f, textHeight + bottomTextTopMargin + textDescent, bottomTextPaint);
                }
            }
        }

        @Override
        protected int getGraphExtentWidth() {
            return Math.round(longestTextWidth * getMaxEntryCount());
        }

        @Override
        protected int getPreferredWidth() {
            return Math.round(backgroundGridWidth * getMaxEntryCount());
        }

        @Override
        protected void onPreMeasure() {
            refreshGridWidth();
            refreshXCoordinateList();
        }

        @Override
        protected void onPostMeasure() {
            refreshAfterDataChanged();
        }

        private class Dot {
            float x;
            float y;

            Dot(float x, float y) {
                this.x = x;
                this.y = y;
            }

            @NonNull
            @Override
            public String toString() {
                return "Dot{" + "x=" + x + ", y=" + y + '}';
            }
        }

        private class TextEntry {
            String text;
            float x;
            float y;

            TextEntry(String text, float x, float y) {
                this.text = text;
                this.x = x;
                this.y = y;
            }
        }
    }
}