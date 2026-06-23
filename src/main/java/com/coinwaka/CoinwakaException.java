package com.coinwaka;

/** Thrown for any non-2xx Coinwaka API response (the standard error envelope). */
public class CoinwakaException extends RuntimeException {
    public final int statusCode;
    public final String code;
    public final String requestId;

    public CoinwakaException(int statusCode, String code, String message, String requestId) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
        this.requestId = requestId;
    }
}
