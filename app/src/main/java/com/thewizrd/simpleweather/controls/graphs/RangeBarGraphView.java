package com.thewizrd.simpleweather.controls.graphs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
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

import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.ObservableArrayList;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.controls.GraphTemperature;

import java.util.ArrayList;
import java.util.List;

public class RangeBarGraphView extends HorizontalScrollView implements IGraph {

    private HorizontalScrollView mScrollViewer;
    private final RectF visibleRect = new RectF();
    private RangeBarChartGraph graph;
    private OnClickListener onClickListener;

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param v          The view whose scroll position has changed.
         * @param scrollX    Current horizontal scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         */
        void onScrollChange(RangeBarGraphView v, int scrollX, int oldScrollX);
    }

    private OnScrollChangeListener mOnScrollChangeListener;

    public interface OnSizeChangedListener {
        void onSizeChanged(RangeBarGraphView v, int canvasWidth);
    }

    private OnSizeChangedListener mOnSizeChangedListener;

    public RangeBarGraphView(Context context) {
        super(context);
        initialize(context);
    }

    public RangeBarGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public RangeBarGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RangeBarGraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
     * @see View#getScrollX()
     */
    public void setOnScrollChangedListener(@Nullable OnScrollChangeListener l) {
        mOnScrollChangeListener = l;
    }

    public void setOnSizeChangedListener(@Nullable OnSizeChangedListener l) {
        mOnSizeChangedListener = l;
    }

    private void initialize(Context context) {
        graph = new RangeBarChartGraph(context);
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

    public void setBottomTextColor(@ColorInt int color) {
        this.graph.BOTTOM_TEXT_COLOR = color;
        if (this.graph.bottomTextPaint != null) {
            this.graph.bottomTextPaint.setColor(this.graph.BOTTOM_TEXT_COLOR);
        }
    }

    public void setDrawIconLabels(boolean drawIconsLabels) {
        this.graph.drawIconsLabels = drawIconsLabels;
    }

    public void setDrawDataLabels(boolean drawDataLabels) {
        this.graph.drawDataLabels = drawDataLabels;
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

    public List<GraphTemperature> getDataLists() {
        return this.graph.dataLists;
    }

    public void resetData(boolean invalidate) {
        this.graph.resetData(invalidate);
    }

    private class RangeBarChartGraph extends View implements IGraph {
        private int mViewHeight;
        private int mViewWidth;
        // Containers to check if we're drawing w/in bounds
        private final RectF drawingRect = new RectF();
        private float drwTextWidth;
        private float bottomTextHeight = 0;
        private final ObservableArrayList<XLabelData> dataLabels; // X
        private final ObservableArrayList<GraphTemperature> dataLists; // Y data

        private final ArrayList<Float> xCoordinateList;
        private int horizontalGridNum;

        private final ArrayList<Bar> drawDotLists;

        private final Paint bottomTextPaint;
        private int bottomTextDescent;

        private final float iconHeight;

        private final float iconBottomMargin = ActivityUtils.dpToPx(getContext(), 2);
        private final float bottomTextTopMargin = ActivityUtils.dpToPx(getContext(), 6);
        private final int MIN_HORIZONTAL_GRID_NUM = 1;
        private int BOTTOM_TEXT_COLOR = Colors.WHITE;

        private float sideLineLength = 0;
        private float backgroundGridWidth = ActivityUtils.dpToPx(getContext(), 45);
        private float longestTextWidth;

        private boolean drawDataLabels = false;
        private boolean drawIconsLabels = false;

        private final Paint linePaint;

        RangeBarChartGraph(Context context) {
            this(context, null);
        }

        RangeBarChartGraph(Context context, AttributeSet attrs) {
            super(context, attrs);
            setWillNotDraw(false);

            bottomTextPaint = new Paint();
            dataLabels = new ObservableArrayList<>();
            dataLists = new ObservableArrayList<>();
            xCoordinateList = new ArrayList<>();
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

            iconHeight = ActivityUtils.dpToPx(getContext(), 30);

            linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(ActivityUtils.dpToPx(getContext(), 8));
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

        private void resetData(boolean invalidate) {
            this.dataLists.clear();
            this.dataLabels.clear();
            this.xCoordinateList.clear();
            this.drawDotLists.clear();
            bottomTextDescent = 0;
            longestTextWidth = 0;
            horizontalGridNum = MIN_HORIZONTAL_GRID_NUM;
            if (invalidate) {
                this.postInvalidate();
            }
        }

        private final OnListChangedListener<XLabelData> onXLabelDataChangedListener = new OnListChangedListener<XLabelData>() {
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

        private final OnListChangedListener<GraphTemperature> onLineDataSeriesChangedListener = new OnListChangedListener<GraphTemperature>() {
            @Override
            public void onChanged(@NonNull ArrayList<GraphTemperature> sender, @NonNull final ListChangedArgs<GraphTemperature> args) {
                switch (args.action) {
                    case ADD:
                    case MOVE:
                    case REMOVE:
                    case REPLACE:
                        refreshDrawDotList();
                        postInvalidate();
                        break;
                    case RESET:
                        longestTextWidth = 0;
                        refreshXCoordinateList();

                        refreshDrawDotList();
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

        private void refreshDrawDotList() {
            if (dataLists != null && !dataLists.isEmpty()) {
                drawDotLists.clear();
                float maxValue = Float.MIN_VALUE;
                float minValue = Float.MAX_VALUE;
                for (GraphTemperature tempData : dataLists) {
                    if (tempData.getHiTempData() != null)
                        maxValue = Math.max(maxValue, tempData.getHiTempData().getY());
                    if (tempData.getLoTempData() != null)
                        minValue = Math.min(minValue, tempData.getLoTempData().getY());
                }

                final float graphHeight = getGraphHeight();
                final float graphTop = getGraphTop();

                int drawDotSize = drawDotLists.isEmpty() ? 0 : drawDotLists.size();

                if (drawDotSize > 0) {
                    drawDotLists.ensureCapacity(dataLists.size());
                }

                for (int i = 0; i < dataLists.size(); i++) {
                    GraphTemperature entry = dataLists.get(i);
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
                visibleRect.set(mScrollViewer.getScrollX(),
                        mScrollViewer.getScrollY(),
                        mScrollViewer.getScrollX() + mScrollViewer.getWidth(),
                        mScrollViewer.getScrollY() + mScrollViewer.getHeight());
            }

            drawText(canvas);
            drawLines(canvas);
        }

        private void drawText(Canvas canvas) {
            // draw bottom text
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

                    if (drawIconsLabels && xData.getIcon() != Resources.ID_NULL) {
                        int rotation = xData.getIconRotation();
                        Drawable iconDrawable = ContextCompat.getDrawable(getContext(), xData.getIcon());

                        Rect bounds = new Rect(0, 0, (int) iconHeight, (int) iconHeight);
                        iconDrawable.setBounds(bounds);
                        drawingRect.set(x, y, x + bounds.width(), y + bounds.height());

                        if (RectF.intersects(drawingRect, visibleRect)) {
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

        private void drawLines(Canvas canvas) {
            if (!drawDotLists.isEmpty()) {
                final int hiTempColor = Colors.ORANGERED;
                final int loTempColor = Colors.LIGHTSKYBLUE;

                for (int i = 0; i < drawDotLists.size(); i++) {
                    Bar bar = drawDotLists.get(i);
                    GraphTemperature entry = dataLists.get(i);
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
                            bar.x + drwTextWidth + linePaint.getStrokeWidth() / 2f,
                            bar.loY + bottomTextHeight + bottomTextDescent);

                    if (RectF.intersects(drawingRect, visibleRect)) {
                        if (drawLine) {
                            canvas.drawLine(bar.x, bar.hiY, bar.x, bar.loY, linePaint);
                        } else {
                            canvas.drawLine(bar.x, bar.hiY - linePaint.getStrokeWidth() / 4f, bar.x, bar.hiY, linePaint);
                        }

                        if (drawDataLabels) {
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
            refreshDrawDotList();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (mOnSizeChangedListener != null)
                mOnSizeChangedListener.onSizeChanged(RangeBarGraphView.this, xCoordinateList.size() > 0 ? Iterables.getLast(xCoordinateList).intValue() : 0);
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