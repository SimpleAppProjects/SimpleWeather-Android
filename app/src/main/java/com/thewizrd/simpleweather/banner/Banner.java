package com.thewizrd.simpleweather.banner;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

/**
 * Wrapper for the Material Banner implementation
 * Banner is managed by {@link BannerManager}
 */
public class Banner {
    private final Context mContext;
    private Drawable mBannerIcon;
    private CharSequence mMessageText;
    private CharSequence mPrimaryActionText;
    private CharSequence mSecondaryActionText;
    private View.OnClickListener mPrimaryAction;
    private View.OnClickListener mSecondaryAction;

    private Banner(@NonNull Context context) {
        mContext = context;
    }

    @NonNull
    public static Banner make(@NonNull Context context, @StringRes int resId) {
        Banner banner = new Banner(context);
        banner.mMessageText = context.getText(resId);
        return banner;
    }

    @NonNull
    public static Banner make(@NonNull Context context, CharSequence messageText) {
        Banner banner = new Banner(context);
        banner.mMessageText = messageText;
        return banner;
    }

    public void setBannerIcon(@DrawableRes int resId) {
        this.mBannerIcon = ContextCompat.getDrawable(mContext, resId);
    }

    public void setBannerIcon(@Nullable Drawable drawable) {
        this.mBannerIcon = drawable;
    }

    public void setPrimaryAction(@StringRes int resId, View.OnClickListener bannerAction) {
        this.mPrimaryActionText = mContext.getText(resId);
        this.mPrimaryAction = bannerAction;
    }

    public void setPrimaryAction(CharSequence actionText, View.OnClickListener bannerAction) {
        this.mPrimaryActionText = actionText;
        this.mPrimaryAction = bannerAction;
    }

    public void setSecondaryAction(@StringRes int resId, View.OnClickListener bannerAction) {
        this.mSecondaryActionText = mContext.getText(resId);
        this.mSecondaryAction = bannerAction;
    }

    public void setSecondaryAction(CharSequence actionText, View.OnClickListener bannerAction) {
        this.mSecondaryActionText = actionText;
        this.mSecondaryAction = bannerAction;
    }

    public Drawable getBannerIcon() {
        return mBannerIcon;
    }

    public CharSequence getMessageText() {
        return mMessageText;
    }

    public CharSequence getPrimaryActionText() {
        return mPrimaryActionText;
    }

    public View.OnClickListener getPrimaryAction() {
        return mPrimaryAction;
    }

    public CharSequence getSecondaryActionText() {
        return mSecondaryActionText;
    }

    public View.OnClickListener getSecondaryAction() {
        return mSecondaryAction;
    }
}
