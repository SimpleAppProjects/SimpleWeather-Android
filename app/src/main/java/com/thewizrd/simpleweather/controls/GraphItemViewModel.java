package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.BaseForecast;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.lineview.XLabelData;
import com.thewizrd.simpleweather.controls.lineview.YEntryData;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

public class GraphItemViewModel {
    private EntryData<XLabelData, GraphTemperature> tempEntryData;
    private EntryData<XLabelData, YEntryData> windEntryData;
    private EntryData<XLabelData, YEntryData> chanceEntryData;

    public GraphItemViewModel(BaseForecast forecast) {
        Context context = App.getInstance().getAppContext();
        boolean isFahrenheit = Settings.isFahrenheit();

        String date;
        GraphTemperature tempData = new GraphTemperature();

        if (forecast instanceof Forecast) {
            Forecast fcast = (Forecast) forecast;
            date = fcast.getDate().format(DateTimeFormatter.ofPattern(context.getString(R.string.forecast_date_format)));
        } else if (forecast instanceof HourlyForecast) {
            HourlyForecast fcast = (HourlyForecast) forecast;

            if (DateFormat.is24HourFormat(context)) {
                date = fcast.getDate().format(DateTimeFormatter.ofPattern(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_24HR)));
            } else {
                date = fcast.getDate().format(DateTimeFormatter.ofPattern(DateTimeConstants.ABBREV_12HR_AMPM));
            }
        } else {
            date = "";
        }

        // Temp Data
        XLabelData xTemp = new XLabelData(date, forecast.getIcon(), 0);
        if (forecast.getHighF() != null && forecast.getHighC() != null) {
            int value = isFahrenheit ? Math.round(forecast.getHighF()) : Math.round(forecast.getHighC());
            String hiTemp = String.format(Locale.getDefault(), "%d°", value);
            tempData.setHiTempData(new YEntryData(value, hiTemp));
        }
        if (forecast instanceof Forecast) {
            Forecast fcast = (Forecast) forecast;
            if (fcast.getLowF() != null && fcast.getLowC() != null) {
                int value = isFahrenheit ? Math.round(fcast.getLowF()) : Math.round(fcast.getLowC());
                String loTemp = String.format(Locale.getDefault(), "%d°", value);
                tempData.setLoTempData(new YEntryData(value, loTemp));
            }
        }
        tempEntryData = new EntryData<>(xTemp, tempData);

        // Wind Data
        if (forecast.getExtras().getWindMph() != null && forecast.getExtras().getWindKph() != null && forecast.getExtras().getWindMph() >= 0 &&
                forecast.getExtras().getWindDegrees() != null && forecast.getExtras().getWindDegrees() >= 0) {
            int speedVal = isFahrenheit ? Math.round(forecast.getExtras().getWindMph()) : Math.round(forecast.getExtras().getWindKph());
            String speedUnit = WeatherUtils.getSpeedUnit();

            String windSpeed = String.format(Locale.getDefault(), "%d %s", speedVal, speedUnit);
            int windDirection = forecast.getExtras().getWindDegrees();

            YEntryData y = new YEntryData(speedVal, windSpeed);
            XLabelData x = new XLabelData(date, context.getString(R.string.wi_wind_direction), windDirection + 180);
            windEntryData = new EntryData<>(x, y);
        }

        // PoP Chance Data
        if (forecast.getExtras().getPop() != null && forecast.getExtras().getPop() >= 0) {
            YEntryData y = new YEntryData(forecast.getExtras().getPop(), forecast.getExtras().getPop() + "%");
            XLabelData x = new XLabelData(date, context.getString(R.string.wi_raindrop), 0);
            chanceEntryData = new EntryData<>(x, y);
        }
    }

    public EntryData<XLabelData, GraphTemperature> getTempEntryData() {
        return tempEntryData;
    }

    public EntryData<XLabelData, YEntryData> getWindEntryData() {
        return windEntryData;
    }

    public EntryData<XLabelData, YEntryData> getChanceEntryData() {
        return chanceEntryData;
    }

    static class EntryData<X extends XLabelData, Y> {
        @NonNull
        private XLabelData labelData;
        @NonNull
        private Y entryData;

        EntryData(@NonNull XLabelData x, @NonNull Y y) {
            labelData = x;
            entryData = y;
        }

        @NonNull
        public XLabelData getLabelData() {
            return labelData;
        }

        public void setLabelData(@NonNull XLabelData labelData) {
            this.labelData = labelData;
        }

        @NonNull
        public Y getEntryData() {
            return entryData;
        }

        public void setEntryData(@NonNull Y entryData) {
            this.entryData = entryData;
        }
    }

    static class GraphTemperature {
        private YEntryData hiTempData;
        private YEntryData loTempData;

        private GraphTemperature() {
        }

        public YEntryData getHiTempData() {
            return hiTempData;
        }

        public void setHiTempData(YEntryData hiTempData) {
            this.hiTempData = hiTempData;
        }

        public YEntryData getLoTempData() {
            return loTempData;
        }

        public void setLoTempData(YEntryData loTempData) {
            this.loTempData = loTempData;
        }
    }
}
