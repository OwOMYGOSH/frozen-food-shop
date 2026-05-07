package com.frozenstore.coupon.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 優惠券完整資訊（Admin 用）
 *
 * Customer 結帳時若要驗證折扣碼，Phase 3 後期會另外做輕量版 Response
 * （只含 discountType / discountValue / 可用結論），避免洩漏使用次數等內部資訊。
 */
public record CouponResponse(
    Long id,
    String code,
    String discountType,
    BigDecimal discountValue,
    BigDecimal minOrderAmount,
    Integer maxUses,
    int usedCount,
    OffsetDateTime expiresAt,
    boolean isActive,
    OffsetDateTime createdAt
) {}
