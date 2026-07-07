package com.mini_merchant.pay.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.mini_merchant.pay.common.event.PaymentCompletedEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(NotificationService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void onPaymentCompleted_logsSimulatedNotification() {
        UUID merchantId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .transactionId(transactionId)
                .merchantId(merchantId)
                .amount(new BigDecimal("100.00"))
                .currency("VND")
                .status("SUCCESS")
                .occurredAt(LocalDateTime.now())
                .build();

        notificationService.onPaymentCompleted(event);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent logEvent = appender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage())
                .contains(merchantId.toString())
                .contains(transactionId.toString())
                .contains("VND");
    }
}
