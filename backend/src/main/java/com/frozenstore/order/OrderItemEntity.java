package com.frozenstore.order;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "order_items")
public class OrderItemEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Column(name = "product_id", nullable = false)
    public Long productId;

    // ★ 快照欄位：下單當下的商品名稱與價格
    // 商品之後改名、改價、甚至下架，這筆歷史訂單的資料完全不受影響
    @Column(name = "product_name", nullable = false, length = 200)
    public String productName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    public BigDecimal unitPrice;

    @Column(nullable = false)
    public int quantity;

    // ── 查詢方法 ────────────────────────────────────────────────────────────

    // 一次查一批訂單的品項，避免 N+1（和 Phase 1 商品圖片的做法相同）
    public static List<OrderItemEntity> findByOrderIds(List<Long> orderIds) {
        return list("orderId IN ?1", orderIds);
    }

    public static List<OrderItemEntity> findByOrderId(Long orderId) {
        return list("orderId", orderId);
    }
}
