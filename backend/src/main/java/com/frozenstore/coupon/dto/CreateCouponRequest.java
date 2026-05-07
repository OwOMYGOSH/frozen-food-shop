package com.frozenstore.coupon.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 建立優惠券的請求（僅 ADMIN 使用）
 *
 * null 的語意：
 *   minOrderAmount = null → 無消費門檻
 *   maxUses        = null → 無使用次數上限
 *   expiresAt      = null → 永不過期
 */
public record CreateCouponRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT") String discountType,
    @NotNull @DecimalMin("0.01") BigDecimal discountValue, 
    @DecimalMin("0.01") BigDecimal minOrderAmount,
    @Min(1) Integer maxUses,
    OffsetDateTime expiresAt
) {}
