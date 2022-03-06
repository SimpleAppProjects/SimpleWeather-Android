package com.thewizrd.simpleweather.preferences.colorpreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;

import com.thewizrd.simpleweather.R;

import java.util.Locale;

public class ColorPreference extends DialogPreference {
    private final int mWidgetLayoutResId = R.layout.layout_colorbox;

    @ColorInt
    private int mColorValue = Color.TRANSPARENT;

    @ColorInt
    public int getColor() {
        return mColorValue;
    }

    public void setColor(@ColorInt int value) {
        mColorValue = value;

        persistInt(value);

        notifyChanged();
    }

    public ColorPreference(Context context) {
        this(context, null);
    }

    @SuppressLint("RestrictedApi")
    public ColorPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setWidgetLayoutResource(mWidgetLayoutResId);
        setSummaryProvider(SimpleSummaryProvider.getInstance());
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        if (a.peekValue(index) != null) {
            int type = a.peekValue(index).type;
            if (type == TypedValue.TYPE_STRING) {
                return Color.parseColor(a.getString(index));
            } else if (TypedValue.TYPE_FIRST_COLOR_INT <= type && type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return a.getColor(index, Color.TRANSPARENT);
            } else if (TypedValue.TYPE_FIRST_INT <= type && type <= TypedValue.TYPE_LAST_INT) {
                return a.getInt(index, Color.TRANSPARENT);
            }
        }
        return Color.TRANSPARENT;
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        if (defaultValue instanceof Integer) {
            setColor(getPersistedInt((int) defaultValue));
        } else {
            setColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View view = holder.findViewById(R.id.color_box);
        view.setBackgroundColor(getColor());
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final ColorPreference.SavedState myState = new ColorPreference.SavedState(superState);
        myState.mColor = getColor();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(ColorPreference.SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        ColorPreference.SavedState myState = (ColorPreference.SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setColor(myState.mColor);
    }

    private static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<ColorPreference.SavedState> CREATOR =
                new Parcelable.Creator<ColorPreference.SavedState>() {
                    @Override
                    public ColorPreference.SavedState createFromParcel(Parcel in) {
                        return new ColorPreference.SavedState(in);
                    }

                    @Override
                    public ColorPreference.SavedState[] newArray(int size) {
                        return new ColorPreference.SavedState[size];
                    }
                };

        @ColorInt
        int mColor;

        SavedState(Parcel source) {
            super(source);
            mColor = source.readInt();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mColor);
        }
    }

    /**
     * A simple {@link androidx.preference.Preference.SummaryProvider} implementation for an
     * {@link ColorPreference}.
     */
    public static final class SimpleSummaryProvider implements SummaryProvider<ColorPreference> {

        private static ColorPreference.SimpleSummaryProvider sSimpleSummaryProvider;

        private SimpleSummaryProvider() {
        }

        /**
         * Retrieve a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation.
         *
         * @return a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation
         */
        public static ColorPreference.SimpleSummaryProvider getInstance() {
            if (sSimpleSummaryProvider == null) {
                sSimpleSummaryProvider = new ColorPreference.SimpleSummaryProvider();
            }
            return sSimpleSummaryProvider;
        }

        @Override
        public CharSequence provideSummary(ColorPreference preference) {
            return String.format(Locale.ROOT, "#%08X", preference.getColor());
        }
    }
}
