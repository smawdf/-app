import 'dart:math' show min;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../../data/food_images.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';
import '../widgets/cozy_toast.dart';

/// 订单详情页 —— 1:1 对齐 `demo/index.html` 中的 `#subpage-order-detail`
///
/// 严格实现 6 项核心结构：
/// 1. 顶部返回栏：标题「订单详情」，返回上一页。
/// 2. Order Stepper 卡片（白色圆角卡片，`CozyCard`）：
///    - 顶部行：订单编号（`#${order.id.substring(0, min(8, order.id.length))}`）+ 状态胶囊（待确认/已接单/备餐中/上菜啦/已完成/已取消）
///    - 5步流程时间轴（横向连线 Stepper）：
///      1: 已下单
///      2: 已接单
///      3: 备餐中
///      4: 上菜啦
///      5: 打卡完成
///      横向一条底线，激活部分为 primary 颜色，小圆圈显示序号 1-5，下方是阶段名称。
/// 3. 饲养员出锅大照卡片（`📸 饲养员出锅大照` + `打卡回忆录` 胶囊）：
///    - 真实图片（优先 momentImageUrl，其次菜品图）或大厨空态占位引导卡片。
/// 4. 菜品清单与结算卡片（`菜品清单与结算`）：
///    - 每道菜的名称、份数与价格，分隔线下方为实付糖糖币（🍬 ${order.candyCoinsSpent} 币）。
/// 5. 吃货好评反馈卡片（`吃货好评反馈`）：
///    - 真实评价或温和引导文案「等待吃货品尝后留下爱心小评价~」。
/// 6. 底部操作行：
///    - 饲养员角色：如果可以推进，显示渐变大按钮「推进状态：${_nextActionText(order.status)}」；
///    - 允许取消时，提供「取消订单」按钮。
class OrderDetailPage extends StatefulWidget {
  const OrderDetailPage({super.key, required this.order});

  final Order order;

