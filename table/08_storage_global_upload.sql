-- ============================================================
-- 简化 Storage RLS — 所有认证用户可上传图片
-- 执行方式：在 Supabase SQL Editor 中运行
-- ============================================================

-- 删除旧的目录级隔离策略
DROP POLICY IF EXISTS "Users can upload to their pair folder" ON storage.objects;

-- 新建：所有认证用户可上传到 dish-images
CREATE POLICY "Authenticated users can upload images"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'dish-images'
        AND auth.role() = 'authenticated'
    );
