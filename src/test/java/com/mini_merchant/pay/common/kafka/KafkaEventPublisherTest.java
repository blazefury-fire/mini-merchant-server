package com.mini_merchant.pay.common.kafka;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaEventPublisher kafkaEventPublisher;

    @BeforeEach
    void setUp() {
        kafkaEventPublisher = new KafkaEventPublisher(kafkaTemplate);
    }

    @Test
    void publish_sendsPayloadToTopicWithKey() {
        Object payload = new Object();
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("payment-completed"), eq("merchant-1"), eq(payload)))
                .thenReturn(future);

        kafkaEventPublisher.publish("payment-completed", "merchant-1", payload);

        verify(kafkaTemplate).send("payment-completed", "merchant-1", payload);
    }
}
