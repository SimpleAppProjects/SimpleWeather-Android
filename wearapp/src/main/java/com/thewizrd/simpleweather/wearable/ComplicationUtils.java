package com.thewizrd.simpleweather.wearable;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.reflect.TypeToken;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.App;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComplicationUtils {
    // Shared Settings
    private static SharedPreferences complicationPrefs = App.getInstance().getAppContext().getSharedPreferences("complications", Context.MODE_PRIVATE);
    private static SharedPreferences.Editor editor = complicationPrefs.edit();

    // Keys
    private static final String KEY_IDS = "ids";

    private static List<Integer> getComplicationIds() {
        String listJson = complicationPrefs.getString(KEY_IDS, "");
        if (!StringUtils.isNullOrWhitespace(listJson)) {
            Type intArrListType = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
            if (idList != null) {
                return idList;
            }
        }

        return new ArrayList<>(0);
    }

    public static void addComplicationId(int complicationId) {
        String listJson = complicationPrefs.getString(KEY_IDS, "");
        if (StringUtils.isNullOrWhitespace(listJson)) {
            ArrayList<Integer> newlist = new ArrayList<>(Collections.singletonList(complicationId));
            saveIds(newlist);
        } else {
            Type intArrListType = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
            if (idList != null && !idList.contains(complicationId)) {
                idList.add(complicationId);
                saveIds(idList);
            }
        }
    }

    public static void removeComplicationId(int complicationId) {
        String listJson = complicationPrefs.getString(KEY_IDS, "");
        if (!StringUtils.isNullOrWhitespace(listJson)) {
            Type intArrListType = new TypeToken<ArrayList<Integer>>() {
            }.getType();
            ArrayList<Integer> idList = JSONParser.deserializer(listJson, intArrListType);
            if (idList != null) {
                idList.remove(Integer.valueOf(complicationId));

                if (idList.size() == 0)
                    editor.remove(KEY_IDS).commit();
                else
                    saveIds(idList);
            }
        }
    }

    public static boolean complicationsExist() {
        String listJson = complicationPrefs.getString(KEY_IDS, "");
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

    private static void saveIds(ArrayList<Integer> idList) {
        String json = JSONParser.serializer(idList, ArrayList.class);
        editor.putString(KEY_IDS, json).commit();
    }
}
