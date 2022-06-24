package com.thewizrd.common.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.thewizrd.shared_resources.appLib
import kotlinx.coroutines.*
import kotlin.coroutines.resume

object LiveDataUtils {
    suspend fun <T> LiveData<T>.await(): T {
        return withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine {
                val observer = object : Observer<T> {
                    override fun onChanged(t: T) {
                        removeObserver(this)
                        it.resume(t)
                    }
                }

                observeForever(observer)

                it.invokeOnCancellation {
                    appLib.appScope.launch(Dispatchers.Main.immediate) {
                        removeObserver(observer)
                    }
                }
            }
        }
    }

    suspend fun <T> LiveData<T>.awaitWithTimeout(timeoutMs: Long): T? {
        return withTimeoutOrNull(timeoutMs) {
            await()
        }
    }
}