-- V3: 訂單相關查詢的 index
-- 最常見的查詢：「查某個用戶的所有訂單」和「查某筆訂單的所有品項」

-- 用戶訂單列表（GET /api/orders 會走這個 index）
CREATE INDEX idx_orders_user_id ON orders(user_id);

-- 訂單品項批次查詢（findByOrderIds 會走這個 index）
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
