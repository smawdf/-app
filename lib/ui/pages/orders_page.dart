import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../orders/order_detail_page.dart';
import '../theme/cozy_glass.dart';
import '../theme/couple_theme.dart';
import '../widgets/cozy_dish_photo.dart';
import '../widgets/cozy_skeletons.dart';
import '../widgets/cozy_toast.dart';

/// 订单页 —— 按 `demo/index.html` 的 `#tab-orders`（index.html:689-712）重排页面骨架。
///
/// demo 自上而下三段，**空数据时三段都还在**，只有列表区换成引导卡：
///   1. 顶部标题区：`ORDER HISTORY` 胶囊 + 标题「做饭进度与记录」+ 右侧「推进状态: 仅饲养员可用」
///   2. 状态筛选：白底分段控件 `全部 / 制作中 / 已完成`（选中＝主色实心 + 白字）
///   3. 订单卡片流（卡片解剖取自 demo `renderOrders()`，index.html:2535-2566）：
///      `#单号` + 状态胶囊 ／ 成品图 + 菜名 + 下单时间 + 消耗糖币 ／ 分隔线 + 提示 + 操作
///
/// 数据只来自 `AppState.instance.orders`（云端 `orders` 表）——
/// 没有对应字段的内容一律不显示，不补任何样例数据。
///
/// 视觉与 `home_page.dart` 同一套：白卡 + `outlineVariant@72%` 描边 +
/// `CozyLight.cardShadow` + 18 / 20 圆角 + 20 水平内边距 + 12~14 卡间距。
/// 注：原生 Kotlin 的 `CozyMotion` token 没有移植到本工程（`cozy_glass.dart` 里
/// 不存在该类），动效沿用 `home_page.dart` 的写法（`flutter_animate` + 显式时长）。
class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key, this.onGoOrdering});

  /// 空态里「去点菜」按钮的回调。
  /// `main_shell.dart:92` 传入 `() => _onTabTap(1)`；缺省时按钮为 no-op。
  final VoidCallback? onGoOrdering;

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

/// demo `Status Filters` 的三段：`all` / `cooking` / `completed`。
///
/// 与 demo 一致只保留三段：`cooking` = `Order.isActive`（submitted…delivering）；
/// demo 没有「已取消」分段，已取消的订单归入「全部」查看。
class _OrderFilter {
  const _OrderFilter(this.key, this.label);

  final String key;
  final String label;

  static const List<_OrderFilter> values = <_OrderFilter>[
    _OrderFilter('all', '全部'),
    _OrderFilter('cooking', '制作中'),
    _OrderFilter('completed', '已完成'),
  ];
}

class _OrdersPageState extends State<OrdersPage> {
  _OrderFilter _filter = _OrderFilter.values.first;

