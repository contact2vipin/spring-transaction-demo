package com.vk.records.response;

import java.math.BigDecimal;

public record OrderResponse(Long id, Long productId, Integer quantity, BigDecimal totalPrice) {
}
