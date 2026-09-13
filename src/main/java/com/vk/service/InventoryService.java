package com.vk.service;

import com.vk.entities.Product;
import com.vk.exception.ProductNotFoundException;
import com.vk.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateProductDetails(Product product) {
        // forcefully throwing to simulate use of tx
        if (product.getPrice().intValue() > 5000) {
            throw new RuntimeException("DB Crashed....");
        }
        inventoryRepository.save(product);
    }

    public Product getProduct(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }
}
