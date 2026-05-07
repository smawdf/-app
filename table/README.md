# OrderDisk — Supabase 接入指南

## 一、Supabase 是什么

Supabase 是 Firebase 的开源替代品，提供：
- **PostgreSQL 数据库** — 存储所有数据
- **Auth 认证** — 邮箱注册/登录
- **Realtime 实时同步** — 设备 A 操作即时推送到设备 B
- **Storage 文件存储** — 菜品图片上传
- **RLS (Row Level Security)** — 行级别安全策略

## 二、创建 Supabase 项目

### 步骤 1：注册并创建项目

1. 打开 https://supabase.com/ ，点击 **Start your project**
2. 用 GitHub 账号登录
3. 点击 **New project**
4. 填写：
   - **Name**：`orderdisk`（或自定义）
   - **Database Password**：设置一个强密码（**记下来！后面要用**）
   - **Region**：选择离用户最近的区域（如 `Asia Pacific (Singapore)`）
   - **Pricing Plan**：选 `Free` 即可
5. 点击 **Create project**，等待 1-2 分钟

### 步骤 2：获取 API 凭证

1. 项目创建完成后，进入项目 Dashboard
2. 左侧菜单 → **Settings** → **API**
3. 复制以下两个值：
   - **Project URL**：`https://xxxxxxxxxxxx.supabase.co`
   - **anon public key**：`eyJhbGciOi...`（长字符串）

### 步骤 3：配置到 App

打开 `app/src/main/java/com/myorderapp/ApiConfig.kt`，填入：

```kotlin
object ApiConfig {
    const val JUHE_API_KEY = "你的聚合数据key"   // 可选，没有则用内置
    const val SUPABASE_URL = "https://xxxxxxxxxxxx.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOi..."
}
```

## 三、导入数据库表结构

### 在 Supabase SQL Editor 中依次执行：

1. 左侧菜单 → **SQL Editor** → **New query**
2. 按顺序复制粘贴以下文件内容并执行（每个文件点 **Run**）：

| 顺序 | 文件 | 说明 |
|------|------|------|
| 1 | `01_schema.sql` | 创建 6 张核心表 |
| 2 | `02_indexes.sql` | 创建性能索引 |
| 3 | `03_rls_policies.sql` | 配置行级安全策略（pair 隔离） |
| 4 | `04_realtime.sql` | 开启实时数据推送 |
| 5 | `05_storage.sql` | 创建图片存储桶 |
| 6 | `06_fix_rls_recursion.sql` | 修复 profiles RLS 递归问题 |
| 7 | `07_global_dishes_rls.sql` | **菜品全局可见**（所有登录用户可见全部菜品） |
| 8 | `08_storage_global_upload.sql` | **图片上传**（所有认证用户可上传到 dish-images） |

> 验证：执行后在左侧 **Table Editor** 中应能看到 6 张表：
> `profiles`, `dishes`, `dish_tags`, `meals`, `meal_items`, `wishlists`

## 四、Supabase Auth 配置

### 4.1 启用邮箱注册

1. 左侧菜单 → **Authentication** → **Providers**
2. **Email** provider 默认已启用（确认一下）
3. 可选：关闭 **Confirm email**（开发阶段方便测试）
   - **Authentication** → **Settings** → 取消勾选 **Enable email confirmations**

## 五、数据架构说明

### 5.1 菜品：全局可见

所有登录用户可以看到全部菜品，不按 pair_id 隔离。

```
用户 A 添加的菜 → Supabase dishes 表 → 用户 B 也能看到
用户 B 添加的菜 → Supabase dishes 表 → 用户 A 也能看到
```

RLS 策略通过 `07_global_dishes_rls.sql` 实现：
- **SELECT**：所有认证用户可查看全部菜品
- **INSERT/UPDATE/DELETE**：仅可操作自己 pair_id 的菜品

### 5.2 配对：点餐同步

配对仅用于**点餐功能**的实时同步，不影响菜品库的可见性。

1. **用户 A** 注册/登录 → 生成配对码
2. **用户 B** 注册/登录 → 输入配对码 → B 的 `pair_id` 更新为 A 的 `pair_id`
3. 配对后，两人的点餐数据（meals/meal_items）实时同步

### 5.3 本地持久化

App 同时使用云端和本地两层存储，断网也能用：

| 数据 | 在线（已登录+有网） | 离线（没网） |
|------|-------------------|------------|
| 菜品 | Supabase + 本地 JSON 文件 | 本地 JSON 文件 |
| 个人资料 | Supabase + SharedPreferences | SharedPreferences |
| 登录态 | JWT token (SharedPreferences) | JWT token（离线也保留） |

- 本地文件路径：`filesDir/dishes.json`、`filesDir/dishes_cloud.json`
- SharedPreferences key：`orderdisk_session`（token）、`profile_prefs`（昵称/头像）

## 六、表结构快速参考

```
profiles ── 用户资料（昵称/头像/口味偏好）
dishes   ── 菜品库（全局可见，所有用户共享）
dish_tags ── 菜品标签
meals    ── 点餐记录（按 pair_id 隔离）
meal_items ── 点餐明细
wishlists ── 心愿单（按 pair_id 隔离）
```
