package com.frozenstore.auth.dto;

/**
 * 登入成功回應 DTO
 *
 * accessToken：放 Authorization Header，前端每次 API 請求帶上
 * tokenType：固定 "Bearer"，這是 OAuth 2.0 標準
 * expiresIn：秒數，前端用來計算何時要做 silent refresh
 *
 * Refresh Token 不在這裡回傳！
 * 它是 HttpOnly Cookie，由後端直接 Set-Cookie，前端的 JS 碰不到它，
 * 這樣就算前端被 XSS 攻擊，攻擊者也拿不到 Refresh Token。
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
