package com.thewizrd.shared_resources.okhttp3;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CacheInterceptor implements Interceptor {
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String CACHE_CONTROL_NO_CACHE = "no-cache";

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        boolean shouldUseCache = !CACHE_CONTROL_NO_CACHE.equalsIgnoreCase(request.header(CACHE_CONTROL_HEADER));

        if (!shouldUseCache) {
            return response;
        }

        boolean hasCacheHeader = !StringUtils.isNullOrWhitespace(request.header(CACHE_CONTROL_HEADER));

        // Override server cache protocol
        Response.Builder builder = response.newBuilder()
                .removeHeader("Pragma");

        if (!hasCacheHeader) {
            // If original response does not contain a Cache-Control header
            // cache the response for a minimum of 2 min to avoid repeat requests
            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(2, TimeUnit.MINUTES)
                    .build();

            builder.header(CACHE_CONTROL_HEADER, cacheControl.toString());
        } else {
            builder.header(CACHE_CONTROL_HEADER, request.cacheControl().toString());
        }

        return builder.build();
    }
}
