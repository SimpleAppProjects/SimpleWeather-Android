package com.thewizrd.simpleweather.adapters;

import android.annotation.SuppressLint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.text.SpannableStringBuilderKt;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.common.controls.BaseForecastItemViewModel;
import com.thewizrd.common.controls.ForecastItemViewModel;
import com.thewizrd.common.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast;
import com.thewizrd.simpleweather.databinding.WeatherForecastPanelBinding;
import com.thewizrd.simpleweather.databinding.WeatherHrforecastPanelBinding;

public class ForecastItemAdapter<T extends BaseForecastItemViewModel>
        extends PagedListAdapter<T, RecyclerView.ViewHolder> {
    private static class ItemType {
        static final int FORECAST = 0;
        static final int HOURLYFORECAST = 1;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        private final WeatherForecastPanelBinding binding;

        public ForecastViewHolder(WeatherForecastPanelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ForecastItemViewModel model) {
            binding.setViewModel(model);
            binding.executePendingBindings();
        }
    }

    static class HourlyForecastViewHolder extends RecyclerView.ViewHolder {
        private final WeatherHrforecastPanelBinding binding;

        public HourlyForecastViewHolder(WeatherHrforecastPanelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(HourlyForecastItemViewModel model) {
            binding.setViewModel(model);
            binding.executePendingBindings();

            final HourlyForecast fcast = model.getForecast();
            final boolean is24hr = DateFormat.is24HourFormat(itemView.getContext());
            final String dayOfWeek = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK));
            final String time;
            final String timeSuffix;
            if (is24hr) {
                time = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_24HR)));
                timeSuffix = "";
            } else {
                time = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale("h"));
                timeSuffix = fcast.getDate().format(DateTimeUtils.ofPatternForUserLocale("a"));
            }

            SpannableStringBuilder sb = new SpannableStringBuilder(time);
            SpannableStringBuilderKt.scale(sb, 0.8f, spannableStringBuilder -> {
                spannableStringBuilder.append(timeSuffix);
                return null;
            });
            sb.append(StringUtils.lineSeparator());
            final int start = sb.length();
            sb.append(dayOfWeek);
            sb.setSpan(new RelativeSizeSpan(0.8f), start, sb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(ColorUtils.setAlphaComponent(Colors.WHITE, 0xB3)), start, sb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            binding.hrforecastDate.setText(sb);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) instanceof HourlyForecastItemViewModel) {
            return ItemType.HOURLYFORECAST;
        } else {
            return ItemType.FORECAST;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ForecastItemAdapter() {
        super(new ForecastDiffer<>());
    }

    @SuppressLint("NewApi")
    @NonNull
    @Override
    // Create new views (invoked by the layout manager)
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewGroup.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (viewType == ItemType.HOURLYFORECAST) {
            WeatherHrforecastPanelBinding binding = WeatherHrforecastPanelBinding.inflate(inflater);
            // FOR WEAR ONLY
            // set the view's size, margins, paddings and layout parameters
            binding.getRoot().setLayoutParams(layoutParams);
            return new HourlyForecastViewHolder(binding);
        } else {
            // create a new view
            WeatherForecastPanelBinding binding = WeatherForecastPanelBinding.inflate(inflater);
            // FOR WEAR ONLY
            // set the view's size, margins, paddings and layout parameters
            binding.getRoot().setLayoutParams(layoutParams);
            return new ForecastViewHolder(binding);
        }
    }

    @Override
    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (holder instanceof HourlyForecastViewHolder) {
            HourlyForecastViewHolder vh = (HourlyForecastViewHolder) holder;
            vh.bind((HourlyForecastItemViewModel) getItem(position));
        } else if (holder instanceof ForecastViewHolder) {
            ForecastViewHolder vh = (ForecastViewHolder) holder;
            vh.bind((ForecastItemViewModel) getItem(position));
        }
    }
}