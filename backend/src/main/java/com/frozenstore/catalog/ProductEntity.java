package com.frozenstore.catalog;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 商品實體（Active Record 模式）
 *
 * 設計重點：
 * - price 用 NUMERIC(10,2)，不用 FLOAT（金額必須精確）
 * - isActive 做軟刪除，商品下架不刪 DB 資料（歷史訂單還需要參照）
 * - 圖片不在這裡做 @OneToMany，由 Service 層批次查詢（避免 N+1）
 */
@Entity
@Table(name = "products")
public class ProductEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "category_id", nullable = false)
    public Long categoryId;

    @Column(nullable = false, length = 200)
    public String name;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal price;

    @Column(name = "stock_qty", nullable = false)
    public int stockQty = 0;

    @Column(name = "weight_grams")
    public Integer weightGrams;

    @Column(name = "is_frozen", nullable = false)
    public boolean isFrozen = true;

    /** false = 下架（軟刪除），不從 DB 實際刪除 */
    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
