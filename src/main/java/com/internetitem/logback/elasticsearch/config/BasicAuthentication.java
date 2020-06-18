package com.internetitem.logback.elasticsearch.config;

import org.apache.commons.codec.binary.Base64;

import java.net.HttpURLConnection;

public class BasicAuthentication implements Authentication {
    public void addAuth(HttpURLConnection urlConnection, String body) {
        String userInfo = urlConnection.getURL().getUserInfo();
        if (userInfo != null) {
            String basicAuth = "Basic " + new String(Base64.encodeBase64(userInfo.getBytes()));
            urlConnection.setRequestProperty("Authorization", basicAuth);
        }
    }
}
