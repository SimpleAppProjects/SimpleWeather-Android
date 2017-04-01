package com.thewizrd.simpleweather.weather.weatherunderground;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.thewizrd.simpleweather.utils.JSONParser;
import com.thewizrd.simpleweather.utils.WeatherException;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_Location;
import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_RESULT;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class AutoCompleteQuery extends AsyncTask<String, Void, List<AC_Location>>
{
    private String queryAPI = "http://autocomplete.wunderground.com/aq?query=";
    private String options = "&h=0&cities=1";

    private Context context;
    private WeatherException wEx = null;

    public AutoCompleteQuery(Context context) {
        this.context = context;
    }

    protected List<AC_Location> doInBackground(String... query)
    {
        return getLocations(query[0]);
    }

    @Override
    protected void onPostExecute(List<AC_Location> ac_locations) {
        if (wEx != null) {
            Toast.makeText(context, wEx.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private List<AC_Location> getLocations(String query) {
        List<AC_Location> locationResults = null;
        URLConnection client = null;

        try {
            URL queryURL = new URL(queryAPI + query + options);
            client = queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Read to buffer
            ByteArrayOutputStream buffStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = stream.read(buffer)) != -1) {
                buffStream.write(buffer, 0, length);
            }

            // Load data
            String response = buffStream.toString("UTF-8");
            locationResults = parseLocations(response);

            // Close
            buffStream.close();
            stream.close();
        } catch (UnknownHostException uHEx) {
            wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (locationResults == null)
                locationResults = new ArrayList<>();
        }

        return locationResults;
    }

    private ArrayList<AC_Location> parseLocations(String json)
    {
        int maxResults = 10;
        ArrayList<AC_Location> results = new ArrayList<>();

        try {
            JSONObject root = new JSONObject(json);
            JSONArray resultsArray = root.getJSONArray("RESULTS");

            for (int i = 0; i < resultsArray.length(); i++)
            {
                JSONObject result = resultsArray.getJSONObject(i);
                AC_RESULT ac_result = (AC_RESULT) JSONParser.deserializer(result.toString(), AC_RESULT.class);

                if (!ac_result.type.equals("city"))
                    continue;

                AC_Location location = new AC_Location(ac_result);
                results.add(location);

                // Limit amount of results
                maxResults--;
                if (maxResults <= 0)
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
}

