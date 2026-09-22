import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

/// 苹果风格订单详情页 (OrderDetailPage)
class OrderDetailPage extends StatefulWidget {
  final Order order;

  const OrderDetailPage({super.key, required this.order});

  @override
  State<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends State<OrderDetailPage> {
  late Order _currentOrder;

  @override
  void initState() {
    super.initState();
    _currentOrder = widget.order;
  }

  Future<void> _advanceOrder() async {
    HapticFeedback.mediumImpact();
    final state = AppState.instance;
    final next = _currentOrder.nextStatus;
    if (next == null) return;

    final ok = await state.advanceOrder(_currentOrder);
    if (!mounted) return;
    if (ok) {
      setState(() {
        _currentOrder = _currentOrder.copyWithStatus(next);
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('已推进做饭状态为：${_currentOrder.statusLabel}')),
      );
    }
  }

  Future<void> _cancelOrder() async {
    HapticFeedback.mediumImpact();
    final state = AppState.instance;
    final ok = await state.cancelOrder(_currentOrder);
    if (!mounted) return;
    if (ok) {
      setState(() {
        _currentOrder = _currentOrder.copyWithStatus('cancelled');
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('订单已取消，糖币已退还！')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;
    final isCaretaker = state.isCaretaker;
    final nextAction = _currentOrder.nextStatusAction;
    final canAdvance = isCaretaker && nextAction != null;
    final canCancel = !isCaretaker && (_currentOrder.status == 'submitted' || _currentOrder.status == 'confirmed');

    return Scaffold(
      backgroundColor: CozyTheme.pureWhite,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        title: const Text(
          "订单详情",
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 18, color: CozyTheme.sweetCocoa),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 10, 20, 40),
        children: [
          // 1. 状态大卡片
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: const Color(0x0A000000)),
              boxShadow: const [
                BoxShadow(color: Color(0x04000000), blurRadius: 14, offset: Offset(0, 4)),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      _currentOrder.statusLabel,
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
                    ),
                    Text(
                      "总计 ${_currentOrder.totalPrice.toStringAsFixed(0)} 🍬",
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: CozyTheme.primaryPink),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  "下单时间: ${_currentOrder.createdAt.toLocal().toString().substring(0, 16)}",
                  style: const TextStyle(fontSize: 12, color: CozyTheme.mutedText),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 2. 做饭全节点时间轴
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: const Color(0x0A000000)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text("做饭时间轴 ⏱️",
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                const SizedBox(height: 16),
                _timelineStep(label: "吃货心动点单", desc: "清单已飞到饲养员手心", isDone: true, isCurrent: _currentOrder.status == 'submitted'),
                _timelineStep(label: "饲养员接单确认", desc: "开始采购食材与清点调料", isDone: _isStepDone(1), isCurrent: _currentOrder.status == 'confirmed'),
                _timelineStep(label: "厨房热气腾腾烹饪中", desc: "大火收汁或慢煲热汤", isDone: _isStepDone(2), isCurrent: _currentOrder.status == 'preparing'),
                _timelineStep(label: "摆盘端上餐桌", desc: "开饭铃响，准备动筷", isDone: _isStepDone(3), isCurrent: _currentOrder.status == 'delivering'),
                _timelineStep(label: "开饭全部吃光", desc: "美味下肚，今日甜蜜满分", isDone: _isStepDone(4), isCurrent: _currentOrder.status == 'completed', isLast: true),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 3. 菜品明细清单
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: const Color(0x0A000000)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text("菜品清单",
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                const SizedBox(height: 12),
                ..._currentOrder.items.map((item) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(item.name,
                            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: Color(0xFF222222))),
                        Text("x${item.quantity}   ${item.subtotal.toStringAsFixed(0)} 🍬",
                            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                      ],
                    ),
                  );
                }),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // 4. 底部操作按钮
          if (canAdvance)
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton(
                onPressed: _advanceOrder,
                style: ElevatedButton.styleFrom(
                  backgroundColor: CozyTheme.sweetCocoa,
                  elevation: 0,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
                ),
                child: Text(
                  nextAction,
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: Colors.white),
                ),
              ),
            ),
          if (canCancel) ...[
            const SizedBox(height: 10),
            Center(
              child: TextButton(
                onPressed: _cancelOrder,
                child: const Text("取消订单并退还糖币",
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: Color(0xFFDC2626))),
              ),
            ),
          ],
        ],
      ),
    );
  }

  bool _isStepDone(int stepIndex) {
    const orderSteps = ['submitted', 'confirmed', 'preparing', 'delivering', 'completed'];
    final currentIdx = orderSteps.indexOf(_currentOrder.status);
    return currentIdx >= stepIndex;
  }

  Widget _timelineStep({
    required String label,
    required String desc,
    required bool isDone,
    required bool isCurrent,
    bool isLast = false,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Column(
          children: [
            Container(
              width: 18,
              height: 18,
              decoration: BoxDecoration(
                color: isDone ? CozyTheme.primaryPink : const Color(0xFFE5E5EA),
                shape: BoxShape.circle,
                border: isCurrent ? Border.all(color: CozyTheme.softPink, width: 3) : null,
              ),
              child: isDone ? const Icon(Icons.check, size: 11, color: Colors.white) : null,
            ),
            if (!isLast)
              Container(
                width: 2,
                height: 38,
                color: isDone ? CozyTheme.primaryPink.withValues(alpha: 0.4) : const Color(0xFFE5E5EA),
              ),
          ],
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: TextStyle(
                  fontSize: 13.5,
                  fontWeight: isCurrent ? FontWeight.w900 : FontWeight.w700,
                  color: isDone ? CozyTheme.sweetCocoa : CozyTheme.mutedText,
                ),
              ),
              const SizedBox(height: 2),
              Text(desc, style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText)),
            ],
          ),
        ),
      ],
    );
  }
}
