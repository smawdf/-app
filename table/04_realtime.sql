-- ============================================================
-- OrderDisk — Supabase Realtime 配置
-- 说明：启用实时数据推送，设备 A 的操作即时同步到设备 B
-- 可重复执行：已加入 publication 的表会跳过，不会报错
-- ============================================================

-- 1. 设置 REPLICA IDENTITY FULL（UPDATE/DELETE 推送完整数据）
-- ============================================================
ALTER TABLE dishes     REPLICA IDENTITY FULL;
ALTER TABLE meals      REPLICA IDENTITY FULL;
ALTER TABLE meal_items REPLICA IDENTITY FULL;
ALTER TABLE wishlists  REPLICA IDENTITY FULL;

-- 2. 安全注册到 supabase_realtime publication（跳过已注册的）
-- ============================================================
DO $$
BEGIN
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE dishes;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'dishes already in publication, skipping';
    END;

    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE meals;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'meals already in publication, skipping';
    END;

    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE meal_items;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'meal_items already in publication, skipping';
    END;

    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE wishlists;
    EXCEPTION WHEN duplicate_object THEN
        RAISE NOTICE 'wishlists already in publication, skipping';
    END;
END;
$$;

-- ============================================================
-- Supabase Realtime 使用说明（Kotlin 端）
-- ============================================================
-- 订阅格式（通过 Supabase Kotlin SDK）：
-- ```kotlin
-- val channel = supabaseClient.realtime.createChannel("dishes") {
--     broadcast<PostgresAction.Insert>("dishes", filter = "pair_id=eq.${pairId}")
--     broadcast<PostgresAction.Update>("dishes", filter = "pair_id=eq.${pairId}")
--     broadcast<PostgresAction.Delete>("dishes", filter = "pair_id=eq.${pairId}")
-- }
-- channel.subscribe()
-- ```
-- ============================================================
