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
| 3 | `03_rls_policies.sql` | 配置行级安全策略 |
| 4 | `04_realtime.sql` | 开启实时数据推送 |
| 5 | `05_storage.sql` | 创建图片存储桶 |

> 验证：执行后在左侧 **Table Editor** 中应能看到 6 张表：
> `profiles`, `dishes`, `dish_tags`, `meals`, `meal_items`, `wishlists`

## 四、Supabase Auth 配置

### 4.1 启用邮箱注册

1. 左侧菜单 → **Authentication** → **Providers**
2. **Email** provider 默认已启用（确认一下）
3. 可选：关闭 **Confirm email**（开发阶段方便测试）
   - **Authentication** → **Settings** → 取消勾选 **Enable email confirmations**

### 4.2 测试认证

```kotlin
// 注册
val supabaseClient = createSupabaseClient(
    supabaseUrl = ApiConfig.SUPABASE_URL,
    supabaseKey = ApiConfig.SUPABASE_ANON_KEY
)
supabaseClient.auth.signUpWithEmail(email = "test@example.com", password = "123456")

// 登录
supabaseClient.auth.signInWithEmail(email = "test@example.com", password = "123456")
```

## 五、配对流程

两人使用同一个 App 的配对机制：

1. **用户 A** 注册/登录 → 生成一个配对码（用 `pair_id` 生成 6 位数字）
2. **用户 B** 注册/登录 → 输入配对码 → 将 B 的 `pair_id` 更新为 A 的 `pair_id`
3. 之后两人共享 `pair_id`，RLS 策略自动让他们看到彼此的数据

```sql
-- A 生成配对码后，B 加入配对
UPDATE profiles
SET pair_id = 'A的pair_id'
WHERE user_id = 'B的user_id';
```

## 六、Supabase Kotlin SDK 关键用法

### 查询菜品（带本地缓存）

```kotlin
// 在线：Supabase Realtime 流
supabaseClient.from("dishes")
    .select { filter { eq("pair_id", pairId) } }
    .decodeList<Dish>()

// 离线：Room 本地缓存作为降级
```

### 实时订阅

```kotlin
val channel = supabaseClient.realtime.createChannel("dishes") {
    broadcast<PostgresAction.Insert>("dishes")
    broadcast<PostgresAction.Update>("dishes")
    broadcast<PostgresAction.Delete>("dishes")
}
channel.subscribe()
```

### 上传菜品图片

```kotlin
val bucket = supabaseClient.storage.from("dish-images")
val path = "$pairId/$dishId/${System.currentTimeMillis()}.jpg"
bucket.upload(path, imageBytes).upsert()
val publicUrl = bucket.publicUrl(path)
```

## 七、表结构快速参考

```
profiles ──1:N── dishes
profiles ──1:N── meals
dishes   ──N:M── meals (through meal_items)
dishes   ──1:N── wishlists
dishes   ──1:N── dish_tags
```
