$ErrorActionPreference = "Stop"
$base = "http://localhost:8085/api/v1"

function Post($url, $body, $token) {
  $headers = @("Content-Type: application/json")
  if ($token) { $headers += "Authorization: Bearer $token" }
  $args = @("-s", "-X", "POST", $url) + ($headers | ForEach-Object { @("-H", $_) }) + @("-d", $body)
  return (curl.exe @args | Out-String)
}
function Get2($url, $token) {
  return (curl.exe -s -H "Authorization: Bearer $token" $url | Out-String)
}

Write-Host "`n[1] 注册饲养员与吃货..."
$c = (Post "$base/auth/register" '{"username":"cook2","password":"123","nickname":"小金毛","role":"caretaker"}' $null) | ConvertFrom-Json
$e = (Post "$base/auth/register" '{"username":"eat2","password":"123","nickname":"小马","role":"eater"}' $null) | ConvertFrom-Json
$cookToken = $c.token
$eatToken  = $e.token
Write-Host "    饲养员: $($c.user.nickname)  吃货: $($e.user.nickname)"

Write-Host "`n[2] 饲养员创建小店（应自动预置 6 道招牌菜）..."
$pair = (Post "$base/pairs" '{}' $cookToken) | ConvertFrom-Json
$cookToken = $pair.token
Write-Host "    邀请码: $($pair.invite_code)  初始糖币: $($pair.candy_coins)"

Write-Host "`n[3] 吃货用邀请码绑定伴侣..."
$join = (Post "$base/pairs/join" ("{`"invite_code`":`"$($pair.invite_code)`"}" ) $eatToken) | ConvertFrom-Json
$eatToken = $join.token
Write-Host "    绑定成功, pair_id=$($join.id)"

Write-Host "`n[4] GET /me （前端启动档案）..."
$me = (Get2 "$base/me" $eatToken) | ConvertFrom-Json
Write-Host "    paired=$($me.paired)  糖币=$($me.pair.candy_coins)  小店=$($me.shop.name)"

Write-Host "`n[5] GET /menu （菜单，验证预置数据）..."
$menu = (Get2 "$base/menu" $eatToken) | ConvertFrom-Json
Write-Host "    菜品数量: $($menu.items.Count)"
foreach ($it in $menu.items) { Write-Host "      - $($it.image_url) $($it.name)  $($it.price) 糖币" }

Write-Host "`n[6] 吃货下单（取菜单前两样）..."
$first  = $menu.items[0]
$second = $menu.items[1]
$orderBody = @"
{"buyer_note":"少放盐哦","items":[
 {"menu_id":"$($first.id)","name":"$($first.name)","image_url":"$($first.image_url)","unit_price":$($first.price),"quantity":1},
 {"menu_id":"$($second.id)","name":"$($second.name)","image_url":"$($second.image_url)","unit_price":$($second.price),"quantity":2}]}
"@
$order = (Post "$base/orders" $orderBody $eatToken) | ConvertFrom-Json
Write-Host "    订单号: $($order.id)"
Write-Host "    总价: $($order.total_price)  消耗糖币: $($order.candy_coins_spent)  状态: $($order.status)"

Write-Host "`n[7] GET /orders （订单列表）..."
$orders = (Get2 "$base/orders" $cookToken) | ConvertFrom-Json
Write-Host "    订单数: $($orders.orders.Count)  首个订单明细数: $($orders.orders[0].items.Count)"

Write-Host "`n[8] 饲养员推进做饭状态..."
$adv = (Post "$base/orders/$($order.id)/status" '{"new_status":"confirmed"}' $cookToken)
Write-Host ""

Write-Host "`n[9] GET /candy/transactions （糖币流水）..."
$tx = (Get2 "$base/candy/transactions" $eatToken) | ConvertFrom-Json
foreach ($t in $tx.transactions) { Write-Host "      $($t.type)  $($t.amount)  余额=$($t.balance)  $($t.description)" }

Write-Host "`n=== 后端全链路联调通过 ==="
