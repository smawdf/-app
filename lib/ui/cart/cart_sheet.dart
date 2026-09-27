import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../../data/food_images.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

/// 原生 `yuanText(value) = "¥" + "%.2f".format(value)`
/// （OrderDisk/app/src/main/java/com/myorderapp/ui/util/PriceText.kt:3）
String _yuanText(double value) => '¥${value.toStringAsFixed(2)}';

/// 购物车详情半屏弹层。
///
/// 原生对应：
/// - `ui/shop/components/CartSheet.kt`（点餐页里的半屏购物篮）
/// - `ui/cart/CartScreen.kt`（清单卡片 / 数量步进器 / 空态 / 清空 / 结算栏）
///
/// 只保留原文件已有的接线（onAdd / onRemove / onClear / onCheckout / candyBalance）。
class CartDetailSheet extends StatelessWidget {
  final Map<String, MenuItem> cart;
  final Map<String, int> qty;
  final ValueChanged<MenuItem> onAdd;
  final ValueChanged<MenuItem> onRemove;
  final VoidCallback onClear;
  final VoidCallback onCheckout;
  final int candyBalance;

  const CartDetailSheet({
    super.key,
    required this.cart,
    required this.qty,
    required this.onAdd,
    required this.onRemove,
    required this.onClear,
    required this.onCheckout,
    required this.candyBalance,
  });

  /// 对应原生 `CartState.itemCount`（domain/model/Cart.kt:27）
  int get totalCount => qty.values.fold(0, (a, b) => a + b);

  /// 对应原生 `CartState.subtotal / totalPrice`（本工程无配送费，两者相等）
  double get totalPrice =>
      qty.entries.fold(0.0, (sum, e) => sum + (cart[e.key]!.price * e.value));

  double get subtotal => totalPrice;

  /// 对应原生 `candyCoinsCost(totalPrice) = ceil(totalPrice)`（CheckoutViewModel.kt:144）
  int get coinCost => totalPrice.ceil();

