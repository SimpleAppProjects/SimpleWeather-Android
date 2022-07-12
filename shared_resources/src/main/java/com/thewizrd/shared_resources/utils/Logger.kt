package com.thewizrd.shared_resources.utils

import android.content.Context
import com.thewizrd.shared_resources.BuildConfig
import com.thewizrd.shared_resources.appLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree

object Logger {
    @JvmStatic
    fun init(context: Context) {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            if (BuildConfig.IS_NONGMS) {
                Timber.plant(FileLoggingTree(context.applicationContext))
            }

            cleanupLogs(context.applicationContext)
        }
    }

    fun enableDebugLogger(context: Context, enable: Boolean) {
        if (enable) {
            if (!Timber.forest().any { it is FileLoggingTree }) {
                Timber.plant(FileLoggingTree(context.applicationContext))
            }
        } else {
            Timber.forest().forEach {
                if (it is FileLoggingTree) {
                    Timber.uproot(it)
                }
            }
        }
    }

    @JvmStatic
    fun registerLogger(tree: Timber.Tree) {
        Timber.plant(tree)
    }

    @JvmStatic
    fun shutdown() {
        Timber.uprootAll()
    }

    @JvmStatic
    fun writeLine(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {
        Timber.log(priority, t, message, *args)
    }

    @JvmStatic
    fun writeLine(priority: Int, message: String?, vararg args: Any?) {
        Timber.log(priority, message, *args)
    }

    @JvmStatic
    fun writeLine(priority: Int, t: Throwable?) {
        Timber.log(priority, t)
    }

    private fun cleanupLogs(context: Context) {
        appLib.appScope.launch(Dispatchers.IO) {
            FileUtils.deleteDirectory(context.getExternalFilesDir(null).toString() + "/logs")
        }
    }
}