package com.vk.records.request;

import java.math.BigDecimal;

public record OrderRequest(String name, Long productId, BigDecimal price, Integer quantity) {
}
