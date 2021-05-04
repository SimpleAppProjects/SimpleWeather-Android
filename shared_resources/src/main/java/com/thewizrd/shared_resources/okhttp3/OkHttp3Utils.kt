package com.thewizrd.shared_resources.okhttp3

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OkHttp3Utils {
    @Throws(IOException::class)
    @JvmStatic
    fun Response.getStream(): InputStream {
        return if ("gzip".equals(this.header("Content-Encoding"), ignoreCase = true)) {
            GZIPInputStream(this.body!!.byteStream())
        } else {
            this.body!!.byteStream()
        }
    }

    suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            val callback = ResponseCallback(this, continuation)
            this.enqueue(callback)
            continuation.invokeOnCancellation(callback)
        }
    }

    private class ResponseCallback(
            private val call: Call,
            private val continuation: CancellableContinuation<Response>
    ) : Callback, CompletionHandler {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!call.isCanceled()) {
                continuation.resumeWithException(e)
            }
        }

        override fun invoke(cause: Throwable?) {
            try {
                call.cancel()
            } catch (t: Throwable) { /* no-op */
            }
        }
    }
}