  @override
  Widget build(BuildContext context) {
    final entries = <MapEntry<String, int>>[
      for (final e in qty.entries)
        if (cart.containsKey(e.key)) e,
    ];
    final bool isEmpty = entries.isEmpty;

    // 【玻璃】原来是白底 Container（原生 CartSheet.kt:50），现在换成玻璃外壳；
    // 外层把手由这一层自己的 Column 画，所以 showHandle: false，避免两条把手。
    return CozyGlassSheet(
      showHandle: false,
      // 原生 CartSheet.kt:52 `padding(start = 20, top = 10, end = 20, bottom = 20)`
      padding: EdgeInsets.fromLTRB(
        18,
        12,
        18,
        MediaQuery.of(context).padding.bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        // 原生 CozyCard 自带 fillMaxWidth（StitchNativeComponents.kt:239）
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 原生 ModalBottomSheet 默认拖拽把手（32 × 4）
          Center(
            child: Container(
              width: 32,
              height: 4,
              decoration: BoxDecoration(
                color: CozyPalette.onSurfaceVariant.withValues(alpha: 0.4),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 12),
          _cartHeader(context, isEmpty),
          const SizedBox(height: 14), // 原生 spacedBy(14.dp)
          if (isEmpty) _emptyCartState(context) else _cartItemList(entries),
          const SizedBox(height: 14),
          _cartSummaryCard(context, isEmpty),
        ],
      ),
    );
  }

  /// 原生 `CartSheet.kt:55-73` 头部 + `CartScreen.kt:141-150` 清空按钮
  Widget _cartHeader(BuildContext context, bool isEmpty) {
    final text = Theme.of(context).textTheme;
    // 原生 `CartSheet.kt:66` 用购物篮所属小店名，空则显示「购物篮」
    final shopName = AppState.instance.shop?.name ?? '';

    return Row(
      children: [
        // 原生 Surface(CircleShape, #FFD1DC@0.64) + Icon(ShoppingBasket, padding 12, size 24)
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: CozyPalette.secondaryContainer.withValues(alpha: 0.64),
            shape: BoxShape.circle,
          ),
          child: const Icon(
            Icons.shopping_basket,
            size: 24,
            color: CozyPalette.primary,
          ),
        ),
        const SizedBox(width: 12), // 原生 spacedBy(12.dp)
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                shopName.isEmpty ? '购物篮' : shopName,
                style: text.titleLarge!.copyWith(
                  color: CozyPalette.onSurface,
                  fontWeight: FontWeight.w900,
                ),
              ),
              Text(
                '确认购物篮后进入确认点菜',
                style: text.bodySmall!.copyWith(
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        if (!isEmpty)
          TextButton(
            onPressed: () {
              HapticFeedback.lightImpact();
              onClear();
              Navigator.pop(context);
            },
            style: TextButton.styleFrom(
              foregroundColor: CozyPalette.primary,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              minimumSize: Size.zero,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.delete, size: 18),
                SizedBox(width: 8),
                Text('清空'),
              ],
            ),
          ),
      ],
    );
  }

  /// 原生 `CartScreen.kt:154-179` EmptyCartState
  Widget _emptyCartState(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return CozyCard(
      // 原生 `modifier.padding(28.dp)`（外间距），contentPadding 为默认 18
      margin: const EdgeInsets.all(28),
      padding: const EdgeInsets.all(18),
      radius: 24,
      child: Column(
        children: [
          // 原生 Surface(CircleShape, #FFD1DC@0.62) + Icon(ShoppingCart, padding 18, size 36)
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: CozyPalette.secondaryContainer.withValues(alpha: 0.62),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.shopping_cart,
              size: 36,
              color: CozyPalette.primary,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            '购物篮为空',
            style: text.bodyLarge!.copyWith(
              color: CozyPalette.onSurface,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            '回到点菜页添加喜欢的菜品。',
            textAlign: TextAlign.center,
            style: text.bodySmall!.copyWith(
              color: CozyPalette.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  /// 原生 `CartSheet.kt:75-98` 的 LazyColumn（spacedBy 10）+ `CartScreen.kt:80-103` 的卡片
  Widget _cartItemList(List<MapEntry<String, int>> entries) {
    return ConstrainedBox(
      constraints: const BoxConstraints(maxHeight: 320),
      child: ListView.separated(
        shrinkWrap: true,
        padding: EdgeInsets.zero,
        itemCount: entries.length,
        separatorBuilder: (BuildContext context, int index) =>
            const SizedBox(height: 10),
        itemBuilder: (BuildContext context, int index) {
          final String id = entries[index].key;
          return _cartItemCard(context, cart[id]!, entries[index].value);
        },
      ),
    );
  }

  /// 原生 `CartSheet.kt:80-96` / `CartScreen.kt:81-102` 的清单卡片
  Widget _cartItemCard(BuildContext context, MenuItem item, int quantity) {
    final text = Theme.of(context).textTheme;
    return CozyCard(
      radius: 24, // 原生 radius = 24
      padding: const EdgeInsets.all(18),
      child: Row(
        children: [
          // 【图片适配】以前这里只渲染 `MenuItem.imageUrl` 里的 emoji（数据层
          // 历史上就是拿 emoji 当图）。现在数据层会把菜名解析成真实菜品照片，
          // 所以能拿到图片地址时优先显示照片，拿不到再退回 emoji。
          _CartThumb(imageUrl: item.imageUrl, name: item.name),
          const SizedBox(width: 12), // 原生 spacedBy(12.dp)
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 原生 Text(item.menuItemName, color = CozyCocoa, fontWeight = Black)
                Text(
                  item.name,
                  style: text.bodyLarge!.copyWith(
                    color: CozyPalette.onSurface,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 4), // 原生 spacedBy(4.dp)
                // 原生 Text(yuanText(item.unitPrice), color = CozyRose, SemiBold)
                Text(
                  _yuanText(item.price),
                  style: text.bodyLarge!.copyWith(
                    color: CozyPalette.primary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          _quantityStepper(context, item, quantity),
        ],
      ),
    );
  }

  /// 原生 `CartScreen.kt:181-192` QuantityStepper
  Widget _quantityStepper(BuildContext context, MenuItem item, int quantity) {
    final text = Theme.of(context).textTheme;
    return Row(
      children: [
        _stepperButton(Icons.remove, '减少', () => onRemove(item)),
        // 原生 spacedBy(4.dp) + Text 的 horizontal padding 8
        const SizedBox(width: 12),
        Text(
          '$quantity',
          style: text.bodyLarge!.copyWith(
            color: CozyPalette.onSurface,
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(width: 12),
        _stepperButton(Icons.add, '增加', () => onAdd(item)),
      ],
    );
  }

  /// 原生 `CartScreen.kt:194-207` StepperButton
  /// Surface(onClick, CircleShape, #FFD1DC@0.72) + Icon(padding 8, size 18)
  Widget _stepperButton(IconData icon, String label, VoidCallback onTap) {
    return Semantics(
      button: true,
      label: label,
      child: InkWell(
        onTap: onTap,
        customBorder: const CircleBorder(),
        child: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: CozyPalette.secondaryContainer.withValues(alpha: 0.72),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, size: 18, color: CozyPalette.primary),
        ),
      ),
    );
  }

  /// 原生 `CartSheet.kt:100-118` 结算卡片 / `CartScreen.kt:209-245` CartSummaryBar
  Widget _cartSummaryCard(BuildContext context, bool isEmpty) {
    final text = Theme.of(context).textTheme;
    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(18),
      child: Column(
        children: [
          // 原生 Text("小计", color = CozyMuted) / yuanText(subtotal) cocoa Bold
          Row(
            children: [
              Text(
                '小计',
                style: text.bodyLarge!.copyWith(
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
              const Spacer(),
              Text(
                _yuanText(subtotal),
                style: text.bodyLarge!.copyWith(
                  color: CozyPalette.onSurface,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8), // 原生 spacedBy(8.dp)
          // 原生 Text("合计", color = CozyCocoa, Black) / yuanText(totalPrice) CozyRose Black
          Row(
            children: [
              Text(
                '合计',
                style: text.bodyLarge!.copyWith(
                  color: CozyPalette.onSurface,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const Spacer(),
              Text(
                _yuanText(totalPrice),
                style: text.bodyLarge!.copyWith(
                  color: CozyPalette.primary,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          // 件数 + 糖糖币消耗（原生 CartScreen.kt:137「共 N 份菜品」同义文案）
          Row(
            children: [
              Text(
                '共 $totalCount 份菜品',
                style: text.bodySmall!.copyWith(
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
              const Spacer(),
              Text(
                '消耗 $coinCost 糖糖币',
                style: text.bodySmall!.copyWith(
                  color: CozyPalette.primary,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: 4), // 原生 Spacer(height = 4.dp)
          SizedBox(
            width: double.infinity,
            child: CozyPrimaryButton(
              text: '去结算',
              enabled: !isEmpty,
              onTap: () {
                Navigator.pop(context);
                onCheckout();
              },
            ),
          ),
        ],
      ),
    );
  }
}

/// 结算确认与给后厨的悄悄话弹窗 (CheckoutDialog)。
///
/// 原生对应 `ui/checkout/CheckoutScreen.kt`：
/// - 顶部标题 `CheckoutTopBar`（CheckoutScreen.kt:188-208）
/// - 糖糖币核算卡 `CheckoutCandyCoinsCard`（CheckoutScreen.kt:314-365）
/// - 悄悄话卡（CheckoutScreen.kt:123-152）
/// - 底部提交 `CheckoutBottomAction`（CheckoutScreen.kt:367-426）
///
/// 说明：原生的点单明细卡与地址输入区没有搬进弹窗——Flutter 侧购物清单已经在
/// [CartDetailSheet] 里呈现，而云端 `orders.address_snapshot` 由
/// `lib/data/supabase_api.dart:590` 固定写入（原生 CheckoutViewModel 也只是写
/// 「到店取餐 / 本店」默认地址），弹窗内没有这两份数据来源。
class CheckoutDialog extends StatefulWidget {
  final int coinCost;
  final int candyBalance;
  final ValueChanged<String> onConfirm;

  const CheckoutDialog({
    super.key,
    required this.coinCost,
    required this.candyBalance,
    required this.onConfirm,
  });

  @override
  State<CheckoutDialog> createState() => _CheckoutDialogState();
}

class _CheckoutDialogState extends State<CheckoutDialog> {
  final _noteCtrl = TextEditingController();

  /// 原生 `CheckoutCandyCoinsCard`：`val enough = isCartEmpty || balance >= cost`
  bool get _isEmpty => widget.coinCost <= 0;
  bool get _enough => _isEmpty || widget.candyBalance >= widget.coinCost;

  @override
  void dispose() {
    _noteCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      // 原生结算页底色 `Color(0xFFFFFFFF)`（CheckoutScreen.kt:80）
      backgroundColor: CozyPalette.background,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26)),
      insetPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
      clipBehavior: Clip.antiAlias,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _checkoutTopBar(context),
          Flexible(
            child: SingleChildScrollView(
              // 原生 LazyColumn contentPadding = (20, 14, 20, 24)（CheckoutScreen.kt:94）
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 24),
              child: Column(
                // 原生 CozyCard 自带 fillMaxWidth
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _candyCoinsCard(context),
                  const SizedBox(height: 16), // 原生 spacedBy(16.dp)
                  _buyerNoteCard(context),
                ],
              ),
            ),
          ),
          _bottomAction(context),
        ],
      ),
    );
  }

  /// 原生 `CheckoutScreen.kt:188-208` CheckoutTopBar
  Widget _checkoutTopBar(BuildContext context) {
    return SizedBox(
      height: 64, // 原生 heightIn(min = 64.dp)
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Stack(
          alignment: Alignment.center,
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: IconButton(
                onPressed: () => Navigator.pop(context),
                tooltip: '返回',
                icon: const Icon(
                  Icons.arrow_back,
                  color: CozyPalette.primary,
                ),
              ),
            ),
            Text(
              '确认点菜',
              style: Theme.of(context).textTheme.headlineSmall!.copyWith(
                    color: CozyPalette.primary,
                    fontWeight: FontWeight.w900,
                  ),
            ),
          ],
        ),
      ),
    );
  }

  /// 原生 `CheckoutScreen.kt:314-365` CheckoutCandyCoinsCard
  ///
  /// 额外补出父任务要求的核算明细（当前余额 / 本单消耗 / 付后余额），
  /// 文案沿用原生同名字段语义（balance / cost / 还差 N 糖糖币）。
  Widget _candyCoinsCard(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final bool enough = _enough;

    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(20),
      // 原生 borderColor = if (enough) White@0.72 else error@0.34（CheckoutScreen.kt:323）
      borderColor: enough ? null : CozyPalette.error.withValues(alpha: 0.34),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '糖糖币',
                      style: text.titleMedium!.copyWith(
                        color: CozyPalette.onSurface,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 4), // 原生 spacedBy(4.dp)
                    Text(
                      _isEmpty
                          ? '点菜会消耗糖糖币，它不是金币，只是你们的小额度'
                          : enough
                              ? '本次点菜将消耗 ${widget.coinCost} 糖糖币'
                              : '还差 ${widget.coinCost - widget.candyBalance} 糖糖币，找饲养员撒点糖',
                      style: text.bodySmall!.copyWith(
                        color: enough
                            ? CozyPalette.onSurfaceVariant
                            : CozyPalette.error,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              // 原生 Surface(RoundedCornerShape(999)) + Row(padding h12 v7, spacedBy 5)
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 7,
                ),
                decoration: BoxDecoration(
                  color: enough
                      ? CozyPalette.secondaryContainer.withValues(alpha: 0.68)
                      : CozyPalette.errorContainer,
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // 原生 CandyCoinIcon 用 R.drawable.candy_coin 位图
                    Image.asset(
                      'assets/images/candy_coin.png',
                      width: 22,
                      height: 22,
                      fit: BoxFit.contain,
                      errorBuilder: (_, _, _) =>
                          const Text('🍬', style: TextStyle(fontSize: 22)),
                    ),
                    const SizedBox(width: 5),
                    Text(
                      '${widget.candyBalance} 枚',
                      style: text.bodyLarge!.copyWith(
                        color: enough
                            ? CozyPalette.primary
                            : CozyPalette.error,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            height: 1,
            color: CozyPalette.primary.withValues(alpha: 0.08),
          ),
          const SizedBox(height: 8),
          _coinRow(context, '当前余额', '${widget.candyBalance} 枚'),
          const SizedBox(height: 6),
          _coinRow(context, '本单消耗', '-${widget.coinCost} 枚'),
          const SizedBox(height: 6),
          _coinRow(
            context,
            '付后余额',
            '${widget.candyBalance - widget.coinCost} 枚',
            valueColor: enough ? CozyPalette.primary : CozyPalette.error,
          ),
        ],
      ),
    );
  }

  /// 核算明细一行（原生 CheckoutItemsCard 的 SpaceBetween 明细行样式）
  Widget _coinRow(
    BuildContext context,
    String label,
    String value, {
    Color? valueColor,
  }) {
    final text = Theme.of(context).textTheme;
    return Row(
      children: [
        Text(
          label,
          style: text.bodyLarge!.copyWith(
            color: CozyPalette.onSurfaceVariant,
          ),
        ),
        const Spacer(),
        Text(
          value,
          style: text.bodyLarge!.copyWith(
            color: valueColor ?? CozyPalette.onSurface,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }

  /// 原生 `CheckoutScreen.kt:123-152` 给后厨的悄悄话（buyerNote → orders.buyer_note）
  Widget _buyerNoteCard(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '给后厨的悄悄话',
            style: text.titleMedium!.copyWith(
              color: CozyPalette.onSurface,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 10), // 原生 spacedBy(10.dp)
          Text(
            '口味、忌口、想说的话都可以写在这里',
            style: text.bodySmall!.copyWith(
              color: CozyPalette.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _noteCtrl,
            minLines: 3, // 原生 minLines = 3
            decoration: cozyInputDecoration(
              hintText: '比如少辣、多加葱、今天想吃热一点...',
            ),
          ),
        ],
      ),
    );
  }

  /// 原生 `CheckoutScreen.kt:367-426` CheckoutBottomAction
  ///
  /// 【暗色主题修正】原生这里是浅色底 `#FF9FB7` 实底 + 近白文字，换成暗色
  /// palette 后 `primaryContainer(#3B2A1E)` 当底、`surface(#201A17)` 当字，
  /// 变成深底深字，真机上按钮几乎读不出来（证据 `C8-checkout.png`）。
  /// 暗色下主操作统一用「亮琥珀实底 + 深字」，与底栏选中态、空态按钮一致。
  Widget _bottomAction(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final bool enough = _enough;
    // 原生 enabled = !cart.isEmpty && !isSubmitting（CheckoutScreen.kt:385）
    final bool enabled = !_isEmpty;

    final Color fill = !enabled
        ? CozyPalette.surfaceVariant
        : (enough ? CozyPalette.primary : CozyPalette.secondaryContainer);
    final Color label = !enabled
        ? CozyPalette.onSurfaceVariant
        : (enough ? CozyPalette.surface : CozyPalette.onSurface);

    return Container(
      width: double.infinity,
      // 原生 padding(horizontal = 20, vertical = 16)
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: SizedBox(
        height: 54, // 原生 height(54.dp)
        child: InkWell(
          onTap: enabled ? _submit : null,
          borderRadius: BorderRadius.circular(999),
          child: Container(
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: fill,
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              enough
                  ? '提交点菜 · 消耗 ${widget.coinCost} 糖糖币'
                  : '糖糖币不足 · 需要 ${widget.coinCost} 枚',
              style: text.titleMedium!.copyWith(
                color: label,
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _submit() {
    HapticFeedback.mediumImpact();
    Navigator.pop(context);
    widget.onConfirm(_noteCtrl.text.trim());
  }
}

/// 购物车清单缩略图：有真实图片地址就显示照片，否则退回菜名解析出的照片，
/// 再不行才显示 emoji / 餐具占位。44×44、圆角 16，与原生 `.size(44.dp)` 一致。
class _CartThumb extends StatelessWidget {
  const _CartThumb({required this.imageUrl, required this.name});

  final String imageUrl;
  final String name;

  @override
  Widget build(BuildContext context) {
    final String photo =
        isUsableDishPhoto(imageUrl) ? imageUrl.trim() : resolveDishImage(name, current: imageUrl);
    final Widget fallback = Center(
      child: Text(
        imageUrl.isNotEmpty && imageUrl.length <= 4 ? imageUrl : '🍽️',
        style: const TextStyle(fontSize: 22),
      ),
    );
    return Container(
      width: 44,
      height: 44,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: CozyPalette.secondaryContainer.withValues(alpha: 0.62),
        borderRadius: BorderRadius.circular(16),
      ),
      child: isUsableDishPhoto(photo)
          ? Image.network(
              photo,
              fit: BoxFit.cover,
              errorBuilder: (_, _, _) => fallback,
            )
          : fallback,
    );
  }
}
