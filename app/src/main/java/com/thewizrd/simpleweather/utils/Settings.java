package com.thewizrd.simpleweather.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.thewizrd.simpleweather.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by bryan on 2/8/2017.
 */

public class Settings
{
    private static boolean weather_loaded = false;
    private static String api;
    private static String api_key;

    private static String Farenheit = "F";
    private static String Celsius = "C";

    // Shared Settings
    private static SharedPreferences preferences = App.getPreferences();
    private static SharedPreferences.Editor editor = preferences.edit();

    // App data files
    private static File appDataFolder = App.getAppContext().getFilesDir();
    private static File locationsFile;

    public static String getTempUnit() {
        if (!preferences.contains("Unit"))
        {
            return Farenheit;
        }
        else if (preferences.getString("Units", Farenheit).equals(Celsius))
        {
            return Celsius;
        }

        return Farenheit;
    }

    public static void setTempUnit(String unit)
    {
        if (unit.equals(Celsius))
            editor.putString("Unit", Celsius);
        else
            editor.putString("Unit", Farenheit);

        editor.commit();
    }

    public static boolean isWeatherLoaded() throws IOException {
        if (locationsFile == null)
        {
            locationsFile = new File(appDataFolder, "locations.json");

            if (!locationsFile.exists() && !locationsFile.createNewFile())
                throw new IOException("Unable to create locations file");
        }

        if (locationsFile.length() == 0 || !locationsFile.exists())
            return false;

        if (!preferences.contains("weatherLoaded"))
        {
            return false;
        }
        else if (preferences.getBoolean("weatherLoaded", false))
        {
            return true;
        }
        else
            return false;
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
            String key = "";
            key = readAPIKEYfile();

            if (!key.equals("") || key != null)
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
        } catch (IOException e) {
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {}
        }

        return key;
    }

    public static void setAPIKEY(String key)
    {
        if (key != "" || key != null)
            editor.putString("API_KEY", key);

        editor.commit();
    }
}