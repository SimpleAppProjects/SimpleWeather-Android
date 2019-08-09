package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.WeatherUtils;

public class WeatherAlertPanel extends RelativeLayout {
    private AppCompatImageView alertIcon;
    private TextView alertTitle;
    private TextView postDate;
    private CardView headerCard;
    private CardView bodyCard;
    private TextView expandIcon;
    private TextView bodyTextView;

    private boolean expanded = false;

    public WeatherAlertPanel(Context context) {
        super(context);
        initialize(context);
    }

    public WeatherAlertPanel(Context context, WeatherAlertViewModel alertView) {
        super(context);
        initialize(context);
        setAlert(alertView);
    }

    public WeatherAlertPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public WeatherAlertPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewLayout = inflater.inflate(R.layout.weather_alert_panel, this);

        viewLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        alertIcon = viewLayout.findViewById(R.id.alert_icon);
        alertTitle = viewLayout.findViewById(R.id.alert_title);
        if (SimpleLibrary.getInstance().getApp().isPhone())
            postDate = viewLayout.findViewById(R.id.post_date);
        headerCard = viewLayout.findViewById(R.id.header_card);
        bodyCard = viewLayout.findViewById(R.id.body_card);
        expandIcon = viewLayout.findViewById(R.id.expand_icon);
        bodyTextView = viewLayout.findViewById(R.id.body_textview);

        bodyCard.setVisibility(GONE);
        headerCard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                expanded = !expanded;

                expandIcon.setText(expanded ?
                        R.string.materialicon_expand_less :
                        R.string.materialicon_expand_more);
                bodyCard.setVisibility(expanded ? VISIBLE : GONE);
            }
        });
    }

    public void setAlert(WeatherAlertViewModel alertView) {
        headerCard.setCardBackgroundColor(WeatherUtils.getColorFromAlertSeverity(alertView.getAlertSeverity()));
        alertIcon.setImageResource(WeatherUtils.getDrawableFromAlertType(alertView.getAlertType()));
        alertTitle.setText(alertView.getTitle());
        if (postDate != null)
            postDate.setText(alertView.getPostDate());
        bodyTextView.setText(String.format("%s\n%s\n%s", alertView.getExpireDate(), alertView.getMessage(), alertView.getAttribution()));
    }
}
