-- ============================================================
-- OrderDisk — Row Level Security (RLS) 策略
-- 说明：基于 pair_id 的数据隔离，用户只能访问自己配对组的数据
-- ============================================================
-- 防递归设计：
--   profiles 策略只用 user_id = auth.uid()，直接判断无子查询
--   其他表的子查询 SELECT pair_id FROM profiles WHERE user_id = auth.uid()
--   只会命中自己的 profile（由 profiles 策略保证），不会递归
-- ============================================================

-- 1. 启用所有表的 RLS
-- ============================================================
ALTER TABLE profiles   ENABLE ROW LEVEL SECURITY;
ALTER TABLE dishes     ENABLE ROW LEVEL SECURITY;
ALTER TABLE dish_tags  ENABLE ROW LEVEL SECURITY;
ALTER TABLE meals      ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE wishlists  ENABLE ROW LEVEL SECURITY;

-- 2. profiles 策略
-- ============================================================
-- 只允许操作自己的 profile，无递归
CREATE POLICY "Users can manage own profile"
    ON profiles FOR ALL
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 3. dishes 策略
-- ============================================================
CREATE POLICY "Pair members can view dishes"
    ON dishes FOR SELECT
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

CREATE POLICY "Pair members can insert dishes"
    ON dishes FOR INSERT
    WITH CHECK (
        pair_id IN (SELECT pair_id FROM profiles WHERE user_id = auth.uid())
    );

CREATE POLICY "Pair members can update dishes"
    ON dishes FOR UPDATE
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

CREATE POLICY "Pair members can delete dishes"
    ON dishes FOR DELETE
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

-- 4. dish_tags 策略
-- ============================================================
CREATE POLICY "Pair members can access dish tags"
    ON dish_tags FOR ALL
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

-- 5. meals 策略
-- ============================================================
CREATE POLICY "Pair members can access meals"
    ON meals FOR ALL
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

-- 6. meal_items 策略
-- ============================================================
CREATE POLICY "Pair members can access meal items"
    ON meal_items FOR ALL
    USING (meal_id IN (
        SELECT m.id FROM meals m
        JOIN profiles p ON p.pair_id = m.pair_id
        WHERE p.user_id = auth.uid()
    ));

-- 7. wishlists 策略
-- ============================================================
CREATE POLICY "Pair members can access wishlists"
    ON wishlists FOR ALL
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

-- ============================================================
-- 验证 RLS 启用状态
-- ============================================================
-- SELECT tablename, rowsecurity FROM pg_tables
-- WHERE schemaname = 'public';
