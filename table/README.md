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
12. `15_pair_invite_role.sql`：补充邀请预览、角色字段与配对预览策略。
13. `16_single_device_session.sql`：单设备登录字段，记录 `session_id` 与 `session_updated_at`。
14. `17_cloud_user_assets.sql`：头像、店铺、菜单、糖糖币等云端字段。
15. `18_user_preferences.sql`：用户偏好设置同步。
16. `19_user_scoped_shop_sync.sql`：未绑定与已绑定用户的店铺同步范围。
17. `20_client_error_logs.sql`：客户端同步错误日志。
18. `21_atomic_candy_coins.sql`：糖糖币原子扣减。
19. `22_secure_pairing.sql`：安全配对 RPC。
20. `23_atomic_order_transitions.sql`：订单状态与退款原子操作。
21. `24_atomic_partner_candy_coins.sql`：伴侣糖糖币充值。
22. `25_disable_email_confirmation_cleanup.sql`：关闭邮箱确认后的账号清理兼容。
23. `26_pair_invites.sql`：持久化邀请码。
24. `27_pair_events_and_atomic_unpair.sql`：绑定通知与原子解绑。
25. `28_pair_snapshot.sql`：情侣资料快照。
26. `29_profiles_integrity_and_pair_snapshot_fix.sql`：资料唯一性和快照修复。
27. `30_missing_profiles_and_upsert_fix.sql`：缺失资料自动补齐。
28. `31_eater_candy_wallet.sql`：吃货独立糖糖币钱包。
29. `32_profile_null_session_cleanup.sql`：兼容空设备会话字段。
30. `33_pair_sync_integrity.sql`：店铺删除同步和情侣钱包一致性。
31. `34_eater_only_ordering.sql`：仅吃货可提交点菜。
32. `35_caretaker_order_acceptance.sql`：仅饲养员可接单和完成订单。
33. `36_private_storage_scope.sql`：图片上传限制到当前账号或情侣目录。
34. `37_sweet_moment_images.sql`：甜蜜时刻自定义图片字段与安全更新 RPC。

## 当前 App 事实

- 配对码是 6 位文本，`pair_id` 必须是 `text`。
- 用户身份当前为 `caretaker`/`eater`，配对邀请会根据邀请方角色自动确定被邀请方角色。
- 当前私人版允许同一账号在多台设备登录；历史设备会话字段仅保留兼容，不再用于挤下线。
- 点菜消息接收是 App 端轮询订单，不是 Realtime 推送。
- 当前菜谱搜索使用 Tian、本地中文菜谱、下厨房搜索与 Bing/外部图片兜底。
- 旧 Jisu/Juhe 接入已移除；同步异常会写入 `client_error_logs`，用于私人版排错。
- 首次部署或迁移环境必须按顺序执行到 `37_sweet_moment_images.sql`。
