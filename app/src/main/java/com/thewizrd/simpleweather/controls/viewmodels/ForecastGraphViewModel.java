package com.thewizrd.simpleweather.controls.viewmodels;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ContextUtils;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.model.BaseForecast;
import com.thewizrd.shared_resources.weatherdata.model.Forecast;
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.graphs.BarGraphData;
import com.thewizrd.simpleweather.controls.graphs.BarGraphDataSet;
import com.thewizrd.simpleweather.controls.graphs.BarGraphEntry;
import com.thewizrd.simpleweather.controls.graphs.GraphData;
import com.thewizrd.simpleweather.controls.graphs.LineDataSeries;
import com.thewizrd.simpleweather.controls.graphs.LineGraphEntry;
import com.thewizrd.simpleweather.controls.graphs.LineViewData;
import com.thewizrd.simpleweather.controls.graphs.YEntryData;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForecastGraphViewModel {
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

    private GraphData<?> graphData;

    private ForecastGraphType graphType;

    public GraphData<?> getGraphData() {
        return graphData;
    }

    public ForecastGraphType getGraphType() {
        return graphType;
    }

    public void addForecastData(BaseForecast forecast, ForecastGraphType graphType) {
        if (graphType != ForecastGraphType.UVINDEX) {
            if (graphData == null) {
                LineDataSeries series = createSeriesData(new ArrayList<>(), graphType);
                addEntryData(forecast, series, graphType);
                this.graphData = createGraphData(Collections.singletonList(series), graphType);
            } else {
                addEntryData(forecast, (LineDataSeries) graphData.getDataSetByIndex(0), graphType);
            }
        } else {
            if (graphData == null) {
                BarGraphDataSet dataSet = createDataSet(new ArrayList<>(), graphType);
                addEntryData(forecast, dataSet, graphType);
                this.graphData = createGraphData(dataSet, graphType);
            } else {
                addEntryData(forecast, (BarGraphDataSet) graphData.getDataSetByIndex(0), graphType);
            }
        }
    }

    public void setForecastData(List<? extends BaseForecast> forecasts, ForecastGraphType graphType) {
        LineDataSeries series = createSeriesData(new ArrayList<>(forecasts.size()), graphType);

        for (BaseForecast forecast : forecasts) {
            addEntryData(forecast, series, graphType);
        }

        this.graphData = createGraphData(Collections.singletonList(series), graphType);
        this.graphType = graphType;
    }

    public void setMinutelyForecastData(List<MinutelyForecast> forecasts) {
        LineDataSeries series = createSeriesData(new ArrayList<>(forecasts.size()), ForecastGraphType.PRECIPITATION);

        for (MinutelyForecast forecast : forecasts) {
            addMinutelyEntryData(forecast, series);
        }

        this.graphData = createGraphData(Collections.singletonList(series), ForecastGraphType.PRECIPITATION);
        this.graphType = ForecastGraphType.PRECIPITATION;
    }

    private void addEntryData(BaseForecast forecast, LineDataSeries series, ForecastGraphType graphType) {
        Context context = App.getInstance().getAppContext();
        final boolean isFahrenheit = Units.FAHRENHEIT.equals(settingsMgr.getTemperatureUnit());

        final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
        df.applyPattern("0.##");

        final String date = getDateFromForecast(forecast);

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
                    series.addEntry(new LineGraphEntry(date, new YEntryData(forecast.getExtras().getPop(), forecast.getExtras().getPop() + "%")));
                } else {
                    series.addEntry(new LineGraphEntry(date, new YEntryData(0f, "0%")));
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

                    series.addEntry(new LineGraphEntry(date, new YEntryData(speedVal, windSpeed)));
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

                    series.addEntry(new LineGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit))));
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

                    series.addEntry(new LineGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit))));
                }
                break;
            case UVINDEX:
                if (forecast.getExtras() != null && forecast.getExtras().getUvIndex() != null) {
                    series.addEntry(new LineGraphEntry(date, new YEntryData(forecast.getExtras().getUvIndex(), String.format(LocaleUtils.getLocale(), "%.1f", forecast.getExtras().getUvIndex()))));
                }
                break;
            case HUMIDITY:
                if (forecast.getExtras() != null && forecast.getExtras().getHumidity() != null) {
                    series.addEntry(new LineGraphEntry(date, new YEntryData(forecast.getExtras().getHumidity(), String.format(LocaleUtils.getLocale(), "%d%%", forecast.getExtras().getHumidity()))));
                }
                break;
        }
    }

    private void addMinutelyEntryData(MinutelyForecast forecast, LineDataSeries series) {
        if (forecast.getRainMm() != null && forecast.getRainMm() >= 0) {
            Context context = App.getInstance().getAppContext();

            final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
            df.applyPattern("0.##");

            String date;
            if (DateFormat.is24HourFormat(context)) {
                date = forecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_24HR)));
            } else {
                date = forecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM));
            }

            final String unit = settingsMgr.getPrecipitationUnit();
            float precipValue;
            String precipUnit;

            switch (unit) {
                case Units.INCHES:
                default:
                    precipValue = ConversionMethods.mmToIn(forecast.getRainMm());
                    precipUnit = context.getString(R.string.unit_in);
                    break;
                case Units.MILLIMETERS:
                    precipValue = forecast.getRainMm();
                    precipUnit = context.getString(R.string.unit_mm);
                    break;
            }

            series.addEntry(new LineGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit))));
        }
    }

    @NonNull
    private LineDataSeries createSeriesData(List<LineGraphEntry> entryData, @NonNull ForecastGraphType graphType) {
        Context context = App.getInstance().getAppContext();

        LineDataSeries series;

        switch (graphType) {
            /*
            case TEMPERATURE:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(Colors.ORANGERED);
                break;
             */
            default:
            case PRECIPITATION:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(ContextCompat.getColor(context, R.color.colorPrimary));
                series.setSeriesMinMax(0f, 100f);
                break;
            case WIND:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(Colors.SEAGREEN);
                break;
            case RAIN:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(Colors.DEEPSKYBLUE);
                break;
            case SNOW:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(Colors.SKYBLUE);
                break;
            case UVINDEX:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(Colors.ORANGE);
                series.setSeriesMinMax(0f, 12f);
                break;
            case HUMIDITY:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(Colors.MEDIUMPURPLE);
                series.setSeriesMinMax(0f, 100f);
                break;
        }

        return series;
    }

    @NonNull
    private LineViewData createGraphData(List<LineDataSeries> seriesData, @NonNull ForecastGraphType graphType) {
        final String graphLabel = getLabelForGraphType(graphType);
        this.graphType = graphType;

        return new LineViewData(graphLabel, seriesData);
    }

    @NonNull
    private BarGraphDataSet createDataSet(List<BarGraphEntry> entryData, @NonNull ForecastGraphType graphType) {
        final BarGraphDataSet dataSet = new BarGraphDataSet(entryData);

        switch (graphType) {
            case UVINDEX:
                dataSet.setMinMax(0f, 12f);
                break;
        }

        return dataSet;
    }

    @NonNull
    private BarGraphData createGraphData(BarGraphDataSet dataSet, @NonNull ForecastGraphType graphType) {
        final String graphLabel = getLabelForGraphType(graphType);
        this.graphType = graphType;

        return new BarGraphData(graphLabel, dataSet);
    }

    private void addEntryData(BaseForecast forecast, BarGraphDataSet dataSet, ForecastGraphType graphType) {
        final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
        df.applyPattern("0.##");

        final String date = getDateFromForecast(forecast);

        if (graphType == ForecastGraphType.UVINDEX) {
            if (forecast.getExtras() != null && forecast.getExtras().getUvIndex() != null) {
                final BarGraphEntry entry = new BarGraphEntry(date, new YEntryData(forecast.getExtras().getUvIndex(), String.format(LocaleUtils.getLocale(), "%.1f", forecast.getExtras().getUvIndex())));
                entry.setFillColor(WeatherUtils.getColorFromUVIndex(forecast.getExtras().getUvIndex()));
                dataSet.addEntry(entry);
            }
        }
    }

    private String getDateFromForecast(BaseForecast forecast) {
        Context context = App.getInstance().getAppContext();

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

        return date;
    }

    private String getLabelForGraphType(@NonNull ForecastGraphType graphType) {
        Context context = App.getInstance().getAppContext();

        String graphLabel;

        switch (graphType) {
            /*
            case TEMPERATURE:
                graphLabel = context.getString(R.string.label_temperature);
                break;
             */
            default:
            case PRECIPITATION:
                graphLabel = context.getString(R.string.label_precipitation);
                break;
            case WIND:
                graphLabel = context.getString(R.string.label_wind);
                break;
            case RAIN:
                graphLabel = context.getString(R.string.label_qpf_rain);
                break;
            case SNOW:
                graphLabel = context.getString(R.string.label_qpf_snow);
                break;
            case UVINDEX:
                graphLabel = context.getString(R.string.label_uv);
                break;
            case HUMIDITY:
                graphLabel = context.getString(R.string.label_humidity);
                break;
        }

        return graphLabel;
    }
}