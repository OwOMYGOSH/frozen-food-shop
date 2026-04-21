package com.frozenstore.order;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    // 訂單狀態流轉：PENDING_PAYMENT → PAID → PREPARING → SHIPPED → COMPLETED
    // 用字串而不是 enum，彈性更高（不需要配合 DB 型別改動）
    @Column(nullable = false, length = 30)
    public String status = "PENDING_PAYMENT";

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    public BigDecimal totalAmount;

    // 以下三個欄位是快照：下單時的收件資訊
    // 不直接關聯 users 表，因為用戶之後可能改地址，但歷史訂單要保留原始資料
    @Column(name = "shipping_name", nullable = false, length = 100)
    public String shippingName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    public String shippingPhone;

    @Column(name = "shipping_address", nullable = false, length = 300)
    public String shippingAddress;

    @Column(name = "shipping_note", columnDefinition = "TEXT")
    public String shippingNote;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    public BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "paid_at")
    public OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    // ── 查詢方法 ────────────────────────────────────────────────────────────

    // 查某個用戶的所有訂單，依建立時間倒序（最新的在前）
    public static List<OrderEntity> findByUserId(Long userId) {
        return list("userId = ?1 ORDER BY createdAt DESC", userId);
    }

    // 查某個用戶的特定訂單（同時驗證這筆訂單屬於這個用戶）
    public static OrderEntity findByIdAndUserId(Long id, Long userId) {
        return find("id = ?1 AND userId = ?2", id, userId).firstResult();
    }
}
