# coinwaka-java

Official Java client for the [Coinwaka Pay API](https://developers.coinwaka.com).
Java 11+ (uses the JDK HTTP client; Gson for JSON).

```xml
<dependency>
  <groupId>com.coinwaka</groupId>
  <artifactId>coinwaka-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quickstart

```java
import com.coinwaka.Coinwaka;
import java.util.Map;

Coinwaka coinwaka = new Coinwaka(System.getenv("COINWAKA_SECRET_KEY"));

Map<String, Object> intent = coinwaka.createPaymentIntent(Map.of(
    "amount", "2500",
    "currency", "KES",
    "settlement_currency", "USDT",
    "payment_methods", java.util.List.of("mpesa", "card", "coinwaka_balance")
));
// redirect the customer to intent.get("checkout_url")
```

A `cwk_test_…` key runs in sandbox; a `cwk_live_…` key is live. Every POST sends
an `Idempotency-Key` automatically, so the built-in 429/5xx retry never
double-charges. Errors are `CoinwakaException` (`.code`, `.getMessage()`,
`.requestId`).

## Methods

```java
coinwaka.verifyKey();
coinwaka.rates();
coinwaka.createQuote(params);
coinwaka.createPaymentIntent(params);
coinwaka.getPaymentIntent(id);
coinwaka.cancelPaymentIntent(id);
coinwaka.refundRequest(id, reason);
coinwaka.createPaymentLink(params);
coinwaka.getPaymentLink(id);
```

## Verify webhooks

```java
import com.coinwaka.Webhooks;

Map<String, Object> event = Webhooks.verify(
    rawBody,
    request.getHeader("Coinwaka-Signature"),
    request.getHeader("Coinwaka-Timestamp"),
    System.getenv("COINWAKA_WEBHOOK_SECRET")
);
if ("payment_intent.paid".equals(event.get("type"))) {
    // fulfil the order; dedupe on event.get("id")
}
```

## License

MIT
