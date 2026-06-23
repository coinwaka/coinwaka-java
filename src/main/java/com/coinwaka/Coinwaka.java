package com.coinwaka;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;

/**
 * Official Java client for the Coinwaka Pay API. Requires Java 11+.
 *
 * <pre>{@code
 * Coinwaka coinwaka = new Coinwaka(System.getenv("COINWAKA_SECRET_KEY"));
 * Map<String, Object> params = new HashMap<>();
 * params.put("amount", "2500");
 * params.put("currency", "KES");
 * params.put("settlement_currency", "USDT");
 * Map<String, Object> intent = coinwaka.createPaymentIntent(params);
 * // redirect the customer to intent.get("checkout_url")
 * }</pre>
 */
public class Coinwaka {
    private static final Gson GSON = new Gson();
    private static final String API_VERSION = "2026-06-01";

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient http;
    private final int maxRetries;

    public Coinwaka(String apiKey) {
        this(apiKey, "https://api.coinwaka.com/v1");
    }

    public Coinwaka(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Coinwaka: apiKey is required.");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.maxRetries = 2;
    }

    public Map<String, Object> verifyKey() {
        return request("GET", "/auth/verify", null);
    }

    public Map<String, Object> rates() {
        return request("GET", "/rates", null);
    }

    public Map<String, Object> createQuote(Map<String, Object> params) {
        return request("POST", "/quotes", params);
    }

    public Map<String, Object> createPaymentIntent(Map<String, Object> params) {
        return request("POST", "/payment-intents", params);
    }

    public Map<String, Object> getPaymentIntent(String id) {
        return request("GET", "/payment-intents/" + enc(id), null);
    }

    public Map<String, Object> cancelPaymentIntent(String id) {
        return request("POST", "/payment-intents/" + enc(id) + "/cancel", null);
    }

    public Map<String, Object> refundRequest(String id, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("reason", reason);
        return request("POST", "/payment-intents/" + enc(id) + "/refund-request", body);
    }

    public Map<String, Object> createPaymentLink(Map<String, Object> params) {
        return request("POST", "/payment-links", params);
    }

    public Map<String, Object> getPaymentLink(String id) {
        return request("GET", "/payment-links/" + enc(id), null);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(String method, String path, Object body) {
        String json = body != null ? GSON.toJson(body) : null;
        String idempotencyKey = method.equals("POST") ? "cwk_java_" + UUID.randomUUID() : null;

        RuntimeException lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(30))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Accept", "application/json")
                        .header("Coinwaka-Version", API_VERSION)
                        .header("User-Agent", "coinwaka-java/0.1.0");
                if (json != null) {
                    b.header("Content-Type", "application/json");
                }
                if (idempotencyKey != null) {
                    b.header("Idempotency-Key", idempotencyKey);
                }
                b.method(method, json != null
                        ? HttpRequest.BodyPublishers.ofString(json)
                        : HttpRequest.BodyPublishers.noBody());

                HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
                int code = resp.statusCode();
                if (code >= 200 && code < 300) {
                    String respBody = resp.body();
                    if (respBody == null || respBody.isEmpty()) {
                        return new HashMap<>();
                    }
                    return GSON.fromJson(respBody, Map.class);
                }
                if ((code == 429 || code >= 500) && attempt < maxRetries) {
                    sleep(250L * (1L << attempt));
                    continue;
                }
                throw apiError(code, resp.body());
            } catch (CoinwakaException e) {
                throw e;
            } catch (Exception e) {
                lastError = new CoinwakaException(0, "network_error", "Request failed: " + e.getMessage(), null);
                if (attempt < maxRetries) {
                    sleep(250L * (1L << attempt));
                    continue;
                }
            }
        }
        throw lastError != null ? lastError : new CoinwakaException(0, "unknown", "Request failed", null);
    }

    @SuppressWarnings("unchecked")
    private static CoinwakaException apiError(int status, String body) {
        String code = "unknown";
        String message = "API error (HTTP " + status + ")";
        String requestId = null;
        try {
            Map<String, Object> parsed = GSON.fromJson(body, Map.class);
            Object errObj = parsed != null ? parsed.get("error") : null;
            if (errObj instanceof Map) {
                Map<String, Object> err = (Map<String, Object>) errObj;
                if (err.get("code") != null) code = String.valueOf(err.get("code"));
                if (err.get("message") != null) message = String.valueOf(err.get("message"));
                if (err.get("request_id") != null) requestId = String.valueOf(err.get("request_id"));
            }
        } catch (RuntimeException ignored) {
            // non-JSON body — keep the defaults
        }
        return new CoinwakaException(status, code, message, requestId);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
