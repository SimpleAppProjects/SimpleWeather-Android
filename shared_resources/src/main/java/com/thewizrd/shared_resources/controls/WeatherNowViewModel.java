package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.TypefaceSpan;
import android.util.Log;

import androidx.core.util.ObjectsCompat;
import androidx.databinding.Bindable;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.BR;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.helpers.WeatherIconTextSpan;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class WeatherNowViewModel extends ObservableViewModel {
    private String location;
    private String updateDate;

    // Current Condition
    private CharSequence curTemp;
    private String curCondition;
    private String weatherIcon;
    private String hiTemp;
    private String loTemp;
    private CharSequence hiLoTemp;

    // Weather Details
    private SunPhaseViewModel sunPhase;
    private List<DetailItemViewModel> weatherDetails;
    private UVIndexViewModel uvIndex;
    private BeaufortViewModel beaufort;
    private MoonPhaseViewModel moonPhase;
    private AirQualityViewModel airQuality;

    // Radar
    private static final String radarUrlFormat = "https://earth.nullschool.net/#current/wind/surface/level/overlay=precip_3hr/orthographic=%s,%s,3000";
    private String radarURL;

    // Background
    private ImageDataViewModel imageData;
    private static final int DEFAULT_COLOR = Colors.SIMPLEBLUE;
    private int pendingBackground = -1;

    private String weatherCredit;
    private String weatherSource;

    private String weatherLocale;

    @Bindable
    public String getLocation() {
        return location;
    }

    @Bindable
    public String getUpdateDate() {
        return updateDate;
    }

    @Bindable
    public CharSequence getCurTemp() {
        return curTemp;
    }

    @Bindable
    public String getCurCondition() {
        return curCondition;
    }

    @Bindable
    public String getWeatherIcon() {
        return weatherIcon;
    }

    @Bindable
    public String getHiTemp() {
        return hiTemp;
    }

    @Bindable
    public String getLoTemp() {
        return loTemp;
    }

    @Bindable
    public CharSequence getHiLoTemp() {
        return hiLoTemp;
    }

    @Bindable
    public UVIndexViewModel getUvIndex() {
        return uvIndex;
    }

    @Bindable
    public BeaufortViewModel getBeaufort() {
        return beaufort;
    }

    @Bindable
    public MoonPhaseViewModel getMoonPhase() {
        return moonPhase;
    }

    @Bindable
    public AirQualityViewModel getAirQuality() {
        return airQuality;
    }

    @Bindable
    public List<DetailItemViewModel> getWeatherDetails() {
        return weatherDetails;
    }

    @Bindable
    public SunPhaseViewModel getSunPhase() {
        return sunPhase;
    }

    @Bindable
    public String getRadarURL() {
        return radarURL;
    }

    @Bindable
    public ImageDataViewModel getImageData() {
        return imageData;
    }

    @Bindable
    public int getPendingBackground() {
        return pendingBackground;
    }

    @Bindable
    public String getWeatherCredit() {
        return weatherCredit;
    }

    @Bindable
    public String getWeatherSource() {
        return weatherSource;
    }

    @Bindable
    public String getWeatherLocale() {
        return weatherLocale;
    }

    private Weather weather;
    private String tempUnit;

    public WeatherNowViewModel() {
        weatherDetails = new ArrayList<>(WeatherDetailsType.values().length);
    }

    public WeatherNowViewModel(Weather weather) {
        this();
        updateView(weather);
    }

    public void updateView(final Weather weather) {
        if (weather != null && weather.isValid()) {
            if (!ObjectsCompat.equals(this.weather, weather)) {
                final boolean isPhone = SimpleLibrary.getInstance().getApp().isPhone();
                this.weather = weather;

                new AsyncTask<Void>().await(new Callable<Void>() {
                    @Override
                    public Void call() {
                        // Update backgrounds
                        if (imageData != null) {
                            imageData = null;
                            notifyPropertyChanged(BR.imageData);
                        }
                        if (!isPhone) {
                            int pendingBGColor = WeatherUtils.getWeatherBackgroundColor(weather);
                            if (pendingBGColor != pendingBackground) {
                                pendingBackground = pendingBGColor;
                                notifyPropertyChanged(BR.pendingBackground);
                            }
                        } else {
                            if (pendingBackground != DEFAULT_COLOR) {
                                pendingBackground = DEFAULT_COLOR;
                                notifyPropertyChanged(BR.pendingBackground);
                            }
                        }

                        // Location
                        if (!ObjectsCompat.equals(location, weather.getLocation().getName())) {
                            location = weather.getLocation().getName();
                            notifyPropertyChanged(BR.location);
                        }

                        // Additional Details
                        if (!StringUtils.isNullOrWhitespace(weather.getLocation().getLatitude()) && !StringUtils.isNullOrWhitespace(weather.getLocation().getLongitude())) {
                            String newUrl = String.format(Locale.ROOT, radarUrlFormat, weather.getLocation().getLongitude(), weather.getLocation().getLatitude());
                            if (!ObjectsCompat.equals(radarURL, newUrl)) {
                                radarURL = newUrl;
                                notifyPropertyChanged(BR.radarURL);
                            }
                        } else {
                            radarURL = null;
                            notifyPropertyChanged(BR.radarURL);
                        }

                        // Additional Details
                        if (!ObjectsCompat.equals(weatherSource, weather.getSource())) {
                            weatherSource = weather.getSource();
                            notifyPropertyChanged(BR.weatherSource);
                        }

                        // Language
                        weatherLocale = weather.getLocale();
                        notifyPropertyChanged(BR.weatherLocale);

                        // Refresh locale/unit dependent values
                        refreshView();
                        return null;
                    }
                });
            } else if (!ObjectsCompat.equals(tempUnit, Settings.getTempUnit())) {
                new AsyncTask<Void>().await(new Callable<Void>() {
                    @Override
                    public Void call() {
                        refreshView();
                        return null;
                    }
                });
            }
        }
    }

    private void refreshView() {
        final Context context = SimpleLibrary.getInstance().getApp().getAppContext();
        final boolean isPhone = SimpleLibrary.getInstance().getApp().isPhone();

        tempUnit = Settings.getTempUnit();

        // Date Updated
        if (!ObjectsCompat.equals(updateDate, WeatherUtils.getLastBuildDate(weather))) {
            updateDate = WeatherUtils.getLastBuildDate(weather);
            notifyPropertyChanged(BR.updateDate);
        }

        // Update current condition
        SpannableStringBuilder curTempSSBuilder = new SpannableStringBuilder();
        if (weather.getCondition().getTempF() != weather.getCondition().getTempC()) {
            int temp = (int) (Settings.isFahrenheit() ? Math.round(weather.getCondition().getTempF()) : Math.round(weather.getCondition().getTempC()));
            curTempSSBuilder.append(Integer.toString(temp));
        } else {
            curTempSSBuilder.append("--");
        }
        String unitTemp = Settings.isFahrenheit() ? WeatherIcons.FAHRENHEIT : WeatherIcons.CELSIUS;
        curTempSSBuilder.append(unitTemp)
                .setSpan(new WeatherIconTextSpan(context), curTempSSBuilder.length() - unitTemp.length(), curTempSSBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (!ObjectsCompat.equals(curTemp, curTempSSBuilder)) {
            curTemp = curTempSSBuilder;
            notifyPropertyChanged(BR.curTemp);
        }
        String newCondition = (StringUtils.isNullOrWhitespace(weather.getCondition().getWeather()) ? "--" : weather.getCondition().getWeather());
        if (!ObjectsCompat.equals(curCondition, newCondition)) {
            curCondition = newCondition;
            notifyPropertyChanged(BR.curCondition);
        }
        if (!ObjectsCompat.equals(weatherIcon, weather.getCondition().getIcon())) {
            weatherIcon = weather.getCondition().getIcon();
            notifyPropertyChanged(BR.weatherIcon);
        }

        boolean hiTempChanged = false, loTempChanged = false;
        {
            String newHiTemp = (Settings.isFahrenheit() ? Math.round(weather.getCondition().getHighF()) : Math.round(weather.getCondition().getHighC())) + "°";
            String newLoTemp = (Settings.isFahrenheit() ? Math.round(weather.getCondition().getLowF()) : Math.round(weather.getCondition().getLowC())) + "°";
            if (!ObjectsCompat.equals(hiTemp, newHiTemp)) {
                if (weather.getCondition().getHighF() != weather.getCondition().getHighC()) {
                    hiTemp = newHiTemp;
                } else {
                    hiTemp = null;
                }
                notifyPropertyChanged(BR.hiTemp);
                hiTempChanged = true;
            }
            if (!ObjectsCompat.equals(loTemp, newLoTemp)) {
                if (weather.getCondition().getLowF() != weather.getCondition().getLowC()) {
                    loTemp = newLoTemp;
                } else {
                    loTemp = null;
                }
                notifyPropertyChanged(BR.loTemp);
                loTempChanged = true;
            }
        }

        if ((hiTemp != null || loTemp != null) && (hiTempChanged || loTempChanged)) {
            SpannableStringBuilder hiLoTempBuilder = new SpannableStringBuilder();

            hiLoTempBuilder.append(hiTemp != null ? hiTemp : "--")
                    .append(' ');

            int firstIdx = hiLoTempBuilder.length();

            hiLoTempBuilder.append("\uf058")
                    .append(" | ")
                    .append(loTemp != null ? loTemp : "--")
                    .append(' ')
                    .append("\uf044");

            hiLoTempBuilder.setSpan(new WeatherIconTextSpan(context), firstIdx, firstIdx + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            hiLoTempBuilder.setSpan(new WeatherIconTextSpan(context), hiLoTempBuilder.length() - 1, hiLoTempBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            hiLoTemp = hiLoTempBuilder;
            notifyPropertyChanged(BR.hiLoTemp);
        } else if (hiTemp == null && loTemp == null) {
            hiLoTemp = null;
            notifyPropertyChanged(BR.hiLoTemp);
        }

        // WeatherDetails
        weatherDetails.clear();
        // Precipitation
        if (weather.getPrecipitation() != null) {
            String chance = weather.getPrecipitation().getPop() + "%";
            String qpfRain = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfRainIn()) :
                    String.format(Locale.getDefault(), "%.2f mm", weather.getPrecipitation().getQpfRainMm());
            String qpfSnow = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfSnowIn()) :
                    String.format(Locale.getDefault(), "%.2f cm", weather.getPrecipitation().getQpfSnowCm());

            if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, chance));
            } else {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, chance));
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
            }
        }

        // Atmosphere
        if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getPressureMb())) {
            String pressureVal = Settings.isFahrenheit() ?
                    weather.getAtmosphere().getPressureIn() :
                    weather.getAtmosphere().getPressureMb();

            String pressureUnit = Settings.isFahrenheit() ? "in" : "mb";

            try {
                CharSequence pressureStateIcon = getPressureStateIcon(weather.getAtmosphere().getPressureTrend());

                SpannableStringBuilder ssBuilder = new SpannableStringBuilder();
                ssBuilder.append(pressureStateIcon)
                        .append(" ")
                        .append(pressureVal)
                        .append(" ")
                        .append(pressureUnit);

                if (pressureStateIcon.length() > 0) {
                    TypefaceSpan span = new WeatherIconTextSpan(context);
                    ssBuilder.setSpan(span, 0, pressureStateIcon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE, ssBuilder));
            } catch (Exception e) {
                Logger.writeLine(Log.DEBUG, e);
            }
        }

        if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getHumidity())) {
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                    weather.getAtmosphere().getHumidity().endsWith("%") ?
                            weather.getAtmosphere().getHumidity() :
                            weather.getAtmosphere().getHumidity() + "%"));
        }

        if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getDewpointF())) {
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                    Settings.isFahrenheit() ?
                            String.format(Locale.getDefault(), "%dº", Math.round(Float.parseFloat(weather.getAtmosphere().getDewpointF()))) :
                            String.format(Locale.getDefault(), "%dº", Math.round(Float.parseFloat(weather.getAtmosphere().getDewpointC())))));

        }

        if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getVisibilityMi())) {
            String visibilityVal = Settings.isFahrenheit() ?
                    weather.getAtmosphere().getVisibilityMi() :
                    weather.getAtmosphere().getVisibilityKm();

            String visibilityUnit = Settings.isFahrenheit() ? "mi" : "km";

            try {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                        String.format(Locale.getDefault(), "%s %s", visibilityVal, visibilityUnit)));
            } catch (Exception e) {
                Logger.writeLine(Log.DEBUG, e);
            }
        }

        if (weather.getCondition().getUv() != null) {
            if (isPhone) {
                uvIndex = new UVIndexViewModel(weather.getCondition().getUv());
            } else {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(Locale.ROOT, "%s, %s",
                                weather.getCondition().getUv().getIndex(), weather.getCondition().getUv().getDescription())));
            }
        } else {
            uvIndex = null;
        }
        notifyPropertyChanged(BR.uvIndex);

        // Additional Details
        if (weather.getCondition().getAirQuality() != null) {
            if (isPhone) {
                airQuality = new AirQualityViewModel(weather.getCondition().getAirQuality());
            } else {
                weatherDetails.add(new DetailItemViewModel(weather.getCondition().getAirQuality()));
            }
        } else {
            airQuality = null;
        }
        notifyPropertyChanged(BR.airQuality);

        if (weather.getCondition().getFeelslikeF() != weather.getCondition().getFeelslikeC()) {
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                    Settings.isFahrenheit() ?
                            String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getFeelslikeF())) :
                            String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getFeelslikeC()))));
        }

        // Wind
        if (weather.getCondition().getWindMph() != weather.getCondition().getWindKph()) {
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                    Settings.isFahrenheit() ?
                            String.format(Locale.getDefault(), "%d mph, %s", Math.round(weather.getCondition().getWindMph()), WeatherUtils.getWindDirection(weather.getCondition().getWindDegrees())) :
                            String.format(Locale.getDefault(), "%d kph, %s", Math.round(weather.getCondition().getWindKph()), WeatherUtils.getWindDirection(weather.getCondition().getWindDegrees())),
                    weather.getCondition().getWindDegrees()));
        }

        if (weather.getCondition().getBeaufort() != null) {
            if (isPhone) {
                beaufort = new BeaufortViewModel(weather.getCondition().getBeaufort());
            } else {
                weatherDetails.add(new DetailItemViewModel(weather.getCondition().getBeaufort().getScale(),
                        weather.getCondition().getBeaufort().getDescription()));
            }
        } else {
            beaufort = null;
        }
        notifyPropertyChanged(BR.beaufort);

        // Astronomy
        if (weather.getAstronomy() != null) {
            sunPhase = new SunPhaseViewModel(weather.getAstronomy());

            if (!isPhone) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.SUNRISE, sunPhase.getSunrise()));
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.SUNSET, sunPhase.getSunset()));
            }

            if (weather.getAstronomy().getMoonrise() != null && weather.getAstronomy().getMoonset() != null
                    && weather.getAstronomy().getMoonrise().compareTo(DateTimeUtils.getLocalDateTimeMIN()) > 0
                    && weather.getAstronomy().getMoonset().compareTo(DateTimeUtils.getLocalDateTimeMIN()) > 0) {
                if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONRISE,
                            weather.getAstronomy().getMoonrise().format(DateTimeFormatter.ofPattern("HH:mm"))));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONSET,
                            weather.getAstronomy().getMoonset().format(DateTimeFormatter.ofPattern("HH:mm"))));
                } else {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONRISE,
                            weather.getAstronomy().getMoonrise().format(DateTimeFormatter.ofPattern("h:mm a"))));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONSET,
                            weather.getAstronomy().getMoonset().format(DateTimeFormatter.ofPattern("h:mm a"))));
                }
            }

            if (weather.getAstronomy().getMoonPhase() != null) {
                if (isPhone) {
                    moonPhase = new MoonPhaseViewModel(weather.getAstronomy().getMoonPhase());
                } else {
                    weatherDetails.add(new DetailItemViewModel(weather.getAstronomy().getMoonPhase().getPhase(),
                            weather.getAstronomy().getMoonPhase().getDescription()));
                }
            } else {
                moonPhase = null;
            }
        } else {
            sunPhase = null;
            moonPhase = null;
        }
        notifyPropertyChanged(BR.sunPhase);
        notifyPropertyChanged(BR.moonPhase);
        notifyPropertyChanged(BR.weatherDetails);

        String creditPrefix = context.getString(R.string.credit_prefix);
        if (WeatherAPI.YAHOO.equals(weather.getSource()))
            weatherCredit = String.format("%s Yahoo!", creditPrefix);
        else if (WeatherAPI.OPENWEATHERMAP.equals(weather.getSource()))
            weatherCredit = String.format("%s OpenWeatherMap", creditPrefix);
        else if (WeatherAPI.METNO.equals(weather.getSource()))
            weatherCredit = String.format("%s MET Norway", creditPrefix);
        else if (WeatherAPI.HERE.equals(weather.getSource()))
            weatherCredit = String.format("%s HERE Weather", creditPrefix);
        else if (WeatherAPI.NWS.equals(weather.getSource()))
            weatherCredit = String.format("%s U.S. National Weather Service", creditPrefix);
        notifyPropertyChanged(BR.weatherCredit);
    }

    public void reset() {
        location = null;
        updateDate = null;
        curTemp = null;
        curCondition = null;
        weatherIcon = null;
        sunPhase = null;
        weatherDetails.clear();
        imageData = null;
        pendingBackground = -1;
        weatherCredit = null;
        weatherSource = null;
        weatherLocale = null;

        weather = null;
        notifyChange();
    }

    public void updateBackground() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                ImageDataViewModel imageVM = WeatherUtils.getImageData(weather);

                if (imageVM != null) {
                    imageData = imageVM;
                    pendingBackground = imageVM.getColor();
                } else {
                    imageData = null;
                    pendingBackground = DEFAULT_COLOR;
                }

                notifyPropertyChanged(BR.imageData);
                notifyPropertyChanged(BR.pendingBackground);

                return null;
            }
        });
    }

    public boolean isValid() {
        return weather != null && weather.isValid();
    }

    private String getPressureStateIcon(String state) {
        switch (state) {
            // Steady
            case "0":
            default:
                return "";
            // Rising
            case "1":
            case "+":
            case "Rising":
                return "\uf058\uf058";
            // Falling
            case "2":
            case "-":
            case "Falling":
                return "\uf044\uf044";
        }
    }
}