  /// `advanceOrder` 正在推进的订单：AppState 只有全局 busy，不区分订单，
  /// 所以在页面内记当前正在推进的那一单，按钮切「更新中...」。
  String? _updatingOrderId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => AppState.instance.refreshOrders());
  }

  /// demo `filter === 'cooking' ? o.status === 'cooking' : ...`（index.html:2529-2531）
  List<Order> _visibleOrders(List<Order> orders) {
    switch (_filter.key) {
      case 'cooking':
        return orders.where((Order o) => o.isActive).toList();
      case 'completed':
        return orders.where((Order o) => o.status == 'completed').toList();
      default:
        return orders;
    }
  }

  /// 空态文案按当前分段区分，避免三段都长一样。
  String get _emptyTitle {
    switch (_filter.key) {
      case 'cooking':
        return '暂时没有制作中的订单';
      case 'completed':
        return '还没有完成的点菜记录';
      default:
        return '暂无订单哦~';
    }
  }

  /// 失败文案走 `AppState.error`，沿用本文件原有的 SnackBar 反馈。
  Future<void> _advance(Order order) async {
    HapticFeedback.mediumImpact();
    setState(() => _updatingOrderId = order.id);
    final bool ok = await AppState.instance.advanceOrder(order);
    if (!mounted) return;
    setState(() => _updatingOrderId = null);
    if (!ok) {
      final String? error = AppState.instance.error;
      if (error != null) showCozyToast(context, error, error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    // 【性能 Phase 1】订单页只关心订单列表与加载位：只订阅 orders 域。
    return ListenableBuilder(
      listenable: AppState.instance.listenFor(const {Domain.orders}),
      builder: (BuildContext context, Widget? _) {
        final AppState state = AppState.instance;
        final List<Order> visible = _visibleOrders(state.orders);
        final bool firstLoad = state.orders.isEmpty && state.loadingOrders;

        return CozyPage(
          child: RefreshIndicator(
            color: context.coupleTheme.primary,
            backgroundColor: CozyPalette.surface,
            onRefresh: () => state.refreshOrders(),
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              // 与 home_page 同一节奏：滚动区不管水平内边距，每个区块自己 pad 20。
              padding: EdgeInsets.only(bottom: CozyDock.clearanceOf(context)),
              children: <Widget>[
                _buildTopBar(),
                const SizedBox(height: 12),
                _buildFilterTabs(),
                const SizedBox(height: 14),
                ..._buildListBody(
                  isCaretaker: state.isCaretaker,
                  visible: visible,
                  firstLoad: firstLoad,
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  /// demo 顶部标题区（index.html:690-696）。
  /// 左边「ORDER HISTORY 胶囊 + 标题」，右边「推进状态: 仅饲养员可用」；
  /// 3:2 分配宽度，窄屏也不会溢出。
  Widget _buildTopBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 6, 20, 0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Expanded(
            flex: 3,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: CozyPalette.primaryContainer,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: const Text(
                    'ORDER HISTORY',
                    style: TextStyle(
                      fontSize: 10,
                      height: 1.3,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 1.0,
                      color: CozyPalette.primary,
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                Row(
                  children: <Widget>[
                    const Flexible(
                      child: Text(
                        '做饭进度与记录',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 21,
                          height: 1.25,
                          fontWeight: FontWeight.w900,
                          color: CozyPalette.onSurface,
                          letterSpacing: -0.5,
                        ),
                      ),
                    ),
                    const SizedBox(width: 5),
                    Image.asset(
                      'assets/images/cooking.png',
                      width: 20,
                      height: 20,
                      filterQuality: FilterQuality.high,
                      errorBuilder: (context, error, stackTrace) =>
                          const Text('🍳', style: TextStyle(fontSize: 16)),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          const SizedBox.shrink(),
        ],
      ),
    );
  }

  /// demo `Status Filters`（index.html:704-708）：一张白底分段控件。
  /// demo 里外框 `rounded-2xl` + 内块 `rounded-xl`；这里按 home_page 的圆角档位取 18 / 14。
  Widget _buildFilterTabs() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
          ),
        ),
        child: Row(
          children: <Widget>[
            for (final _OrderFilter f in _OrderFilter.values)
              Expanded(child: _filterTab(f)),
          ],
        ),
      ),
    );
  }

  Widget _filterTab(_OrderFilter filter) {
    final bool selected = filter.key == _filter.key;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () {
        if (selected) return;
        HapticFeedback.lightImpact();
        setState(() => _filter = filter);
      },
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
        padding: const EdgeInsets.symmetric(vertical: 7),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected ? context.coupleTheme.primary : Colors.transparent,
          borderRadius: BorderRadius.circular(14),
        ),
        child: Text(
          filter.label,
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 12,
            fontWeight: selected ? FontWeight.w900 : FontWeight.w600,
            color: selected ? Colors.white : CozyPalette.onSurfaceVariant,
          ),
        ),
      ),
    );
  }

  /// demo `#orders-list-container`（index.html:710-713）：卡片流 / 骨架 / 引导空态三选一。
  List<Widget> _buildListBody({
    required bool isCaretaker,
    required List<Order> visible,
    required bool firstLoad,
  }) {
    if (firstLoad) return const <Widget>[CozyOrderSkeletonList()];
    if (visible.isEmpty) return <Widget>[_buildEmptyState()];
    return <Widget>[
      for (int i = 0; i < visible.length; i++) ...<Widget>[
        if (i > 0) const SizedBox(height: 12),
        _buildOrderCard(visible[i], index: i, isCaretaker: isCaretaker),
      ],
    ];
  }

  /// 空态：demo 的列表区换成引导卡 —— 顶部标题区与筛选条不消失。
  Widget _buildEmptyState() {
    final theme = context.coupleTheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 26),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: theme.cardBorder,
            width: 1.2,
          ),
          boxShadow: [
            BoxShadow(
              color: theme.shadowColor.withValues(alpha: 0.08),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          children: <Widget>[
            Container(
              width: 76,
              height: 76,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: theme.primaryLight,
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: theme.cardBorder, width: 1.2),
              ),
              padding: const EdgeInsets.all(4),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(18),
                child: Image.asset(
                  theme.chefAnimAsset,
                  fit: BoxFit.contain,
                  filterQuality: FilterQuality.high,
                  errorBuilder: (context, error, stackTrace) =>
                      const Icon(Icons.restaurant_outlined, size: 32, color: CozyPalette.primary),
                ),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              _emptyTitle,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w900,
                color: CozyPalette.onSurface,
              ),
            ),
            const SizedBox(height: 6),
            const Text(
              '点菜后，这里会显示你们的小饭桌记录',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 11.5, color: CozyPalette.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: CozyPrimaryButton(text: '去点菜', onTap: widget.onGoOrdering ?? () {}),
            ),
          ],
        ),
      ),
    )
        .animate()
        .fadeIn(delay: 40.ms, duration: 280.ms, curve: Curves.fastOutSlowIn)
        .slideY(begin: 0.12, end: 0, duration: 280.ms, curve: Curves.fastOutSlowIn);
  }

  /// 卡片的入场：按序号错峰（第 5 张之后不再加延迟），节奏与 home_page 一致。
  Widget _buildOrderCard(Order order, {required int index, required bool isCaretaker}) {
    final int delay = (index > 4 ? 4 : index) * 28;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: _OrderCard(
        order: order,
        isCaretaker: isCaretaker,
        isUpdating: _updatingOrderId == order.id,
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute<void>(builder: (_) => OrderDetailPage(order: order)),
        ),
        onAdvance: () => _advance(order),
      ),
    )
        .animate()
        .fadeIn(delay: delay.ms, duration: 280.ms, curve: Curves.fastOutSlowIn)
        .slideY(begin: 0.12, end: 0, duration: 280.ms, curve: Curves.fastOutSlowIn);
  }
}

