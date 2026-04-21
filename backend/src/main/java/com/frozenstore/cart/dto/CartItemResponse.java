package com.frozenstore.cart.dto;

// 購物車裡每一筆品項的資料
public record CartItemResponse(
    Long productId,
    int quantity
) {}
