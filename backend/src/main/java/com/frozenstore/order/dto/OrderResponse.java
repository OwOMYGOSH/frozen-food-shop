package com.frozenstore.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    String status,
    BigDecimal totalAmount,
    BigDecimal discountAmount,
    String shippingName,
    String shippingPhone,
    String shippingAddress,
    String shippingNote,
    OffsetDateTime paidAt,
    OffsetDateTime createdAt,
    List<OrderItemResponse> items
) {}
