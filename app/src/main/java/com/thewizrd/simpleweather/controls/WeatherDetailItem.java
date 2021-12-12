package com.thewizrd.simpleweather.controls;

import android.content.Context;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherDetailsType;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.utils.ContextUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.WeatherDetailPanelBinding;

import java.util.Locale;

public class WeatherDetailItem extends LinearLayout {
    /**
     * State indicating the group is expanded.
     */
    private static final int[] GROUP_EXPANDED_STATE_SET = {R.attr.state_expanded};

    private WeatherDetailPanelBinding binding;

    private boolean expandable = true;
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

        this.setOrientation(LinearLayout.VERTICAL);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        binding.headerCard.setOnClickListener(v -> toggle());
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            toggle();
        }
    }

    public void toggle() {
        if (isExpandable() && isEnabled()) {
            expanded = !expanded;
            binding.bodyCard.setVisibility(expanded ? View.VISIBLE : View.GONE);
            refreshDrawableState();
        }
    }

    public void bind(BaseForecastItemViewModel model) {
        // Reset expanded state
        setExpandable(true);
        setExpanded(false);

        if (model instanceof ForecastItemViewModel) {
            bindModel((ForecastItemViewModel) model);
        } else if (model instanceof HourlyForecastItemViewModel) {
            bindModel((HourlyForecastItemViewModel) model);
        } else {
            binding.forecastDate.setText(R.string.placeholder_text);
            binding.forecastIcon.setImageResource(R.drawable.wi_na);
            binding.forecastCondition.setText(R.string.placeholder_text);
            clearForecastExtras();
            setExpandable(false);
            binding.bodyTextview.setText("");
        }

        binding.executePendingBindings();

        WeatherIconsManager wim = WeatherIconsManager.getInstance();
        if (binding.forecastExtraPop.getIconProvider() != null) {
            binding.forecastExtraPop.setShowAsMonochrome(wim.shouldUseMonochrome(binding.forecastExtraPop.getIconProvider()));
        } else {
            binding.forecastExtraPop.setShowAsMonochrome(wim.shouldUseMonochrome());
        }
        if (binding.forecastExtraClouds.getIconProvider() != null) {
            binding.forecastExtraClouds.setShowAsMonochrome(wim.shouldUseMonochrome(binding.forecastExtraClouds.getIconProvider()));
        } else {
            binding.forecastExtraClouds.setShowAsMonochrome(wim.shouldUseMonochrome());
        }
        if (binding.forecastExtraWindspeed.getIconProvider() != null) {
            binding.forecastExtraWindspeed.setShowAsMonochrome(wim.shouldUseMonochrome(binding.forecastExtraWindspeed.getIconProvider()));
        } else {
            binding.forecastExtraWindspeed.setShowAsMonochrome(wim.shouldUseMonochrome());
        }
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
        binding.forecastIcon.setWeatherIcon(forecastView.getWeatherIcon());
        binding.forecastCondition.setText(String.format(Locale.ROOT, "%s / %s - %s",
                forecastView.getHiTemp(), forecastView.getLoTemp(), forecastView.getCondition()));
        clearForecastExtras();

        SpannableStringBuilder sb = new SpannableStringBuilder();

        if (!StringUtils.isNullOrWhitespace(forecastView.getConditionLongDesc())) {
            sb.append(forecastView.getConditionLongDesc())
                    .append(StringUtils.lineSeparator())
                    .append(StringUtils.lineSeparator());
        }

        if (forecastView.getExtras() != null && forecastView.getExtras().size() > 0) {
            Context context = getContext();
            setExpandable(true);

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
                int colorSecondary = ContextUtils.getAttrColor(context, android.R.attr.textColorSecondary);
                sb.setSpan(new ForegroundColorSpan(colorSecondary), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new TabStopSpan.Standard((int) ContextUtils.dpToPx(context, 150)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = sb.length();
                sb.append(detailItem.getValue());
                if (i < forecastView.getExtras().size() - 1)
                    sb.append(StringUtils.lineSeparator());
                int colorPrimary = ContextUtils.getAttrColor(context, android.R.attr.textColorPrimary);
                sb.setSpan(new ForegroundColorSpan(colorPrimary), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            binding.bodyTextview.setText(sb, TextView.BufferType.SPANNABLE);
        } else if (sb.length() > 0) {
            binding.bodyTextview.setText(sb, TextView.BufferType.SPANNABLE);
            setExpandable(true);
        } else {
            setExpandable(false);
        }
    }

    private void bindModel(final HourlyForecastItemViewModel forecastView) {
        binding.forecastDate.setText(forecastView.getDate());
        binding.forecastIcon.setWeatherIcon(forecastView.getWeatherIcon());
        binding.forecastCondition.setText(String.format(Locale.ROOT, "%s - %s",
                forecastView.getHiTemp(), forecastView.getCondition()));
        clearForecastExtras();

        if (forecastView.getExtras() != null && forecastView.getExtras().size() > 0) {
            Context context = getContext();
            setExpandable(true);

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
                int colorSecondary = ContextUtils.getAttrColor(context, android.R.attr.textColorSecondary);
                sb.setSpan(new ForegroundColorSpan(colorSecondary), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new TabStopSpan.Standard((int) ContextUtils.dpToPx(context, 150)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = sb.length();
                sb.append(detailItem.getValue());
                if (i < forecastView.getExtras().size() - 1)
                    sb.append(StringUtils.lineSeparator());
                int colorPrimary = ContextUtils.getAttrColor(context, android.R.attr.textColorPrimary);
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
                        setExpandable(false);
                        return;
                    }

                    binding.bodyTextview.setText(sb, TextView.BufferType.SPANNABLE);
                }
            });
        } else {
            setExpandable(false);
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

        if (isExpanded()) {
            mergeDrawableStates(drawableState, GROUP_EXPANDED_STATE_SET);
        }

        return drawableState;
    }
}
