package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;

import com.thewizrd.simpleweather.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ArrayMultiSelectListPreference extends MultiSelectListPreference {
    private List<CharSequence> mEntries;
    private List<CharSequence> mEntryValues;

    private final Set<String> mValues = new LinkedHashSet<>();

    public ArrayMultiSelectListPreference(Context context) {
        super(context);
        initialize();
    }

    public ArrayMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ArrayMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public ArrayMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        mEntries = new ArrayList<>();
        mEntryValues = new ArrayList<>();

        setSummaryProvider(SimpleSummaryProvider.getInstance());
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        mEntries.clear();
        mEntries.addAll(Arrays.asList(entries));
    }

    @Override
    public CharSequence[] getEntries() {
        return mEntries.toArray(new CharSequence[0]);
    }

    @Override
    public void setEntryValues(CharSequence[] entryValues) {
        mEntryValues.clear();
        mEntryValues.addAll(Arrays.asList(entryValues));
    }

    @Override
    public int findIndexOfValue(String value) {
        if (value != null && !mEntryValues.isEmpty()) {
            int idx = mEntryValues.indexOf(value);
            if (idx >= 0) {
                return idx;
            }
        }
        return -1;
    }

    @Override
    protected boolean[] getSelectedItems() {
        final List<CharSequence> entries = mEntries;
        final int entryCount = entries.size();
        final Set<String> values = getValues();
        boolean[] result = new boolean[entryCount];

        for (int i = 0; i < entryCount; i++) {
            result[i] = values.contains(entries.get(i).toString());
        }

        return result;
    }

    @Override
    public CharSequence[] getEntryValues() {
        return mEntryValues.toArray(new CharSequence[0]);
    }

    @Override
    public void setValues(Set<String> values) {
        mValues.clear();
        mValues.addAll(values);

        persistStringSet(values);
        notifyChanged();
    }

    @Override
    public Set<String> getValues() {
        return mValues;
    }

    public int getEntryCount() {
        return mEntries.size();
    }

    public void addEntry(@StringRes int entryResId, @NonNull CharSequence entryValue) {
        mEntries.add(getContext().getResources().getText(entryResId));
        mEntryValues.add(entryValue);
    }

    public void addEntry(@NonNull CharSequence entry, @NonNull CharSequence entryValue) {
        mEntries.add(entry);
        mEntryValues.add(entryValue);
    }

    public void insertEntry(int index, @NonNull CharSequence entry, @NonNull CharSequence entryValue) {
        mEntries.add(index, entry);
        mEntryValues.add(index, entryValue);
    }

    public void addAllEntries(@NonNull Collection<CharSequence> entries,
                              @NonNull Collection<CharSequence> entryValues) {
        mEntries.addAll(entries);
        mEntryValues.addAll(entryValues);
    }

    public void insertAllEntries(int index, @NonNull Collection<CharSequence> entries,
                                 @NonNull Collection<CharSequence> entryValues) {
        mEntries.addAll(index, entries);
        mEntryValues.addAll(index, entryValues);
    }

    public void removeEntry(int index) {
        mEntries.remove(index);
        mEntryValues.remove(index);
    }

    public void removeAllEntries() {
        mEntries.clear();
        mEntryValues.clear();
    }

    public CharSequence findEntryFromValue(CharSequence entryValue) {
        if (entryValue != null) {
            int idx = mEntryValues.indexOf(entryValue);
            if (idx >= 0)
                return mEntries.get(idx);
        }

        return null;
    }

    public CharSequence findValueFromEntry(CharSequence entry) {
        if (entry != null) {
            int idx = mEntries.indexOf(entry);
            if (idx >= 0)
                return mEntryValues.get(idx);
        }

        return null;
    }

    /**
     * A simple {@link androidx.preference.Preference.SummaryProvider} implementation for a
     * {@link ListPreference}. If no value has been set, the summary displayed will be 'Not set',
     * otherwise the summary displayed will be the entry set for this preference.
     */
    public static final class SimpleSummaryProvider implements SummaryProvider<ArrayMultiSelectListPreference> {

        private static ArrayMultiSelectListPreference.SimpleSummaryProvider sSimpleSummaryProvider;

        private SimpleSummaryProvider() {
        }

        /**
         * Retrieve a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation.
         *
         * @return a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation
         */
        public static ArrayMultiSelectListPreference.SimpleSummaryProvider getInstance() {
            if (sSimpleSummaryProvider == null) {
                sSimpleSummaryProvider = new ArrayMultiSelectListPreference.SimpleSummaryProvider();
            }
            return sSimpleSummaryProvider;
        }

        @Override
        public CharSequence provideSummary(ArrayMultiSelectListPreference preference) {
            if (preference.getValues() == null || preference.getValues().isEmpty()) {
                return (preference.getContext().getString(R.string.not_set));
            } else {
                SpannableStringBuilder sb = new SpannableStringBuilder();

                for (String value : preference.getValues()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }

                    CharSequence entry = preference.findEntryFromValue(value);
                    if (!TextUtils.isEmpty(entry)) {
                        sb.append(entry);
                    }
                }

                return sb;
            }
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.mValues = getValues();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(@Nullable Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValues(myState.mValues);
    }

    private static class SavedState extends BaseSavedState {
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

        Set<String> mValues;

        SavedState(Parcel source) {
            super(source);
            final int size = source.readInt();
            mValues = new HashSet<>();
            String[] strings = new String[size];
            source.readStringArray(strings);

            Collections.addAll(mValues, strings);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mValues.size());
            dest.writeStringArray(mValues.toArray(new String[mValues.size()]));
        }
    }
}
