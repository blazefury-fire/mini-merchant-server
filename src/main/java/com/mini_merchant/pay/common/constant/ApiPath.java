package com.mini_merchant.pay.common.constant;

public final class ApiPath {

    private ApiPath() {
    }

    public static final String API_V1 = "/api/v1";
    public static final String API_V2 = "/api/v2";
    public static final String MERCHANTS = API_V1 + "/merchants";
    public static final String AUTH = API_V1 + "/auth";
    public static final String PAYMENTS = API_V1 + "/payments";
    public static final String PING = "/ping";
}
