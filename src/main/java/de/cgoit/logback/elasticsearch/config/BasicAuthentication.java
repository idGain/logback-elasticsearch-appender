package de.cgoit.logback.elasticsearch.config;

import org.apache.commons.codec.binary.Base64;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicAuthentication implements Authentication {

    private static final Pattern ENV_VARIABLE_PATTERN = Pattern.compile("^\\$\\{env\\.(.+)\\}$");

    private boolean authFromUrl = true;
    private String username = null;
    private String password = null;

    public BasicAuthentication() {

    }

    public BasicAuthentication(String username, String password) {
        this.authFromUrl = false;
        this.username = resolveEnvVariable(username);
        this.password = resolveEnvVariable(password);
    }

    static String getFromEnv(String envVariableName) {
        return System.getenv(envVariableName);
    }

    @Override
    public void addAuth(HttpURLConnection urlConnection, String body) {
        if (authFromUrl) {
            String userInfo = urlConnection.getURL().getUserInfo();
            if (userInfo != null) {
                String basicAuth = "Basic " + new String(Base64.encodeBase64(userInfo.getBytes()));
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }
        } else {
            if (username != null && password != null) {
                String basicAuth = "Basic " + new String(Base64.encodeBase64(String.format("%s:%s", username, password).getBytes()));
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }
        }
    }

    private String resolveEnvVariable(String variable) {
        String resolved = variable;
        if (variable != null) {
            Matcher matcher = ENV_VARIABLE_PATTERN.matcher(variable);
            if (matcher.matches() && matcher.groupCount() == 1) {
                String fromEnv = getFromEnv(matcher.group(1));
                if (fromEnv != null) {
                    resolved = fromEnv;
                }
            }
        }
        return resolved;
    }
}
