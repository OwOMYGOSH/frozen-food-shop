package com.frozenstore.auth;

import com.frozenstore.auth.dto.*;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Auth REST 端點
 *
 * 職責：HTTP 層
 * - 接收請求、解析參數
 * - 呼叫 AuthService（業務邏輯）
 * - 回傳 HTTP Response
 *
 * 不做：業務邏輯、DB 操作（那是 Service 的工作）
 *
 * @Valid 的作用：
 * 觸發 Bean Validation，如果 DTO 的 @NotBlank/@Email 等規則不過，
 * 會丟出 ConstraintViolationException，被 ErrorMapper 接住回傳 422。
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    /**
     * POST /api/auth/register
     * 新用戶註冊
     *
     * 成功：201 Created + RegisterResponse
     * 失敗：422 格式錯誤 / 400 Email 已存在
     */
    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest req) {
        RegisterResponse response = authService.register(req);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * POST /api/auth/login
     * 登入，成功後回傳 Access Token 和 Refresh Token Cookie
     *
     * 成功：200 OK + LoginResponse（body）+ Set-Cookie（refresh_token）
     * 失敗：401 帳密錯誤
     */
    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest req) {
        return authService.login(req);
    }

    /**
     * POST /api/auth/logout
     * 登出，清除 Refresh Token
     *
     * 成功：204 No Content（永遠成功，就算沒帶 Cookie 也是 204）
     */
    @POST
    @Path("/logout")
    @Consumes(MediaType.WILDCARD)
    public Response logout(@CookieParam(AuthService.REFRESH_COOKIE_NAME) Cookie refreshCookie) {
        String token = refreshCookie != null ? refreshCookie.getValue() : null;
        return authService.logout(token);
    }

    /**
     * POST /api/auth/refresh
     * 用 HttpOnly Cookie 裡的 Refresh Token 換新的 Access Token
     *
     * 成功：200 OK + 新 Access Token + 新 Refresh Token Cookie（Token Rotation）
     * 失敗：401 Token 無效或過期
     */
    @POST
    @Path("/refresh")
    @Consumes(MediaType.WILDCARD)
    public Response refresh(@CookieParam(AuthService.REFRESH_COOKIE_NAME) Cookie refreshCookie) {
        String token = refreshCookie != null ? refreshCookie.getValue() : null;
        return authService.refresh(token);
    }
}
