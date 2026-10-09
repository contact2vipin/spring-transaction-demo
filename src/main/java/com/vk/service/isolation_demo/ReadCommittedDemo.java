package com.vk.service.isolation_demo;

import com.vk.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReadCommittedDemo {

    @Autowired
    private ProductService productService;

    public void testReadCommitted(Long id) throws InterruptedException {

        // Start Transaction A (Thread 1) to update the stock and not commit, then
        Thread threadA = new Thread(() -> {
            try {
                productService.updateStock(id,50); // Change stock to 1
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Start Transaction B (Thread 2) to read the stock
        Thread threadB = new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait a moment to ensure Thread A starts
                int stock = productService.checkStock(id);
                System.out.println("Stock read by Transaction B: "+ stock);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        threadA.start();
        threadB.start();

        // Wait for threads to complete
        threadA.join();
        threadB.join();
    }
}
