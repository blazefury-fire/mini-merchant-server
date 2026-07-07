package com.mini_merchant.pay.domain.notification.service;

import com.mini_merchant.pay.common.event.PaymentCompletedEvent;

public interface INotificationService {
    void onPaymentCompleted(PaymentCompletedEvent event);
}
