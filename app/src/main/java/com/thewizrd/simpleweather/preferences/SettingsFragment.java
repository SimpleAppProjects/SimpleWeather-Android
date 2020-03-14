package com.thewizrd.simpleweather.preferences;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.controls.ProviderEntry;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.notifications.WeatherNotificationService;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.thewizrd.shared_resources.utils.Settings.KEY_API;
import static com.thewizrd.shared_resources.utils.Settings.KEY_APIKEY;
import static com.thewizrd.shared_resources.utils.Settings.KEY_FOLLOWGPS;
import static com.thewizrd.shared_resources.utils.Settings.KEY_NOTIFICATIONICON;
import static com.thewizrd.shared_resources.utils.Settings.KEY_ONGOINGNOTIFICATION;
import static com.thewizrd.shared_resources.utils.Settings.KEY_REFRESHINTERVAL;
import static com.thewizrd.shared_resources.utils.Settings.KEY_USEALERTS;
import static com.thewizrd.shared_resources.utils.Settings.KEY_USECELSIUS;
import static com.thewizrd.shared_resources.utils.Settings.KEY_USEPERSONALKEY;
import static com.thewizrd.shared_resources.utils.Settings.KEY_USERTHEME;

public class SettingsFragment extends CustomPreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener, UserThemeMode.OnThemeChangeListener {

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    // Preference Keys
    private static final String KEY_ABOUTAPP = "key_aboutapp";
    private static final String KEY_APIREGISTER = "key_apiregister";
    private static final String CATEGORY_NOTIFICATION = "category_notification";
    private static final String CATEGORY_API = "category_api";

    // Preferences
    private SwitchPreferenceCompat unitPref;
    private SwitchPreferenceCompat followGps;
    private ListPreference providerPref;
    private SwitchPreferenceCompat personalKeyPref;
    private EditTextPreference keyEntry;
    private SwitchPreferenceCompat onGoingNotification;
    private ListPreference notificationIcon;
    private SwitchPreferenceCompat alertNotification;
    private Preference registerPref;
    private ListPreference themePref;

    private PreferenceCategory notCategory;
    private PreferenceCategory apiCategory;

    // Intent queue
    private HashSet<Intent.FilterComparison> intentQueue;

    private List<UserThemeMode.OnThemeChangeListener> mThemeChangeListeners;

    @Override
    protected int getTitle() {
        return R.string.title_activity_settings;
    }

