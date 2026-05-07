-- ============================================================
-- OrderDisk — Supabase Storage 配置
-- 说明：菜品图片存储，按 pair_id 分子目录隔离
-- ============================================================

-- 1. 创建存储桶
-- ============================================================
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'dish-images',
    'dish-images',
    true,                              -- public: 任何人可查看（通过 URL）
    10485760,                          -- 单文件最大 10MB
    '{image/jpeg,image/png,image/webp,image/gif}'
)
ON CONFLICT (id) DO NOTHING;

-- 2. 查看策略：公开可读（先删后建，可重复执行）
-- ============================================================
DROP POLICY IF EXISTS "Anyone can view dish images" ON storage.objects;
CREATE POLICY "Anyone can view dish images"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'dish-images');

-- 3. 上传策略：只能上传到自己 pair_id 对应子目录
-- ============================================================
DROP POLICY IF EXISTS "Users can upload to their pair folder" ON storage.objects;
CREATE POLICY "Users can upload to their pair folder"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'dish-images'
        AND (storage.foldername(name))[1] IN (
            SELECT pair_id::text FROM profiles WHERE user_id = auth.uid()
        )
    );

-- 4. 更新策略：只能更新自己上传的
-- ============================================================
DROP POLICY IF EXISTS "Users can update their own images" ON storage.objects;
CREATE POLICY "Users can update their own images"
    ON storage.objects FOR UPDATE
    USING (
        bucket_id = 'dish-images'
        AND owner = auth.uid()
    );

-- 5. 删除策略：只能删除自己上传的
-- ============================================================
DROP POLICY IF EXISTS "Users can delete their own images" ON storage.objects;
CREATE POLICY "Users can delete their own images"
    ON storage.objects FOR DELETE
    USING (
        bucket_id = 'dish-images'
        AND owner = auth.uid()
    );

-- ============================================================
-- Storage 路径规划
-- ============================================================
-- 文件路径格式：{pair_id}/{dish_id}/{timestamp}_{random}.jpg
-- 示例：d7f3a1b2-.../c8e4f5a6-.../20260505_143022_a1b2c3.jpg
--
-- Kotlin 端上传示例：
-- ```kotlin
-- val bucket = supabaseClient.storage.from("dish-images")
-- val path = "$pairId/$dishId/${System.currentTimeMillis()}.jpg"
-- bucket.upload(path, imageBytes).upsert()
-- val publicUrl = bucket.publicUrl(path)
-- ```
-- ============================================================
