@file:JvmMultifileClass
@file:JvmName("ExtrasKt")

package com.thewizrd.simpleweather.extras

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.Preference
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.firebase.Firebase
import com.google.firebase.installations.installations
import com.thewizrd.extras.extrasModule
import com.thewizrd.extras.R as extrasRes
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.store.PlayStoreUtils
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.FirebaseConfigurator
import com.thewizrd.simpleweather.NavGraphDirections
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.preferences.BaseSettingsFragment
import com.thewizrd.simpleweather.preferences.DevSettingsFragment
import com.thewizrd.simpleweather.preferences.SettingsFragment
import com.thewizrd.simpleweather.preferences.`SettingsFragment$IconsFragmentDirections`
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

fun initializeExtras() {
    extrasModule.initialize()

    MapsInitializer.initialize(
        appLib.context,
        if (BuildConfig.DEBUG) Renderer.LEGACY else Renderer.LATEST
    ) {
        when (it) {
            Renderer.LATEST -> {
                Timber.tag("Application").d("The latest version of the renderer is used.")
            }

            Renderer.LEGACY -> {
                Timber.tag("Application").d("The legacy version of the renderer is used.")
            }
        }
    }
}

fun initializeFirebase(context: Context) {
    FirebaseConfigurator.initialize(context)
}

fun App.attachToBaseContext(context: Context) {
    SplitCompat.install(context)
}

fun UserLocaleActivity.attachToBaseContext() {
    SplitCompat.installActivity(this)
}

fun isIconPackSupported(packKey: String?): Boolean {
    return extrasModule.isIconPackSupported(packKey)
}

fun isWeatherAPISupported(api: String?): Boolean {
    return extrasModule.isWeatherAPISupported(api)
}

fun isPremiumWeatherAPI(api: String?): Boolean {
    return extrasModule.isPremiumWeatherAPI(api)
}

fun BaseSettingsFragment.navigateToPremiumFragment() {
    // Navigate to premium page
    if (isPremiumSupported()) {
        rootView.findNavController().safeNavigate(NavGraphDirections.actionGlobalPremiumFragment())
    } else {
        showSnackbar(
            Snackbar.make(
                rootView.context,
                extrasRes.string.message_premium_required,
                Snackbar.Duration.SHORT
            ),
            null
        )
    }
    return
}

fun SettingsFragment.IconsFragment.navigateUnsupportedIconPack() {
    // Navigate to premium page
    if (isPremiumSupported()) {
        rootView.findNavController()
            .safeNavigate(`SettingsFragment$IconsFragmentDirections`.actionIconsFragmentToPremiumFragment())
    } else {
        showSnackbar(
            Snackbar.make(
                rootView.context,
                extrasRes.string.message_premium_required,
                Snackbar.Duration.SHORT
            ),
            null
        )
    }
    return
}

fun enableAdditionalRefreshIntervals(): Boolean {
    return extrasModule.isPremiumEnabled()
}

fun checkPremiumStatus() {
    extrasModule.checkPremiumStatus()
}

fun isPremiumSupported(): Boolean {
    return extrasModule.areSubscriptionsSupported
}

fun isRadarInteractionEnabled(): Boolean {
    return extrasModule.isAtLeastProEnabled()
}

fun areNotificationExtrasEnabled(): Boolean {
    return extrasModule.isAtLeastProEnabled()
}

fun SettingsFragment.createPremiumPreference(): Preference {
    val premiumPref = Preference(requireContext()).apply {
        title = context.getString(extrasRes.string.pref_title_premium)
        summary = context.getString(extrasRes.string.pref_summary_premium)
        setIcon(R.drawable.ic_star_24dp)
        order = 0
    }
    premiumPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        if (isPremiumSupported()) {
            rootView.findNavController().safeNavigate(
                NavGraphDirections.actionGlobalPremiumFragment()
            )
        } else {
            showSnackbar(
                Snackbar.make(
                    rootView.context,
                    extrasRes.string.message_premium_required,
                    Snackbar.Duration.SHORT
                ),
                null
            )
        }
        true
    }
    return premiumPref
}

fun SettingsFragment.AboutAppFragment.setupReviewPreference(preference: Preference) {
    preference.isVisible = !BuildConfig.IS_NONGMS

    preference.onPreferenceClickListener = object : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            openPlayStore(preference.context)
            return true
        }

        private fun openPlayStore(context: Context) {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(PlayStoreUtils.getPlayStoreURI())
                )
            } catch (e: ActivityNotFoundException) {
                val i = Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(PlayStoreUtils.getPlayStoreWebURI())

                if (i.resolveActivity(context.packageManager) != null) {
                    startActivity(i)
                }
            }
        }
    }
}

fun DevSettingsFragment.updateFirebaseIdPreference(preference: Preference) {
    runCatching {
        lifecycleScope.launch {
            val firebaseId = Firebase.installations.id.await()
            preference.summary = firebaseId
        }
    }
}