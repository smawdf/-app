# 高糖小食（OrderDisk）现代 Go 后端 API 接口文档

- **服务基地址**：`http://<server-ip>:8085/api/v1`
- **实时长连接 (WebSocket)**：`ws://<server-ip>:8085/ws?pair_id={pair_id}&user_id={user_id}`
- **鉴权方式**：HTTP Header `Authorization: Bearer {token}`

---

## 1. 认证接口 (Auth)

### 1.1 注册账号
- **URL**: `POST /auth/register`
- **Body (JSON)**:
  ```json
  {
    "username": "cook_dog",
    "password": "123",
    "nickname": "小金毛厨师",
    "role": "caretaker" // "caretaker"(饲养员) 或 "eater"(吃货)
  }
  ```
- **Response**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5c...",
    "user": {
      "id": "1467a7c3-caa0-414c-bd99-5075cb5d7a95",
      "username": "cook_dog",
      "nickname": "小金毛厨师",
      "role": "caretaker",
      "pair_id": ""
    }
  }
  ```

### 1.2 账号登录
- **URL**: `POST /auth/login`
- **Body (JSON)**:
  ```json
  {
    "username": "cook_dog",
    "password": "123"
  }
  ```

---

## 2. 情侣配对与小店 (Pair & Shop)

### 2.1 创建小店并生成 6 位邀请码
- **URL**: `POST /pairs`
- **Headers**: `Authorization: Bearer {token}`
- **Response**:
  ```json
  {
    "id": "5defed63-cbbf-447f-8a4e-47c34be882c6",
    "invite_code": "HZHPAN",
    "caretaker_id": "1467a7c3-caa0-414c-bd99-5075cb5d7a95",
    "eater_id": "",
    "candy_coins": 66
  }
  ```

### 2.2 输入邀请码加入并绑定伴侣
- **URL**: `POST /pairs/join`
- **Headers**: `Authorization: Bearer {token}`
- **Body (JSON)**:
  ```json
  {
    "invite_code": "HZHPAN"
  }
  ```

---

## 3. 糖糖币业务 (Candy Coins)

### 3.1 饲养员投喂撒糖（充值糖币）
> **权限要求**：只有 `caretaker` (饲养员) 可调用。
- **URL**: `POST /candy/recharge`
- **Headers**: `Authorization: Bearer {caretaker_token}`
- **Body (JSON)**:
  ```json
  {
    "amount": 100,
    "reason": "今天吃货表现真乖"
  }
  ```
- **Response**:
  ```json
  {
    "candy_coins": 125,
    "message": "撒糖成功，对方已实时收到提醒！🍬"
  }
  ```

---

## 4. 点菜与做饭订单流转 (Orders)

### 4.1 吃货选菜下单（强事务扣除糖币）
> **权限要求**：只有 `eater` (吃货) 可调用；饲养员调用会被拦截并返回 `403 EATER_ROLE_REQUIRED`。
- **URL**: `POST /orders`
- **Headers**: `Authorization: Bearer {eater_token}`
- **Body (JSON)**:
  ```json
  {
    "buyer_note": "小鸡翅要微辣，少放盐哦~",
    "items": [
      {
        "menu_id": "m1",
        "name": "可乐蜜汁小鸡翅",
        "image_url": "https://example.com/wing.png",
        "unit_price": 18.5,
        "quantity": 1
      },
      {
        "menu_id": "m2",
        "name": "草莓舒芙蕾",
        "image_url": "https://example.com/cake.png",
        "unit_price": 22.0,
        "quantity": 1
      }
    ]
  }
  ```
- **Response**:
  ```json
  {
    "id": "bc8743af-d2bf-4620-9027-366799ceb06c",
    "pair_id": "5defed63-cbbf-447f-8a4e-47c34be882c6",
    "buyer_name": "小马尔济斯",
    "status": "submitted",
    "subtotal": 40.5,
    "total_price": 40.5,
    "candy_coins_spent": 41,
    "items": [...]
  }
  ```

### 4.2 饲养员推进做饭状态机
> **权限要求**：只有 `caretaker` (饲养员) 可调用。
- **URL**: `PUT /orders/:order_id/status`
- **Headers**: `Authorization: Bearer {caretaker_token}`
- **Body (JSON)**:
  ```json
  {
    "new_status": "confirmed" // "confirmed"(接单), "preparing"(备料烹饪中), "delivering"(端盘上桌), "completed"(吃光开饭)
  }
  ```

### 4.3 取消订单（自动原路退回糖币）
- **URL**: `POST /orders/:order_id/cancel`
- **Headers**: `Authorization: Bearer {token}`

---

## 5. WebSocket 实时推送事件一览 (WebSocket Events)

连接地址：`ws://<host>:8085/ws?pair_id={pair_id}&user_id={user_id}`

| 事件 Type | 触发时机 | 携带 Payload | 前端相应表现 |
| :--- | :--- | :--- | :--- |
| `order_created` | 吃货下单成功 | `Order` 完整对象 | 饲养员端弹出铃铛提单、播放软萌“叮咚”音效、飘出糖币动效 |
| `order_updated` | 饲养员推进状态 | `Order` 最新状态 | 吃货端菜品状态实时变为“备料中/快出锅啦” |
| `candy_changed` | 饲养员投喂撒糖 | `{"candy_coins": 125, "delta": 100}` | 吃货端糖币余额动画数字跳动 +100，弹出爱心撒糖动画 |
| `pair_joined` | 伴侣输入邀请码 | `Pair` 对象 | 首页实时由虚线加号刷新为双方头像与做饭天数 |
