package com.frozenstore.coupon;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "coupons")
public class CouponEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 50)
    public String code;

    @Column(name = "discount_type", nullable = false, length = 20)
    public String discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    public BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    public BigDecimal minOrderAmount;

    @Column(name = "max_uses")
    public Integer maxUses;

    @Column(name = "used_count", nullable = false)
    public int usedCount = 0;

    @Column(name = "expires_at")
    public OffsetDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    /** 依 code 找優惠券，驗證折扣碼時用 **/
    public static Optional<CouponEntity> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }
}