/// demo `renderOrders()` 的卡片（index.html:2536-2565）：
///   行1 `#单号` + 状态胶囊
///   行2 成品图 + 菜名 + 下单时间 + 消耗糖币
///   行3 分隔线 + 左提示 + 右操作（饲养员可推进 / 已完成 / 已取消）
class _OrderCard extends StatelessWidget {
  const _OrderCard({
    required this.order,
    required this.isCaretaker,
    required this.isUpdating,
    required this.onTap,
    required this.onAdvance,
  });

  final Order order;
  final bool isCaretaker;
  final bool isUpdating;
  final VoidCallback onTap;
  final VoidCallback onAdvance;

  @override
  Widget build(BuildContext context) {
    final bool completed = order.status == 'completed';
    final bool cancelled = order.status == 'cancelled';
    final String? actionText = isCaretaker ? _caretakerActionText(order.status) : null;

    return CozyCard(
      onTap: onTap,
      radius: 20,
      padding: const EdgeInsets.all(14),
      color: Colors.white,
      borderColor: CozyPalette.outlineVariant.withValues(alpha: 0.72),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                _orderNumber(order.id),
                style: const TextStyle(
                  fontSize: 10,
                  letterSpacing: 0.4,
                  fontWeight: FontWeight.w600,
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
              const Spacer(),
              _StatusPill(order: order),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: <Widget>[
              ClipRRect(
                borderRadius: BorderRadius.circular(14),
                child: SizedBox(width: 56, height: 56, child: _photo()),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      _dishSummary(order),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w900,
                        color: CozyPalette.onSurface,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '下单时间: ${_orderTime(order.createdAt)}',
                      maxLines: 1,
                      style: const TextStyle(
                        fontSize: 10.5,
                        color: CozyPalette.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: <Widget>[
                        Image.asset(
                          'assets/images/candy.png',
                          width: 14,
                          height: 14,
                          filterQuality: FilterQuality.high,
                          errorBuilder: (context, error, stackTrace) => const Icon(
                            Icons.monetization_on_rounded,
                            size: 13,
                            color: Color(0xFFE8385A),
                          ),
                        ),
                        const SizedBox(width: 3),
                        Text(
                          '${order.candyCoinsSpent} 糖币',
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w900,
                            color: Color(0xFFE8385A),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            height: 1,
            color: CozyPalette.outlineVariant.withValues(alpha: 0.6),
          ),
          const SizedBox(height: 10),
          Row(
            children: <Widget>[
              Expanded(
                child: Text(
                  isCaretaker ? '点击卡片查看详情，或直接推进进度' : '点击查看做饭进度与打卡',
                  maxLines: 2,
                  style: const TextStyle(
                    fontSize: 10.5,
                    color: CozyPalette.onSurfaceVariant,
                  ),
                ),
              ),
              if (actionText != null) ...<Widget>[
                const SizedBox(width: 8),
                _SquishyOrderActionButton(
                  text: isUpdating ? '更新中...' : actionText,
                  iconAsset: 'assets/images/pot.png',
                  enabled: !isUpdating,
                  onTap: onAdvance,
                ),
              ] else if (completed) ...<Widget>[
                const SizedBox(width: 8),
                const Icon(Icons.check_circle_rounded, size: 14, color: CozyPalette.success),
                const SizedBox(width: 4),
                const Text(
                  '已美味享用',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: CozyPalette.success,
                  ),
                ),
              ] else if (cancelled) ...<Widget>[
                const SizedBox(width: 8),
                const Icon(Icons.undo_rounded, size: 14, color: CozyPalette.onSurfaceVariant),
                const SizedBox(width: 4),
                Text(
                  '已退还 ${order.candyCoinsSpent} 糖币',
                  style: const TextStyle(
                    fontSize: 11,
                    color: CozyPalette.onSurfaceVariant,
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }

  /// 卡片成品图：优先 `momentImageUrl`（饲养员出锅打卡照），其次第一道菜的图；
  /// 两者都没有就用 3D 图标兜底 —— 不编造图片。
  Widget _photo() {
    return CozyDishPhoto(
      url: _orderPhotoUrl(order),
      cssWidth: 56,
      fit: BoxFit.cover,
      placeholderColor: CozyPalette.surfaceContainer,
      fallback: Container(
        color: CozyPalette.primaryContainer.withValues(alpha: 0.35),
        alignment: Alignment.center,
        child: Image.asset(
          'assets/images/pot.png',
          width: 26,
          height: 26,
          filterQuality: FilterQuality.high,
          errorBuilder: (context, error, stackTrace) =>
              const Icon(Icons.restaurant_outlined, size: 20, color: CozyPalette.primary),
        ),
      ),
    );
  }
}

/// demo 状态胶囊：制作中 = 暖色、已完成 = 绿色、已取消 = 中性。
/// 文案直接取 `Order.statusLabel`（模型侧唯一真相）。
class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.order});

  final Order order;

  @override
  Widget build(BuildContext context) {
    final bool cancelled = order.status == 'cancelled';
    final bool completed = order.status == 'completed';

    final Color fill = cancelled
        ? CozyPalette.surfaceVariant
        : (completed
            ? CozyPalette.success.withValues(alpha: 0.16)
            : CozyPalette.tertiaryContainer.withValues(alpha: 0.55));
    final Color tint = cancelled
        ? CozyPalette.onSurfaceVariant
        : (completed ? CozyPalette.success : CozyPalette.tertiary);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
      decoration: BoxDecoration(
        color: fill,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: tint.withValues(alpha: 0.22)),
      ),
      child: Text(
        order.statusLabel,
        style: TextStyle(fontSize: 10, fontWeight: FontWeight.w900, color: tint),
      ),
    );
  }
}

/// demo 卡片右下角的推进按钮：`bg-cozy-primary text-white` + 3D 锅图标。
class _SquishyOrderActionButton extends StatefulWidget {
  const _SquishyOrderActionButton({
    required this.text,
    required this.enabled,
    required this.onTap,
    this.iconAsset,
  });

  final String text;
  final bool enabled;
  final VoidCallback onTap;
  final String? iconAsset;

  @override
  State<_SquishyOrderActionButton> createState() => _SquishyOrderActionButtonState();
}

class _SquishyOrderActionButtonState extends State<_SquishyOrderActionButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    return AnimatedScale(
      scale: _pressed ? 0.96 : 1.0,
      duration: const Duration(milliseconds: 120),
      curve: Curves.easeOut,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: (_) => setState(() => _pressed = true),
        onTapUp: (_) => setState(() => _pressed = false),
        onTapCancel: () => setState(() => _pressed = false),
        onTap: widget.enabled ? widget.onTap : null,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
          decoration: BoxDecoration(
            color: widget.enabled ? CozyPalette.primary : CozyPalette.surfaceVariant,
            borderRadius: BorderRadius.circular(999),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text(
                widget.text,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w900,
                  color: widget.enabled ? Colors.white : CozyPalette.onSurfaceVariant,
                ),
              ),
              if (widget.iconAsset != null) ...<Widget>[
                const SizedBox(width: 4),
                Image.asset(
                  widget.iconAsset!,
                  width: 15,
                  height: 15,
                  filterQuality: FilterQuality.high,
                  errorBuilder: (context, error, stackTrace) => const SizedBox.shrink(),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// demo 首行是 `#<短单号>`；真实订单 id 是 UUID，这里只展示尾 6 位。
String _orderNumber(String id) {
  if (id.isEmpty) return '#—';
  final String tail = id.length > 6 ? id.substring(id.length - 6) : id;
  return '#${tail.toUpperCase()}';
}

/// 成品照优先，其次第一道菜的图（与 `anniversary_page.dart:1813` 同一口径）。
String _orderPhotoUrl(Order order) {
  if (order.momentImageUrl.isNotEmpty) return order.momentImageUrl;
  for (final OrderItem item in order.items) {
    if (item.imageUrl.isNotEmpty) return item.imageUrl;
  }
  return '';
}

/// 菜名摘要：`items` 为空时退回备注，再退回占位文案（不编造菜品）。
String _dishSummary(Order order) {
  final String names = order.items.map((OrderItem it) => it.name).join('、');
  if (names.isNotEmpty) return names;
  return order.buyerNote.isNotEmpty ? order.buyerNote : '还没有菜品明细';
}

/// 本地时间 `yyyy-MM-dd HH:mm`。
String _orderTime(DateTime time) {
  final DateTime local = time.toLocal();
  String two(int v) => v.toString().padLeft(2, '0');
  return '${local.year}-${two(local.month)}-${two(local.day)} '
      '${two(local.hour)}:${two(local.minute)}';
}

/// 饲养员可推进的状态 → 按钮文案；其它状态没有推进动作。
String? _caretakerActionText(String status) {
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
