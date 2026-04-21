package com.frozenstore.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long productId,
    String productName,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal subtotal  // unitPrice * quantity，方便前端直接顯示
) {}
