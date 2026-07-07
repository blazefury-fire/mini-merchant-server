package com.mini_merchant.pay.common.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.mini_merchant.pay.common.event.PaymentCompletedEvent;
import com.mini_merchant.pay.domain.notification.service.INotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaEventSubscriber {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventSubscriber.class);

    private final INotificationService iNotificationService;

    @KafkaListener(topics = "${app.kafka.topics.payment-completed:payment-completed}",
            groupId = "${app.kafka.consumer.group-id:notification-service}")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment-completed event for transaction {}", event.getTransactionId());
        iNotificationService.onPaymentCompleted(event);
    }
}
