package com.thewizrd.simpleweather.banner;

import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.thewizrd.simpleweather.databinding.LayoutBannerCardviewBinding;

/**
 * Manager for the banner of an activity or fragment
 * There should be only one BannerManager and one banner in the activity or fragment.
 * Based on the SnackbarManager implementation
 */
public final class BannerManager {
    private LayoutBannerCardviewBinding mBannerView;
    private Banner mCurrentBanner;
    private final ViewGroup mParentView;

    /**
     * Constructs a BannerManager to show a banner in the given window.
     *
     * @param parent The ViewGroup used to display banner.
     */
    public BannerManager(@NonNull ViewGroup parent) {
        mParentView = parent;
    }

    /**
     * Shows a banner
     *
     * @param banner The banner to show
     */
    @MainThread
    public void show(@NonNull final Banner banner) {
        // Add current banner to stack
        mCurrentBanner = banner;

        // Update Banner view
        updateView();
    }

    /**
     * Dismiss the banners.
     */
    @MainThread
    public void dismiss() {
        mCurrentBanner = null;
        updateView();
    }

    /**
     * Update the Banner view
     */
    @MainThread
    private void updateView() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            // NOT ON THE MAIN THREAD!!
            throw new IllegalStateException("Cannot update the Banner view off the main thread");
        }

        // Get current Banner
        final Banner banner = mCurrentBanner;
        if (banner == null) {
            // Dismiss view if there are no more banners
            if (mBannerView != null) {
                mParentView.removeView(mBannerView.getRoot());
                mBannerView = null;
            }
        } else {
            // Check if Banner view instance exists
            if (mBannerView == null) {
                mBannerView = LayoutBannerCardviewBinding.inflate(
                        LayoutInflater.from(mParentView.getContext()), mParentView, false
                );
            }

            // Update view
            mBannerView.bannerMessage.setText(banner.getMessageText());
            if (!TextUtils.isEmpty(banner.getPrimaryActionText())) {
                mBannerView.primaryBtn.setText(banner.getPrimaryActionText());
                mBannerView.primaryBtn.setOnClickListener(banner.getPrimaryAction());
                mBannerView.primaryBtn.setVisibility(View.VISIBLE);
            } else {
                mBannerView.primaryBtn.setText("");
                mBannerView.primaryBtn.setOnClickListener(null);
                mBannerView.primaryBtn.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(banner.getSecondaryActionText())) {
                mBannerView.secondaryBtn.setText(banner.getSecondaryActionText());
                mBannerView.secondaryBtn.setOnClickListener(banner.getSecondaryAction());
                mBannerView.secondaryBtn.setVisibility(View.VISIBLE);
            } else {
                mBannerView.secondaryBtn.setText("");
                mBannerView.secondaryBtn.setOnClickListener(null);
                mBannerView.secondaryBtn.setVisibility(View.GONE);
            }
            if (banner.getBannerIcon() != null) {
                mBannerView.bannerIcon.setImageDrawable(banner.getBannerIcon());
                mBannerView.bannerIconGroup.setVisibility(View.VISIBLE);
            } else {
                mBannerView.bannerIcon.setImageDrawable(null);
                mBannerView.bannerIconGroup.setVisibility(View.GONE);
            }

            if (mParentView.indexOfChild(mBannerView.getRoot()) == -1) {
                mParentView.addView(mBannerView.getRoot(), 0);
            }
        }
    }
}