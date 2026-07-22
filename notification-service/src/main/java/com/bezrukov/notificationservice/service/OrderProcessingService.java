package com.bezrukov.notificationservice.service;

import event.OrderCreatedEvent;

public interface OrderProcessingService {
    void processOrder(OrderCreatedEvent event);
}
