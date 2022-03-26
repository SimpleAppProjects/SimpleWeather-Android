package com.thewizrd.simpleweather.preferences.colorpreference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.skydoves.colorpickerview.ActionMode;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.databinding.LayoutColorpickerdialogBinding;

import java.util.Locale;

public class ColorPreferenceDialogFragment extends PreferenceDialogFragmentCompat {
    private static final String SAVE_STATE_COLOR = "ColorPreferenceDialogFragment.color";

    @ColorInt
    private int mColor;

    public static ColorPreferenceDialogFragment newInstance(String key) {
        final ColorPreferenceDialogFragment
                fragment = new ColorPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mColor = getColorPreference().getColor();
        } else {
            mColor = savedInstanceState.getInt(SAVE_STATE_COLOR);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_COLOR, mColor);
    }

    @Override
    protected View onCreateDialogView(@NonNull Context context) {
        final LayoutColorpickerdialogBinding pickerBinding = LayoutColorpickerdialogBinding.inflate(LayoutInflater.from(context));
        pickerBinding.whiteTileView.setPaintColor(Colors.WHITE);
        pickerBinding.whiteTileView.setOnClickListener(v -> {
            pickerBinding.colorPickerView.setInitialColor(Colors.WHITE);
            pickerBinding.colorPickerText.setText(String.format(Locale.ROOT, "#%08X", Colors.WHITE));
        });
        pickerBinding.blackTileView.setPaintColor(Colors.BLACK);
        pickerBinding.blackTileView.setOnClickListener(v -> {
            pickerBinding.colorPickerView.setInitialColor(Colors.BLACK);
            pickerBinding.colorPickerText.setText(String.format(Locale.ROOT, "#%08X", Colors.BLACK));
        });
        pickerBinding.transparentTileView.setPaintColor(Colors.TRANSPARENT);
        pickerBinding.transparentTileView.setOnClickListener(v -> {
            pickerBinding.colorPickerView.setInitialColor(Colors.TRANSPARENT);
            pickerBinding.colorPickerText.setText(String.format(Locale.ROOT, "#%08X", Colors.TRANSPARENT));
        });

        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    try {
                        pickerBinding.colorPickerView.setInitialColor(Color.parseColor(s.toString()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        pickerBinding.colorPickerView.setActionMode(ActionMode.ALWAYS);
        pickerBinding.colorPickerView.setColorListener((ColorListener) (color, fromUser) -> {
            mColor = color;
            pickerBinding.previewTileView.setPaintColor(color);

            if (fromUser) {
                pickerBinding.colorPickerText.removeTextChangedListener(textWatcher);
                pickerBinding.colorPickerText.setText(String.format(Locale.ROOT, "#%08X", color));
                pickerBinding.colorPickerText.addTextChangedListener(textWatcher);
            }
        });
        pickerBinding.colorPickerView.attachAlphaSlider(pickerBinding.alphaSlideBar);
        pickerBinding.colorPickerView.attachBrightnessSlider(pickerBinding.brightnessSlideBar);
        pickerBinding.colorPickerView.setInitialColor(mColor);
        pickerBinding.colorPickerText.setText(String.format(Locale.ROOT, "#%08X", mColor));
        pickerBinding.colorPickerText.addTextChangedListener(textWatcher);
        return pickerBinding.getRoot();
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
    }

    private ColorPreference getColorPreference() {
        return (ColorPreference) getPreference();
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected boolean needInputMethod() {
        return false;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            final ColorPreference preference = getColorPreference();
            if (preference.callChangeListener(mColor)) {
                preference.setColor(mColor);
            }
        }
    }
}
