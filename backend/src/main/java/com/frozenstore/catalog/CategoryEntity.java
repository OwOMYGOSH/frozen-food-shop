package com.frozenstore.catalog;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;

/**
 * 商品分類實體（Active Record 模式）
 *
 * 支援多層分類：parent_id 指向自己（Self-referential FK）
 * parent_id = null → 頂層分類（海鮮、肉類...）
 * parent_id = 某 id → 子分類（例如「蝦類」屬於「海鮮」）
 */
@Entity
@Table(name = "categories")
public class CategoryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String name;

    /** URL 友善名稱，如 seafood，全域唯一 */
    @Column(nullable = false, unique = true, length = 100)
    public String slug;

    @Column(name = "parent_id")
    public Long parentId;

    @Column(name = "sort_order", nullable = false)
    public int sortOrder = 0;

    // ── 查詢方法 ──

    public static List<CategoryEntity> listOrdered() {
        return list("ORDER BY sortOrder, id");
    }

    public static CategoryEntity findBySlug(String slug) {
        return find("slug", slug).firstResult();
    }

    public static boolean existsBySlug(String slug) {
        return count("slug", slug) > 0;
    }

    /** 更新時確認 slug 唯一性，排除自己 */
    public static boolean existsBySlugAndIdNot(String slug, Long id) {
        return count("slug = ?1 AND id != ?2", slug, id) > 0;
    }
}
