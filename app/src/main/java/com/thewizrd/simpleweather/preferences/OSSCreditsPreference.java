package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ActivityUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

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

        final Context context = getContext();

        int bg_color = Settings.getUserThemeMode() != UserThemeMode.AMOLED_DARK ?
                ActivityUtils.getColor(context, android.R.attr.colorBackground) : Colors.BLACK;
        holder.itemView.setBackgroundColor(bg_color);

        TextView webView = holder.itemView.findViewById(R.id.textview);

        String creditsText = new AsyncTask<String>().await(new Callable<String>() {
            @Override
            public String call() throws Exception {
                StringBuilder sBuilder = new StringBuilder();
                InputStreamReader sReader = null;
                try {
                    sReader = new InputStreamReader(context.getAssets().open("credits/licenses.html"));

                    int c = 0;
                    while ((c = sReader.read()) != -1) {
                        sBuilder.append((char) c);
                    }
                } catch (IOException ignored) {
                } finally {
                    if (sReader != null)
                        sReader.close();
                }

                return sBuilder.toString();
            }
        });

        webView.setText(HtmlCompat.fromHtml(creditsText.replace("\n", "<br/>"), HtmlCompat.FROM_HTML_MODE_COMPACT));
        webView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
