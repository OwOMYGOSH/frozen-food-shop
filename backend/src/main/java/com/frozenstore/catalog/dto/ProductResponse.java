package com.frozenstore.catalog.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductResponse(
    Long id,
    Long categoryId,
    String categoryName,
    String name,
    String description,
    BigDecimal price,
    int stockQty,
    Integer weightGrams,
    boolean isFrozen,
    boolean isActive,
    List<String> imageUrls,
    OffsetDateTime createdAt
) {}
