package com.frozenstore.catalog.dto;

public record CategoryResponse(
    Long id,
    String name,
    String slug,
    Long parentId,
    int sortOrder
) {}
