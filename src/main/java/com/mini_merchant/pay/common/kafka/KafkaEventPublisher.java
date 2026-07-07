package com.mini_merchant.pay.common.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaEventPublisher implements IEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, String key, Object payload) {
        // The caller supplies the key; keying by a domain id (e.g. merchantId) keeps
        // all events for that id on the same partition (ordering).
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to topic {} (key {})", topic, key, ex);
                    } else {
                        log.info("Published event to topic {} (key {})", topic, key);
                    }
                });
    }
}
