import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

class OrderingPage extends StatefulWidget {
  const OrderingPage({super.key});

  @override
  State<OrderingPage> createState() => _OrderingPageState();
}

class _OrderingPageState extends State<OrderingPage> {
  /// 购物车：菜品 id -> 数量
  final Map<String, MenuItem> _cart = {};
  final Map<String, int> _qty = {};

  int get _count => _qty.values.fold(0, (a, b) => a + b);
  double get _total =>
      _qty.entries.fold(0.0, (sum, e) => sum + (_cart[e.key]!.price * e.value));
  int get _coinCost => _total.ceil();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshMenu();
    });
  }

  void _add(MenuItem item) {
    HapticFeedback.lightImpact();
    setState(() {
      _cart[item.id] = item;
      _qty[item.id] = (_qty[item.id] ?? 0) + 1;
    });
  }

  void _remove(MenuItem item) {
    HapticFeedback.selectionClick();
    setState(() {
      final q = (_qty[item.id] ?? 0) - 1;
      if (q <= 0) {
        _qty.remove(item.id);
        _cart.remove(item.id);
      } else {
        _qty[item.id] = q;
      }
    });
  }

  Future<void> _submit() async {
    final state = AppState.instance;
    final dishes = <MenuItem>[];
    _qty.forEach((id, q) {
      for (var i = 0; i < q; i++) {
        dishes.add(_cart[id]!);
      }
    });

    final ok = await state.submitOrder(dishes: dishes);
    if (!mounted) return;
    if (ok) {
      setState(() {
        _cart.clear();
        _qty.clear();
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(state.toast ?? '点单成功'), backgroundColor: CozyTheme.sweetCocoa),
      );
    } else if (state.error != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(state.error!), backgroundColor: const Color(0xFFD64545)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return ListenableBuilder(
      listenable: state,
      builder: (context, _) {
        final items = state.menu;
        final isEater = !state.isCaretaker;

        return Stack(
          clipBehavior: Clip.none,
          children: [
            RefreshIndicator(
              color: CozyTheme.primaryPink,
              onRefresh: () => state.refreshMenu(),
              child: CustomScrollView(
                slivers: [
                  // 小店头部
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  state.shop?.name ?? '我们的小店',
                                  style: const TextStyle(
                                      fontSize: 21, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  state.shop?.announcement ?? '今天也要一起好好吃饭',
                                  style: const TextStyle(fontSize: 12.5, color: CozyTheme.mutedText),
                                ),
                              ],
                            ),
                          ),
                          // 糖币胶囊
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 7),
                            decoration: BoxDecoration(
                              color: CozyTheme.softPink,
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Row(
                              children: [
                                const Text('🍬 ', style: TextStyle(fontSize: 12)),
                                Text(
                                  '${state.candyCoins}',
                                  style: const TextStyle(
                                      fontSize: 13, fontWeight: FontWeight.w900, color: CozyTheme.primaryPink),
                                ),
                              ],
                            ),
                          ).animate().scale(duration: 260.ms, curve: Curves.easeOutBack),
                        ],
                      ),
                    ),
                  ),

                  // 角色提示
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(20, 0, 20, 14),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: isEater ? CozyTheme.softPink : const Color(0xFFF5F7FF),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Row(
                          children: [
                            Text(isEater ? '🍽️' : '🍳', style: const TextStyle(fontSize: 15)),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                isEater
                                    ? '我是吃货，选好菜点单，饲养员就会收到通知'
                                    : '我是饲养员，负责做饭与推进订单状态',
                                style: TextStyle(
                                  fontSize: 11.5,
                                  fontWeight: FontWeight.w700,
                                  color: isEater ? CozyTheme.primaryPink : const Color(0xFF5B6BD6),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),

                  if (items.isEmpty)
                    const SliverFillRemaining(
                      hasScrollBody: false,
                      child: Center(
                        child: Text('菜单还是空的，请饲养员先上新菜品',
                            style: TextStyle(color: CozyTheme.mutedText, fontSize: 13)),
                      ),
                    )
                  else
                    SliverPadding(
                      padding: EdgeInsets.fromLTRB(20, 0, 20, _count > 0 ? 190 : 120),
                      sliver: SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (context, index) {
                            final item = items[index];
                            final q = _qty[item.id] ?? 0;
                            return _dishCard(item, q, isEater, index);
                          },
                          childCount: items.length,
                        ),
                      ),
                    ),
                ],
              ),
            ),

            // iOS 26 风格真实折射玻璃购物车条（吃货选菜后浮现）
            if (_count > 0 && isEater)
              Positioned(
                left: 20,
                right: 20,
                bottom: 112,
                child: GestureDetector(
                  // GlassCard 本身不含 onTap，外包一层手势并设为 opaque 保证整块可点
                  onTap: state.busy ? null : _submit,
                  behavior: HitTestBehavior.opaque,
                  child: GlassCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    child: Row(
                    children: [
                      Container(
                        width: 36,
                        height: 36,
                        decoration: const BoxDecoration(
                            color: CozyTheme.sweetCocoa, shape: BoxShape.circle),
                        child: Center(
                          child: Text('$_count',
                              style: const TextStyle(
                                  color: Colors.white, fontSize: 13, fontWeight: FontWeight.w900)),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('合计 ${_total.toStringAsFixed(0)} 元',
                                style: const TextStyle(
                                    fontSize: 14, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                            Text('消耗 $_coinCost 糖币 · 剩余 ${state.candyCoins}',
                                style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText)),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 9),
                        decoration: BoxDecoration(
                          color: CozyTheme.primaryPink,
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: state.busy
                            ? const SizedBox(
                                width: 15,
                                height: 15,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                            : const Text('去点单',
                                style: TextStyle(
                                    fontSize: 13, fontWeight: FontWeight.w900, color: Colors.white)),
                      ),
                    ],
                  ),
                ),
                ),
              ).animate().fadeIn(duration: 220.ms).slideY(begin: 0.25, end: 0),
          ],
        );
      },
    );
  }

  Widget _dishCard(MenuItem item, int qty, bool isEater, int index) {
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: CozyTheme.cardStroke),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 16, offset: const Offset(0, 4)),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 74,
            height: 74,
            decoration: BoxDecoration(
              color: const Color(0xFFFFF6F0),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Center(
              child: Text(
                item.imageUrl.isNotEmpty && item.imageUrl.length <= 4 ? item.imageUrl : '🍽️',
                style: const TextStyle(fontSize: 34),
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.name,
                    style: const TextStyle(
                        fontSize: 15.5, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                if (item.description.isNotEmpty) ...[
                  const SizedBox(height: 5),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: CozyTheme.softPink,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(item.description,
                        style: const TextStyle(
                            fontSize: 10.5, fontWeight: FontWeight.bold, color: CozyTheme.primaryPink)),
                  ),
                ],
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('${item.price.toStringAsFixed(0)} 🍬 糖币',
                        style: const TextStyle(
                            fontSize: 15.5, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                    if (isEater) _stepper(item, qty),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    ).animate().fadeIn(delay: (index * 45).ms, duration: 380.ms).slideY(begin: 0.08, end: 0);
  }

  Widget _stepper(MenuItem item, int qty) {
    if (qty == 0) {
      return InkWell(
        onTap: () => _add(item),
        borderRadius: BorderRadius.circular(17),
        child: Container(
          width: 34,
          height: 34,
          decoration: BoxDecoration(
            color: CozyTheme.sweetCocoa,
            borderRadius: BorderRadius.circular(17),
            boxShadow: [
              BoxShadow(color: Colors.black.withValues(alpha: 0.15), blurRadius: 8, offset: const Offset(0, 2)),
            ],
          ),
          child: const Icon(Icons.add, color: Colors.white, size: 18),
        ),
      );
    }

    return Row(
      children: [
        GestureDetector(
          onTap: () => _remove(item),
          child: Container(
            width: 30,
            height: 30,
            decoration: BoxDecoration(
              color: CozyTheme.softPink,
              borderRadius: BorderRadius.circular(15),
            ),
            child: const Icon(Icons.remove, size: 16, color: CozyTheme.primaryPink),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          child: Text('$qty',
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
        ),
        GestureDetector(
          onTap: () => _add(item),
          child: Container(
            width: 30,
            height: 30,
            decoration: BoxDecoration(
              color: CozyTheme.sweetCocoa,
              borderRadius: BorderRadius.circular(15),
            ),
            child: const Icon(Icons.add, size: 16, color: Colors.white),
          ),
        ),
      ],
    );
  }
}
