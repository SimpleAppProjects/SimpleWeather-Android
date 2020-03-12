package com.thewizrd.simpleweather.preferences;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.ConfirmationOverlay;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.location.LocationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.wear.widget.SwipeDismissFrameLayout;

import com.google.android.wearable.intent.RemoteIntent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.controls.ProviderEntry;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearConnectionStatus;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.fragments.SwipeDismissPreferenceFragment;
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;

import java.util.HashSet;
import java.util.List;

import static com.thewizrd.shared_resources.utils.Settings.KEY_API;
import static com.thewizrd.shared_resources.utils.Settings.KEY_APIKEY;
import static com.thewizrd.shared_resources.utils.Settings.KEY_DATASYNC;
import static com.thewizrd.shared_resources.utils.Settings.KEY_FOLLOWGPS;
import static com.thewizrd.shared_resources.utils.Settings.KEY_USECELSIUS;
import static com.thewizrd.shared_resources.utils.Settings.KEY_USEPERSONALKEY;

public class SettingsActivity extends WearableActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        Fragment fragment = getFragmentManager().findFragmentById(android.R.id.content);

        // Check if fragment exists
        if (fragment == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().findFragmentById(android.R.id.content) instanceof SettingsFragment) {
            SettingsFragment fragment = (SettingsFragment) getFragmentManager().findFragmentById(android.R.id.content);
            ListPreference keyPref = (ListPreference) fragment.findPreference(KEY_API);
            if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && WeatherManager.isKeyRequired(keyPref.getValue())) {
                // Set keyentrypref color to red
                Toast.makeText(this, R.string.message_enter_apikey, Toast.LENGTH_LONG).show();
                if (fragment.getView() instanceof SwipeDismissFrameLayout) {
                    //dismissLayout.reset();
                }
                return;
            }
        }
        super.onBackPressed();
    }

    public static class SettingsFragment extends SwipeDismissPreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

        // Preference Keys
        private static final String KEY_ABOUTAPP = "key_aboutapp";
        private static final String KEY_CONNSTATUS = "key_connectionstatus";
        private static final String KEY_APIREGISTER = "key_apiregister";

        private static final String CATEGORY_API = "category_api";

        // Preferences
        private SwitchPreference followGps;
        private ListPreference providerPref;
        private SwitchPreference personalKeyPref;
        private KeyEntryPreference keyEntry;
        private ListPreference syncPreference;
        private Preference connStatusPref;
        private Preference registerPref;

        private PreferenceCategory apiCategory;

        private BroadcastReceiver connStatusReceiver;

        // Intent queue
        private HashSet<Intent.FilterComparison> intentQueue;

        @Override
        public void onResume() {
            super.onResume();

            LocalBroadcastManager.getInstance(mActivity)
                    .registerReceiver(connStatusReceiver, new IntentFilter(WearableDataListenerService.ACTION_UPDATECONNECTIONSTATUS));
            mActivity.startService(new Intent(mActivity, WearableDataListenerService.class)
                    .setAction(WearableDataListenerService.ACTION_UPDATECONNECTIONSTATUS));

            // Register listener
            ApplicationLib app = App.getInstance();
            app.getPreferences().unregisterOnSharedPreferenceChangeListener(app.getSharedPreferenceListener());
            app.getPreferences().registerOnSharedPreferenceChangeListener(this);

            // Initialize queue
            intentQueue = new HashSet<>();
        }

        @Override
        public void onPause() {
            if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && WeatherManager.isKeyRequired(providerPref.getValue())) {
                // Fallback to supported weather provider
                WeatherManager wm = WeatherManager.getInstance();
                providerPref.setValue(WeatherAPI.HERE);
                providerPref.getOnPreferenceChangeListener()
                        .onPreferenceChange(providerPref, WeatherAPI.HERE);
                Settings.setAPI(WeatherAPI.HERE);
                wm.updateAPI();

                if (wm.isKeyRequired() && StringUtils.isNullOrWhitespace(wm.getAPIKey())) {
                    // If (internal) key doesn't exist, fallback to Yahoo
                    providerPref.setValue(WeatherAPI.YAHOO);
                    providerPref.getOnPreferenceChangeListener()
                            .onPreferenceChange(providerPref, WeatherAPI.YAHOO);
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

            LocalBroadcastManager mLocalBroadcastManager =
                    LocalBroadcastManager.getInstance(mActivity);
            mLocalBroadcastManager.unregisterReceiver(connStatusReceiver);

            // Unregister listener
            ApplicationLib app = App.getInstance();
            app.getPreferences().unregisterOnSharedPreferenceChangeListener(this);
            app.getPreferences().registerOnSharedPreferenceChangeListener(app.getSharedPreferenceListener());

            for (Intent.FilterComparison filter : intentQueue) {
                if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(filter.getIntent().getAction())) {
                    WeatherManager.getInstance().updateAPI();
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI));
                    if (!BuildConfig.DEBUG) {
                        // Log event
                        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(mActivity);
                        Bundle bundle = new Bundle();
                        bundle.putString("API", Settings.getAPI());
                        bundle.putString("API_IsInternalKey", Boolean.toString(!Settings.usePersonalKey()));
                        mFirebaseAnalytics.logEvent("Update_API", bundle);
                    }
                } else if (CommonActions.ACTION_SETTINGS_UPDATEGPS.equals(filter.getIntent().getAction())) {
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEGPS));
                } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT.equals(filter.getIntent().getAction())) {
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT));
                } else if (CommonActions.ACTION_SETTINGS_UPDATEDATASYNC.equals(filter.getIntent().getAction())) {
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC));
                } else {
                    mActivity.startService(filter.getIntent());
                }
            }

            super.onPause();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            apiCategory = (PreferenceCategory) findPreference(CATEGORY_API);

            findPreference(KEY_ABOUTAPP).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Display the fragment as the main content.
                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new AboutAppFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                }
            });

            followGps = (SwitchPreference) findPreference(KEY_FOLLOWGPS);
            followGps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SwitchPreference pref = (SwitchPreference) preference;
                    if ((boolean) newValue) {
                        if (mActivity != null && mActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                mActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                            return false;
                        } else {
                            LocationManager locMan = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
                            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                                Toast.makeText(mActivity, R.string.error_enable_location_services, Toast.LENGTH_LONG).show();
                                Settings.setFollowGPS(false);
                            }
                            return false;
                        }
                    }

                    return true;
                }
            });

            keyEntry = (KeyEntryPreference) findPreference(KEY_APIKEY);
            keyEntry.setOnDialogCreatedListener(new KeyEntryPreference.DialogCreatedListener() {
                @Override
                public void beforeDialogCreated() {
                    if (keyEntry != null) {
                        keyEntry.updateAPI(providerPref.getValue());
                    }
                }
            });
            keyEntry.setPositiveButtonOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String key = keyEntry.getAPIKey();

                    String API = providerPref.getValue();
                    try {
                        if (WeatherManager.isKeyValid(key, API)) {
                            Settings.setAPIKEY(key);
                            Settings.setAPI(API);

                            Settings.setKeyVerified(true);
                            updateKeySummary();

                            keyEntry.getDialog().dismiss();
                        }
                    } catch (WeatherException e) {
                        Logger.writeLine(Log.ERROR, e);
                        Toast.makeText(mActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            personalKeyPref = (SwitchPreference) findPreference(KEY_USEPERSONALKEY);
            personalKeyPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SwitchPreference pref = (SwitchPreference) preference;
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

                    return true;
                }
            });

            registerPref = findPreference(KEY_APIREGISTER);
            registerPref.setOnPreferenceClickListener(registerPrefClickListener);

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

            syncPreference = (ListPreference) findPreference(KEY_DATASYNC);
            syncPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int newVal = Integer.parseInt(newValue.toString());

                    ListPreference pref = (ListPreference) preference;
                    pref.setSummary(pref.getEntries()[newVal]);

                    enableSyncedSettings(WearableDataSync.valueOf(newVal) == WearableDataSync.OFF);
                    return true;
                }
            });
            syncPreference.setSummary(syncPreference.getEntries()[Integer.parseInt(syncPreference.getValue())]);
            enableSyncedSettings(Settings.getDataSync() == WearableDataSync.OFF);

            connStatusPref = findPreference(KEY_CONNSTATUS);
            connStatusReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (WearableDataListenerService.ACTION_UPDATECONNECTIONSTATUS.equals(intent.getAction())) {
                        WearConnectionStatus connStatus = WearConnectionStatus.valueOf(intent.getIntExtra(WearableDataListenerService.EXTRA_CONNECTIONSTATUS, 0));
                        switch (connStatus) {
                            case DISCONNECTED:
                                connStatusPref.setSummary(R.string.status_disconnected);
                                connStatusPref.setOnPreferenceClickListener(null);
                                break;
                            case CONNECTING:
                                connStatusPref.setSummary(R.string.status_connecting);
                                connStatusPref.setOnPreferenceClickListener(null);
                                break;
                            case APPNOTINSTALLED:
                                connStatusPref.setSummary(R.string.status_notinstalled);
                                connStatusPref.setOnPreferenceClickListener(connStatusPrefClickListener);
                                break;
                            case CONNECTED:
                                connStatusPref.setSummary(R.string.status_connected);
                                connStatusPref.setOnPreferenceClickListener(null);
                                break;
                            default:
                                break;
                        }
                    }
                }
            };
        }

        private void enableSyncedSettings(boolean enable) {
            findPreference(KEY_USECELSIUS).setEnabled(enable);
            followGps.setEnabled(enable);
            apiCategory.setEnabled(enable);
        }

        private Preference.OnPreferenceClickListener connStatusPrefClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intentAndroid = new Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(WearableHelper.getPlayStoreURI());

                RemoteIntent.startRemoteActivity(mActivity, intentAndroid,
                        new ConfirmationResultReceiver(mActivity));

                return true;
            }
        };

        private Preference.OnPreferenceClickListener registerPrefClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intentAndroid = new Intent(preference.getIntent())
                        .addCategory(Intent.CATEGORY_BROWSABLE);

                RemoteIntent.startRemoteActivity(mActivity, intentAndroid,
                        new ConfirmationResultReceiver(mActivity));

                return true;
            }
        };

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
                        Toast.makeText(mActivity, R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                    }
                    return;
                default:
                    break;
            }
        }

        private boolean enqueueIntent(Intent intent) {
            if (intent == null)
                return false;
            else
                return intentQueue.add(new Intent.FilterComparison(intent));
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (StringUtils.isNullOrWhitespace(key))
                return;

            switch (key) {
                // Weather Provider changed
                case KEY_API:
                    enqueueIntent(new Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI));
                    break;
                // FollowGPS changed
                case KEY_FOLLOWGPS:
                    enqueueIntent(new Intent(CommonActions.ACTION_SETTINGS_UPDATEGPS));
                    break;
                // Settings unit changed
                case KEY_USECELSIUS:
                    enqueueIntent(new Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT));
                    break;
                // Refresh interval changed
                case KEY_DATASYNC:
                    enqueueIntent(new Intent(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC));
                    break;
                default:
                    break;
            }
        }
    }

    public static class AboutAppFragment extends SwipeDismissPreferenceFragment {
        // Preference Keys
        private static final String KEY_ABOUTCREDITS = "key_aboutcredits";
        private static final String KEY_ABOUTOSLIBS = "key_aboutoslibs";
        private static final String KEY_ABOUTVERSION = "key_aboutversion";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_aboutapp);

            findPreference(KEY_ABOUTCREDITS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Display the fragment as the main content.
                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new CreditsFragment())
                            .addToBackStack(null)
                            .commit();

                    return true;
                }
            });

            findPreference(KEY_ABOUTOSLIBS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Display the fragment as the main content.
                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new OSSCreditsFragment())
                            .addToBackStack(null)
                            .commit();

                    return true;
                }
            });

            try {
                PackageInfo packageInfo = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
                findPreference(KEY_ABOUTVERSION).setSummary(String.format("v%s", packageInfo.versionName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static class CreditsFragment extends SwipeDismissPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_credits);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference != null && preference.getIntent() != null) {
                RemoteIntent.startRemoteActivity(mActivity, preference.getIntent()
                                .setAction(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE),
                        null);

                // Show open on phone animation
                new ConfirmationOverlay().setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                        .setMessage(mActivity.getString(R.string.message_openedonphone))
                        .showAbove(getView());

                return true;
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    public static class OSSCreditsFragment extends SwipeDismissPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_oslibs);
        }
    }
}
