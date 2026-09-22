import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/models.dart';
import '../theme/cozy_glass.dart';

/// 苹果风格购物车详情半屏弹层 (CartModalSheet)
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

  int get totalCount => qty.values.fold(0, (a, b) => a + b);
  double get totalPrice =>
      qty.entries.fold(0.0, (sum, e) => sum + (cart[e.key]!.price * e.value));
  int get coinCost => totalPrice.ceil();

  @override
  Widget build(BuildContext context) {
    final items = qty.entries.toList();

    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      padding: EdgeInsets.fromLTRB(20, 12, 20, MediaQuery.of(context).padding.bottom + 20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 顶部小横条把手
          Center(
            child: Container(
              width: 36,
              height: 4.5,
              decoration: BoxDecoration(
                color: const Color(0xFFE5E5EA),
                borderRadius: BorderRadius.circular(3),
              ),
            ),
          ),
          const SizedBox(height: 14),

          // 标题与清空
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Text(
                    "购物篮清单",
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: CozyTheme.softPink,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      "$totalCount 份",
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: CozyTheme.primaryPink),
                    ),
                  ),
                ],
              ),
              GestureDetector(
                onTap: () {
                  HapticFeedback.lightImpact();
                  onClear();
                  Navigator.pop(context);
                },
                child: const Row(
                  children: [
                    Icon(Icons.delete_outline, size: 16, color: CozyTheme.mutedText),
                    SizedBox(width: 4),
                    Text("清空", style: TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // 菜品清单列表
          ConstrainedBox(
            constraints: const BoxConstraints(maxHeight: 280),
            child: ListView.separated(
              shrinkWrap: true,
              itemCount: items.length,
              separatorBuilder: (context, index) => const Divider(height: 16, color: Color(0xFFF6F6F7)),
              itemBuilder: (ctx, idx) {
                final entry = items[idx];
                final item = cart[entry.key]!;
                final q = entry.value;

                return Row(
                  children: [
                    Container(
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        color: const Color(0xFFFAFAFA),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Center(
                        child: Text(item.imageUrl.isNotEmpty ? item.imageUrl : "🍽️",
                            style: const TextStyle(fontSize: 22)),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(item.name,
                              style: const TextStyle(
                                  fontSize: 14.5, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                          Text("${item.price.toStringAsFixed(0)} 🍬",
                              style: const TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
                        ],
                      ),
                    ),
                    // 步进器
                    Row(
                      children: [
                        GestureDetector(
                          onTap: () => onRemove(item),
                          child: Container(
                            width: 26,
                            height: 26,
                            decoration: BoxDecoration(
                              color: const Color(0xFFF2F2F7),
                              borderRadius: BorderRadius.circular(13),
                            ),
                            child: const Icon(Icons.remove, size: 14, color: CozyTheme.sweetCocoa),
                          ),
                        ),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 10),
                          child: Text('$q',
                              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                        ),
                        GestureDetector(
                          onTap: () => onAdd(item),
                          child: Container(
                            width: 26,
                            height: 26,
                            decoration: BoxDecoration(
                              color: CozyTheme.sweetCocoa,
                              borderRadius: BorderRadius.circular(13),
                            ),
                            child: const Icon(Icons.add, size: 14, color: Colors.white),
                          ),
                        ),
                      ],
                    ),
                  ],
                );
              },
            ),
          ),
          const SizedBox(height: 20),

          // 糖币消耗提示卡片
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: candyBalance >= coinCost ? const Color(0xFFFAFAFA) : const Color(0xFFFFF0F0),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: candyBalance >= coinCost ? const Color(0x0A000000) : const Color(0xFFFFD1D1),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Text("🍬 账户糖糖币: ", style: TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
                    Text("$candyBalance",
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w900,
                          color: candyBalance >= coinCost ? CozyTheme.sweetCocoa : const Color(0xFFDC2626),
                        )),
                  ],
                ),
                Text(
                  candyBalance >= coinCost ? "余额充足 ✨" : "糖币不足，请饲养员撒糖 ⚠️",
                  style: TextStyle(
                    fontSize: 11.5,
                    fontWeight: FontWeight.w700,
                    color: candyBalance >= coinCost ? const Color(0xFF16A34A) : const Color(0xFFDC2626),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 底部结算条
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("合计 ¥${totalPrice.toStringAsFixed(0)}",
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                    Text("消耗 $coinCost 糖糖币",
                        style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText)),
                  ],
                ),
              ),
              ElevatedButton(
                onPressed: () {
                  Navigator.pop(context);
                  onCheckout();
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: CozyTheme.primaryPink,
                  elevation: 0,
                  padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 13),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
                ),
                child: const Text("去结算",
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: Colors.white)),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// 结算确认与给后厨的悄悄话弹窗 (CheckoutDialog)
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

  @override
  void dispose() {
    _noteCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final canAfford = widget.candyBalance >= widget.coinCost;

    return Dialog(
      backgroundColor: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26)),
      insetPadding: const EdgeInsets.symmetric(horizontal: 24),
      child: Padding(
        padding: const EdgeInsets.all(22),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Text("确认点单", style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                const Spacer(),
                GestureDetector(
                  onTap: () => Navigator.pop(context),
                  child: const Icon(Icons.close, size: 20, color: CozyTheme.mutedText),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // 糖币消耗摘要
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFFAFAFA),
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: const Color(0x0A000000)),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text("本次消耗糖币", style: TextStyle(fontSize: 13, color: CozyTheme.mutedText)),
                  Text("-${widget.coinCost} 🍬",
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: CozyTheme.primaryPink)),
                ],
              ),
            ),
            const SizedBox(height: 14),

            // 给后厨的悄悄话输入框 (对应原版 buyerNote)
            const Text("给后厨伴侣的悄悄话 💬",
                style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
            const SizedBox(height: 6),
            Container(
              decoration: BoxDecoration(
                color: const Color(0xFFF8F8FA),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: const Color(0x0A000000)),
              ),
              child: TextField(
                controller: _noteCtrl,
                maxLines: 3,
                style: const TextStyle(fontSize: 13),
                decoration: const InputDecoration(
                  hintText: "比如少辣、多加葱、今天想吃热一点...",
                  hintStyle: TextStyle(fontSize: 12, color: CozyTheme.mutedText),
                  border: InputBorder.none,
                  contentPadding: EdgeInsets.all(12),
                ),
              ),
            ),
            const SizedBox(height: 20),

            // 提交按钮
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: canAfford
                    ? () {
                        HapticFeedback.mediumImpact();
                        Navigator.pop(context);
                        widget.onConfirm(_noteCtrl.text.trim());
                      }
                    : null,
                style: ElevatedButton.styleFrom(
                  backgroundColor: CozyTheme.primaryPink,
                  disabledBackgroundColor: const Color(0xFFE5E5EA),
                  elevation: 0,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
                ),
                child: Text(
                  canAfford ? "心动提交给伴侣 💕" : "糖币不足无法下单",
                  style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.w900, color: Colors.white),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
