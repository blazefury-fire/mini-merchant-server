package com.mini_merchant.pay.common.constant;

import java.util.Map;
import java.util.Set;

public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED;

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(SUCCESS, FAILED),
            SUCCESS, Set.of(), // terminal — không đi đâu được nữa
            FAILED, Set.of() // terminal — không đi đâu được nữa
    );

    public boolean canTransitionTo(TransactionStatus newStatus) {
        return ALLOWED_TRANSITIONS.get(this).contains(newStatus);
    }
}
