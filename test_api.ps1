$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8085/api/v1"

Write-Host "`n1. [POST] 注册两个测试账号（饲养员 & 吃货）..."
$caretakerJson = '{"username":"cook_dog","password":"123","nickname":"小金毛厨师","role":"caretaker"}'
$res1 = curl.exe -s -X POST -H "Content-Type: application/json" -d $caretakerJson "$baseUrl/auth/register" | ConvertFrom-Json
$caretakerToken = $res1.token
$caretakerId = $res1.user.id
Write-Host "   -> 饲养员注册成功, ID: $caretakerId"

$eaterJson = '{"username":"eat_dog","password":"123","nickname":"小马尔济斯","role":"eater"}'
$res2 = curl.exe -s -X POST -H "Content-Type: application/json" -d $eaterJson "$baseUrl/auth/register" | ConvertFrom-Json
$eaterToken = $res2.token
$eaterId = $res2.user.id
Write-Host "   -> 吃货注册成功, ID: $eaterId"

Write-Host "`n2. [POST] 饲养员创建情侣小店并生成 6 位邀请码..."
$pair = curl.exe -s -X POST -H "Authorization: Bearer $caretakerToken" "$baseUrl/pairs" | ConvertFrom-Json
$inviteCode = $pair.invite_code
$pairId = $pair.id
Write-Host "   -> 配对小店生成成功! PairID: $pairId, 邀请码: $inviteCode, 初始糖币: $($pair.candy_coins)"

Write-Host "`n3. [POST] 吃货输入邀请码完成配对绑定..."
$joinJson = "{`"invite_code`":`"$inviteCode`"}"
$joinRes = curl.exe -s -X POST -H "Authorization: Bearer $eaterToken" -H "Content-Type: application/json" -d $joinJson "$baseUrl/pairs/join" | ConvertFrom-Json
Write-Host "   -> 伴侣绑定成功! 双方已共享小店!"

# 吃货重新登录获取带有 PairID 的最新 Token
$eaterLoginJson = '{"username":"eat_dog","password":"123"}'
$resLogin = curl.exe -s -X POST -H "Content-Type: application/json" -d $eaterLoginJson "$baseUrl/auth/login" | ConvertFrom-Json
$eaterToken = $resLogin.token

# 饲养员重新登录获取带有 PairID 的最新 Token
$cookLoginJson = '{"username":"cook_dog","password":"123"}'
$resCookLogin = curl.exe -s -X POST -H "Content-Type: application/json" -d $cookLoginJson "$baseUrl/auth/login" | ConvertFrom-Json
$caretakerToken = $resCookLogin.token

Write-Host "`n4. [权限拦截测试] 尝试让饲养员直接下单（应被硬隔离拦截）..."
$orderBad = curl.exe -s -X POST -H "Authorization: Bearer $caretakerToken" -H "Content-Type: application/json" -d '{"items":[]}' "$baseUrl/orders"
Write-Host "   -> 权限拦截反馈: $orderBad"

Write-Host "`n5. [POST] 吃货挑选菜品提交点菜订单（扣除糖糖币）..."
$orderJson = '{
  "buyer_note": "小鸡翅要微辣，少放盐哦~",
  "items": [
    {"menu_id": "m1", "name": "可乐蜜汁小鸡翅", "unit_price": 18.5, "quantity": 1},
    {"menu_id": "m2", "name": "草莓舒芙蕾", "unit_price": 22.0, "quantity": 1}
  ]
}'
$order = curl.exe -s -X POST -H "Authorization: Bearer $eaterToken" -H "Content-Type: application/json" -d $orderJson "$baseUrl/orders" | ConvertFrom-Json
$orderId = $order.id
Write-Host "   -> 点单成功! 订单号: $orderId, 消费糖币: $($order.candy_coins_spent), 状态: $($order.status)"

Write-Host "`n6. [PUT] 饲养员推进做饭状态机: 接单 -> 备料制作中..."
$advanceJson1 = '{"new_status":"confirmed"}'
$resAdv1 = curl.exe -s -X PUT -H "Authorization: Bearer $caretakerToken" -H "Content-Type: application/json" -d $advanceJson1 "$baseUrl/orders/$orderId/status" | ConvertFrom-Json
Write-Host "   -> 饲养员已接单: $($resAdv1.status)"

$advanceJson2 = '{"new_status":"preparing"}'
$resAdv2 = curl.exe -s -X PUT -H "Authorization: Bearer $caretakerToken" -H "Content-Type: application/json" -d $advanceJson2 "$baseUrl/orders/$orderId/status" | ConvertFrom-Json
Write-Host "   -> 饲养员正在下锅烹饪: $($resAdv2.status)"

Write-Host "`n7. [POST] 饲养员投喂撒糖（充值糖糖币 +100）..."
$rechargeJson = '{"amount": 100, "reason": "今天吃货表现真乖"}'
$resRecharge = curl.exe -s -X POST -H "Authorization: Bearer $caretakerToken" -H "Content-Type: application/json" -d $rechargeJson "$baseUrl/candy/recharge" | ConvertFrom-Json
Write-Host "   -> 撒糖成功! 最新糖币余额: $($resRecharge.candy_coins), 提示: $($resRecharge.message)"

Write-Host "`n=== 自动化联调测试全部通过! ==="
