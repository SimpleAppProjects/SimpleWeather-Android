package com.thewizrd.simpleweather.setup;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.stepstone.stepper.Step;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import com.thewizrd.shared_resources.controls.ComboBoxItem;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSetupSettingsBinding;

import java.util.ArrayList;
import java.util.List;

public class SetupSettingsFragment extends Fragment implements Step {

    private WeatherManager wm;

    private FragmentSetupSettingsBinding binding;

    private AppCompatActivity mActivity;
    private StepperDataManager mDataManager;
    private StepperLayout mStepperLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupSettingsBinding.inflate(inflater, container, false);
        wm = WeatherManager.getInstance();

        // Setup units pref
        binding.unitsPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.unitsPrefSwitch.performClick();
            }
        });

        binding.unitsPrefSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                binding.unitsPrefSummary.setText(isChecked ? R.string.pref_summary_celsius : R.string.pref_summary_fahrenheit);
                mDataManager.getArguments().putString(Settings.KEY_UNITS,
                        isChecked ? Settings.CELSIUS : Settings.FAHRENHEIT);
            }
        });

        if (mDataManager != null && mDataManager.getArguments().containsKey(Settings.KEY_UNITS)) {
            if (Settings.CELSIUS.equals(mDataManager.getArguments().getString(Settings.KEY_UNITS))) {
                binding.unitsPrefSwitch.setChecked(true);
            } else {
                binding.unitsPrefSwitch.setChecked(false);
            }
        }

        // Setup interval spinner
        binding.intervalPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.intervalPrefSpinner.performClick();
            }
        });
        List<ComboBoxItem> refreshList = new ArrayList<>();
        String[] refreshEntries = getResources().getStringArray(R.array.refreshinterval_entries);
        String[] refreshValues = getResources().getStringArray(R.array.refreshinterval_values);
        for (int i = 0; i < refreshEntries.length; i++) {
            refreshList.add(new ComboBoxItem(refreshEntries[i], refreshValues[i]));
        }
        ArrayAdapter<ComboBoxItem> refreshAdapter = new ArrayAdapter<>(
                mActivity, android.R.layout.simple_spinner_item,
                refreshList);
        refreshAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.intervalPrefSpinner.setAdapter(refreshAdapter);
        binding.intervalPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.intervalPrefSpinner.performClick();
            }
        });
        binding.intervalPrefSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (binding.intervalPrefSpinner.getSelectedItem() instanceof ComboBoxItem) {
                    ComboBoxItem item = (ComboBoxItem) binding.intervalPrefSpinner.getSelectedItem();
                    binding.intervalPrefSummary.setText(item.getDisplay());
                    mDataManager.getArguments().putInt(Settings.KEY_REFRESHINTERVAL, Integer.valueOf(item.getValue()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int index = 1;
        if (mDataManager != null && mDataManager.getArguments().containsKey(Settings.KEY_REFRESHINTERVAL)) {
            for (ComboBoxItem item : refreshList) {
                if (Integer.toString(mDataManager.getArguments().getInt(Settings.KEY_REFRESHINTERVAL, Settings.DEFAULTINTERVAL)).equals(item.getValue())) {
                    index = refreshList.indexOf(item);
                    break;
                }
            }
        }
        binding.intervalPrefSpinner.setSelection(index);

        // Setup notification pref
        binding.notificationPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.notificationPrefSwitch.performClick();
            }
        });
        binding.notificationPrefSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Change setting
                mDataManager.getArguments().putBoolean(Settings.KEY_ONGOINGNOTIFICATION, isChecked);
            }
        });
        if (mDataManager != null && mDataManager.getArguments().containsKey(Settings.KEY_ONGOINGNOTIFICATION)) {
            binding.notificationPrefSwitch.setChecked(mDataManager.getArguments().getBoolean(Settings.KEY_ONGOINGNOTIFICATION, false));
        }

        // Setup alerts pref
        binding.alertsPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.alertsPrefSwitch.performClick();
            }
        });
        binding.alertsPrefSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Change setting
                mDataManager.getArguments().putBoolean(Settings.KEY_USEALERTS, isChecked);
            }
        });
        if (mDataManager != null && mDataManager.getArguments().containsKey(Settings.KEY_USEALERTS)) {
            binding.alertsPrefSwitch.setChecked(mDataManager.getArguments().getBoolean(Settings.KEY_USEALERTS, false));
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (AppCompatActivity) context;
        mDataManager = (StepperDataManager) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mActivity = null;
        mDataManager = null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity = null;
        mDataManager = null;
    }

    private void runOnUiThread(Runnable action) {
        if (mActivity != null)
            mActivity.runOnUiThread(action);
    }

    @Nullable
    @Override
    public VerificationError verifyStep() {
        return null;
    }

    @Override
    public void onSelected() {

    }

    @Override
    public void onError(@NonNull VerificationError error) {

    }
}
