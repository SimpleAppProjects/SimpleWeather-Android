package com.thewizrd.common.databinding;

import androidx.annotation.NonNull;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.databinding.BindingAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.progressindicator.BaseProgressIndicator;

public class ProgressBarAdapter {
    @BindingAdapter("showProgressBarIfTrue")
    public static void showProgressBarIfTrue(@NonNull ContentLoadingProgressBar progressBar, boolean show) {
        if (show) {
            progressBar.show();
        } else {
            progressBar.hide();
        }
    }

    @BindingAdapter("showProgressBarIfTrue")
    public static void showProgressBarIfTrue(@NonNull BaseProgressIndicator<?> progressBar, boolean show) {
        if (show) {
            progressBar.show();
        } else {
            progressBar.hide();
        }
    }

    @BindingAdapter("isRefreshing")
    public static void setRefreshing(@NonNull SwipeRefreshLayout refreshLayout, boolean showRefresh) {
        refreshLayout.setRefreshing(showRefresh);
    }
}
