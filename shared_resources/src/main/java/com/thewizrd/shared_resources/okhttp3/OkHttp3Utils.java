package com.thewizrd.shared_resources.okhttp3;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import okhttp3.Response;

public class OkHttp3Utils {
    public static InputStream getStream(@NonNull Response response) throws IOException {
        if ("gzip".equalsIgnoreCase(response.header("Content-Encoding"))) {
            return new GZIPInputStream(response.body().byteStream());
        } else {
            return response.body().byteStream();
        }
    }
}
