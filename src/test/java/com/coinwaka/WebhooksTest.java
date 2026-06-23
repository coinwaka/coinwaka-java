package com.coinwaka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class WebhooksTest {

    private static String sign(String secret, String ts, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal((ts + "." + body).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    void verifiesValidAndRejectsTamperedOrStale() throws Exception {
        String secret = "whsec_test_abc";
        String body = "{\"id\":\"evt_1\",\"type\":\"payment_intent.paid\",\"data\":{\"id\":\"pi_1\"}}";
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String sig = sign(secret, ts, body);

        Map<String, Object> event = Webhooks.verify(body, sig, ts, secret);
        assertEquals("payment_intent.paid", event.get("type"));

        assertThrows(CoinwakaException.class, () -> Webhooks.verify(body + "x", sig, ts, secret));
        assertThrows(CoinwakaException.class, () -> Webhooks.verify(body, sig, "1000", secret));
    }
}
