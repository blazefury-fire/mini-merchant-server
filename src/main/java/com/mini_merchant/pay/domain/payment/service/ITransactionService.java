package com.mini_merchant.pay.domain.payment.service;

import java.util.UUID;

import com.mini_merchant.pay.domain.payment.dto.transaction.CreateTransactionReqModel;
import com.mini_merchant.pay.domain.payment.dto.transaction.CreateTransactionResModel;

public interface ITransactionService {
    CreateTransactionResModel createTransaction(UUID merchantId, String idempotencyKey,
            CreateTransactionReqModel request);
}
