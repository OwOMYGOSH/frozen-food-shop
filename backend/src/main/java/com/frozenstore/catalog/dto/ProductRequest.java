package com.frozenstore.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
    @NotNull Long categoryId,
    @NotBlank @Size(max = 200) String name,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @Min(0) int stockQty,
    Integer weightGrams,
    boolean isFrozen
) {}
