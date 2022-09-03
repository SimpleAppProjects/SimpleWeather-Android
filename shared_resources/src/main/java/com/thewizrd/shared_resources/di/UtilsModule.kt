package com.thewizrd.shared_resources.di

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.preferences.SettingsManager

val settingsManager: SettingsManager by lazy { appLib.settingsManager }
val localBroadcastManager: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(appLib.context) }