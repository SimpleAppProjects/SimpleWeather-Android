package com.thewizrd.simpleweather.controls.graphs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewGroupCompat;

import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel;

public class ForecastGraphPanel extends LinearLayout {
    private LineView lineView;

    private ForecastGraphViewModel graphData;

    private Configuration currentConfig;

    // Event listeners
    private RecyclerOnClickListenerInterface onClickListener;

    public void setOnClickPositionListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public ForecastGraphPanel(Context context) {
        super(context);
        initialize(context);
    }

    public ForecastGraphPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ForecastGraphPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ForecastGraphPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentConfig = new Configuration(newConfig);

        updateViewColors();

        lineView.postInvalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize(Context context) {
        this.currentConfig = new Configuration(context.getResources().getConfiguration());
        setOrientation(LinearLayout.VERTICAL);
        lineView = new LineView(context);

        int lineViewHeight = context.getResources().getDimensionPixelSize(R.dimen.forecast_panel_height);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, lineViewHeight);
        View.OnTouchListener onTouchListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (onClickListener != null && v instanceof IGraph) {
                    onClickListener.onClick(v, ((IGraph) v).getItemPositionFromPoint(event.getX()));
                }
                return true;
            }
            return false;
        };
        lineView.setLayoutParams(layoutParams);
        lineView.setOnTouchListener(onTouchListener);

        this.removeAllViews();
        this.addView(lineView);
        // Individual transitions on the view can cause
        // OpenGLRenderer: GL error:  GL_INVALID_VALUE
        ViewGroupCompat.setTransitionGroup(this, true);

        resetView();
    }

    private void updateViewColors() {
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;
        final int bottomTextColor = isNightMode ? Colors.WHITE : Colors.BLACK;

        lineView.setBackgroundLineColor(ColorUtils.setAlphaComponent(isNightMode ? Colors.WHITE : Colors.GRAY, 0x99));
        lineView.setBottomTextColor(bottomTextColor);
    }

    private void resetView() {
        updateViewColors();

        lineView.resetData(false);

        updateGraph();

        lineView.smoothScrollTo(0, 0);
    }

    private void updateGraph() {
        if (graphData != null) {
            lineView.setDrawGridLines(false);
            lineView.setDrawDotLine(false);
            lineView.setDrawDataLabels(true);
            lineView.setDrawIconLabels(true);
            lineView.setDrawGraphBackground(true);
            lineView.setDrawDotPoints(false);

            lineView.getDataLabels().addAll(graphData.getLabelData());
            lineView.getDataLists().addAll(graphData.getSeriesData());
        }
    }

    public void updateForecasts(@NonNull final ForecastGraphViewModel graphData) {
        if (this.graphData != graphData) {
            this.graphData = graphData;
            resetView();
        }
    }
}