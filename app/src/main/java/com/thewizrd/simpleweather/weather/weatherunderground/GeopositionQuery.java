package com.thewizrd.simpleweather.weather.weatherunderground;

import android.location.Location;
import android.os.AsyncTask;

import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_Location;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeopositionQuery extends AsyncTask<Location, Void, List<AC_Location>> {
    private static String queryAPI = "http://api.wunderground.com/auto/wui/geo/GeoLookupXML/index.xml?query=";
    private static String options = "";

    protected List<AC_Location> doInBackground(Location... location)
    {
        return getLocation(location[0]);
    }

    private List<AC_Location> getLocation(Location location)
    {
        List<AC_Location> locationResults = null;
        String query = String.format(Locale.getDefault(), "%f,%f", location.getLatitude(), location.getLongitude());

        try {
            URL queryURL = new URL(queryAPI + query + options);
            URLConnection client = queryURL.openConnection();
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
            locationResults = parseXML(response);

            // Close
            buffStream.close();
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (locationResults == null)
                locationResults = new ArrayList<>();
        }

        return locationResults;
    }

    private ArrayList<AC_Location> parseXML(String xml)
    {
        ArrayList<AC_Location> results = new ArrayList<>();

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
                        results.add(new AC_Location(location));
                        break;
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
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
