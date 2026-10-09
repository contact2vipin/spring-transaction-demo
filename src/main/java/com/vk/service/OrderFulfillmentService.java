package com.vk.service;

import com.vk.records.request.OrderRequest;
import com.vk.records.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderFulfillmentService {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    // All Required process
    /*
    1. Inventory service check order availability
    2. Process payment for order
    3. Notify to the user
    4. Packaging
    5. assign delivery partner
    6. assign trailer
    7. dispatch product
    * */

    public OrderResponse processOrder(OrderRequest orderRequest) throws InterruptedException {
        OrderResponse orderResponse = null;
        if(inventoryService.checkProductAvailability(orderRequest.productId())) {
            // handle exception here
            orderResponse = paymentService.processPayment(orderRequest);
        } else {
            throw new RuntimeException("Technical issue please retry");
        }
        return orderResponse;
    }

    @Async("asyncTaskExecutor")
    public void notifyUser(OrderRequest orderRequest) throws InterruptedException {
        Thread.sleep(4000L);
        log.info("Notify to the user "+ Thread.currentThread().getName());
    }

    @Async("asyncTaskExecutor")
    public void assignVendor(OrderRequest orderRequest) throws InterruptedException {
        Thread.sleep(5000L);
        log.info("Assign order to vendor "+ Thread.currentThread().getName());
    }

    @Async("asyncTaskExecutor")
    public void packaging(OrderRequest orderRequest) throws InterruptedException {
        Thread.sleep(2000L);
        log.info("Order packaging completed "+ Thread.currentThread().getName());
    }

    @Async("asyncTaskExecutor")
    public void assignDeliveryPartner(OrderRequest orderRequest) throws InterruptedException {
        Thread.sleep(2000L);
        log.info("Delivery partner assigned "+ Thread.currentThread().getName());
    }

    @Async("asyncTaskExecutor")
    public void assignTrailerAndDispatch(OrderRequest orderRequest) throws InterruptedException {
        Thread.sleep(2000L);
        log.info("Trailer assigned and order dispatched "+ Thread.currentThread().getName());
    }
}
