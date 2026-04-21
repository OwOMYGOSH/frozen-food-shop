package com.frozenstore.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 修改數量的 request body
// quantity = 0 代表移除這個商品（讓前端不用呼叫兩個不同 API）
public record UpdateItemRequest(
    @NotNull @Min(0) int quantity
) {}
