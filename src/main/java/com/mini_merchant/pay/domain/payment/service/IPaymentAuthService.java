package com.mini_merchant.pay.domain.payment.service;

import com.mini_merchant.pay.entity.Merchants;

public interface IPaymentAuthService {
    Merchants authenticate(String apiKey, String signature, String timestamp, String method, String path);
}
