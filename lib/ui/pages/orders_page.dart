import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../orders/order_detail_page.dart';
import '../theme/cozy_glass.dart';
import '../widgets/cozy_skeletons.dart';
import '../widgets/cozy_toast.dart';

/// 订单列表 —— 1:1 移植原生 `OrdersScreen.kt`
///
/// 对照原生：
///   - `OrdersScreen`（OrdersScreen.kt:79-157）：筛选 / 空态 / 卡片列表骨架
///   - `OrderFilter`（70-75）：全部 / 待确认 / 已完成 / 已取消
///   - `OrdersFilterTabs` + `HandDrawnTab`（203-251）
///   - `EmptyOrdersState`（253-270）+ `OrderGuidanceEmptyState`（OrderDesignSystem.kt:70-93）
///   - `StitchOrderCard`（272-385）/ `SquishyOrderActionButton`（387-413）/ `OrderShopAvatar`（415-432）
///   - 文本映射：`toOrderBadgeText`（434-440）/ `toOrderMessage`（444-450）/
///     `caretakerActionText`（452-456）/ `toFriendlyOrderTime`（458-461）
///
/// 原生顶栏由外壳渲染（MainActivity.kt:229-235，标题映射 MainActivity.kt:262-269），
/// Flutter 外壳没有主顶栏，这里按原生 `CozyMainTopBar`（StitchNativeComponents.kt:175-223）自绘。
class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key, this.onGoOrdering});

  /// 原生 `OrdersScreen(onGoOrderingClick = ...)`。
  /// Flutter 外壳（main_shell.dart:79）目前写死 `const OrdersPage()`，
  /// 接线（`OrdersPage(onGoOrdering: () => _onTabTap(1))`）后空态「去点菜」才会跳转。
  final VoidCallback? onGoOrdering;

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

/// 原生 `private enum class OrderFilter(val label: String)`（OrdersScreen.kt:70-75）
class _OrderFilter {
  const _OrderFilter(this.key, this.label);

  final String key;
  final String label;

  static const List<_OrderFilter> values = <_OrderFilter>[
    _OrderFilter('all', '全部'),
    _OrderFilter('pending', '待确认'),
    _OrderFilter('completed', '已完成'),
    _OrderFilter('cancelled', '已取消'),
  ];
}

class _OrdersPageState extends State<OrdersPage> {
  _OrderFilter _filter = _OrderFilter.values.first;

