-- ============================================================
-- OrderDisk — 索引脚本
-- 说明：提升查询性能的索引定义
-- ============================================================

-- profiles 索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_profiles_pair_id ON profiles(pair_id);
CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id);

-- dishes 索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_dishes_pair_id ON dishes(pair_id);
CREATE INDEX IF NOT EXISTS idx_dishes_source ON dishes(pair_id, source);
CREATE INDEX IF NOT EXISTS idx_dishes_category ON dishes(pair_id, category);
CREATE INDEX IF NOT EXISTS idx_dishes_name_search ON dishes USING gin(to_tsvector('simple', name));
CREATE INDEX IF NOT EXISTS idx_dishes_external_id ON dishes(external_id, external_source);
CREATE INDEX IF NOT EXISTS idx_dishes_created_at ON dishes(pair_id, created_at DESC);

-- 为 JSONB cook_steps 建的 GIN 索引（支持全文搜索步骤描述）
-- CREATE INDEX IF NOT EXISTS idx_dishes_cook_steps ON dishes USING gin(cook_steps);

-- dish_tags 索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_dish_tags_pair_id ON dish_tags(pair_id);
CREATE INDEX IF NOT EXISTS idx_dish_tags_dish_id ON dish_tags(dish_id);
CREATE INDEX IF NOT EXISTS idx_dish_tags_name ON dish_tags(pair_id, name);

-- meals 索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_meals_pair_id ON meals(pair_id);
CREATE INDEX IF NOT EXISTS idx_meals_date ON meals(pair_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_meals_status ON meals(pair_id, status);

-- meal_items 索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_meal_items_meal_id ON meal_items(meal_id);
CREATE INDEX IF NOT EXISTS idx_meal_items_dish_id ON meal_items(dish_id);
CREATE INDEX IF NOT EXISTS idx_meal_items_chosen_by ON meal_items(chosen_by);

-- wishlists 索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_wishlists_pair_id ON wishlists(pair_id);
CREATE INDEX IF NOT EXISTS idx_wishlists_status ON wishlists(pair_id, status);
CREATE INDEX IF NOT EXISTS idx_wishlists_dish_id ON wishlists(dish_id);
