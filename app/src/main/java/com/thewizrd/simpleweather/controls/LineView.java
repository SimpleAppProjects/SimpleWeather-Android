package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Pair;
import com.thewizrd.simpleweather.helpers.ActivityUtils;

import java.util.ArrayList;
import java.util.Collections;

public class LineView extends HorizontalScrollView {

    private LineViewGraph graph;
    private OnClickListener onClickListener;

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
        this.setLayerType(LAYER_TYPE_SOFTWARE, null);
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);

        this.removeAllViews();
        this.addView(graph, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
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

    public void setDataLabels(ArrayList<Pair<String, Integer>> iconLabels, ArrayList<Pair<String, String>> dataLabels) {
        this.graph.setDataLabels(iconLabels, dataLabels);
    }

    public void setDataLabels(ArrayList<Pair<String, String>> dataLabels) {
        this.graph.setDataLabels(dataLabels);
    }

    public void setDataList(ArrayList<ArrayList<Float>> dataLists) {
        this.graph.setDataList(dataLists);
    }

    /*
     *  Single series line graph
     *  Based on LineView from http://www.androidtrainee.com/draw-android-line-chart-with-animation/
     *  Graph background (under line) based on - https://github.com/jjoe64/GraphView (LineGraphSeries)
     */
    class LineViewGraph extends View {
        private int mViewHeight;
        //drawBackground
        private int dataOfAGird = 10;
        private float bottomTextHeight = 0;
        private ArrayList<Pair<String, String>> dataLabels; // X, Y labels
        private ArrayList<Pair<String, Integer>> iconLabels; // X-axis icon labels

        private ArrayList<ArrayList<Float>> dataLists; // Y data

        private ArrayList<Float> xCoordinateList;
        private ArrayList<Float> yCoordinateList;

        private ArrayList<ArrayList<Dot>> drawDotLists;

        private Paint bottomTextPaint;
        private int bottomTextDescent;

        private final float iconBottomMargin = ActivityUtils.dpToPx(getContext(), 2);
        private final float bottomTextTopMargin = ActivityUtils.dpToPx(getContext(), 6);
        private final float bottomLineLength = ActivityUtils.dpToPx(getContext(), 22);
        private final float DOT_INNER_CIR_RADIUS = ActivityUtils.dpToPx(getContext(), 2);
        private final float DOT_OUTER_CIR_RADIUS = ActivityUtils.dpToPx(getContext(), 5);
        private final float MIN_TOP_LINE_LENGTH = ActivityUtils.dpToPx(getContext(), 12);
        private final int MIN_VERTICAL_GRID_NUM = 4;
        private final int MIN_HORIZONTAL_GRID_NUM = 1;
        private final int BACKGROUND_LINE_COLOR = Colors.WHITESMOKE;
        private final int BOTTOM_TEXT_COLOR = Colors.WHITE;

        private float topLineLength = MIN_TOP_LINE_LENGTH;
        private float sideLineLength = ActivityUtils.dpToPx(getContext(), 45) / 3 * 2;
        private float backgroundGridWidth = ActivityUtils.dpToPx(getContext(), 45);

        private int[] colorArray = {Colors.SIMPLEBLUE, Colors.RED, Colors.LIGHTSEAGREEN};

        private boolean drawGridLines = false;
        private boolean drawDotLine = false;
        private boolean drawDotPoints = false;
        private boolean drawGraphBackground = false;
        private boolean drawDataLabels = false;
        private boolean drawIconsLabels = false;

        private Runnable animator = new Runnable() {
            @Override
            public void run() {
                boolean needNewFrame = false;
                for (ArrayList<Dot> data : drawDotLists) {
                    for (Dot dot : data) {
                        dot.update();
                        if (!dot.isAtRest()) {
                            needNewFrame = true;
                        }
                    }
                }
                if (needNewFrame) {
                    postDelayed(this, 25);
                }
                invalidate();
            }
        };

        LineViewGraph(Context context) {
            this(context, null);
        }

        LineViewGraph(Context context, AttributeSet attrs) {
            super(context, attrs);
            bottomTextPaint = new Paint();
            dataLabels = new ArrayList<>();
            iconLabels = new ArrayList<>();
            xCoordinateList = new ArrayList<>();
            yCoordinateList = new ArrayList<>();
            drawDotLists = new ArrayList<>();

            bottomTextPaint.setAntiAlias(true);
            bottomTextPaint.setTextSize(ActivityUtils.dpToPx(getContext(), 12));
            bottomTextPaint.setTextAlign(Paint.Align.CENTER);
            bottomTextPaint.setStyle(Paint.Style.FILL);
            bottomTextPaint.setColor(BOTTOM_TEXT_COLOR);
        }

        boolean isDrawIcons() {
            return drawIconsLabels && iconLabels != null && iconLabels.size() > 0;
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

        void setDataLabels(ArrayList<Pair<String, String>> dataLabels) {
            this.setDataLabels(null, dataLabels);
        }

        void setDataLabels(ArrayList<Pair<String, Integer>> iconLabels, ArrayList<Pair<String, String>> dataLabels) {
            this.iconLabels = iconLabels;
            this.dataLabels = dataLabels;

            if (!drawIconsLabels && iconLabels != null && iconLabels.size() > 0)
                drawIconsLabels = true;
            else if (drawIconsLabels && (iconLabels == null || iconLabels.size() <= 0))
                drawIconsLabels = false;

            Rect r = new Rect();
            Rect r2 = new Rect();
            int longestWidth = 0;
            String longestStr = "";
            bottomTextDescent = 0;
            for (Pair<String, String> p : dataLabels) {
                String s = p.getKey();
                String s2 = p.getValue();
                bottomTextPaint.getTextBounds(s, 0, s.length(), r);
                bottomTextPaint.getTextBounds(s2, 0, s2.length(), r2);
                if (bottomTextHeight < r.height()) {
                    bottomTextHeight = r.height();
                }
                if (bottomTextHeight < r2.height()) {
                    bottomTextHeight = r2.height();
                }
                if (longestWidth < r.width()) {
                    longestWidth = r.width();
                    longestStr = s;
                }
                if (longestWidth < r2.width()) {
                    longestWidth = r2.width();
                    longestStr = s2;
                }
                if (bottomTextDescent < (Math.abs(r.bottom))) {
                    bottomTextDescent = Math.abs(r.bottom);
                }
                if (bottomTextDescent < (Math.abs(r2.bottom))) {
                    bottomTextDescent = Math.abs(r2.bottom);
                }
            }

            if (backgroundGridWidth < longestWidth) {
                backgroundGridWidth = longestWidth + (int) bottomTextPaint.measureText(longestStr, 0, 1);
            }
            if (sideLineLength < longestWidth / 2f) {
                sideLineLength = longestWidth / 2f;
            }

            refreshXCoordinateList(getHorizontalGridNum());
        }

        void setDataList(ArrayList<ArrayList<Float>> dataLists) {
            this.dataLists = dataLists;
            for (ArrayList<Float> list : dataLists) {
                if (list.size() > dataLabels.size()) {
                    throw new RuntimeException("dacer.LineView error:" +
                            " dataList.size() > bottomTextList.size() !!!");
                }
            }
            float biggestData = 0;
            for (ArrayList<Float> list : dataLists) {
                for (Float i : list) {
                    if (biggestData < i) {
                        biggestData = i;
                    }
                }
                dataOfAGird = 1;
                while (biggestData / 10 > dataOfAGird) {
                    dataOfAGird *= 10;
                }
            }

            refreshAfterDataChanged();
            setMinimumWidth(0); // It can help the LineView reset the Width,
            // I don't know the better way..
            postInvalidate();
        }

        private void refreshAfterDataChanged() {
            float verticalGridNum = getVerticalGridlNum();
            refreshTopLineLength(verticalGridNum);
            refreshYCoordinateList(verticalGridNum);
            refreshDrawDotList(verticalGridNum);
        }

        private float getVerticalGridlNum() {
            float verticalGridNum = MIN_VERTICAL_GRID_NUM;
            if (dataLists != null && !dataLists.isEmpty()) {
                for (ArrayList<Float> list : dataLists) {
                    for (Float number : list) {
                        if (verticalGridNum < (number + 1)) {
                            verticalGridNum = number + 1;
                        }
                    }
                }
            }
            return verticalGridNum;
        }

        private int getHorizontalGridNum() {
            int horizontalGridNum = dataLabels.size() - 1;
            if (horizontalGridNum < MIN_HORIZONTAL_GRID_NUM) {
                horizontalGridNum = MIN_HORIZONTAL_GRID_NUM;
            }
            return horizontalGridNum;
        }

        private void refreshXCoordinateList(float horizontalGridNum) {
            xCoordinateList.clear();
            for (int i = 0; i < (horizontalGridNum + 1); i++) {
                xCoordinateList.add(sideLineLength + backgroundGridWidth * i);
            }
        }

        private void refreshYCoordinateList(float verticalGridNum) {
            yCoordinateList.clear();
            for (int i = 0; i < (verticalGridNum + 1); i++) {
                yCoordinateList.add(topLineLength + ((getGraphHeight()) * i / (verticalGridNum)));
            }
        }

        private void refreshDrawDotList(float verticalGridNum) {
            if (dataLists != null && !dataLists.isEmpty()) {
                if (drawDotLists.size() == 0) {
                    for (int k = 0; k < dataLists.size(); k++) {
                        drawDotLists.add(new ArrayList<LineViewGraph.Dot>());
                    }
                }
                float maxValue = 0;
                float minValue = 0;
                for (int k = 0; k < dataLists.size(); k++) {
                    float kMax = Collections.max(dataLists.get(k));
                    float kMin = Collections.min(dataLists.get(k));

                    if (maxValue < kMax)
                        maxValue = kMax;
                    if (minValue > kMin)
                        minValue = kMin;
                }
                for (int k = 0; k < dataLists.size(); k++) {
                    int drawDotSize = drawDotLists.get(k).isEmpty() ? 0 : drawDotLists.get(k).size();

                    for (int i = 0; i < dataLists.get(k).size(); i++) {
                        float x = xCoordinateList.get(i);
                        // Make space for y data labels
                        float y;
                        if (maxValue == minValue) {
                            y = topLineLength + (getGraphHeight()) / 2f;
                        } else {
                            y = topLineLength + (getGraphHeight()) * (maxValue - dataLists.get(k).get(i)) / (maxValue - minValue);
                        }
                        // int y = yCoordinateList.get(verticalGridNum - dataLists.get(k).get(i)) + bottomTextHeight + bottomTextTopMargin + bottomTextDescent;

                        if (i > drawDotSize - 1) {
                            drawDotLists.get(k).add(new Dot(x, 0, x, y, dataLists.get(k).get(i), k));
                        } else {
                            drawDotLists.get(k).set(i, drawDotLists.get(k).get(i).setTargetData(x, y, dataLists.get(k).get(i), k));
                        }
                    }

                    int temp = drawDotLists.get(k).size() - dataLists.get(k).size();
                    for (int i = 0; i < temp; i++) {
                        drawDotLists.get(k).remove(drawDotLists.get(k).size() - 1);
                    }
                }
            }
            removeCallbacks(animator);
            post(animator);
        }

        private void refreshTopLineLength(float verticalGridNum) {
            // For prevent popup can't be completely showed when backgroundGridHeight is too small.
            // But this code not so good.
            float labelsize = bottomTextHeight * 2 + bottomTextTopMargin;

            if (drawDataLabels && (getGraphHeight()) /
                    (verticalGridNum + 2) < labelsize) {
                topLineLength = labelsize + 2;
            } else {
                topLineLength = MIN_TOP_LINE_LENGTH;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            drawBackgroundLines(canvas);
            drawLines(canvas);
            drawDots(canvas);
        }

        private void drawDots(Canvas canvas) {
            if (drawDotPoints) {
                Paint bigCirPaint = new Paint();
                bigCirPaint.setAntiAlias(true);
                Paint smallCirPaint = new Paint(bigCirPaint);
                smallCirPaint.setColor(Color.parseColor("#FFFFFF"));
                if (drawDotLists != null && !drawDotLists.isEmpty()) {
                    for (int k = 0; k < drawDotLists.size(); k++) {
                        bigCirPaint.setColor(colorArray[k % 3]);
                        for (Dot dot : drawDotLists.get(k)) {
                            canvas.drawCircle(dot.x, dot.y, DOT_OUTER_CIR_RADIUS, bigCirPaint);
                            canvas.drawCircle(dot.x, dot.y, DOT_INNER_CIR_RADIUS, smallCirPaint);
                        }
                    }
                }
            }
        }

        private void drawLines(Canvas canvas) {
            Paint linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(ActivityUtils.dpToPx(getContext(), 2));

            Path mPathBackground = new Path();
            Paint mPaintBackground = new Paint();
            float graphHeight = getGraphHeight();
            float graphWidth = this.getWidth();
            float graphLeft = this.getLeft();
            float graphTop = this.getTop() + topLineLength;

            for (int k = 0; k < drawDotLists.size(); k++) {
                float firstX = -1;
                float firstY = -1;
                // needed to end the path for background
                float lastUsedEndY = 0;

                linePaint.setColor(Colors.WHITE);
                mPaintBackground.setColor(ColorUtils.setAlphaComponent(colorArray[k % 3], 0x50));
                for (int i = 0; i < drawDotLists.get(k).size() - 1; i++) {
                    Dot dot = drawDotLists.get(k).get(i);
                    Dot nextDot = drawDotLists.get(k).get(i + 1);

                    float startX = dot.x;
                    float startY = dot.y;
                    float endX = nextDot.x;
                    float endY = nextDot.y;

                    if (firstX == -1) {
                        canvas.drawLine(0, dot.y, dot.x, dot.y, linePaint);
                    }

                    canvas.drawLine(dot.x, dot.y, nextDot.x, nextDot.y, linePaint);

                    // Draw top label
                    if (k == 0 && drawDataLabels)
                        canvas.drawText(dataLabels.get(i).getValue(), sideLineLength + backgroundGridWidth * i, dot.y - bottomTextHeight, bottomTextPaint);

                    if (firstX == -1) {
                        firstX = 0;
                        firstY = startY;
                        if (k == 0 && drawGraphBackground)
                            mPathBackground.moveTo(0, startY);
                    }

                    if (k == 0 && drawGraphBackground) {
                        mPathBackground.lineTo(startX, startY);
                        mPathBackground.lineTo(endX, endY);
                    }

                    // Draw last items
                    if (i + 1 == drawDotLists.get(k).size() - 1) {
                        if (k == 0 && drawDataLabels)
                            canvas.drawText(dataLabels.get(i + 1).getValue(), sideLineLength + backgroundGridWidth * (i + 1), nextDot.y - bottomTextHeight, bottomTextPaint);
                        canvas.drawLine(nextDot.x, nextDot.y, graphWidth, nextDot.y, linePaint);

                        if (k == 0 && drawGraphBackground) {
                            mPathBackground.lineTo(endX, endY);
                            mPathBackground.lineTo(graphWidth, endY);
                        }
                    }

                    lastUsedEndY = endY;
                }

                if (k == 0 && drawGraphBackground && firstX != -1) {
                    // end / close path
                    if (lastUsedEndY != graphHeight + graphTop) {
                        // dont draw line to same point, otherwise the path is completely broken
                        mPathBackground.lineTo(graphWidth, graphHeight + graphTop);
                    }
                    mPathBackground.lineTo(firstX, graphHeight + graphTop);
                    if (firstY != graphHeight + graphTop) {
                        // dont draw line to same point, otherwise the path is completely broken
                        mPathBackground.lineTo(firstX, firstY);
                    }
                    //mPathBackground.close();
                    canvas.drawPath(mPathBackground, mPaintBackground);
                }
            }
        }

        private void drawBackgroundLines(Canvas canvas) {
            if (drawGridLines) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(ActivityUtils.dpToPx(getContext(), 1f));
                paint.setColor(BACKGROUND_LINE_COLOR);
                PathEffect effects = new DashPathEffect(
                        new float[]{10, 5, 10, 5}, 1);

                // draw vertical lines
                for (int i = 0; i < xCoordinateList.size(); i++) {
                    canvas.drawLine(xCoordinateList.get(i),
                            0,
                            xCoordinateList.get(i),
                            getGraphHeight() + topLineLength,
                            paint);
                }

                if (!drawDotLine) {
                    // draw solid lines
                    for (int i = 0; i < yCoordinateList.size(); i++) {
                        if ((yCoordinateList.size() - 1 - i) % dataOfAGird == 0) {
                            canvas.drawLine(0, yCoordinateList.get(i), getWidth(), yCoordinateList.get(i), paint);
                        }
                    }
                } else {
                    // draw dotted lines
                    paint.setPathEffect(effects);
                    Path dottedPath = new Path();
                    for (int i = 0; i < yCoordinateList.size(); i++) {
                        if ((yCoordinateList.size() - 1 - i) % dataOfAGird == 0) {
                            dottedPath.moveTo(0, yCoordinateList.get(i));
                            dottedPath.lineTo(getWidth(), yCoordinateList.get(i));
                            canvas.drawPath(dottedPath, paint);
                        }
                    }
                }
            }

            //draw bottom text
            if (dataLabels != null) {
                boolean drawIcons = false;

                TextPaint iconPaint = new TextPaint();
                if (iconLabels != null && iconLabels.size() > 0) {
                    iconPaint.setAntiAlias(true);
                    iconPaint.setTextSize(ActivityUtils.dpToPx(getContext(), 24));
                    iconPaint.setTextAlign(Paint.Align.CENTER);
                    iconPaint.setStyle(Paint.Style.FILL);
                    iconPaint.setColor(BOTTOM_TEXT_COLOR);
                    Typeface weathericons = ResourcesCompat.getFont(getContext(), com.thewizrd.shared_resources.R.font.weathericons);
                    iconPaint.setSubpixelText(true);
                    iconPaint.setTypeface(weathericons);
                    drawIcons = true;
                }

                for (int i = 0; i < dataLabels.size(); i++) {
                    float x = sideLineLength + backgroundGridWidth * i;
                    float y = mViewHeight - bottomTextDescent;

                    canvas.drawText(dataLabels.get(i).getKey(), x, y, bottomTextPaint);
                    if (drawIcons) {
                        int rotation = iconLabels.get(iconLabels.size() == 1 ? 0 : i).getValue();
                        String icon = iconLabels.get(iconLabels.size() == 1 ? 0 : i).getKey();
                        Rect r = new Rect();
                        iconPaint.getTextBounds(icon, 0, icon.length(), r);

                        StaticLayout mTextLayout = new StaticLayout(
                                icon, iconPaint, r.width(), Layout.Alignment.ALIGN_NORMAL, 0.0f, 0.0f, false);

                        canvas.save();
                        canvas.translate(x, y - mTextLayout.getHeight() - bottomTextHeight - iconBottomMargin * 2f);
                        canvas.rotate(rotation, 0, mTextLayout.getHeight() / 2f);
                        mTextLayout.draw(canvas);
                        canvas.restore();
                    }
                }
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int mViewWidth = measureWidth(widthMeasureSpec);
            mViewHeight = measureHeight(heightMeasureSpec);
            refreshAfterDataChanged();
            setMeasuredDimension(mViewWidth, mViewHeight);
        }

        private int measureWidth(int measureSpec) {
            int horizontalGridNum = getHorizontalGridNum();
            int preferred = (int) (backgroundGridWidth * horizontalGridNum + sideLineLength * 2);
            return getMeasurement(measureSpec, preferred);
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
                default:
                    measurement = preferred;
                    break;
            }
            return measurement;
        }

        class Dot {
            float x;
            float y;
            float data;
            float targetX;
            float targetY;
            int linenumber;
            float velocity = (int) ActivityUtils.dpToPx(getContext(), 18);

            Dot(float x, float y, float targetX, float targetY, float data, int linenumber) {
                this.x = x;
                this.y = y;
                this.linenumber = linenumber;
                setTargetData(targetX, targetY, data, linenumber);
            }

            Dot setTargetData(float targetX, float targetY, float data, int linenumber) {
                this.targetX = targetX;
                this.targetY = targetY;
                this.data = data;
                this.linenumber = linenumber;
                return this;
            }

            boolean isAtRest() {
                return (x == targetX) && (y == targetY);
            }

            void update() {
                x = updateSelf(x, targetX, velocity);
                y = updateSelf(y, targetY, velocity);
            }

            private float updateSelf(float origin, float target, float velocity) {
                if (origin < target) {
                    origin += velocity;
                } else if (origin > target) {
                    origin -= velocity;
                }
                if (Math.abs(target - origin) < velocity) {
                    origin = target;
                }
                return origin;
            }
        }
    }
}