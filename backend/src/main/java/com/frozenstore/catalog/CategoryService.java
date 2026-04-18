package com.frozenstore.catalog;

import com.frozenstore.catalog.dto.CategoryRequest;
import com.frozenstore.catalog.dto.CategoryResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

@ApplicationScoped
public class CategoryService {

    public List<CategoryResponse> listAll() {
        return CategoryEntity.listOrdered().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        if (CategoryEntity.existsBySlug(req.slug())) {
            throw new IllegalArgumentException("Slug「" + req.slug() + "」已被使用");
        }
        if (req.parentId() != null && CategoryEntity.findById(req.parentId()) == null) {
            throw new IllegalArgumentException("父分類不存在：" + req.parentId());
        }

        CategoryEntity cat = new CategoryEntity();
        cat.name      = req.name().strip();
        cat.slug      = req.slug();
        cat.parentId  = req.parentId();
        cat.sortOrder = req.sortOrder();
        cat.persist();

        return toResponse(cat);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        CategoryEntity cat = CategoryEntity.findById(id);
        if (cat == null) throw new NotFoundException("分類不存在：" + id);

        if (CategoryEntity.existsBySlugAndIdNot(req.slug(), id)) {
            throw new IllegalArgumentException("Slug「" + req.slug() + "」已被使用");
        }
        if (req.parentId() != null && req.parentId().equals(id)) {
            throw new IllegalArgumentException("分類不能以自己為父分類");
        }

        cat.name      = req.name().strip();
        cat.slug      = req.slug();
        cat.parentId  = req.parentId();
        cat.sortOrder = req.sortOrder();

        return toResponse(cat);
    }

    @Transactional
    public void delete(Long id) {
        CategoryEntity cat = CategoryEntity.findById(id);
        if (cat == null) throw new NotFoundException("分類不存在：" + id);

        long productCount = ProductEntity.count("categoryId = ?1 AND isActive = true", id);
        if (productCount > 0) {
            throw new IllegalArgumentException(
                "此分類下仍有 " + productCount + " 個上架商品，請先下架商品後再刪除分類"
            );
        }

        cat.delete();
    }

    private CategoryResponse toResponse(CategoryEntity cat) {
        return new CategoryResponse(cat.id, cat.name, cat.slug, cat.parentId, cat.sortOrder);
    }
}
