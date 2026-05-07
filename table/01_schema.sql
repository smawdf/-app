-- ============================================================
-- OrderDisk — Supabase 建表脚本
-- 版本：v1.0
-- 说明：二人专属点菜 & 做菜 Android 应用数据库
-- 使用方式：在 Supabase SQL Editor 中按顺序执行
-- ============================================================

-- 1. profiles — 用户信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS profiles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    pair_id     UUID NOT NULL,
    nickname    TEXT NOT NULL DEFAULT '',
    avatar_url  TEXT,
    taste_prefs JSONB DEFAULT '{}',
    allergies   TEXT[] DEFAULT '{}',
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

COMMENT ON TABLE profiles IS '用户信息表 — 一人一条，通过 pair_id 绑定情侣关系';
COMMENT ON COLUMN profiles.user_id IS 'Supabase Auth 用户 ID';
COMMENT ON COLUMN profiles.pair_id IS '情侣配对 ID，两个人共享同一个 pair_id';
COMMENT ON COLUMN profiles.taste_prefs IS '口味偏好，如 {"spicy":true, "sweet":false, "sour":true, "salty":false, "light":true, "heavy":false}';
COMMENT ON COLUMN profiles.allergies IS '忌口列表，如 {"花生", "牛奶", "海鲜"}';

-- 2. dishes — 菜品库表
-- ============================================================
CREATE TABLE IF NOT EXISTS dishes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pair_id         UUID NOT NULL,
    name            TEXT NOT NULL,
    source          TEXT NOT NULL CHECK (source IN ('custom', 'external', 'builtin')),
    external_id     TEXT,
    external_source TEXT,
    category        TEXT DEFAULT '中餐',
    image_url       TEXT,
    cook_steps      JSONB DEFAULT '[]',
    ingredients     TEXT[] DEFAULT '{}',
    difficulty      SMALLINT DEFAULT 1 CHECK (difficulty BETWEEN 1 AND 5),
    cook_time_min   INTEGER DEFAULT 0,
    who_likes       TEXT[] DEFAULT '{}',
    rating          REAL DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    notes           TEXT DEFAULT '',
    created_by      UUID REFERENCES profiles(id),
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

COMMENT ON TABLE dishes IS '菜品库表 — 自定义菜品 + 外部 API 导入 + 内置菜谱';
COMMENT ON COLUMN dishes.source IS '来源：custom=用户自建, external=外部API导入, builtin=系统内置';
COMMENT ON COLUMN dishes.cook_steps IS 'JSONB 数组：[{"step":1, "description":"切菜", "tip":"切细一点", "image_url":null}]';
COMMENT ON COLUMN dishes.who_likes IS '谁爱吃：["user_a", "user_b"]';
COMMENT ON COLUMN dishes.difficulty IS '难度：1=新手, 2=简单, 3=中等, 4=困难, 5=大厨';

-- 3. dish_tags — 菜品标签关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS dish_tags (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dish_id    UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    pair_id    UUID NOT NULL,
    name       TEXT NOT NULL,
    color      TEXT DEFAULT '#FF6B6B',
    created_at TIMESTAMPTZ DEFAULT now()
);

COMMENT ON TABLE dish_tags IS '菜品标签 — 每个菜品可以有多个标签（如 "快手菜", "下饭"）';

-- 4. meals — 点餐记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS meals (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pair_id      UUID NOT NULL,
    meal_type    TEXT NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'supper', 'other')),
    date         DATE NOT NULL DEFAULT CURRENT_DATE,
    status       TEXT NOT NULL DEFAULT 'ordering' CHECK (status IN ('ordering', 'confirmed', 'completed', 'cancelled')),
    created_by   UUID REFERENCES profiles(id),
    confirmed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ DEFAULT now(),
    updated_at   TIMESTAMPTZ DEFAULT now()
);

COMMENT ON TABLE meals IS '点餐记录表 — 一顿饭的会话';
COMMENT ON COLUMN meals.meal_type IS '餐次：breakfast=早餐, lunch=午餐, dinner=晚餐, supper=夜宵, other=其他';
COMMENT ON COLUMN meals.status IS '状态：ordering=点餐中, confirmed=已确认, completed=已完成, cancelled=已取消';

-- 5. meal_items — 点餐明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS meal_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_id    UUID NOT NULL REFERENCES meals(id) ON DELETE CASCADE,
    dish_id    UUID NOT NULL REFERENCES dishes(id),
    chosen_by  UUID REFERENCES profiles(id),
    quantity   INTEGER DEFAULT 1 CHECK (quantity > 0),
    notes      TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now()
);

COMMENT ON TABLE meal_items IS '点餐明细 — 某顿饭里的某道菜，记录是谁选的';

-- 6. wishlists — 心愿单表
-- ============================================================
CREATE TABLE IF NOT EXISTS wishlists (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pair_id    UUID NOT NULL,
    dish_id    UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    added_by   UUID REFERENCES profiles(id),
    status     TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'tried', 'rejected')),
    notes      TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now()
);

COMMENT ON TABLE wishlists IS '心愿单 — "想尝试"的菜品列表';
COMMENT ON COLUMN wishlists.status IS '状态：pending=待尝试, tried=已尝试, rejected=已拒绝';
