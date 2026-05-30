package com.thewizrd.simpleweather.controls.viewmodels;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.preferences.SettingsManager;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.model.BaseForecast;
import com.thewizrd.shared_resources.weatherdata.model.Forecast;
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast;
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
    public enum GraphType {
        Line, Bar
    }

    private final Context context;

    private final SettingsManager settingsMgr;

    private GraphData<?> graphData;

    private ForecastType forecastType;

    private GraphType graphType;

    public GraphData<?> getGraphData() {
        return graphData;
    }

    public ForecastType getForecastType() {
        return forecastType;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public ForecastGraphViewModel(@NonNull Context context) {
        this.context = context;
        this.settingsMgr = new SettingsManager(context);
    }

    public void addForecastData(BaseForecast forecast, ForecastType forecastType, GraphType graphType) {
        if (graphType == GraphType.Bar) {
            if (graphData == null) {
                BarGraphDataSet dataSet = createDataSet(new ArrayList<>(), forecastType);
                addEntryData(forecast, dataSet, forecastType);
                this.graphData = createGraphData(dataSet, forecastType);
                this.graphType = graphType;
            } else {
                addEntryData(forecast, (BarGraphDataSet) graphData.getDataSetByIndex(0), forecastType);
            }
        } else {
            if (graphData == null) {
                LineDataSeries series = createSeriesData(new ArrayList<>(), forecastType);
                addEntryData(forecast, series, forecastType);
                this.graphData = createGraphData(Collections.singletonList(series), forecastType);
                this.graphType = graphType;
            } else {
                addEntryData(forecast, (LineDataSeries) graphData.getDataSetByIndex(0), forecastType);
            }
        }

        // Re-calc min/max
        this.graphData.notifyDataChanged();
    }

    public void setForecastData(@NonNull List<? extends BaseForecast> forecasts, ForecastType forecastType, GraphType graphType) {
        if (graphType == GraphType.Bar) {
            BarGraphDataSet dataSet = createDataSet(new ArrayList<>(forecasts.size()), forecastType);

            for (BaseForecast forecast : forecasts) {
                addEntryData(forecast, dataSet, forecastType);
            }

            this.graphData = createGraphData(dataSet, forecastType);
        } else {
            LineDataSeries series = createSeriesData(new ArrayList<>(forecasts.size()), forecastType);

            for (BaseForecast forecast : forecasts) {
                addEntryData(forecast, series, forecastType);
            }

            this.graphData = createGraphData(Collections.singletonList(series), forecastType);
        }

        this.forecastType = forecastType;
        this.graphType = graphType;
        updateDataSetMinMax();
    }

    public void setMinutelyForecastData(@NonNull List<MinutelyForecast> forecasts, GraphType graphType) {
        if (graphType == GraphType.Bar) {
            BarGraphDataSet dataSet = createDataSet(new ArrayList<>(forecasts.size()), ForecastType.MINUTELY);

            for (MinutelyForecast forecast : forecasts) {
                addMinutelyEntryData(forecast, dataSet);
            }

            this.graphData = createGraphData(dataSet, ForecastType.MINUTELY);
        } else {
            LineDataSeries series = createSeriesData(new ArrayList<>(forecasts.size()), ForecastType.MINUTELY);

            for (MinutelyForecast forecast : forecasts) {
                addMinutelyEntryData(forecast, series);
            }

            this.graphData = createGraphData(Collections.singletonList(series), ForecastType.MINUTELY);
        }

        this.forecastType = ForecastType.MINUTELY;
        this.graphType = graphType;
        updateDataSetMinMax();
    }

    private void addEntryData(BaseForecast forecast, LineDataSeries series, @NonNull ForecastType forecastType) {
        final boolean isFahrenheit = Units.FAHRENHEIT.equals(settingsMgr.getTemperatureUnit());

        final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
        df.applyPattern("0.##");

        final String date = getDateFromForecast(forecast);

        switch (forecastType) {
            case TEMPERATURE:
                if (forecast.getHighF() != null && forecast.getHighC() != null) {
                    int value = isFahrenheit ? Math.round(forecast.getHighF()) : Math.round(forecast.getHighC());
                    String hiTemp = String.format(LocaleUtils.getLocale(), "%d°", value);
                    series.addEntry(new LineGraphEntry(date, new YEntryData(value, hiTemp)));
                }
                break;
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

                    switch (unit) {
                        case Units.MILES_PER_HOUR:
                        default:
                            speedVal = Math.round(forecast.getExtras().getWindMph());
                            break;
                        case Units.KILOMETERS_PER_HOUR:
                            speedVal = Math.round(forecast.getExtras().getWindKph());
                            break;
                        case Units.METERS_PER_SECOND:
                            speedVal = Math.round(ConversionMethods.kphToMsec(forecast.getExtras().getWindKph()));
                            break;
                        case Units.KNOTS:
                            speedVal = Math.round(ConversionMethods.mphToKts(forecast.getExtras().getWindMph()));
                            break;
                    }

                    String windSpeed = String.format(LocaleUtils.getLocale(), "%d", speedVal);

                    series.addEntry(new LineGraphEntry(date, new YEntryData(speedVal, windSpeed)));
                }
                break;
            case RAIN:
                if (forecast.getExtras() != null && forecast.getExtras().getQpfRainIn() != null && forecast.getExtras().getQpfRainMm() != null) {
                    final String unit = settingsMgr.getPrecipitationUnit();
                    float precipValue;

                    switch (unit) {
                        case Units.INCHES:
                        default:
                            precipValue = forecast.getExtras().getQpfRainIn();
                            break;
                        case Units.MILLIMETERS:
                            precipValue = forecast.getExtras().getQpfRainMm();
                            break;
                    }

                    series.addEntry(new LineGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s", df.format(precipValue)))));
                }
                break;
            case SNOW:
                if (forecast.getExtras() != null && forecast.getExtras().getQpfSnowIn() != null && forecast.getExtras().getQpfSnowCm() != null) {
                    final String unit = settingsMgr.getPrecipitationUnit();
                    float precipValue;

                    switch (unit) {
                        case Units.INCHES:
                        default:
                            precipValue = forecast.getExtras().getQpfSnowIn();
                            break;
                        case Units.MILLIMETERS:
                            precipValue = forecast.getExtras().getQpfSnowCm() * 10;
                            break;
                    }

                    series.addEntry(new LineGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s", df.format(precipValue)))));
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

    private void addMinutelyEntryData(@NonNull MinutelyForecast forecast, LineDataSeries series) {
        if ((forecast.getRainMm() != null && forecast.getRainMm() >= 0) || (forecast.getSnowMm() != null && forecast.getSnowMm() >= 0)) {

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

            float rainMm = forecast.getRainMm() != null ? forecast.getRainMm() : 0f;
            float snowMm = forecast.getSnowMm() != null ? forecast.getSnowMm() : 0f;
            boolean isSnow = snowMm > rainMm;

            switch (unit) {
                case Units.INCHES:
                default:
                    precipValue = ConversionMethods.mmToIn(isSnow ? snowMm : rainMm);
                    break;
                case Units.MILLIMETERS:
                    precipValue = isSnow ? snowMm : rainMm;
                    break;
            }

            series.addEntry(new LineGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s", df.format(precipValue)))));
        }
    }

    @NonNull
    private LineDataSeries createSeriesData(List<LineGraphEntry> entryData, @NonNull ForecastType forecastType) {
        LineDataSeries series;

        switch (forecastType) {
            case TEMPERATURE:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(Colors.ORANGERED);
                break;
            default:
            case PRECIPITATION:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(ContextCompat.getColor(context, com.thewizrd.shared_resources.R.color.colorPrimary));
                series.setSeriesMinMax(0f, 100f);
                break;
            case WIND:
                series = new LineDataSeries(entryData);
                series.setSeriesColors(Colors.SEAGREEN);
                break;
            case MINUTELY:
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

        switch (forecastType) {
            case TEMPERATURE, PRECIPITATION, HUMIDITY, UVINDEX -> {/* ignore */}
            case WIND -> {
                final String unit = settingsMgr.getSpeedUnit();
                series.setSeriesLabel(Units.getUnitString(context, unit));
            }
            case RAIN, SNOW, MINUTELY -> {
                final String unit = settingsMgr.getPrecipitationUnit();
                series.setSeriesLabel(Units.getUnitString(context, unit));
            }
        }

        return series;
    }

    @NonNull
    private LineViewData createGraphData(List<LineDataSeries> seriesData, @NonNull ForecastType forecastType) {
        final String graphLabel = GraphModelUtils.getLabelForGraphType(context, forecastType);
        this.forecastType = forecastType;

        return new LineViewData(graphLabel, seriesData);
    }

    @NonNull
    private BarGraphDataSet createDataSet(List<BarGraphEntry> entryData, @NonNull ForecastType forecastType) {
        final BarGraphDataSet dataSet = new BarGraphDataSet(entryData);

        switch (forecastType) {
            case PRECIPITATION, HUMIDITY -> dataSet.setMinMax(0f, 100f);
            case UVINDEX -> dataSet.setMinMax(0f, 12f);
            case TEMPERATURE -> {
            }
            case WIND -> {
                final String unit = settingsMgr.getSpeedUnit();
                // Max: 75mph
                Float maxValue = switch (unit) {
                    case Units.MILES_PER_HOUR -> 75f;
                    case Units.KILOMETERS_PER_HOUR -> 121f;
                    case Units.METERS_PER_SECOND -> 34f;
                    case Units.KNOTS -> 65f;
                    default -> null;
                };

                dataSet.setMinMax(0f, maxValue);
            }
            case SNOW, MINUTELY, RAIN -> dataSet.setMinMax(0f);
        }

        switch (forecastType) {
            case TEMPERATURE, PRECIPITATION, HUMIDITY, UVINDEX -> {/* ignore */}
            case WIND -> {
                final String unit = settingsMgr.getSpeedUnit();
                dataSet.setLabel(Units.getUnitString(context, unit));
            }
            case RAIN, SNOW, MINUTELY -> {
                final String unit = settingsMgr.getPrecipitationUnit();
                dataSet.setLabel(Units.getUnitString(context, unit));
            }
        }

        return dataSet;
    }

    @NonNull
    private BarGraphData createGraphData(BarGraphDataSet dataSet, @NonNull ForecastType forecastType) {
        final String graphLabel = GraphModelUtils.getLabelForGraphType(context, forecastType);
        this.forecastType = forecastType;

        return new BarGraphData(graphLabel, dataSet);
    }

    private void addEntryData(@NonNull BaseForecast forecast, @NonNull BarGraphDataSet dataSet, @NonNull ForecastType forecastType) {
        final boolean isFahrenheit = Units.FAHRENHEIT.equals(settingsMgr.getTemperatureUnit());

        final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
        df.applyPattern("0.##");

        final String date = getDateFromForecast(forecast);

        switch (forecastType) {
            case TEMPERATURE -> {
                if (forecast.getHighF() != null && forecast.getHighC() != null) {
                    int value = isFahrenheit ? Math.round(forecast.getHighF()) : Math.round(forecast.getHighC());
                    String hiTemp = String.format(LocaleUtils.getLocale(), "%d°", value);

                    final BarGraphEntry entry = new BarGraphEntry(date, new YEntryData(value, hiTemp));
                    entry.setFillColor(WeatherUtils.getColorFromTempF(forecast.getHighF()));
                    dataSet.addEntry(entry);
                }
            }
            case PRECIPITATION -> {
                final BarGraphEntry entry;

                if (forecast.getExtras().getPop() != null && forecast.getExtras().getPop() >= 0) {
                    entry = new BarGraphEntry(date, new YEntryData(forecast.getExtras().getPop(), forecast.getExtras().getPop() + "%"));
                } else {
                    entry = new BarGraphEntry(date, new YEntryData(0f, "0%"));
                }

                entry.setFillColor(ContextCompat.getColor(context, com.thewizrd.shared_resources.R.color.colorPrimary));
                dataSet.addEntry(entry);
            }
            case WIND -> {
                if (forecast.getExtras() != null &&
                        forecast.getExtras().getWindMph() != null && forecast.getExtras().getWindKph() != null && forecast.getExtras().getWindMph() >= 0) {
                    final String unit = settingsMgr.getSpeedUnit();
                    int speedVal;

                    switch (unit) {
                        case Units.MILES_PER_HOUR:
                        default:
                            speedVal = Math.round(forecast.getExtras().getWindMph());
                            break;
                        case Units.KILOMETERS_PER_HOUR:
                            speedVal = Math.round(forecast.getExtras().getWindKph());
                            break;
                        case Units.METERS_PER_SECOND:
                            speedVal = Math.round(ConversionMethods.kphToMsec(forecast.getExtras().getWindKph()));
                            break;
                        case Units.KNOTS:
                            speedVal = Math.round(ConversionMethods.mphToKts(forecast.getExtras().getWindMph()));
                            break;
                    }

                    String windSpeed = String.format(LocaleUtils.getLocale(), "%d", speedVal);
//                    String windDirection = null;
//
//                    if (forecast.getExtras().getWindDegrees() != null && forecast.getExtras().getWindDegrees() >= 0) {
//                        windDirection = WeatherUtils.getWindDirection(forecast.getExtras().getWindDegrees());
//                    }
//
//                    if (windDirection != null) {
//                        windSpeed = String.format("%s\n%s", windSpeed, windDirection);
//                    }

                    final BarGraphEntry entry = new BarGraphEntry(date, new YEntryData(speedVal, windSpeed));
                    if (forecast.getExtras().getWindDegrees() != null) {
                        entry.setXIconRotation(forecast.getExtras().getWindDegrees() + 180);
                    }
                    entry.setFillColor(Colors.SEAGREEN);
                    dataSet.addEntry(entry);
                }
            }
            case RAIN -> {
                if (forecast.getExtras() != null && forecast.getExtras().getQpfRainIn() != null && forecast.getExtras().getQpfRainMm() != null) {
                    final String unit = settingsMgr.getPrecipitationUnit();
                    float precipValue;

                    switch (unit) {
                        case Units.INCHES:
                        default:
                            precipValue = forecast.getExtras().getQpfRainIn();
                            break;
                        case Units.MILLIMETERS:
                            precipValue = forecast.getExtras().getQpfRainMm();
                            break;
                    }

                    final BarGraphEntry entry = new BarGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s", df.format(precipValue))));
                    entry.setFillColor(Colors.DEEPSKYBLUE);
                    dataSet.addEntry(entry);
                }
            }
            case SNOW -> {
                if (forecast.getExtras() != null && forecast.getExtras().getQpfSnowIn() != null && forecast.getExtras().getQpfSnowCm() != null) {
                    final String unit = settingsMgr.getPrecipitationUnit();
                    float precipValue;

                    switch (unit) {
                        case Units.INCHES:
                        default:
                            precipValue = forecast.getExtras().getQpfSnowIn();
                            break;
                        case Units.MILLIMETERS:
                            precipValue = forecast.getExtras().getQpfSnowCm() * 10;
                            break;
                    }

                    final BarGraphEntry entry = new BarGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s", df.format(precipValue))));
                    entry.setFillColor(Colors.SKYBLUE);
                    dataSet.addEntry(entry);
                }
            }
            case UVINDEX -> {
                if (forecast.getExtras() != null && forecast.getExtras().getUvIndex() != null) {
                    final BarGraphEntry entry = new BarGraphEntry(date, new YEntryData(forecast.getExtras().getUvIndex(), df.format(forecast.getExtras().getUvIndex())));
                    entry.setFillColor(WeatherUtils.getColorFromUVIndex(forecast.getExtras().getUvIndex()));
                    dataSet.addEntry(entry);
                }
            }
            case HUMIDITY -> {
                if (forecast.getExtras() != null && forecast.getExtras().getHumidity() != null) {
                    final BarGraphEntry entry = new BarGraphEntry(date, new YEntryData(forecast.getExtras().getHumidity(), String.format(LocaleUtils.getLocale(), "%d%%", forecast.getExtras().getHumidity())));
                    entry.setFillColor(Colors.MEDIUMPURPLE);
                    dataSet.addEntry(entry);
                }
            }
        }
    }

    private void addMinutelyEntryData(@NonNull MinutelyForecast forecast, BarGraphDataSet dataSet) {
        if ((forecast.getRainMm() != null && forecast.getRainMm() >= 0) || (forecast.getSnowMm() != null && forecast.getSnowMm() >= 0)) {

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

            float rainMm = forecast.getRainMm() != null ? forecast.getRainMm() : 0f;
            float snowMm = forecast.getSnowMm() != null ? forecast.getSnowMm() : 0f;
            boolean isSnow = snowMm > rainMm;

            precipValue = switch (unit) {
                default -> ConversionMethods.mmToIn(isSnow ? snowMm : rainMm);
                case Units.MILLIMETERS -> isSnow ? snowMm : rainMm;
            };

            final BarGraphEntry entry = new BarGraphEntry(date, new YEntryData(precipValue, String.format(LocaleUtils.getLocale(), "%s", df.format(precipValue))));
            entry.setXWeatherIcon(isSnow ? WeatherIcons.SNOWFLAKE_COLD : WeatherIcons.RAINDROP);
            entry.setFillColor(isSnow ? Colors.SKYBLUE : Colors.DEEPSKYBLUE);
            dataSet.addEntry(entry);
        }
    }

    private String getDateFromForecast(BaseForecast forecast) {
        String date;

        if (forecast instanceof Forecast) {
            Forecast fcast = (Forecast) forecast;
            date = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale(context.getString(com.thewizrd.shared_resources.R.string.forecast_date_format)));
        } else if (forecast instanceof HourlyForecast) {
            HourlyForecast fcast = (HourlyForecast) forecast;

            if (DateFormat.is24HourFormat(context)) {
                String skeleton = DateTimeConstants.SKELETON_24HR;
                date = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(skeleton)));
            } else {
                String pattern = DateTimeConstants.ABBREV_12HR_AMPM;
                date = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale(pattern));
            }
        } else {
            date = "";
        }

        return date;
    }

    public void updateDataSetMinMax() {
        GraphData<?> graphData = this.graphData;

        if (graphData instanceof BarGraphData barGraphData) {
            for (BarGraphDataSet dataSet : barGraphData.getDataSets()) {
                switch (forecastType) {
                    case RAIN, SNOW -> {
                        final String unit = settingsMgr.getPrecipitationUnit();

                        // Heavy rain — rate is >= 7.6 mm (0.30 in) per hr
                        // Snow will often accumulate at a rate of 0.5in (12.7mm) an hour
                        switch (unit) {
                            default:
                            case Units.INCHES:
                                if (forecastType == ForecastType.SNOW) {
                                    dataSet.setMinMax(0f, Math.max(dataSet.getYMax(), 0.5f));
                                } else {
                                    dataSet.setMinMax(0f, Math.max(dataSet.getYMax(), 0.3f));
                                }
                                break;
                            case Units.MILLIMETERS:
                                if (forecastType == ForecastType.SNOW) {
                                    dataSet.setMinMax(0f, Math.max(dataSet.getYMax(), 12.7f));
                                } else {
                                    dataSet.setMinMax(0f, Math.max(dataSet.getYMax(), 7.6f));
                                }
                                break;
                        }
                    }
                    case MINUTELY -> {
                        // Heavy rain — rate is >= 7.6 mm (0.30 in) per hr
                        final String unit = settingsMgr.getPrecipitationUnit();
                        switch (unit) {
                            default:
                            case Units.INCHES:
                                dataSet.setMinMax(0f, Math.max(dataSet.getYMax(), 0.3f));
                                break;
                            case Units.MILLIMETERS:
                                dataSet.setMinMax(0f, Math.max(dataSet.getYMax(), 7.6f));
                                break;
                        }
                    }
                }
            }
        } else if (graphData instanceof LineViewData lineViewData) {
            for (LineDataSeries series : lineViewData.getDataSets()) {
                switch (forecastType) {
                    case RAIN, SNOW -> {
                        final String unit = settingsMgr.getPrecipitationUnit();

                        // Heavy rain — rate is >= 7.6 mm (0.30 in) per hr
                        // Snow will often accumulate at a rate of 0.5in (12.7mm) an hour
                        switch (unit) {
                            default:
                            case Units.INCHES:
                                if (forecastType == ForecastType.SNOW) {
                                    series.setSeriesMinMax(0f, Math.max(series.getYMax(), 0.5f));
                                } else {
                                    series.setSeriesMinMax(0f, Math.max(series.getYMax(), 0.3f));
                                }
                                break;
                            case Units.MILLIMETERS:
                                if (forecastType == ForecastType.SNOW) {
                                    series.setSeriesMinMax(0f, Math.max(series.getYMax(), 12.7f));
                                } else {
                                    series.setSeriesMinMax(0f, Math.max(series.getYMax(), 7.6f));
                                }
                                break;
                        }
                    }
                    case MINUTELY -> {
                        // Heavy rain — rate is >= 7.6 mm (0.30 in) per hr
                        final String unit = settingsMgr.getPrecipitationUnit();
                        switch (unit) {
                            default:
                            case Units.INCHES:
                                series.setSeriesMinMax(0f, Math.max(series.getYMax(), 0.3f));
                                break;
                            case Units.MILLIMETERS:
                                series.setSeriesMinMax(0f, Math.max(series.getYMax(), 7.6f));
                                break;
                        }
                    }
                }
            }
        }

        if (graphData != null) {
            graphData.notifyDataChanged();
        }
    }
}