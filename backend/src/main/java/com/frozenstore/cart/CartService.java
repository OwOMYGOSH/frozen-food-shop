package com.frozenstore.cart;

import com.frozenstore.cart.dto.CartItemResponse;
import com.frozenstore.cart.dto.CartResponse;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CartService {

    // Redis key 格式：cart:42（42 是 user_id）
    // 用前綴區隔不同類型的 key，避免和 refresh_token: 等其他 key 衝突
    private static final String CART_KEY_PREFIX = "cart:";

    // 購物車 30 天沒操作就自動清除
    private static final Duration CART_TTL = Duration.ofDays(30);

    @Inject
    RedisDataSource redisDataSource;

    // HashCommands：操作 Redis Hash 結構（類似 Map<field, value>）
    // 泛型三個參數：<KeyType, FieldType, ValueType>
    // 這裡 key 是 String（"cart:42"），field 是 String（productId），value 是 String（quantity）
    // Redis 所有資料都是字串，數字要自己轉
    private HashCommands<String, String, String> hashCommands;
    private KeyCommands<String> keyCommands;

    // @PostConstruct：Bean 初始化完成後執行（所有 @Inject 都注入好之後）
    // 不能在欄位宣告時直接呼叫 redisDataSource.hash()，因為那時 @Inject 還沒完成
    @jakarta.annotation.PostConstruct
    void init() {
        this.hashCommands = redisDataSource.hash(String.class);
        this.keyCommands  = redisDataSource.key(String.class);
    }

    // ── 讀取購物車 ─────────────────────────────────────────────────────────

    // 給 OrderService 用：直接拿原始 Map，不包成 DTO
    // 這樣 OrderService 可以直接走訪品項做庫存檢查，不需要再解析 DTO
    public Map<String, String> getRawCart(Long userId) {
        return hashCommands.hgetall(cartKey(userId));
    }

    public CartResponse getCart(Long userId) {
        String key = cartKey(userId);

        // HGETALL cart:42 → 回傳 Map<productId, quantity>（字串對字串）
        Map<String, String> raw = hashCommands.hgetall(key);

        // 把 Map 轉成 List<CartItemResponse>
        List<CartItemResponse> items = raw.entrySet().stream()
            .map(e -> new CartItemResponse(
                Long.parseLong(e.getKey()),    // productId：字串轉 Long
                Integer.parseInt(e.getValue()) // quantity：字串轉 int
            ))
            .toList();

        // totalItems = 所有品項的數量加總（例如蝦子3包 + 豬肉1包 = 4）
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();

        return new CartResponse(items, totalItems);
    }

    // ── 加入商品 ────────────────────────────────────────────────────────────

    public void addItem(Long userId, Long productId, int quantity) {
        String key = cartKey(userId);

        // HGET cart:42 {productId} → 取得目前數量（null 代表還沒加過）
        String existing = hashCommands.hget(key, productId.toString());
        int newQty = (existing == null ? 0 : Integer.parseInt(existing)) + quantity;

        // HSET cart:42 {productId} {newQty} → 設定（或覆蓋）這個 field
        hashCommands.hset(key, productId.toString(), String.valueOf(newQty));

        // 每次操作都重設 TTL，確保「最後一次操作後 30 天才過期」
        resetTtl(key);
    }

    // ── 修改數量 ────────────────────────────────────────────────────────────

    public void updateItem(Long userId, Long productId, int quantity) {
        if (quantity == 0) {
            // quantity = 0 視為移除，讓前端只呼叫一個 API 就好
            removeItem(userId, productId);
            return;
        }

        String key = cartKey(userId);
        hashCommands.hset(key, productId.toString(), String.valueOf(quantity));
        resetTtl(key);
    }

    // ── 移除單一商品 ────────────────────────────────────────────────────────

    public void removeItem(Long userId, Long productId) {
        // HDEL cart:42 {productId} → 刪除 Hash 裡的這個 field
        hashCommands.hdel(cartKey(userId), productId.toString());
    }

    // ── 清空購物車（結帳後由 OrderService 呼叫）────────────────────────────

    public void clearCart(Long userId) {
        // DEL cart:42 → 刪除整個 key（比一個一個 HDEL 快）
        keyCommands.del(cartKey(userId));
    }

    // ── Private Helper ──────────────────────────────────────────────────────

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private void resetTtl(String key) {
        keyCommands.expire(key, CART_TTL);
    }
}
