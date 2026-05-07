package com.frozenstore.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全域例外處理（對應 Quarkus 的 ErrorMapper）
 *
 * 差異對照：
 *   Quarkus: 一個方法一個 @ServerExceptionMapper
 *   Spring : 一個類別標 @RestControllerAdvice，方法標 @ExceptionHandler(Xxx.class)
 *
 * 擴充規則：
 *   - 業務層丟 IllegalArgumentException   → 400
 *   - @Valid 驗證失敗                      → 422
 *   - 認證失敗（Spring Security 自己丟） → 401
 *   - 授權失敗（角色不符）                 → 403
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 422 — @Valid 在 @RequestBody 驗證失敗（DTO 欄位不合法） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));

        return ResponseEntity.unprocessableEntity()
            .body(new ApiError(422, "Validation Failed", message, req.getRequestURI()));
    }

    /** 422 — @Validated 在 @PathVariable / @RequestParam 驗證失敗 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex,
                                                     HttpServletRequest req) {
        String message = ex.getConstraintViolations().stream()
            .map(v -> {
                String field = v.getPropertyPath().toString();
                int lastDot = field.lastIndexOf('.');
                if (lastDot >= 0) field = field.substring(lastDot + 1);
                return field + ": " + v.getMessage();
            })
            .sorted()
            .collect(Collectors.joining("; "));

        return ResponseEntity.unprocessableEntity()
            .body(new ApiError(422, "Validation Failed", message, req.getRequestURI()));
    }

    /** 400 — 業務邏輯層主動拋出（例如：Email 已被註冊） */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex,
                                                     HttpServletRequest req) {
        return ResponseEntity.badRequest()
            .body(new ApiError(400, "Bad Request", ex.getMessage(), req.getRequestURI()));
    }

    /** 401 — 密碼錯誤等認證失敗 */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex,
                                                         HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiError(401, "Unauthorized", "Email 或密碼錯誤", req.getRequestURI()));
    }

    /** 403 — 已登入但角色權限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiError(403, "Forbidden", "權限不足", req.getRequestURI()));
    }

    /** 404 — 路由不存在 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoHandlerFoundException ex,
                                                   HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(404, "Not Found", "找不到資源", req.getRequestURI()));
    }
}
