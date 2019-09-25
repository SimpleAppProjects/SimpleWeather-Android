package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.preference.ListPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArrayListPreference extends ListPreference {
    private List<CharSequence> mEntries;
    private List<CharSequence> mEntryValues;

    public ArrayListPreference(Context context) {
        super(context);
        initialize();
    }

    public ArrayListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ArrayListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public ArrayListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        mEntries = new ArrayList<>();
        mEntryValues = new ArrayList<>();
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
    public CharSequence getEntry() {
        int index = findIndexOfValue(getValue());
        return index >= 0 && !mEntries.isEmpty() ? mEntries.get(index) : null;
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
    public void setValueIndex(int index) {
        if (!mEntryValues.isEmpty()) {
            setValue(mEntryValues.get(index).toString());
        }
    }

    @Override
    public CharSequence[] getEntryValues() {
        return mEntryValues.toArray(new CharSequence[0]);
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
}