  /// 原生 `uiState.updatingOrderId == order.id`（OrdersViewModel.kt:24）；
  /// AppState 只有全局 busy，不区分订单，所以在页面内记当前正在推进的订单。
  String? _updatingOrderId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => AppState.instance.refreshOrders());
  }

  /// 原生 `visibleOrders`（OrdersScreen.kt:87-92）
  List<Order> _visibleOrders(List<Order> orders) {
    switch (_filter.key) {
      case 'pending':
        return orders.where((Order o) => o.status != 'completed' && o.status != 'cancelled').toList();
      case 'completed':
        return orders.where((Order o) => o.status == 'completed').toList();
      case 'cancelled':
        return orders.where((Order o) => o.status == 'cancelled').toList();
      default:
        return orders;
    }
  }

  /// 原生 `EmptyOrdersState`（253-270）
  String get _emptyTitle {
    switch (_filter.key) {
      case 'pending':
        return '暂时没有待确认订单';
      case 'completed':
        return '还没有完成的点菜记录';
      case 'cancelled':
        return '还没有取消的点菜记录';
      default:
        return '暂无订单哦~';
    }
  }

  /// 原生 `OrdersViewModel.advanceOrder`（OrdersViewModel.kt:59-79）失败文案走 message，
  /// Flutter 侧等价物是 `AppState.error`，沿用文件原有的 SnackBar 反馈。
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
    return ListenableBuilder(
      listenable: AppState.instance,
      builder: (BuildContext context, Widget? _) {
        final AppState state = AppState.instance;
        final List<Order> visible = _visibleOrders(state.orders);
        final bool firstLoad = state.orders.isEmpty && state.busy;

        return CozyPage(
          child: Column(
            children: <Widget>[
              _topBar(context),
              Expanded(
                child: RefreshIndicator(
                  color: CozyPalette.primary,
                  onRefresh: () => state.refreshOrders(),
                  child: ListView.separated(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: EdgeInsets.fromLTRB(20, 20, 20, CozyDock.clearanceOf(context)),
                    itemCount: firstLoad ? 2 : (visible.isEmpty ? 2 : 1 + visible.length),
                    separatorBuilder: (BuildContext context, int index) => const SizedBox(height: 12),
                    itemBuilder: (BuildContext context, int index) {
                      if (index == 0) return _filterTabs();
                      if (firstLoad) return _loadingState();
                      if (visible.isEmpty) return _emptyState();
                      final int i = index - 1;
                      return _orderCard(visible[i], index: i, state: state);
                    },
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  /// 原生 `CozyMainTopBar`（StitchNativeComponents.kt:175-223）
  Widget _topBar(BuildContext context) {
    return CozyMainTopBar(
      title: const Text(
        '订单 - 甜蜜点菜记录',
        textAlign: TextAlign.center,
        maxLines: 2,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontSize: 20,
          height: 24 / 20,
          fontWeight: FontWeight.w900,
          letterSpacing: 0,
          color: CozyPalette.primary,
        ),
      ),
      leading: SizedBox(
        width: 44,
        child: Align(
          alignment: Alignment.centerLeft,
          child: Icon(Icons.favorite, size: 26, color: CozyPalette.primary.withValues(alpha: 0.82)),
        ),
      ),
      trailing: const SizedBox(
        width: 44,
        child: Align(
          alignment: Alignment.centerRight,
          child: Icon(Icons.notifications, size: 24, color: CozyPalette.onSurfaceVariant),
        ),
      ),
    );
  }

  /// 原生 `OrdersFilterTabs`（203-220）+ `HandDrawnTab`（222-251）
  Widget _filterTabs() {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: <Widget>[
          for (int i = 0; i < _OrderFilter.values.length; i++) ...<Widget>[
            if (i > 0) const SizedBox(width: 10),
            CozyPill(
              text: _OrderFilter.values[i].label,
              selected: _OrderFilter.values[i].key == _filter.key,
              color: CozyPalette.secondary,
              onTap: () => setState(() => _filter = _OrderFilter.values[i]),
            ),
          ],
        ],
      ),
    );
  }

  /// 首帧加载态：原生靠 5s 轮询（OrdersViewModel.kt:22）没有独立加载态，
  /// Flutter 无轮询 —— 首次拉取时铺骨架屏而不是裸转圈，避免整页空一下再跳出来。
  Widget _loadingState() {
    return const CozyOrderSkeletonList();
  }

  /// 原生 `EmptyOrdersState`（253-270）→ `OrderGuidanceEmptyState`（OrderDesignSystem.kt:70-93）
  Widget _emptyState() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: Column(
        children: <Widget>[
          Container(
            width: 88,
            height: 88,
            decoration: BoxDecoration(
              color: CozyPalette.primaryContainer,
              borderRadius: BorderRadius.circular(28),
            ),
            child: const Center(child: Icon(Icons.restaurant_outlined, size: 36, color: CozyPalette.primary)),
          ),
          const SizedBox(height: 8),
          Text(
            _emptyTitle,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.titleLarge!.copyWith(
                  color: CozyPalette.onSurface,
                  fontWeight: FontWeight.w700,
                ),
          ),
          const SizedBox(height: 8),
          Text(
            '点菜后，这里会显示你们的小饭桌记录',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium!.copyWith(color: CozyPalette.onSurfaceVariant),
          ),
          const SizedBox(height: 4),
          CozyPrimaryButton(text: '去点菜', onTap: widget.onGoOrdering ?? () {}),
        ],
      ),
    ).animate().fadeIn(delay: 40.ms, duration: 280.ms, curve: Curves.fastOutSlowIn).slideY(
          begin: 0.12,
          end: 0,
          duration: 280.ms,
          curve: Curves.fastOutSlowIn,
        );
  }

  /// 原生 `CozyMotionVisibility(delayMillis = index.coerceAtMost(4) * 28)`（OrdersScreen.kt:104）
  Widget _orderCard(Order order, {required int index, required AppState state}) {
    final int delay = (index > 4 ? 4 : index) * 28;
    return _StitchOrderCard(
      order: order,
      shopName: state.shop?.name ?? '',
      shopCoverUrl: state.shop?.coverUrl ?? '',
      isCaretaker: state.isCaretaker,
      isUpdating: _updatingOrderId == order.id,
      onTap: () => Navigator.of(context).push(
        MaterialPageRoute<void>(builder: (_) => OrderDetailPage(order: order)),
      ),
      onAdvance: () => _advance(order),
    )
        .animate()
        .fadeIn(delay: delay.ms, duration: 280.ms, curve: Curves.fastOutSlowIn)
        .slideY(begin: 0.12, end: 0, duration: 280.ms, curve: Curves.fastOutSlowIn);
  }
}

/// 原生 `StitchOrderCard`（OrdersScreen.kt:272-385）
class _StitchOrderCard extends StatelessWidget {
  const _StitchOrderCard({
    required this.order,
    required this.shopName,
    required this.shopCoverUrl,
    required this.isCaretaker,
    required this.isUpdating,
    required this.onTap,
    required this.onAdvance,
  });

  final Order order;
  final String shopName;
  final String shopCoverUrl;
  final bool isCaretaker;
  final bool isUpdating;
  final VoidCallback onTap;
  final VoidCallback onAdvance;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    final bool completed = order.status == 'completed';
    final bool cancelled = order.status == 'cancelled';
    final bool waiting = order.status == 'submitted' || order.status == 'confirmed';

    final Color cardColor = completed
        ? CozyPalette.surfaceContainerLow.withValues(alpha: 0.92)
        : CozyPalette.secondaryContainer.withValues(alpha: 0.42);

    final String displayName = isCaretaker
        ? (order.buyerName.isNotEmpty ? order.buyerName : '吃货')
        : (shopName.isNotEmpty ? shopName : '我的店铺');
    final String displayLabel = isCaretaker ? '点餐人' : '店铺';

    final String names = order.items.take(2).map((OrderItem it) => it.name).join('、');
    final String dishSummary = names.isNotEmpty
        ? names
        : (order.buyerNote.isNotEmpty ? order.buyerNote : '还没有菜品明细');

    final String? actionText = isCaretaker ? _caretakerActionText(order.status) : null;

    return CozyCard(
      onTap: onTap,
      radius: 14,
      padding: EdgeInsets.zero,
      color: cardColor,
      borderColor: CozyPalette.secondary.withValues(alpha: 0.58),
      shadows: const <BoxShadow>[],
      child: Stack(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 13),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                // 店铺头像 + 「点餐人 / 店铺」+ 下单时间（原生 padding(end = 96.dp) 给右上角徽标让位）
                Padding(
                  padding: const EdgeInsets.only(right: 96),
                  child: Row(
                    children: <Widget>[
                      _OrderShopAvatar(shopName: shopName, coverUrl: shopCoverUrl),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              '$displayLabel：$displayName',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: text.labelLarge!.copyWith(
                                color: CozyPalette.secondary,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            const SizedBox(height: 1),
                            Text(
                              _toFriendlyOrderTime(order.createdAt),
                              maxLines: 1,
                              style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  _toOrderMessage(order.status),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: text.bodySmall!.copyWith(color: CozyPalette.onSurface),
                ),
                const SizedBox(height: 8),
                Row(
                  children: <Widget>[
                    Icon(
                      completed ? Icons.receipt_long : Icons.restaurant_outlined,
                      size: 18,
                      color: completed ? CozyPalette.secondary : CozyPalette.primary,
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        dishSummary,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: text.labelMedium!.copyWith(
                          color: completed ? CozyPalette.secondary : CozyPalette.primary,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    const SizedBox(width: 6),
                    // 原生 CandyCoinIcon(size = 16.dp)
                    Image.asset(
                      'assets/images/candy_coin.png',
                      width: 16,
                      height: 16,
                      fit: BoxFit.contain,
                      errorBuilder: (_, _, _) =>
                          const Text('🍬', style: TextStyle(fontSize: 16)),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      cancelled ? '已返 ${order.candyCoinsSpent}' : '${order.candyCoinsSpent} 枚',
                      maxLines: 1,
                      style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
                    ),
                  ],
                ),
                if (actionText != null) ...<Widget>[
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: <Widget>[
                      _SquishyOrderActionButton(
                        text: isUpdating ? '更新中...' : actionText,
                        enabled: !isUpdating,
                        onTap: onAdvance,
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
          // 右上角状态徽标（OrdersScreen.kt:299-323）：只有左下角圆角 12
          Positioned(
            top: 0,
            right: 0,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 4),
              decoration: BoxDecoration(
                color: (cancelled || completed)
                    ? CozyPalette.surfaceVariant
                    : (waiting ? CozyPalette.tertiaryContainer : CozyPalette.secondary),
                borderRadius: const BorderRadius.only(bottomLeft: Radius.circular(12)),
              ),
              child: Text(
                _toOrderBadgeText(order.status),
                style: text.labelMedium!.copyWith(
                  fontWeight: FontWeight.w900,
                  color: (cancelled || completed)
                      ? CozyPalette.onSurfaceVariant
                      : (waiting ? CozyPalette.tertiary : Colors.white),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 原生 `OrderShopAvatar`（OrdersScreen.kt:415-432）：44dp / 圆角 12 / 白底 / 12% 粉边
class _OrderShopAvatar extends StatelessWidget {
  const _OrderShopAvatar({required this.shopName, required this.coverUrl});

  final String shopName;
  final String coverUrl;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 44,
      height: 44,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: CozyPalette.secondary.withValues(alpha: 0.58)),
      ),
      child: coverUrl.isNotEmpty
          ? Image.network(
              coverUrl,
              // 原生 contentDescription = "${shopName.ifBlank{"店铺"}}的头像"（OrdersScreen.kt:428）
              semanticLabel: '${shopName.isEmpty ? '店铺' : shopName}的头像',
              fit: BoxFit.cover,
              errorBuilder: (BuildContext context, Object error, StackTrace? stackTrace) => const _ShopAvatarFallback(),
            )
          : const _ShopAvatarFallback(),
    );
  }
}

/// 原生兜底是 `R.drawable.shop_banner_stitch`（`OrdersScreen.kt:425`）
class _ShopAvatarFallback extends StatelessWidget {
  const _ShopAvatarFallback();

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      'assets/images/shop_banner_stitch.png',
      fit: BoxFit.cover,
      semanticLabel: '店铺封面',
    );
  }
}

/// 原生 `SquishyOrderActionButton`（OrdersScreen.kt:387-413）
class _SquishyOrderActionButton extends StatefulWidget {
  const _SquishyOrderActionButton({required this.text, required this.enabled, required this.onTap});

  final String text;
  final bool enabled;
  final VoidCallback onTap;

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
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 7),
          decoration: BoxDecoration(
            color: widget.enabled ? CozyPalette.primary : CozyPalette.surfaceVariant,
            borderRadius: BorderRadius.circular(999),
            border: Border.all(color: CozyPalette.secondary.withValues(alpha: 0.58)),
          ),
          child: Text(
            widget.text,
            style: Theme.of(context).textTheme.labelLarge!.copyWith(
                  fontWeight: FontWeight.w900,
                  color: widget.enabled ? Colors.white : CozyPalette.onSurfaceVariant,
                ),
          ),
        ),
      ),
    );
  }
}

/// 原生 `toOrderBadgeText`（OrdersScreen.kt:434-440）
String _toOrderBadgeText(String status) {
  switch (status) {
    case 'submitted':
    case 'confirmed':
      return '待饲养员确认';
    case 'preparing':
    case 'delivering':
      return '准备中';
    case 'completed':
      return '已送达胃里';
    case 'cancelled':
      return '已取消';
    default:
      return '待确认';
  }
}

/// 原生 `toOrderMessage`（OrdersScreen.kt:444-450）—— 文案本身带半角引号
String _toOrderMessage(String status) {
  switch (status) {
    case 'submitted':
    case 'confirmed':
      return '"订单已提交，等待饲养员确认接单。"';
    case 'preparing':
    case 'delivering':
      return '"饲养员已经接单，正在认真准备。"';
    case 'completed':
      return '"这顿安排上啦。"';
    case 'cancelled':
      return '"这单已经取消，下一顿再约。"';
    default:
      return '"对方的小愿望已经送到厨房啦。"';
  }
}

/// 原生 `caretakerActionText`（OrdersScreen.kt:452-456）
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

/// 原生 `toFriendlyOrderTime`（OrdersScreen.kt:458-461）：
/// 空 → "刚刚下单"；否则 take(16)（把 UTC 串换成本地时间后再取前 16 位 = yyyy-MM-dd HH:mm）
String _toFriendlyOrderTime(DateTime time) {
  final String text = time.toLocal().toString();
  if (text.isEmpty) return '刚刚下单';
  return text.length >= 16 ? text.substring(0, 16) : text;
}
