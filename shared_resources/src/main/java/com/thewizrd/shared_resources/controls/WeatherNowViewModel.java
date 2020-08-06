package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.TypefaceSpan;
import android.util.Log;

import androidx.annotation.WorkerThread;
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
    private CharSequence hiTemp;
    private CharSequence loTemp;
    private CharSequence hiLoTemp;
    private boolean showHiLo;

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
    public CharSequence getHiTemp() {
        return hiTemp;
    }

    @Bindable
    public CharSequence getLoTemp() {
        return loTemp;
    }

    @Bindable
    public CharSequence getHiLoTemp() {
        return hiLoTemp;
    }

    @Bindable
    public boolean isShowHiLo() {
        return showHiLo;
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

    @Bindable
    public String getTempUnit() {
        return tempUnit;
    }

    public String getQuery() {
        if (weather != null) {
            return weather.getQuery();
        } else {
            return null;
        }
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
                        if (weather.getLocation().getLatitude() != null && weather.getLocation().getLongitude() != null) {
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
        notifyPropertyChanged(BR.tempUnit);

        // Date Updated
        if (!ObjectsCompat.equals(updateDate, WeatherUtils.getLastBuildDate(weather))) {
            updateDate = WeatherUtils.getLastBuildDate(weather);
            notifyPropertyChanged(BR.updateDate);
        }

        // Update current condition
        SpannableStringBuilder curTempSSBuilder = new SpannableStringBuilder();
        if (weather.getCondition().getTempF() != null &&
                !ObjectsCompat.equals(weather.getCondition().getTempF(), weather.getCondition().getTempC())) {
            int temp = Settings.isFahrenheit() ? Math.round(weather.getCondition().getTempF()) : Math.round(weather.getCondition().getTempC());
            curTempSSBuilder.append(String.format(Locale.getDefault(), "%d", temp));
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

        {
            boolean shouldHideHi = false, shouldHideLo = false;
            SpannableStringBuilder hiTempBuilder = new SpannableStringBuilder();
            if (weather.getCondition().getHighF() != null &&
                    !ObjectsCompat.equals(weather.getCondition().getHighF(), weather.getCondition().getHighC())) {
                hiTempBuilder.append(String.valueOf(Settings.isFahrenheit() ? Math.round(weather.getCondition().getHighF()) : Math.round(weather.getCondition().getHighC())))
                        .append("°");
            } else {
                hiTempBuilder.append("--°");
                shouldHideHi = true;
            }

            int idx = hiTempBuilder.length();

            hiTempBuilder.append(" \uf058");
            hiTempBuilder.setSpan(new WeatherIconTextSpan(context), idx, idx + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            hiTemp = hiTempBuilder;
            notifyPropertyChanged(BR.hiTemp);

            SpannableStringBuilder loTempBuilder = new SpannableStringBuilder();
            if (weather.getCondition().getLowF() != null &&
                    !ObjectsCompat.equals(weather.getCondition().getLowF(), weather.getCondition().getLowC())) {
                loTempBuilder.append(String.valueOf(Settings.isFahrenheit() ? Math.round(weather.getCondition().getLowF()) : Math.round(weather.getCondition().getLowC())))
                        .append("°");
            } else {
                loTempBuilder.append("--°");
                shouldHideLo = true;
            }

            idx = loTempBuilder.length();

            loTempBuilder.append(" \uf044");
            loTempBuilder.setSpan(new WeatherIconTextSpan(context), idx, idx + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            loTemp = loTempBuilder;
            notifyPropertyChanged(BR.loTemp);

            showHiLo = !shouldHideHi && !shouldHideLo;
            notifyPropertyChanged(BR.showHiLo);
        }

        hiLoTemp = new SpannableStringBuilder().append(hiTemp).append("  |  ").append(loTemp);
        notifyPropertyChanged(BR.hiLoTemp);

        // WeatherDetails
        weatherDetails.clear();
        // Precipitation
        if (weather.getPrecipitation() != null) {
            String chance = weather.getPrecipitation().getPop() != null ? weather.getPrecipitation().getPop() + "%" : null;

            if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                if (weather.getPrecipitation().getQpfRainIn() != null && weather.getPrecipitation().getQpfRainIn() >= 0) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN,
                            Settings.isFahrenheit() ?
                                    String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfRainIn()) :
                                    String.format(Locale.getDefault(), "%.2f mm", weather.getPrecipitation().getQpfRainMm())));
                }
                if (weather.getPrecipitation().getQpfSnowIn() != null && weather.getPrecipitation().getQpfSnowIn() >= 0) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                            Settings.isFahrenheit() ?
                                    String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfSnowIn()) :
                                    String.format(Locale.getDefault(), "%.2f cm", weather.getPrecipitation().getQpfSnowCm())));
                }
                if (weather.getPrecipitation().getPop() != null && weather.getPrecipitation().getPop() >= 0) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, chance));
                }
            } else {
                if (weather.getPrecipitation().getPop() != null && weather.getPrecipitation().getPop() >= 0) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, chance));
                }
                if (weather.getPrecipitation().getQpfRainIn() != null && weather.getPrecipitation().getQpfRainIn() >= 0) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN,
                            Settings.isFahrenheit() ?
                                    String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfRainIn()) :
                                    String.format(Locale.getDefault(), "%.2f mm", weather.getPrecipitation().getQpfRainMm())));
                }
                if (weather.getPrecipitation().getQpfSnowIn() != null && weather.getPrecipitation().getQpfSnowIn() >= 0) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                            Settings.isFahrenheit() ?
                                    String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfSnowIn()) :
                                    String.format(Locale.getDefault(), "%.2f cm", weather.getPrecipitation().getQpfSnowCm())));
                }
            }
        }

        // Atmosphere
        if (weather.getAtmosphere().getPressureMb() != null) {
            float pressureVal = Settings.isFahrenheit() ? weather.getAtmosphere().getPressureIn() : weather.getAtmosphere().getPressureMb();
            String pressureUnit = Settings.isFahrenheit() ? "in" : "mb";

            try {
                CharSequence pressureStateIcon = getPressureStateIcon(weather.getAtmosphere().getPressureTrend());

                SpannableStringBuilder ssBuilder = new SpannableStringBuilder();
                ssBuilder.append(pressureStateIcon)
                        .append(" ")
                        .append(String.format(Locale.getDefault(), "%.2f %s", pressureVal, pressureUnit));

                if (pressureStateIcon.length() > 0) {
                    TypefaceSpan span = new WeatherIconTextSpan(context);
                    ssBuilder.setSpan(span, 0, pressureStateIcon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE, ssBuilder));
            } catch (Exception e) {
                Logger.writeLine(Log.DEBUG, e);
            }
        }

        if (weather.getAtmosphere().getHumidity() != null) {
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                    String.format(Locale.getDefault(), "%d%%", weather.getAtmosphere().getHumidity())));
        }

        if (weather.getAtmosphere().getDewpointF() != null && !ObjectsCompat.equals(weather.getAtmosphere().getDewpointF(), weather.getAtmosphere().getDewpointC())) {
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                    String.format(Locale.getDefault(), "%d°",
                            Settings.isFahrenheit() ?
                                    Math.round(weather.getAtmosphere().getDewpointF()) :
                                    Math.round(weather.getAtmosphere().getDewpointC())
                    )));
        }

        if (weather.getAtmosphere().getVisibilityMi() != null && weather.getAtmosphere().getVisibilityMi() >= 0) {
            float visibilityVal = Settings.isFahrenheit() ? weather.getAtmosphere().getVisibilityMi() : weather.getAtmosphere().getVisibilityKm();
            String visibilityUnit = Settings.isFahrenheit() ? "mi" : "km";

            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                    String.format(Locale.getDefault(), "%.2f %s", visibilityVal, visibilityUnit)));
        }

        if (weather.getCondition().getUv() != null) {
            if (isPhone) {
                uvIndex = new UVIndexViewModel(weather.getCondition().getUv());
            } else {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(Locale.getDefault(), "%.1f, %s",
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

        if (weather.getCondition().getFeelslikeF() != null &&
                !ObjectsCompat.equals(weather.getCondition().getFeelslikeF(), weather.getCondition().getFeelslikeC())) {
            int value = Settings.isFahrenheit() ? Math.round(weather.getCondition().getFeelslikeF()) : Math.round(weather.getCondition().getFeelslikeC());

            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                    String.format(Locale.getDefault(), "%d°", value)));
        }

        // Wind
        if (weather.getCondition().getWindMph() != null &&
                !ObjectsCompat.equals(weather.getCondition().getWindMph(), weather.getCondition().getWindKph())) {
            int speedVal = Settings.isFahrenheit() ? Math.round(weather.getCondition().getWindMph()) : Math.round(weather.getCondition().getWindKph());
            String speedUnit = Settings.isFahrenheit() ? "mph" : "kph";

            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                    String.format(Locale.getDefault(), "%d %s, %s", speedVal, speedUnit, WeatherUtils.getWindDirection(weather.getCondition().getWindDegrees())),
                    weather.getCondition().getWindDegrees() + 180));
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
                    && weather.getAstronomy().getMoonrise().isAfter(DateTimeUtils.getLocalDateTimeMIN())
                    && weather.getAstronomy().getMoonset().isAfter(DateTimeUtils.getLocalDateTimeMIN())) {
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

    @WorkerThread
    public void updateBackground() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                if (weather != null) {
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
                }

                return null;
            }
        });
    }

    public boolean isValid() {
        return weather != null && weather.isValid();
    }

    private String getPressureStateIcon(String state) {
        if (state == null) state = "";

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
