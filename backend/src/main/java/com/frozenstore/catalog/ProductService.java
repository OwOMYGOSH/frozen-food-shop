package com.frozenstore.catalog;

import com.frozenstore.catalog.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductService {

    /**
     * 分頁查詢商品列表
     *
     * @param categorySlug 分類 slug（null = 不篩選）
     * @param keyword      商品名稱關鍵字（null = 不篩選）
     * @param page         頁碼，從 0 開始
     * @param size         每頁筆數（1～100）
     * @param sort         排序：price_asc / price_desc / name_asc / created_desc（預設）
     */
    public ProductPageResponse listPage(
        String categorySlug, String keyword, int page, int size, String sort
    ) {
        // ── 組合查詢條件 ──
        StringBuilder where = new StringBuilder("isActive = true");
        Map<String, Object> params = new HashMap<>();

        if (categorySlug != null && !categorySlug.isBlank()) {
            CategoryEntity cat = CategoryEntity.findBySlug(categorySlug);
            if (cat == null) throw new NotFoundException("分類不存在：" + categorySlug);
            where.append(" AND categoryId = :catId");
            params.put("catId", cat.id);
        }

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND LOWER(name) LIKE :kw");
            params.put("kw", "%" + keyword.toLowerCase().strip() + "%");
        }

        String orderBy = switch (sort != null ? sort : "") {
            case "price_asc"  -> "price ASC";
            case "price_desc" -> "price DESC";
            case "name_asc"   -> "name ASC";
            default           -> "createdAt DESC";
        };

        // ── 分頁查詢 ──
        var query = ProductEntity.find(where + " ORDER BY " + orderBy, params);
        long total = query.count();
        List<ProductEntity> products = query.page(page, size).list();

        // ── 批次查主圖（避免 N+1）──
        List<Long> ids = products.stream().map(p -> p.id).toList();
        Map<Long, String> primaryImages = ProductImageEntity.findPrimaryUrlsByProductIds(ids);

        // ── 批次查分類名稱（避免 N+1）──
        Set<Long> catIds = products.stream().map(p -> p.categoryId).collect(Collectors.toSet());
        Map<Long, String> catNames = catIds.isEmpty() ? Map.of() :
            CategoryEntity.<CategoryEntity>list("id IN ?1", new ArrayList<>(catIds))
                .stream().collect(Collectors.toMap(c -> c.id, c -> c.name));

        List<ProductResponse> content = products.stream()
            .map(p -> toResponse(
                p,
                catNames.get(p.categoryId),
                primaryImages.containsKey(p.id) ? List.of(primaryImages.get(p.id)) : List.of()
            ))
            .toList();

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new ProductPageResponse(
            content, page, size, total, totalPages,
            page == 0,
            page >= totalPages - 1
        );
    }

    /** 查詢單一商品詳情（含所有圖片） */
    public ProductResponse findById(Long id) {
        ProductEntity product = ProductEntity.findById(id);
        if (product == null || !product.isActive) {
            throw new NotFoundException("商品不存在：" + id);
        }

        CategoryEntity cat = CategoryEntity.findById(product.categoryId);
        List<String> imageUrls = ProductImageEntity.findUrlsByProductId(id);

        return toResponse(product, cat != null ? cat.name : null, imageUrls);
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (CategoryEntity.findById(req.categoryId()) == null) {
            throw new IllegalArgumentException("分類不存在：" + req.categoryId());
        }

        ProductEntity product = new ProductEntity();
        product.categoryId  = req.categoryId();
        product.name        = req.name().strip();
        product.description = req.description();
        product.price       = req.price();
        product.stockQty    = req.stockQty();
        product.weightGrams = req.weightGrams();
        product.isFrozen    = req.isFrozen();
        product.persist();

        CategoryEntity cat = CategoryEntity.findById(product.categoryId);
        return toResponse(product, cat != null ? cat.name : null, List.of());
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        ProductEntity product = ProductEntity.findById(id);
        if (product == null) throw new NotFoundException("商品不存在：" + id);

        if (!product.categoryId.equals(req.categoryId()) &&
            CategoryEntity.findById(req.categoryId()) == null) {
            throw new IllegalArgumentException("分類不存在：" + req.categoryId());
        }

        product.categoryId  = req.categoryId();
        product.name        = req.name().strip();
        product.description = req.description();
        product.price       = req.price();
        product.stockQty    = req.stockQty();
        product.weightGrams = req.weightGrams();
        product.isFrozen    = req.isFrozen();

        CategoryEntity cat = CategoryEntity.findById(product.categoryId);
        List<String> imageUrls = ProductImageEntity.findUrlsByProductId(id);
        return toResponse(product, cat != null ? cat.name : null, imageUrls);
    }

    /** 下架商品（軟刪除），不從 DB 實際刪除，歷史訂單仍可參照 */
    @Transactional
    public void softDelete(Long id) {
        ProductEntity product = ProductEntity.findById(id);
        if (product == null) throw new NotFoundException("商品不存在：" + id);
        product.isActive = false;
    }

    private ProductResponse toResponse(ProductEntity p, String categoryName, List<String> imageUrls) {
        return new ProductResponse(
            p.id, p.categoryId, categoryName,
            p.name, p.description, p.price, p.stockQty, p.weightGrams,
            p.isFrozen, p.isActive, imageUrls, p.createdAt
        );
    }
}
