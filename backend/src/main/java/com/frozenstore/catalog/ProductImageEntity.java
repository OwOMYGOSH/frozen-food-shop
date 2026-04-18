package com.frozenstore.catalog;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品圖片實體
 *
 * 刻意不在 ProductEntity 用 @OneToMany，
 * 改由 Service 層做批次查詢，避免 N+1 問題。
 */
@Entity
@Table(name = "product_images")
public class ProductImageEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "product_id", nullable = false)
    public Long productId;

    /** MinIO 物件路徑或完整 URL */
    @Column(nullable = false, length = 500)
    public String url;

    @Column(name = "is_primary", nullable = false)
    public boolean isPrimary = false;

    @Column(name = "sort_order", nullable = false)
    public int sortOrder = 0;

    // ── 查詢方法 ──

    /**
     * 批次查詢多個商品的主圖，回傳 productId → url 的 Map
     * 用在商品列表頁，避免對每個商品各發一次 SQL（N+1 問題）
     */
    public static Map<Long, String> findPrimaryUrlsByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        return ProductImageEntity.<ProductImageEntity>list(
            "productId IN ?1 AND isPrimary = true", productIds
        ).stream().collect(Collectors.toMap(
            img -> img.productId,
            img -> img.url,
            (a, b) -> a  // 若有多筆主圖取第一筆（資料異常防護）
        ));
    }

    /**
     * 查詢單一商品的所有圖片 URL
     * 主圖優先（isPrimary DESC），再依 sortOrder 排序
     */
    public static List<String> findUrlsByProductId(Long productId) {
        return ProductImageEntity.<ProductImageEntity>list(
            "productId = ?1 ORDER BY isPrimary DESC, sortOrder ASC", productId
        ).stream().map(img -> img.url).toList();
    }
}
