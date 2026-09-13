package com.vk.service;

import com.vk.entities.Product;
import com.vk.exception.ProductNotFoundException;
import com.vk.repository.InventoryRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public void updateProductDetails(Product product) {
        inventoryRepository.save(product);
    }

    public Product getProduct(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }
}
