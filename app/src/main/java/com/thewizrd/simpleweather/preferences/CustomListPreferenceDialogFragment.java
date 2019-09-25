package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.helpers.WindowColorManager;

public class CustomListPreferenceDialogFragment extends PreferenceDialogFragmentCompat
        implements WindowColorManager {

    private static final String SAVED_BACK_STACK_ID = "CustomListPreferenceDialogFragment.backStackId";
    private static final String SAVE_STATE_INDEX = "CustomListPreferenceDialogFragment.index";
    private static final String SAVE_STATE_ENTRIES = "CustomListPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES =
            "CustomListPreferenceDialogFragment.entryValues";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
            int mClickedDialogEntryIndex;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    private int mBackStackId = -1;

    public static CustomListPreferenceDialogFragment newInstance(String key) {
        final CustomListPreferenceDialogFragment fragment =
                new CustomListPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            final ListPreference preference = getListPreference();

            if (preference.getEntries() == null || preference.getEntryValues() == null) {
                throw new IllegalStateException(
                        "ListPreference requires an entries array and an entryValues array.");
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
            mEntries = preference.getEntries();
            mEntryValues = preference.getEntryValues();
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0);
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
            mBackStackId = savedInstanceState.getInt(SAVED_BACK_STACK_ID, -1);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
        outState.putInt(SAVED_BACK_STACK_ID, mBackStackId);
    }

    private ListPreference getListPreference() {
        return (ListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mClickedDialogEntryIndex = which;

                        // Clicking on an item simulates the positive button click, and dismisses
                        // the dialog.
                        CustomListPreferenceDialogFragment.this.onClick(dialog,
                                DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                });

        // The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
        // dialog instead of the user having to press 'Ok'.
        builder.setPositiveButton(null, null);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            String value = mEntryValues[mClickedDialogEntryIndex].toString();
            final ListPreference preference = getListPreference();
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }
    }

    /* Full-screen additions */
    // Views
    private AppBarLayout mAppBarLayout;
    private CoordinatorLayout mRootView;
    private Toolbar mToolbar;

    /**
     * Display the full-screen fragment, adding the fragment to the given FragmentManager.  This
     * is a convenience for explicitly creating a transaction, adding the
     * fragment to it with the given tag, and {@link FragmentTransaction#commit() committing} it.
     *
     * @param manager The FragmentManager this fragment will be added to.
     * @param tag     The tag for this fragment, as per
     *                {@link FragmentTransaction#add(Fragment, String) FragmentTransaction.add}.
     */
    public int showFullScreen(@NonNull FragmentManager manager, @IdRes int containerViewId, @Nullable String tag) {
        // The device is smaller, so show the fragment fullscreen
        FragmentTransaction transaction = manager.beginTransaction();
        // For a little polish, specify a transition animation
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        // To make it fullscreen, use the 'content' root view as the container
        // for the fragment, which is always the root view for the activity
        transaction.add(containerViewId, this, tag);
        mBackStackId = transaction.addToBackStack(null).commit();
        return mBackStackId;
    }

    /**
     * The system calls this to get the DialogFragment's layout, regardless
     * of whether it's being displayed as a dialog or an embedded fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_toolbar_layout, container, false);

        mRootView = (CoordinatorLayout) root;
        mAppBarLayout = root.findViewById(R.id.app_bar);
        mToolbar = root.findViewById(R.id.toolbar);
        mToolbar.setTitle(getPreference().getTitle());
        mToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Clicking on an item simulates the positive button
                 * click, and dismisses the dialog.
                 */
                onDialogClosed(false);
                dismissFragment();
            }
        });

        if (container != null && container.getId() == android.R.id.content) {
            mRootView.setFitsSystemWindows(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(mRootView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        ListView listView = new ListView(inflater.getContext());
        listView.setAdapter(new CheckedItemAdapter(inflater.getContext(), R.layout.fullscreendialog_singlechoice_material,
                android.R.id.text1, mEntries));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mClickedDialogEntryIndex = position;
                parent.setSelection(mClickedDialogEntryIndex);

                /*
                 * Clicking on an item simulates the positive button
                 * click, and dismisses the dialog.
                 */
                onDialogClosed(true);
                dismissFragment();
            }
        });
        listView.setDivider(null);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        if (mClickedDialogEntryIndex > -1) {
            listView.setItemChecked(mClickedDialogEntryIndex, true);
            listView.setSelection(mClickedDialogEntryIndex);
        }

        CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        root.addView(listView, lp);

        return root;
    }

    public void dismissFragment() {
        if (mBackStackId >= 0) {
            requireFragmentManager().popBackStack(mBackStackId,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mBackStackId = -1;
        } else {
            FragmentTransaction ft = requireFragmentManager().beginTransaction();
            ft.remove(this);
            ft.commitAllowingStateLoss();
        }
    }

    private static class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
        public CheckedItemAdapter(Context context, int resource, int textViewResourceId,
                                  CharSequence[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    /* WindowColorFragment additions */
    private Configuration prevConfig;

    @Override
    public void onResume() {
        super.onResume();
        prevConfig = new Configuration(getResources().getConfiguration());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateWindowColors();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int diff = newConfig.diff(prevConfig);
        prevConfig = new Configuration(newConfig);

        if ((diff & ActivityInfo.CONFIG_UI_MODE) != 0) {
            if (!this.isHidden() && this.isVisible()) {
                updateWindowColors();
            }
        }
    }

    @Override
    public void updateWindowColors() {
        if (getContext() == null) return;

        Configuration config = prevConfig != null ? prevConfig : getContext().getResources().getConfiguration();
        final int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        @ColorInt int color = ActivityUtils.getColor(getContext(), R.attr.colorPrimary);
        @ColorInt int bg_color = ActivityUtils.getColor(getContext(), android.R.attr.colorBackground);
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                bg_color = Colors.BLACK;
            } else {
                bg_color = ActivityUtils.getColor(getContext(), android.R.attr.colorBackground);
            }
            color = bg_color;
        }
        mRootView.setBackgroundColor(bg_color);
        mAppBarLayout.setBackgroundColor(color);
        mRootView.setStatusBarBackgroundColor(color);
    }
}
