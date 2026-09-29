package com.bezrukov.notificationservice.service;

import com.bezrukov.common.event.OrderConfirmedEvent;

public interface OrderProcessingService {
    void saveConfirmedOrder(OrderConfirmedEvent orderConfirmedEvent);
}
