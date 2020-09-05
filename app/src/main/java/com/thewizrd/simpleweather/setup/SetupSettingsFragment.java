package com.thewizrd.simpleweather.setup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialSharedAxis;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSetupSettingsBinding;
import com.thewizrd.simpleweather.preferences.CustomPreferenceFragmentCompat;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

public class SetupSettingsFragment extends CustomPreferenceFragmentCompat {

    private FragmentSetupSettingsBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        View mStepperNavBar = getAppCompatActivity().findViewById(R.id.bottom_nav_bar);
        SnackbarManager mSnackMgr = new SnackbarManager(binding.getRoot());
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        mSnackMgr.setAnchorView(mStepperNavBar);
        return mSnackMgr;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupSettingsBinding.inflate(inflater, container, false);
        ViewGroup root = (ViewGroup) binding.getRoot();
        View inflatedView = super.onCreateView(inflater, container, savedInstanceState);

        binding.fragmentContainer.addView(inflatedView);

        setDivider(new ColorDrawable(ActivityUtils.getColor(root.getContext(), R.attr.colorPrimary)));
        setDividerHeight((int) ActivityUtils.dpToPx(root.getContext(), 1f));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_setup, null);

        final ListPreference notIconPref = findPreference("key_notificationicon");
        final SwitchPreferenceCompat onGoingPref = findPreference("key_ongoingnotification");

        onGoingPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean value = (boolean) newValue;
                notIconPref.setVisible(value);

                if (value && Settings.useFollowGPS() && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && !Settings.requestedBGAccess() &&
                        ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requireView().post(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar snackbar = Snackbar.make(R.string.bg_location_permission_rationale, Snackbar.Duration.LONG);
                            snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                            0);
                                }
                            });
                            showSnackbar(snackbar, null);
                            Settings.setRequestBGAccess(true);
                        }
                    });
                }
                return true;
            }
        });

        notIconPref.setVisible(onGoingPref.isChecked());
    }
}
