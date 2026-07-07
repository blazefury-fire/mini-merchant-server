package com.mini_merchant.pay.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class PaymentCompletedEvent {
    private UUID transactionId;
    private UUID merchantId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime occurredAt;
}
