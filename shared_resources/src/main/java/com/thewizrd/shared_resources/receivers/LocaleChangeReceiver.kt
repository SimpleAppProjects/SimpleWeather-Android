package com.thewizrd.shared_resources.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import com.thewizrd.shared_resources.utils.LocaleUtils

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class LocaleChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
            if (intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME) == context.packageName) {
                val localeList =
                    intent.getParcelableExtra(Intent.EXTRA_LOCALE_LIST, LocaleList::class.java)

                if (localeList != null && !localeList.isEmpty) {
                    val locale = localeList.get(0)
                    if (locale != LocaleUtils.getLocale()) {
                        val tag = locale.toLanguageTag()
                        LocaleUtils.setLocaleCode(tag)
                    }
                } else {
                    LocaleUtils.setLocaleCode("")
                }
            }
        }
    }
}