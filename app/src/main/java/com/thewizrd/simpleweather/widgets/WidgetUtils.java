package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.utils.ArrayUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WidgetUtils {
    // Shared Settings
    private static SharedPreferences widgetPrefs = App.getInstance().getAppContext().getSharedPreferences("appwidgets", Context.MODE_PRIVATE);
    private static SharedPreferences.Editor editor = widgetPrefs.edit();

    // Widget Prefs
    private static final int CurrentPrefsVersion = 3;

    // Keys
    private static final String KEY_VERSION = "key_version";
    private static final String KEY_WEATHERDATA = "key_weatherdata";
    private static final String KEY_LOCATIONDATA = "key_locationdata";
    private static final String KEY_LOCATIONQUERY = "key_locationquery";
    private static final String KEY_WIDGETBACKGROUND = "key_widgetbackground";
    private static final String KEY_WIDGETBACKGROUNDSTYLE = "key_widgetbackgroundstyle";
    private static final String KEY_HIDELOCATIONNAME = "key_hidelocationname";
    private static final String KEY_HIDESETTINGSBUTTON = "key_hidesettingsbutton";
    private static final String KEY_CLOCKAPP = "key_clockapp";
    private static final String KEY_CALENDARAPP = "key_calendarapp";
    private static final String KEY_FORECASTOPTION = "key_fcastoption";
    private static final String KEY_USETIMEZONE = "key_usetimezone";

    private static final int FORECAST_LENGTH = 3; // 3-day
    private static final int MEDIUM_FORECAST_LENGTH = 4; // 4-day
    private static final int WIDE_FORECAST_LENGTH = 5; // 5-day

    static {
        init();
    }

    enum WidgetBackground {
        CURRENT_CONDITIONS(0),
        WHITE(1),
        BLACK(2),
        TRANSPARENT(3);

        private final int value;

        public int getValue() {
            return value;
        }

        private WidgetBackground(int value) {
            this.value = value;
        }

        private static SparseArray<WidgetBackground> map = new SparseArray<>();

        static {
            for (WidgetBackground background : values()) {
                map.put(background.value, background);
            }
        }

        public static WidgetBackground valueOf(int value) {
            return map.get(value);
        }
    }

    enum WidgetBackgroundStyle {
        FULLBACKGROUND(0),
        PANDA(1),
        PENDINGCOLOR(2),
        DARK(3),
        LIGHT(4);

        private final int value;

        public int getValue() {
            return value;
        }

        private WidgetBackgroundStyle(int value) {
            this.value = value;
        }

        private static SparseArray<WidgetBackgroundStyle> map = new SparseArray<>();

        static {
            for (WidgetBackgroundStyle style : values()) {
                map.put(style.value, style);
            }
        }

        public static WidgetBackgroundStyle valueOf(int value) {
            return map.get(value);
        }
    }

    enum ForecastOption {
        FULL(0),
        DAILY(1),
        HOURLY(2);

        private final int value;

        public int getValue() {
            return value;
        }

        private ForecastOption(int value) {
            this.value = value;
        }

        private static SparseArray<ForecastOption> map = new SparseArray<>();

        static {
            for (ForecastOption opt : values()) {
                map.put(opt.value, opt);
            }
        }

        public static ForecastOption valueOf(int value) {
            return map.get(value);
        }
    }

    private static void init() {
        // Check prefs
        if (getVersion() < CurrentPrefsVersion) {
            switch (getVersion()) {
                case -1:
                    // First time, so load all current widgets under Home location
                    if (Settings.isWeatherLoaded()) {
                        LocationData homeLocation = Settings.getHomeData();
                        if (homeLocation != null) {
                            saveIds(homeLocation.getQuery(), ArrayUtils.toArrayList(getAllWidgetIds()));
                        } else {
                            break;
                        }
                    }

                    Map<String, ?> widgetMap = widgetPrefs.getAll();
                    for (String key : widgetMap.keySet()) {
                        String json = (String) widgetMap.get(key);
                        if (json.contains("System.Collections.Generic.List"))
                            json = json.replace("\"$type\":\"System.Collections.Generic.List`1[[System.Int32, mscorlib]], mscorlib\",\"$values\":", "");
                        if (json.startsWith("{") && json.endsWith("}")) {
                            int length = json.length();
                            json = json.substring(1, length - 1);
                        }
                        editor.putString(key, json).commit();
                    }
                    break;
                case 0:
                case 1:
                    widgetMap = widgetPrefs.getAll();
                    for (String key : widgetMap.keySet()) {
                        String json = (String) widgetMap.get(key);
                        if (json.contains("System.Collections.Generic.List"))
                            json = json.replace("\"$type\":\"System.Collections.Generic.List`1[[System.Int32, mscorlib]], mscorlib\",\"$values\":", "");
                        if (json.startsWith("{") && json.endsWith("}")) {
                            int length = json.length();
                            json = json.substring(1, length - 1);
                        }
                        editor.putString(key, json).commit();
                    }
                    break;
                case 2:
                    if (Settings.isWeatherLoaded()) {
                        LocationData homeLocation = Settings.getHomeData();
                        widgetMap = widgetPrefs.getAll();
                        for (String key : widgetMap.keySet()) {
                            if (key.equals(homeLocation.getQuery())) {
                                String listJson = (String) widgetMap.get(key);
                                editor.putString(Constants.KEY_GPS, listJson).commit();
                                editor.remove(key).commit();
                                break;
                            }
                        }
                    }
                    break;
                default:
                    break;
            }

            // Set to latest version
            setVersion(CurrentPrefsVersion);
        }
    }

    private static int getVersion() {
        return Integer.parseInt(widgetPrefs.getString(KEY_VERSION, "-1"));
    }

    private static void setVersion(int value) {
        editor.putString(KEY_VERSION, Integer.toString(value));
        editor.commit();
    }

    private static int[] getAllWidgetIds() {
        AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(App.getInstance().getAppContext());
        WeatherWidgetProvider1x1 mAppWidget1x1 = WeatherWidgetProvider1x1.getInstance();
        WeatherWidgetProvider2x2 mAppWidget2x2 = WeatherWidgetProvider2x2.getInstance();
        WeatherWidgetProvider4x1 mAppWidget4x1 = WeatherWidgetProvider4x1.getInstance();
        WeatherWidgetProvider4x2 mAppWidget4x2 = WeatherWidgetProvider4x2.getInstance();
        WeatherWidgetProvider4x1Google mAppWidget4x1G = WeatherWidgetProvider4x1Google.getInstance();
        WeatherWidgetProvider4x1Notification mAppWidget4x1N = WeatherWidgetProvider4x1Notification.getInstance();
        WeatherWidgetProvider4x2Clock mAppWidget4x2C = WeatherWidgetProvider4x2Clock.getInstance();
        WeatherWidgetProvider4x2Huawei mAppWidget4x2BC = WeatherWidgetProvider4x2Huawei.getInstance();

        return ArrayUtils.concat(
                mAppWidgetManager.getAppWidgetIds(mAppWidget1x1.getComponentName()),
                mAppWidgetManager.getAppWidgetIds(mAppWidget2x2.getComponentName()),
                mAppWidgetManager.getAppWidgetIds(mAppWidget4x1.getComponentName()),
                mAppWidgetManager.getAppWidgetIds(mAppWidget4x2.getComponentName()),
                mAppWidgetManager.getAppWidgetIds(mAppWidget4x1G.getComponentName()),
                mAppWidgetManager.getAppWidgetIds(mAppWidget4x1N.getComponentName()),
                mAppWidgetManager.getAppWidgetIds(mAppWidget4x2C.getComponentName()),
                mAppWidgetManager.getAppWidgetIds(mAppWidget4x2BC.getComponentName())
        );
    }

    public static void addWidgetId(String location_query, int widgetId) {
        String listJson = widgetPrefs.getString(location_query, "");
        if (StringUtils.isNullOrWhitespace(listJson)) {
            List<Integer> newlist = Collections.singletonList(widgetId);
            saveIds(location_query, newlist);
        } else {
            Type intArrListType = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
            if (idList != null && !idList.contains(widgetId)) {
                idList.add(widgetId);
                saveIds(location_query, idList);
            }
        }

        cleanupWidgetData();
        cleanupWidgetIds();
    }

    public static void removeWidgetId(String location_query, int widgetId) {
        String listJson = widgetPrefs.getString(location_query, "");
        if (!StringUtils.isNullOrWhitespace(listJson)) {
            Type intArrListType = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
            if (idList != null) {
                idList.remove(Integer.valueOf(widgetId));

                if (idList.size() == 0)
                    editor.remove(location_query).commit();
                else
                    saveIds(location_query, idList);
            }
        }
        deletePreferences(widgetId);
    }

    private static void deletePreferences(int widgetId) {
        getEditor(widgetId).clear().commit();

        Context context = App.getInstance().getAppContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences(String.format(Locale.ROOT, "appwidget_%d", widgetId));
        } else {
            String parentPath = context.getFilesDir().getParent();
            String sharedPrefsPath = String.format(Locale.ROOT, "%s/shared_prefs/appwidget_%d.xml", parentPath, widgetId);
            File sharedPrefsFile = new File(sharedPrefsPath);

            if (sharedPrefsFile.exists() &&
                    sharedPrefsFile.canWrite() && sharedPrefsFile.getParentFile().canWrite()) {
                sharedPrefsFile.delete();
            }
        }
    }

    public static void updateWidgetIds(String oldQuery, LocationData newLocation) {
        String listJson = widgetPrefs.getString(oldQuery, "");
        editor.remove(oldQuery);
        editor.putString(newLocation.getQuery(), listJson).commit();

        for (int id : getWidgetIds(newLocation.getQuery())) {
            saveLocationData(id, newLocation);
        }

        cleanupWidgetData();
        cleanupWidgetIds();
    }

    public static List<Integer> getWidgetIds(String location_query) {
        String listJson = widgetPrefs.getString(location_query, "");
        if (!StringUtils.isNullOrWhitespace(listJson)) {
            Type intArrListType = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
            if (idList != null) {
                return idList;
            }
        }

        return Collections.emptyList();
    }

    public static boolean exists(String location_query) {
        String listJson = widgetPrefs.getString(location_query, "");
        if (!StringUtils.isNullOrWhitespace(listJson)) {
            Type intArrListType = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
            if (idList != null && idList.size() > 0) {
                return true;
            }
        }

        return false;
    }

    public static boolean exists(int appWidgetId) {
        LocationData locData = getLocationData(appWidgetId);
        if (locData != null) {
            String listJson = widgetPrefs.getString(locData.getQuery(), "");
            if (!StringUtils.isNullOrWhitespace(listJson)) {
                Type intArrListType = new TypeToken<ArrayList<Integer>>() {
                }.getType();
                ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
                if (idList != null) {
                    return idList.contains(appWidgetId);
                }
            }
        }

        return false;
    }

    private static boolean saveIds(String key, List<Integer> idList) {
        String json = JSONParser.serializer(idList, ArrayList.class);
        return editor.putString(key, json)
                .commit();
    }

    private static SharedPreferences getPreferences(int appWidgetId) {
        return App.getInstance().getAppContext().getSharedPreferences(String.format(Locale.ROOT, "appwidget_%d", appWidgetId), Context.MODE_PRIVATE);
    }

    private static SharedPreferences.Editor getEditor(int appWidgetId) {
        return getPreferences(appWidgetId).edit();
    }

    public static void saveAllData(int appWidgetId, LocationData location, Weather weather) {
        SharedPreferences.Editor editor = getEditor(appWidgetId);

        String locJson = JSONParser.serializer(location, LocationData.class);
        String weatherJson = JSONParser.serializer(weather, Weather.class);

        if (locJson != null)
            editor.putString(KEY_LOCATIONDATA, locJson);
        if (weatherJson != null)
            editor.putString(KEY_WEATHERDATA, weatherJson);
        editor.commit();
    }

    static void saveLocationData(int appWidgetId, LocationData location) {
        SharedPreferences.Editor editor = getEditor(appWidgetId);

        String locJson = JSONParser.serializer(location, LocationData.class);

        if (locJson != null)
            editor.putString(KEY_LOCATIONDATA, locJson);
        editor.commit();
    }

    public static void saveWeatherData(int appWidgetId, Weather weather) {
        SharedPreferences.Editor editor = getEditor(appWidgetId);

        String weatherJson = JSONParser.serializer(weather, Weather.class);

        if (weatherJson != null)
            editor.putString(KEY_WEATHERDATA, weatherJson);
        editor.commit();
    }

    static LocationData getLocationData(int appWidgetId) {
        SharedPreferences prefs = getPreferences(appWidgetId);
        String locDataJson = prefs.getString(KEY_LOCATIONDATA, null);

        if (StringUtils.isNullOrWhitespace(locDataJson)) {
            return null;
        } else {
            return JSONParser.deserializer(locDataJson, LocationData.class);
        }
    }

    static Weather getWeatherData(int appWidgetId) {
        SharedPreferences prefs = getPreferences(appWidgetId);
        String weatherDataJson = prefs.getString(KEY_WEATHERDATA, null);

        if (StringUtils.isNullOrWhitespace(weatherDataJson)) {
            return null;
        } else {
            return JSONParser.deserializer(weatherDataJson, Weather.class);
        }
    }

    public static void cleanupWidgetIds() {
        List<LocationData> locs = new ArrayList<>(Settings.getLocationData());
        LocationData homeData = Settings.getLastGPSLocData();
        if (homeData != null) locs.add(homeData);
        List<String> currLocQueries = new ArrayList<>(locs.size());
        for (LocationData loc : locs) {
            currLocQueries.add(loc.getQuery());
        }
        Map<String, ?> widgetMap = widgetPrefs.getAll();
        for (String key : widgetMap.keySet()) {
            if (!KEY_VERSION.equals(key) && !Constants.KEY_GPS.equals(key) && !currLocQueries.contains(key))
                editor.remove(key);
        }
        editor.commit();
    }

    public static void cleanupWidgetData() {
        List<Integer> currentIds = ArrayUtils.toArrayList(getAllWidgetIds());

        Context context = App.getInstance().getAppContext();
        String parentPath = context.getFilesDir().getParent();
        String sharedPrefsPath = String.format(Locale.ROOT, "%s/shared_prefs", parentPath);
        File sharedPrefsFolder = new File(sharedPrefsPath);
        File[] appWidgetFiles = sharedPrefsFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowerCaseName = name.toLowerCase();
                return lowerCaseName.startsWith("appwidget_") && lowerCaseName.endsWith(".xml");
            }
        });

        for (File file : appWidgetFiles) {
            String fileName = file.getName();
            String idString = "";
            if (!StringUtils.isNullOrWhitespace(fileName)) {
                idString = fileName.replace("appwidget_", "")
                        .replace(".xml", "");

                try {
                    Integer id = Integer.valueOf(idString);

                    if (!currentIds.contains(id) && file.exists() && file.canWrite() && file.getParentFile().canWrite()) {
                        file.delete();
                    }
                } catch (Exception ex) {
                    Logger.writeLine(Log.ERROR, ex);
                }
            }
        }
    }

    public static boolean isGPS(int widgetId) {
        String listJson = widgetPrefs.getString(Constants.KEY_GPS, "");
        if (!StringUtils.isNullOrWhitespace(listJson)) {
            Type intArrListType = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
            if (idList != null && idList.size() > 0) {
                return idList.contains(widgetId);
            }
        }

        return false;
    }

    public static void deleteWidget(int id) {
        if (isGPS(id)) {
            removeWidgetId(Constants.KEY_GPS, id);
        } else {
            LocationData locData = getLocationData(id);
            if (locData != null) {
                removeWidgetId(locData.getQuery(), id);
            }
        }
    }

    public static void remapWidget(int oldId, int newId) {
        if (isGPS(oldId)) {
            removeWidgetId(Constants.KEY_GPS, oldId);
            addWidgetId(Constants.KEY_GPS, newId);
        } else {
            LocationData locData = getLocationData(oldId);
            if (locData != null) {
                removeWidgetId(locData.getQuery(), oldId);
                addWidgetId(locData.getQuery(), newId);
            }
        }
    }

    /**
     * Returns number of cells needed for given size of the widget.
     *
     * @param size Widget size in dp.
     * @return Size in number of cells.
     */
    public static int getCellsForSize(int size) {
        // The hardwired sizes in this function come from the hardwired formula found in
        // Android's UI guidelines for widget design:
        // http://developer.android.com/guide/practices/ui_guidelines/widget_design.html
        return (size + 30) / 70;
    }

    public static int getForecastLength(WidgetType widgetType, int cellWidth) {
        if (widgetType != WidgetType.Widget4x1 && widgetType != WidgetType.Widget4x2)
            return 0;

        boolean is4x2 = widgetType == WidgetType.Widget4x2;
        int forecastLength;

        if (cellWidth >= 5) {
            forecastLength = WIDE_FORECAST_LENGTH;
        } else if (cellWidth < 2) {
            forecastLength = 1;
        } else {
            forecastLength = is4x2 ? cellWidth + 1 : cellWidth;
        }

        return forecastLength;
    }

    public static WidgetType getWidgetTypeFromID(int appWidgetId) {
        AppWidgetProviderInfo providerInfo = AppWidgetManager.getInstance(App.getInstance().getAppContext())
                .getAppWidgetInfo(appWidgetId);

        if (providerInfo != null) {
            if (providerInfo.initialLayout == WeatherWidgetProvider1x1.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget1x1;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider2x2.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget2x2;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x1.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x1;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x2.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x2;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x1Google.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x1Google;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x1Notification.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x1Notification;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x2Clock.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x2Clock;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x2Huawei.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x2Huawei;
            }
        }

        return WidgetType.Unknown;
    }

    public static WidgetBackground getWidgetBackground(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);

        String value = prefs.getString(KEY_WIDGETBACKGROUND, "3");
        if (StringUtils.isNullOrWhitespace(value))
            value = "3";

        return WidgetBackground.valueOf(Integer.parseInt(value));
    }

    public static void setWidgetBackground(int widgetId, int value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putString(KEY_WIDGETBACKGROUND, Integer.toString(value));
        editor.commit();
    }

    public static WidgetBackgroundStyle getBackgroundStyle(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);

        String value = prefs.getString(KEY_WIDGETBACKGROUNDSTYLE, "0");
        if (StringUtils.isNullOrWhitespace(value))
            value = "0";

        return WidgetBackgroundStyle.valueOf(Integer.parseInt(value));
    }

    public static void setBackgroundStyle(int widgetId, int value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putString(KEY_WIDGETBACKGROUNDSTYLE, Integer.toString(value));
        editor.commit();
    }

    public static boolean isClockWidget(WidgetType widgetType) {
        return (widgetType == WidgetType.Widget2x2 || widgetType == WidgetType.Widget4x2 || widgetType == WidgetType.Widget4x2Clock || widgetType == WidgetType.Widget4x2Huawei);
    }

    public static boolean isDateWidget(WidgetType widgetType) {
        return (widgetType == WidgetType.Widget2x2 || widgetType == WidgetType.Widget4x2 || widgetType == WidgetType.Widget4x1Google || widgetType == WidgetType.Widget4x2Clock || widgetType == WidgetType.Widget4x2Huawei);
    }

    public static boolean isForecastWidget(WidgetType widgetType) {
        return (widgetType == WidgetType.Widget4x1 || widgetType == WidgetType.Widget4x2);
    }

    public static boolean isBackgroundOptionalWidget(WidgetType widgetType) {
        return widgetType != WidgetType.Unknown && widgetType != WidgetType.Widget4x1Google && widgetType != WidgetType.Widget4x1Notification && widgetType != WidgetType.Widget4x2Clock && widgetType != WidgetType.Widget4x2Huawei;
    }

    public static boolean isLocationNameOptionalWidget(WidgetType widgetType) {
        return widgetType == WidgetType.Widget1x1 || widgetType == WidgetType.Widget4x1 || widgetType == WidgetType.Widget4x1Google || widgetType == WidgetType.Widget4x2Clock;
    }

    public static @ColorInt
    int getTextColor(WidgetUtils.WidgetBackground background) {
        if (background == WidgetUtils.WidgetBackground.BLACK) {
            return Colors.WHITE;
        } else if (background == WidgetUtils.WidgetBackground.WHITE) {
            return Colors.BLACK;
        } else if (background == WidgetUtils.WidgetBackground.TRANSPARENT) {
            return Colors.WHITE;
        } else {
            return Colors.WHITE;
        }
    }

    public static @ColorInt
    int getPanelTextColor(WidgetUtils.WidgetBackground background, @Nullable WidgetUtils.WidgetBackgroundStyle style, boolean isNightMode) {
        if (background == WidgetUtils.WidgetBackground.BLACK ||
                style == WidgetBackgroundStyle.DARK) {
            return Colors.WHITE;
        } else if (background == WidgetUtils.WidgetBackground.WHITE ||
                style == WidgetBackgroundStyle.LIGHT) {
            return Colors.BLACK;
        } else if (background == WidgetUtils.WidgetBackground.TRANSPARENT) {
            return Colors.WHITE;
        } else if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            if (style == WidgetUtils.WidgetBackgroundStyle.PANDA)
                return isNightMode ? Colors.WHITE : Colors.BLACK;
            else
                return Colors.WHITE;
        } else {
            return Colors.WHITE;
        }
    }

    public static @ColorInt
    int getBackgroundColor(@NonNull Context context, WidgetUtils.WidgetBackground background) {
        if (background == WidgetUtils.WidgetBackground.BLACK) {
            return ContextCompat.getColor(context, R.color.card_background_dark);
        } else if (background == WidgetUtils.WidgetBackground.WHITE) {
            return Colors.WHITE;
        } else if (background == WidgetUtils.WidgetBackground.TRANSPARENT) {
            return Colors.TRANSPARENT;
        } else {
            return Colors.TRANSPARENT;
        }
    }

    public static boolean isLocationNameHidden(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);
        return prefs.getBoolean(KEY_HIDELOCATIONNAME, false);
    }

    public static void setLocationNameHidden(int widgetId, boolean value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putBoolean(KEY_HIDELOCATIONNAME, value);
        editor.commit();
    }

    public static boolean isSettingsButtonHidden(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);
        return prefs.getBoolean(KEY_HIDESETTINGSBUTTON, false);
    }

    public static void setSettingsButtonHidden(int widgetId, boolean value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putBoolean(KEY_HIDESETTINGSBUTTON, value);
        editor.commit();
    }

    public static String getOnClickClockApp() {
        SharedPreferences prefs = App.getInstance().getPreferences();
        return prefs.getString(KEY_CLOCKAPP, null);
    }

    public static void setOnClickClockApp(String value) {
        SharedPreferences.Editor editor = App.getInstance().getPreferences().edit();

        editor.putString(KEY_CLOCKAPP, value);
        editor.commit();
    }

    public static String getOnClickCalendarApp() {
        SharedPreferences prefs = App.getInstance().getPreferences();
        return prefs.getString(KEY_CALENDARAPP, null);
    }

    public static void setOnClickCalendarApp(String value) {
        SharedPreferences.Editor editor = App.getInstance().getPreferences().edit();

        editor.putString(KEY_CALENDARAPP, value);
        editor.commit();
    }

    @Nullable
    public static ComponentName getClockAppComponent(@NonNull Context context) {
        String key = WidgetUtils.getOnClickClockApp();

        if (key != null) {
            String[] data = key.split("/");
            if (data.length == 2) {
                String pkgName = data[0];
                String activityName = data[1];

                if (!StringUtils.isNullOrWhitespace(pkgName) && !StringUtils.isNullOrWhitespace(activityName)) {
                    ComponentName componentName = new ComponentName(pkgName, activityName);
                    if (ActivityUtils.verifyActivityInfo(context, componentName)) {
                        return componentName;
                    }
                }
            }

            // App not available
            WidgetUtils.setOnClickClockApp(null);
        }

        return null;
    }

    @Nullable
    public static ComponentName getCalendarAppComponent(@NonNull Context context) {
        String key = WidgetUtils.getOnClickCalendarApp();

        if (key != null) {
            String[] data = key.split("/");
            if (data.length == 2) {
                String pkgName = data[0];
                String activityName = data[1];

                if (!StringUtils.isNullOrWhitespace(pkgName) && !StringUtils.isNullOrWhitespace(activityName)) {
                    ComponentName componentName = new ComponentName(pkgName, activityName);
                    if (ActivityUtils.verifyActivityInfo(context, componentName)) {
                        return componentName;
                    }
                }
            }

            // App not available
            WidgetUtils.setOnClickCalendarApp(null);
        }

        return null;
    }

    public static ForecastOption getForecastOption(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);

        String value = prefs.getString(KEY_FORECASTOPTION, "0");
        if (StringUtils.isNullOrWhitespace(value))
            value = "0";

        return ForecastOption.valueOf(Integer.parseInt(value));
    }

    public static void setForecastOption(int widgetId, int value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putString(KEY_FORECASTOPTION, Integer.toString(value));
        editor.commit();
    }

    public static boolean useTimeZone(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);
        return prefs.getBoolean(KEY_USETIMEZONE, false);
    }

    public static void setUseTimeZone(int widgetId, boolean value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putBoolean(KEY_USETIMEZONE, value);
        editor.commit();
    }
}
