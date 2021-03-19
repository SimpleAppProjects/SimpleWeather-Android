package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.thewizrd.shared_resources.R;

/**
 * TextView which allows setting the size of the compound drawables
 * <p>
 * Based on: https://stackoverflow.com/a/31916731
 */
public class TextViewDrawableCompat extends AppCompatTextView {
    private int mDrawableWidth;
    private int mDrawableHeight;

    public TextViewDrawableCompat(@NonNull Context context) {
        super(context);
        init(context, null, android.R.attr.textViewStyle);
    }

    public TextViewDrawableCompat(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, android.R.attr.textViewStyle);
    }

    public TextViewDrawableCompat(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TextViewDrawableCompat, defStyleAttr, 0);

        try {
            mDrawableWidth = array.getDimensionPixelSize(R.styleable.TextViewDrawableCompat_drawableWidth, -1);
            mDrawableHeight = array.getDimensionPixelSize(R.styleable.TextViewDrawableCompat_drawableHeight, -1);
        } finally {
            array.recycle();
        }

        if (mDrawableWidth > 0 || mDrawableHeight > 0) {
            initCompoundDrawableSize();
        }
    }

    private void initCompoundDrawableSize() {
        Drawable[] drawables = getCompoundDrawablesRelative();
        for (Drawable drawable : drawables) {
            if (drawable == null) {
                continue;
            }

            Rect realBounds = new Rect(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            float actualDrawableWidth = realBounds.width();
            float actualDrawableHeight = realBounds.height();
            float actualDrawableRatio = actualDrawableHeight / actualDrawableWidth;

            float scale = 1f;
            // check if both width and height defined then adjust drawable size according to the ratio
            if (mDrawableHeight > 0 && mDrawableWidth > 0) {
                float placeholderRatio = mDrawableHeight / (float) mDrawableWidth;
                if (placeholderRatio > actualDrawableRatio) {
                    scale = mDrawableWidth / actualDrawableWidth;
                } else {
                    scale = mDrawableHeight / actualDrawableHeight;
                }
            } else if (mDrawableHeight > 0) { // only height defined
                scale = mDrawableHeight / actualDrawableHeight;
            } else if (mDrawableWidth > 0) { // only width defined
                scale = mDrawableWidth / actualDrawableWidth;
            }

            actualDrawableWidth = actualDrawableWidth * scale;
            actualDrawableHeight = actualDrawableHeight * scale;

            realBounds.right = realBounds.left + Math.round(actualDrawableWidth);
            realBounds.bottom = realBounds.top + Math.round(actualDrawableHeight);

            drawable.setBounds(realBounds);
        }
        setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3]);
    }
}