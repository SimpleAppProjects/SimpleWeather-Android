package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.simpleweather.R;

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
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView textView = view.findViewById(R.id.textview);

        // Load html to string
        String creditsText = new AsyncTask<String>().await(new Callable<String>() {
            @Override
            public String call() {
                StringBuilder sBuilder = new StringBuilder();
                try (InputStreamReader sReader = new InputStreamReader(getContext().getAssets().open("credits/licenses.txt"))) {

                    int c = 0;
                    while ((c = sReader.read()) != -1) {
                        sBuilder.append((char) c);
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                }

                return sBuilder.toString();
            }
        });

        textView.setText(creditsText);
    }
}
