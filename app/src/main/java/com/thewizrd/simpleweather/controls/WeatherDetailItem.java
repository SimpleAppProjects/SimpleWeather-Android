package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.design.card.MaterialCardView;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.TabStopSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherDetailsType;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ActivityUtils;

import java.util.Locale;

public class WeatherDetailItem extends ConstraintLayout {
    private TextView forecastDate;
    private TextView forecastIcon;
    private TextView forecastCondition;
    private TextView forecastExtra;
    private MaterialCardView headerCard;
    private MaterialCardView bodyCard;
    private TextView bodyTextView;

    private boolean expanded = false;

    public WeatherDetailItem(Context context) {
        super(context);
        initialize(context);
    }

    public WeatherDetailItem(Context context, ForecastItemViewModel forecastView) {
        super(context);
        initialize(context);
        setForecast(forecastView);
    }

    public WeatherDetailItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public WeatherDetailItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewLayout = inflater.inflate(R.layout.weather_detail_panel, this);

        forecastDate = viewLayout.findViewById(R.id.forecast_date);
        forecastIcon = viewLayout.findViewById(R.id.forecast_icon);
        forecastCondition = viewLayout.findViewById(R.id.forecast_condition);
        forecastExtra = viewLayout.findViewById(R.id.forecast_extra);
        headerCard = viewLayout.findViewById(R.id.header_card);
        bodyCard = viewLayout.findViewById(R.id.body_card);
        bodyTextView = viewLayout.findViewById(R.id.body_textview);

        forecastExtra.setVisibility(GONE);

        bodyCard.setVisibility(GONE);
        headerCard.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            expanded = !expanded;
            bodyCard.setVisibility(expanded ? VISIBLE : GONE);
        }
    };

    public void setForecast(ForecastItemViewModel forecastView) {
        forecastDate.setText(forecastView.getDate());
        forecastIcon.setText(forecastView.getWeatherIcon());
        forecastCondition.setText(String.format(Locale.ROOT, "%s/ %s- %s",
                forecastView.getHiTemp(), forecastView.getLoTemp(), forecastView.getCondition()));
        forecastExtra.setVisibility(GONE);

        bodyCard.setVisibility(GONE);
        if (forecastView.getExtras() != null) {
            Context context = getContext();
            headerCard.setOnClickListener(onClickListener);

            StringBuilder sbExtra = new StringBuilder();
            SpannableStringBuilder sb = new SpannableStringBuilder();

            if (!StringUtils.isNullOrWhitespace(forecastView.getConditionLongDesc())) {
                sb.append(forecastView.getConditionLongDesc())
                        .append(StringUtils.lineSeparator())
                        .append(StringUtils.lineSeparator());
            } else {
                TextPaint paint = forecastCondition.getPaint();
                Layout layout = forecastCondition.getLayout();
                float textWidth = paint.measureText(forecastView.getCondition());

                if (textWidth > layout.getWidth()) {
                    sb.append(forecastView.getCondition())
                            .append(StringUtils.lineSeparator())
                            .append(StringUtils.lineSeparator());
                }
            }

            for (int i = 0; i < forecastView.getExtras().size(); i++) {
                DetailItemViewModel detailItem = forecastView.getExtras().get(i);

                if (detailItem.getDetailsType() == WeatherDetailsType.POPCHANCE
                        || detailItem.getDetailsType() == WeatherDetailsType.POPCLOUDINESS
                        || detailItem.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                    if (sbExtra.length() > 0)
                        sbExtra.append("\u2003");

                    if (detailItem.getDetailsType() == WeatherDetailsType.WINDSPEED)
                        sbExtra.append(String.format(Locale.ROOT, "%s %s", WeatherIcons.STRONG_WIND, detailItem.getValue()));
                    else
                        sbExtra.append(String.format(Locale.ROOT, "%s %s", detailItem.getIcon(), detailItem.getValue()));
                    continue;
                }

                int start = sb.length();
                sb.append(detailItem.getLabel());
                sb.append("\t");
                sb.setSpan(new ForegroundColorSpan(Colors.GRAY), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new TabStopSpan.Standard((int) ActivityUtils.dpToPx(context, 150)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = sb.length();
                sb.append(detailItem.getValue());
                if (i < forecastView.getExtras().size() - 1)
                    sb.append(StringUtils.lineSeparator());
                sb.setSpan(new ForegroundColorSpan(Colors.BLACK), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (sbExtra.length() > 0) {
                forecastExtra.setVisibility(VISIBLE);
                forecastExtra.setText(sbExtra);
            }

            bodyTextView.setText(sb, TextView.BufferType.SPANNABLE);
        } else {
            headerCard.setOnClickListener(null);
        }
    }

    public void setForecast(final HourlyForecastItemViewModel forecastView) {
        forecastDate.setText(forecastView.getDate());
        forecastIcon.setText(forecastView.getWeatherIcon());
        forecastCondition.setText(String.format(Locale.ROOT, "%s- %s",
                forecastView.getHiTemp(), forecastView.getCondition()));
        forecastExtra.setVisibility(GONE);

        bodyCard.setVisibility(GONE);
        if (forecastView.getExtras() != null) {
            Context context = getContext();
            headerCard.setOnClickListener(onClickListener);

            final SpannableStringBuilder sbExtra = new SpannableStringBuilder();
            final SpannableStringBuilder sb = new SpannableStringBuilder();

            for (int i = 0; i < forecastView.getExtras().size(); i++) {
                DetailItemViewModel detailItem = forecastView.getExtras().get(i);

                if (detailItem.getDetailsType() == WeatherDetailsType.POPCHANCE
                        || detailItem.getDetailsType() == WeatherDetailsType.POPCLOUDINESS
                        || detailItem.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                    if (sbExtra.length() > 0)
                        sbExtra.append("\u2003");

                    if (detailItem.getDetailsType() == WeatherDetailsType.WINDSPEED)
                        sbExtra.append(String.format(Locale.ROOT, "%s %s", WeatherIcons.STRONG_WIND, detailItem.getValue()));
                    else
                        sbExtra.append(String.format(Locale.ROOT, "%s %s", detailItem.getIcon(), detailItem.getValue()));
                    continue;
                }

                int start = sb.length();
                sb.append(detailItem.getLabel());
                sb.append("\t");
                sb.setSpan(new ForegroundColorSpan(Colors.GRAY), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new TabStopSpan.Standard((int) ActivityUtils.dpToPx(context, 150)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = sb.length();
                sb.append(detailItem.getValue());
                if (i < forecastView.getExtras().size() - 1)
                    sb.append(StringUtils.lineSeparator());
                sb.setSpan(new ForegroundColorSpan(Colors.BLACK), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (sbExtra.length() > 0) {
                forecastExtra.setVisibility(VISIBLE);
                forecastExtra.setText(sbExtra);
            }

            forecastCondition.post(new Runnable() {
                @Override
                public void run() {
                    TextPaint paint = forecastCondition.getPaint();
                    float textWidth = paint.measureText(forecastView.getCondition());

                    if (textWidth > forecastCondition.getWidth()) {
                        sb.insert(0, forecastView.getCondition())
                                .append(StringUtils.lineSeparator())
                                .append(StringUtils.lineSeparator());
                    }

                    bodyTextView.setText(sb, TextView.BufferType.SPANNABLE);
                }
            });
        } else {
            headerCard.setOnClickListener(null);
        }
    }
}
