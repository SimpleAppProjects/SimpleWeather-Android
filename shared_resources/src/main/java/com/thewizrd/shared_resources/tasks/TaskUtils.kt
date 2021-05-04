package com.thewizrd.shared_resources.tasks

import com.google.android.gms.tasks.CancellationToken

object TaskUtils {
    @JvmStatic
    @Throws(InterruptedException::class)
    fun CancellationToken?.throwIfCancellationRequested() {
        if (this != null && this.isCancellationRequested) {
            throw InterruptedException()
        }
    }
}