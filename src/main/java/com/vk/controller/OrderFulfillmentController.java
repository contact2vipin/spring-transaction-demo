package com.vk.controller;

import com.vk.records.request.OrderRequest;
import com.vk.records.response.ApiResponse;
import com.vk.records.response.OrderResponse;
import com.vk.service.OrderFulfillmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/async-demo")
public class OrderFulfillmentController {

    @Autowired
    private OrderFulfillmentService orderFulfillmentService;

    @PostMapping("/order-process")
    public ResponseEntity<ApiResponse<OrderResponse>> processOrder(@RequestBody OrderRequest orderRequest) throws InterruptedException {
        OrderResponse orderResponse = orderFulfillmentService.processOrder(orderRequest);
        orderFulfillmentService.notifyUser(orderRequest);
        orderFulfillmentService.assignVendor(orderRequest);
        orderFulfillmentService.packaging(orderRequest);
        orderFulfillmentService.assignDeliveryPartner(orderRequest);
        orderFulfillmentService.assignTrailerAndDispatch(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(orderResponse, "Order placed successfully.")
        );
    }
}
