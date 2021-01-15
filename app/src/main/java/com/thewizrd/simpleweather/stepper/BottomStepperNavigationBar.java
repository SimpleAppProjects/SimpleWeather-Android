package com.thewizrd.simpleweather.stepper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.BottomNavStepperLayoutBinding;

public class BottomStepperNavigationBar extends RelativeLayout {
    private BottomNavStepperLayoutBinding binding;

    /* NavBar View */
    private @DrawableRes
    int mBackgroundColor;
    private ColorStateList mForegroundColor;

    private String mStartButtonText;
    private String mBackButtonText;
    private String mNextButtonText;
    private String mCompleteButtonText;

    private Drawable mBackButtonIcon;
    private Drawable mNextButtonIcon;
    private Drawable mCompleteButtonIcon;

    private int mItemCount;
    private int mSelectedIdx;

    public BottomStepperNavigationBar(Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public BottomStepperNavigationBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public BottomStepperNavigationBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BottomStepperNavigationBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize(Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = BottomNavStepperLayoutBinding.inflate(inflater, this);

        initDefaultValues(context);
        initAttributeValues(attrs, defStyleAttr);

        binding.getRoot().setBackgroundResource(mBackgroundColor);
        binding.navBackbutton.setTextColor(mForegroundColor);
        binding.navNextbutton.setTextColor(mForegroundColor);

        binding.navBackbutton.setRippleColor(mForegroundColor);
        binding.navNextbutton.setRippleColor(mForegroundColor);

        binding.navBackbutton.setText(mBackButtonText);
        binding.navNextbutton.setText(mNextButtonText);

        binding.navBackbutton.setIcon(mBackButtonIcon);
        binding.navBackbutton.setIconTint(mForegroundColor);
        binding.navNextbutton.setIcon(mNextButtonIcon);
        binding.navNextbutton.setIconTint(mForegroundColor);
    }

    private void initDefaultValues(Context context) {
        mBackgroundColor = ActivityUtils.getResourceId(context, android.R.attr.colorBackground);
        mForegroundColor = ActivityUtils.getColorStateList(context, R.attr.colorAccent);

        mBackButtonText = context.getString(R.string.label_back);
        mStartButtonText = mNextButtonText = context.getString(R.string.label_next);
        mCompleteButtonText = context.getString(R.string.abc_action_mode_done);

        mBackButtonIcon = ContextCompat.getDrawable(context, R.drawable.ic_chevron_left);
        mNextButtonIcon = ContextCompat.getDrawable(context, R.drawable.ic_chevron_right);
        mCompleteButtonIcon = ContextCompat.getDrawable(context, R.drawable.ic_done_white_24dp);
    }

    private void initAttributeValues(@Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        if (attrs != null) {
            final TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.BottomStepperNavigationBar, defStyleAttr, 0);

            if (a.hasValue(R.styleable.BottomStepperNavigationBar_nav_backgroundColor)) {
                mBackgroundColor = a.getResourceId(R.styleable.BottomStepperNavigationBar_nav_backgroundColor, 0);
            }
            if (a.hasValue(R.styleable.BottomStepperNavigationBar_nav_foregroundColor)) {
                mForegroundColor = a.getColorStateList(R.styleable.BottomStepperNavigationBar_nav_foregroundColor);
            }

            if (a.hasValue(R.styleable.BottomStepperNavigationBar_nav_backButtonText)) {
                mBackButtonText = a.getString(R.styleable.BottomStepperNavigationBar_nav_backButtonText);
            }
            if (a.hasValue(R.styleable.BottomStepperNavigationBar_nav_startButtonText)) {
                mStartButtonText = a.getString(R.styleable.BottomStepperNavigationBar_nav_startButtonText);
            }
            if (a.hasValue(R.styleable.BottomStepperNavigationBar_nav_completeButtonText)) {
                mCompleteButtonText = a.getString(R.styleable.BottomStepperNavigationBar_nav_completeButtonText);
            }

            if (a.hasValue(R.styleable.BottomStepperNavigationBar_nav_backButtonIcon)) {
                mBackButtonIcon = a.getDrawable(R.styleable.BottomStepperNavigationBar_nav_backButtonIcon);
            }
            if (a.hasValue(R.styleable.BottomStepperNavigationBar_nav_nextButtonIcon)) {
                mNextButtonIcon = a.getDrawable(R.styleable.BottomStepperNavigationBar_nav_nextButtonIcon);
            }
            if (a.hasValue(R.styleable.BottomStepperNavigationBar_nav_completeButtonIcon)) {
                mCompleteButtonIcon = a.getDrawable(R.styleable.BottomStepperNavigationBar_nav_completeButtonIcon);
            }

            a.recycle();
        }
    }

    public int getCurrentPosition() {
        return mSelectedIdx;
    }

    public void setItemCount(@IntRange(from = 0) int count) {
        if (mItemCount != count) {
            mItemCount = count;
            binding.navStepslayout.removeAllViews();
            for (int i = 0; i < count; i++) {
                View dot = LayoutInflater.from(getContext()).inflate(R.layout.nav_dot, binding.navStepslayout, false);
                ViewCompat.setBackgroundTintList(dot, mForegroundColor);
                binding.navStepslayout.addView(dot);
            }
            setSelectedItem(0);
        }
    }

    public void setSelectedItem(@IntRange(from = 0) int idx) {
        mSelectedIdx = idx;

        for (int i = 0; i < mItemCount; i++) {
            View dot = binding.navStepslayout.getChildAt(i);
            if (i == mSelectedIdx) {
                dot.setAlpha(1f);
            } else {
                dot.setAlpha(0.25f);
            }
        }

        if (idx == 0) {
            binding.navBackbutton.setVisibility(View.INVISIBLE);
            binding.navNextbutton.setVisibility(View.VISIBLE);

            binding.navBackbutton.setText(mBackButtonText);
            binding.navNextbutton.setText(mStartButtonText);

            binding.navBackbutton.setIcon(mBackButtonIcon);
            binding.navNextbutton.setIcon(mNextButtonIcon);
        } else if (idx == mItemCount - 1) {
            binding.navBackbutton.setVisibility(View.VISIBLE);
            binding.navNextbutton.setVisibility(View.VISIBLE);

            binding.navBackbutton.setText(mBackButtonText);
            binding.navNextbutton.setText(mCompleteButtonText);

            binding.navBackbutton.setIcon(mBackButtonIcon);
            binding.navNextbutton.setIcon(mCompleteButtonIcon);
        } else {
            binding.navBackbutton.setVisibility(View.VISIBLE);
            binding.navNextbutton.setVisibility(View.VISIBLE);

            binding.navBackbutton.setText(mBackButtonText);
            binding.navNextbutton.setText(mNextButtonText);

            binding.navBackbutton.setIcon(mBackButtonIcon);
            binding.navNextbutton.setIcon(mNextButtonIcon);
        }
    }

    public void showBackButton(boolean show) {
        binding.navBackbutton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    public void showNextButton(boolean show) {
        binding.navNextbutton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    public void setOnBackButtonClickListener(@Nullable OnClickListener listener) {
        binding.navBackbutton.setOnClickListener(listener);
    }

    public void setOnNextButtonClickListener(@Nullable OnClickListener listener) {
        binding.navNextbutton.setOnClickListener(listener);
    }
}
