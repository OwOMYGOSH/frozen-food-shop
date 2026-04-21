package com.frozenstore.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// record = Java 14+ 的語法，專門拿來寫「只有資料、沒有行為」的類別
// 等同於一個有 constructor、getter、equals、hashCode 的 class，但更精簡
// 這裡代表「加入購物車」的 request body
public record AddItemRequest(
    @NotNull Long productId,       // 哪個商品
    @NotNull @Min(1) int quantity  // 數量（至少 1）
) {}
