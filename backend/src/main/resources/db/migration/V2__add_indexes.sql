-- V2__add_indexes.sql — 效能索引
-- 為高頻查詢欄位加索引，避免全表掃描
--
-- 規則：SELECT WHERE 條件常用的欄位就加索引
-- 代價：每個索引讓 INSERT/UPDATE 稍微變慢（要同時更新索引），所以不是每個欄位都加

-- users：依 email 查詢（登入、重複信箱檢查）
CREATE INDEX IF NOT EXISTS idx_users_email        ON users (email);

-- users：依 OAuth 身份查詢（OAuth 登入流程）
CREATE INDEX IF NOT EXISTS idx_users_oauth        ON users (oauth_provider, oauth_subject);

-- products：依分類列出商品
CREATE INDEX IF NOT EXISTS idx_products_category  ON products (category_id);

-- products：首頁只顯示上架商品，這個查詢頻率極高
CREATE INDEX IF NOT EXISTS idx_products_active    ON products (is_active);

-- orders：會員查詢自己的訂單
CREATE INDEX IF NOT EXISTS idx_orders_user        ON orders (user_id);

-- orders：依訂單狀態篩選（後台管理）
CREATE INDEX IF NOT EXISTS idx_orders_status      ON orders (status);

-- order_items：依訂單 ID 取商品清單（每次顯示訂單詳情都會查）
CREATE INDEX IF NOT EXISTS idx_order_items_order  ON order_items (order_id);
