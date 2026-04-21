package com.frozenstore.cart;

import com.frozenstore.cart.dto.AddItemRequest;
import com.frozenstore.cart.dto.CartResponse;
import com.frozenstore.cart.dto.UpdateItemRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * 購物車 REST 端點
 *
 * 所有端點都需要登入（@RolesAllowed("CUSTOMER")）。
 * userId 從 JWT token 裡取出，不讓 client 自己帶——
 * 如果讓 client 帶 userId，用戶 A 可以傳 userId=B 去操作別人的購物車。
 *
 * GET    /api/cart                        取得我的購物車
 * POST   /api/cart/items                  加入商品
 * PUT    /api/cart/items/{productId}      修改數量（0 = 移除）
 * DELETE /api/cart/items/{productId}      移除商品
 */
@Path("/api/cart")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("CUSTOMER")
public class CartResource {

    @Inject
    CartService cartService;

    // ── 取得購物車 ──────────────────────────────────────────────────────────

    @GET
    @Consumes(MediaType.WILDCARD)
    public CartResponse getCart(@Context SecurityContext sec) {
        Long userId = extractUserId(sec);
        return cartService.getCart(userId);
    }

    // ── 加入商品 ────────────────────────────────────────────────────────────

    @POST
    @Path("/items")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addItem(@Context SecurityContext sec, @Valid AddItemRequest req) {
        Long userId = extractUserId(sec);
        cartService.addItem(userId, req.productId(), req.quantity());
        return Response.status(Response.Status.CREATED).build();
    }

    // ── 修改數量 ────────────────────────────────────────────────────────────

    @PUT
    @Path("/items/{productId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateItem(
        @Context SecurityContext sec,
        @PathParam("productId") Long productId,
        @Valid UpdateItemRequest req
    ) {
        Long userId = extractUserId(sec);
        cartService.updateItem(userId, productId, req.quantity());
        return Response.noContent().build();
    }

    // ── 移除商品 ────────────────────────────────────────────────────────────

    @DELETE
    @Path("/items/{productId}")
    @Consumes(MediaType.WILDCARD)
    public Response removeItem(@Context SecurityContext sec, @PathParam("productId") Long productId) {
        Long userId = extractUserId(sec);
        cartService.removeItem(userId, productId);
        return Response.noContent().build();
    }

    // ── Private Helper ──────────────────────────────────────────────────────

    // SecurityContext 是 JAX-RS 注入的物件，包含當前登入用戶的資訊
    // getName() 回傳的是 JWT 裡的 sub（subject）欄位，也就是 user id
    // 參考 AuthService.generateAccessToken()：.subject(user.id.toString())
    private Long extractUserId(SecurityContext sec) {
        return Long.parseLong(sec.getUserPrincipal().getName());
    }
}
