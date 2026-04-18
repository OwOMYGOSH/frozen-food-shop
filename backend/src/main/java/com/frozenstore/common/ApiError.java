package com.frozenstore.common;

import java.time.OffsetDateTime;

/**
 * 統一的 API 錯誤回應格式
 * 所有錯誤都回傳這個結構，讓前端有一致的錯誤處理方式
 *
 * 範例：
 * {
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "email: Email 格式不正確",
 *   "path": "/api/auth/register",
 *   "timestamp": "2026-04-15T10:00:00Z"
 * }
 */
public record ApiError(
    int status,
    String error,
    String message,
    String path,
    OffsetDateTime timestamp
) {
    /** 簡化建構子：timestamp 自動填入當下時間 */
    public ApiError(int status, String error, String message, String path) {
        this(status, error, message, path, OffsetDateTime.now());
    }
}
