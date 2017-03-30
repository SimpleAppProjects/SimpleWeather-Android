package com.thewizrd.simpleweather.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.thewizrd.simpleweather.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Settings
{
    public final static String Fahrenheit = "F";
    public final static String Celsius = "C";

    // Shared Settings
    private static SharedPreferences preferences = App.getPreferences();
    private static SharedPreferences.Editor editor = preferences.edit();

    // App data files
    private static File appDataFolder = App.getAppContext().getFilesDir();
    private static File locationsFile = null;

    public static String getTempUnit() {
        if (!preferences.contains("key_usecelsius"))
        {
            return Fahrenheit;
        }
        else if (preferences.getBoolean("key_usecelsius", false))
        {
            return Celsius;
        }

        return Fahrenheit;
    }

    public static boolean isWeatherLoaded() {
        if (locationsFile == null)
        {
            locationsFile = new File(appDataFolder, "locations.json");

            try {
                if (!locationsFile.exists() && !locationsFile.createNewFile()) {
                    setWeatherLoaded(false);
                    return false;
                }
            } catch (Exception e) {
                setWeatherLoaded(false);
                return false;
            }
        }

        if (locationsFile.length() == 0 || !locationsFile.exists())
            return false;

        if (!preferences.contains("weatherLoaded"))
            return false;
        else return preferences.getBoolean("weatherLoaded", false);
    }

    public static void setWeatherLoaded(boolean isLoaded)
    {
        if (isLoaded)
            editor.putBoolean("weatherLoaded", true);
        else
            editor.putBoolean("weatherLoaded", false);

        editor.commit();
    }

    public static String getAPI()
    {
        if (!preferences.contains("API"))
        {
            setAPI("WUnderground");
            return "WUnderground";
        }
        else
            return preferences.getString("API", null);
    }

    public static void setAPI(String api)
    {
        editor.putString("API", api);
        editor.commit();
    }

    public static String getAPIKEY()
    {
        if (!preferences.contains("API_KEY"))
        {
            String key;
            key = readAPIKEYfile();

            if (!TextUtils.isEmpty(key))
                setAPIKEY(key);

            return key;
        }
        else
            return preferences.getString("API_KEY", null);
    }

    private static String readAPIKEYfile()
    {
        String key = "";
        BufferedReader reader = null;

        try
        {
            reader = new BufferedReader(
                    new InputStreamReader(App.getAppContext().getAssets().open("API_KEY.txt")));

            key = reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }

        return key;
    }

    public static void setAPIKEY(String key)
    {
        if (!TextUtils.isEmpty(key))
            editor.putString("API_KEY", key);

        editor.commit();
    }

    public static List<String> getLocations_WU() {
        if (locationsFile == null) {
            locationsFile = new File(appDataFolder, "locations.json");
        }

        if (!locationsFile.exists() || locationsFile.length() == 0)
            return null;

        List<String> locations;

        try {
            locations = (ArrayList<String>) JSONParser.deserializer(FileUtils.readFile(locationsFile), ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
            locations = null;
        }

        return locations;
    }

    public static void saveLocations(List<String> locations) throws IOException {
        if (locationsFile == null) {
            locationsFile = new File(appDataFolder, "locations.json");

            if (!locationsFile.exists() && !locationsFile.createNewFile())
                throw new IOException("Unable to create locations file");
        }

        JSONParser.serializer(locations, locationsFile);
    }
}