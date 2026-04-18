package com.frozenstore.auth.dto;

import java.time.OffsetDateTime;

/**
 * 註冊成功回應 DTO
 *
 * 注意：不回傳 password（即使是 hash 也不該給前端）
 * 注意：不回傳 accessToken，設計上要求用戶「註冊完再登入」，
 *       這樣可以強制走一次完整的登入流程（e.g. 信箱驗證）
 */
public record RegisterResponse(
    Long id,
    String email,
    String name,
    String role,
    OffsetDateTime createdAt
) {}
