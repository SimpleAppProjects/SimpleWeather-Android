@file:JvmMultifileClass
@file:JvmName("ExtrasKt")

package com.thewizrd.simpleweather.extras

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.navigation.findNavController
import androidx.preference.Preference
import com.google.android.gms.maps.MapsInitializer
import com.google.android.play.core.splitcompat.SplitCompat
import com.thewizrd.extras.ExtrasLibrary
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.store.PlayStoreUtils
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.FirebaseConfigurator
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.preferences.SettingsFragment
import com.thewizrd.simpleweather.preferences.SettingsFragmentDirections
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import timber.log.Timber

fun initializeExtras(app: ApplicationLib) {
    ExtrasLibrary.initialize(app)
    FirebaseConfigurator.initialize(app.appContext)
    if (BuildConfig.DEBUG) {
        MapsInitializer.initialize(app.appContext, MapsInitializer.Renderer.LATEST) {
            when (it) {
                MapsInitializer.Renderer.LATEST -> {
                    Timber.tag("Application").d("The latest version of the renderer is used.")
                }
                MapsInitializer.Renderer.LEGACY -> {
                    Timber.tag("Application").d("The legacy version of the renderer is used.")
                }
            }
        }
    }
}

fun App.attachToBaseContext(context: Context) {
    SplitCompat.install(context)
}

fun UserLocaleActivity.attachToBaseContext() {
    SplitCompat.installActivity(this)
}

fun isIconPackSupported(packKey: String?): Boolean {
    return com.thewizrd.extras.isIconPackSupported(packKey)
}

fun isWeatherAPISupported(api: String?): Boolean {
    return com.thewizrd.extras.isWeatherAPISupported(api)
}

fun SettingsFragment.navigateToPremiumFragment() {
    // Navigate to premium page
    if (isPremiumSupported()) {
        rootView.findNavController()
            .navigate(SettingsFragmentDirections.actionSettingsFragmentToPremiumFragment())
    } else {
        showSnackbar(
            Snackbar.make(R.string.message_premium_required, Snackbar.Duration.SHORT),
            null
        )
    }
    return
}

fun SettingsFragment.IconsFragment.navigateUnsupportedIconPack() {
    // Navigate to premium page
    if (isPremiumSupported()) {
        rootView.findNavController().navigate(R.id.action_iconsFragment_to_premiumFragment)
    } else {
        showSnackbar(
            Snackbar.make(R.string.message_premium_required, Snackbar.Duration.SHORT),
            null
        )
    }
    return
}

fun enableAdditionalRefreshIntervals(): Boolean {
    return ExtrasLibrary.isEnabled()
}

fun checkPremiumStatus() {
    ExtrasLibrary.checkPremiumStatus()
}

fun isPremiumSupported(): Boolean {
    return ExtrasLibrary.areSubscriptionsSupported
}

fun isRadarInteractionEnabled(): Boolean {
    return ExtrasLibrary.isEnabled()
}

fun areNotificationExtrasEnabled(): Boolean {
    return ExtrasLibrary.isEnabled()
}

fun SettingsFragment.createPremiumPreference(): Preference {
    val premiumPref = Preference(requireContext()).apply {
        title = context.getString(R.string.pref_title_premium)
        summary = context.getString(R.string.message_premium_prompt)
        setIcon(R.drawable.ic_star_24dp)
        order = 0
    }
    premiumPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        if (isPremiumSupported()) {
            rootView.findNavController().safeNavigate(
                SettingsFragmentDirections.actionSettingsFragmentToPremiumFragment()
            )
        } else {
            showSnackbar(
                Snackbar.make(R.string.message_premium_required, Snackbar.Duration.SHORT),
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
            openPlayStore()
            return true
        }

        private fun openPlayStore() {
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

                if (i.resolveActivity(appCompatActivity.packageManager) != null) {
                    startActivity(i)
                }
            }
        }
    }
}