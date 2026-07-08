# OrderDisk Supabase SQL

OrderDisk 当前使用 Supabase Auth、Postgrest、Storage。订单接收采用 App 端短轮询，不再依赖 Supabase Realtime。

## 推荐执行顺序

在 Supabase SQL Editor 中按需执行：

1. `01_schema.sql`：核心资料、菜品、餐次表。
2. `02_indexes.sql`：索引。
3. `03_rls_policies.sql`：基础 RLS。
4. `05_storage.sql`：图片存储。
5. `06_fix_rls_recursion.sql`：清理旧递归 RLS。
6. `07_global_dishes_rls.sql`：历史兼容的菜品可见策略。
7. `08_storage_global_upload.sql`：图片上传兼容策略。
8. `10_required_runtime_fix.sql`：当前下单/订单所需表。
9. `11_pair_order_notifications.sql`：情侣订单共享字段与策略。
10. `12_candy_coins.sql`：糖糖币字段。
11. `14_pair_id_text_runtime_fix.sql`：把配对码链路统一为 6 位文本 `pair_id`，并重建相关 RLS。

## 当前 App 事实

- 配对码是 6 位文本，`pair_id` 必须是 `text`。
- 点菜消息接收是 App 端轮询订单，不是 Realtime 推送。
- 当前菜谱搜索使用 Tian、本地中文菜谱、下厨房搜索与 Bing/外部图片兜底。
- 旧 Jisu/Juhe 接入和云端 debug 日志已移除。
