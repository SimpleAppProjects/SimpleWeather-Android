package com.thewizrd.simpleweather.utils

import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController

// https://nezspencer.medium.com/navigation-components-a-fix-for-navigation-action-cannot-be-found-in-the-current-destination-95b63e16152e
object NavigationUtils {
    fun NavController.safeNavigate(direction: NavDirections) {
        currentDestination?.getAction(direction.actionId)?.run {
            navigate(direction)
        }
    }

    fun NavController.safeNavigate(direction: NavDirections, extras: Navigator.Extras) {
        currentDestination?.getAction(direction.actionId)?.run {
            navigate(direction, extras)
        }
    }

    @MainThread
    inline fun <reified VM : ViewModel> Fragment.navControllerViewModels(
        @IdRes navGraphId: Int
    ): Lazy<VM> {
        val navController by lazy {
            findNavController()
        }
        val owner by lazy(LazyThreadSafetyMode.NONE) {
            navController.getViewModelStoreOwner(navGraphId)
        }
        return lazy(LazyThreadSafetyMode.NONE) {
            ViewModelProvider(owner).get()
        }
    }
}