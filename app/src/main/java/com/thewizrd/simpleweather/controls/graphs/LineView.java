package com.thewizrd.simpleweather.controls.graphs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.ObservableArrayList;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class LineView extends HorizontalScrollView implements IGraph {

    private HorizontalScrollView mScrollViewer;
    private final RectF visibleRect = new RectF();
    private LineViewGraph graph;
    private OnClickListener onClickListener;

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param v          The view whose scroll position has changed.
         * @param scrollX    Current horizontal scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         */
        void onScrollChange(LineView v, int scrollX, int oldScrollX);
    }

    private OnScrollChangeListener mOnScrollChangeListener;

    public interface OnSizeChangedListener {
        void onSizeChanged(LineView v, int canvasWidth);
    }

    private OnSizeChangedListener mOnSizeChangedListener;

    public LineView(Context context) {
        super(context);
        initialize(context);
    }

    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public LineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LineView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
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

    public void setOnSizeChangedListener(@Nullable OnSizeChangedListener l) {
        mOnSizeChangedListener = l;
    }

    private void initialize(Context context) {
        graph = new LineViewGraph(context);
        graph.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null)
                    onClickListener.onClick(v);
            }
        });

        this.setFillViewport(true);
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.setOverScrollMode(View.OVER_SCROLL_NEVER);

        this.removeAllViews();
        this.addView(graph, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mScrollViewer = this;
    }

    public void setDrawGridLines(boolean drawGridLines) {
        this.graph.drawGridLines = drawGridLines;
    }

    public void setDrawDotLine(boolean drawDotLine) {
        this.graph.drawDotLine = drawDotLine;
    }

    public void setDrawDotPoints(boolean drawDotPoints) {
        this.graph.drawDotPoints = drawDotPoints;
    }

    public void setDrawGraphBackground(boolean drawGraphBackground) {
        this.graph.drawGraphBackground = drawGraphBackground;
    }

    public void setDrawIconLabels(boolean drawIconsLabels) {
        this.graph.drawIconsLabels = drawIconsLabels;
    }

    public void setDrawDataLabels(boolean drawDataLabels) {
        this.graph.drawDataLabels = drawDataLabels;
    }

    public void setDrawSeriesLabels(boolean drawSeriesLabels) {
        this.graph.drawSeriesLabels = drawSeriesLabels;
    }

    public void setBackgroundLineColor(@ColorInt int color) {
        this.graph.BACKGROUND_LINE_COLOR = color;
        if (this.graph.bgLinesPaint != null) {
            this.graph.bgLinesPaint.setColor(this.graph.BACKGROUND_LINE_COLOR);
        }
    }

    public void setBottomTextColor(@ColorInt int color) {
        this.graph.BOTTOM_TEXT_COLOR = color;
        if (this.graph.bottomTextPaint != null) {
            this.graph.bottomTextPaint.setColor(this.graph.BOTTOM_TEXT_COLOR);
        }
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

    public final int getItemPositionFromPoint(float xCoordinate) {
        return this.graph.getItemPositionFromPoint(xCoordinate);
    }

    public List<XLabelData> getDataLabels() {
        return this.graph.dataLabels;
    }

    public List<LineDataSeries> getDataLists() {
        return this.graph.dataLists;
    }

    public void resetData(boolean invalidate) {
        this.graph.resetData(invalidate);
    }

    /*
     *  Multi-series line graph
     *  Based on LineView from http://www.androidtrainee.com/draw-android-line-chart-with-animation/
     *  Graph background (under line) based on - https://github.com/jjoe64/GraphView (LineGraphSeries)
     */
    private class LineViewGraph extends View implements IGraph {
        private int mViewHeight;
        private int mViewWidth;
        // Containers to check if we're drawing w/in bounds
        private final RectF drawingRect = new RectF();
        private float drwTextWidth;
        private int dataOfAGird = 10;
        private float bottomTextHeight = 0;
        private final ObservableArrayList<XLabelData> dataLabels; // X
        private final ObservableArrayList<LineDataSeries> dataLists; // Y data

        private final ArrayList<Float> xCoordinateList;
        private final ArrayList<Float> yCoordinateList;
        private int horizontalGridNum;
        private int verticalGridNum;

        private final List<ArrayList<Dot>> drawDotLists;

        private final Paint bottomTextPaint;
        private int bottomTextDescent;

        private final float iconHeight;

        private final float iconBottomMargin = ContextUtils.dpToPx(getContext(), 2);
        private final float bottomTextTopMargin = ContextUtils.dpToPx(getContext(), 6);
        private final float DOT_INNER_CIR_RADIUS = ContextUtils.dpToPx(getContext(), 2);
        private final float DOT_OUTER_CIR_RADIUS = ContextUtils.dpToPx(getContext(), 5);
        private final int MIN_VERTICAL_GRID_NUM = 4;
        private final int MIN_HORIZONTAL_GRID_NUM = 1;
        private int BACKGROUND_LINE_COLOR = Colors.WHITESMOKE;
        private int BOTTOM_TEXT_COLOR = Colors.WHITE;

        private float sideLineLength = 0;
        private float backgroundGridWidth = ContextUtils.dpToPx(getContext(), 45);
        private float longestTextWidth;

        private boolean drawGridLines = false;
        private boolean drawDotLine = false;
        private boolean drawDotPoints = false;
        private boolean drawGraphBackground = false;
        private boolean drawDataLabels = false;
        private boolean drawIconsLabels = false;
        private boolean drawSeriesLabels = false;

        private final Paint bigCirPaint;
        private final Paint smallCirPaint;
        private final Paint linePaint;
        private final Path mPathBackground;
        private final Paint mPaintBackground;
        private final Paint bgLinesPaint;
        private final PathEffect dashEffects;
        private final Paint seriesRectPaint;

        private final Stack<AnimatedVectorDrawable> animatedDrawables = new Stack<>();

        LineViewGraph(Context context) {
            this(context, null);
        }

        LineViewGraph(Context context, AttributeSet attrs) {
            super(context, attrs);
            setWillNotDraw(false);

            bottomTextPaint = new Paint();
            bigCirPaint = new Paint();
            dataLabels = new ObservableArrayList<>();
            dataLists = new ObservableArrayList<>();
            xCoordinateList = new ArrayList<>();
            yCoordinateList = new ArrayList<>();
            drawDotLists = new ArrayList<>();

            resetData(false);
            dataLabels.addOnListChangedCallback(onXLabelDataChangedListener);
            dataLists.addOnListChangedCallback(onLineDataSeriesChangedListener);

            bottomTextPaint.setAntiAlias(true);
            bottomTextPaint.setTextSize(ContextUtils.dpToPx(getContext(), 12));
            bottomTextPaint.setTextAlign(Paint.Align.CENTER);
            bottomTextPaint.setStyle(Paint.Style.FILL);
            bottomTextPaint.setColor(BOTTOM_TEXT_COLOR);
            bottomTextPaint.setShadowLayer(1, 1, 1, ColorsUtils.isSuperLight(BOTTOM_TEXT_COLOR) ? Colors.BLACK : Colors.GRAY);

            iconHeight = ContextUtils.dpToPx(getContext(), 30);

            bigCirPaint.setAntiAlias(true);
            smallCirPaint = new Paint(bigCirPaint);

            linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(ContextUtils.dpToPx(getContext(), 2));

            mPathBackground = new Path();
            mPaintBackground = new Paint();

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
            if (drawIconsLabels) graphHeight -= iconHeight;

            return graphHeight;
        }

        private float getLegendHeight() {
            return bottomTextTopMargin + bottomTextHeight * 2f + bottomTextDescent * 2f;
        }

        private void resetData(boolean invalidate) {
            this.dataLists.clear();
            this.dataLabels.clear();
            this.xCoordinateList.clear();
            this.yCoordinateList.clear();
            this.drawDotLists.clear();
            bottomTextDescent = 0;
            longestTextWidth = 0;
            horizontalGridNum = MIN_HORIZONTAL_GRID_NUM;
            verticalGridNum = MIN_VERTICAL_GRID_NUM;
            if (invalidate) {
                this.postInvalidate();
            }
        }

        private OnListChangedListener<XLabelData> onXLabelDataChangedListener = new OnListChangedListener<XLabelData>() {
            @Override
            public void onChanged(@NonNull ArrayList<XLabelData> sender, @NonNull ListChangedArgs<XLabelData> args) {
                switch (args.action) {
                    case ADD:
                        Rect r = new Rect();
                        float longestWidth = 0;
                        String longestStr = "";

                        for (XLabelData labelData : args.newItems) {
                            String s = labelData.getLabel().toString();
                            bottomTextPaint.getTextBounds(s, 0, s.length(), r);
                            if (bottomTextHeight < r.height()) {
                                bottomTextHeight = r.height();
                            }
                            if (longestWidth < r.width()) {
                                longestStr = s;
                                longestWidth = r.width() + bottomTextPaint.measureText(longestStr, 0, 1) * 2.25f;
                            }
                            if (bottomTextDescent < (Math.abs(r.bottom))) {
                                bottomTextDescent = Math.abs(r.bottom);
                            }
                        }

                        if (longestTextWidth < longestWidth) {
                            longestTextWidth = longestWidth;
                        }
                        if (sideLineLength < longestWidth / 2f) {
                            sideLineLength = longestWidth / 2f;
                        }

                        backgroundGridWidth = longestTextWidth;

                        // Add XCoordinate list
                        updateHorizontalGridNum();
                        refreshXCoordinateList();
                        break;
                    case REMOVE:
                        updateHorizontalGridNum();
                        break;
                    case MOVE:
                    case REPLACE:
                    case RESET:
                        bottomTextDescent = 0;
                        longestTextWidth = 0;
                        updateHorizontalGridNum();
                        refreshXCoordinateList();
                        break;
                }
            }
        };

        private OnListChangedListener<LineDataSeries> onLineDataSeriesChangedListener = new OnListChangedListener<LineDataSeries>() {
            @Override
            public void onChanged(@NonNull ArrayList<LineDataSeries> sender, @NonNull final ListChangedArgs<LineDataSeries> args) {
                switch (args.action) {
                    case ADD:
                        float biggestData = 0;
                        final float prevLongestTextWidth = longestTextWidth;
                        Rect r = new Rect();
                        float longestWidth = 0;
                        String longestStr = "";

                        for (LineDataSeries series : args.newItems) {
                            if (series.getSeriesData().size() > dataLabels.size()) {
                                throw new RuntimeException("LineView error:" +
                                        " seriesData.size() > dataLabels.size() !!!");
                            }

                            for (YEntryData i : series.getSeriesData()) {
                                if (biggestData < i.getY()) {
                                    biggestData = i.getY();
                                }

                                // Measure Y label
                                String s = i.getLabel().toString();
                                bottomTextPaint.getTextBounds(s, 0, s.length(), r);
                                if (longestWidth < r.width()) {
                                    longestStr = s;
                                    longestWidth = r.width() + bottomTextPaint.measureText(longestStr, 0, 1) * 2.25f;
                                }
                                if (longestTextWidth < longestWidth) {
                                    longestTextWidth = longestWidth;
                                }
                                if (sideLineLength < longestWidth / 2f) {
                                    sideLineLength = longestWidth / 2f;
                                }
                            }
                            dataOfAGird = 1;
                            while (biggestData / 10 > dataOfAGird) {
                                dataOfAGird *= 10;
                            }
                        }

                        backgroundGridWidth = longestTextWidth;

                        if (prevLongestTextWidth != longestTextWidth) {
                            refreshXCoordinateList();
                        }
                        updateAfterDataChanged(args.newItems);
                        postInvalidate();
                        break;
                    case MOVE:
                    case REMOVE:
                    case REPLACE:
                        refreshAfterDataChanged();
                        postInvalidate();
                        break;
                    case RESET:
                        verticalGridNum = MIN_VERTICAL_GRID_NUM;

                        longestTextWidth = 0;
                        refreshXCoordinateList();

                        refreshAfterDataChanged();
                        postInvalidate();
                        break;
                }
            }
        };

        private void refreshGridWidth() {
            // Reset the grid width
            backgroundGridWidth = longestTextWidth;

            if (getPreferredWidth() < mScrollViewer.getMeasuredWidth()) {
                int freeSpace = mScrollViewer.getMeasuredWidth() - getPreferredWidth();
                float additionalSpace = (float) freeSpace / horizontalGridNum;
                backgroundGridWidth += additionalSpace;
            }
            refreshXCoordinateList();
        }

        private void refreshAfterDataChanged() {
            updateVerticalGridNum(dataLists);
            refreshYCoordinateList();
            refreshDrawDotList();
        }

        private void updateAfterDataChanged(List<LineDataSeries> dataSeriesList) {
            updateVerticalGridNum(dataSeriesList);
            refreshYCoordinateList();
            refreshDrawDotList();
        }

        private void updateVerticalGridNum(List<LineDataSeries> dataSeriesList) {
            if (dataSeriesList != null && !dataSeriesList.isEmpty()) {
                for (LineDataSeries series : dataSeriesList) {
                    for (YEntryData entry : series.getSeriesData()) {
                        verticalGridNum = Math.max(verticalGridNum, Math.max(MIN_VERTICAL_GRID_NUM, (int) Math.ceil(entry.getY()) + 1));
                    }
                }
            } else {
                verticalGridNum = MIN_VERTICAL_GRID_NUM;
            }
        }

        private void updateHorizontalGridNum() {
            horizontalGridNum = Math.max(horizontalGridNum,
                    Math.max(MIN_HORIZONTAL_GRID_NUM, dataLabels.size() - 1));
        }

        private void refreshXCoordinateList() {
            xCoordinateList.clear();
            xCoordinateList.ensureCapacity(horizontalGridNum);
            for (int i = 0; i < (horizontalGridNum + 1); i++) {
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
                yCoordinateList.add(((float) i / (verticalGridNum)) * (getGraphHeight() - getGraphTop()) + getGraphTop());
            }
        }

        private void refreshDrawDotList() {
            if (dataLists != null && !dataLists.isEmpty()) {
                if (drawDotLists.size() == 0 || drawDotLists.size() != dataLists.size()) {
                    drawDotLists.clear();
                    for (int k = 0; k < dataLists.size(); k++) {
                        drawDotLists.add(new ArrayList<LineViewGraph.Dot>());
                    }
                }
                float maxValue = 0;
                float minValue = 0;
                for (int k = 0; k < dataLists.size(); k++) {
                    float kMax = 0;
                    float kMin = 0;
                    for (YEntryData seriesData : dataLists.get(k).getSeriesData()) {
                        if (kMax < seriesData.getY())
                            kMax = seriesData.getY();
                        if (kMin > seriesData.getY())
                            kMin = seriesData.getY();
                    }

                    if (maxValue < kMax)
                        maxValue = kMax;
                    if (minValue > kMin)
                        minValue = kMin;
                }

                final float graphHeight = getGraphHeight();
                final float graphTop = getGraphTop();

                for (int k = 0; k < dataLists.size(); k++) {
                    int drawDotSize = drawDotLists.get(k).isEmpty() ? 0 : drawDotLists.get(k).size();

                    if (drawDotSize > 0) {
                        drawDotLists.get(k).ensureCapacity(dataLists.get(k).getSeriesData().size());
                    }

                    for (int i = 0; i < dataLists.get(k).getSeriesData().size(); i++) {
                        float x = xCoordinateList.get(i);
                        float y;
                        if (maxValue == minValue) {
                            if (maxValue == 0) {
                                y = graphHeight;
                            } else if (maxValue == 100) {
                                y = graphTop;
                            } else {
                                y = graphHeight / 2f;
                            }
                        } else {
                            /*
                             * Scaling formula
                             *
                             * ((value - minValue) / (maxValue - minValue)) * (scaleMax - scaleMin) + scaleMin
                             * graphTop is scaleMax & graphHeight is scaleMin due to View coordinate system
                             */
                            y = ((dataLists.get(k).getSeriesData().get(i).getY() - minValue) / (maxValue - minValue)) * (graphTop - graphHeight) + graphHeight;
                        }

                        if (i > drawDotSize - 1) {
                            drawDotLists.get(k).add(new Dot(x, y));
                        } else {
                            drawDotLists.get(k).set(i, new Dot(x, y));
                        }
                    }

                    int temp = drawDotLists.get(k).size() - dataLists.get(k).getSeriesData().size();
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
                visibleRect.set(mScrollViewer.getScrollX(),
                        mScrollViewer.getScrollY(),
                        mScrollViewer.getScrollX() + mScrollViewer.getWidth(),
                        mScrollViewer.getScrollY() + mScrollViewer.getHeight());
            }

            // Stop running animations
            while (!animatedDrawables.empty()) {
                AnimatedVectorDrawable drw = animatedDrawables.pop();
                drw.stop();
                drw = null;
            }

            drawBackgroundLines(canvas);
            drawLines(canvas);
            drawDots(canvas);
            drawSeriesLegend(canvas);
        }

        private void drawDots(Canvas canvas) {
            if (drawDotPoints && drawDotLists != null && !drawDotLists.isEmpty()) {
                for (int k = 0; k < drawDotLists.size(); k++) {
                    LineDataSeries series = dataLists.get(k);
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
                    LineDataSeries series = dataLists.get(k);
                    List<TextEntry> textEntries = new LinkedList<>();

                    float firstX = -1;
                    // needed to end the path for background
                    Dot currentDot = null;

                    mPathBackground.rewind();
                    linePaint.setColor(series.getColor(k));
                    mPaintBackground.setColor(ColorUtils.setAlphaComponent(series.getColor(k), 0x99));

                    if (drawGraphBackground) {
                        mPathBackground.moveTo(visibleRect.left, graphHeight);
                    }

                    for (int i = 0; i < drawDotLists.get(k).size() - 1; i++) {
                        Dot dot = drawDotLists.get(k).get(i);
                        Dot nextDot = drawDotLists.get(k).get(i + 1);
                        YEntryData entry = dataLists.get(k).getSeriesData().get(i);
                        YEntryData nextEntry = dataLists.get(k).getSeriesData().get(i + 1);

                        float startX = dot.x;
                        float startY = dot.y;
                        float endX = nextDot.x;
                        float endY = nextDot.y;

                        drawingRect.set(visibleRect.left, dot.y, dot.x, dot.y);
                        if (firstX == -1 && RectF.intersects(drawingRect, visibleRect)) {
                            canvas.drawLine(visibleRect.left, dot.y, dot.x, dot.y, linePaint);
                        }

                        drawingRect.set(dot.x, dot.y, nextDot.x, nextDot.y);
                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawLine(dot.x, dot.y, nextDot.x, nextDot.y, linePaint);

                        if (drawDataLabels) {
                            // Draw top label
                            drwTextWidth = bottomTextPaint.measureText(entry.getLabel().toString());
                            drawingRect.set(dot.x, dot.y, dot.x + drwTextWidth, dot.y + bottomTextHeight);
                            if (RectF.intersects(drawingRect, visibleRect))
                                textEntries.add(new TextEntry(entry.getLabel().toString(), dot.x, dot.y - bottomTextHeight - bottomTextDescent));
                        }

                        if (firstX == -1) {
                            firstX = visibleRect.left;
                            if (drawGraphBackground)
                                mPathBackground.lineTo(firstX, startY);
                        }

                        if (drawGraphBackground) {
                            mPathBackground.lineTo(startX, startY);
                        }

                        currentDot = dot;

                        // Draw last items
                        if (i + 1 == drawDotLists.get(k).size() - 1) {
                            if (drawDataLabels) {
                                // Draw top label
                                drwTextWidth = bottomTextPaint.measureText(nextEntry.getLabel().toString());
                                drawingRect.set(nextDot.x, nextDot.y, nextDot.x + drwTextWidth, nextDot.y + bottomTextHeight);

                                if (RectF.intersects(drawingRect, visibleRect))
                                    textEntries.add(new TextEntry(nextEntry.getLabel().toString(), nextDot.x, nextDot.y - bottomTextHeight - bottomTextDescent));
                            }

                            drawingRect.set(nextDot.x, nextDot.y, visibleRect.right, nextDot.y);

                            if (RectF.intersects(drawingRect, visibleRect))
                                canvas.drawLine(nextDot.x, nextDot.y, visibleRect.right, nextDot.y, linePaint);

                            currentDot = nextDot;

                            if (drawGraphBackground) {
                                mPathBackground.lineTo(endX, endY);
                            }
                        }
                    }

                    if (drawGraphBackground) {
                        if (currentDot != null) {
                            mPathBackground.lineTo(visibleRect.right, currentDot.y);
                        }
                        if (firstX != -1) {
                            mPathBackground.lineTo(visibleRect.right, graphHeight);
                        }

                        mPathBackground.close();

                        canvas.drawPath(mPathBackground, mPaintBackground);
                    }

                    if (drawDataLabels) {
                        for (TextEntry entry : textEntries) {
                            canvas.drawText(entry.text, entry.x, entry.y, bottomTextPaint);
                        }
                    }
                }
            }
        }

        private void drawBackgroundLines(Canvas canvas) {
            if (drawGridLines && !xCoordinateList.isEmpty()) {
                // draw vertical lines
                for (int i = 0; i < xCoordinateList.size(); i++) {
                    drawingRect.set(xCoordinateList.get(i), getGraphTop(), xCoordinateList.get(i), getGraphHeight());

                    if (RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(xCoordinateList.get(i),
                                getGraphTop(),
                                xCoordinateList.get(i),
                                getGraphHeight(),
                                bgLinesPaint);
                    }
                }

                // draw dotted lines
                bgLinesPaint.setPathEffect(drawDotLine ? dashEffects : null);

                // draw horizontal lines
                for (int i = 0; i < yCoordinateList.size(); i++) {
                    drawingRect.set(visibleRect.left, yCoordinateList.get(i), visibleRect.right, yCoordinateList.get(i));

                    if ((yCoordinateList.size() - 1 - i) % dataOfAGird == 0 && RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(visibleRect.left, yCoordinateList.get(i), visibleRect.right, yCoordinateList.get(i), bgLinesPaint);
                    }
                }
            }

            // draw bottom text
            if (dataLabels != null) {
                for (int i = 0; i < dataLabels.size(); i++) {
                    float x = xCoordinateList.get(i);
                    float y = mViewHeight - bottomTextDescent;
                    XLabelData xData = dataLabels.get(i);

                    if (!TextUtils.isEmpty(xData.getLabel())) {
                        drwTextWidth = bottomTextPaint.measureText(xData.getLabel().toString());
                        drawingRect.set(x, y, x + drwTextWidth, y + bottomTextHeight);

                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawText(xData.getLabel().toString(), x, y, bottomTextPaint);
                    }

                    if (drawIconsLabels && xData.getIcon() != 0) {
                        int rotation = xData.getIconRotation();
                        Drawable iconDrawable = ContextCompat.getDrawable(getContext(), xData.getIcon());

                        Rect bounds = new Rect(0, 0, (int) iconHeight, (int) iconHeight);
                        iconDrawable.setBounds(bounds);
                        drawingRect.set(x, y, x + bounds.width(), y + bounds.height());

                        if (RectF.intersects(drawingRect, visibleRect)) {
                            if (iconDrawable instanceof AnimatedVectorDrawable) {
                                ((AnimatedVectorDrawable) iconDrawable).start();

                                animatedDrawables.push((AnimatedVectorDrawable) iconDrawable);
                            }

                            canvas.save();
                            canvas.translate(x - bounds.width() / 2f, y - bounds.height() - bottomTextHeight - iconBottomMargin);
                            canvas.rotate(rotation, bounds.width() / 2f, bounds.height() / 2f);
                            iconDrawable.draw(canvas);
                            canvas.restore();
                        }
                    }
                }
            }
        }

        private void drawSeriesLegend(Canvas canvas) {
            if (drawSeriesLabels && !dataLists.isEmpty()) {
                int seriesSize = dataLists.size();

                Rect r = new Rect();
                int longestWidth = 0;
                String longestStr = "";
                float textWidth = 0;
                float paddingLength = 0;
                float textHeight = 0;
                float textDescent = 0;
                for (int i = 0; i < seriesSize; i++) {
                    String title = dataLists.get(i).getSeriesLabel();
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
                    LineDataSeries series = dataLists.get(i);
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
        public int getItemPositionFromPoint(float xCoordinate) {
            if (horizontalGridNum <= 1) {
                return 0;
            }

            return binarySearchPointIndex(xCoordinate);
        }

        private int binarySearchPointIndex(float targetXPoint) {
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
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            mViewWidth = measureWidth(widthMeasureSpec);
            mViewHeight = measureHeight(heightMeasureSpec);
            setMeasuredDimension(mViewWidth, mViewHeight);
            refreshGridWidth();
            refreshAfterDataChanged();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (mOnSizeChangedListener != null)
                mOnSizeChangedListener.onSizeChanged(LineView.this, xCoordinateList.size() > 0 ? Iterables.getLast(xCoordinateList).intValue() : 0);
        }

        private int getPreferredWidth() {
            return (int) ((backgroundGridWidth * horizontalGridNum) + (sideLineLength * 2));
        }

        private int measureWidth(int measureSpec) {
            return getMeasurement(measureSpec, getPreferredWidth());
        }

        private int measureHeight(int measureSpec) {
            int preferred = 0;
            return getMeasurement(measureSpec, preferred);
        }

        private int getMeasurement(int measureSpec, int preferred) {
            int specSize = MeasureSpec.getSize(measureSpec);
            int measurement;
            switch (MeasureSpec.getMode(measureSpec)) {
                case MeasureSpec.EXACTLY:
                    measurement = specSize;
                    break;
                case MeasureSpec.AT_MOST:
                    measurement = Math.min(preferred, specSize);
                    break;
                case MeasureSpec.UNSPECIFIED:
                default:
                    measurement = preferred;
                    break;
            }
            return measurement;
        }

        private class Dot {
            float x;
            float y;

            Dot(float x, float y) {
                this.x = x;
                this.y = y;
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