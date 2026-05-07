-- V1__init.sql — 初始資料庫結構
-- Quarkus Flyway 會在 docker compose up 時自動執行
-- ============================================================

-- 會員
-- password 允許 NULL：OAuth 用戶（Google/Facebook 登入）沒有密碼
-- oauth_provider + oauth_subject：存 OAuth 身份，兩者合起來全域唯一
CREATE TABLE users (
  id             BIGSERIAL PRIMARY KEY,
  email          VARCHAR(255) NOT NULL UNIQUE,
  password       VARCHAR(255),                    -- BCrypt hash；OAuth 用戶為 NULL
  name           VARCHAR(100) NOT NULL,
  phone          VARCHAR(20),
  role           VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',  -- CUSTOMER | ADMIN
  is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
  oauth_provider VARCHAR(20),                     -- 'GOOGLE' | 'FACEBOOK' | NULL
  oauth_subject  VARCHAR(255),                    -- OAuth provider 給的唯一用戶 ID
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  -- 同一個 OAuth provider 的 subject ID 不能重複
  UNIQUE (oauth_provider, oauth_subject)
);

-- 商品分類（支援多層，parent_id 指向自己 = Self-referential FK）
CREATE TABLE categories (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  slug        VARCHAR(100) NOT NULL UNIQUE,  -- URL 友善名稱，如 seafood
  parent_id   BIGINT REFERENCES categories(id),   -- NULL = 頂層分類
  sort_order  INT NOT NULL DEFAULT 0
);

-- 商品
CREATE TABLE products (
  id           BIGSERIAL PRIMARY KEY,
  category_id  BIGINT       NOT NULL REFERENCES categories(id),
  name         VARCHAR(200) NOT NULL,
  description  TEXT,
  price        NUMERIC(10,2) NOT NULL,           -- 不用 FLOAT，金額用精確型別
  stock_qty    INT          NOT NULL DEFAULT 0,
  weight_grams INT,                               -- 材積計算用，不是必填
  is_frozen    BOOLEAN      NOT NULL DEFAULT TRUE,
  is_active    BOOLEAN      NOT NULL DEFAULT TRUE, -- 軟刪除用
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 商品圖片
CREATE TABLE product_images (
  id          BIGSERIAL PRIMARY KEY,
  product_id  BIGINT       NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  url         VARCHAR(500) NOT NULL,  -- MinIO 物件路徑
  is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
  sort_order  INT          NOT NULL DEFAULT 0
);

-- 優惠券
-- discount_type = PERCENTAGE：discount_value 為百分比（如 20 = 打八折）
-- discount_type = FIXED_AMOUNT：discount_value 為折抵金額（如 100 = 折 100 元）
CREATE TABLE coupons (
  id               BIGSERIAL PRIMARY KEY,
  code             VARCHAR(50)   NOT NULL UNIQUE,  -- 折扣碼，如 SUMMER20
  discount_type    VARCHAR(20)   NOT NULL,          -- PERCENTAGE | FIXED_AMOUNT
  discount_value   NUMERIC(10,2) NOT NULL,
  min_order_amount NUMERIC(10,2),                   -- 最低消費門檻，NULL = 無限制
  max_uses         INT,                             -- 最多可用次數，NULL = 無限制
  used_count       INT           NOT NULL DEFAULT 0,
  expires_at       TIMESTAMPTZ,                     -- NULL = 永不過期
  is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- 訂單
-- coupon_id + discount_amount：下單時快照折扣資訊，優惠券日後修改不影響歷史訂單
CREATE TABLE orders (
  id               BIGSERIAL PRIMARY KEY,
  user_id          BIGINT        NOT NULL REFERENCES users(id),
  status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING_PAYMENT',
  -- PENDING_PAYMENT | PAID | PREPARING | SHIPPED | COMPLETED | CANCELLED
  total_amount     NUMERIC(10,2) NOT NULL,          -- 已扣除折扣後的最終金額
  coupon_id        BIGINT        REFERENCES coupons(id), -- NULL = 未使用優惠券
  discount_amount  NUMERIC(10,2) NOT NULL DEFAULT 0, -- 快照折扣金額
  shipping_name    VARCHAR(100)  NOT NULL,
  shipping_phone   VARCHAR(20)   NOT NULL,
  shipping_address VARCHAR(300)  NOT NULL,           -- 快照下單時的收件地址
  shipping_note    TEXT,
  paid_at          TIMESTAMPTZ,                      -- NULL = 尚未付款
  created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- 訂單品項（購買當下的價格快照，商品調價不影響歷史訂單）
CREATE TABLE order_items (
  id           BIGSERIAL PRIMARY KEY,
  order_id     BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id   BIGINT        NOT NULL REFERENCES products(id), -- 無 CASCADE：商品下架不刪歷史記錄
  product_name VARCHAR(200)  NOT NULL,  -- 名稱快照，防商品被軟刪後找不到名稱
  unit_price   NUMERIC(10,2) NOT NULL,  -- ★ 價格快照，這是電商標準設計
  quantity     INT           NOT NULL
);

-- 金流記錄
-- raw_callback JSONB：存原始 ECPay callback，日後對帳或爭議時有依據
CREATE TABLE payments (
  id                BIGSERIAL PRIMARY KEY,
  order_id          BIGINT        NOT NULL REFERENCES orders(id),
  gateway           VARCHAR(20)   NOT NULL DEFAULT 'ECPAY', -- 預留多金流擴充
  merchant_trade_no VARCHAR(50)   UNIQUE,                   -- ECPay 訂單編號
  amount            NUMERIC(10,2) NOT NULL,
  status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  raw_callback      JSONB,
  created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 初始種子資料
-- ============================================================

INSERT INTO categories (name, slug, sort_order) VALUES
  ('海鮮', 'seafood',      1),
  ('肉類', 'meat',         2),
  ('蔬菜', 'vegetables',   3),
  ('點心甜食', 'desserts',  4),
  ('熟食便當', 'ready-meals', 5);
