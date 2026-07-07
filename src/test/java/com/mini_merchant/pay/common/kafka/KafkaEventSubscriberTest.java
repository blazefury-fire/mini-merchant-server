package com.mini_merchant.pay.common.kafka;

import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mini_merchant.pay.common.event.PaymentCompletedEvent;
import com.mini_merchant.pay.domain.notification.service.INotificationService;

@ExtendWith(MockitoExtension.class)
class KafkaEventSubscriberTest {

    @Mock
    private INotificationService iNotificationService;

    @InjectMocks
    private KafkaEventSubscriber kafkaEventSubscriber;

    @Test
    void onPaymentCompleted_delegatesToNotificationService() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("VND")
                .status("SUCCESS")
                .occurredAt(LocalDateTime.now())
                .build();

        kafkaEventSubscriber.onPaymentCompleted(event);

        verify(iNotificationService).onPaymentCompleted(event);
    }
}
