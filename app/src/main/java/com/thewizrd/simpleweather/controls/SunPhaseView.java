package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ContextUtils;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.simpleweather.R;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;

public class SunPhaseView extends View {
    private Configuration currentConfig;

    private int mViewHeight;
    private int mViewWidth;
    private float bottomTextHeight = 0;

    private final Drawable iconDrawable;
    private final Paint backgroundPaint;
    private final Paint fullArcPaint;
    private final Paint pathArcPaint;
    private final Paint bottomTextPaint;
    private final Paint bigCirPaint;
    private int bottomTextDescent;

    private LocalDateTime sunrise;
    private LocalDateTime sunset;
    private ZoneOffset offset = ZoneOffset.UTC;

    private float sunriseX;
    private float sunsetX;

    private final float bottomTextTopMargin = ContextUtils.dpToPx(getContext(), 8);
    private final float DOT_RADIUS = ContextUtils.dpToPx(getContext(), 4);

    private float sideLineLength = ContextUtils.dpToPx(getContext(), 45) / 3 * 2;
    private float backgroundGridWidth = ContextUtils.dpToPx(getContext(), 45);

    private int BOTTOM_TEXT_COLOR = Colors.WHITE;
    private int PHASE_ARC_COLOR = Colors.WHITE;
    private int PAINT_COLOR = Colors.YELLOW;

    public SunPhaseView(Context context) {
        this(context, null);
    }

    public SunPhaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.currentConfig = new Configuration(context.getResources().getConfiguration());

        final LocalDate date = LocalDate.now();
        sunrise = date.atTime(LocalTime.MIDNIGHT);
        sunset = date.atTime(LocalTime.MIDNIGHT);

        iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_full_sun).mutate();

        bottomTextPaint = new Paint();
        bottomTextPaint.setAntiAlias(true);
        bottomTextPaint.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.forecast_condition_size));
        bottomTextPaint.setTextAlign(Paint.Align.CENTER);
        bottomTextPaint.setStyle(Paint.Style.FILL);
        bottomTextPaint.setColor(BOTTOM_TEXT_COLOR);

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(ColorUtils.setAlphaComponent(PAINT_COLOR, 0x50));

        fullArcPaint = new Paint();
        fullArcPaint.setStyle(Paint.Style.STROKE);
        fullArcPaint.setAntiAlias(true);
        fullArcPaint.setColor(ColorUtils.setAlphaComponent(PHASE_ARC_COLOR, 0x40));
        fullArcPaint.setStrokeWidth(ContextUtils.dpToPx(context, 1));

        final float intervalOn = ContextUtils.dpToPx(context, 8);
        final float intervalOff = ContextUtils.dpToPx(context, 10);

        PathEffect dashEffect = new DashPathEffect(
                new float[]{intervalOn, intervalOff, intervalOn, intervalOff}, 1);
        fullArcPaint.setPathEffect(dashEffect);

        pathArcPaint = new Paint(fullArcPaint);
        pathArcPaint.setStrokeWidth(ContextUtils.dpToPx(context, 2f));
        pathArcPaint.setPathEffect(null);
        pathArcPaint.setColor(PAINT_COLOR);

        bigCirPaint = new Paint();
        bigCirPaint.setStyle(Paint.Style.FILL);
        bigCirPaint.setAntiAlias(true);
        bigCirPaint.setColor(PAINT_COLOR);

        updateColors();
    }

    private float getGraphHeight() {
        return mViewHeight - bottomTextTopMargin - bottomTextHeight - bottomTextDescent;
    }

    private String getSunriseLabel() {
        if (DateFormat.is24HourFormat(getContext())) {
            return sunrise.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR));
        } else {
            return sunrise.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM));
        }
    }

    private String getSunsetLabel() {
        if (DateFormat.is24HourFormat(getContext())) {
            return sunset.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR));
        } else {
            return sunset.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM));
        }
    }

    private void setTextColor(@ColorInt int color) {
        if (BOTTOM_TEXT_COLOR != color) {
            BOTTOM_TEXT_COLOR = color;
            if (bottomTextPaint != null) {
                bottomTextPaint.setColor(BOTTOM_TEXT_COLOR);
                invalidate();
            }
        }
    }

    private void setPhaseArcColor(@ColorInt int color) {
        if (PHASE_ARC_COLOR != color) {
            PHASE_ARC_COLOR = color;
            if (fullArcPaint != null) {
                fullArcPaint.setColor(ColorUtils.setAlphaComponent(color, 0x40));
            }
            invalidate();
        }
    }

    private void setPaintColor(@ColorInt int color) {
        if (PAINT_COLOR != color) {
            PAINT_COLOR = color;
            if (backgroundPaint != null) {
                backgroundPaint.setColor(ColorUtils.setAlphaComponent(color, 0x50));
            }
            if (pathArcPaint != null) {
                pathArcPaint.setColor(color);
            }
            if (bigCirPaint != null) {
                bigCirPaint.setColor(color);
            }
            invalidate();
        }
    }

    public void setSunriseSetTimes(LocalTime sunrise, LocalTime sunset) {
        setSunriseSetTimes(sunrise, sunset, ZoneOffset.UTC);
    }

    public void setSunriseSetTimes(LocalTime sunrise, LocalTime sunset, ZoneOffset offset) {
        LocalDate date = LocalDate.now(offset);
        setSunriseSetTimes(sunrise.atDate(date), sunset.atDate(date), offset);
    }

    public void setSunriseSetTimes(LocalDateTime sunrise, LocalDateTime sunset) {
        setSunriseSetTimes(sunrise, sunset, ZoneOffset.UTC);
    }

    public void setSunriseSetTimes(LocalDateTime sunrise, LocalDateTime sunset, ZoneOffset offset) {
        if (!ObjectsCompat.equals(this.sunrise, sunrise) || !ObjectsCompat.equals(this.sunset, sunset) || !ObjectsCompat.equals(this.offset, offset)) {
            this.sunrise = sunrise;
            this.sunset = sunset;
            this.offset = offset;

            Rect r = new Rect();
            int longestWidth = 0;
            String longestStr = "";
            bottomTextDescent = 0;
            for (String s : Arrays.asList(getSunriseLabel(), getSunriseLabel())) {
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
            }

            if (backgroundGridWidth < longestWidth) {
                backgroundGridWidth = longestWidth + (int) bottomTextPaint.measureText(longestStr, 0, 1);
            }
            if (sideLineLength < longestWidth / 2f) {
                sideLineLength = longestWidth / 2f;
            }

            refreshXCoordinateList();
            invalidate();
        }
    }

    private void refreshXCoordinateList() {
        sunriseX = sideLineLength;
        sunsetX = mViewWidth - sideLineLength;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawLabels(canvas);
        drawArc(canvas);
        drawDots(canvas);
    }

    private void drawDots(Canvas canvas) {
        canvas.drawCircle(sunriseX, getGraphHeight(), DOT_RADIUS, bigCirPaint);
        canvas.drawCircle(sunsetX, getGraphHeight(), DOT_RADIUS, bigCirPaint);
    }

    private void drawArc(Canvas canvas) {
        float radius = getGraphHeight() * 0.9f;
        float trueRadius = (sunsetX - sunriseX) * 0.5f;

        float x, y;
        float centerX = mViewWidth * 0.5f;
        float centerY = getGraphHeight();

        /*
            Point on circle (width = height = r)
            x(t) = r cos(t) + j
            y(t) = r sin(t) + k

            Point on ellipse
            int ePX = X + (int) (width  * Math.cos(Math.toRadians(t)));
            int ePY = Y + (int) (height * Math.sin(Math.toRadians(t)));
         */

        boolean isDay = false;
        int angle = 0;

        {
            LocalDateTime now = LocalDateTime.now(offset);

            if (now.isBefore(sunrise)) {
                angle = 0;
                isDay = false;
            } else if (now.isAfter(sunset)) {
                angle = 180;
                isDay = false;
            } else if (now.isAfter(sunrise)) {
                long sunUpDuration = (sunset.toEpochSecond(offset) - sunrise.toEpochSecond(offset)) / 60;
                long minAfterSunrise = (now.toEpochSecond(offset) / 60) - (sunrise.toEpochSecond(offset) / 60);

                angle = (int) (((float) minAfterSunrise / sunUpDuration) * 180);
                isDay = true;
            }
        }

        x = (float) (trueRadius * Math.cos(Math.toRadians(angle - 180))) + centerX;
        y = (float) (radius * Math.sin(Math.toRadians(angle - 180))) + centerY;

        Path mPathBackground = new Path();
        float firstX = -1;
        float firstY = -1;
        // needed to end the path for background
        float lastUsedEndX = 0;
        float lastUsedEndY = 0;

        for (int i = 0; i < angle; i++) {
            float startX = (float) (trueRadius * Math.cos(Math.toRadians(i - 180))) + centerX;
            float startY = (float) (radius * Math.sin(Math.toRadians(i - 180))) + centerY;
            float endX = (float) (trueRadius * Math.cos(Math.toRadians((i + 1) - 180))) + centerX;
            float endY = (float) (radius * Math.sin(Math.toRadians((i + 1) - 180))) + centerY;

            if (firstX == -1) {
                firstX = sunriseX;
                firstY = getGraphHeight();
                mPathBackground.moveTo(firstX, firstY);
            }

            mPathBackground.lineTo(startX, startY);
            mPathBackground.lineTo(endX, endY);

            lastUsedEndX = endX;
            lastUsedEndY = endY;
        }

        final RectF oval = new RectF(sunriseX, getGraphHeight() - radius, sunsetX, getGraphHeight() + radius);
        canvas.drawArc(oval, -180, 180, true, fullArcPaint);

        if (firstX != -1) {
            // end / close path
            if (lastUsedEndY != getGraphHeight()) {
                // dont draw line to same point, otherwise the path is completely broken
                mPathBackground.lineTo(lastUsedEndX, getGraphHeight());
            }
            mPathBackground.lineTo(firstX, getGraphHeight());
            if (firstY != getGraphHeight()) {
                // dont draw line to same point, otherwise the path is completely broken
                mPathBackground.lineTo(firstX, firstY);
            }

            canvas.drawPath(mPathBackground, backgroundPaint);
            canvas.drawArc(oval, -180, angle, false, pathArcPaint);
        }

        if (isDay) {
            final float iconSize = ContextUtils.dpToPx(getContext(), 28); // old: 24dp
            DrawableCompat.setTint(iconDrawable, PAINT_COLOR);
            iconDrawable.setBounds(0, 0, (int) iconSize, (int) iconSize);

            canvas.save();
            canvas.translate(x - iconSize / 2f, y - iconSize / 2f);
            iconDrawable.draw(canvas);
            canvas.restore();
        }
    }

    private void drawLabels(Canvas canvas) {
        // Draw bottom text
        float y = mViewHeight - bottomTextDescent;
        canvas.drawText(getSunriseLabel(), sunriseX, y, bottomTextPaint);
        canvas.drawText(getSunsetLabel(), sunsetX, y, bottomTextPaint);
    }

    private void updateColors() {
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;

        setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
        setPhaseArcColor(isNightMode ? Colors.WHITE : Colors.BLACK);
        setPaintColor(isNightMode ? Colors.YELLOW : Colors.ORANGE);
    }

    private void refreshAfterDataChanged() {
        refreshXCoordinateList();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mViewWidth = measureWidth(widthMeasureSpec);
        mViewHeight = measureHeight(heightMeasureSpec);
        refreshAfterDataChanged();
        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    private int measureWidth(int measureSpec) {
        int MIN_HORIZONTAL_GRID_NUM = 2;
        int preferred = (int) (backgroundGridWidth * MIN_HORIZONTAL_GRID_NUM + sideLineLength * 2);
        return getMeasurement(measureSpec, preferred);
    }

    private int measureHeight(int measureSpec) {
        int preferred = 0;
        return getMeasurement(measureSpec, preferred);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentConfig = new Configuration(newConfig);
        updateColors();

        invalidate();
    }

    private int getMeasurement(int measureSpec, int preferred) {
        int specSize = View.MeasureSpec.getSize(measureSpec);
        int measurement;
        switch (View.MeasureSpec.getMode(measureSpec)) {
            case View.MeasureSpec.EXACTLY:
                measurement = specSize;
                break;
            case View.MeasureSpec.AT_MOST:
                measurement = Math.min(preferred, specSize);
                break;
            default:
                measurement = preferred;
                break;
        }
        return measurement;
    }
}
