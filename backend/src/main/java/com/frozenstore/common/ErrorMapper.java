package com.frozenstore.common;

import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.stream.Collectors;

/**
 * Global Error Handler
 *
 * 修正記錄：
 * - UNPROCESSABLE_ENTITY 在部分 JAX-RS 版本不存在，改用 Response.status(422)
 * - 移除 handleGeneric(Exception)：太貪心，會攔截 Quarkus 內部的 response 處理流程，
 *   導致正常的 204 回應也被轉成 500
 */
@Singleton
public class ErrorMapper {

    /** 422 — Bean Validation 失敗（@NotBlank, @Email 等） */
    @ServerExceptionMapper
    public Response handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(v -> {
                String field = v.getPropertyPath().toString();
                int lastDot = field.lastIndexOf('.');
                if (lastDot >= 0) field = field.substring(lastDot + 1);
                return field + ": " + v.getMessage();
            })
            .sorted()
            .collect(Collectors.joining("; "));

        return Response.status(422)
            .entity(new ApiError(422, "Validation Failed", message, ""))
            .build();
    }

    /** 400 — 業務邏輯層主動拋出的非法參數（如：Email 已被註冊） */
    @ServerExceptionMapper
    public Response handleIllegalArgument(IllegalArgumentException ex) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ApiError(400, "Bad Request", ex.getMessage(), ""))
            .build();
    }

    /** 401 — 未帶 token 或 token 無效 */
    @ServerExceptionMapper
    public Response handleUnauthorized(NotAuthorizedException ex) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ApiError(401, "Unauthorized", "請先登入", ""))
            .build();
    }

    /** 404 — 路由不存在 */
    @ServerExceptionMapper
    public Response handleNotFound(NotFoundException ex) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ApiError(404, "Not Found", ex.getMessage(), ""))
            .build();
    }
}