  @override
  State<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends State<OrderDetailPage> {
  late Order _currentOrder;
  bool _updating = false;

  @override
  void initState() {
    super.initState();
    _currentOrder = widget.order;
  }

  /// 饲养员推进做饭状态
  Future<void> _advanceOrder() async {
    final String? next = _currentOrder.nextStatus;
    if (next == null) return;
    HapticFeedback.mediumImpact();
    setState(() => _updating = true);
    final bool ok = await AppState.instance.advanceOrder(_currentOrder);
    if (!mounted) return;
    setState(() {
      _updating = false;
      if (ok) _currentOrder = _currentOrder.copyWithStatus(next);
    });
    showCozyToast(
      context,
      ok ? '已推进做饭状态为：${_currentOrder.statusLabel}' : (AppState.instance.error ?? '推进失败'),
      error: !ok,
    );
  }

  /// 取消订单（仅在进行中有效）
  Future<void> _cancelOrder() async {
    HapticFeedback.lightImpact();
    setState(() => _updating = true);
    final bool ok = await AppState.instance.cancelOrder(_currentOrder);
    if (!mounted) return;
    setState(() {
      _updating = false;
      if (ok) _currentOrder = _currentOrder.copyWithStatus('cancelled');
    });
    showCozyToast(
      context,
      ok ? '订单已取消，糖币已退还！' : (AppState.instance.error ?? '取消失败'),
      error: !ok,
    );
  }

  @override
  Widget build(BuildContext context) {
    final AppState state = AppState.instance;
    final Order order = _currentOrder;

    final bool isCaretaker = state.isCaretaker;
    final String? nextActionText = _nextActionText(order.status);
    final bool canAdvance = isCaretaker && nextActionText != null;
    final bool canCancel = order.isActive;
    final bool showActionRow = canAdvance || canCancel || (!isCaretaker && order.isActive);

    return Scaffold(
      backgroundColor: CozyPalette.background,
      body: CozyPage(
        child: Column(
          children: <Widget>[
            _topBar(context),
            Expanded(
              child: ListView(
                padding: EdgeInsets.fromLTRB(16, 4, 16, CozyDock.clearanceOf(context) + 16),
                children: <Widget>[
                  // 1. Order Stepper 卡片
                  _stepperCard(context, order),
                  const SizedBox(height: 14),

                  // 2. 饲养员出锅大照卡片
                  _photoCard(context, order),
                  const SizedBox(height: 14),

                  // 3. 菜品清单与结算卡片
                  _dishItemsCard(context, order),
                  const SizedBox(height: 14),

                  // 4. 吃货好评反馈卡片
                  _buyerFeedbackCard(context, order),

                  // 5. 底部操作行
                  if (showActionRow) ...<Widget>[
                    const SizedBox(height: 18),
                    _bottomActions(context, order, isCaretaker, nextActionText, canCancel),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 顶部返回栏：标题「订单详情」，返回上一页
  Widget _topBar(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: ConstrainedBox(
        constraints: const BoxConstraints(minHeight: 56),
        child: Stack(
          alignment: Alignment.center,
          children: <Widget>[
            Align(
              alignment: Alignment.centerLeft,
              child: IconButton(
                onPressed: () => Navigator.of(context).maybePop(),
                icon: const Icon(Icons.arrow_back, color: CozyPalette.primary),
              ),
            ),
            Text(
              '订单详情',
              style: Theme.of(context).textTheme.headlineSmall!.copyWith(
                    fontWeight: FontWeight.w900,
                    color: CozyPalette.primary,
                  ),
            ),
          ],
        ),
      ),
    );
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  1. Order Stepper 卡片 (白色圆角卡片，CozyCard)
  // ══════════════════════════════════════════════════════════════════════════════
  Widget _stepperCard(BuildContext context, Order order) {
    final String shortId = order.id.substring(0, min(8, order.id.length));

    return CozyCard(
      radius: 24,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          // 顶部行：订单编号 + 状态胶囊
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text(
                '#$shortId',
                style: const TextStyle(
                  fontFamily: 'monospace',
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF9CA3AF),
                ),
              ),
              _statusBadge(order.status),
            ],
          ),
          const SizedBox(height: 16),

          // 5步流程时间轴（横向连线 Stepper）
          _stepperTimeline(order.status),
        ],
      ),
    );
  }

  /// 状态胶囊：根据状态着色（待确认/已接单/备餐中/上菜啦/已完成/已取消）
  Widget _statusBadge(String status) {
    final (String text, Color bg, Color textColor) = switch (status) {
      'submitted' => ('待确认', const Color(0xFFFEF3C7), const Color(0xFF92400E)),
      'confirmed' => ('已接单', const Color(0xFFEDE9FE), const Color(0xFF6D28D9)),
      'preparing' => ('备餐中', const Color(0xFFFEF3C7), const Color(0xFF92400E)),
      'delivering' => ('上菜啦', const Color(0xFFE0F2FE), const Color(0xFF0369A1)),
      'completed' => ('已完成', const Color(0xFFD1FAE5), const Color(0xFF065F46)),
      'cancelled' => ('已取消', const Color(0xFFF3F4F6), const Color(0xFF6B7280)),
      _ => (status, const Color(0xFFF3F4F6), const Color(0xFF6B7280)),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w700,
          color: textColor,
        ),
      ),
    );
  }

  /// 5步流程时间轴（横向连线 Stepper）
  Widget _stepperTimeline(String status) {
    final int currentStep = switch (status) {
      'submitted' => 1,
      'confirmed' => 2,
      'preparing' => 3,
      'delivering' => 4,
      'completed' => 5,
      _ => 0, // cancelled
    };
    final bool isCancelled = status == 'cancelled';

    const List<(int, String)> steps = <(int, String)>[
      (1, '已下单'),
      (2, '已接单'),
      (3, '备餐中'),
      (4, '上菜啦'),
      (5, '打卡完成'),
    ];

    final double progress = (isCancelled || currentStep <= 1)
        ? 0.0
        : ((currentStep - 1) / 4.0).clamp(0.0, 1.0);

    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        const double itemWidth = 44.0;
        final double totalWidth = constraints.maxWidth;
        final double startX = itemWidth / 2;
        final double endX = totalWidth - itemWidth / 2;
        final double lineSpan = endX - startX;

        return Stack(
          alignment: Alignment.topCenter,
          children: <Widget>[
            // 背景灰底线
            Positioned(
              top: 11,
              left: startX,
              width: lineSpan,
              child: Container(
                height: 2,
                color: const Color(0xFFE5E7EB),
              ),
            ),
            // 激活主色线
            if (progress > 0)
              Positioned(
                top: 11,
                left: startX,
                width: lineSpan * progress,
                child: Container(
                  height: 2,
                  color: CozyPalette.primary,
                ),
              ),
            // 5个步骤圆圈与名称
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: steps.map(((int, String) step) {
                final int stepNum = step.$1;
                final String stepLabel = step.$2;
                final bool isPassed = !isCancelled && stepNum <= currentStep;
                final bool isCurrent = !isCancelled && stepNum == currentStep;

                final Color circleBg = isPassed ? CozyPalette.primary : const Color(0xFFE5E7EB);
                final Color circleText = isPassed ? Colors.white : const Color(0xFF9CA3AF);
                final Color labelColor = isPassed ? CozyPalette.primary : const Color(0xFF9CA3AF);
                final FontWeight labelWeight = isCurrent
                    ? FontWeight.w800
                    : (isPassed ? FontWeight.w700 : FontWeight.w500);

                return SizedBox(
                  width: itemWidth,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      Container(
                        width: 24,
                        height: 24,
                        decoration: BoxDecoration(
                          color: circleBg,
                          shape: BoxShape.circle,
                          boxShadow: isCurrent
                              ? <BoxShadow>[
                                  BoxShadow(
                                    color: CozyPalette.primary.withValues(alpha: 0.35),
                                    blurRadius: 6,
                                    offset: const Offset(0, 2),
                                  ),
                                ]
                              : null,
                        ),
                        alignment: Alignment.center,
                        child: Text(
                          '$stepNum',
                          style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.w800,
                            color: circleText,
                          ),
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        stepLabel,
                        maxLines: 1,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: labelWeight,
                          color: labelColor,
                        ),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ],
        );
      },
    );
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  2. 饲养员出锅大照卡片 (📸 饲养员出锅大照 + 打卡回忆录 胶囊)
  // ══════════════════════════════════════════════════════════════════════════════
  Widget _photoCard(BuildContext context, Order order) {
    final String? photoUrl = _resolveOrderPhoto(order);

    return CozyCard(
      radius: 24,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          // 标题行
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              const Row(
                children: <Widget>[
                  Text('📸', style: TextStyle(fontSize: 14)),
                  SizedBox(width: 4),
                  Text(
                    '饲养员出锅大照',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w900,
                      color: CozyPalette.onSurface,
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF1F2),
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(color: const Color(0xFFFECDD3)),
                ),
                child: const Text(
                  '打卡回忆录',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                    color: Color(0xFFE11D48),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // 真实照片或优雅空态引导
          Container(
            width: double.infinity,
            height: 176,
            clipBehavior: Clip.antiAlias,
            decoration: BoxDecoration(
              color: CozyPalette.surfaceContainerLow,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: CozyPalette.outlineVariant.withValues(alpha: 0.3)),
            ),
            child: photoUrl != null
                ? Stack(
                    fit: StackFit.expand,
                    children: <Widget>[
                      Image.network(
                        photoUrl,
                        fit: BoxFit.cover,
                        errorBuilder: (_, _, _) => _chefEmptyPlaceholder(),
                      ),
                      Positioned(
                        bottom: 8,
                        right: 8,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.black.withValues(alpha: 0.6),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              Text('👨‍🍳', style: TextStyle(fontSize: 11)),
                              SizedBox(width: 4),
                              Text(
                                '饲养员掌勺打卡',
                                style: TextStyle(
                                  fontSize: 10,
                                  fontWeight: FontWeight.w500,
                                  color: Colors.white,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  )
                : _chefEmptyPlaceholder(),
          ),
        ],
      ),
    );
  }

  /// 优雅的空态占位引导卡片（大厨图标 + 引导文案）
  Widget _chefEmptyPlaceholder() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: CozyPalette.primary.withValues(alpha: 0.08),
              shape: BoxShape.circle,
            ),
            alignment: Alignment.center,
            child: const Text('👨‍🍳', style: TextStyle(fontSize: 24)),
          ),
          const SizedBox(height: 10),
          Text(
            '出锅后饲养员拍照打卡将展示在这里~',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: CozyPalette.onSurfaceVariant.withValues(alpha: 0.8),
            ),
          ),
        ],
      ),
    );
  }

  /// 解析真实可用照片：优先打卡照片，其次菜品真实照片
  String? _resolveOrderPhoto(Order order) {
    if (isUsableDishPhoto(order.momentImageUrl)) {
      return order.momentImageUrl.trim();
    }
    for (final OrderItem item in order.items) {
      if (isUsableDishPhoto(item.imageUrl)) {
        return item.imageUrl.trim();
      }
    }
    for (final OrderItem item in order.items) {
      if (item.name.trim().isNotEmpty) {
        final String resolved = resolveDishImage(item.name.trim(), current: item.imageUrl);
        if (isUsableDishPhoto(resolved)) {
          return resolved;
        }
      }
    }
    return null;
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  3. 菜品清单与结算卡片 (菜品清单与结算)
  // ══════════════════════════════════════════════════════════════════════════════
  Widget _dishItemsCard(BuildContext context, Order order) {
    return CozyCard(
      radius: 24,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          const Text(
            '菜品清单与结算',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w900,
              color: CozyPalette.onSurface,
            ),
          ),
          const SizedBox(height: 10),

          // 菜品清单
          if (order.items.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                '暂无菜品明细',
                style: TextStyle(
                  fontSize: 12,
                  color: CozyPalette.onSurfaceVariant.withValues(alpha: 0.7),
                ),
              ),
            )
          else
            ...order.items.map((OrderItem item) {
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 5),
                child: Row(
                  children: <Widget>[
                    _DishThumb(imageUrl: item.imageUrl, name: item.name),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        item.name,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: CozyPalette.onSurface,
                        ),
                      ),
                    ),
                    Text(
                      'x${item.quantity}',
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: CozyPalette.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Text(
                      '¥${item.subtotal.toStringAsFixed(2)}',
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: CozyPalette.onSurface,
                      ),
                    ),
                  ],
                ),
              );
            }),

          const SizedBox(height: 10),
          Container(
            height: 1,
            color: const Color(0xFFF0E3DB),
          ),
          const SizedBox(height: 10),

          // 分隔线下方：实付糖糖币
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              const Text(
                '实付糖糖币',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Image.asset(
                    'assets/images/candy_coin.png',
                    width: 15,
                    height: 15,
                    fit: BoxFit.contain,
                    errorBuilder: (_, _, _) => const Text('🍬', style: TextStyle(fontSize: 13)),
                  ),
                  const SizedBox(width: 4),
                  Text(
                    '${order.candyCoinsSpent} 币',
                    style: const TextStyle(
                      fontSize: 14,
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

  // ══════════════════════════════════════════════════════════════════════════════
  //  4. 吃货点单备注卡片 (点单备注)
  // ══════════════════════════════════════════════════════════════════════════════
  Widget _buyerFeedbackCard(BuildContext context, Order order) {
    final bool hasNote = order.buyerNote.trim().isNotEmpty;

    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: <Color>[Colors.white, Color(0xFFFDF0F4)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFF0E3DB)),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: Color(0x0F1D1B18),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          const Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Row(
                children: <Widget>[
                  Text('💌', style: TextStyle(fontSize: 14)),
                  SizedBox(width: 4),
                  Text(
                    '吃货点单备注',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: CozyPalette.onSurface,
                    ),
                  ),
                ],
              ),
              Text(
                '给后厨的悄悄话',
                style: TextStyle(fontSize: 11, color: Color(0xFFE11D48)),
              ),
            ],
          ),
          const SizedBox(height: 10),

          // 真实点单备注内容或默认文案
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.85),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFFF0E3DB)),
            ),
            child: Text(
              hasNote ? '“${order.buyerNote.trim()}”' : '没有特殊忌口，尽情发挥饲养员的厨艺吧~',
              style: TextStyle(
                fontSize: 12,
                height: 1.5,
                fontStyle: hasNote ? FontStyle.italic : FontStyle.normal,
                color: hasNote
                    ? CozyPalette.onSurface
                    : CozyPalette.onSurfaceVariant.withValues(alpha: 0.7),
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  5. 底部操作行
  // ══════════════════════════════════════════════════════════════════════════════
  Widget _bottomActions(
    BuildContext context,
    Order order,
    bool isCaretaker,
    String? nextActionText,
    bool canCancel,
  ) {
    final bool canAdvance = isCaretaker && nextActionText != null;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        // 饲养员角色：如果可以推进，显示渐变大按钮
        if (canAdvance) ...<Widget>[
          _GradientAdvanceButton(
            text: nextActionText,
            onTap: _advanceOrder,
            loading: _updating,
          ),
          if (canCancel) const SizedBox(height: 10),
        ],

        // 吃货角色提示卡片（进行中且不可推进时）
        if (!isCaretaker && order.isActive) ...<Widget>[
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: const Color(0xFFFFFBEB),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFFFDE68A)),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Text('👨‍🍳', style: TextStyle(fontSize: 14)),
                SizedBox(width: 6),
                Flexible(
                  child: Text(
                    '饲养员正在厨房忙碌做饭中，稍候美味即来~',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: Color(0xFF92400E),
                    ),
                  ),
                ),
              ],
            ),
          ),
          if (canCancel) const SizedBox(height: 10),
        ],

        // 允许取消时，提供「取消订单」按钮
        if (canCancel)
          SizedBox(
            width: double.infinity,
            height: 44,
            child: TextButton(
              onPressed: _updating ? null : _cancelOrder,
              style: TextButton.styleFrom(
                foregroundColor: const Color(0xFFB85C5C),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(999),
                ),
              ),
              child: const Text(
                '取消订单',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFFB85C5C),
                ),
              ),
            ),
          ),
      ],
    );
  }

  /// 状态推进文案映射
  String? _nextActionText(String status) {
    switch (status) {
      case 'submitted':
      case 'confirmed':
        return '确认接单';
      case 'preparing':
      case 'delivering':
        return '完成这顿饭';
      default:
        return null;
    }
  }
}

