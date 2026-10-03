import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/food_images.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

/// 购物车详情与结算确认弹层。
///
/// 视觉与骨架 1:1 对齐 `demo/index.html` 中的 `#subpage-checkout`（确认点菜 / 结算页）：
/// 1. 就餐地点卡片（Delivery Address / Table）
/// 2. 待做美味清单卡片（Ordered Dishes Breakdown List）
/// 3. 给大厨的爱心小纸条卡片（Remarks & Notes for the Chef）
/// 4. 糖糖币结算明细卡片（Candy Coin Settlement Summary）
/// 5. 底部固定结算按钮（呼叫饲养员做饭）
class CartDetailSheet extends StatefulWidget {
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

  @override
  State<CartDetailSheet> createState() => _CartDetailSheetState();
}

class _CartDetailSheetState extends State<CartDetailSheet> {
  final TextEditingController _remarksCtrl = TextEditingController();

  /// 对应原生 `CartState.itemCount`
  int get totalCount => widget.qty.values.fold(0, (a, b) => a + b);

  /// 对应原生 `CartState.totalPrice`
  double get totalPrice => widget.qty.entries.fold(
        0.0,
        (sum, e) => sum + ((widget.cart[e.key]?.price ?? 0.0) * e.value),
      );

  /// 对应原生 `candyCoinsCost(totalPrice) = ceil(totalPrice)`
  int get coinCost => totalPrice.ceil();

  @override
  void dispose() {
    _remarksCtrl.dispose();
    super.dispose();
  }

