package com.frozenstore.common;

import java.time.OffsetDateTime;

/**
 * 統一的 API 錯誤回應格式（與 Quarkus 版完全相同）
 *
 * 範例：
 * {
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "email: Email 格式不正確",
 *   "path": "/api/auth/register",
 *   "timestamp": "2026-04-22T10:00:00Z"
 * }
 */
public record ApiError(
    int status,
    String error,
    String message,
    String path,
    OffsetDateTime timestamp
) {
    public ApiError(int status, String error, String message, String path) {
        this(status, error, message, path, OffsetDateTime.now());
    }
}
