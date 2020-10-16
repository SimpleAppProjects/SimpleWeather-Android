package com.thewizrd.simpleweather.setup;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.activity.UserLocaleActivity;
import com.thewizrd.simpleweather.databinding.ActivitySetupBinding;

public class SetupActivity extends UserLocaleActivity {

    private ActivitySetupBinding binding;
    private NavController mNavController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("SetupActivity: onCreate");

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            NavHostFragment hostFragment = NavHostFragment.create(R.navigation.setup_graph);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, hostFragment)
                    .setPrimaryNavigationFragment(hostFragment)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mNavController = Navigation.findNavController(this, R.id.fragment_container);
    }
}