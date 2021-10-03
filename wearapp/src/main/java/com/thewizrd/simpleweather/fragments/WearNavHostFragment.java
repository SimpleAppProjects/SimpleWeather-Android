package com.thewizrd.simpleweather.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NavigationRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;

import com.thewizrd.simpleweather.R;

public class WearNavHostFragment extends NavHostFragment {
    private static final String KEY_GRAPH_ID = "android-support-nav:fragment:graphId";
    private static final String KEY_START_DESTINATION_ARGS =
            "android-support-nav:fragment:startDestinationArgs";
    private static final String KEY_NAV_CONTROLLER_STATE =
            "android-support-nav:fragment:navControllerState";
    private static final String KEY_DEFAULT_NAV_HOST = "android-support-nav:fragment:defaultHost";

    /**
     * Create a new NavHostFragment instance with an inflated {@link NavGraph} resource.
     *
     * @param graphResId resource id of the navigation graph to inflate
     * @return a new NavHostFragment instance
     */
    @NonNull
    public static WearNavHostFragment create(@NavigationRes int graphResId) {
        return create(graphResId, null);
    }

    /**
     * Create a new NavHostFragment instance with an inflated {@link NavGraph} resource.
     *
     * @param graphResId           resource id of the navigation graph to inflate
     * @param startDestinationArgs arguments to send to the start destination of the graph
     * @return a new NavHostFragment instance
     */
    @NonNull
    public static WearNavHostFragment create(@NavigationRes int graphResId,
                                             @Nullable Bundle startDestinationArgs) {
        Bundle b = null;
        if (graphResId != 0) {
            b = new Bundle();
            b.putInt(KEY_GRAPH_ID, graphResId);
        }
        if (startDestinationArgs != null) {
            if (b == null) {
                b = new Bundle();
            }
            b.putBundle(KEY_START_DESTINATION_ARGS, startDestinationArgs);
        }

        final WearNavHostFragment result = new WearNavHostFragment();
        if (b != null) {
            result.setArguments(b);
        }
        return result;
    }

    @NonNull
    @Override
    protected Navigator<? extends FragmentNavigator.Destination> createFragmentNavigator() {
        return new WearFragmentNavigator(requireContext(), getChildFragmentManager(), getContainerId());
    }

    /**
     * We specifically can't use {@link View#NO_ID} as the container ID (as we use
     * {@link androidx.fragment.app.FragmentTransaction#add(int, Fragment)} under the hood),
     * so we need to make sure we return a valid ID when asked for the container ID.
     *
     * @return a valid ID to be used to contain child fragments
     */
    private int getContainerId() {
        int id = getId();
        if (id != 0 && id != View.NO_ID) {
            return id;
        }
        // Fallback to using our own ID if this Fragment wasn't added via
        // add(containerViewId, Fragment)
        return R.id.nav_host_fragment_container;
    }
}
