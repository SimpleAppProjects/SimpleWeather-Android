package com.thewizrd.simpleweather.preferences;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.thewizrd.shared_resources.controls.ComboBoxItem;

public class ListAdapterPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_INDEX = "ListAdapterPreferenceDialogFragment.index";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
            int mClickedDialogEntryIndex;
    private ListAdapter mAdapter;

    /**
     * Which button was clicked.
     */
    private int mWhichButtonClicked;

    public int getClickedDialogEntryIndex() {
        return mClickedDialogEntryIndex;
    }

    public static ListAdapterPreferenceDialogFragment newInstance(String key) {
        final ListAdapterPreferenceDialogFragment fragment =
                new ListAdapterPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ListAdapterPreference preference = getListAdapterPreference();

        if (preference.getAdapter() == null) {
            throw new IllegalStateException(
                    "ListAdapterPreference requires a valid adapter.");
        }

        if (savedInstanceState == null) {
            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0);
        }

        mAdapter = preference.getAdapter();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex);
    }

    private ListAdapterPreference getListAdapterPreference() {
        return (ListAdapterPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setSingleChoiceItems(mAdapter, mClickedDialogEntryIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mClickedDialogEntryIndex = which;

                        /*
                         * Clicking on an item simulates the positive button
                         * click, and dismisses the dialog.
                         */
                        ListAdapterPreferenceDialogFragment.this.onClick(dialog,
                                DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                });

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.setPositiveButton(null, null);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        final ListAdapterPreference preference = getListAdapterPreference();
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            Object value = mAdapter.getItem(mClickedDialogEntryIndex);
            if (preference.callChangeListener(value)) {
                preference.setValue(value instanceof ComboBoxItem ? (ComboBoxItem) value : null);
            }
        }
    }
}
