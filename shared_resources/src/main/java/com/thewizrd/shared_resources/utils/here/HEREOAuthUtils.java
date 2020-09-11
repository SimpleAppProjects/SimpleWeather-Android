package com.thewizrd.shared_resources.utils.here;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.oauth.OAuthRequest;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.Callable;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HEREOAuthUtils {
    public static final String HERE_OAUTH_URL = "https://account.api.here.com/oauth2/token";
    private static final String KEY_TOKEN = "token";

    public static String getBearerToken(boolean forceRefresh) {
        if (!forceRefresh) {
            String token = AsyncTask.await(new Callable<String>() {
                @Override
                public String call() {
                    return getTokenFromStorage();
                }
            });
            if (!StringUtils.isNullOrWhitespace(token))
                return token;
            else
                forceRefresh = true;
        }

        if (forceRefresh) {
            OAuthRequest oAuthRequest = new OAuthRequest(Keys.getHERECliID(), Keys.getHERECliSecr(),
                    OAuthRequest.SignatureMethod.HMAC_SHA256, OAuthRequest.HTTPRequestType.POST);

            OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
            Response response = null;

            try {
                String authorization = oAuthRequest.getAuthorizationHeader(HERE_OAUTH_URL, true);

                Request request = new Request.Builder()
                        .url(HERE_OAUTH_URL)
                        .addHeader("Authorization", authorization)
                        .addHeader("Cache-Control", "no-cache")
                        .post(new FormBody.Builder().addEncoded("grant_type", "client_credentials").build())
                        .build();

                response = client.newCall(request).execute();

                final InputStream stream = response.body().byteStream();
                String dateField = response.header("Date", null);
                ZonedDateTime date = ZonedDateTime.parse(dateField, DateTimeFormatter.RFC_1123_DATE_TIME);

                TokenRootobject tokenRoot = JSONParser.deserializer(stream, TokenRootobject.class);

                if (tokenRoot != null) {
                    String tokenStr = String.format(Locale.ROOT, "Bearer %s", tokenRoot.getAccessToken());

                    // Store token for future operations
                    Token token = new Token();
                    token.setExpirationDate(date.plusSeconds(tokenRoot.getExpiresIn()));
                    token.setAccess_token(tokenStr);

                    storeToken(token);

                    return tokenStr;
                }
            } catch (Exception e) {
                Logger.writeLine(Log.ERROR, e, "HEREOAuthUtils: Error retrieving token");
            } finally {
                if (response != null)
                    response.close();
            }
        }

        return null;
    }

    private static String getTokenFromStorage() {
        Context context = SimpleLibrary.getInstance().getAppContext();
        SharedPreferences prefs = context.getSharedPreferences(WeatherAPI.HERE, Context.MODE_PRIVATE);

        if (prefs.contains(KEY_TOKEN)) {
            final String tokenJSON = prefs.getString(KEY_TOKEN, null);
            if (tokenJSON != null) {
                Token token = AsyncTask.await(new Callable<Token>() {
                    @Override
                    public Token call() {
                        return JSONParser.deserializer(tokenJSON, Token.class);
                    }
                });

                if (token != null && token.getExpirationDate().plusSeconds(-90).isAfter(ZonedDateTime.now(ZoneOffset.UTC))) {
                    return token.getAccess_token();
                }
            }
        }

        return null;
    }

    private static void storeToken(Token token) {
        // Shared Settings
        Context context = SimpleLibrary.getInstance().getAppContext();
        SharedPreferences prefs = context.getSharedPreferences(WeatherAPI.HERE, Context.MODE_PRIVATE);

        prefs.edit()
                .putString(KEY_TOKEN, JSONParser.serializer(token, Token.class))
                .apply();
    }
}
