package com.vk.service;

import com.vk.entities.Order;
import com.vk.records.request.OrderRequest;
import com.vk.records.response.OrderResponse;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessService {

    private final NotificationService notificationService;
    private final OrderProcessingService orderProcessingService;

    public OrderProcessService(NotificationService notificationService, OrderProcessingService orderProcessingService) {
        this.notificationService = notificationService;
        this.orderProcessingService = orderProcessingService;
    }

    public OrderResponse processOrder(OrderRequest orderRequest) {
        // Step 1: Place the order
        OrderResponse savedOrder = orderProcessingService.placeAnOrder(orderRequest);
        Order order = Order.builder()
                .id(savedOrder.id())
                .build();
        // Step 2: Send notification (non-transactional)
        notificationService.sendOrderConfirmationNotification(order);
        return savedOrder;
    }
}
