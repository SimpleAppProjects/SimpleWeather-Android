package com.thewizrd.simpleweather.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.simpleweather.R;

public class KeyEntryPreference extends EditTextPreference {
    public interface DialogCreatedListener {
        void beforeDialogCreated();
    }

    private DialogInterface.OnClickListener posButtonClickListener;
    private DialogInterface.OnClickListener negButtonClickListener;
    private DialogCreatedListener dialogCreatedListener;

    private String currentAPI;
    private String key;

    private EditText keyEntry;
    private EditText keyEntry2;

    public String getAPIKey() {
        return key;
    }

    public KeyEntryPreference(Context context) {
        super(context);
        this.currentAPI = Settings.getAPI();
    }

    public KeyEntryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.currentAPI = Settings.getAPI();
    }

    public KeyEntryPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.currentAPI = Settings.getAPI();
    }

    public KeyEntryPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.currentAPI = Settings.getAPI();
    }

    public void updateAPI(String currentAPI) {
        this.currentAPI = currentAPI;
    }

    @Override
    protected View onCreateDialogView() {
        if (dialogCreatedListener != null)
            dialogCreatedListener.beforeDialogCreated();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (currentAPI.equals(WeatherAPI.HERE)) {
            return inflater.inflate(R.layout.layout_keyentry2_dialog, null);
        } else {
            return inflater.inflate(R.layout.layout_keyentry_dialog, null);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        keyEntry = view.findViewById(android.R.id.edit);
        keyEntry.addTextChangedListener(editTextWatcher);

        if (currentAPI.equals(WeatherAPI.HERE)) {
            keyEntry2 = view.findViewById(R.id.keyEntry2);
            keyEntry2.addTextChangedListener(editTextWatcher);
        }
    }

    private TextWatcher editTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (currentAPI.equals(WeatherAPI.HERE)) {
                String app_id = null;
                String app_code = null;
                if (keyEntry != null) app_id = keyEntry.getText().toString();
                if (keyEntry2 != null) app_code = keyEntry2.getText().toString();

                key = String.format("%s;%s", app_id, app_code);
            } else {
                key = s.toString();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    public void setPositiveButtonOnClickListener(DialogInterface.OnClickListener listener) {
        posButtonClickListener = listener;
    }

    public void setNegativeButtonOnClickListener(DialogInterface.OnClickListener listener) {
        negButtonClickListener = listener;
    }

    public void setOnDialogCreatedListener(DialogCreatedListener listener) {
        dialogCreatedListener = listener;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(android.R.string.ok, posButtonClickListener);
        builder.setNegativeButton(android.R.string.cancel, negButtonClickListener);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        final AlertDialog alertDialog = (AlertDialog) getDialog();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (posButtonClickListener != null) {
                    posButtonClickListener.onClick(alertDialog, AlertDialog.BUTTON_POSITIVE);
                } else {
                    alertDialog.dismiss();
                }
            }
        });

        key = Settings.getAPIKEY();

        if (currentAPI.equals(WeatherAPI.HERE)) {
            String app_id = "";
            String app_code = "";

            if (!StringUtils.isNullOrWhitespace(key)) {
                String[] keyArr = key.split(";");
                if (keyArr.length > 0) {
                    app_id = keyArr[0];
                    app_code = keyArr[keyArr.length > 1 ? keyArr.length - 1 : 0];
                }
            }

            keyEntry.setText(app_id);
            keyEntry2.setText(app_code);
        }
    }
}
