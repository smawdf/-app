-- ============================================================
-- OrderDisk — 修复 RLS 无限递归
-- 说明：在 Supabase SQL Editor 中先运行此脚本，再运行 03_rls_policies.sql
-- 问题：旧的 "Users can view partner profile" 策略导致 profiles 自查询递归
-- ============================================================

-- 1. 删除旧 profiles 策略（引起递归的）
DROP POLICY IF EXISTS "Users can view own profile" ON profiles;
DROP POLICY IF EXISTS "Users can view partner profile" ON profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON profiles;
DROP POLICY IF EXISTS "Users can insert own profile" ON profiles;

-- 2. 删除旧 dishes 策略
DROP POLICY IF EXISTS "Pair members can view dishes" ON dishes;
DROP POLICY IF EXISTS "Pair members can insert dishes" ON dishes;
DROP POLICY IF EXISTS "Pair members can update dishes" ON dishes;
DROP POLICY IF EXISTS "Pair members can delete dishes" ON dishes;

-- 3. 删除旧 dish_tags 策略
DROP POLICY IF EXISTS "Pair members can access dish tags" ON dish_tags;

-- 4. 删除旧 meals 策略
DROP POLICY IF EXISTS "Pair members can access meals" ON meals;

-- 5. 删除旧 meal_items 策略
DROP POLICY IF EXISTS "Pair members can access meal items" ON meal_items;

-- 6. 删除旧 wishlists 策略
DROP POLICY IF EXISTS "Pair members can access wishlists" ON wishlists;

-- 策略已清除，现在请执行 03_rls_policies.sql 重建正确的策略
