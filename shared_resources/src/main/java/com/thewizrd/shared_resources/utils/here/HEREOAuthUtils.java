package com.thewizrd.shared_resources.utils.here;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.oauth.OAuthRequest;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Callable;

public class HEREOAuthUtils {
    public static final String HERE_OAUTH_URL = "https://account.api.here.com/oauth2/token";
    private static final String KEY_TOKEN = "token";

    public static String getBearerToken(boolean forceRefresh) {
        if (!forceRefresh) {
            String token = new AsyncTask<String>().await(new Callable<String>() {
                @Override
                public String call() {
                    return getTokenFromStorage();
                }
            });
            if (token != null)
                return token;
            else
                forceRefresh = true;
        }

        if (forceRefresh) {
            OAuthRequest oAuthRequest = new OAuthRequest(Keys.getHERECliID(), Keys.getHERECliSecr(),
                    OAuthRequest.SignatureMethod.HMAC_SHA256, OAuthRequest.HTTPRequestType.POST);

            HttpURLConnection client = null;

            try {
                String authorization = oAuthRequest.getAuthorizationHeader(HERE_OAUTH_URL, true);

                client = (HttpURLConnection) new URL(HERE_OAUTH_URL).openConnection();
                client.setRequestMethod("POST");
                client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
                client.setReadTimeout(Settings.READ_TIMEOUT);

                // Add headers to request
                client.addRequestProperty("Authorization", authorization);
                client.addRequestProperty("Cache-Control", "no-cache");

                // Connect to webstream
                String formParams = "grant_type=client_credentials";
                client.setDoOutput(true);

                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                dos.writeBytes(formParams);
                dos.close();

                final InputStream stream = client.getInputStream();
                String dateField = client.getHeaderField("Date");
                ZonedDateTime date = ZonedDateTime.parse(dateField, DateTimeFormatter.RFC_1123_DATE_TIME);

                TokenRootobject tokenRoot = new AsyncTask<TokenRootobject>().await(new Callable<TokenRootobject>() {
                    @Override
                    public TokenRootobject call() {
                        return JSONParser.deserializer(stream, TokenRootobject.class);
                    }
                });

                if (tokenRoot != null) {
                    String tokenStr = String.format(Locale.ROOT, "Bearer %s", tokenRoot.getAccessToken());

                    // Store token for future operations
                    Token token = new Token();
                    token.setExpirationDate(date.plusSeconds(tokenRoot.getExpiresIn()));
                    token.setAccessToken(tokenStr);

                    storeToken(token);

                    return tokenStr;
                }
            } catch (Exception e) {
                Logger.writeLine(Log.ERROR, e, "HEREOAuthUtils: Error retrieving token");
            } finally {
                if (client != null)
                    client.disconnect();
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
                Token token = new AsyncTask<Token>().await(new Callable<Token>() {
                    @Override
                    public Token call() {
                        return JSONParser.deserializer(tokenJSON, Token.class);
                    }
                });

                if (token != null && token.getExpirationDate().plusSeconds(-90).compareTo(ZonedDateTime.now(ZoneOffset.UTC)) > 0) {
                    return token.getAccessToken();
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
