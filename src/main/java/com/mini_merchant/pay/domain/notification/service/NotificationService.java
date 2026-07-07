package com.mini_merchant.pay.domain.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mini_merchant.pay.common.event.PaymentCompletedEvent;

@Service
public class NotificationService implements INotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Override
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        // Simulated notification — no persistence or external call (service-only slice).
        log.info("Notifying merchant {} of completed payment {} — amount {} {} (status {}, at {})",
                event.getMerchantId(),
                event.getTransactionId(),
                event.getAmount(),
                event.getCurrency(),
                event.getStatus(),
                event.getOccurredAt());
    }
}