    /**
     * Registers a listener.
     */
    private void registerOnThemeChangeListener(UserThemeMode.OnThemeChangeListener listener) {
        synchronized (this) {
            if (mThemeChangeListeners == null) {
                mThemeChangeListeners = new ArrayList<>();
            }

            if (!mThemeChangeListeners.contains(listener)) {
                mThemeChangeListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a listener.
     */
    private void unregisterOnThemeChangeListener(UserThemeMode.OnThemeChangeListener listener) {
        synchronized (this) {
            if (mThemeChangeListeners != null) {
                mThemeChangeListeners.remove(listener);
            }
        }
    }

    private void dispatchThemeChanged(UserThemeMode mode) {
        List<UserThemeMode.OnThemeChangeListener> list;

        synchronized (this) {
            if (mThemeChangeListeners == null) return;
            list = new ArrayList<>(mThemeChangeListeners);
        }

        final int N = list.size();
        for (int i = 0; i < N; i++) {
            list.get(i).onThemeChanged(mode);
        }
    }

    @Override
    public void onThemeChanged(UserThemeMode mode) {
        updateWindowColors(mode);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register listener
        ApplicationLib app = App.getInstance();
        app.getPreferences().unregisterOnSharedPreferenceChangeListener(app.getSharedPreferenceListener());
        app.getPreferences().registerOnSharedPreferenceChangeListener(this);
        registerOnThemeChangeListener(this);
        registerOnThemeChangeListener((UserThemeMode.OnThemeChangeListener) getAppCompatActivity());

        // Initialize queue
        intentQueue = new HashSet<>();
    }

    @Override
    public void onPause() {
        if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && WeatherManager.isKeyRequired(providerPref.getValue())) {
            // Fallback to supported weather provider
            WeatherManager wm = WeatherManager.getInstance();
            providerPref.setValue(WeatherAPI.HERE);
            providerPref.callChangeListener(WeatherAPI.HERE);
            Settings.setAPI(WeatherAPI.HERE);
            wm.updateAPI();

            if (wm.isKeyRequired() && StringUtils.isNullOrWhitespace(wm.getAPIKey())) {
                // If (internal) key doesn't exist, fallback to Yahoo
                providerPref.setValue(WeatherAPI.YAHOO);
                providerPref.callChangeListener(WeatherAPI.YAHOO);
                Settings.setAPI(WeatherAPI.YAHOO);
                wm.updateAPI();
                Settings.setPersonalKey(true);
                Settings.setKeyVerified(false);
            } else {
                // If key exists, go ahead
                Settings.setPersonalKey(false);
                Settings.setKeyVerified(true);
            }
        }

        // Unregister listener
        ApplicationLib app = App.getInstance();
        app.getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        app.getPreferences().registerOnSharedPreferenceChangeListener(app.getSharedPreferenceListener());
        unregisterOnThemeChangeListener((UserThemeMode.OnThemeChangeListener) getAppCompatActivity());
        unregisterOnThemeChangeListener(this);

        for (Intent.FilterComparison filter : intentQueue) {
            if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(filter.getIntent().getAction())) {
                WeatherManager.getInstance().updateAPI();
                // Log event
                if (!BuildConfig.DEBUG) {
                    FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(getAppCompatActivity());
                    Bundle bundle = new Bundle();
                    bundle.putString("API", Settings.getAPI());
                    bundle.putString("API_IsInternalKey", Boolean.toString(!Settings.usePersonalKey()));
                    mFirebaseAnalytics.logEvent("Update_API", bundle);
                }
            } else if (WeatherWidgetService.class.getName().equals(filter.getIntent().getComponent().getClassName())) {
                WeatherWidgetService.enqueueWork(getAppCompatActivity(), filter.getIntent());
            } else if (WeatherUpdaterWorker.class.getName().equals(filter.getIntent().getComponent().getClassName())) {
                WeatherUpdaterWorker.enqueueAction(getAppCompatActivity(), filter.getIntent().getAction());
            } else if (WearableDataListenerService.class.getName().equals(filter.getIntent().getComponent().getClassName())) {
                WearableDataListenerService.enqueueWork(getAppCompatActivity(), filter.getIntent());
            } else {
                getAppCompatActivity().startService(filter.getIntent());
            }
        }

        super.onPause();
    }

    @Override
    public boolean onBackPressed() {
        if (getAppCompatActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof SettingsFragment) {
            SettingsFragment fragment = (SettingsFragment) getAppCompatActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            ListPreference keyPref = (ListPreference) fragment.findPreference(KEY_API);
            if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && WeatherManager.isKeyRequired(keyPref.getValue())) {
                // Set keyentrypref color to red
                Snackbar.make(getRootView(), R.string.message_enter_apikey, Snackbar.LENGTH_LONG).show();
                return true;
            }
        }

        return false;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        updateWindowColors();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_general, null);

        notCategory = (PreferenceCategory) findPreference(CATEGORY_NOTIFICATION);
        apiCategory = (PreferenceCategory) findPreference(CATEGORY_API);

