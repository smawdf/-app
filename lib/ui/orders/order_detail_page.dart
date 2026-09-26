import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../../data/food_images.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

/// 订单详情 —— 1:1 移植原生 `OrderDetailScreen.kt`
///
/// 对照原生：
///   - `OrderDetailScreen`（OrderDetailScreen.kt:73-143）：白色满屏 + 自绘顶栏 + 14dp 间距列表
///   - `OrderDetailTopBar`（144-164）
///   - `OrderSummaryCard`（166-246）
///   - `CaretakerOnlyCard`（248-260）
///   - `OrderActionRow`（262-288）/ `GradientOrderActionButton`（290-310）
///   - `TimelineCard`（312-344）+ `progressTimelineEntries`（400-428）
///   - `OrderItemsCard`（346-378）
///   - 文本派生：`toOrderStatusText`（380-386）/ `nextActionText`（388-392）/
///     `buyerDetailText`（430-434）/ `yuanText`（PriceText.kt:3）
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

  /// 原生 `OrderDetailViewModel.advanceStatus`（OrderDetailViewModel.kt:52-92）：
  /// 守卫 + 目标态映射（submitted/confirmed → preparing，preparing/delivering → completed）。
  /// Flutter 侧沿用文件原有的 `AppState.instance.advanceOrder` + 本地乐观副本。
  Future<void> _advanceOrder() async {
    final String? next = _currentOrder.nextStatus;
    if (next == null) return;
    final ScaffoldMessengerState messenger = ScaffoldMessenger.of(context);
    HapticFeedback.mediumImpact();
    setState(() => _updating = true);
    final bool ok = await AppState.instance.advanceOrder(_currentOrder);
    if (!mounted) return;
    setState(() {
      _updating = false;
      if (ok) _currentOrder = _currentOrder.copyWithStatus(next);
    });
    messenger.showSnackBar(
      SnackBar(content: Text(ok ? '已推进做饭状态为：${_currentOrder.statusLabel}' : (AppState.instance.error ?? '推进失败'))),
    );
  }

  /// 原生 `OrderDetailViewModel.cancelOrder`（OrderDetailViewModel.kt:94-120）：
  /// 仅 status 不在 (completed, cancelled) 时可取消。
  Future<void> _cancelOrder() async {
    final ScaffoldMessengerState messenger = ScaffoldMessenger.of(context);
    setState(() => _updating = true);
    final bool ok = await AppState.instance.cancelOrder(_currentOrder);
    if (!mounted) return;
    setState(() {
      _updating = false;
      if (ok) _currentOrder = _currentOrder.copyWithStatus('cancelled');
    });
    messenger.showSnackBar(
      SnackBar(content: Text(ok ? '订单已取消，糖币已退还！' : (AppState.instance.error ?? '取消失败'))),
    );
  }

  @override
  Widget build(BuildContext context) {
    final AppState state = AppState.instance;
    final Order order = _currentOrder;

    // 原生额外要求 `!isHistoricalOrder`（OrderDetailScreen.kt:63-65），
    // Flutter `Order` 没有 pairId，无法判断历史单，只能按角色判断。
    final bool canAdvance = state.isCaretaker;
    final String? nextActionText = canAdvance ? _nextActionText(order.status) : null;
    final bool canCancel = order.isActive;
    final bool caretakerOnly =
        !state.isCaretaker && (order.status == 'submitted' || order.status == 'confirmed');
    final bool showActionRow = nextActionText != null || canCancel;

    return Scaffold(
      backgroundColor: CozyPalette.background,
      body: CozyPage(
        child: Column(
          children: <Widget>[
            _topBar(context),
            Expanded(
              child: ListView(
                padding: EdgeInsets.fromLTRB(20, 8, 20, CozyDock.clearanceOf(context)),
                children: <Widget>[
                  _summaryCard(context, order, state),
                  if (caretakerOnly) ...<Widget>[
                    const SizedBox(height: 14),
                    _caretakerOnlyCard(context),
                  ],
                  if (showActionRow) ...<Widget>[
                    const SizedBox(height: 14),
                    _actionRow(context, nextActionText),
                  ],
                  const SizedBox(height: 14),
                  _timelineCard(context, order),
                  const SizedBox(height: 14),
                  _itemsCard(context, order),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 原生 `OrderDetailTopBar`（OrderDetailScreen.kt:144-164）：
  /// 高度下限 64 / 左右 12 上下 8 / 返回按钮居中偏左 / 「订单详情」居中且 primary 色
  Widget _topBar(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: ConstrainedBox(
        constraints: const BoxConstraints(minHeight: 64),
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

  /// 原生 `OrderSummaryCard`（OrderDetailScreen.kt:166-246）
  Widget _summaryCard(BuildContext context, Order order, AppState state) {
    final TextTheme text = Theme.of(context).textTheme;
    final bool cancelled = order.status == 'cancelled';
    final String shopName =
        (state.shop?.name.isNotEmpty ?? false) ? state.shop!.name : '我的店铺';

    return CozyCard(
      radius: 24,
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              _ShopCover(coverUrl: state.shop?.coverUrl ?? '', shopName: shopName),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  shopName,
                  style: text.bodyLarge!.copyWith(
                    color: CozyPalette.onSurface,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              CozyPill(
                text: _toOrderStatusText(order.status),
                selected: true,
                color: CozyPalette.primary,
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: <Widget>[
              // 原生 buyerAvatarUrl 为空时兜底 Icons.Filled.RestaurantMenu（OrderDetailScreen.kt:207-219）；
              // Flutter `Order` 没有 buyerAvatarUrl，直接走图标兜底。
              Container(
                width: 30,
                height: 30,
                decoration: const BoxDecoration(shape: BoxShape.circle, color: CozyPalette.surface),
                child: const Center(
                  child: Icon(Icons.restaurant_menu, size: 16, color: CozyPalette.primary),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  _buyerDetailText(order),
                  style: text.bodyLarge!.copyWith(
                    color: CozyPalette.primary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          // 原生 `order.addressSnapshot.ifBlank { "小饭桌信息待补充" }`（OrderDetailScreen.kt:222-225）；
          // Flutter `Order` 没有 addressSnapshot，只能常显兜底文案。
          Text(
            '小饭桌信息待补充',
            style: text.bodyLarge!.copyWith(color: CozyPalette.onSurfaceVariant),
          ),
          const SizedBox(height: 12),
          Text(
            order.buyerNote.isNotEmpty ? order.buyerNote : '暂无备注',
            style: text.bodyLarge!.copyWith(color: CozyPalette.onSurfaceVariant),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text(
                '合计',
                style: text.bodyLarge!.copyWith(
                  color: CozyPalette.onSurface,
                  fontWeight: FontWeight.w900,
                ),
              ),
              Text(
                _yuanText(order.totalPrice),
                style: text.bodyLarge!.copyWith(
                  color: CozyPalette.error,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Row(
                children: <Widget>[
                  // 原生 `CandyCoinIcon(size = 20.dp)`
                  Image.asset(
                    'assets/images/candy_coin.png',
                    width: 20,
                    height: 20,
                    fit: BoxFit.contain,
                    errorBuilder: (_, _, _) =>
                        const Text('🍬', style: TextStyle(fontSize: 20)),
                  ),
                  const SizedBox(width: 5),
                  Text(
                    '糖糖币',
                    style: text.bodyLarge!.copyWith(color: CozyPalette.onSurfaceVariant),
                  ),
                ],
              ),
              Text(
                cancelled ? '已返还 ${order.candyCoinsSpent} 枚' : '消耗 ${order.candyCoinsSpent} 枚',
                style: text.bodyLarge!.copyWith(
                  color: cancelled ? CozyPalette.onSurfaceVariant : CozyPalette.primary,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 原生 `CaretakerOnlyCard`（OrderDetailScreen.kt:248-260）
  Widget _caretakerOnlyCard(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: CozyPalette.secondaryContainer.withValues(alpha: 0.52),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            '待饲养员确认',
            style: text.bodyLarge!.copyWith(
              color: CozyPalette.onSurface,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '订单已送达，只有饲养员可以确认接单并开始准备。',
            style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
          ),
        ],
      ),
    );
  }

  /// 原生 `OrderActionRow`（OrderDetailScreen.kt:262-288）：
  /// 两个按钮各占 weight(1f)，间距 12
  Widget _actionRow(BuildContext context, String? nextActionText) {
    final bool canCancel = _currentOrder.isActive;
    return Row(
      children: <Widget>[
        if (nextActionText != null)
          Expanded(
            child: CozyPrimaryButton(
              text: nextActionText,
              onTap: _advanceOrder,
              enabled: !_updating,
            ),
          ),
        if (nextActionText != null && canCancel) const SizedBox(width: 12),
        if (canCancel) Expanded(child: _cancelButton(context)),
      ],
    );
  }

  /// 原生 `OrderActionRow` 里的取消 TextButton（contentColor = Color(0xFFB85C5C)，高 52，全圆角）。
  /// 该色不在 CozyPalette 中，用 `CozyPalette.error` 替代。
  Widget _cancelButton(BuildContext context) {
    return SizedBox(
      height: 52,
      child: TextButton(
        onPressed: _updating ? null : _cancelOrder,
        style: TextButton.styleFrom(
          foregroundColor: CozyPalette.error,
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(999)),
          ),
        ),
        child: Text(
          '取消订单',
          style: Theme.of(context).textTheme.labelLarge!.copyWith(fontWeight: FontWeight.w700),
        ),
      ),
    );
  }

  /// 原生 `TimelineCard`（OrderDetailScreen.kt:312-344）
  Widget _timelineCard(BuildContext context, Order order) {
    final TextTheme text = Theme.of(context).textTheme;
    final List<_TimelineEntry> entries = _progressTimelineEntries(order);

    return CozyCard(
      radius: 24,
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            '订单进度',
            style: text.bodyLarge!.copyWith(
              color: CozyPalette.onSurface,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 12),
          if (entries.isEmpty)
            Text(
              '暂无进度记录',
              style: text.bodyMedium!.copyWith(color: CozyPalette.onSurfaceVariant),
            )
          else
            for (int i = 0; i < entries.length; i++) ...<Widget>[
              if (i > 0) const SizedBox(height: 12),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Icon(
                    entries[i].isCompleted ? Icons.check_circle : Icons.radio_button_unchecked,
                    size: 20,
                    color: entries[i].isCompleted
                        ? CozyPalette.primary
                        : CozyPalette.onSurfaceVariant,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          entries[i].title,
                          style: text.bodyLarge!.copyWith(
                            color: CozyPalette.onSurface,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        if (entries[i].timestamp.isNotEmpty)
                          Text(
                            entries[i].timestamp,
                            style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ],
        ],
      ),
    );
  }

  /// 原生 `OrderItemsCard`（OrderDetailScreen.kt:346-378）
  Widget _itemsCard(BuildContext context, Order order) {
    final TextTheme text = Theme.of(context).textTheme;
    final int total = order.items.fold<int>(0, (int sum, OrderItem it) => sum + it.quantity);

    final List<Widget> rows = <Widget>[];
    for (int i = 0; i < order.items.length; i++) {
      if (i > 0) {
        // 原生 `Box(fillMaxWidth().height(1.dp), background = CozyRose.copy(alpha = 0.08f))`
        rows.add(
          Container(
            width: double.infinity,
            height: 1,
            color: CozyPalette.primary.withValues(alpha: 0.08),
          ),
        );
      }
      rows.add(_itemRow(text, order.items[i]));
    }

    return CozyCard(
      radius: 24,
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text(
                '菜品明细',
                style: text.bodyLarge!.copyWith(
                  color: CozyPalette.onSurface,
                  fontWeight: FontWeight.w900,
                ),
              ),
              Text(
                '共 $total 份',
                style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
              ),
            ],
          ),
          for (final Widget row in rows) ...<Widget>[
            const SizedBox(height: 12),
            row,
          ],
        ],
      ),
    );
  }

  Widget _itemRow(TextTheme text, OrderItem item) {
    return Row(
      children: <Widget>[
        // 【图片适配】明细行原本只有菜名 + 价格，没有任何图片。这里补一个
        // 与购物车一致的 44×44 菜品照片（下单前的菜品图是 emoji 时，
        // 用菜名现解析一张真实照片）。
        _DishThumb(imageUrl: item.imageUrl, name: item.name),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(
                item.name,
                style: text.bodyLarge!.copyWith(
                  color: CozyPalette.onSurface,
                  fontWeight: FontWeight.w600,
                ),
              ),
              Text(
                '${item.quantity} 份 x ${_yuanText(item.unitPrice)}',
                style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
              ),
            ],
          ),
        ),
        Text(
          _yuanText(item.subtotal),
          style: text.bodyLarge!.copyWith(
            color: CozyPalette.onSurface,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}

/// 订单明细行里的 44×44 菜品照片。有图片地址就用，没有就用菜名解析一张，
/// 再不行退回 emoji / 餐具占位。
class _DishThumb extends StatelessWidget {
  const _DishThumb({required this.imageUrl, required this.name});

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
      width: 44,
      height: 44,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: CozyPalette.secondaryContainer.withValues(alpha: 0.62),
        borderRadius: BorderRadius.circular(14),
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

/// 原生 `OrderSummaryCard` 里的 52dp 店铺封面（OrderDetailScreen.kt:180-200），
/// 兜底图 `R.drawable.shop_banner_stitch`（OrderDetailScreen.kt:186）已打包为资源。
class _ShopCover extends StatelessWidget {  const _ShopCover({required this.coverUrl, required this.shopName});

  final String coverUrl;
  final String shopName;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 52,
      height: 52,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: CozyPalette.secondaryContainer.withValues(alpha: 0.62),
        borderRadius: BorderRadius.circular(14),
      ),
      child: coverUrl.isNotEmpty
          ? Image.network(
              coverUrl,
              // 原生 contentDescription = "${shopName.ifBlank{"店铺"}}的头像"（OrderDetailScreen.kt:190）
              semanticLabel: '${shopName.isEmpty ? '店铺' : shopName}的头像',
              fit: BoxFit.cover,
              errorBuilder: (BuildContext context, Object error, StackTrace? stackTrace) =>
                  _placeholder(),
            )
          : _placeholder(),
    );
  }

  Widget _placeholder() => Image.asset(
        'assets/images/shop_banner_stitch.png',
        fit: BoxFit.cover,
        semanticLabel: '店铺封面',
      );
}

/// 原生 `OrderTimelineEntry`（domain model）等值物，只在详情页内部使用
class _TimelineEntry {
  const _TimelineEntry(this.title, this.timestamp, this.isCompleted);

  final String title;
  final String timestamp;
  final bool isCompleted;
}

/// 原生 `toOrderStatusText`（OrderDetailScreen.kt:380-386）
String _toOrderStatusText(String status) {
  switch (status) {
    case 'submitted':
    case 'confirmed':
      return '待饲养员确认';
    case 'preparing':
    case 'delivering':
      return '准备中';
    case 'completed':
      return '已完成';
    case 'cancelled':
      return '已取消';
    default:
      return status;
  }
}

/// 原生 `nextActionText`（OrderDetailScreen.kt:388-392）
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

/// 原生 `orderProgressSteps`（OrderDetailScreen.kt:394-398）+ `progressTimelineEntries`（400-428）
List<_TimelineEntry> _progressTimelineEntries(Order order) {
  const List<List<String>> steps = <List<String>>[
    <String>['submitted', '待饲养员确认'],
    <String>['preparing', '准备中'],
    <String>['completed', '已完成'],
  ];

  final String created = _timestampText(order.createdAt);
  if (order.status == 'cancelled') {
    return <_TimelineEntry>[
      _TimelineEntry('待饲养员确认', created, true),
      const _TimelineEntry('已取消', '', true),
    ];
  }

  String normalized = order.status;
  if (normalized == 'confirmed') normalized = 'submitted';
  if (normalized == 'delivering') normalized = 'preparing';

  int currentIndex = 0;
  for (int i = 0; i < steps.length; i++) {
    if (steps[i][0] == normalized) {
      currentIndex = i;
      break;
    }
  }

  return <_TimelineEntry>[
    for (int i = 0; i < steps.length; i++)
      _TimelineEntry(steps[i][1], i == 0 ? created : '', i <= currentIndex),
  ];
}

/// 原生 `buyerDetailText`（OrderDetailScreen.kt:430-434）
String _buyerDetailText(Order order) {
  final String buyer = order.buyerName.isNotEmpty ? order.buyerName : '对方';
  int count = order.items.fold<int>(0, (int sum, OrderItem it) => sum + it.quantity);
  if (count < order.items.length) count = order.items.length;
  return '$buyer 点了 $count 道菜';
}

/// 原生 `yuanText`（PriceText.kt:3）："¥" + "%.2f"
String _yuanText(double value) => '¥${value.toStringAsFixed(2)}';

/// 原生 timeline 里的时间戳直接用 `createdAt` 串的前 16 位
String _timestampText(DateTime time) {
  final String text = time.toLocal().toString();
  return text.length >= 16 ? text.substring(0, 16) : text;
}
