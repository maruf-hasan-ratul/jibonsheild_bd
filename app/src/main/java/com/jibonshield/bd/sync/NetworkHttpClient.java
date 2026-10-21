package com.jibonshield.bd.sync;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * Resilient, Android-compatible HTTP client for optional synchronization.
 * Handles timeouts, DNS errors, rate limiting, and server unavailability safely
 * without crashing or throwing unhandled runtime exceptions.
 */
public class NetworkHttpClient {

    private static final String TAG = "NetworkHttpClient";
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 8000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 8000;
    public static final String USER_AGENT = "JibonShieldBD/1.0 (Android; Offline-First Disaster Resilience)";

    public static class HttpResponse {
        private final int statusCode;
        private final String body;
        private final String errorMessage;
        private final boolean isSuccess;
        private final ErrorType errorType;

        public enum ErrorType {
            NONE,
            NO_CONNECTIVITY,
            DNS_FAILURE,
            TIMEOUT,
            HTTP_ERROR,
            RATE_LIMITED,
            AUTH_FAILURE,
            SERVER_UNAVAILABLE,
            UNKNOWN
        }

        public HttpResponse(int statusCode, String body, String errorMessage, ErrorType errorType) {
            this.statusCode = statusCode;
            this.body = body != null ? body : "";
            this.errorMessage = errorMessage != null ? errorMessage : "";
            this.isSuccess = (statusCode >= 200 && statusCode < 300);
            this.errorType = errorType;
        }

        public static HttpResponse ok(int code, String body) {
            return new HttpResponse(code, body, null, ErrorType.NONE);
        }

        public static HttpResponse error(int code, String message, ErrorType type) {
            return new HttpResponse(code, null, message, type);
        }

        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isSuccess() { return isSuccess; }
        public ErrorType getErrorType() { return errorType; }
    }

    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public NetworkHttpClient() {
        this(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    public NetworkHttpClient(int connectTimeoutMs, int readTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Executes an HTTP GET request synchronously.
     * MUST be called from a background worker thread.
     */
    public HttpResponse executeGet(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return HttpResponse.error(0, "Invalid endpoint URL", HttpResponse.ErrorType.UNKNOWN);
        }

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString.trim());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json, text/plain; q=0.9, */*; q=0.8");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);

            int responseCode = connection.getResponseCode();

            if (responseCode == 429) {
                return HttpResponse.error(responseCode, "Rate limit exceeded (HTTP 429)", HttpResponse.ErrorType.RATE_LIMITED);
            } else if (responseCode == 401 || responseCode == 403) {
                return HttpResponse.error(responseCode, "Authentication or access forbidden (HTTP " + responseCode + ")", HttpResponse.ErrorType.AUTH_FAILURE);
            } else if (responseCode == 503 || responseCode == 502 || responseCode == 504) {
                return HttpResponse.error(responseCode, "Remote server unavailable or timeout (HTTP " + responseCode + ")", HttpResponse.ErrorType.SERVER_UNAVAILABLE);
            } else if (responseCode >= 400) {
                return HttpResponse.error(responseCode, "Remote server error HTTP " + responseCode, HttpResponse.ErrorType.HTTP_ERROR);
            }

            InputStream inputStream = connection.getInputStream();
            if (inputStream == null) {
                return HttpResponse.error(responseCode, "Empty response stream", HttpResponse.ErrorType.HTTP_ERROR);
            }

            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append('\n');
            }

            return HttpResponse.ok(responseCode, responseBuilder.toString().trim());

        } catch (UnknownHostException e) {
            Log.w(TAG, "DNS resolution failed: " + e.getMessage());
            return HttpResponse.error(0, "DNS error: Unable to resolve host (" + e.getMessage() + ")", HttpResponse.ErrorType.DNS_FAILURE);
        } catch (SocketTimeoutException e) {
            Log.w(TAG, "Connection/Read timeout: " + e.getMessage());
            return HttpResponse.error(0, "Connection timed out (" + e.getMessage() + ")", HttpResponse.ErrorType.TIMEOUT);
        } catch (SecurityException e) {
            Log.w(TAG, "Security permission error: " + e.getMessage());
            return HttpResponse.error(0, "Network permission denied", HttpResponse.ErrorType.AUTH_FAILURE);
        } catch (Exception e) {
            Log.w(TAG, "HTTP execution error: " + e.getMessage());
            return HttpResponse.error(0, "Network error: " + e.getMessage(), HttpResponse.ErrorType.UNKNOWN);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
