package com.thewizrd.simpleweather.controls.viewmodels;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.weatherdata.model.BaseForecast;
import com.thewizrd.shared_resources.weatherdata.model.Forecast;
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.graphs.LineDataSeries;
import com.thewizrd.simpleweather.controls.graphs.XLabelData;
import com.thewizrd.simpleweather.controls.graphs.YEntryData;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForecastGraphViewModel extends ViewModel {
    public enum ForecastGraphType {
        //TEMPERATURE,
        PRECIPITATION,
        WIND,
        HUMIDITY,
        UVINDEX,
        RAIN,
        SNOW
    }

    private final SettingsManager settingsMgr = App.getInstance().getSettingsManager();

    private List<XLabelData> labelData;
    private List<LineDataSeries> seriesData;
    private String graphLabel;
    private ForecastGraphType graphType;

    public List<XLabelData> getLabelData() {
        return labelData;
    }

    public List<LineDataSeries> getSeriesData() {
        return seriesData;
    }

    public String getGraphLabel() {
        return graphLabel;
    }

    public ForecastGraphType getGraphType() {
        return graphType;
    }

    public void addForecastData(BaseForecast forecast, ForecastGraphType graphType) {
        if (labelData == null) {
            labelData = new ArrayList<>();
        }

        if (seriesData == null) {
            List<YEntryData> yEntryData = new ArrayList<>();
            addEntryData(forecast, labelData, yEntryData, graphType);
            seriesData = createSeriesData(yEntryData, graphType);
        } else {
            addEntryData(forecast, labelData, seriesData.get(0).getSeriesData(), graphType);
        }
    }

    public void setForecastData(List<? extends BaseForecast> forecasts, ForecastGraphType graphType) {
        List<XLabelData> xData = new ArrayList<>(forecasts.size());
        List<YEntryData> yData = new ArrayList<>(forecasts.size());

        for (BaseForecast forecast : forecasts) {
            addEntryData(forecast, xData, yData, graphType);
        }

        labelData = xData;
        seriesData = createSeriesData(yData, graphType);
        this.graphType = graphType;
    }

    private void addEntryData(BaseForecast forecast, List<XLabelData> xData, List<YEntryData> yData, ForecastGraphType graphType) {
        Context context = App.getInstance().getAppContext();
        final boolean isFahrenheit = Units.FAHRENHEIT.equals(settingsMgr.getTemperatureUnit());

        final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
        df.applyPattern("0.##");

        String date;
        if (forecast instanceof Forecast) {
            Forecast fcast = (Forecast) forecast;
            date = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale(context.getString(R.string.forecast_date_format)));
        } else if (forecast instanceof HourlyForecast) {
            HourlyForecast fcast = (HourlyForecast) forecast;

            if (DateFormat.is24HourFormat(context)) {
                String skeleton;
                if (ContextUtils.isLargeTablet(context)) {
                    skeleton = DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR;
                } else {
                    skeleton = DateTimeConstants.SKELETON_24HR;
                }
                date = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(skeleton)));
            } else {
                String pattern;
                if (ContextUtils.isLargeTablet(context)) {
                    pattern = DateTimeConstants.ABBREV_DAYOFWEEK_AND_12HR_AMPM;
                } else {
                    pattern = DateTimeConstants.ABBREV_12HR_AMPM;
                }
                date = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale(pattern));
            }
        } else {
            date = "";
        }

        xData.add(new XLabelData(date));

        switch (graphType) {
            /*
            case TEMPERATURE:
                if (forecast.getHighF() != null && forecast.getHighC() != null) {
                    int value = isFahrenheit ? Math.round(forecast.getHighF()) : Math.round(forecast.getHighC());
                    String hiTemp = String.format(LocaleUtils.getLocale(), "%d°", value);
                    yData.add(new YEntryData(value, hiTemp));
                }
                break;
             */
            default:
            case PRECIPITATION:
                if (forecast.getExtras().getPop() != null && forecast.getExtras().getPop() >= 0) {
                    yData.add(new YEntryData(forecast.getExtras().getPop(), forecast.getExtras().getPop() + "%"));
                }
                break;
            case WIND:
                if (forecast.getExtras() != null &&
                        forecast.getExtras().getWindMph() != null && forecast.getExtras().getWindKph() != null && forecast.getExtras().getWindMph() >= 0) {
                    final String unit = settingsMgr.getSpeedUnit();
                    int speedVal;
                    String speedUnit;

                    switch (unit) {
                        case Units.MILES_PER_HOUR:
                        default:
                            speedVal = Math.round(forecast.getExtras().getWindMph());
                            speedUnit = context.getString(com.thewizrd.shared_resources.R.string.unit_mph);
                            break;
                        case Units.KILOMETERS_PER_HOUR:
                            speedVal = Math.round(forecast.getExtras().getWindKph());
                            speedUnit = context.getString(com.thewizrd.shared_resources.R.string.unit_kph);
                            break;
                        case Units.METERS_PER_SECOND:
                            speedVal = Math.round(ConversionMethods.kphToMsec(forecast.getExtras().getWindKph()));
                            speedUnit = context.getString(com.thewizrd.shared_resources.R.string.unit_msec);
                            break;
                    }

                    String windSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit);

                    yData.add(new YEntryData(speedVal, windSpeed));
                }
                break;
            case RAIN:
                if (forecast.getExtras() != null && forecast.getExtras().getQpfRainIn() != null && forecast.getExtras().getQpfRainMm() != null) {
                    final String unit = settingsMgr.getPrecipitationUnit();
                    float precipValue;
                    String precipUnit;

                    switch (unit) {
                        case Units.INCHES:
                        default:
                            precipValue = forecast.getExtras().getQpfRainIn();
                            precipUnit = context.getString(R.string.unit_in);
                            break;
                        case Units.MILLIMETERS:
                            precipValue = forecast.getExtras().getQpfRainMm();
                            precipUnit = context.getString(R.string.unit_mm);
                            break;
                    }

                    yData.add(new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit)));
                }
                break;
            case SNOW:
                if (forecast.getExtras() != null && forecast.getExtras().getQpfSnowIn() != null && forecast.getExtras().getQpfSnowCm() != null) {
                    final String unit = settingsMgr.getPrecipitationUnit();
                    float precipValue;
                    String precipUnit;

                    switch (unit) {
                        case Units.INCHES:
                        default:
                            precipValue = forecast.getExtras().getQpfSnowIn();
                            precipUnit = context.getString(R.string.unit_in);
                            break;
                        case Units.MILLIMETERS:
                            precipValue = forecast.getExtras().getQpfSnowCm() * 10;
                            precipUnit = context.getString(R.string.unit_mm);
                            break;
                    }

                    yData.add(new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit)));
                }
                break;
            case UVINDEX:
                if (forecast.getExtras() != null && forecast.getExtras().getUvIndex() != null) {
                    yData.add(new YEntryData(forecast.getExtras().getUvIndex(), String.format(LocaleUtils.getLocale(), "%.1f", forecast.getExtras().getUvIndex())));
                }
                break;
            case HUMIDITY:
                if (forecast.getExtras() != null && forecast.getExtras().getHumidity() != null) {
                    yData.add(new YEntryData(forecast.getExtras().getHumidity(), String.format(LocaleUtils.getLocale(), "%d%%", forecast.getExtras().getHumidity())));
                }
                break;
        }
    }

    private List<LineDataSeries> createSeriesData(List<YEntryData> yData, ForecastGraphType graphType) {
        Context context = App.getInstance().getAppContext();

        LineDataSeries series;

        switch (graphType) {
            /*
            case TEMPERATURE:
                graphLabel = context.getString(R.string.label_temperature);
                series = new LineDataSeries(yData);
                series.setSeriesColors(Colors.ORANGERED);
                break;
             */
            default:
            case PRECIPITATION:
                graphLabel = context.getString(R.string.label_precipitation);
                series = new LineDataSeries(yData);
                series.setSeriesColors(ContextCompat.getColor(context, R.color.colorPrimary));
                series.setSeriesMinMax(0f, 100f);
                break;
            case WIND:
                graphLabel = context.getString(R.string.label_wind);
                series = new LineDataSeries(yData);
                series.setSeriesColors(Colors.SEAGREEN);
                break;
            case RAIN:
                graphLabel = context.getString(R.string.label_qpf_rain);
                series = new LineDataSeries(yData);
                series.setSeriesColors(Colors.DEEPSKYBLUE);
                break;
            case SNOW:
                graphLabel = context.getString(R.string.label_qpf_snow);
                series = new LineDataSeries(yData);
                series.setSeriesColors(Colors.SKYBLUE);
                break;
            case UVINDEX:
                graphLabel = context.getString(R.string.label_uv);
                series = new LineDataSeries(yData);
                series.setSeriesColors(Colors.ORANGE);
                series.setSeriesMinMax(0f, 12f);
                break;
            case HUMIDITY:
                graphLabel = context.getString(R.string.label_humidity);
                series = new LineDataSeries(yData);
                series.setSeriesColors(Colors.MEDIUMPURPLE);
                series.setSeriesMinMax(0f, 100f);
                break;
        }

        this.graphType = graphType;

        return Collections.singletonList(series);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        labelData = null;
        seriesData = null;
        graphLabel = null;
        graphType = null;
    }
}