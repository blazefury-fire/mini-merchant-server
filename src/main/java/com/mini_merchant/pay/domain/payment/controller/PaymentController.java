package com.mini_merchant.pay.domain.payment.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mini_merchant.pay.common.constant.ApiPath;
import com.mini_merchant.pay.common.dto.ApiResponse;
import com.mini_merchant.pay.common.security.PaymentApiKeyAuthFilter;
import com.mini_merchant.pay.domain.payment.dto.transaction.CreateTransactionReqModel;
import com.mini_merchant.pay.domain.payment.dto.transaction.CreateTransactionResModel;
import com.mini_merchant.pay.domain.payment.service.ITransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiPath.PAYMENTS)
@RequiredArgsConstructor
public class PaymentController {

    private final ITransactionService iTransactionService;

    @GetMapping("/ping")
    public ApiResponse<UUID> ping(
            @RequestAttribute(PaymentApiKeyAuthFilter.MERCHANT_ID_ATTRIBUTE) UUID merchantId) {
        return ApiResponse.success(merchantId);
    }

    @PostMapping("/transaction")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateTransactionResModel> createTransaction(
            @RequestAttribute(PaymentApiKeyAuthFilter.MERCHANT_ID_ATTRIBUTE) UUID merchantId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTransactionReqModel request) {
        return ApiResponse.created(
                iTransactionService.createTransaction(merchantId, idempotencyKey, request));
    }
}
