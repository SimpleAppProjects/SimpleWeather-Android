/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thewizrd.simpleweather.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.FragmentNavigator;

import java.util.ArrayDeque;
import java.util.Map;

/**
 * Navigator that navigates through {@link FragmentTransaction fragment transactions}. Every
 * destination using this Navigator must set a valid Fragment class name with
 * <code>android:name</code> or {@link FragmentNavigator.Destination#setClassName(String)}.
 * <p>
 * The current Fragment from FragmentNavigator's perspective can be retrieved by calling
 * {@link FragmentManager#getPrimaryNavigationFragment()} with the FragmentManager
 * passed to this FragmentNavigator.
 * <p>
 * Note that the default implementation does Fragment transactions
 * asynchronously, so the current Fragment will not be available immediately
 * (i.e., in callbacks to {@link NavController.OnDestinationChangedListener}).
 */
@Navigator.Name("fragment")
public class WearFragmentNavigator extends Navigator<FragmentNavigator.Destination> {
    private static final String TAG = "FragmentNavigator";
    private static final String KEY_BACK_STACK_IDS = "androidx-nav-fragment:navigator:backStackIds";

    private final Context mContext;
    private final FragmentManager mFragmentManager;
    private final int mContainerId;
    private ArrayDeque<Integer> mBackStack = new ArrayDeque<>();

