package com.vk.controller;

import com.vk.records.request.OrderRequest;
import com.vk.records.response.ApiResponse;
import com.vk.records.response.OrderResponse;
import com.vk.service.OrderProcessService;
import com.vk.service.OrderProcessingService;
import com.vk.service.isolation_demo.ReadCommittedDemo;
import com.vk.service.isolation_demo.ReadUnCommittedDemo;
import com.vk.service.isolation_demo.RepeatableReadDemo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderProcessingController {
    private final OrderProcessingService orderProcessingService;
    private final OrderProcessService orderProcessService;
    private final ReadUnCommittedDemo readUnCommittedDemo;
    private final ReadCommittedDemo readCommittedDemo;
    private final RepeatableReadDemo repeatableReadDemo;

    public OrderProcessingController(
            OrderProcessingService orderProcessingService,
            OrderProcessService orderProcessService,
            ReadUnCommittedDemo readUnCommittedDemo,
            ReadCommittedDemo readCommittedDemo,
            RepeatableReadDemo repeatableReadDemo) {
        this.orderProcessingService = orderProcessingService;
        this.orderProcessService = orderProcessService;
        this.readUnCommittedDemo = readUnCommittedDemo;
        this.readCommittedDemo = readCommittedDemo;
        this.repeatableReadDemo = repeatableReadDemo;
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

    @GetMapping("/isolation-read-uncommitted")
    public String testIsolationReadUnCommitted() throws InterruptedException {
        readUnCommittedDemo.testReadUnCommitted(1L);
        return "Success: See the console.";
    }

    @GetMapping("/isolation-read-committed")
    public String testIsolationReadCommitted() throws InterruptedException {
        readCommittedDemo.testReadCommitted(1L);
        return "Success: See the console.";
    }

    @GetMapping("/isolation-repeatable-read")
    public String testIsolationRepeatableRead() throws InterruptedException {
        repeatableReadDemo.testRepeatableRead(1L);
        return "Success: See the console.";
    }
}
