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

import com.thewizrd.shared_resources.BR;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.helpers.WeatherIconTextSpan;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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
    private final List<DetailItemViewModel> weatherDetails;
    private UVIndexViewModel uvIndex;
    private BeaufortViewModel beaufort;
    private MoonPhaseViewModel moonPhase;
    private AirQualityViewModel airQuality;

    // Radar
    private final WeatherUtils.Coordinate locationCoord;

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
    public WeatherUtils.Coordinate getLocationCoord() {
        return locationCoord;
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
    @Units.TemperatureUnits
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
    private String unitCode;
    private String localeCode;

    public WeatherNowViewModel() {
        weatherDetails = new ArrayList<>(WeatherDetailsType.values().length);
        locationCoord = new WeatherUtils.Coordinate(0, 0);
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
                    locationCoord.setCoordinate(weather.getLocation().getLatitude(), weather.getLocation().getLongitude());
                } else {
                    locationCoord.setCoordinate(0, 0);
                }
                notifyPropertyChanged(BR.locationCoord);

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
            } else if (!ObjectsCompat.equals(unitCode, Settings.getUnitString()) || !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode())) {
                refreshView();
            }
        }
    }

    private void refreshView() {
        final Context context = SimpleLibrary.getInstance().getApp().getAppContext();
        final boolean isPhone = SimpleLibrary.getInstance().getApp().isPhone();
        final WeatherProviderImpl provider = WeatherManager.getProvider(weather.getSource());
        final boolean isFahrenheit = Units.FAHRENHEIT.equals(Settings.getTemperatureUnit());

        final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
        df.applyPattern("0.##");

        tempUnit = Settings.getTemperatureUnit();
        unitCode = Settings.getUnitString();
        notifyPropertyChanged(BR.tempUnit);

        localeCode = LocaleUtils.getLocaleCode();

        // Date Updated
        if (!ObjectsCompat.equals(updateDate, WeatherUtils.getLastBuildDate(weather))) {
            updateDate = WeatherUtils.getLastBuildDate(weather);
            notifyPropertyChanged(BR.updateDate);
        }

        // Update current condition
        SpannableStringBuilder curTempSSBuilder = new SpannableStringBuilder();
        if (weather.getCondition().getTempF() != null &&
                !ObjectsCompat.equals(weather.getCondition().getTempF(), weather.getCondition().getTempC())) {
            int temp = isFahrenheit ? Math.round(weather.getCondition().getTempF()) : Math.round(weather.getCondition().getTempC());
            curTempSSBuilder.append(String.format(LocaleUtils.getLocale(), "%d", temp));
        } else {
            curTempSSBuilder.append("--");
        }
        String unitTemp = isFahrenheit ? WeatherIcons.FAHRENHEIT : WeatherIcons.CELSIUS;
        curTempSSBuilder.append(unitTemp)
                .setSpan(new WeatherIconTextSpan(context), curTempSSBuilder.length() - unitTemp.length(), curTempSSBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (!ObjectsCompat.equals(curTemp, curTempSSBuilder)) {
            curTemp = curTempSSBuilder;
            notifyPropertyChanged(BR.curTemp);
        }
        final String weatherCondition = provider.supportsWeatherLocale() ? weather.getCondition().getWeather() : provider.getWeatherCondition(weather.getCondition().getIcon());
        String newCondition = (StringUtils.isNullOrWhitespace(weatherCondition) ? "--" : weatherCondition);
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
                hiTempBuilder.append(String.valueOf(isFahrenheit ? Math.round(weather.getCondition().getHighF()) : Math.round(weather.getCondition().getHighC())))
                        .append("°");
            } else {
                hiTempBuilder.append("--");
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
                loTempBuilder.append(String.valueOf(isFahrenheit ? Math.round(weather.getCondition().getLowF()) : Math.round(weather.getCondition().getLowC())))
                        .append("°");
            } else {
                loTempBuilder.append("--");
                shouldHideLo = true;
            }

            idx = loTempBuilder.length();

            loTempBuilder.append(" \uf044");
            loTempBuilder.setSpan(new WeatherIconTextSpan(context), idx, idx + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            loTemp = loTempBuilder;
            notifyPropertyChanged(BR.loTemp);

            showHiLo = !shouldHideHi || !shouldHideLo;
            notifyPropertyChanged(BR.showHiLo);
        }

        hiLoTemp = new SpannableStringBuilder().append(hiTemp).append("  |  ").append(loTemp);
        notifyPropertyChanged(BR.hiLoTemp);

        // WeatherDetails
        weatherDetails.clear();
        // Precipitation
        if (weather.getPrecipitation() != null) {
            if (weather.getPrecipitation().getPop() != null && weather.getPrecipitation().getPop() >= 0) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, weather.getPrecipitation().getPop() + "%"));
            }
            if (weather.getPrecipitation().getQpfRainIn() != null && weather.getPrecipitation().getQpfRainIn() >= 0) {
                final String unit = Settings.getPrecipitationUnit();
                float precipValue;
                String precipUnit;

                switch (unit) {
                    case Units.INCHES:
                    default:
                        precipValue = weather.getPrecipitation().getQpfRainIn();
                        precipUnit = context.getString(R.string.unit_in);
                        break;
                    case Units.MILLIMETERS:
                        precipValue = weather.getPrecipitation().getQpfRainMm();
                        precipUnit = context.getString(R.string.unit_mm);
                        break;
                }

                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN,
                                String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit)
                        )
                );
            }
            if (weather.getPrecipitation().getQpfSnowIn() != null && weather.getPrecipitation().getQpfSnowIn() >= 0) {
                final String unit = Settings.getPrecipitationUnit();
                float precipValue;
                String precipUnit;

                switch (unit) {
                    case Units.INCHES:
                    default:
                        precipValue = weather.getPrecipitation().getQpfSnowIn();
                        precipUnit = context.getString(R.string.unit_in);
                        break;
                    case Units.MILLIMETERS:
                        precipValue = weather.getPrecipitation().getQpfSnowCm() * 10;
                        precipUnit = context.getString(R.string.unit_mm);
                        break;
                }

                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                                String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit)
                        )
                );
            }
            if (weather.getPrecipitation().getCloudiness() != null && weather.getPrecipitation().getCloudiness() >= 0) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, weather.getPrecipitation().getCloudiness() + "%"));
            }
        }

        // Atmosphere
        if (weather.getAtmosphere().getPressureMb() != null) {
            final String unit = Settings.getPressureUnit();
            float pressureVal;
            String pressureUnit;

            switch (unit) {
                case Units.INHG:
                default:
                    pressureVal = weather.getAtmosphere().getPressureIn();
                    pressureUnit = context.getString(R.string.unit_inHg);
                    break;
                case Units.MILLIBAR:
                    pressureVal = weather.getAtmosphere().getPressureMb();
                    pressureUnit = context.getString(R.string.unit_mBar);
                    break;
            }

            try {
                CharSequence pressureStateIcon = getPressureStateIcon(weather.getAtmosphere().getPressureTrend());

                SpannableStringBuilder ssBuilder = new SpannableStringBuilder();
                ssBuilder.append(pressureStateIcon)
                        .append(" ")
                        .append(String.format(LocaleUtils.getLocale(), "%s %s", df.format(pressureVal), pressureUnit));

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
                    String.format(LocaleUtils.getLocale(), "%d%%", weather.getAtmosphere().getHumidity())));
        }

        if (weather.getAtmosphere().getDewpointF() != null && !ObjectsCompat.equals(weather.getAtmosphere().getDewpointF(), weather.getAtmosphere().getDewpointC())) {
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                    String.format(LocaleUtils.getLocale(), "%d°",
                            isFahrenheit ?
                                    Math.round(weather.getAtmosphere().getDewpointF()) :
                                    Math.round(weather.getAtmosphere().getDewpointC())
                    )));
        }

        if (weather.getAtmosphere().getVisibilityMi() != null && weather.getAtmosphere().getVisibilityMi() >= 0) {
            final String unit = Settings.getDistanceUnit();
            int visibilityVal;
            String visibilityUnit;

            switch (unit) {
                case Units.MILES:
                default:
                    visibilityVal = Math.round(weather.getAtmosphere().getVisibilityMi());
                    visibilityUnit = context.getString(R.string.unit_miles);
                    break;
                case Units.KILOMETERS:
                    visibilityVal = Math.round(weather.getAtmosphere().getVisibilityKm());
                    visibilityUnit = context.getString(R.string.unit_kilometers);
                    break;
            }

            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                    String.format(LocaleUtils.getLocale(), "%d %s", visibilityVal, visibilityUnit)));
        }

        if (weather.getCondition().getUv() != null) {
            if (isPhone) {
                uvIndex = new UVIndexViewModel(weather.getCondition().getUv());
            } else {
                weatherDetails.add(new DetailItemViewModel(weather.getCondition().getUv()));
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
            int value = isFahrenheit ? Math.round(weather.getCondition().getFeelslikeF()) : Math.round(weather.getCondition().getFeelslikeC());

            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                    String.format(LocaleUtils.getLocale(), "%d°", value)));
        }

        // Wind
        if (weather.getCondition().getWindMph() != null &&
                !ObjectsCompat.equals(weather.getCondition().getWindMph(), weather.getCondition().getWindKph())) {
            final String unit = Settings.getSpeedUnit();
            int speedVal;
            String speedUnit;

            switch (unit) {
                case Units.MILES_PER_HOUR:
                default:
                    speedVal = Math.round(weather.getCondition().getWindMph());
                    speedUnit = context.getString(R.string.unit_mph);
                    break;
                case Units.KILOMETERS_PER_HOUR:
                    speedVal = Math.round(weather.getCondition().getWindKph());
                    speedUnit = context.getString(R.string.unit_kph);
                    break;
                case Units.METERS_PER_SECOND:
                    speedVal = Math.round(ConversionMethods.kphToMsec(weather.getCondition().getWindKph()));
                    speedUnit = context.getString(R.string.unit_msec);
                    break;
            }

            if (weather.getCondition().getWindDegrees() != null) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(LocaleUtils.getLocale(), "%d %s, %s", speedVal, speedUnit, WeatherUtils.getWindDirection(weather.getCondition().getWindDegrees())),
                        weather.getCondition().getWindDegrees() + 180));
            } else {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit), 180));
            }
        }

        if (weather.getCondition().getWindGustMph() != null && weather.getCondition().getWindGustKph() != null &&
                !ObjectsCompat.equals(weather.getCondition().getWindGustMph(), weather.getCondition().getWindGustKph())) {
            final String unit = Settings.getSpeedUnit();
            int speedVal;
            String speedUnit;

            switch (unit) {
                case Units.MILES_PER_HOUR:
                default:
                    speedVal = Math.round(weather.getCondition().getWindGustMph());
                    speedUnit = context.getString(R.string.unit_mph);
                    break;
                case Units.KILOMETERS_PER_HOUR:
                    speedVal = Math.round(weather.getCondition().getWindGustKph());
                    speedUnit = context.getString(R.string.unit_kph);
                    break;
                case Units.METERS_PER_SECOND:
                    speedVal = Math.round(ConversionMethods.kphToMsec(weather.getCondition().getWindGustKph()));
                    speedUnit = context.getString(R.string.unit_msec);
                    break;
            }

            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.WINDGUST,
                            String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit)
                    )
            );
        }

        if (weather.getCondition().getBeaufort() != null) {
            if (isPhone) {
                beaufort = new BeaufortViewModel(weather.getCondition().getBeaufort());
            } else {
                weatherDetails.add(new DetailItemViewModel(weather.getCondition().getBeaufort().getScale()));
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
                            weather.getAstronomy().getMoonrise().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR))));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONSET,
                            weather.getAstronomy().getMoonset().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR))));
                } else {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONRISE,
                            weather.getAstronomy().getMoonrise().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM))));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONSET,
                            weather.getAstronomy().getMoonset().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM))));
                }
            }

            if (weather.getAstronomy().getMoonPhase() != null) {
                if (isPhone) {
                    moonPhase = new MoonPhaseViewModel(weather.getAstronomy().getMoonPhase());
                } else {
                    weatherDetails.add(new DetailItemViewModel(weather.getAstronomy().getMoonPhase().getPhase()));
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

    @Override
    protected void onCleared() {
        super.onCleared();
        reset();
    }

    @WorkerThread
    public void updateBackground() {
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
