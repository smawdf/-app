import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key});

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends State<OrdersPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshOrders();
    });
  }

  Future<void> _advance(Order order) async {
    HapticFeedback.mediumImpact();
    final state = AppState.instance;
    final ok = await state.advanceOrder(order);
    if (!mounted) return;
    if (!ok && state.error != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(state.error!)));
    }
  }

  Future<void> _cancel(Order order) async {
    final state = AppState.instance;
    final ok = await state.cancelOrder(order);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? '订单已取消，糖币已退还' : (state.error ?? '取消失败'))),
    );
  }

  Color _statusColor(String status) => switch (status) {
        'submitted' => const Color(0xFFE8A33D),
        'confirmed' => const Color(0xFF5B8DEF),
        'preparing' => const Color(0xFFE86A3D),
        'delivering' => const Color(0xFF9B5BD6),
        'completed' => const Color(0xFF3DA96B),
        _ => CozyTheme.mutedText,
      };

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return ListenableBuilder(
      listenable: state,
      builder: (context, _) {
        final orders = state.orders;

        if (orders.isEmpty) {
          return const Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('🍽️', style: TextStyle(fontSize: 48)),
                SizedBox(height: 14),
                Text('还没有订单', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                SizedBox(height: 6),
                Text('吃货去点菜页选几道菜试试吧', style: TextStyle(fontSize: 12.5, color: CozyTheme.mutedText)),
              ],
            ),
          );
        }

        return RefreshIndicator(
          color: CozyTheme.primaryPink,
          onRefresh: () => state.refreshOrders(),
          child: ListView.builder(
            padding: const EdgeInsets.fromLTRB(20, 14, 20, 120),
            itemCount: orders.length,
            itemBuilder: (context, index) {
              final order = orders[index];
              final color = _statusColor(order.status);
              return Container(
                margin: const EdgeInsets.only(bottom: 14),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: CozyTheme.cardStroke),
                  boxShadow: [
                    BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 16, offset: const Offset(0, 4)),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 头部：下单人 + 状态
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            '${order.buyerName} 点的菜',
                            style: const TextStyle(
                                fontSize: 15, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                          decoration: BoxDecoration(
                            color: color.withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            order.statusLabel,
                            style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: color),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),

                    // 菜品明细
                    ...order.items.map((it) => Padding(
                          padding: const EdgeInsets.only(bottom: 6),
                          child: Row(
                            children: [
                              Text(
                                it.imageUrl.isNotEmpty && it.imageUrl.length <= 4 ? it.imageUrl : '🍽️',
                                style: const TextStyle(fontSize: 15),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  it.name,
                                  style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: CozyTheme.sweetCocoa),
                                ),
                              ),
                              Text('x${it.quantity}',
                                  style: const TextStyle(fontSize: 12.5, color: CozyTheme.mutedText)),
                            ],
                          ),
                        )),

                    if (order.buyerNote.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF7F7F8),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Row(
                          children: [
                            const Text('📝', style: TextStyle(fontSize: 12)),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(order.buyerNote,
                                  style: const TextStyle(fontSize: 11.5, color: CozyTheme.mutedText)),
                            ),
                          ],
                        ),
                      ),
                    ],

                    const SizedBox(height: 12),
                    const Divider(height: 1, color: CozyTheme.cardStroke),
                    const SizedBox(height: 12),

                    // 底部：金额 + 操作
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            '合计 ${order.totalPrice.toStringAsFixed(0)} 元 · ${order.candyCoinsSpent} 糖币',
                            style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa),
                          ),
                        ),
                        if (order.isActive && !state.isCaretaker)
                          TextButton(
                            onPressed: () => _cancel(order),
                            child: const Text('取消订单',
                                style: TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
                          ),
                        if (order.isActive && state.isCaretaker && order.nextStatus != null)
                          GestureDetector(
                            onTap: () => _advance(order),
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
                              decoration: BoxDecoration(
                                color: CozyTheme.primaryPink,
                                borderRadius: BorderRadius.circular(18),
                              ),
                              child: Text(
                                order.nextStatusLabel ?? '推进',
                                style: const TextStyle(
                                    fontSize: 12.5, fontWeight: FontWeight.w900, color: Colors.white),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
              ).animate().fadeIn(delay: (index * 50).ms, duration: 380.ms).slideY(begin: 0.08, end: 0);
            },
          ),
        );
      },
    );
  }
}
