package com.rikkeibank.common.constant;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_NAME = "X-User-Username";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String TOPIC_TRANSACTION_EVENTS = "transaction-events";
}
