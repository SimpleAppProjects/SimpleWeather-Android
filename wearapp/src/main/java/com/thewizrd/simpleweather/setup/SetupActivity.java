package com.thewizrd.simpleweather.setup;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.AcceptDenyDialog;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.wear.widget.drawer.WearableDrawerLayout;
import androidx.wear.widget.drawer.WearableDrawerView;

import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.SetupGraphDirections;
import com.thewizrd.simpleweather.databinding.ActivitySetupBinding;
import com.thewizrd.simpleweather.helpers.AcceptDenyDialogBuilder;
import com.thewizrd.simpleweather.main.MainActivity;

public class SetupActivity extends FragmentActivity implements MenuItem.OnMenuItemClickListener {

    private ActivitySetupBinding binding;
    private NavController mNavController;

    private static final int REQUEST_CODE_SYNC_ACTIVITY = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("SetupActivity: onCreate");

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.activitySetup.setDrawerStateCallback(new WearableDrawerLayout.DrawerStateCallback() {
            @Override
            public void onDrawerOpened(WearableDrawerLayout layout, WearableDrawerView drawerView) {
                super.onDrawerOpened(layout, drawerView);
                drawerView.requestFocus();
            }

            @Override
            public void onDrawerClosed(WearableDrawerLayout layout, WearableDrawerView drawerView) {
                super.onDrawerClosed(layout, drawerView);
                drawerView.clearFocus();
            }
        });

        binding.bottomActionDrawer.setOnMenuItemClickListener(this);
        binding.bottomActionDrawer.setLockedWhenClosed(true);
        binding.bottomActionDrawer.setPeekOnScrollDownEnabled(true);
        binding.bottomActionDrawer.getController().peekDrawer();

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
        mNavController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                if (destination.getId() == R.id.locationSearchFragment) {
                    binding.bottomActionDrawer.getController().closeDrawer();
                    binding.bottomActionDrawer.setIsLocked(true);
                } else {
                    binding.bottomActionDrawer.setIsLocked(false);
                    binding.bottomActionDrawer.getController().peekDrawer();
                }
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                mNavController.navigate(SetupGraphDirections.actionGlobalSettingsActivity2());
                break;
            case R.id.menu_setupfromphone:
                new AcceptDenyDialogBuilder(this, new AcceptDenyDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            startActivityForResult(new Intent(SetupActivity.this, SetupSyncActivity.class), REQUEST_CODE_SYNC_ACTIVITY);
                        }
                    }
                }).setMessage(R.string.prompt_confirmsetup)
                        .show();
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_SYNC_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    if (Settings.getHomeData() != null) {
                        Settings.setDataSync(WearableDataSync.DEVICEONLY);
                        Settings.setWeatherLoaded(true);
                        // Start WeatherNow Activity
                        startActivity(new Intent(this, MainActivity.class));
                        finishAffinity();
                    }
                }
                break;
            default:
                break;
        }
    }
}