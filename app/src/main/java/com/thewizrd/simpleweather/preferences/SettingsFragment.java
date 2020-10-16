package com.thewizrd.simpleweather.preferences;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
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
import android.text.TextUtils;
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
import androidx.navigation.Navigation;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.controls.ProviderEntry;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.wearable.WearableWorker;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

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

public class SettingsFragment extends ToolbarPreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener, UserThemeMode.OnThemeChangeListener {

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final int PERMISSION_BGLOCATION_REQUEST_CODE = 1;

    // Preference Keys
    private static final String KEY_FEATURES = "key_features";
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
    private ListPreference languagePref;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("SettingsFragment: onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();

        AnalyticsLogger.logEvent("SettingsFragment: onResume");

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
        AnalyticsLogger.logEvent("SettingsFragment: onPause");

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
                Bundle bundle = new Bundle();
                bundle.putString("API", Settings.getAPI());
                bundle.putString("API_IsInternalKey", Boolean.toString(!Settings.usePersonalKey()));
                AnalyticsLogger.logEvent("Update_API", bundle);
            } else if (WeatherWidgetService.class.getName().equals(filter.getIntent().getComponent().getClassName())) {
                WeatherWidgetService.enqueueWork(getAppCompatActivity(), filter.getIntent());
            } else if (WeatherUpdaterWorker.class.getName().equals(filter.getIntent().getComponent().getClassName())) {
                WeatherUpdaterWorker.enqueueAction(getAppCompatActivity(), filter.getIntent().getAction());
            } else if (WearableWorker.class.getName().equals(filter.getIntent().getComponent().getClassName())) {
                WearableWorker.enqueueAction(getAppCompatActivity(), filter.getIntent().getAction());
            } else {
                getAppCompatActivity().startService(filter.getIntent());
            }
        }

        super.onPause();
    }

    @Override
    public boolean onBackPressed() {
        if (Settings.usePersonalKey() &&
                StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) &&
                WeatherManager.isKeyRequired(providerPref.getValue())) {
            // Set keyentrypref color to red
            showSnackbar(Snackbar.make(R.string.message_enter_apikey, Snackbar.Duration.LONG), null);
            return true;
        }

        return false;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_general, null);

        notCategory = findPreference(CATEGORY_NOTIFICATION);
        apiCategory = findPreference(CATEGORY_API);

        findPreference(KEY_ABOUTAPP).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Display the fragment as the main content.
                Navigation.findNavController(getAppCompatActivity(), R.id.fragment_container)
                        .navigate(SettingsFragmentDirections.actionSettingsFragmentToAboutAppFragment());
                return true;
            }
        });

        unitPref = findPreference(KEY_USECELSIUS);
        unitPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    preference.setIcon(R.drawable.ic_celsius);
                    AnalyticsLogger.logEvent("Settings: celsius checked");
                } else {
                    preference.setIcon(R.drawable.ic_fahrenheit);
                    AnalyticsLogger.logEvent("Settings: fahrenheit checked");
                }
                tintIcons(unitPref, ActivityUtils.getColor(getAppCompatActivity(), R.attr.colorPrimary));
                return true;
            }
        });

        if (Settings.isFahrenheit()) {
            unitPref.setIcon(R.drawable.ic_fahrenheit);
        } else {
            unitPref.setIcon(R.drawable.ic_celsius);
        }

        followGps = findPreference(KEY_FOLLOWGPS);
        followGps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AnalyticsLogger.logEvent("Settings: followGps toggled");
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
                            showSnackbar(Snackbar.make(R.string.error_enable_location_services, Snackbar.Duration.SHORT), null);

                            Settings.setFollowGPS(false);
                            return false;
                        } else {
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && !Settings.requestedBGAccess() &&
                                    ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                Snackbar snackbar = Snackbar.make(R.string.bg_location_permission_rationale, Snackbar.Duration.LONG);
                                snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                                PERMISSION_BGLOCATION_REQUEST_CODE);
                                    }
                                });
                                showSnackbar(snackbar, null);
                                Settings.setRequestBGAccess(true);
                            }
                        }
                    }
                }

                return true;
            }
        });

        themePref = findPreference(KEY_USERTHEME);
        themePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Bundle args = new Bundle();
                args.putString("mode", newValue.toString());
                AnalyticsLogger.logEvent("Settings: theme changed", args);

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
                    case "3": // Light
                        mode = UserThemeMode.LIGHT;
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                }
                dispatchThemeChanged(mode);
                return true;
            }
        });

        keyEntry = findPreference(KEY_APIKEY);
        personalKeyPref = findPreference(KEY_USEPERSONALKEY);
        personalKeyPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
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
        providerPref = findPreference(KEY_API);

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

        onGoingNotification = findPreference(KEY_ONGOINGNOTIFICATION);
        onGoingNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SwitchPreferenceCompat pref = (SwitchPreferenceCompat) preference;
                Context context = App.getInstance().getAppContext();

                // On-going notification
                if ((boolean) newValue) {
                    WeatherNotificationWorker.enqueueAction(context, new Intent(context, WeatherNotificationWorker.class)
                            .setAction(WeatherNotificationWorker.ACTION_REFRESHNOTIFICATION));

                    if (notCategory.findPreference(KEY_NOTIFICATIONICON) == null)
                        notCategory.addPreference(notificationIcon);

                    if (Settings.useFollowGPS() && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && !Settings.requestedBGAccess() &&
                            ContextCompat.checkSelfPermission(getAppCompatActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Snackbar snackbar = Snackbar.make(R.string.bg_location_permission_rationale, Snackbar.Duration.LONG);
                        snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        PERMISSION_BGLOCATION_REQUEST_CODE);
                            }
                        });
                        showSnackbar(snackbar, null);
                        Settings.setRequestBGAccess(true);
                    }
                } else {
                    WeatherNotificationWorker.enqueueAction(context, new Intent(context, WeatherNotificationWorker.class)
                            .setAction(WeatherNotificationWorker.ACTION_REMOVENOTIFICATION));

                    notCategory.removePreference(notificationIcon);
                }

                return true;
            }
        });

        notificationIcon = findPreference(KEY_NOTIFICATIONICON);
        notificationIcon.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Context context = App.getInstance().getAppContext();
                WeatherNotificationWorker.enqueueAction(context, new Intent(context, WeatherNotificationWorker.class)
                        .setAction(WeatherNotificationWorker.ACTION_REFRESHNOTIFICATION));
                return true;
            }
        });

        // Remove preferences
        if (!onGoingNotification.isChecked()) {
            notCategory.removePreference(notificationIcon);
        }

        alertNotification = findPreference(KEY_USEALERTS);
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

        findPreference(KEY_FEATURES).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Display the fragment as the main content.
                Navigation.findNavController(getAppCompatActivity(), R.id.fragment_container)
                        .navigate(SettingsFragmentDirections.actionSettingsFragmentToFeaturesFragment2());
                return true;
            }
        });

        languagePref = findPreference(LocaleUtils.KEY_LANGUAGE);
        CharSequence[] langCodes = languagePref.getEntryValues();
        CharSequence[] langEntries = new CharSequence[langCodes.length];
        for (int i = 0; i < langCodes.length; i++) {
            CharSequence code = langCodes[i];

            if (TextUtils.isEmpty(code)) {
                langEntries[i] = requireContext().getString(R.string.summary_default);
            } else {
                String localeCode = code.toString();
                Locale locale = new Locale(localeCode);
                langEntries[i] = locale.getDisplayName(locale);
            }
        }
        languagePref.setEntries(langEntries);

        languagePref.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                if (StringUtils.isNullOrWhitespace(preference.getValue())) {
                    return preference.getContext().getString(R.string.summary_default);
                } else {
                    return LocaleUtils.getLocaleDisplayName();
                }
            }
        });

        languagePref.setDefaultValue("");
        languagePref.setValue(LocaleUtils.getLocaleCode());
        languagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                LocaleUtils.setLocaleCode(newValue.toString());
                requireActivity().recreate();
                return true;
            }
        });

        tintIcons(getPreferenceScreen(), ActivityUtils.getColor(getAppCompatActivity(), R.attr.colorPrimary));
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

        if (getParentFragmentManager().findFragmentByTag(TAG) != null) {
            return;
        }

        if (preference instanceof EditTextPreference && KEY_APIKEY.equals(preference.getKey())) {
            final KeyEntryPreferenceDialogFragment fragment = KeyEntryPreferenceDialogFragment.newInstance(preference.getKey());
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
                        } else {
                            Toast.makeText(getAppCompatActivity(), R.string.message_keyinvalid, Toast.LENGTH_SHORT).show();
                        }
                    } catch (WeatherException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }
                }
            });

            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), TAG);
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
                    showSnackbar(Snackbar.make(R.string.error_location_denied, Snackbar.Duration.SHORT), null);
                }
                return;
            case PERMISSION_BGLOCATION_REQUEST_CODE:
                break;
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
                enqueueIntent(new Intent(context, WearableWorker.class)
                        .setAction(WearableWorker.ACTION_SENDSETTINGSUPDATE));
                enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER));
                break;
            // FollowGPS changed
            case KEY_FOLLOWGPS:
                boolean value = sharedPreferences.getBoolean(key, false);
                enqueueIntent(new Intent(context, WearableWorker.class)
                        .setAction(WearableWorker.ACTION_SENDSETTINGSUPDATE));
                enqueueIntent(new Intent(context, WearableWorker.class)
                        .setAction(WearableWorker.ACTION_SENDLOCATIONUPDATE));
                enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER));
                enqueueIntent(new Intent(context, WearableWorker.class)
                        .setAction(WearableWorker.ACTION_SENDWEATHERUPDATE));
                enqueueIntent(new Intent(context, WeatherWidgetService.class)
                        .setAction(value ? WeatherWidgetService.ACTION_REFRESHGPSWIDGETS : WeatherWidgetService.ACTION_RESETGPSWIDGETS));
                break;
            // Settings unit changed
            case KEY_USECELSIUS:
                enqueueIntent(new Intent(context, WearableWorker.class)
                        .setAction(WearableWorker.ACTION_SENDSETTINGSUPDATE));
                enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER));
                break;
            // Refresh interval changed
            case KEY_REFRESHINTERVAL:
                enqueueIntent(new Intent(context, WeatherUpdaterWorker.class)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEALARM));
                break;
            // Language changed
            case LocaleUtils.KEY_LANGUAGE:
                enqueueIntent(new Intent(context, WearableWorker.class)
                        .setAction(WearableWorker.ACTION_SENDSETTINGSUPDATE));
            default:
                break;
        }
    }

    public static class KeyEntryPreferenceDialogFragment extends EditTextPreferenceDialogFragmentCompat {
        private View.OnClickListener posButtonClickListener;
        private View.OnClickListener negButtonClickListener;

        private String key;
        private EditText keyEntry;

        public String getKey() {
            return key;
        }

        public static KeyEntryPreferenceDialogFragment newInstance(String key) {
            KeyEntryPreferenceDialogFragment fragment = new KeyEntryPreferenceDialogFragment();
            Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        protected View onCreateDialogView(Context context) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.layout_keyentry_dialog, null);
        }

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);

            keyEntry = view.findViewById(android.R.id.edit);
            keyEntry.addTextChangedListener(editTextWatcher);
        }

        private TextWatcher editTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                key = s.toString();
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
        public void setupDialog(@NonNull Dialog dialog, int style) {
            super.setupDialog(dialog, style);
            final AlertDialog alertDialog = (AlertDialog) getDialog();
            if (alertDialog != null) {
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
            }

            key = Settings.getAPIKEY();
        }
    }

    public static class FeaturesFragment extends ToolbarPreferenceFragmentCompat {
        @Override
        protected int getTitle() {
            return R.string.pref_title_features;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_features, null);
        }
    }

    public static class AboutAppFragment extends ToolbarPreferenceFragmentCompat {
        // Preference Keys
        private static final String KEY_ABOUTCREDITS = "key_aboutcredits";
        private static final String KEY_ABOUTOSLIBS = "key_aboutoslibs";
        private static final String KEY_FEEDBACK = "key_feedback";
        private static final String KEY_RATEREVIEW = "key_ratereview";
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
                    Navigation.findNavController(getAppCompatActivity(), R.id.fragment_container)
                            .navigate(SettingsFragment$AboutAppFragmentDirections.actionAboutAppFragmentToCreditsFragment());
                    return true;
                }
            });

            findPreference(KEY_ABOUTOSLIBS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Display the fragment as the main content.
                    Navigation.findNavController(getAppCompatActivity(), R.id.fragment_container)
                            .navigate(SettingsFragment$AboutAppFragmentDirections.actionAboutAppFragmentToOSSCreditsFragment());
                    return true;
                }
            });

            findPreference(KEY_FEEDBACK).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent sendTo = new Intent(Intent.ACTION_SENDTO);
                    sendTo.setData(Uri.parse("mailto:thewizrd.dev+SimpleWeatherAndroid@gmail.com"));
                    startActivity(Intent.createChooser(sendTo, null));
                    return true;
                }
            });

            findPreference(KEY_RATEREVIEW).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        final ReviewManager manager = ReviewManagerFactory.create(requireContext());
                        Task<ReviewInfo> request = manager.requestReviewFlow();
                        request.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
                            @Override
                            public void onComplete(@NonNull Task<ReviewInfo> task) {
                                if (isViewAlive()) {
                                    if (task.isSuccessful()) {
                                        // We can get the ReviewInfo object
                                        ReviewInfo reviewInfo = task.getResult();
                                        manager.launchReviewFlow(getAppCompatActivity(), reviewInfo);
                                    } else {
                                        openPlayStore();
                                    }
                                }
                            }
                        });
                    } else {
                        openPlayStore();
                    }

                    return true;
                }

                private void openPlayStore() {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .setData(WearableHelper.getPlayStoreURI()));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .setData(WearableHelper.getPlayStoreWebURI()));
                    }
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

    public static class CreditsFragment extends ToolbarPreferenceFragmentCompat {
        @Override
        protected int getTitle() {
            return R.string.pref_title_credits;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_credits, null);
        }
    }

    public static class OSSCreditsFragment extends ToolbarPreferenceFragmentCompat {
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