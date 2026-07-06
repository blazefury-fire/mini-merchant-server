package com.mini_merchant.pay.domain.payment.dto.transaction;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class CreateTransactionResModel {
    private UUID id;
    private String status;
}
