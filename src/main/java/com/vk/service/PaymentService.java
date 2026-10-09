package com.vk.service;

import com.vk.records.request.OrderRequest;
import com.vk.records.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class PaymentService {
    public OrderResponse processPayment(OrderRequest orderRequest) throws InterruptedException {
        log.info("initiate payment for order " + orderRequest.productId());
        // call actual payment gateway
        Thread.sleep(2000L);
        log.info("Complete payment for order" + orderRequest.productId());
        return new OrderResponse(1L, orderRequest.productId(), orderRequest.quantity(), orderRequest.price(), UUID.randomUUID().toString());
    }
}
