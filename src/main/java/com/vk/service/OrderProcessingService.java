package com.vk.service;

import com.vk.entities.Order;
import com.vk.entities.Product;
import com.vk.records.request.OrderRequest;
import com.vk.records.response.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderProcessingService {

    private final OrderService orderService;
    private final InventoryService inventoryService;

    public OrderProcessingService(OrderService orderService, InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    // REQUIRED: join an existing transaction or create a new one if not exist
    @Transactional(propagation = Propagation.REQUIRED)
    public OrderResponse placeAnOrder(OrderRequest orderRequest) {
        // get Product inventory
        Product product = inventoryService.getProduct(orderRequest.productId());

        // validate stock availability
        validateStockAvailability(orderRequest, product);

        Order order = Order.builder()
                .quantity(orderRequest.quantity())
                .productId(orderRequest.productId())
                .build();

        // Update total price in order entity
        order.setTotalPrice(BigDecimal.valueOf(order.getQuantity()).multiply(product.getPrice()));

        // save order
        Order savedOrder = orderService.saveOrder(order);

        // Update stock in inventory
        updateInventoryStock(order, product);

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalPrice()
        );
    }

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
