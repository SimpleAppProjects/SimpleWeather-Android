package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.controls.ProviderEntry;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class WeatherAPI {
    // APIs
    public static final String YAHOO = "Yahoo";
    public static final String WEATHERUNDERGROUND = "WUnderground";
    public static final String OPENWEATHERMAP = "openweather";
    public static final String METNO = "Metno";
    public static final String HERE = "Here";
    public static final String LOCATIONIQ = "LocIQ";
    public static final String NWS = "NWS";

    public static List<ProviderEntry> APIs = new ArrayList<>(asList(
            new ProviderEntry("HERE Weather", HERE,
                    "https://www.here.com/en", "https://developer.here.com/?create=Freemium-Basic&keepState=true&step=account"),
            new ProviderEntry("Yahoo Weather", YAHOO,
                    "https://www.yahoo.com/weather?ilc=401", "https://www.yahoo.com/weather?ilc=401"),
            new ProviderEntry("MET Norway", METNO,
                    "https://www.met.no/en", "https://www.met.no/en"),
            new ProviderEntry("U.S. National Weather Service (NOAA - U.S. Only)", NWS,
                    "https://www.weather.gov", "https://www.weather.gov"),
            new ProviderEntry("OpenWeatherMap", OPENWEATHERMAP,
                    "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up")
    ));

    public static List<ProviderEntry> LocationAPIs = new ArrayList<>(asList(
            new ProviderEntry("HERE Maps", HERE,
                    "https://www.here.com/en", "https://developer.here.com/"),
            new ProviderEntry("LocationIQ", LOCATIONIQ,
                    "https://locationiq.com", "https://locationiq.com")
    ));
}
