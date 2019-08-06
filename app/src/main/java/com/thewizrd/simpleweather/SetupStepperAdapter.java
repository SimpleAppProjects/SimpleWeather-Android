package com.thewizrd.simpleweather;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.stepstone.stepper.Step;
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter;
import com.stepstone.stepper.viewmodel.StepViewModel;
import com.thewizrd.shared_resources.utils.Settings;

public class SetupStepperAdapter extends AbstractFragmentStepAdapter {

    private final boolean isWeatherLoaded;

    public SetupStepperAdapter(@NonNull FragmentManager fm, @NonNull Context context) {
        super(fm, context);
        isWeatherLoaded = Settings.isWeatherLoaded();
    }

    @Override
    public Step createStep(int position) {
        switch (position) {
            case 0:
            default:
                return new SetupWelcomeFragment();
            case 1:
            case 2:
                if (isWeatherLoaded && position == 1) {
                    return new SetupSettingsFragment();
                } else if (position == 1) {
                    return new SetupLocationFragment();
                } else {
                    return new SetupSettingsFragment();
                }
        }
    }

    @Override
    public int getCount() {
        if (isWeatherLoaded)
            return 2;
        else
            return 3;
    }

    @NonNull
    @Override
    public StepViewModel getViewModel(int position) {
        StepViewModel.Builder builder = new StepViewModel.Builder(context);
        switch (position) {
            case 0:
                if (isWeatherLoaded)
                    builder.setEndButtonLabel(R.string.label_continue);
                else
                    builder.setEndButtonLabel(R.string.label_getstarted);
                break;
            case 1:
            case 2:
                if (isWeatherLoaded && position == 1) {
                    builder
                            .setBackButtonVisible(false)
                            .setEndButtonLabel(R.string.abc_action_mode_done);
                } else if (position == 1) {
                    builder
                            .setBackButtonVisible(false)
                            .setEndButtonVisible(false);
                } else {
                    builder
                            .setBackButtonVisible(false)
                            .setEndButtonLabel(R.string.abc_action_mode_done);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported position: " + position);
        }

        return builder.create();
    }
}