    public WearFragmentNavigator(@NonNull Context context, @NonNull FragmentManager manager,
                                 int containerId) {
        mContext = context;
        mFragmentManager = manager;
        mContainerId = containerId;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method must call
     * {@link FragmentTransaction#setPrimaryNavigationFragment(Fragment)}
     * if the pop succeeded so that the newly visible Fragment can be retrieved with
     * {@link FragmentManager#getPrimaryNavigationFragment()}.
     * <p>
     * Note that the default implementation pops the Fragment
     * asynchronously, so the newly visible Fragment from the back stack
     * is not instantly available after this call completes.
     */
    @Override
    public boolean popBackStack() {
        if (mBackStack.isEmpty()) {
            return false;
        }
        if (mFragmentManager.isStateSaved()) {
            Log.i(TAG, "Ignoring popBackStack() call: FragmentManager has already"
                    + " saved its state");
            return false;
        }
        mFragmentManager.popBackStack(
                generateBackStackName(mBackStack.size(), mBackStack.peekLast()),
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mBackStack.removeLast();
        return true;
    }

    @NonNull
    @Override
    public FragmentNavigator.Destination createDestination() {
        return new FragmentNavigator.Destination(this);
    }

    /**
     * Instantiates the Fragment via the FragmentManager's
     * {@link androidx.fragment.app.FragmentFactory}.
     * <p>
     * Note that this method is <strong>not</strong> responsible for calling
     * {@link Fragment#setArguments(Bundle)} on the returned Fragment instance.
     *
     * @param context         Context providing the correct {@link ClassLoader}
     * @param fragmentManager FragmentManager the Fragment will be added to
     * @param className       The Fragment to instantiate
     * @param args            The Fragment's arguments, if any
     * @return A new fragment instance.
     * @deprecated Set a custom {@link androidx.fragment.app.FragmentFactory} via
     * {@link FragmentManager#setFragmentFactory(FragmentFactory)} to control
     * instantiation of Fragments.
     */
    @SuppressWarnings("DeprecatedIsStillUsed") // needed to maintain forward compatibility
    @Deprecated
    @NonNull
    public Fragment instantiateFragment(@NonNull Context context,
                                        @NonNull FragmentManager fragmentManager,
                                        @NonNull String className, @SuppressWarnings("unused") @Nullable Bundle args) {
        return fragmentManager.getFragmentFactory().instantiate(
                context.getClassLoader(), className);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method should always call
     * {@link FragmentTransaction#setPrimaryNavigationFragment(Fragment)}
     * so that the Fragment associated with the new destination can be retrieved with
     * {@link FragmentManager#getPrimaryNavigationFragment()}.
     * <p>
     * Note that the default implementation commits the new Fragment
     * asynchronously, so the new Fragment is not instantly available
     * after this call completes.
     */
    @SuppressWarnings("deprecation") /* Using instantiateFragment for forward compatibility */
    @Nullable
    @Override
    public NavDestination navigate(@NonNull FragmentNavigator.Destination destination, @Nullable Bundle args,
                                   @Nullable NavOptions navOptions, @Nullable Navigator.Extras navigatorExtras) {
        if (mFragmentManager.isStateSaved()) {
            Log.i(TAG, "Ignoring navigate() call: FragmentManager has already"
                    + " saved its state");
            return null;
        }
        String className = destination.getClassName();
        if (className.charAt(0) == '.') {
            className = mContext.getPackageName() + className;
        }
        final Fragment frag = instantiateFragment(mContext, mFragmentManager,
                className, args);
        frag.setArguments(args);
        final FragmentTransaction ft = mFragmentManager.beginTransaction();

        int enterAnim = navOptions != null ? navOptions.getEnterAnim() : -1;
        int exitAnim = navOptions != null ? navOptions.getExitAnim() : -1;
        int popEnterAnim = navOptions != null ? navOptions.getPopEnterAnim() : -1;
        int popExitAnim = navOptions != null ? navOptions.getPopExitAnim() : -1;
        if (enterAnim != -1 || exitAnim != -1 || popEnterAnim != -1 || popExitAnim != -1) {
            enterAnim = enterAnim != -1 ? enterAnim : 0;
            exitAnim = exitAnim != -1 ? exitAnim : 0;
            popEnterAnim = popEnterAnim != -1 ? popEnterAnim : 0;
            popExitAnim = popExitAnim != -1 ? popExitAnim : 0;
            ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim);
        }

        ft.add(mContainerId, frag);
        ft.setPrimaryNavigationFragment(frag);

        final @IdRes int destId = destination.getId();
        final boolean initialNavigation = mBackStack.isEmpty();
        // TODO Build first class singleTop behavior for fragments
        final boolean isSingleTopReplacement = navOptions != null && !initialNavigation
                && navOptions.shouldLaunchSingleTop()
                && mBackStack.peekLast() == destId;

        boolean isAdded;
        if (initialNavigation) {
            isAdded = true;
        } else if (isSingleTopReplacement) {
            // Single Top means we only want one instance on the back stack
            if (mBackStack.size() > 1) {
                // If the Fragment to be replaced is on the FragmentManager's
                // back stack, a simple replace() isn't enough so we
                // remove it from the back stack and put our replacement
                // on the back stack in its place
                mFragmentManager.popBackStack(
                        generateBackStackName(mBackStack.size(), mBackStack.peekLast()),
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
                ft.addToBackStack(generateBackStackName(mBackStack.size(), destId));
            }
            isAdded = false;
        } else {
            ft.addToBackStack(generateBackStackName(mBackStack.size() + 1, destId));
            isAdded = true;
        }
        if (navigatorExtras instanceof FragmentNavigator.Extras) {
            FragmentNavigator.Extras extras = (FragmentNavigator.Extras) navigatorExtras;
            for (Map.Entry<View, String> sharedElement : extras.getSharedElements().entrySet()) {
                ft.addSharedElement(sharedElement.getKey(), sharedElement.getValue());
            }
        }
        ft.setReorderingAllowed(true);
        ft.commit();
        // The commit succeeded, update our view of the world
        if (isAdded) {
            mBackStack.add(destId);
            return destination;
        } else {
            return null;
        }
    }

    @Override
    @Nullable
    public Bundle onSaveState() {
        Bundle b = new Bundle();
        int[] backStack = new int[mBackStack.size()];
        int index = 0;
        for (Integer id : mBackStack) {
            backStack[index++] = id;
        }
        b.putIntArray(KEY_BACK_STACK_IDS, backStack);
        return b;
    }

    @Override
    public void onRestoreState(@Nullable Bundle savedState) {
        if (savedState != null) {
            int[] backStack = savedState.getIntArray(KEY_BACK_STACK_IDS);
            if (backStack != null) {
                mBackStack.clear();
                for (int destId : backStack) {
                    mBackStack.add(destId);
                }
            }
        }
    }

    @NonNull
    private String generateBackStackName(int backStackIndex, int destId) {
        return backStackIndex + "-" + destId;
    }
}