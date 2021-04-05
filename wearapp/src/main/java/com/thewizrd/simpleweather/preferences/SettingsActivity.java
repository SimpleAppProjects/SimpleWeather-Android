package com.thewizrd.simpleweather.preferences;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.wearable.view.ConfirmationOverlay;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.wearable.intent.RemoteIntent;
import com.thewizrd.extras.ExtrasLibrary;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.controls.ProviderEntry;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearConnectionStatus;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.fragments.SwipeDismissPreferenceFragment;
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver;
import com.thewizrd.simpleweather.preferences.iconpreference.iconpreference.IconProviderPickerFragment;
import com.thewizrd.simpleweather.wearable.WearableListenerActivity;
import com.thewizrd.simpleweather.wearable.WeatherComplicationHelper;
import com.thewizrd.simpleweather.wearable.WeatherTileHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_API;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_APIKEY;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_DATASYNC;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_DISTANCEUNIT;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_FOLLOWGPS;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_PRECIPITATIONUNIT;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_PRESSUREUNIT;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_SPEEDUNIT;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_TEMPUNIT;
import static com.thewizrd.shared_resources.utils.SettingsManager.KEY_USEPERSONALKEY;

public class SettingsActivity extends WearableListenerActivity {
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;

    @Override
    protected BroadcastReceiver getBroadcastReceiver() {
        return mBroadcastReceiver;
    }

