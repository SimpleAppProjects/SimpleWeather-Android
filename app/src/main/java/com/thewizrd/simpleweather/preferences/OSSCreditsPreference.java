package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.thewizrd.simpleweather.R;

public class OSSCreditsPreference extends Preference {

    public OSSCreditsPreference(Context context) {
        super(context);
    }

    public OSSCreditsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OSSCreditsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OSSCreditsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        WebView webView = holder.itemView.findViewById(R.id.webview);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webView.loadUrl("file:///android_asset/credits/licenses.html");
        webView.setBackgroundColor(Color.TRANSPARENT);
    }
}
