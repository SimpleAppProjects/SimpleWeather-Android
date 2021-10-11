package com.thewizrd.simpleweather.utils

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigator

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
}