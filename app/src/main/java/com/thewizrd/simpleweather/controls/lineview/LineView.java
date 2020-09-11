package com.thewizrd.simpleweather.controls.lineview;

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
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.ObservableArrayList;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class LineView extends HorizontalScrollView {

    private HorizontalScrollView mScrollViewer;
    private RectF visibleRect = new RectF();
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
    }

    public void setBottomTextColor(@ColorInt int color) {
        this.graph.BOTTOM_TEXT_COLOR = color;
        if (this.graph.bottomTextPaint != null) {
            this.graph.bottomTextPaint.setColor(this.graph.BOTTOM_TEXT_COLOR);
            this.graph.iconPaint.setColor(this.graph.BOTTOM_TEXT_COLOR);
        }
    }

    public void setLineColor(@ColorInt int color) {
        this.graph.LINE_COLOR = color;
        if (this.graph.smallCirPaint != null) {
            this.graph.smallCirPaint.setColor(this.graph.LINE_COLOR);
            this.graph.linePaint.setColor(this.graph.LINE_COLOR);
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
        this.graph.invalidate();
        visibleRect.setEmpty();
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
    private class LineViewGraph extends View {
        private int mViewHeight;
        private int mViewWidth;
        // Containers to check if we're drawing w/in bounds
        private RectF drawingRect = new RectF();
        private float drwTextWidth;
        //drawBackground
        private int dataOfAGird = 10;
        private float bottomTextHeight = 0;
        private ObservableArrayList<XLabelData> dataLabels; // X
        private ObservableArrayList<LineDataSeries> dataLists; // Y data

        private ArrayList<Float> xCoordinateList;
        private ArrayList<Float> yCoordinateList;
        private float horizontalGridNum;
        private float verticalGridNum;

        private List<ArrayList<Dot>> drawDotLists;

        private Paint bottomTextPaint;
        private int bottomTextDescent;

        private TextPaint iconPaint;

        private final float iconBottomMargin = ActivityUtils.dpToPx(getContext(), 2);
        private final float bottomTextTopMargin = ActivityUtils.dpToPx(getContext(), 6);
        private final float bottomLineLength = ActivityUtils.dpToPx(getContext(), 22);
        private final float DOT_INNER_CIR_RADIUS = ActivityUtils.dpToPx(getContext(), 2);
        private final float DOT_OUTER_CIR_RADIUS = ActivityUtils.dpToPx(getContext(), 5);
        private final float MIN_TOP_LINE_LENGTH = ActivityUtils.dpToPx(getContext(), 12);
        private final float LEGEND_MARGIN_HEIGHT = ActivityUtils.dpToPx(getContext(), 20);
        private final int MIN_VERTICAL_GRID_NUM = 4;
        private final int MIN_HORIZONTAL_GRID_NUM = 1;
        private int BACKGROUND_LINE_COLOR = Colors.WHITESMOKE;
        private int BOTTOM_TEXT_COLOR = Colors.WHITE;
        private int LINE_COLOR = Colors.BLACK;

        private float topLineLength = MIN_TOP_LINE_LENGTH;
        private float sideLineLength = ActivityUtils.dpToPx(getContext(), 45) / 3 * 2;
        private float backgroundGridWidth = ActivityUtils.dpToPx(getContext(), 45);
        private float longestTextWidth;

        private int[] colorArray = {Colors.SIMPLEBLUE, Colors.LIGHTSEAGREEN, Colors.YELLOWGREEN};

        private boolean drawGridLines = false;
        private boolean drawDotLine = false;
        private boolean drawDotPoints = false;
        private boolean drawGraphBackground = false;
        private boolean drawDataLabels = false;
        private boolean drawIconsLabels = false;
        private boolean drawSeriesLabels = false;

        private Paint bigCirPaint;
        private Paint smallCirPaint;
        private Paint linePaint;
        private Path mPathBackground;
        private Paint mPaintBackground;
        private Paint bgLinesPaint;
        private PathEffect dashEffects;
        private Paint seriesRectPaint;

        LineViewGraph(Context context) {
            this(context, null);
        }

        LineViewGraph(Context context, AttributeSet attrs) {
            super(context, attrs);
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
            bottomTextPaint.setTextSize(ActivityUtils.dpToPx(getContext(), 12));
            bottomTextPaint.setTextAlign(Paint.Align.CENTER);
            bottomTextPaint.setStyle(Paint.Style.FILL);
            bottomTextPaint.setColor(BOTTOM_TEXT_COLOR);
            bottomTextPaint.setShadowLayer(1, 1, 1, ColorsUtils.isSuperLight(BOTTOM_TEXT_COLOR) ? Colors.BLACK : Colors.GRAY);

            iconPaint = new TextPaint();
            iconPaint.setAntiAlias(true);
            iconPaint.setTextSize(ActivityUtils.dpToPx(getContext(), 24));
            iconPaint.setTextAlign(Paint.Align.CENTER);
            iconPaint.setStyle(Paint.Style.FILL);
            iconPaint.setColor(BOTTOM_TEXT_COLOR);
            Typeface weathericons = ResourcesCompat.getFont(getContext(), R.font.weathericons);
            iconPaint.setSubpixelText(true);
            iconPaint.setTypeface(weathericons);
            iconPaint.setShadowLayer(1, 1, 1, ColorsUtils.isSuperLight(BOTTOM_TEXT_COLOR) ? Colors.BLACK : Colors.GRAY);

            bigCirPaint.setAntiAlias(true);
            smallCirPaint = new Paint(bigCirPaint);
            smallCirPaint.setColor(LINE_COLOR);

            linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(ActivityUtils.dpToPx(getContext(), 2));

            mPathBackground = new Path();
            mPaintBackground = new Paint();

            bgLinesPaint = new Paint();
            bgLinesPaint.setStyle(Paint.Style.STROKE);
            bgLinesPaint.setStrokeWidth(ActivityUtils.dpToPx(getContext(), 1f));
            bgLinesPaint.setColor(BACKGROUND_LINE_COLOR);
            dashEffects = new DashPathEffect(new float[]{10, 5, 10, 5}, 1);

            seriesRectPaint = new Paint();
            seriesRectPaint.setAntiAlias(true);
            seriesRectPaint.setStyle(Paint.Style.FILL);
        }

        private int getAdj() {
            int adj = 1;
            if (drawIconsLabels) adj = 2; // Make space for icon labels

            return adj;
        }

        private float getGraphHeight() {
            float graphHeight = mViewHeight - bottomTextTopMargin * getAdj() - bottomTextHeight * getAdj() - bottomLineLength - bottomTextDescent - topLineLength;
            if (drawIconsLabels) graphHeight -= iconBottomMargin;

            return graphHeight;
        }

        private void resetData(boolean invalidate) {
            this.dataLists.clear();
            this.dataLabels.clear();
            this.xCoordinateList.clear();
            this.yCoordinateList.clear();
            this.drawDotLists.clear();
            bottomTextDescent = 0;
            longestTextWidth = 0;
            drawIconsLabels = false;
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
                        if (!drawIconsLabels && dataLabels != null && dataLabels.size() > 0 && !TextUtils.isEmpty(dataLabels.get(0).getIcon()))
                            drawIconsLabels = true;
                        else if (drawIconsLabels && (dataLabels == null || dataLabels.size() <= 0))
                            drawIconsLabels = false;

                        Rect r = new Rect();
                        int longestWidth = 0;
                        String longestStr = "";

                        for (XLabelData labelData : args.newItems) {
                            String s = labelData.getLabel().toString();
                            bottomTextPaint.getTextBounds(s, 0, s.length(), r);
                            if (bottomTextHeight < r.height()) {
                                bottomTextHeight = r.height();
                            }
                            if (longestWidth < r.width()) {
                                longestWidth = r.width();
                                longestStr = s;
                            }
                            if (bottomTextDescent < (Math.abs(r.bottom))) {
                                bottomTextDescent = Math.abs(r.bottom);
                            }

                            if (longestTextWidth < longestWidth) {
                                longestTextWidth = longestWidth + (int) bottomTextPaint.measureText(longestStr, 0, 1) * 2.25f;
                            }
                            if (sideLineLength < longestWidth / 2f) {
                                sideLineLength = longestWidth / 2f;
                            }

                            backgroundGridWidth = longestTextWidth;

                            // Add XCoordinate list
                            updateHorizontalGridNum();
                            refreshXCoordinateList();
                        }
                        break;
                    case REMOVE:
                        updateHorizontalGridNum();
                        break;
                    case MOVE:
                    case REPLACE:
                    case RESET:
                        bottomTextDescent = 0;
                        longestTextWidth = 0;
                        drawIconsLabels = false;
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
                        for (LineDataSeries series : args.newItems) {
                            if (series.getSeriesData().size() > dataLabels.size()) {
                                throw new RuntimeException("LineView error:" +
                                        " seriesData.size() > dataLabels.size() !!!");
                            }
                        }
                        float biggestData = 0;
                        for (LineDataSeries series : args.newItems) {
                            for (YEntryData i : series.getSeriesData()) {
                                if (biggestData < i.getY()) {
                                    biggestData = i.getY();
                                }
                            }
                            dataOfAGird = 1;
                            while (biggestData / 10 > dataOfAGird) {
                                dataOfAGird *= 10;
                            }
                        }

                        AsyncTask.await(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                updateAfterDataChanged(args.newItems);
                                return null;
                            }
                        });
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
                float additionalSpace = freeSpace / horizontalGridNum;
                backgroundGridWidth += additionalSpace;
                refreshXCoordinateList();
            }
        }

        private void refreshAfterDataChanged() {
            updateVerticalGridNum(dataLists);
            refreshTopLineLength();
            refreshYCoordinateList();
            refreshDrawDotList();
        }

        private void updateAfterDataChanged(List<LineDataSeries> dataSeriesList) {
            updateVerticalGridNum(dataSeriesList);
            refreshTopLineLength();
            refreshYCoordinateList();
            refreshDrawDotList();
        }

        private void updateVerticalGridNum(List<LineDataSeries> dataSeriesList) {
            if (dataSeriesList != null && !dataSeriesList.isEmpty()) {
                for (LineDataSeries series : dataSeriesList) {
                    for (YEntryData entry : series.getSeriesData()) {
                        verticalGridNum = Math.max(verticalGridNum,
                                Math.max(MIN_VERTICAL_GRID_NUM, entry.getY() + 1));
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
            xCoordinateList.ensureCapacity((int) Math.ceil(horizontalGridNum));
            for (int i = 0; i < (horizontalGridNum + 1); i++) {
                xCoordinateList.add(sideLineLength + backgroundGridWidth * i);
            }
        }

        private void refreshYCoordinateList() {
            yCoordinateList.clear();
            yCoordinateList.ensureCapacity((int) Math.ceil(horizontalGridNum));
            for (int i = 0; i < (verticalGridNum + 1); i++) {
                yCoordinateList.add(topLineLength + ((getGraphHeight()) * i / (verticalGridNum)));
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
                for (int k = 0; k < dataLists.size(); k++) {
                    int drawDotSize = drawDotLists.get(k).isEmpty() ? 0 : drawDotLists.get(k).size();

                    if (drawDotSize > 0) {
                        drawDotLists.get(k).ensureCapacity(dataLists.get(k).getSeriesData().size());
                    }

                    for (int i = 0; i < dataLists.get(k).getSeriesData().size(); i++) {
                        float x = xCoordinateList.get(i);
                        // Make space for y data labels
                        float y;
                        if (maxValue == minValue) {
                            y = topLineLength + (getGraphHeight()) / 2f;
                        } else {
                            y = topLineLength + (getGraphHeight()) * (maxValue - dataLists.get(k).getSeriesData().get(i).getY()) / (maxValue - minValue);
                        }

                        // Make space for each series if necessary
                        y += (topLineLength * k * 1.25);
                        if (y >= getGraphHeight()) {
                            y = getGraphHeight();
                        }

                        if (drawSeriesLabels) y += LEGEND_MARGIN_HEIGHT;

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

        private void refreshTopLineLength() {
            float labelsize = bottomTextHeight * 3f + bottomTextTopMargin;

            if (drawDataLabels && (getGraphHeight()) /
                    (verticalGridNum + 2) < labelsize) {
                topLineLength = labelsize + 2;
            } else {
                topLineLength = MIN_TOP_LINE_LENGTH;
            }
        }

        @Override
        public void invalidate() {
            setMinimumWidth(0);
            super.invalidate();
            visibleRect.setEmpty();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (visibleRect.isEmpty()) {
                visibleRect.set(mScrollViewer.getScrollX(),
                        mScrollViewer.getScrollY(),
                        mScrollViewer.getScrollX() + mScrollViewer.getWidth(),
                        mScrollViewer.getScrollY() + mScrollViewer.getHeight());
            }

            drawBackgroundLines(canvas);
            drawLines(canvas);
            drawDots(canvas);
            drawSeriesLegend(canvas);
        }

        private void drawDots(Canvas canvas) {
            if (drawDotPoints && drawDotLists != null && !drawDotLists.isEmpty()) {
                for (int k = 0; k < drawDotLists.size(); k++) {
                    bigCirPaint.setColor(colorArray[k % 3]);
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
                float graphTop = this.getTop() + topLineLength;

                for (int k = 0; k < drawDotLists.size(); k++) {
                    float firstX = -1;
                    float firstY = -1;
                    // needed to end the path for background
                    float lastUsedEndY = 0;

                    mPathBackground.rewind();
                    linePaint.setColor(LINE_COLOR);
                    mPaintBackground.setColor(ColorUtils.setAlphaComponent(colorArray[k % 3], 0x50));
                    for (int i = 0; i < drawDotLists.get(k).size() - 1; i++) {
                        Dot dot = drawDotLists.get(k).get(i);
                        Dot nextDot = drawDotLists.get(k).get(i + 1);
                        YEntryData entry = dataLists.get(k).getSeriesData().get(i);
                        YEntryData nextEntry = dataLists.get(k).getSeriesData().get(i + 1);

                        float startX = dot.x;
                        float startY = dot.y;
                        float endX = nextDot.x;
                        float endY = nextDot.y;

                        drawingRect.set(0, dot.y, dot.x, dot.y);
                        if (firstX == -1 && RectF.intersects(drawingRect, visibleRect)) {
                            canvas.drawLine(0, dot.y, dot.x, dot.y, linePaint);
                        }

                        drawingRect.set(dot.x, dot.y, nextDot.x, nextDot.y);
                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawLine(dot.x, dot.y, nextDot.x, nextDot.y, linePaint);

                        // Draw top label
                        drwTextWidth = bottomTextPaint.measureText(entry.getLabel().toString());
                        drawingRect.set(sideLineLength + backgroundGridWidth * i,
                                dot.y,
                                sideLineLength + backgroundGridWidth * i + drwTextWidth,
                                dot.y + bottomTextHeight);
                        if (drawDataLabels && RectF.intersects(drawingRect, visibleRect))
                            canvas.drawText(entry.getLabel().toString(), sideLineLength + backgroundGridWidth * i, dot.y - bottomTextHeight * 1.5f, bottomTextPaint);

                        if (firstX == -1) {
                            firstX = visibleRect.left;
                            firstY = startY;
                            if (drawGraphBackground)
                                mPathBackground.moveTo(firstX, startY);
                        }

                        drawingRect.set(startX, startY, endX, endY);
                        if (drawGraphBackground && RectF.intersects(drawingRect, visibleRect)) {
                            mPathBackground.lineTo(startX, startY);
                            mPathBackground.lineTo(endX, endY);
                        }

                        // Draw last items
                        if (i + 1 == drawDotLists.get(k).size() - 1) {
                            // Draw top label
                            drwTextWidth = bottomTextPaint.measureText(nextEntry.getLabel().toString());
                            drawingRect.set(sideLineLength + backgroundGridWidth * (i + 1),
                                    nextDot.y,
                                    sideLineLength + backgroundGridWidth * (i + 1) + drwTextWidth,
                                    nextDot.y + bottomTextHeight);

                            if (drawDataLabels && RectF.intersects(drawingRect, visibleRect))
                                canvas.drawText(nextEntry.getLabel().toString(), sideLineLength + backgroundGridWidth * (i + 1), nextDot.y - bottomTextHeight * 1.5f, bottomTextPaint);

                            drawingRect.set(nextDot.x, nextDot.y, visibleRect.right, nextDot.y);

                            if (RectF.intersects(drawingRect, visibleRect))
                                canvas.drawLine(nextDot.x, nextDot.y, visibleRect.right, nextDot.y, linePaint);

                            drawingRect.set(endX, endY, visibleRect.right, endY);
                            if (drawGraphBackground && RectF.intersects(drawingRect, visibleRect)) {
                                mPathBackground.lineTo(endX, endY);
                                mPathBackground.lineTo(visibleRect.right, endY);
                            }
                        }

                        lastUsedEndY = endY;
                    }

                    if (drawGraphBackground && firstX != -1) {
                        // end / close path
                        if (lastUsedEndY != graphHeight + graphTop) {
                            // dont draw line to same point, otherwise the path is completely broken
                            mPathBackground.lineTo(visibleRect.right, graphHeight + graphTop);
                        }
                        mPathBackground.lineTo(firstX, graphHeight + graphTop);
                        if (firstY != graphHeight + graphTop) {
                            // dont draw line to same point, otherwise the path is completely broken
                            mPathBackground.lineTo(firstX, firstY);
                        }

                        canvas.drawPath(mPathBackground, mPaintBackground);
                    }
                }
            }
        }

        private void drawBackgroundLines(Canvas canvas) {
            if (drawGridLines && !xCoordinateList.isEmpty()) {
                // draw vertical lines
                for (int i = 0; i < xCoordinateList.size(); i++) {
                    drawingRect.set(xCoordinateList.get(i), 0, xCoordinateList.get(i), getGraphHeight() + topLineLength);

                    if (RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(xCoordinateList.get(i),
                                0,
                                xCoordinateList.get(i),
                                getGraphHeight() + topLineLength,
                                bgLinesPaint);
                    }
                }

                // draw dotted lines
                bgLinesPaint.setPathEffect(drawDotLine ? dashEffects : null);

                // draw solid lines
                for (int i = 0; i < yCoordinateList.size(); i++) {
                    drawingRect.set(0, yCoordinateList.get(i), visibleRect.right, yCoordinateList.get(i));

                    if ((yCoordinateList.size() - 1 - i) % dataOfAGird == 0 && RectF.intersects(drawingRect, visibleRect)) {
                        canvas.drawLine(0, yCoordinateList.get(i), visibleRect.right, yCoordinateList.get(i), bgLinesPaint);
                    }
                }
            }

            //draw bottom text
            if (dataLabels != null) {
                for (int i = 0; i < dataLabels.size(); i++) {
                    float x = sideLineLength + backgroundGridWidth * i;
                    float y = mViewHeight - bottomTextDescent;
                    XLabelData xData = dataLabels.get(i);

                    if (!TextUtils.isEmpty(xData.getLabel())) {
                        drwTextWidth = bottomTextPaint.measureText(xData.getLabel().toString());
                        drawingRect.set(x, y, x + drwTextWidth, y + bottomTextHeight);

                        if (RectF.intersects(drawingRect, visibleRect))
                            canvas.drawText(xData.getLabel().toString(), x, y, bottomTextPaint);
                    }

                    if (!TextUtils.isEmpty(xData.getIcon())) {
                        int rotation = xData.getIconRotation();
                        String icon = xData.getIcon().toString();
                        Rect r = new Rect();
                        iconPaint.getTextBounds(icon, 0, icon.length(), r);

                        drawingRect.set(x, y, x + r.width(), y + r.height());

                        if (RectF.intersects(drawingRect, visibleRect)) {
                            StaticLayout mTextLayout = new StaticLayout(
                                    icon, iconPaint, r.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                            canvas.save();
                            canvas.translate(x, y - mTextLayout.getHeight() - bottomTextHeight - iconBottomMargin * 2f);
                            canvas.rotate(rotation, 0, mTextLayout.getHeight() / 2f);
                            mTextLayout.draw(canvas);
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
                textWidth += ActivityUtils.dpToPx(getContext(), 4);

                float rectSize = textHeight;
                float rect2textPadding = ActivityUtils.dpToPx(getContext(), 8);

                for (int i = 0; i < seriesSize; i++) {
                    int seriesColor = colorArray[i % 3];
                    String title = dataLists.get(i).getSeriesLabel();
                    if (StringUtils.isNullOrWhitespace(title)) {
                        title = String.format(Locale.getDefault(), "%s %d", getContext().getString(R.string.label_series), i);
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

        private int getItemPositionFromPoint(float xCoordinate) {
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
            refreshGridWidth();
            refreshAfterDataChanged();
            setMeasuredDimension(mViewWidth, mViewHeight);
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
    }
}