  void _appendRemark(String phrase) {
    HapticFeedback.selectionClick();
    final current = _remarksCtrl.text.trim();
    if (current.isEmpty) {
      _remarksCtrl.text = phrase;
    } else {
      _remarksCtrl.text = '$current， $phrase';
    }
    _remarksCtrl.selection = TextSelection.fromPosition(
      TextPosition(offset: _remarksCtrl.text.length),
    );
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final entries = <MapEntry<String, int>>[
      for (final e in widget.qty.entries)
        if (widget.cart.containsKey(e.key) && e.value > 0) e,
    ];
    final bool isEmpty = entries.isEmpty;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        decoration: BoxDecoration(
          color: CozyPalette.surface,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
          boxShadow: <BoxShadow>[
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.12),
              blurRadius: 24,
              offset: const Offset(0, -4),
            ),
          ],
        ),
        padding: EdgeInsets.fromLTRB(
          16,
          10,
          16,
          MediaQuery.of(context).padding.bottom + 16,
        ),
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxHeight: MediaQuery.of(context).size.height * 0.88,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 拖拽把手（32 × 4）
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  margin: const EdgeInsets.only(bottom: 12),
                  decoration: BoxDecoration(
                    color: CozyPalette.onSurfaceVariant.withValues(alpha: 0.35),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              Flexible(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // 1. 就餐地点卡片
                      _buildDeliveryAddressCard(context),
                      const SizedBox(height: 10),
                      // 2. 待做美味清单卡片
                      _buildDishesBreakdownCard(context, entries, isEmpty),
                      const SizedBox(height: 10),
                      // 3. 给大厨的爱心小纸条卡片
                      _buildRemarksCard(context),
                      const SizedBox(height: 10),
                      // 4. 糖糖币结算明细卡片
                      _buildSettlementSummaryCard(context, isEmpty),
                      const SizedBox(height: 10),
                    ],
                  ),
                ),
              ),
              // 5. 底部固定结算按钮
              _buildBottomCheckoutButton(context, isEmpty),
            ],
          ),
        ),
      ),
    );
  }

  /// 通用圆角卡片包装
  Widget _buildCard({
    required Widget child,
    EdgeInsetsGeometry padding = const EdgeInsets.all(14),
    Color? color,
    Border? border,
    Gradient? gradient,
  }) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: gradient == null ? (color ?? Colors.white) : null,
        gradient: gradient,
        borderRadius: BorderRadius.circular(24),
        border: border ??
            Border.all(
              color: CozyPalette.outlineVariant.withValues(alpha: 0.35),
            ),
        boxShadow: const [
          BoxShadow(
            color: Color(0x081D1B18),
            blurRadius: 2,
            offset: Offset(0, 1),
          ),
          BoxShadow(
            color: Color(0x0D1D1B18),
            blurRadius: 14,
            spreadRadius: -3,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: child,
    );
  }

  /// 1. 就餐地点卡片（Delivery Address / Table）
  Widget _buildDeliveryAddressCard(BuildContext context) {
    return _buildCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: CozyPalette.secondaryContainer.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: const Text(
                  '就餐地点',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    color: CozyPalette.primary,
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFFECFDF5),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: const Color(0xFFA7F3D0)),
                ),
                child: const Text(
                  '免配送费',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF059669),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 36,
                height: 36,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF1F2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Text(
                  '🛋️',
                  style: TextStyle(fontSize: 18),
                ),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '甜蜜小窝 · 客厅沙发茶几',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w900,
                        color: CozyPalette.onSurface,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      '自营小饭桌 · 饲养员亲手做好端上桌',
                      style: TextStyle(
                        fontSize: 11,
                        color: CozyPalette.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 2. 待做美味清单卡片（Ordered Dishes Breakdown List）
  Widget _buildDishesBreakdownCard(
    BuildContext context,
    List<MapEntry<String, int>> entries,
    bool isEmpty,
  ) {
    return _buildCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                '🍽️ 待做美味清单',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w900,
                  color: CozyPalette.onSurface,
                ),
              ),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: CozyPalette.secondaryContainer.withValues(alpha: 0.5),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Text(
                      '共 $totalCount 道菜',
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: CozyPalette.primary,
                      ),
                    ),
                  ),
                  if (!isEmpty) ...[
                    const SizedBox(width: 8),
                    InkWell(
                      onTap: () {
                        HapticFeedback.lightImpact();
                        widget.onClear();
                      },
                      borderRadius: BorderRadius.circular(999),
                      child: const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 6, vertical: 3),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.delete_outline, size: 14, color: CozyPalette.primary),
                            SizedBox(width: 2),
                            Text(
                              '清空',
                              style: TextStyle(
                                fontSize: 11,
                                color: CozyPalette.primary,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ],
          ),
          const SizedBox(height: 10),
          Divider(
            height: 1,
            thickness: 0.8,
            color: CozyPalette.outlineVariant.withValues(alpha: 0.3),
          ),
          const SizedBox(height: 8),
          if (isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 20),
              child: Center(
                child: Text(
                  '购物篮空空如也，去挑几道爱吃的美味吧~',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    color: CozyPalette.onSurfaceVariant,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            )
          else
            ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 240),
              child: ListView.separated(
                shrinkWrap: true,
                padding: EdgeInsets.zero,
                physics: const ClampingScrollPhysics(),
                itemCount: entries.length,
                separatorBuilder: (_, _) => Divider(
                  height: 16,
                  thickness: 0.6,
                  color: CozyPalette.outlineVariant.withValues(alpha: 0.25),
                ),
                itemBuilder: (context, index) {
                  final id = entries[index].key;
                  final item = widget.cart[id]!;
                  final quantity = entries[index].value;
                  final int itemCoinCost = (item.price.ceil()) * quantity;
                  return Row(
                    children: [
                      _CartThumb(imageUrl: item.imageUrl, name: item.name),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              item.name,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.bold,
                                color: CozyPalette.onSurface,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '单价 🍬${item.price.ceil()} × $quantity',
                              style: const TextStyle(
                                fontSize: 11,
                                color: CozyPalette.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                      _quantityStepper(context, item, quantity),
                      const SizedBox(width: 10),
                      Text(
                        '🍬 $itemCoinCost',
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: CozyPalette.primary,
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
        ],
      ),
    );
  }

  /// 份数步进器
  Widget _quantityStepper(BuildContext context, MenuItem item, int quantity) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _stepperButton(Icons.remove, '减少', () => widget.onRemove(item)),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          child: Text(
            '$quantity',
            style: const TextStyle(
              fontSize: 13,
              color: CozyPalette.onSurface,
              fontWeight: FontWeight.w900,
            ),
          ),
        ),
        _stepperButton(Icons.add, '增加', () => widget.onAdd(item)),
      ],
    );
  }

  Widget _stepperButton(IconData icon, String label, VoidCallback onTap) {
    return Semantics(
      button: true,
      label: label,
      child: InkWell(
        onTap: onTap,
        customBorder: const CircleBorder(),
        child: Container(
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(
            color: CozyPalette.secondaryContainer.withValues(alpha: 0.65),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, size: 16, color: CozyPalette.primary),
        ),
      ),
    );
  }

  /// 3. 给大厨的爱心小纸条卡片（Remarks & Notes for the Chef）
  Widget _buildRemarksCard(BuildContext context) {
    return _buildCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '📝 给大厨的爱心小纸条',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w900,
              color: CozyPalette.onSurface,
            ),
          ),
          const SizedBox(height: 8),
          Container(
            decoration: BoxDecoration(
              color: const Color(0xFFFBF8F5),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: CozyPalette.outlineVariant.withValues(alpha: 0.4),
              ),
            ),
            child: TextField(
              controller: _remarksCtrl,
              maxLines: 2,
              minLines: 2,
              style: const TextStyle(
                fontSize: 12,
                color: CozyPalette.onSurface,
              ),
              decoration: const InputDecoration(
                hintText: '例：番茄牛腩多汁一点、不要香菜、饭后加一份抱抱~',
                hintStyle: TextStyle(
                  fontSize: 11,
                  color: Color(0xFF9CA3AF),
                ),
                contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                border: InputBorder.none,
              ),
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              _buildRemarkChip('少放辣椒 🌶️'),
              _buildRemarkChip('微甜浓郁 🍯'),
              _buildRemarkChip('辛苦大厨啦 ❤️'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildRemarkChip(String label) {
    return InkWell(
      onTap: () => _appendRemark(label),
      borderRadius: BorderRadius.circular(999),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xFFFBF8F5),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: CozyPalette.outlineVariant.withValues(alpha: 0.4),
          ),
        ),
        child: Text(
          label,
          style: const TextStyle(
            fontSize: 11,
            color: CozyPalette.onSurfaceVariant,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }

  /// 4. 糖糖币结算明细卡片（Candy Coin Settlement Summary）
  Widget _buildSettlementSummaryCard(BuildContext context, bool isEmpty) {
    final bool enough = isEmpty || widget.candyBalance >= coinCost;

    return _buildCard(
      padding: const EdgeInsets.all(14),
      gradient: LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          Colors.white,
          const Color(0xFFFEF3C7).withValues(alpha: 0.35),
        ],
      ),
      border: Border.all(
        color: const Color(0xFFFDE68A).withValues(alpha: 0.8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                '菜品总额',
                style: TextStyle(
                  fontSize: 12,
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
              Text(
                '🍬 $coinCost 币',
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: CozyPalette.onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          const Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '家庭自营端餐费',
                style: TextStyle(
                  fontSize: 12,
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
              Text(
                '免费 (爱的奉献)',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF059669),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Divider(
            height: 1,
            thickness: 0.8,
            color: const Color(0xFFFDE68A).withValues(alpha: 0.6),
          ),
          const SizedBox(height: 8),
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '吃货钱包可用额度',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: CozyPalette.onSurface,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      enough
                          ? '当前余额: 🍬 ${widget.candyBalance} (余额充足)'
                          : '当前余额: 🍬 ${widget.candyBalance} (不足，还差 ${coinCost - widget.candyBalance} 币)',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: enough ? FontWeight.w500 : FontWeight.bold,
                        color: enough ? const Color(0xFFB45309) : CozyPalette.error,
                      ),
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const Text(
                    '应扣除:',
                    style: TextStyle(
                      fontSize: 10,
                      color: CozyPalette.onSurfaceVariant,
                    ),
                  ),
                  Text(
                    '🍬 $coinCost 币',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w900,
                      color: Color(0xFFE11D48),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 5. 底部固定结算按钮
  Widget _buildBottomCheckoutButton(BuildContext context, bool isEmpty) {
    final bool enabled = !isEmpty;

    return Padding(
      padding: const EdgeInsets.only(top: 6),
      child: SizedBox(
        width: double.infinity,
        height: 48,
        child: InkWell(
          onTap: enabled
              ? () {
                  HapticFeedback.mediumImpact();
                  Navigator.pop(context);
                  widget.onCheckout();
                }
              : null,
          borderRadius: BorderRadius.circular(999),
          child: Container(
            alignment: Alignment.center,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(999),
              gradient: enabled
                  ? const LinearGradient(
                      colors: [
                        Color(0xFF894C5C),
                        Color(0xFFF43F5E),
                        Color(0xFFF59E0B),
                      ],
                    )
                  : null,
              color: enabled ? null : const Color(0xFFE5E7EB),
              boxShadow: enabled
                  ? [
                      BoxShadow(
                        color: const Color(0xFFF43F5E).withValues(alpha: 0.3),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      ),
                    ]
                  : null,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  '确认点菜 · 呼叫饲养员做饭 🍳',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w900,
                    color: enabled ? Colors.white : const Color(0xFF9CA3AF),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// 购物车清单缩略图
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
        style: const TextStyle(fontSize: 20),
      ),
    );
    return Container(
      width: 40,
      height: 40,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: CozyPalette.secondaryContainer.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: CozyPalette.outlineVariant.withValues(alpha: 0.35),
        ),
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

/// 结算确认与给后厨的悄悄话弹窗 (CheckoutDialog)。
///
/// 保留兼容原有调用。
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
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _candyCoinsCard(context),
                  const SizedBox(height: 16),
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

  Widget _checkoutTopBar(BuildContext context) {
    return SizedBox(
      height: 64,
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

  Widget _candyCoinsCard(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final bool enough = _enough;

    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(20),
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
                    const SizedBox(height: 4),
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
          const SizedBox(height: 10),
          Text(
            '口味、忌口、想说的话都可以写在这里',
            style: text.bodySmall!.copyWith(
              color: CozyPalette.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _noteCtrl,
            minLines: 3,
            decoration: cozyInputDecoration(
              hintText: '比如少辣、多加葱、今天想吃热一点...',
            ),
          ),
        ],
      ),
    );
  }

  Widget _bottomAction(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final bool enough = _enough;
    final bool enabled = !_isEmpty;

    final Color fill = !enabled
        ? CozyPalette.surfaceVariant
        : (enough ? CozyPalette.primary : CozyPalette.secondaryContainer);
    final Color label = !enabled
        ? CozyPalette.onSurfaceVariant
        : (enough ? CozyPalette.surface : CozyPalette.onSurface);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: SizedBox(
        height: 54,
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
