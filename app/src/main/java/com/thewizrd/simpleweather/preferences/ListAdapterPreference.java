package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.DialogPreference;

import com.thewizrd.shared_resources.controls.ComboBoxItem;
import com.thewizrd.simpleweather.R;

public class ListAdapterPreference extends DialogPreference {
    private ComboBoxItem mValue;
    private String mSummary;
    private boolean mValueSet;

    private final Context mContext;
    private final ArrayAdapter<ComboBoxItem> mAdapter;

    public ListAdapterPreference(Context context) {
        this(context, null);
    }

    public ListAdapterPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, androidx.preference.R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle));
    }

    public ListAdapterPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ListAdapterPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        /* Retrieve the Preference summary attribute since it's private
         * in the Preference class.
         */
        TypedArray a = context.obtainStyledAttributes(attrs,
                androidx.preference.R.styleable.Preference, defStyleAttr, defStyleRes);

        mSummary = TypedArrayUtils.getString(a, androidx.preference.R.styleable.Preference_summary,
                androidx.preference.R.styleable.Preference_android_summary);

        a.recycle();

        mContext = context;
        mAdapter = createAdapter();
    }

    /**
     * By default, this class uses a simple {@link android.widget.ArrayAdapter}. But if you need
     * a more complicated {@link android.widget.ArrayAdapter}, this method can be overridden to
     * create a custom one.
     * <p> Note: This method is called from the constructor. So, overridden methods will get called
     * before any subclass initialization.
     *
     * @return The custom {@link android.widget.ArrayAdapter} that needs to be used with this class.
     */
    protected ArrayAdapter<ComboBoxItem> createAdapter() {
        return new ArrayAdapter<>(mContext, R.layout.alertdialog_singlechoice_material);
    }

    public final ArrayAdapter<ComboBoxItem> getAdapter() {
        return mAdapter;
    }

    /**
     * Sets the value of the key. This should be one of the entries in
     * {@link #getAdapter()}.
     *
     * @param item The value to set for the key.
     */
    public void setValue(ComboBoxItem item) {
        String strValue = null;

        if (item != null)
            strValue = item.getValue();

        // Always persist/notify the first time.
        final boolean changed = !(mValue == item);
        if (changed || !mValueSet) {
            mValue = item;
            mValueSet = true;
            persistString(strValue);
            if (changed) {
                notifyChanged();
            }
        }
    }

    /**
     * Returns the summary of this ListPreference. If the summary
     * has a {@linkplain java.lang.String#format String formatting}
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place.
     *
     * @return the summary with appropriate string substitution
     */
    @Override
    public CharSequence getSummary() {
        final ComboBoxItem entry = getEntry();
        if (mSummary == null) {
            return super.getSummary();
        } else {
            return String.format(mSummary, entry == null ? "" : entry.toString());
        }
    }

    /**
     * Sets the summary for this Preference with a CharSequence.
     * If the summary has a
     * {@linkplain java.lang.String#format String formatting}
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place when it's retrieved.
     *
     * @param summary The summary for the preference.
     */
    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        if (summary == null && mSummary != null) {
            mSummary = null;
        } else if (summary != null && !summary.equals(mSummary)) {
            mSummary = summary.toString();
        }
    }

    /**
     * Sets the value to the given index from the entry values.
     *
     * @param index The index of the value to set.
     */
    public void setValueIndex(int index) {
        if (mAdapter != null) {
            setValue(mAdapter.getItem(index));
        }
    }

    /**
     * Returns the value of the key. This should be one of the entries in
     * {@link #getAdapter()}.
     *
     * @return The value of the key.
     */
    public ComboBoxItem getValue() {
        return mValue;
    }

    /**
     * Returns the entry corresponding to the current value.
     *
     * @return The entry corresponding to the current value, or null.
     */
    public ComboBoxItem getEntry() {
        int index = getValueIndex();
        return index >= 0 && mAdapter != null ? mAdapter.getItem(index) : null;
    }

    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    public int findIndexOfValue(ComboBoxItem value) {
        if (value != null && mAdapter != null) {
            return mAdapter.getPosition(value);
        }
        return -1;
    }

    private int getValueIndex() {
        return findIndexOfValue(mValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        ComboBoxItem value;

        public SavedState(Parcel source) {
            super(source);
            value = new ComboBoxItem(source.readString(), source.readString());
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(value.getDisplay());
            dest.writeString(value.getValue());
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
