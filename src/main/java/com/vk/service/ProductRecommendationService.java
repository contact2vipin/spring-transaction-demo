package com.vk.service;

import com.vk.entities.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductRecommendationService {

    // Fetch product recommendations (NOT_SUPPORTED)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Product> getRecommendations() {
        // Simulate hardcoded product recommendations
        List<Product> recommendations = new ArrayList<>();

        recommendations.add(new Product(5L, "Wireless Headphones", BigDecimal.valueOf(99.99), 15));
        recommendations.add(new Product(6L, "Smartphone case", BigDecimal.valueOf(19.99), 20));
        recommendations.add(new Product(7L, "Bluetooth speaker", BigDecimal.valueOf(49.99), 10));
        recommendations.add(new Product(8L, "Gaming mouse", BigDecimal.valueOf(59.99), 30));

        System.out.println("Recommendations fetched!");

        return recommendations;
    }
}