        findPreference(KEY_ABOUTAPP).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Display the fragment as the main content.
                getAppCompatActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AboutAppFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
        });

        unitPref = (SwitchPreferenceCompat) findPreference(KEY_USECELSIUS);
        unitPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    preference.setIcon(R.drawable.ic_celsius);
                } else {
                    preference.setIcon(R.drawable.ic_fahrenheit);
                }
                tintIcons(unitPref, Colors.SIMPLEBLUELIGHT);
                return true;
            }
        });

        if (Settings.isFahrenheit()) {
            unitPref.setIcon(R.drawable.ic_fahrenheit);
        } else {
            unitPref.setIcon(R.drawable.ic_celsius);
        }

        followGps = (SwitchPreferenceCompat) findPreference(KEY_FOLLOWGPS);
        followGps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SwitchPreferenceCompat pref = (SwitchPreferenceCompat) preference;
                if ((boolean) newValue) {
                    if (ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                        } else {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                        }
                        return false;
                    } else {
                        LocationManager locMan = (LocationManager) getAppCompatActivity().getSystemService(Context.LOCATION_SERVICE);
                        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                            Snackbar.make(getRootView(), R.string.error_enable_location_services, Snackbar.LENGTH_SHORT).show();

                            Settings.setFollowGPS(false);
                            return false;
                        }
                    }
                }

                return true;
            }
        });

        themePref = (ListPreference) findPreference(KEY_USERTHEME);
        themePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                UserThemeMode mode;
                switch (newValue.toString()) {
                    case "0": // System
                    default:
                        mode = UserThemeMode.FOLLOW_SYSTEM;
                        if (Build.VERSION.SDK_INT >= 29)
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        else
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                        break;
                    case "1": // Dark
                        mode = UserThemeMode.DARK;
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    case "2": // Dark (AMOLED / Black)
                        mode = UserThemeMode.AMOLED_DARK;
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                }
                dispatchThemeChanged(mode);
                return true;
            }
        });

        keyEntry = (EditTextPreference) findPreference(KEY_APIKEY);
        personalKeyPref = (SwitchPreferenceCompat) findPreference(KEY_USEPERSONALKEY);
        personalKeyPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SwitchPreferenceCompat pref = (SwitchPreferenceCompat) preference;
                if ((boolean) newValue) {
                    if (apiCategory.findPreference(KEY_APIKEY) == null)
                        apiCategory.addPreference(keyEntry);
                    if (apiCategory.findPreference(KEY_APIREGISTER) == null)
                        apiCategory.addPreference(registerPref);
                    keyEntry.setEnabled(true);
                } else {
                    WeatherProviderImpl selectedWProv = WeatherManager.getProvider(providerPref.getValue());

                    if (!selectedWProv.isKeyRequired() || !StringUtils.isNullOrWhitespace(selectedWProv.getAPIKey())) {
                        // We're using our own (verified) keys
                        Settings.setKeyVerified(true);
                        Settings.setAPI(providerPref.getValue());
                    }

                    keyEntry.setEnabled(false);
                    apiCategory.removePreference(keyEntry);
                    apiCategory.removePreference(registerPref);
                }

                return true;
            }
        });

        final List<ProviderEntry> providers = WeatherAPI.APIs;
        providerPref = (ListPreference) findPreference(KEY_API);

        String[] entries = new String[providers.size()];
        String[] entryValues = new String[providers.size()];

        for (int i = 0; i < providers.size(); i++) {
            entries[i] = providers.get(i).getDisplay();
            entryValues[i] = providers.get(i).getValue();
        }

        providerPref.setEntries(entries);
        providerPref.setEntryValues(entryValues);
        providerPref.setPersistent(false);
        providerPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference pref = (ListPreference) preference;
                WeatherProviderImpl selectedWProv = WeatherManager.getProvider(newValue.toString());

                if (selectedWProv.isKeyRequired()) {
                    if (StringUtils.isNullOrWhitespace(selectedWProv.getAPIKey())) {
                        Settings.setPersonalKey(true);
                        personalKeyPref.setChecked(true);
                        personalKeyPref.setEnabled(false);
                        keyEntry.setEnabled(false);
                        apiCategory.removePreference(keyEntry);
                        apiCategory.removePreference(registerPref);
                    } else {
                        personalKeyPref.setEnabled(true);
                    }

                    if (!Settings.usePersonalKey()) {
                        // We're using our own (verified) keys
                        Settings.setKeyVerified(true);
                        keyEntry.setEnabled(false);
                        apiCategory.removePreference(keyEntry);
                        apiCategory.removePreference(registerPref);
                    } else {
                        // User is using personal (unverified) keys
                        Settings.setKeyVerified(false);
                        // Clear API KEY entry to avoid issues
                        Settings.setAPIKEY("");

                        keyEntry.setEnabled(true);

                        if (apiCategory.findPreference(KEY_APIKEY) == null)
                            apiCategory.addPreference(keyEntry);
                        if (apiCategory.findPreference(KEY_APIREGISTER) == null)
                            apiCategory.addPreference(registerPref);
                    }

                    if (apiCategory.findPreference(KEY_USEPERSONALKEY) == null)
                        apiCategory.addPreference(personalKeyPref);

                    // Reset to old value if not verified
                    if (!Settings.isKeyVerified())
                        Settings.setAPI(pref.getValue());
                    else
                        Settings.setAPI(newValue.toString());

                    ProviderEntry providerEntry = null;
                    for (ProviderEntry entry : providers) {
                        if (entry.getValue().equals(newValue.toString())) {
                            providerEntry = entry;
                            break;
                        }
                    }
                    updateKeySummary(providerEntry.getDisplay());
                    updateRegisterLink(providerEntry.getValue());
                } else {
                    Settings.setKeyVerified(false);
                    keyEntry.setEnabled(false);
                    personalKeyPref.setEnabled(false);

                    Settings.setAPI(newValue.toString());
                    // Clear API KEY entry to avoid issues
                    Settings.setAPIKEY("");

                    apiCategory.removePreference(personalKeyPref);
                    apiCategory.removePreference(keyEntry);
                    apiCategory.removePreference(registerPref);
                    updateKeySummary();
                    updateRegisterLink();
                }

                updateAlertPreference(WeatherManager.getInstance().supportsAlerts());

                return true;
            }
        });

        registerPref = findPreference(KEY_APIREGISTER);

        // Set key as verified if API Key is req for API and its set
        if (WeatherManager.getInstance().isKeyRequired()) {
            keyEntry.setEnabled(true);

            if (!StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && !Settings.isKeyVerified())
                Settings.setKeyVerified(true);

            if (StringUtils.isNullOrWhitespace(WeatherManager.getInstance().getAPIKey())) {
                Settings.setPersonalKey(true);
                personalKeyPref.setChecked(true);
                personalKeyPref.setEnabled(false);
                keyEntry.setEnabled(false);
                apiCategory.removePreference(keyEntry);
                apiCategory.removePreference(registerPref);
            } else {
                personalKeyPref.setEnabled(true);
            }

            if (!Settings.usePersonalKey()) {
                // We're using our own (verified) keys
                Settings.setKeyVerified(true);
                keyEntry.setEnabled(false);
                apiCategory.removePreference(keyEntry);
                apiCategory.removePreference(registerPref);
            } else {
                // User is using personal (unverified) keys
                //Settings.setKeyVerified(false);
                // Clear API KEY entry to avoid issues
                //Settings.setAPIKEY("");

                keyEntry.setEnabled(true);

                if (apiCategory.findPreference(KEY_APIKEY) == null)
                    apiCategory.addPreference(keyEntry);
                if (apiCategory.findPreference(KEY_APIREGISTER) == null)
                    apiCategory.addPreference(registerPref);
            }
        } else {
            keyEntry.setEnabled(false);
            personalKeyPref.setEnabled(false);
            apiCategory.removePreference(personalKeyPref);
            apiCategory.removePreference(keyEntry);
            apiCategory.removePreference(registerPref);
            Settings.setKeyVerified(false);
            // Clear API KEY entry to avoid issues
            Settings.setAPIKEY("");
        }

        updateKeySummary();
        updateRegisterLink();

        onGoingNotification = (SwitchPreferenceCompat) findPreference(KEY_ONGOINGNOTIFICATION);
        onGoingNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SwitchPreferenceCompat pref = (SwitchPreferenceCompat) preference;
                Context context = App.getInstance().getAppContext();

                // On-going notification
                if ((boolean) newValue) {
                    WeatherNotificationService.enqueueWork(context, new Intent(context, WeatherNotificationService.class)
                            .setAction(WeatherNotificationService.ACTION_REFRESHNOTIFICATION));

                    if (notCategory.findPreference(KEY_NOTIFICATIONICON) == null)
                        notCategory.addPreference(notificationIcon);
                } else {
                    WeatherNotificationService.enqueueWork(context, new Intent(context, WeatherNotificationService.class)
                            .setAction(WeatherNotificationService.ACTION_REMOVENOTIFICATION));

                    notCategory.removePreference(notificationIcon);
                }

                return true;
            }
        });

        notificationIcon = (ListPreference) findPreference(KEY_NOTIFICATIONICON);
        notificationIcon.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Context context = App.getInstance().getAppContext();
                WeatherNotificationService.enqueueWork(context, new Intent(context, WeatherNotificationService.class)
                        .setAction(WeatherNotificationService.ACTION_REFRESHNOTIFICATION));
                return true;
            }
        });

        // Remove preferences
        if (!onGoingNotification.isChecked()) {
            notCategory.removePreference(notificationIcon);
        }

        alertNotification = (SwitchPreferenceCompat) findPreference(KEY_USEALERTS);
        alertNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SwitchPreferenceCompat pref = (SwitchPreferenceCompat) preference;
                Context context = App.getInstance().getAppContext();

                // Alert notification
                if ((boolean) newValue) {
                    enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                            .setAction(WeatherUpdaterWorker.ACTION_UPDATEALARM));
                } else {
                    enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                            .setAction(WeatherUpdaterWorker.ACTION_CANCELALARM));
                }
                return true;
            }
        });
        updateAlertPreference(WeatherManager.getInstance().supportsAlerts());

        tintIcons(getPreferenceScreen(), Colors.SIMPLEBLUELIGHT);
    }

    private static void tintIcons(Preference preference, @ColorInt int color) {
        if (preference instanceof PreferenceGroup) {
            PreferenceGroup group = ((PreferenceGroup) preference);
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                tintIcons(group.getPreference(i), color);
            }
        } else {
            Drawable icon = preference.getIcon();
            if (icon != null) {
                icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        final String TAG = "KeyEntryPreferenceDialogFragment";

        if (getAppCompatActivity().getSupportFragmentManager().findFragmentByTag(TAG) != null) {
            return;
        }

        if (preference instanceof EditTextPreference && KEY_APIKEY.equals(preference.getKey())) {
            final KeyEntryPreferenceDialogFragment fragment = KeyEntryPreferenceDialogFragment.newInstance(providerPref.getValue(), preference.getKey());
            fragment.setPositiveButtonOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String key = fragment.getKey();

                    String API = providerPref.getValue();
                    try {
                        if (WeatherManager.isKeyValid(key, API)) {
                            Settings.setAPIKEY(key);
                            Settings.setAPI(API);

                            Settings.setKeyVerified(true);
                            updateKeySummary();
                            updateAlertPreference(WeatherManager.getInstance().supportsAlerts());

                            fragment.getDialog().dismiss();
                        }
                    } catch (WeatherException e) {
                        Logger.writeLine(Log.ERROR, e);
                        Toast.makeText(getAppCompatActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            fragment.setTargetFragment(this, 0);
            fragment.show(getAppCompatActivity().getSupportFragmentManager(), TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void updateKeySummary() {
        updateKeySummary(providerPref.getEntry());
    }

    private void updateKeySummary(CharSequence providerAPI) {
        if (!StringUtils.isNullOrWhitespace(Settings.getAPIKEY())) {
            boolean keyVerified = Settings.isKeyVerified();

            ForegroundColorSpan colorSpan = new ForegroundColorSpan(keyVerified ?
                    Color.GREEN : Color.RED);
            Spannable summary = new SpannableString(keyVerified ?
                    getString(R.string.message_keyverified) : getString(R.string.message_keyinvalid));
            summary.setSpan(colorSpan, 0, summary.length(), 0);
            keyEntry.setSummary(summary);
        } else {
            keyEntry.setSummary(getString(R.string.pref_summary_apikey, providerAPI));
        }
    }

    private void updateRegisterLink() {
        updateRegisterLink(providerPref.getValue());
    }

    private void updateRegisterLink(CharSequence providerAPI) {
        ProviderEntry prov = null;
        for (ProviderEntry provider : WeatherAPI.APIs) {
            if (provider.getValue().equals(providerAPI.toString())) {
                prov = provider;
                break;
            }
        }

        registerPref.setIntent(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse(prov.getApiRegisterURL())));
    }

    private void updateAlertPreference(boolean enable) {
        alertNotification.setEnabled(enable);
        alertNotification.setSummary(enable ?
                getString(R.string.pref_summary_alerts) : getString(R.string.pref_summary_alerts_disabled));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    followGps.setChecked(true);
                    Settings.setFollowGPS(true);
                    // Reset home location data
                    //Settings.SaveLastGPSLocData(new WeatherData.LocationData());
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    followGps.setChecked(false);
                    Settings.setFollowGPS(false);
                    Snackbar.make(getRootView(), R.string.error_location_denied, Snackbar.LENGTH_SHORT).show();
                }
                return;
            default:
                break;
        }
    }

    private boolean enqueueIntent(Intent intent) {
        if (intent == null)
            return false;
        else {
            if (WeatherUpdaterWorker.ACTION_UPDATEALARM.equals(intent.getAction())) {
                for (Intent.FilterComparison filter : intentQueue) {
                    if (WeatherUpdaterWorker.ACTION_CANCELALARM.equals(filter.getIntent().getAction())) {
                        intentQueue.remove(filter);
                        break;
                    }
                }
            } else if (WeatherUpdaterWorker.ACTION_CANCELALARM.equals(intent.getAction())) {
                for (Intent.FilterComparison filter : intentQueue) {
                    if (WeatherUpdaterWorker.ACTION_UPDATEALARM.equals(filter.getIntent().getAction())) {
                        intentQueue.remove(filter);
                        break;
                    }
                }
            }

            return intentQueue.add(new Intent.FilterComparison(intent));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (StringUtils.isNullOrWhitespace(key))
            return;

        Context context = getAppCompatActivity();

        switch (key) {
            // Weather Provider changed
            case KEY_API:
                enqueueIntent(new Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI));
                enqueueIntent(new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER));
                break;
            // FollowGPS changed
            case KEY_FOLLOWGPS:
                boolean value = sharedPreferences.getBoolean(key, false);
                enqueueIntent(new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                enqueueIntent(new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER));
                enqueueIntent(new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));
                enqueueIntent(new Intent(context, WeatherWidgetService.class)
                        .setAction(value ? WeatherWidgetService.ACTION_REFRESHGPSWIDGETS : WeatherWidgetService.ACTION_RESETGPSWIDGETS));
                break;
            // Settings unit changed
            case KEY_USECELSIUS:
                enqueueIntent(new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER));
                break;
            // Refresh interval changed
            case KEY_REFRESHINTERVAL:
                enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEALARM));
                break;
            default:
                break;
        }
    }

    public static class KeyEntryPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {
        private View.OnClickListener posButtonClickListener;
        private View.OnClickListener negButtonClickListener;

        private String currentAPI;
        private String key;

        private EditText keyEntry;
        private EditText keyEntry2;

        public String getKey() {
            return key;
        }

        public KeyEntryPreferenceDialogFragment() {
            super();

            if (StringUtils.isNullOrWhitespace(currentAPI))
                currentAPI = Settings.getAPI();
        }

        @SuppressLint("ValidFragment")
        public KeyEntryPreferenceDialogFragment(String currentAPI) {
            super();
            this.currentAPI = currentAPI;
        }

        public static KeyEntryPreferenceDialogFragment newInstance(String API, String key) {
            KeyEntryPreferenceDialogFragment fragment = new KeyEntryPreferenceDialogFragment(API);
            Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        protected View onCreateDialogView(Context context) {
            LayoutInflater inflater = LayoutInflater.from(context);
            if (WeatherAPI.HERE.equals(currentAPI)) {
                return inflater.inflate(R.layout.layout_keyentry2_dialog, null);
            } else {
                return inflater.inflate(R.layout.layout_keyentry_dialog, null);
            }
        }

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);

            keyEntry = view.findViewById(android.R.id.edit);
            keyEntry.addTextChangedListener(editTextWatcher);

            if (WeatherAPI.HERE.equals(currentAPI)) {
                keyEntry2 = view.findViewById(R.id.keyEntry2);
                keyEntry2.addTextChangedListener(editTextWatcher);
            }
        }

        private TextWatcher editTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (WeatherAPI.HERE.equals(currentAPI)) {
                    String app_id = null;
                    if (keyEntry != null) app_id = keyEntry.getText().toString();
                    String app_code = null;
                    if (keyEntry2 != null) app_code = keyEntry2.getText().toString();

                    key = String.format("%s;%s", app_id, app_code);
                } else {
                    key = s.toString();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        public void setPositiveButtonOnClickListener(View.OnClickListener listener) {
            posButtonClickListener = listener;
        }

        public void setNegativeButtonOnClickListener(View.OnClickListener listener) {
            negButtonClickListener = listener;
        }

        @SuppressLint("RestrictedApi")
        @Override
        public void setupDialog(Dialog dialog, int style) {
            super.setupDialog(dialog, style);
            final AlertDialog alertDialog = (AlertDialog) getDialog();
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface dialog) {
                    View posButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    View negButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                    posButton.setOnClickListener(posButtonClickListener);
                    if (negButtonClickListener == null) {
                        negButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                    } else {
                        negButton.setOnClickListener(negButtonClickListener);
                    }
                }
            });

            key = Settings.getAPIKEY();

            if (WeatherAPI.HERE.equals(currentAPI)) {
                String app_id = "";
                String app_code = "";

                if (!StringUtils.isNullOrWhitespace(key)) {
                    String[] keyArr = key.split(";");
                    if (keyArr.length > 0) {
                        app_id = keyArr[0];
                        app_code = keyArr[keyArr.length > 1 ? keyArr.length - 1 : 0];
                    }
                }

                keyEntry.setText(app_id);
                keyEntry2.setText(app_code);
            }
        }
    }

    public static class AboutAppFragment extends CustomPreferenceFragmentCompat {
        // Preference Keys
        private static final String KEY_ABOUTCREDITS = "key_aboutcredits";
        private static final String KEY_ABOUTOSLIBS = "key_aboutoslibs";
        private static final String KEY_ABOUTVERSION = "key_aboutversion";

        @Override
        protected int getTitle() {
            return R.string.pref_title_about;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_aboutapp, null);

            findPreference(KEY_ABOUTCREDITS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Display the fragment as the main content.
                    getAppCompatActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new CreditsFragment())
                            .addToBackStack(null)
                            .commit();

                    return true;
                }
            });

            findPreference(KEY_ABOUTOSLIBS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Display the fragment as the main content.
                    getAppCompatActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new OSSCreditsFragment())
                            .addToBackStack(null)
                            .commit();

                    return true;
                }
            });

            try {
                PackageInfo packageInfo = getAppCompatActivity().getPackageManager().getPackageInfo(getAppCompatActivity().getPackageName(), 0);
                findPreference(KEY_ABOUTVERSION).setSummary(String.format("v%s", packageInfo.versionName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static class CreditsFragment extends CustomPreferenceFragmentCompat {
        @Override
        protected int getTitle() {
            return R.string.pref_title_credits;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_credits, null);
        }
    }

    public static class OSSCreditsFragment extends CustomPreferenceFragmentCompat {
        @Override
        protected int getTitle() {
            return R.string.pref_title_oslibs;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_oslibs, null);
        }
    }
}