    @Override
    protected IntentFilter getIntentFilter() {
        return mIntentFilter;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Use night mode resources (needed for external weather icons)
        super.attachBaseContext(ContextUtils.getThemeContextOverride(newBase, false));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("SettingsActivity: onCreate");

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_SENDCONNECTIONSTATUS.equals(intent.getAction())) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            updateConnectionStatus();
                        }
                    });
                }
            }
        };

        mIntentFilter = new IntentFilter(ACTION_SENDCONNECTIONSTATUS);

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
        Fragment current = getFragmentManager().findFragmentById(android.R.id.content);
        OnBackPressedFragmentListener fragBackPressedListener = null;
        if (current instanceof OnBackPressedFragmentListener)
            fragBackPressedListener = (OnBackPressedFragmentListener) current;

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            super.onBackPressed();
        }
    }

    public static class SettingsFragment extends SwipeDismissPreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener, OnBackPressedFragmentListener {

        private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

        // Preference Keys
        private static final String KEY_ABOUTAPP = "key_aboutapp";
        private static final String KEY_CONNSTATUS = "key_connectionstatus";
        private static final String KEY_APIREGISTER = "key_apiregister";
        private static final String KEY_UNITS = "key_units";
        private static final String KEY_ICONS = "key_icons";

        private static final String CATEGORY_GENERAL = "category_general";
        private static final String CATEGORY_API = "category_api";

        // Preferences
        private SwitchPreference followGps;
        private ListPreference languagePref;
        private ListPreference providerPref;
        private SwitchPreference personalKeyPref;
        private KeyEntryPreference keyEntry;
        private ListPreference syncPreference;
        private Preference unitsPref;
        private Preference iconsPref;
        private Preference connStatusPref;
        private Preference registerPref;

        private PreferenceCategory generalCategory;
        private PreferenceCategory apiCategory;

        // Intent queue
        private HashSet<Intent.FilterComparison> intentQueue;

        // Wearable status
        private WearConnectionStatus mConnectionStatus = WearConnectionStatus.DISCONNECTED;
        private BroadcastReceiver statusReceiver;

        @Override
        public boolean onBackPressed() {
            if (getSettingsManager().usePersonalKey() &&
                    StringUtils.isNullOrWhitespace(getSettingsManager().getAPIKEY()) &&
                    WeatherManager.isKeyRequired(providerPref.getValue())) {
                // Set keyentrypref color to red
                showToast(R.string.message_enter_apikey, Toast.LENGTH_SHORT);
                return true;
            }

            return false;
        }

        @Override
        public void onResume() {
            super.onResume();

            AnalyticsLogger.logEvent("SettingsFragment: onResume");

            // Register listener
            ApplicationLib app = App.getInstance();
            app.unregisterAppSharedPreferenceListener();
            app.registerAppSharedPreferenceListener(this);
            // Initialize queue
            intentQueue = new HashSet<>();

            statusReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (WearableListenerActivity.ACTION_UPDATECONNECTIONSTATUS.equals(intent.getAction())) {
                        mConnectionStatus = WearConnectionStatus.valueOf(intent.getIntExtra(WearableListenerActivity.EXTRA_CONNECTIONSTATUS, 0));
                        updateConnectionPref();
                    }
                }
            };

            LocalBroadcastManager mBroadcastMgr =
                    LocalBroadcastManager.getInstance(getParentActivity());
            mBroadcastMgr.registerReceiver(statusReceiver,
                    new IntentFilter(WearableListenerActivity.ACTION_UPDATECONNECTIONSTATUS));
            mBroadcastMgr.sendBroadcast(
                    new Intent(WearableListenerActivity.ACTION_SENDCONNECTIONSTATUS));
        }

        @Override
        public void onPause() {
            AnalyticsLogger.logEvent("SettingsFragment: onPause");

            if (getSettingsManager().usePersonalKey() && StringUtils.isNullOrWhitespace(getSettingsManager().getAPIKEY()) && WeatherManager.isKeyRequired(providerPref.getValue())) {
                // Fallback to supported weather provider
                final String API = RemoteConfig.getDefaultWeatherProvider();
                providerPref.setValue(API);
                providerPref.getOnPreferenceChangeListener()
                        .onPreferenceChange(providerPref, API);
                getSettingsManager().setAPI(API);
                WeatherManager.getInstance().updateAPI();

                getSettingsManager().setPersonalKey(false);
                getSettingsManager().setKeyVerified(true);
            }

            // Unregister listener
            ApplicationLib app = App.getInstance();
            app.unregisterAppSharedPreferenceListener(this);
            app.registerAppSharedPreferenceListener();

            LocalBroadcastManager mLocalBroadcastManager =
                    LocalBroadcastManager.getInstance(getParentActivity());
            mLocalBroadcastManager.unregisterReceiver(statusReceiver);

            for (Intent.FilterComparison filter : intentQueue) {
                if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(filter.getIntent().getAction())) {
                    WeatherManager.getInstance().updateAPI();
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI));
                    // Log event
                    Bundle bundle = new Bundle();
                    bundle.putString("API", getSettingsManager().getAPI());
                    bundle.putString("API_IsInternalKey", Boolean.toString(!getSettingsManager().usePersonalKey()));
                    AnalyticsLogger.logEvent("Update_API", bundle);
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
                    getParentActivity().startService(filter.getIntent());
                }
            }

            super.onPause();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState) {
            addPreferencesFromResource(R.xml.pref_general);

            generalCategory = (PreferenceCategory) findPreference(CATEGORY_GENERAL);
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
                    AnalyticsLogger.logEvent("Settings: followGps toggled");

                    if ((boolean) newValue) {
                        if (ContextCompat.checkSelfPermission(getParentActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(getParentActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                            return false;
                        } else {
                            LocationManager locMan = (LocationManager) getParentActivity().getSystemService(Context.LOCATION_SERVICE);
                            if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                                showToast(R.string.error_enable_location_services, Toast.LENGTH_SHORT);

                                getSettingsManager().setFollowGPS(false);
                                return false;
                            }
                        }
                    }

                    return true;
                }
            });

            iconsPref = findPreference(KEY_ICONS);
            iconsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Display the fragment as the main content.
                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new IconsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                }
            });

            unitsPref = findPreference(KEY_UNITS);
            unitsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Display the fragment as the main content.
                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new UnitsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                }
            });

            languagePref = (ListPreference) findPreference(LocaleUtils.KEY_LANGUAGE);
            CharSequence[] langCodes = languagePref.getEntryValues();
            CharSequence[] langEntries = new CharSequence[langCodes.length];
            for (int i = 0; i < langCodes.length; i++) {
                CharSequence code = langCodes[i];

                if (TextUtils.isEmpty(code)) {
                    langEntries[i] = getString(R.string.summary_default);
                } else {
                    String localeCode = code.toString();
                    Locale locale = new Locale(localeCode);
                    langEntries[i] = locale.getDisplayName(locale);
                }
            }
            languagePref.setEntries(langEntries);

            languagePref.setDefaultValue("");
            languagePref.setValue(LocaleUtils.getLocaleCode());
            languagePref.setSummary(localeSummaryFunc.apply(languagePref.getValue()));
            languagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    LocaleUtils.setLocaleCode(newValue.toString());
                    languagePref.setSummary(localeSummaryFunc.apply(newValue.toString()));
                    return true;
                }
            });

            keyEntry = (KeyEntryPreference) findPreference(KEY_APIKEY);
            keyEntry.setPositiveButtonOnClickListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String key = keyEntry.getAPIKey();

                    String API = providerPref.getValue();
                    try {
                        if (WeatherManager.isKeyValid(key, API)) {
                            getSettingsManager().setAPIKEY(key);
                            getSettingsManager().setAPI(API);

                            getSettingsManager().setKeyVerified(true);
                            updateKeySummary();

                            dialog.dismiss();
                        } else {
                            showToast(R.string.message_keyinvalid, Toast.LENGTH_SHORT);
                        }
                    } catch (WeatherException e) {
                        Logger.writeLine(Log.ERROR, e);
                        showToast(e.getMessage(), Toast.LENGTH_SHORT);
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
                            getSettingsManager().setKeyVerified(true);
                            getSettingsManager().setAPI(providerPref.getValue());
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
                    if (Objects.equals(WeatherAPI.HERE, newValue.toString()) && !ExtrasLibrary.Companion.isEnabled()) {
                        showToast(R.string.message_premium_required, Toast.LENGTH_SHORT);
                        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(
                                new Intent(WearableListenerActivity.ACTION_OPENONPHONE)
                                        .putExtra(WearableListenerActivity.EXTRA_SHOWANIMATION, true));
                        return false;
                    }

                    ListPreference pref = (ListPreference) preference;
                    WeatherProviderImpl selectedWProv = WeatherManager.getProvider(newValue.toString());

                    if (selectedWProv.isKeyRequired()) {
                        if (StringUtils.isNullOrWhitespace(selectedWProv.getAPIKey())) {
                            getSettingsManager().setPersonalKey(true);
                            personalKeyPref.setChecked(true);
                            personalKeyPref.setEnabled(false);
                            keyEntry.setEnabled(false);
                            apiCategory.removePreference(keyEntry);
                            apiCategory.removePreference(registerPref);
                        } else {
                            personalKeyPref.setEnabled(true);
                        }

                        if (!getSettingsManager().usePersonalKey()) {
                            // We're using our own (verified) keys
                            getSettingsManager().setKeyVerified(true);
                            keyEntry.setEnabled(false);
                            apiCategory.removePreference(keyEntry);
                            apiCategory.removePreference(registerPref);
                        } else {
                            // User is using personal (unverified) keys
                            getSettingsManager().setKeyVerified(false);
                            // Clear API KEY entry to avoid issues
                            getSettingsManager().setAPIKEY("");

                            keyEntry.setEnabled(true);

                            if (apiCategory.findPreference(KEY_APIKEY) == null)
                                apiCategory.addPreference(keyEntry);
                            if (apiCategory.findPreference(KEY_APIREGISTER) == null)
                                apiCategory.addPreference(registerPref);
                        }

                        if (apiCategory.findPreference(KEY_USEPERSONALKEY) == null)
                            apiCategory.addPreference(personalKeyPref);

                        // Reset to old value if not verified
                        if (!getSettingsManager().isKeyVerified())
                            getSettingsManager().setAPI(pref.getValue());
                        else
                            getSettingsManager().setAPI(newValue.toString());

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
                        getSettingsManager().setKeyVerified(false);
                        keyEntry.setEnabled(false);
                        personalKeyPref.setEnabled(false);

                        getSettingsManager().setAPI(newValue.toString());
                        // Clear API KEY entry to avoid issues
                        getSettingsManager().setAPIKEY("");

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

                if (!StringUtils.isNullOrWhitespace(getSettingsManager().getAPIKEY()) && !getSettingsManager().isKeyVerified())
                    getSettingsManager().setKeyVerified(true);

                if (StringUtils.isNullOrWhitespace(WeatherManager.getInstance().getAPIKey())) {
                    getSettingsManager().setPersonalKey(true);
                    personalKeyPref.setChecked(true);
                    personalKeyPref.setEnabled(false);
                    keyEntry.setEnabled(false);
                    apiCategory.removePreference(keyEntry);
                    apiCategory.removePreference(registerPref);
                } else {
                    personalKeyPref.setEnabled(true);
                }

                if (!getSettingsManager().usePersonalKey()) {
                    // We're using our own (verified) keys
                    getSettingsManager().setKeyVerified(true);
                    keyEntry.setEnabled(false);
                    apiCategory.removePreference(keyEntry);
                    apiCategory.removePreference(registerPref);
                } else {
                    // User is using personal (unverified) keys
                    //getSettingsManager().setKeyVerified(false);
                    // Clear API KEY entry to avoid issues
                    //getSettingsManager().setAPIKEY("");

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
                getSettingsManager().setKeyVerified(false);
                // Clear API KEY entry to avoid issues
                getSettingsManager().setAPIKEY("");
            }

            updateKeySummary();
            updateRegisterLink();

            syncPreference = (ListPreference) findPreference(KEY_DATASYNC);
            syncPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int newVal = Integer.parseInt(newValue.toString());

                    Bundle args = new Bundle();
                    args.putInt("mode", newVal);
                    AnalyticsLogger.logEvent("Settings: sync pref changed", args);

                    ListPreference pref = (ListPreference) preference;
                    pref.setSummary(pref.getEntries()[newVal]);

                    enableSyncedSettings(WearableDataSync.valueOf(newVal) == WearableDataSync.OFF);
                    return true;
                }
            });
            syncPreference.setSummary(syncPreference.getEntries()[Integer.parseInt(syncPreference.getValue())]);
            enableSyncedSettings(getSettingsManager().getDataSync() == WearableDataSync.OFF);

            connStatusPref = findPreference(KEY_CONNSTATUS);
        }

        private final Function<String, CharSequence> localeSummaryFunc = new Function<String, CharSequence>() {
            @Override
            public CharSequence apply(String input) {
                if (StringUtils.isNullOrWhitespace(input)) {
                    return getString(R.string.summary_default);
                } else {
                    return LocaleUtils.getLocaleDisplayName();
                }
            }
        };

        private void enableSyncedSettings(boolean enable) {
            generalCategory.setEnabled(enable);
            apiCategory.setEnabled(enable);
        }

        private final Preference.OnPreferenceClickListener connStatusPrefClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intentAndroid = new Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(WearableHelper.getPlayStoreURI());

                RemoteIntent.startRemoteActivity(getParentActivity(), intentAndroid,
                        new ConfirmationResultReceiver(getParentActivity()));

                return true;
            }
        };

        private final Preference.OnPreferenceClickListener registerPrefClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intentAndroid = new Intent(preference.getIntent())
                        .addCategory(Intent.CATEGORY_BROWSABLE);

                RemoteIntent.startRemoteActivity(getParentActivity(), intentAndroid,
                        new ConfirmationResultReceiver(getParentActivity()));

                return true;
            }
        };

        private void updateKeySummary() {
            updateKeySummary(providerPref.getEntry());
        }

        private void updateKeySummary(CharSequence providerAPI) {
            if (!StringUtils.isNullOrWhitespace(getSettingsManager().getAPIKEY())) {
                boolean keyVerified = getSettingsManager().isKeyVerified();

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

            if (prov != null) {
                registerPref.setIntent(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(prov.getApiRegisterURL())));
            }
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
                        getSettingsManager().setFollowGPS(true);
                        // Reset home location data
                        //getSettingsManager().SaveLastGPSLocData(new WeatherData.LocationData());
                    } else {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        followGps.setChecked(false);
                        getSettingsManager().setFollowGPS(false);
                        showToast(R.string.error_location_denied, Toast.LENGTH_SHORT);
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
                // Refresh interval changed
                case KEY_DATASYNC:
                    enqueueIntent(new Intent(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC));
                    break;
                default:
                    break;
            }
        }

        private void updateConnectionPref() {
            switch (mConnectionStatus) {
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

    public static class UnitsFragment extends SwipeDismissPreferenceFragment {
        private static final String KEY_RESETUNITS = "key_resetunits";

        private ListPreference tempUnitPref;
        private ListPreference speedUnitPref;
        private ListPreference distanceUnitPref;
        private ListPreference precipationUnitPref;
        private ListPreference pressureUnitPref;

        private LocalBroadcastManager localBroadcastMgr;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState) {
            addPreferencesFromResource(R.xml.pref_units);

            getPreferenceScreen().setTitle(R.string.pref_title_units);

            localBroadcastMgr = LocalBroadcastManager.getInstance(getParentActivity());

            tempUnitPref = (ListPreference) findPreference(KEY_TEMPUNIT);
            speedUnitPref = (ListPreference) findPreference(KEY_SPEEDUNIT);
            distanceUnitPref = (ListPreference) findPreference(KEY_DISTANCEUNIT);
            precipationUnitPref = (ListPreference) findPreference(KEY_PRECIPITATIONUNIT);
            pressureUnitPref = (ListPreference) findPreference(KEY_PRESSUREUNIT);

            findPreference(KEY_RESETUNITS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getParentActivity())
                            .setTitle(R.string.pref_title_units)
                            .setItems(R.array.default_units, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final boolean isFahrenheit = which == 0;
                                    tempUnitPref.setValue(isFahrenheit ? Units.FAHRENHEIT : Units.CELSIUS);
                                    speedUnitPref.setValue(isFahrenheit ? Units.MILES_PER_HOUR : Units.KILOMETERS_PER_HOUR);
                                    distanceUnitPref.setValue(isFahrenheit ? Units.MILES : Units.KILOMETERS);
                                    precipationUnitPref.setValue(isFahrenheit ? Units.INCHES : Units.MILLIMETERS);
                                    pressureUnitPref.setValue(isFahrenheit ? Units.INHG : Units.MILLIBAR);
                                    dialog.dismiss();

                                    localBroadcastMgr.sendBroadcast(new Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT));
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setCancelable(true)
                            .show();
                    return true;
                }
            });
        }
    }

    public static class IconsFragment extends IconProviderPickerFragment {
        @Override
        protected void onSelectionPerformed(boolean success) {
            super.onSelectionPerformed(success);

            // Update tiles and complications
            WeatherComplicationHelper.requestComplicationUpdateAll(getContext());
            WeatherTileHelper.requestTileUpdateAll(getContext());
        }
    }

    public static class AboutAppFragment extends SwipeDismissPreferenceFragment {
        // Preference Keys
        private static final String KEY_ABOUTCREDITS = "key_aboutcredits";
        private static final String KEY_ABOUTOSLIBS = "key_aboutoslibs";
        private static final String KEY_ABOUTVERSION = "key_aboutversion";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState) {
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
                PackageInfo packageInfo = getParentActivity().getPackageManager().getPackageInfo(getParentActivity().getPackageName(), 0);
                findPreference(KEY_ABOUTVERSION).setSummary(String.format("v%s", packageInfo.versionName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static class CreditsFragment extends SwipeDismissPreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState) {
            addPreferencesFromResource(R.xml.pref_credits);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference != null && preference.getIntent() != null) {
                RemoteIntent.startRemoteActivity(getParentActivity(), preference.getIntent()
                                .setAction(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE),
                        null);

                // Show open on phone animation
                new ConfirmationOverlay().setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                        .setMessage(getParentActivity().getString(R.string.message_openedonphone))
                        .showAbove(getView());

                return true;
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    public static class OSSCreditsFragment extends SwipeDismissPreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState) {
            addPreferencesFromResource(R.xml.pref_oslibs);
        }
    }
}
