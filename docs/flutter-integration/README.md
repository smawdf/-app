# Flutter + Go 前后端联调验证报告

> 验证环境：雷电模拟器 9（Android 9 / API 28，x86_64）
> 后端：`orderdisk-server`（Go 1.27.1 + Gin + GORM + SQLite + WebSocket），监听 `:8085`
> 前端：`OrderDisk_Flutter`（Flutter 3.47.2 + Impeller），纯白底色 + 水滴液态玻璃

## 一、验证结论

**全链路打通。** 从登录、配对、拉菜单、加购、下单扣糖到订单列表，全部走真实 Go 后端，无任何 Mock。

| 步骤 | 验证点 | 结果 |
| :--- | :--- | :--- |
| 1 | 登录 `POST /auth/login` → JWT 鉴权 | ✅ |
| 2 | `GET /me` 拉取用户 + 配对 + 小店 | ✅ 标题显示「我们的小家餐厅 💕」（来自 Shop 表） |
| 3 | `GET /menu` 拉取菜单 | ✅ 6 道菜全部来自数据库 |
| 4 | 加购 → 悬浮玻璃购物车条 | ✅ 实时显示数量/金额/糖币消耗 |
| 5 | `POST /orders` 下单 + 事务扣糖 | ✅ 糖币 104 → 86，扣款准确 |
| 6 | 余额不足拦截 | ✅ 糖币仅剩 4 时下单被拒绝 |
| 7 | `GET /orders` 订单列表 | ✅ 两笔订单、状态标签、菜品明细、备注完整 |

## 二、截图证据

| 文件 | 说明 |
| :--- | :--- |
| [01-login.png](./01-login.png) | 纯白登录页，含后端地址提示与演示账号快捷填入 |
| [02-ordering-backend-data.png](./02-ordering-backend-data.png) | 登录成功；标题/公告/糖币/菜品**全部来自后端** |
| [03-glass-cart-bar.png](./03-glass-cart-bar.png) | 加购后弹出**悬浮水滴玻璃购物车条** |
| [04-order-submitted.png](./04-order-submitted.png) | 下单成功提示「已消费 18 糖币」，余额同步刷新 |
| [05-orders-list.png](./05-orders-list.png) | 订单列表：状态标签、明细、备注、取消按钮 |

## 三、联调中定位并修复的三个关键问题

### 1. Android 9 默认禁止明文 HTTP（导致请求根本发不出去）
API 28 起系统默认 `usesCleartextTraffic=false`，`http://<host>:8085` 被静默拦截。
**注意**：`adb shell curl` 不受该限制，所以命令行测试能通、App 却不行，极易误判为后端问题。
修复：`android/app/src/main/AndroidManifest.xml` 增加 `android:usesCleartextTraffic="true"` 与 `INTERNET` 权限。

### 2. 自定义玻璃组件的点击命中缺陷
`LiquidDropGlass` 内部 `Container` 使用 `decoration`（渐变）渲染为 `DecoratedBox`，
它**不参与自身命中测试**；而 `GestureDetector` 默认 `HitTestBehavior.deferToChild`，
导致只有点到文字才能触发。
修复：显式设置 `behavior: HitTestBehavior.opaque`。

### 3. 自动化定位元素：Flutter 语义树
Flutter 默认不构建语义树，`uiautomator dump` 看不到控件。
修复：`main()` 中调用 `SemanticsBinding.instance.ensureSemantics()`，
既让自动化测试能精确定位控件，也提升了无障碍可达性。

## 四、当前架构

```
Flutter (纯白 + 水滴玻璃)                Go 后端 (:8085)
├── ui/theme/cozy_glass.dart   ←→       ├── /api/v1/auth/*      JWT + bcrypt
├── ui/auth/auth_screen.dart            ├── /api/v1/pairs/*     6 位邀请码配对
├── ui/auth/pair_screen.dart            ├── /api/v1/me          档案聚合
├── ui/shell/main_shell.dart            ├── /api/v1/menu        菜单读写(角色隔离)
├── ui/pages/ordering_page.dart         ├── /api/v1/orders/*    订单状态机 + 事务扣糖
├── ui/pages/orders_page.dart           ├── /api/v1/candy/*     糖币流水
├── ui/pages/profile_page.dart          └── /ws                 WebSocket 实时双向推送
└── data/{api_client,app_state,models}      Gin + GORM + SQLite
```

## 五、已知待办

1. **Token 持久化**：当前会话存于内存，App 重启需重新登录。建议接入 `shared_preferences`。
2. **WebSocket 双端实时冒烟**：代码已实现（`order_created` / `candy_changed` / `menu_updated` 等事件），
   但尚未用两台设备实测推送。
3. **菜品真实图片**：当前后端 `image_url` 存的是 emoji 占位符，需接入真实图床。
4. **发糖后端地址可配置**：已支持 `--dart-define=API_HOST=... --dart-define=API_PORT=...`，
   正式构建时应改为域名 + HTTPS（届时可移除 `usesCleartextTraffic`）。
