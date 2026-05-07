-- ============================================================
-- 全局菜品可见 — 所有登录用户可看所有菜品
-- 执行方式：在 Supabase SQL Editor 中运行
-- ============================================================

-- 删除旧的 dishes SELECT 策略（仅 pair 内可见）
DROP POLICY IF EXISTS "Pair members can view dishes" ON dishes;

-- 新建：所有认证用户可以查看所有菜品
CREATE POLICY "Authenticated users can view all dishes"
    ON dishes FOR SELECT
    USING (auth.role() = 'authenticated');

-- 插入/修改/删除策略保持不变（只能操作自己 pair 的菜品）
-- 如需所有人都能编辑，取消下面注释：
-- DROP POLICY IF EXISTS "Pair members can insert dishes" ON dishes;
-- CREATE POLICY "Authenticated users can insert dishes"
--     ON dishes FOR INSERT
--     WITH CHECK (auth.role() = 'authenticated');
