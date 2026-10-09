package com.vk.service;

import com.vk.entities.Order;
import com.vk.entities.Product;
import com.vk.records.request.OrderRequest;
import com.vk.records.response.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderProcessingService {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final PaymentValidatorService paymentValidatorService;
    private final NotificationService notificationService;
    private final ProductRecommendationService productRecommendationService;

    public OrderProcessingService(
            OrderService orderService,
            InventoryService inventoryService,
            AuditLogService auditLogService,
            PaymentValidatorService paymentValidatorService,
            NotificationService notificationService,
            ProductRecommendationService productRecommendationService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.paymentValidatorService = paymentValidatorService;
        this.notificationService = notificationService;
        this.productRecommendationService = productRecommendationService;
    }

    // REQUIRED: join an existing transaction or create a new one if not exist
    // REQUIRED_NEW: Always create new transaction, suspending if any existing transaction
    // MANDATORY: Require an existing transaction, if nothing found it will throw an exception
    // NEVER: Ensure the method will run without transaction, throw an exception if found
    // NOT_SUPPORTED: Execute method without transaction,suspending any active transaction
    // SUPPORTS: Supports if there is any active transaction, if not then execute without transaction
    // NESTED: Executes within a nested transaction, allowing the nested transaction to roll back independently without affecting the outer transaction
    /**ISOLATION: controls the visibility of changes made by one transaction to other transaction*/
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT)  // Outer Transaction
    public OrderResponse placeAnOrder(OrderRequest orderRequest) {
        // get Product inventory
        Product product = inventoryService.getProduct(orderRequest.productId());

        // validate stock availability
        validateStockAvailability(orderRequest, product);

        Order order = Order.builder()
                .quantity(orderRequest.quantity())
                .productId(orderRequest.productId())
                .trackingId(UUID.randomUUID().toString())
                .build();

        // Update total price in order entity
        order.setTotalPrice(BigDecimal.valueOf(order.getQuantity()).multiply(product.getPrice()));
        Order savedOrder = null;
        try {
            // save order
            savedOrder = orderService.saveOrder(order);
            // Update stock in inventory
            updateInventoryStock(order, product);
            // Required new transaction
            auditLogService.logAuditDetails(order, "Order placement succeeded");
        } catch (Exception ex) {
            // Required new transaction
            auditLogService.logAuditDetails(order, "Order placement failed");
        }

        productRecommendationService.getRecommendations();

        // Here we can retry or send confirmation to user etc. but every time when we retry we don't want to send notification to user.
        // Because of this, I don't want my sendOrderConfirmationNotification to be executed as a part of any transaction. that's why I commented this
        // and added @Transactional(propagation = Propagation.NEVER), so that no one should accidentally add this method within any transaction.
        // processOrder method in this class is an alternate way of this implementation as it will throw exception. as we are calling it inside a transaction.
        // notificationService.sendOrderConfirmationNotification(order); // throw "Existing transaction found for transaction marked with propagation 'never'"

        // validate payment - MANDATORY
        paymentValidatorService.validatePayment(order); // This only needs existing transaction, if an exception occurs inside it, it will roll back the whole transaction

        //It should also work, when we comment @Transactional code written at this method
        getCustomerDetails(); // Should work with or without any active transaction

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalPrice(),
                savedOrder.getTrackingId()
        );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void getCustomerDetails() {
        System.out.println("Customer details fetched!!!");
    }

    // Note: @Transactional works when the method is invoked through the Spring proxy, not when another method in the same class directly calls it.
    // Show below method won't trigger transaction and because of this we will get "No existing transaction found for transaction marked with propagation 'mandatory'"
    // Add this method, just to verify the same behaviour, for correct solution i have added OrderProcessService and added this method there.
    // Call this method after placeAnOrder is successfully completed
    /*public OrderResponse processOrder(OrderRequest orderRequest) {
        // Step 1: Place the order
        OrderResponse savedOrder = placeAnOrder(orderRequest);
        Order order = Order.builder()
                .id(savedOrder.id())
                .build();
        // Step 2: Send notification (non-transactional)
        notificationService.sendOrderConfirmationNotification(order);
        return savedOrder;
    }*/

    private static void validateStockAvailability(OrderRequest orderRequest, Product product) {
        if (orderRequest.quantity() > product.getStockQuantity()) {
            throw new RuntimeException("Insufficient stock!");
        }
    }

    private void updateInventoryStock(Order order, Product product) {
        int availableStock = product.getStockQuantity() - order.getQuantity();
        product.setStockQuantity(availableStock);
        inventoryService.updateProductDetails(product);
    }
}
