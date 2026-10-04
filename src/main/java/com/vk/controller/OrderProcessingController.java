package com.vk.controller;

import com.vk.records.request.OrderRequest;
import com.vk.records.response.ApiResponse;
import com.vk.records.response.OrderResponse;
import com.vk.service.OrderProcessService;
import com.vk.service.OrderProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderProcessingController {
    private final OrderProcessingService orderProcessingService;
    private final OrderProcessService orderProcessService;

    public OrderProcessingController(OrderProcessingService orderProcessingService, OrderProcessService orderProcessService) {
        this.orderProcessingService = orderProcessingService;
        this.orderProcessService = orderProcessService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@RequestBody OrderRequest orderRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(orderProcessService.processOrder(orderRequest), "Order placed successfully.")
        );
        /*return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(orderProcessingService.placeAnOrder(orderRequest), "Order placed successfully.")
        );*/
    }
}
