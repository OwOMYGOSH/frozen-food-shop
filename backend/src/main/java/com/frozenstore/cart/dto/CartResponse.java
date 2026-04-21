package com.frozenstore.cart.dto;

import java.util.List;

// GET /api/cart 的完整回傳格式
// {
//   "items": [ { "productId": 101, "quantity": 3 }, ... ],
//   "totalItems": 4   ← 所有商品數量加總
// }
public record CartResponse(
    List<CartItemResponse> items,
    int totalItems
) {}
