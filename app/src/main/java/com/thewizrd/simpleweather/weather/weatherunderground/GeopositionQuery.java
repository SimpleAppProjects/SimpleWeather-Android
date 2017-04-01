package com.thewizrd.simpleweather.weather.weatherunderground;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.thewizrd.simpleweather.utils.WeatherException;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_Location;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Locale;

public class GeopositionQuery extends AsyncTask<WeatherUtils.Coordinate, Void, AC_Location> {
    private static String queryAPI = "http://api.wunderground.com/auto/wui/geo/GeoLookupXML/index.xml?query=";
    private static String options = "";

    private Context context;
    private WeatherException wEx = null;

    public GeopositionQuery(Context context) {
        this.context = context;
    }

    protected AC_Location doInBackground(WeatherUtils.Coordinate... coordinates) {
        return getLocation(coordinates[0]);
    }

    @Override
    protected void onPostExecute(AC_Location ac_location) {
        if (wEx != null) {
            Toast.makeText(context, wEx.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private AC_Location getLocation(WeatherUtils.Coordinate coordinate) {
        AC_Location result = null;
        String query = String.format(Locale.getDefault(), "%f,%f", coordinate.getLatitude(), coordinate.getLongitude());
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
            result = parseXML(response);

            // Close
            buffStream.close();
            stream.close();
        } catch (UnknownHostException uknHEx) {
            wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private AC_Location parseXML(String xml)
    {
        AC_Location result = null;

        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new StringReader(xml));

            location location = null;
            int eventType = parser.getEventType();
            String tagName = null;
            String currentTag = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                tagName = parser.getName();

                if (eventType == XmlPullParser.START_TAG) {
                    if (tagName.equals("location")) {
                        // location tag check if city
                        String type = parser.getAttributeValue(parser.getNamespace(), "type");
                        if (type.equals("CITY")) {
                            location = new location();
                            location.type = type;
                        }
                    }
                    currentTag = tagName;
                } else if (eventType == XmlPullParser.TEXT) {
                    // We found some text. let's see the tagName to know the tag related to the text
                    if ("country".equals(currentTag)) {
                        location.country = parser.getText();
                    } else if ("state".equals(currentTag)) {
                        location.state = parser.getText();
                    } else if ("city".equals(currentTag)) {
                        location.city = parser.getText();
                    } else if ("lat".equals(currentTag)) {
                        location.lat = parser.getText();
                    } else if ("lon".equals(currentTag)) {
                        location.lon = parser.getText();
                    } else if ("zip".equals(currentTag)) {
                        location.zip = parser.getText();
                    } else if ("magic".equals(currentTag)) {
                        location.magic = parser.getText();
                    } else if ("wmo".equals(currentTag)) {
                        location.wmo = parser.getText();
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    // We don't want to analyze other tag at the moment
                    currentTag = null;
                    if ("wmo".equals(tagName)) {
                        // STOP here
                        result = new AC_Location(location);
                        break;
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public class location {
        public String type;
        public String country;
        public String state;
        public String city;
        public String lat;
        public String lon;
        public String zip;
        public String magic;
        public String wmo;
    }
}
