package com.frozenstore.order;

import com.frozenstore.cart.CartService;
import com.frozenstore.catalog.ProductEntity;
import com.frozenstore.order.dto.CreateOrderRequest;
import com.frozenstore.order.dto.OrderItemResponse;
import com.frozenstore.order.dto.OrderResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OrderService {

    @Inject
    CartService cartService;

    // EntityManager 是 JPA 的核心物件，用來執行需要細緻控制的查詢（例如悲觀鎖）
    // 一般查詢用 Panache 的靜態方法就夠，但 SELECT FOR UPDATE 需要 EntityManager
    @Inject
    EntityManager em;

    // ── 建立訂單（核心流程）──────────────────────────────────────────────────

    // @Transactional：這個方法裡的所有 DB 操作都在同一個 transaction 裡
    // 任何一步拋出例外 → 整個 transaction rollback，DB 回到原始狀態
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {

        // 1. 從 Redis 讀取購物車
        Map<String, String> cartRaw = cartService.getRawCart(userId);
        if (cartRaw.isEmpty()) {
            throw new BadRequestException("購物車是空的");
        }

        // 2. 對每個商品做庫存檢查 + 扣減（悲觀鎖）
        List<OrderItemEntity> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<String, String> entry : cartRaw.entrySet()) {
            Long productId = Long.parseLong(entry.getKey());
            int quantity   = Integer.parseInt(entry.getValue());

            // ★ SELECT ... FOR UPDATE：鎖定這一列
            // 其他 transaction 碰到同一個 productId 時，必須等這個 transaction commit 才能繼續
            // 這就是悲觀鎖：先鎖再讀，假設一定會有衝突
            ProductEntity product = em.find(ProductEntity.class, productId, LockModeType.PESSIMISTIC_WRITE);

            if (product == null || !product.isActive) {
                throw new BadRequestException("商品不存在或已下架：id=" + productId);
            }
            if (product.stockQty < quantity) {
                throw new BadRequestException("商品庫存不足：" + product.name);
            }

            // 扣庫存（在 transaction 鎖的保護下，不會被其他 transaction 同時修改）
            product.stockQty -= quantity;

            // 建立訂單品項，快照當下的名稱與價格
            OrderItemEntity item = new OrderItemEntity();
            item.productId   = productId;
            item.productName = product.name;     // 快照：商品之後改名不影響這筆訂單
            item.unitPrice   = product.price;    // 快照：商品之後改價不影響這筆訂單
            item.quantity    = quantity;
            orderItems.add(item);

            totalAmount = totalAmount.add(product.price.multiply(BigDecimal.valueOf(quantity)));
        }

        // 3. 建立訂單主檔
        OrderEntity order = new OrderEntity();
        order.userId          = userId;
        order.totalAmount     = totalAmount;
        order.shippingName    = req.shippingName();
        order.shippingPhone   = req.shippingPhone();
        order.shippingAddress = req.shippingAddress();
        order.shippingNote    = req.shippingNote();
        order.persist();

        // 4. 存入訂單品項（order.id 在 persist() 後才有值）
        for (OrderItemEntity item : orderItems) {
            item.orderId = order.id;
            item.persist();
        }

        // 5. 清空 Redis 購物車
        // 注意：Redis 操作不在 DB transaction 裡，如果 DB commit 成功但 Redis DEL 失敗，
        // 購物車會殘留，但訂單已成立。這個風險極低，且影響只是用戶看到舊購物車，
        // 不影響金額或庫存正確性。
        cartService.clearCart(userId);

        return toResponse(order, orderItems);
    }

    // ── 查詢我的訂單列表 ────────────────────────────────────────────────────

    public List<OrderResponse> listMyOrders(Long userId) {
        List<OrderEntity> orders = OrderEntity.findByUserId(userId);
        if (orders.isEmpty()) return List.of();

        // 批次查詢品項（避免 N+1，和 Phase 1 圖片批次查詢相同思路）
        List<Long> orderIds = orders.stream().map(o -> o.id).toList();
        List<OrderItemEntity> allItems = OrderItemEntity.findByOrderIds(orderIds);

        return orders.stream().map(order -> {
            List<OrderItemEntity> items = allItems.stream()
                .filter(i -> i.orderId.equals(order.id))
                .toList();
            return toResponse(order, items);
        }).toList();
    }

    // ── 查詢單筆訂單詳情 ────────────────────────────────────────────────────

    public OrderResponse getOrder(Long userId, Long orderId) {
        // findByIdAndUserId 同時驗證「這筆訂單屬於這個用戶」
        // 避免用戶 A 用 orderId 去查用戶 B 的訂單
        OrderEntity order = OrderEntity.findByIdAndUserId(orderId, userId);
        if (order == null) throw new NotFoundException("訂單不存在");

        List<OrderItemEntity> items = OrderItemEntity.findByOrderId(orderId);
        return toResponse(order, items);
    }

    // ── Private Helper ──────────────────────────────────────────────────────

    private OrderResponse toResponse(OrderEntity order, List<OrderItemEntity> items) {
        List<OrderItemResponse> itemResponses = items.stream()
            .map(i -> new OrderItemResponse(
                i.productId,
                i.productName,
                i.unitPrice,
                i.quantity,
                i.unitPrice.multiply(BigDecimal.valueOf(i.quantity)) // subtotal
            ))
            .toList();

        return new OrderResponse(
            order.id, order.status, order.totalAmount, order.discountAmount,
            order.shippingName, order.shippingPhone, order.shippingAddress,
            order.shippingNote, order.paidAt, order.createdAt, itemResponses
        );
    }
}
