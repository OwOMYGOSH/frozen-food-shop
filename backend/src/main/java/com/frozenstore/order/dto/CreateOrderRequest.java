package com.frozenstore.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 建立訂單時 client 需要提供的收件資訊
// 購物車品項從 Redis 讀，不需要 client 再帶一次（避免被竄改）
public record CreateOrderRequest(
    @NotBlank @Size(max = 100) String shippingName,
    @NotBlank @Size(max = 20)  String shippingPhone,
    @NotBlank @Size(max = 300) String shippingAddress,
    String shippingNote
) {}
