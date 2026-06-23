package com.coinwaka;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;

/** Webhook signature verification. */
public final class Webhooks {
    private static final Gson GSON = new Gson();
    private static final long DEFAULT_TOLERANCE_SEC = 300;

    private Webhooks() {
    }

    /**
     * Verify the HMAC signature over {@code <timestamp>.<rawBody>} and the
     * timestamp freshness, then return the decoded event. Throws
     * CoinwakaException on a bad signature or stale timestamp.
     *
     * @param payload   the RAW request body
     * @param signature the Coinwaka-Signature header (hex HMAC)
     * @param timestamp the Coinwaka-Timestamp header (unix seconds)
     * @param secret    the endpoint signing secret
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> verify(String payload, String signature, String timestamp, String secret) {
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw invalid();
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > DEFAULT_TOLERANCE_SEC) {
            throw invalid();
        }
        String expected = hmacSha256Hex(secret, timestamp + "." + payload);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature == null ? new byte[0] : signature.getBytes(StandardCharsets.UTF_8))) {
            throw invalid();
        }
        return GSON.fromJson(payload, Map.class);
    }

    private static CoinwakaException invalid() {
        return new CoinwakaException(400, "invalid_signature", "Invalid webhook signature.", null);
    }

    private static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new CoinwakaException(500, "hmac_error", e.getMessage(), null);
        }
    }
}
