package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.App;
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
    private static final SharedPreferences widgetPrefs = App.getInstance().getAppContext().getSharedPreferences("appwidgets", Context.MODE_PRIVATE);
    private static final SharedPreferences.Editor editor = widgetPrefs.edit();

    // Widget Prefs
    private static final int CurrentPrefsVersion = 5;

    // Keys
    private static final String KEY_VERSION = "key_version";
    private static final String KEY_LOCATIONDATA = "key_locationdata";
    private static final String KEY_LOCATIONQUERY = "key_locationquery";
    private static final String KEY_WIDGETBACKGROUND = "key_widgetbackground";
    private static final String KEY_WIDGETBACKGROUNDSTYLE = "key_widgetbackgroundstyle";
    private static final String KEY_HIDELOCATIONNAME = "key_hidelocationname";
    private static final String KEY_HIDESETTINGSBUTTON = "key_hidesettingsbutton";
    private static final String KEY_CLOCKAPP = "key_clockapp";
    private static final String KEY_CALENDARAPP = "key_calendarapp";
    private static final String KEY_FORECASTOPTION = "key_fcastoption";
    private static final String KEY_TAP2SWITCH = "key_tap2switch";
    private static final String KEY_USETIMEZONE = "key_usetimezone";
    private static final String KEY_BGCOLORCODE = "key_bgcolorcode";
    private static final String KEY_TXTCOLORCODE = "key_txtcolorcode";

    private static final int FORECAST_LENGTH = 3; // 3-day
    private static final int MEDIUM_FORECAST_LENGTH = 4; // 4-day
    private static final int WIDE_FORECAST_LENGTH = 5; // 5-day

    static {
        init();
    }

    enum WidgetBackground {
        CURRENT_CONDITIONS(0),
        TRANSPARENT(1),
        CUSTOM(2);

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
            // NOTE: set default since we removed a style here
            return map.get(value, PANDA);
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
                case 3:
                    // Migrate color options
                    int[] widgetIds = getAllWidgetIds();
                    for (int appWidgetId : widgetIds) {
                        SharedPreferences prefs = getPreferences(appWidgetId);
                        String value = prefs.getString(KEY_WIDGETBACKGROUND, null);
                        if (value != null) {
                            Integer intVal = NumberUtils.tryParseInt(value, -1);
                            switch (intVal) {
                                case 0: // No-op
                                    break;
                                case 1: // Old: White
                                    setWidgetBackground(appWidgetId, WidgetBackground.CUSTOM.getValue());
                                    setBackgroundColor(appWidgetId, Colors.WHITE);
                                    setTextColor(appWidgetId, Colors.BLACK);
                                    break;
                                case 2: // Old: Black
                                    setWidgetBackground(appWidgetId, WidgetBackground.CUSTOM.getValue());
                                    setBackgroundColor(appWidgetId, Colors.BLACK);
                                    setTextColor(appWidgetId, Colors.WHITE);
                                    break;
                                case 4: // Old: Custom
                                    setWidgetBackground(appWidgetId, WidgetBackground.CUSTOM.getValue());
                                    break;
                                case 3: // Old: Transparent
                                default:
                                    setWidgetBackground(appWidgetId, WidgetBackground.TRANSPARENT.getValue());
                                    break;
                            }
                        }
                    }
                    break;
                case 4:
                    // Migrate color options
                    int[] widgetIds4x1 = getWidgetIds(WidgetType.Widget4x1);
                    for (int appWidgetId : widgetIds4x1) {
                        setWidgetBackground(appWidgetId, WidgetBackground.TRANSPARENT.getValue());
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
        return Integer.parseInt(widgetPrefs.getString(KEY_VERSION, String.valueOf(CurrentPrefsVersion)));
    }

    private static void setVersion(int value) {
        editor.putString(KEY_VERSION, Integer.toString(value));
        editor.commit();
    }

    private static int[] getAllWidgetIds() {
        AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(App.getInstance().getAppContext());
        WidgetProviderInfo mAppWidget1x1 = WeatherWidgetProvider1x1.Info.getInstance();
        WidgetProviderInfo mAppWidget2x2 = WeatherWidgetProvider2x2.Info.getInstance();
        WidgetProviderInfo mAppWidget4x1 = WeatherWidgetProvider4x1.Info.getInstance();
        WidgetProviderInfo mAppWidget4x2 = WeatherWidgetProvider4x2.Info.getInstance();
        WidgetProviderInfo mAppWidget4x1G = WeatherWidgetProvider4x1.Info.getInstance();
        WidgetProviderInfo mAppWidget4x1N = WeatherWidgetProvider4x1Notification.Info.getInstance();
        WidgetProviderInfo mAppWidget4x2C = WeatherWidgetProvider4x2Clock.Info.getInstance();
        WidgetProviderInfo mAppWidget4x2BC = WeatherWidgetProvider4x2Huawei.Info.getInstance();

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

    private static int[] getWidgetIds(WidgetType widgetType) {
        AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(App.getInstance().getAppContext());

        switch (widgetType) {
            default:
            case Unknown:
                return new int[0];
            case Widget1x1:
                return mAppWidgetManager.getAppWidgetIds(WeatherWidgetProvider1x1.Info.getInstance().getComponentName());
            case Widget2x2:
                return mAppWidgetManager.getAppWidgetIds(WeatherWidgetProvider2x2.Info.getInstance().getComponentName());
            case Widget4x1:
                return mAppWidgetManager.getAppWidgetIds(WeatherWidgetProvider4x1.Info.getInstance().getComponentName());
            case Widget4x2:
                return mAppWidgetManager.getAppWidgetIds(WeatherWidgetProvider4x2.Info.getInstance().getComponentName());
            case Widget4x1Google:
                return mAppWidgetManager.getAppWidgetIds(WeatherWidgetProvider4x1Google.Info.getInstance().getComponentName());
            case Widget4x1Notification:
                return mAppWidgetManager.getAppWidgetIds(WeatherWidgetProvider4x1Notification.Info.getInstance().getComponentName());
            case Widget4x2Clock:
                return mAppWidgetManager.getAppWidgetIds(WeatherWidgetProvider4x2Clock.Info.getInstance().getComponentName());
            case Widget4x2Huawei:
                return mAppWidgetManager.getAppWidgetIds(WeatherWidgetProvider4x2Huawei.Info.getInstance().getComponentName());
        }
    }

    @Nullable
    public static WidgetProviderInfo getWidgetProviderInfoFromType(final WidgetType widgetType) {
        switch (widgetType) {
            default:
            case Unknown:
                return null;
            case Widget1x1:
                return WeatherWidgetProvider1x1.Info.getInstance();
            case Widget2x2:
                return WeatherWidgetProvider2x2.Info.getInstance();
            case Widget4x1:
                return WeatherWidgetProvider4x1.Info.getInstance();
            case Widget4x2:
                return WeatherWidgetProvider4x2.Info.getInstance();
            case Widget4x1Google:
                return WeatherWidgetProvider4x1Google.Info.getInstance();
            case Widget4x1Notification:
                return WeatherWidgetProvider4x1Notification.Info.getInstance();
            case Widget4x2Clock:
                return WeatherWidgetProvider4x2Clock.Info.getInstance();
            case Widget4x2Huawei:
                return WeatherWidgetProvider4x2Huawei.Info.getInstance();
        }
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
            if (idList != null && idList.contains(widgetId)) {
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

    static void saveLocationData(int appWidgetId, LocationData location) {
        SharedPreferences.Editor editor = getEditor(appWidgetId);

        String locJson = JSONParser.serializer(location, LocationData.class);

        if (locJson != null)
            editor.putString(KEY_LOCATIONDATA, locJson);
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
                saveLocationData(newId, locData);
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

        int forecastLength;

        if (cellWidth >= 5) {
            forecastLength = WIDE_FORECAST_LENGTH + 1;
        } else if (cellWidth < 2) {
            forecastLength = 1;
        } else {
            forecastLength = cellWidth + 1;
        }

        return forecastLength;
    }

    public static WidgetType getWidgetTypeFromID(int appWidgetId) {
        AppWidgetProviderInfo providerInfo = AppWidgetManager.getInstance(App.getInstance().getAppContext())
                .getAppWidgetInfo(appWidgetId);

        if (providerInfo != null) {
            if (providerInfo.initialLayout == WeatherWidgetProvider1x1.Info.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget1x1;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider2x2.Info.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget2x2;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x1.Info.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x1;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x2.Info.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x2;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x1Google.Info.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x1Google;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x1Notification.Info.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x1Notification;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x2Clock.Info.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x2Clock;
            } else if (providerInfo.initialLayout == WeatherWidgetProvider4x2Huawei.Info.getInstance().getWidgetLayoutId()) {
                return WidgetType.Widget4x2Huawei;
            }
        }

        return WidgetType.Unknown;
    }

    public static WidgetBackground getWidgetBackground(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);

        String value = prefs.getString(KEY_WIDGETBACKGROUND, "1");
        if (StringUtils.isNullOrWhitespace(value))
            value = "1";

        return WidgetBackground.valueOf(Integer.parseInt(value));
    }

    public static void setWidgetBackground(int widgetId, int value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putString(KEY_WIDGETBACKGROUND, Integer.toString(value));
        editor.commit();
    }

    public static WidgetBackgroundStyle getBackgroundStyle(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);

        String value = prefs.getString(KEY_WIDGETBACKGROUNDSTYLE, "1");
        if (StringUtils.isNullOrWhitespace(value))
            value = "1";

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
        return widgetType == WidgetType.Widget2x2 || widgetType == WidgetType.Widget4x2;
    }

    public static boolean isLocationNameOptionalWidget(WidgetType widgetType) {
        return widgetType == WidgetType.Widget1x1 || widgetType == WidgetType.Widget4x1 || widgetType == WidgetType.Widget4x1Google || widgetType == WidgetType.Widget4x2Clock;
    }

    public static @ColorInt
    int getTextColor(final int appWidgetId, WidgetUtils.WidgetBackground background) {
        if (background == WidgetBackground.CUSTOM) {
            return getTextColor(appWidgetId);
        } else {
            return Colors.WHITE;
        }
    }

    public static @ColorInt
    int getPanelTextColor(final int appWidgetId, WidgetUtils.WidgetBackground background, @Nullable WidgetUtils.WidgetBackgroundStyle style, boolean isNightMode) {
        if (background == WidgetBackground.CUSTOM) {
            return getTextColor(appWidgetId);
        } else if (style == WidgetBackgroundStyle.DARK) {
            return Colors.WHITE;
        } else if (style == WidgetBackgroundStyle.LIGHT) {
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
    int getBackgroundColor(int appWidgetId, WidgetUtils.WidgetBackground background) {
        if (background == WidgetUtils.WidgetBackground.CUSTOM) {
            return getBackgroundColor(appWidgetId);
        } else {
            return Colors.TRANSPARENT;
        }
    }

    public static @ColorInt
    int getBackgroundColor(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);
        return prefs.getInt(KEY_BGCOLORCODE, Color.TRANSPARENT);
    }

    public static void setBackgroundColor(int widgetId, @ColorInt int value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putInt(KEY_BGCOLORCODE, value);
        editor.commit();
    }

    public static @ColorInt
    int getTextColor(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);
        return prefs.getInt(KEY_TXTCOLORCODE, Color.WHITE);
    }

    public static void setTextColor(int widgetId, @ColorInt int value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putInt(KEY_TXTCOLORCODE, value);
        editor.commit();
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
                    if (ContextUtils.verifyActivityInfo(context, componentName)) {
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
                    if (ContextUtils.verifyActivityInfo(context, componentName)) {
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

    public static boolean isTap2Switch(int widgetId) {
        SharedPreferences prefs = getPreferences(widgetId);
        return prefs.getBoolean(KEY_TAP2SWITCH, true);
    }

    public static void setTap2Switch(int widgetId, boolean value) {
        SharedPreferences.Editor editor = getEditor(widgetId);

        editor.putBoolean(KEY_TAP2SWITCH, value);
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
