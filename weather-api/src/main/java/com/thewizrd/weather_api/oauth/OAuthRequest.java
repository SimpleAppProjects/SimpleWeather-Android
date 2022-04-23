package com.thewizrd.weather_api.oauth;

import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.utils.StringUtils;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class OAuthRequest {
    public enum SignatureMethod {
        HMAC_SHA1,
        HMAC_SHA256
    }

    public enum HTTPRequestType {
        GET,
        POST
    }

    private static final String OAUTH_VERSION = "1.0";
    private static final String OAUTH_SIGNMETHOD_SHA1 = "HMAC-SHA1";
    private static final String OAUTH_SIGNMETHOD_SHA256 = "HMAC-SHA256";
    private final String consumerKey;
    private final String consumerSecret;
    private final SignatureMethod oAuthSignMethod;
    private final HTTPRequestType oAuthRequestType;

    public OAuthRequest(String consumerKey, String consumerSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.oAuthSignMethod = SignatureMethod.HMAC_SHA1;
        this.oAuthRequestType = HTTPRequestType.GET;
    }

    public OAuthRequest(String consumerKey, String consumerSecret, SignatureMethod signatureMethod, HTTPRequestType requestType) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.oAuthSignMethod = signatureMethod;
        this.oAuthRequestType = requestType;
    }

    @NonNull
    private static String uriEncode(String s) {
        return Uri.encode(s).replace("\\+", "%20");
    }

    private static String absoluteUriWithoutQuery(@NonNull Uri uri) {
        if (StringUtils.isNullOrEmpty(uri.getQuery())) {
            return uri.toString();
        }

        return uri.toString().replace("?" + uri.getQuery(), "");
    }

    @NonNull
    private static String getTimeStamp() {
        return (System.currentTimeMillis() / 1000) + "";
    }

    private static String getNonce() {
        return Base64.encodeToString((System.nanoTime() + "").getBytes(Charset.forName("UTF-8")), Base64.NO_WRAP);
    }

    public String getAuthorizationHeader(String url) throws InvalidKeyException, NoSuchAlgorithmException {
        return getAuthorizationHeader(url, false);
    }

    public String getAuthorizationHeader(String urlString, boolean addGrantType) throws NoSuchAlgorithmException, InvalidKeyException {
        final Uri url = Uri.parse(urlString);
        String lNonce = getNonce();
        String lTimes = getTimeStamp();
        String lCKey = consumerSecret + "&";
        String lSignMethod = oAuthSignMethod == SignatureMethod.HMAC_SHA256 ? OAUTH_SIGNMETHOD_SHA256 : OAUTH_SIGNMETHOD_SHA1;

        SortedMap<String, String> oauthParams = new TreeMap<>();

        if (!StringUtils.isNullOrEmpty(url.getQuery())) {
            for (String param_name : url.getQueryParameterNames()) {
                oauthParams.put(param_name, url.getQueryParameter(param_name));
            }
        }

        if (addGrantType) {
            oauthParams.put("grant_type", "client_credentials");
        }
        oauthParams.put("oauth_consumer_key", consumerKey);
        oauthParams.put("oauth_nonce", lNonce);
        oauthParams.put("oauth_signature_method", lSignMethod);
        oauthParams.put("oauth_timestamp", lTimes);
        oauthParams.put("oauth_version", OAUTH_VERSION);

        // Needs to be sorted || // note the sort order !!!
        Set<String> oauthParamKeys = oauthParams.keySet();

        StringBuilder signBuilder = new StringBuilder();
        for (String key : oauthParamKeys) {
            signBuilder.append(String.format("%s=%s&", uriEncode(key), uriEncode(oauthParams.get(key))));
        }
        signBuilder.deleteCharAt(signBuilder.length() - 1);

        String lSign = signBuilder.toString();

        lSign = (oAuthRequestType == HTTPRequestType.GET ? "GET&" : "POST&") +
                uriEncode(absoluteUriWithoutQuery(url)) + "&" + uriEncode(lSign);

        Mac lHasher;
        if (oAuthSignMethod == SignatureMethod.HMAC_SHA256) {
            lHasher = Mac.getInstance("HmacSHA256");
        } else {
            lHasher = Mac.getInstance("HmacSHA1");
        }

        SecretKeySpec secret_key = new SecretKeySpec(lCKey.getBytes(Charset.forName("UTF-8")), lHasher.getAlgorithm());
        lHasher.init(secret_key);
        lSign = Base64.encodeToString(lHasher.doFinal(lSign.getBytes(Charset.forName("UTF-8"))), Base64.NO_WRAP);

        return "OAuth " +
                "oauth_consumer_key=\"" + uriEncode(consumerKey) + "\", " +
                "oauth_nonce=\"" + uriEncode(lNonce) + "\", " +
                "oauth_timestamp=\"" + lTimes + "\", " +
                "oauth_signature_method=\"" + lSignMethod + "\", " +
                "oauth_signature=\"" + uriEncode(lSign) + "\", " +
                "oauth_version=\"" + OAUTH_VERSION + "\"";
    }
}
