package com.mini_merchant.pay.common.kafka;

public interface IEventPublisher {
    void publish(String topic, String key, Object payload);
}
