package com.frozenstore.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "slug 只能包含小寫字母、數字和連字號")
    String slug,
    Long parentId,
    int sortOrder
) {}