/// 底部渐变大按钮：from CozyPalette.primary to Color(0xFFF59E0B)
class _GradientAdvanceButton extends StatelessWidget {
  const _GradientAdvanceButton({
    required this.text,
    required this.onTap,
    this.loading = false,
  });

  final String text;
  final VoidCallback onTap;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      height: 50,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: <Color>[CozyPalette.primary, Color(0xFFF59E0B)],
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
        ),
        borderRadius: BorderRadius.circular(999),
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: CozyPalette.primary.withValues(alpha: 0.3),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(999),
          onTap: loading ? null : onTap,
          child: Center(
            child: loading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                    ),
                  )
                : Text(
                    text,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w900,
                      color: Colors.white,
                    ),
                  ),
          ),
        ),
      ),
    );
  }
}

/// 菜品缩略小图
class _DishThumb extends StatelessWidget {
  const _DishThumb({required this.imageUrl, required this.name});

  final String imageUrl;
  final String name;

  @override
  Widget build(BuildContext context) {
    final String photo = isUsableDishPhoto(imageUrl)
        ? imageUrl.trim()
        : resolveDishImage(name, current: imageUrl);
    final Widget fallback = Center(
      child: Text(
        imageUrl.isNotEmpty && imageUrl.length <= 4 ? imageUrl : '🍽️',
        style: const TextStyle(fontSize: 16),
      ),
    );
    return Container(
      width: 40,
      height: 40,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: CozyPalette.surfaceContainerLow,
        borderRadius: BorderRadius.circular(10),
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
