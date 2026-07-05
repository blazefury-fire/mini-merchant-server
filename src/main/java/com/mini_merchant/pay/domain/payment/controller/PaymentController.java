package com.mini_merchant.pay.domain.payment.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mini_merchant.pay.common.constant.ApiPath;
import com.mini_merchant.pay.common.dto.ApiResponse;
import com.mini_merchant.pay.common.security.PaymentApiKeyAuthFilter;

@RestController
@RequestMapping(ApiPath.PAYMENTS)
public class PaymentController {

    @GetMapping("/ping")
    public ApiResponse<UUID> ping(
            @RequestAttribute(PaymentApiKeyAuthFilter.MERCHANT_ID_ATTRIBUTE) UUID merchantId) {
        return ApiResponse.success(merchantId);
    }
}
