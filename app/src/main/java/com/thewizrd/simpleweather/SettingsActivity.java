package com.thewizrd.simpleweather;

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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.EditTextPreferenceDialogFragmentCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.controls.ProviderEntry;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.util.HashSet;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Display the fragment as the main content.
        Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);

        // Check if fragment exists
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onOptionsItemSelected(item)) {
                onBackPressed();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        String KEY_API = "API";

        if (getSupportFragmentManager().findFragmentById(android.R.id.content) instanceof SettingsFragment) {
            SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
            ListPreference keyPref = (ListPreference) fragment.findPreference(KEY_API);
            if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && WeatherManager.isKeyRequired(keyPref.getValue())) {
                // Set keyentrypref color to red
                Toast.makeText(this, R.string.message_enter_apikey, Toast.LENGTH_LONG).show();
                return;
            }
        }
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

        // Preference Keys
        private static final String KEY_ABOUTAPP = "key_aboutapp";
        private static final String KEY_FOLLOWGPS = "key_followgps";
        private static final String KEY_API = "API";
        private static final String KEY_APIKEY = "API_KEY";
        private static final String KEY_USECELSIUS = "key_usecelsius";
        private static final String KEY_REFRESHINTERVAL = "key_refreshinterval";
        private static final String KEY_ONGOINGNOTIFICATION = "key_ongoingnotification";
        private static final String KEY_NOTIFICATIONICON = "key_notificationicon";
        private static final String KEY_USEALERTS = "key_usealerts";
        private static final String KEY_APIREGISTER = "key_apiregister";
        private static final String KEY_USEPERSONALKEY = "key_usepersonalkey";

        private static final String CATEGORY_NOTIFICATION = "category_notification";
        private static final String CATEGORY_API = "category_api";

        // Preferences
        private SwitchPreferenceCompat followGps;
        private DropDownPreference providerPref;
        private SwitchPreferenceCompat personalKeyPref;
        private EditTextPreference keyEntry;
        private SwitchPreferenceCompat onGoingNotification;
        private DropDownPreference notificationIcon;
        private SwitchPreferenceCompat alertNotification;
        private Preference registerPref;

        private PreferenceCategory notCategory;
        private PreferenceCategory apiCategory;

        // Intent queue
        private HashSet<Intent.FilterComparison> intentQueue;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, null);
            setHasOptionsMenu(false);

            notCategory = (PreferenceCategory) findPreference(CATEGORY_NOTIFICATION);
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

            followGps = (SwitchPreferenceCompat) findPreference(KEY_FOLLOWGPS);
            followGps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SwitchPreferenceCompat pref = (SwitchPreferenceCompat) preference;
                    if ((boolean) newValue) {
                        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSION_LOCATION_REQUEST_CODE);
                            return false;
                        } else {
                            // Reset home location data
                            //Settings.saveLastGPSLocData(new LocationData());
                        }
                    }

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

                        if (!StringUtils.isNullOrWhitespace(selectedWProv.getAPIKey())) {
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
            providerPref = (DropDownPreference) findPreference(KEY_API);

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
                        WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                                .setAction(WeatherWidgetService.ACTION_REFRESHNOTIFICATION));

                        if (notCategory.findPreference(KEY_NOTIFICATIONICON) == null)
                            notCategory.addPreference(notificationIcon);
                    } else {
                        WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                                .setAction(WeatherWidgetService.ACTION_REMOVENOTIFICATION));

                        notCategory.removePreference(notificationIcon);
                    }

                    return true;
                }
            });

            notificationIcon = (DropDownPreference) findPreference(KEY_NOTIFICATIONICON);
            notificationIcon.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = App.getInstance().getAppContext();
                    WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                            .setAction(WeatherWidgetService.ACTION_REFRESHNOTIFICATION));
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
                        enqueueIntent(new Intent(context, WeatherWidgetService.class)
                                .setAction(WeatherWidgetService.ACTION_STARTALARM));
                    } else {
                        enqueueIntent(new Intent(context, WeatherWidgetService.class)
                                .setAction(WeatherWidgetService.ACTION_CANCELALARM));
                    }
                    return true;
                }
            });
            updateAlertPreference(WeatherManager.getInstance().supportsAlerts());
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            final String TAG = "KeyEntryPreferenceDialogFragment";

            if (getFragmentManager().findFragmentByTag(TAG) != null) {
                return;
            }

            if (preference instanceof EditTextPreference && KEY_APIKEY.equals(preference.getKey())) {
                final KeyEntryPreferenceDialogFragment fragment = KeyEntryPreferenceDialogFragment.newInstance(providerPref.getValue(), preference.getKey());
                fragment.setPositiveButtonOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String key = fragment.getKey();

                        String API = providerPref.getValue();
                        if (WeatherManager.isKeyValid(key, API)) {
                            Settings.setAPIKEY(key);
                            Settings.setAPI(API);

                            Settings.setKeyVerified(true);
                            updateKeySummary();
                            updateAlertPreference(WeatherManager.getInstance().supportsAlerts());

                            fragment.getDialog().dismiss();
                        }
                    }
                });

                fragment.setTargetFragment(this, 0);
                fragment.show(this.getFragmentManager(), TAG);
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
                        Toast.makeText(getActivity(), R.string.error_location_denied, Toast.LENGTH_SHORT).show();
                    }
                    return;
                default:
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            // Title
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.title_activity_settings);

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
                providerPref.callChangeListener(WeatherAPI.HERE);
                Settings.setAPI(WeatherAPI.HERE);
                wm.updateAPI();

                if (StringUtils.isNullOrWhitespace(wm.getAPIKey())) {
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

            for (Intent.FilterComparison filter : intentQueue) {
                if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(filter.getIntent().getAction())) {
                    WeatherManager.getInstance().updateAPI();
                } else if (WeatherWidgetService.class.getName().equals(filter.getIntent().getComponent().getClassName())) {
                    WeatherWidgetService.enqueueWork(getActivity(), filter.getIntent());
                } else {
                    getActivity().startService(filter.getIntent());
                }
            }

            super.onPause();
        }

        public boolean enqueueIntent(Intent intent) {
            if (intent == null)
                return false;
            else {
                if (WeatherWidgetService.ACTION_STARTALARM.equals(intent.getAction())) {
                    for (Intent.FilterComparison filter : intentQueue) {
                        if (WeatherWidgetService.ACTION_CANCELALARM.equals(filter.getIntent().getAction())) {
                            intentQueue.remove(filter);
                            break;
                        }
                    }
                } else if (WeatherWidgetService.ACTION_CANCELALARM.equals(intent.getAction())) {
                    for (Intent.FilterComparison filter : intentQueue) {
                        if (WeatherWidgetService.ACTION_STARTALARM.equals(filter.getIntent().getAction())) {
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

            Context context = getActivity();

            switch (key) {
                // Weather Provider changed
                case KEY_API:
                    enqueueIntent(new Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI));
                    enqueueIntent(new Intent(context, WearableDataListenerService.class)
                            .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                    enqueueIntent(new Intent(context, WeatherWidgetService.class)
                            .setAction(WeatherWidgetService.ACTION_UPDATEWEATHER));
                    break;
                // FollowGPS changed
                case KEY_FOLLOWGPS:
                    enqueueIntent(new Intent(context, WearableDataListenerService.class)
                            .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                    enqueueIntent(new Intent(context, WearableDataListenerService.class)
                            .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                    enqueueIntent(new Intent(context, WeatherWidgetService.class)
                            .setAction(WeatherWidgetService.ACTION_UPDATEWEATHER));
                    break;
                // Settings unit changed
                case KEY_USECELSIUS:
                    enqueueIntent(new Intent(context, WeatherWidgetService.class)
                            .setAction(WeatherWidgetService.ACTION_UPDATEWEATHER));
                    break;
                // Refresh interval changed
                case KEY_REFRESHINTERVAL:
                    enqueueIntent(new Intent(context, WeatherWidgetService.class)
                            .setAction(WeatherWidgetService.ACTION_UPDATEALARM));
                    break;
                default:
                    break;
            }
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
            if (WeatherAPI.HERE.equals(currentAPI)) {
                LayoutInflater inflater = LayoutInflater.from(context);
                return inflater.inflate(R.layout.layout_keyentry2_dialog, null);
            } else {
                return super.onCreateDialogView(context);
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

    public static class AboutAppFragment extends PreferenceFragmentCompat {
        // Preference Keys
        private static final String KEY_ABOUTCREDITS = "key_aboutcredits";
        private static final String KEY_ABOUTOSLIBS = "key_aboutoslibs";
        private static final String KEY_ABOUTVERSION = "key_aboutversion";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_aboutapp, null);
            setHasOptionsMenu(false);

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
                PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                findPreference(KEY_ABOUTVERSION).setSummary(String.format("v%s", packageInfo.versionName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            // Title
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.pref_title_about);
        }
    }

    public static class CreditsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_credits, null);
            setHasOptionsMenu(false);
        }

        @Override
        public void onResume() {
            super.onResume();

            // Title
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.pref_title_credits);
        }
    }

    public static class OSSCreditsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_oslibs, null);
            setHasOptionsMenu(false);
        }

        @Override
        public void onResume() {
            super.onResume();

            // Title
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.pref_title_oslibs);
        }
    }

}
