package com.vk.service;

import com.vk.entities.Product;
import com.vk.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
public class ProductService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private EntityManager entityManager;

    // Transaction A:
//    @Transactional(isolation = Isolation.READ_UNCOMMITTED) // It allows dirty reads, which create data inconsistency in application transaction
//    @Transactional(isolation = Isolation.READ_COMMITTED) // It allows dirty reads, which create data inconsistency in application transaction
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updateStock(Long productId, int stock) throws InterruptedException {
        // Retrieve the product and update its stock

        Product product = inventoryRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found!"));
        product.setStockQuantity(stock);

        inventoryRepository.save(product);
        entityManager.flush(); // Ensure the update is sent to the DB

        // Simulate a long-running transaction (does not commit yet)
        System.out.println("Transaction A: Stock updated to "+ stock);
        Thread.sleep(5000);
//        System.out.println("Transaction A: Rolling back the update");
//        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // explicit rollback
//        System.out.println("Transaction A: Committed the update");
    }

    // Transaction B: Read stock
//    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Transactional(isolation = Isolation.READ_COMMITTED) // Gives last committed value
    public int checkStock(Long productId) {
        // Retrieve the product and read its stock (potentially dirty read)
        Product product = inventoryRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found!"));

        System.out.println("Transaction B: Read stock as "+ product.getStockQuantity());
        return product.getStockQuantity();
    }

    // Transaction C: Read stock multiple times
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void fetchStock(Long productId) {
        // Retrieve the product and read its stock (potentially dirty read)

        // First Read
        Product product1 = inventoryRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found!"));

        System.out.println("Transaction C: Read stock as "+ product1.getStockQuantity());
        // Simulate a delay to allow Transaction A to update the stock
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Second Read
        Product product2 = inventoryRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found!"));

        System.out.println("Transaction C: Read stock as "+ product2.getStockQuantity());
        // Simulate a delay to allow Transaction A to update the stock

    }
}
