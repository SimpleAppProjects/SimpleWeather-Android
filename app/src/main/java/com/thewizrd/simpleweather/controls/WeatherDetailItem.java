package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.TabStopSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherDetailsType;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.WeatherDetailPanelBinding;

import java.util.Locale;

public class WeatherDetailItem extends ConstraintLayout {
    private WeatherDetailPanelBinding binding;
    private boolean expanded = false;

    public WeatherDetailItem(Context context) {
        super(context);
        initialize(context);
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

        binding = WeatherDetailPanelBinding.inflate(inflater, this, true);

        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        binding.headerCard.setOnClickListener(onClickListener);
    }

    private final View.OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            expanded = !expanded;
            binding.bodyCard.setVisibility(expanded ? VISIBLE : GONE);
        }
    };

    public void bind(BaseForecastItemViewModel model) {
        if (model instanceof ForecastItemViewModel) {
            bindModel((ForecastItemViewModel) model);
        } else if (model instanceof HourlyForecastItemViewModel) {
            bindModel((HourlyForecastItemViewModel) model);
        } else {
            binding.forecastDate.setText(R.string.placeholder_text);
            binding.forecastIcon.setImageResource(R.drawable.wi_na);
            binding.forecastCondition.setText(R.string.placeholder_text);
            clearForecastExtras();
            binding.bodyCard.setVisibility(GONE);
            binding.headerCard.setOnClickListener(null);
            binding.bodyTextview.setText("");
        }

        // Reset expanded state
        expanded = false;
        binding.bodyCard.setVisibility(GONE);

        // Animate weather icon if possible
        final Drawable drwbl = binding.forecastIcon.getDrawable();
        if (drwbl instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawableCompat.clearAnimationCallbacks(drwbl);
            AnimatedVectorDrawableCompat.registerAnimationCallback(drwbl, new Animatable2Compat.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    if (drawable instanceof AnimatedVectorDrawable) {
                        ((AnimatedVectorDrawable) drawable).start();
                    }
                }
            });
            ((AnimatedVectorDrawable) drwbl).start();
        }

        binding.executePendingBindings();
    }

    private void clearForecastExtras() {
        binding.forecastExtraPop.setVisibility(GONE);
        binding.forecastExtraPop.setText("");
        binding.forecastExtraClouds.setVisibility(GONE);
        binding.forecastExtraClouds.setText("");
        binding.forecastExtraWindspeed.setVisibility(GONE);
        binding.forecastExtraWindspeed.setText("");
    }

    private void bindModel(ForecastItemViewModel forecastView) {
        binding.forecastDate.setText(forecastView.getDate());
        binding.forecastIcon.setImageResource(forecastView.getWeatherIcon());
        binding.forecastCondition.setText(String.format(Locale.ROOT, "%s / %s - %s",
                forecastView.getHiTemp(), forecastView.getLoTemp(), forecastView.getCondition()));
        clearForecastExtras();

        binding.bodyCard.setVisibility(GONE);

        SpannableStringBuilder sb = new SpannableStringBuilder();

        if (!StringUtils.isNullOrWhitespace(forecastView.getConditionLongDesc())) {
            sb.append(forecastView.getConditionLongDesc())
                    .append(StringUtils.lineSeparator())
                    .append(StringUtils.lineSeparator());
        }

        if (forecastView.getExtras() != null && forecastView.getExtras().size() > 0) {
            Context context = getContext();
            binding.headerCard.setOnClickListener(onClickListener);

            if (StringUtils.isNullOrWhitespace(forecastView.getConditionLongDesc())) {
                TextPaint paint = binding.forecastCondition.getPaint();
                Layout layout = binding.forecastCondition.getLayout();
                float textWidth = paint.measureText(forecastView.getCondition());

                if (layout != null && textWidth > layout.getWidth()) {
                    sb.append(forecastView.getCondition())
                            .append(StringUtils.lineSeparator())
                            .append(StringUtils.lineSeparator());
                }
            }

            for (int i = 0; i < forecastView.getExtras().size(); i++) {
                DetailItemViewModel detailItem = forecastView.getExtras().get(i);

                if (detailItem.getDetailsType() == WeatherDetailsType.POPCHANCE) {
                    binding.forecastExtraPop.setText(detailItem.getValue());
                    binding.forecastExtraPop.setVisibility(VISIBLE);
                    continue;
                } else if (detailItem.getDetailsType() == WeatherDetailsType.POPCLOUDINESS) {
                    binding.forecastExtraClouds.setText(detailItem.getValue());
                    binding.forecastExtraClouds.setVisibility(VISIBLE);
                    continue;
                } else if (detailItem.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                    binding.forecastExtraWindspeed.setText(detailItem.getValue());
                    binding.forecastExtraWindspeed.setVisibility(VISIBLE);
                    continue;
                }

                int start = sb.length();
                sb.append(detailItem.getLabel());
                sb.append("\t");
                int colorSecondary = ActivityUtils.getColor(context, android.R.attr.textColorSecondary);
                sb.setSpan(new ForegroundColorSpan(colorSecondary), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new TabStopSpan.Standard((int) ActivityUtils.dpToPx(context, 150)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = sb.length();
                sb.append(detailItem.getValue());
                if (i < forecastView.getExtras().size())
                    sb.append(StringUtils.lineSeparator());
                int colorPrimary = ActivityUtils.getColor(context, android.R.attr.textColorPrimary);
                sb.setSpan(new ForegroundColorSpan(colorPrimary), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            binding.bodyTextview.setText(sb, TextView.BufferType.SPANNABLE);
        } else if (sb.length() > 0) {
            binding.bodyTextview.setText(sb, TextView.BufferType.SPANNABLE);
            binding.headerCard.setOnClickListener(onClickListener);
        } else {
            binding.headerCard.setOnClickListener(null);
        }
    }

    private void bindModel(final HourlyForecastItemViewModel forecastView) {
        binding.forecastDate.setText(forecastView.getDate());
        binding.forecastIcon.setImageResource(forecastView.getWeatherIcon());
        binding.forecastCondition.setText(String.format(Locale.ROOT, "%s - %s",
                forecastView.getHiTemp(), forecastView.getCondition()));
        clearForecastExtras();

        binding.bodyCard.setVisibility(GONE);
        if (forecastView.getExtras() != null && forecastView.getExtras().size() > 0) {
            Context context = getContext();
            binding.headerCard.setOnClickListener(onClickListener);

            final SpannableStringBuilder sb = new SpannableStringBuilder();

            for (int i = 0; i < forecastView.getExtras().size(); i++) {
                DetailItemViewModel detailItem = forecastView.getExtras().get(i);

                if (detailItem.getDetailsType() == WeatherDetailsType.POPCHANCE) {
                    binding.forecastExtraPop.setText(detailItem.getValue());
                    binding.forecastExtraPop.setVisibility(VISIBLE);
                    continue;
                } else if (detailItem.getDetailsType() == WeatherDetailsType.POPCLOUDINESS) {
                    binding.forecastExtraClouds.setText(detailItem.getValue());
                    binding.forecastExtraClouds.setVisibility(VISIBLE);
                    continue;
                } else if (detailItem.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                    binding.forecastExtraWindspeed.setText(detailItem.getValue());
                    binding.forecastExtraWindspeed.setVisibility(VISIBLE);
                    continue;
                }

                int start = sb.length();
                sb.append(detailItem.getLabel());
                sb.append("\t");
                int colorSecondary = ActivityUtils.getColor(context, android.R.attr.textColorSecondary);
                sb.setSpan(new ForegroundColorSpan(colorSecondary), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new TabStopSpan.Standard((int) ActivityUtils.dpToPx(context, 150)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = sb.length();
                sb.append(detailItem.getValue());
                if (i < forecastView.getExtras().size())
                    sb.append(StringUtils.lineSeparator());
                int colorPrimary = ActivityUtils.getColor(context, android.R.attr.textColorPrimary);
                sb.setSpan(new ForegroundColorSpan(colorPrimary), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            binding.forecastCondition.post(new Runnable() {
                @Override
                public void run() {
                    if (binding == null) return;
                    TextPaint paint = binding.forecastCondition.getPaint();
                    float textWidth = paint.measureText(forecastView.getCondition());

                    if (textWidth > binding.forecastCondition.getWidth()) {
                        sb.insert(0, forecastView.getCondition())
                                .append(StringUtils.lineSeparator())
                                .append(StringUtils.lineSeparator());
                    } else if (sb.length() == 0) {
                        binding.headerCard.setOnClickListener(null);
                        return;
                    }

                    binding.bodyTextview.setText(sb, TextView.BufferType.SPANNABLE);
                }
            });
        } else {
            binding.headerCard.setOnClickListener(null);
        }
    }
}
