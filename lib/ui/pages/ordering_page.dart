import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../cart/cart_sheet.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';

// ══════════════════════════════════════════════════════════════════════════════
//  常量 —— 1:1 对齐原生 `OrderingScreen.kt:109-115`
// ══════════════════════════════════════════════════════════════════════════════

/// 原生 `FloatingCartHeight = 66.dp`。
const double _kFloatingCartHeight = 66;

/// 原生 `FloatingCartGap = 8.dp`（购物车条与底栏之间的缝）。
const double _kFloatingCartGap = 8;

/// 原生 `OrderingHandDrawnBorder = Color(0xFF78555E)`（`OrderingScreen.kt:114`），
/// 与 `CozyPalette.secondary` 同值。
const Color _kHandDrawnBorder = CozyPalette.secondary;

const String _kDefaultShopName = '我们的小饭桌';
const String _kDefaultAnnouncement = '今天也给你准备了好吃的 ✨';

/// 原生 `MenuCategory`（`OrderingScreen.kt:479-486` 的 `decoratedCategoryName()`）。
class _MenuCategory {
  const _MenuCategory({required this.id, required this.name});

  final String id;
  final String name;

  String get decoratedName {
    if (name.contains('主') ||
        name.contains('披萨') ||
        name.contains('饭') ||
        name.contains('面')) {
      return '主食';
    }
    if (name.contains('饮') ||
        name.contains('喝') ||
        name.contains('咖啡') ||
        name.contains('茶')) {
      return '饮品';
    }
    if (name.contains('甜') || name.contains('蛋糕') || name.contains('布丁')) {
      return '甜点';
    }
    // 原生 `else -> name.take(3)`（OrderingScreen.kt:484）会把「招牌必吃」
    // 砍成「招牌必」——真机上左侧分类栏只剩三个残缺字，观感就是坏图。
    // 分类名是用户自己取的，且这里 Rail 宽 96dp + Text(maxLines: 2, ellipsis)
    // 已能优雅降级，故只保留原生的语义归类（主食/饮品/甜点），不再硬截断。
    return name;
  }
}

/// 原生 `priceYuanText()`（`OrderingScreen.kt:688-695`）。
String _priceYuanText(double value) {
  final int cents = (value * 100).round();
  if (cents % 100 == 0) return '¥ ${cents ~/ 100}';
  return '¥ ${value.toStringAsFixed(2)}';
}

// ══════════════════════════════════════════════════════════════════════════════
//  页面 —— 对齐原生 `OrderingScreen()`（`OrderingScreen.kt:117-294`）
// ══════════════════════════════════════════════════════════════════════════════

class OrderingPage extends StatefulWidget {
  const OrderingPage({super.key});

  @override
  State<OrderingPage> createState() => _OrderingPageState();
}

class _OrderingPageState extends State<OrderingPage> {
  /// 用来把「加入购物篮」按钮与购物篮图标的坐标换算到页面坐标系。
  final GlobalKey _pageKey = GlobalKey();

  /// 悬浮购物车条里的篮子图标（原生 `cartIconCenter`）。
  final GlobalKey _basketKey = GlobalKey();

  /// 每道菜的「+」按钮，用于飞入动画的起点。
  final Map<String, GlobalKey> _addKeys = <String, GlobalKey>{};

  final TextEditingController _searchCtrl = TextEditingController();

  /// 购物车：菜品 id -> 数量
  final Map<String, MenuItem> _cart = {};
  final Map<String, int> _qty = {};

  String _searchQuery = '';
  String _selectedCategory = '';

  int _flySeq = 0;
  Offset? _flyFrom;
  Offset? _flyTo;

  int get _count => _qty.values.fold(0, (a, b) => a + b);
  double get _total =>
      _qty.entries.fold(0.0, (sum, e) => sum + (_cart[e.key]!.price * e.value));
  int get _coinCost => _total.ceil();

  /// 原生 `OrderingUiState.orderingCategories`（`OrderingViewModel.kt:33-34`）。
  ///
  /// 【真机修正】原生走 `menuRepository.getMenuCategories(SINGLE_SHOP_ID)`，
  /// 该列表由 `menu_dishes.category` 聚合而来。`MenuItem` 现在带 `category` 字段，
  /// 直接聚合即可 —— 此前恒为空数组，所以分类栏永远不渲染。
  List<_MenuCategory> get _categories {
    final Set<String> names = <String>{};
    for (final MenuItem item in AppState.instance.menu) {
      final String c = item.category.trim();
      if (c.isNotEmpty) names.add(c);
    }
    final List<String> sorted = names.toList()..sort();
    return <_MenuCategory>[
      for (final String name in sorted) _MenuCategory(id: name, name: name),
    ];
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshMenu();
    });
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  /// 原生 `OrderingUiState.visibleItems`（`OrderingViewModel.kt:36-53`）。
  ///
  /// 【真机修正】分类过滤现在可用：`MenuItem.category` 已补齐，
  /// 按 `_selectedCategory` 先过滤，再叠加搜索（name / description）。
  List<MenuItem> _visibleItems(List<MenuItem> all) {
    Iterable<MenuItem> items = all;
    if (_selectedCategory.isNotEmpty) {
      items = items.where(
        (MenuItem item) => item.category.trim() == _selectedCategory,
      );
    }
    final String query = _searchQuery.trim();
    if (query.isNotEmpty) {
      final String lower = query.toLowerCase();
      items = items.where((MenuItem item) =>
          item.name.toLowerCase().contains(lower) ||
          item.description.toLowerCase().contains(lower));
    }
    return items.toList();
  }

  Offset? _pageLocalCenter(GlobalKey key) {
    final RenderObject? target = key.currentContext?.findRenderObject();
    final RenderObject? page = _pageKey.currentContext?.findRenderObject();
    if (target is! RenderBox || page is! RenderBox) return null;
    if (!target.attached || !target.hasSize || !page.attached) return null;
    return page.globalToLocal(
      target.localToGlobal(target.size.center(Offset.zero)),
    );
  }

  void _add(MenuItem item, {GlobalKey? fromKey}) {
    HapticFeedback.lightImpact();
    setState(() {
      _cart[item.id] = item;
      _qty[item.id] = (_qty[item.id] ?? 0) + 1;
    });

    // 原生：`cartFlyStart = start - pageRootOffset`，target 用已捕获的购物篮中心。
    if (fromKey == null) return;
    final Offset? start = _pageLocalCenter(fromKey);
    final Offset? target = _pageLocalCenter(_basketKey);
    if (start == null || target == null) return;
    setState(() {
      _flyFrom = start;
      _flyTo = target;
      _flySeq++;
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

  /// 打开购物车清单半屏弹层
  void _openCartSheet() {
    HapticFeedback.lightImpact();
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (_) => CartDetailSheet(
        cart: _cart,
        qty: _qty,
        onAdd: _add,
        onRemove: _remove,
        onClear: () {
          setState(() {
            _cart.clear();
            _qty.clear();
          });
        },
        onCheckout: _openCheckoutDialog,
        candyBalance: AppState.instance.candyCoins,
      ),
    );
  }

  /// 打开给后厨伴侣的悄悄话结算弹窗
  void _openCheckoutDialog() {
    showDialog(
      context: context,
      builder: (_) => CheckoutDialog(
        coinCost: _coinCost,
        candyBalance: AppState.instance.candyCoins,
        onConfirm: (buyerNote) => _submit(buyerNote: buyerNote),
      ),
    );
  }

  /// 菜品详情弹层（原生 `OrderingDishDetailSheet`）
  ///
  /// 【真机修正】这里原本没开 `isScrollControlled`，`showModalBottomSheet` 默认
  /// 只给 9/16 屏高，而弹层内容约 490dp（图 210 + 文案 + 价格 + 两个按钮），
  /// 结果「加入购物篮」整行被挤到屏幕外，点不到（证据 `C5-cart.png`：截图底部
  /// 停在「暖心硬菜 / 小店在售」，价格与按钮全在屏外）。开成可滚动 + 包一层
  /// 滚动容器后，内容按需撑高、超出时可滚，按钮永远可达。
  void _openDishDetail(MenuItem item) {
    HapticFeedback.selectionClick();
    final bool isEater = !AppState.instance.isCaretaker;
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: CozyPalette.surfaceContainerLow,
      showDragHandle: true,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (BuildContext sheetContext) => _OrderingDishDetailSheet(
        item: item,
        canOrder: isEater,
        showDescription: isEater,
        onAdd: () {
          // 原生：`if (viewModel.addToCart(item)) detailItem = null`
          Navigator.of(sheetContext).pop();
          _add(item);
        },
        onClose: () => Navigator.of(sheetContext).pop(),
      ),
    );
  }

  /// 原生 `onManageMenuClick` / `onShopNameClick` —— 都进小店与菜单管理页。
  void _openManageMenu() {
    HapticFeedback.selectionClick();
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => const MenuManagementPage()),
    );
  }

  Future<void> _submit({String buyerNote = ''}) async {
    final state = AppState.instance;
    final dishes = <MenuItem>[];
    _qty.forEach((id, q) {
      for (var i = 0; i < q; i++) {
        dishes.add(_cart[id]!);
      }
    });

    final ok = await state.submitOrder(dishes: dishes, note: buyerNote);
    if (!mounted) return;
    if (ok) {
      setState(() {
        _cart.clear();
        _qty.clear();
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(buyerNote.isNotEmpty
              ? '点单成功！悄悄话已传递给伴侣 💕'
              : (state.toast ?? '点单成功')),
          backgroundColor: CozyTheme.sweetCocoa,
        ),
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
        final bool isEater = !state.isCaretaker;
        final List<MenuItem> items = _visibleItems(state.menu);
        final bool cartEmpty = _cart.isEmpty;

        // 原生 `OrderingScreen.kt:187-194`。
        // 原生 floatingNavClearance = navBottom + FloatingBottomNavMargin(14) +
        // FloatingBottomNavHeight(68) = 82dp；外壳底栏（cozy_glass_dock.dart）已经
        // 按 CozyDock 摆好，这里统一用 CozyDock.clearanceOf(context)
        // = 104 + 系统导航栏 inset —— 原版公式里的 navBottom 不能丢，
        // 丢了购物车条/列表末尾就会被系统导航栏和悬浮底栏一起压住。
        final double cartBottomOffset = CozyDock.clearanceOf(context) + _kFloatingCartGap;
        final double bottomClearance = cartEmpty
            ? CozyDock.clearanceOf(context) + 16
            : cartBottomOffset + _kFloatingCartHeight + 18;

        return Stack(
          key: _pageKey,
          clipBehavior: Clip.none,
          children: <Widget>[
            CozyPage(
              decorative: false,
              // 原生是 `Column(Modifier.fillMaxSize())`，所以横向必须撑满。
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  _ShopCard(
                    shopName: (state.shop?.name ?? '').isEmpty
                        ? _kDefaultShopName
                        : state.shop!.name,
                    bannerImageUrl: state.shop?.coverUrl ?? '',
                    announcement: (state.shop?.announcement ?? '').isEmpty
                        ? _kDefaultAnnouncement
                        : state.shop!.announcement,
                    canManageShop: !isEater,
                    onManageShopClick: _openManageMenu,
                  ),
                  Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 840),
                      child: Padding(
                        padding:
                            const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                        child: _OrderingSearchBar(
                          controller: _searchCtrl,
                          onChanged: (String value) =>
                              setState(() => _searchQuery = value),
                          onClear: () {
                            _searchCtrl.clear();
                            setState(() => _searchQuery = '');
                          },
                        ),
                      ),
                    ),
                  ),
                  Expanded(
                    child: LayoutBuilder(
                      builder: (context, constraints) {
                        final double menuMaxWidth =
                            constraints.maxWidth >= 600 ? 760 : constraints.maxWidth;
                        return Center(
                          child: ConstrainedBox(
                            constraints: BoxConstraints(maxWidth: menuMaxWidth),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 20),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: <Widget>[
                                  if (_categories.isNotEmpty)
                                    _CategoryRail(
                                      categories: _categories,
                                      selectedCategory: _selectedCategory,
                                      bottomClearance: bottomClearance,
                                      onSelect: (String id) => setState(
                                        // 【真机修正】再点一次已选分类 = 取消筛选。
                                        // 原生 `OrderingViewModel.selectCategory` 只能单选、
                                        // 没有「全部」入口，选错分类就回不去，这里补上出口。
                                        () => _selectedCategory =
                                            _selectedCategory == id
                                            ? ''
                                            : id,
                                      ),
                                    ),
                                  Expanded(
                                    child: Center(
                                      child: ConstrainedBox(
                                        constraints:
                                            const BoxConstraints(maxWidth: 440),
                                        child: _DishList(
                                          items: items,
                                          canOrder: isEater,
                                          showDescription: isEater,
                                          bottomClearance: bottomClearance,
                                          addKeys: _addKeys,
                                          quantities: _qty,
                                          onAdd: (MenuItem item, GlobalKey key) =>
                                              _add(item, fromKey: key),
                                          onDecrement: _remove,
                                          onDishClick: _openDishDetail,
                                          onManageMenuClick: _openManageMenu,
                                        ),
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),

            // 原生 `AnimatedVisibility(visible = !cartState.isEmpty)` +
            // `CartFloatingBar`（`OrderingScreen.kt:266-283` / `:585-637`）
            if (!cartEmpty)
              Positioned(
                left: 20,
                right: 20,
                bottom: cartBottomOffset,
                child: _CartFloatingBar(
                  basketKey: _basketKey,
                  count: _count,
                  totalPrice: _total,
                  onCartClick: _openCartSheet,
                  onCheckoutClick: () {
                    if (isEater && !state.busy) _openCheckoutDialog();
                  },
                ).animate().fadeIn(duration: 220.ms).slideY(begin: 0.3, end: 0),
              ),

            // 原生 `CartFlyToBasketAnimation`（`OrderingScreen.kt:697-743`）
            if (_flyFrom != null && _flyTo != null)
              Positioned.fill(
                child: IgnorePointer(
                  child: _CartFlyDot(
                    key: ValueKey<int>(_flySeq),
                    start: _flyFrom!,
                    target: _flyTo!,
                    onFinished: () {
                      if (!mounted) return;
                      setState(() {
                        _flyFrom = null;
                        _flyTo = null;
                      });
                    },
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  搜索框 —— 原生 `OrderingSearchBar`（`OrderingScreen.kt:296-323`）
// ══════════════════════════════════════════════════════════════════════════════

class _OrderingSearchBar extends StatelessWidget {
  const _OrderingSearchBar({
    required this.controller,
    required this.onChanged,
    required this.onClear,
  });

  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final VoidCallback onClear;

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<TextEditingValue>(
      valueListenable: controller,
      builder: (context, value, _) {
        return TextField(
          controller: controller,
          onChanged: onChanged,
          maxLines: 1,
          style: Theme.of(context)
              .textTheme
              .bodyLarge!
              .copyWith(color: CozyPalette.onSurface),
          decoration: cozyInputDecoration(
            hintText: '搜索我的店铺菜品',
            prefixIcon: const Icon(
              Icons.search,
              color: CozyPalette.primary,
              size: 20,
            ),
            suffixIcon: value.text.trim().isEmpty
                ? null
                : IconButton(
                    onPressed: onClear,
                    tooltip: '清空搜索',
                    icon: const Icon(
                      Icons.close,
                      color: CozyPalette.onSurfaceVariant,
                      size: 18,
                    ),
                  ),
          ),
        );
      },
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  小店头部 —— 原生 `ShopCard`（`OrderingScreen.kt:325-433`）
// ══════════════════════════════════════════════════════════════════════════════

class _ShopCard extends StatelessWidget {
  const _ShopCard({
    required this.shopName,
    required this.bannerImageUrl,
    required this.announcement,
    required this.canManageShop,
    required this.onManageShopClick,
  });

  final String shopName;
  final String bannerImageUrl;
  final String announcement;
  final bool canManageShop;
  final VoidCallback onManageShopClick;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    final String displayShopName =
        shopName.trim().isEmpty ? _kDefaultShopName : shopName.trim();
    // 原生：外层 ifBlank 带 ✨，内层 ifBlank 不带 ✨。
    final String rawAnnouncement =
        announcement.trim().isEmpty ? _kDefaultAnnouncement : announcement;
    final String displayAnnouncement = rawAnnouncement.trim().isEmpty
        ? '今天也给你准备了好吃的'
        : rawAnnouncement.trim();

    return LayoutBuilder(
      builder: (context, constraints) {
        final bool expanded = constraints.maxWidth >= 600;
        final double coverWidth = expanded ? 96 : 68;
        final double coverHeight = expanded ? 72 : 62;

        return Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 840),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: expanded ? 96 : 86),
                child: CozyCard(
                  radius: 18,
                  padding: EdgeInsets.zero,
                  child: Padding(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                    child: Row(
                      children: <Widget>[
                        Container(
                          width: coverWidth,
                          height: coverHeight,
                          decoration: BoxDecoration(
                            color: CozyPalette.secondaryContainer
                                .withValues(alpha: 0.56),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: _kHandDrawnBorder.withValues(alpha: 0.36),
                            ),
                          ),
                          clipBehavior: Clip.antiAlias,
                          child: _CoverImage(
                            url: bannerImageUrl,
                            name: displayShopName,
                          ),
                        ),
                        const SizedBox(width: 11),
                        Expanded(
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                displayShopName,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: text.titleLarge!.copyWith(
                                  fontWeight: FontWeight.w900,
                                  color: CozyPalette.onSurface,
                                ),
                              ),
                              const SizedBox(height: 3),
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: <Widget>[
                                  const Padding(
                                    padding: EdgeInsets.only(top: 1),
                                    child: Icon(
                                      Icons.campaign,
                                      color: CozyPalette.primary,
                                      size: 15,
                                    ),
                                  ),
                                  const SizedBox(width: 5),
                                  Expanded(
                                    child: Text(
                                      displayAnnouncement,
                                      maxLines: 2,
                                      overflow: TextOverflow.ellipsis,
                                      style: text.bodySmall!.copyWith(
                                        color: _kHandDrawnBorder,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              if (canManageShop) ...<Widget>[
                                const SizedBox(height: 3),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 8, vertical: 3),
                                  decoration: BoxDecoration(
                                    color: CozyPalette.secondaryContainer
                                        .withValues(alpha: 0.72),
                                    borderRadius: BorderRadius.circular(999),
                                  ),
                                  child: Text(
                                    '浏览模式',
                                    style: text.labelSmall!.copyWith(
                                      fontWeight: FontWeight.w700,
                                      color: CozyPalette.primary,
                                    ),
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                        if (canManageShop)
                          GestureDetector(
                            behavior: HitTestBehavior.opaque,
                            onTap: onManageShopClick,
                            child: const SizedBox(
                              width: 48,
                              height: 48,
                              child: Icon(
                                Icons.edit,
                                color: CozyPalette.primary,
                                size: 22,
                                semanticLabel: '管理店铺',
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

/// 店铺封面：网络图 → emoji → 图标兜底。
///
/// 原生用 Coil `AsyncImage`，空串时落到打包资源
/// `R.drawable.shop_banner_stitch`；Flutter 工程没有这张资源，故用图标兜底。
class _CoverImage extends StatelessWidget {
  const _CoverImage({required this.url, required this.name});

  final String url;
  final String name;

  @override
  Widget build(BuildContext context) {
    final String trimmed = url.trim();
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      return Image.network(
        trimmed,
        fit: BoxFit.cover,
        semanticLabel: name,
        errorBuilder: (_, _, _) => _fallback(),
        loadingBuilder:
            (BuildContext context, Widget child, ImageChunkEvent? progress) =>
                progress == null ? child : _fallback(),
      );
    }
    if (trimmed.isNotEmpty) {
      return Center(
        child: Text(trimmed, style: const TextStyle(fontSize: 30)),
      );
    }
    return _fallback();
  }

  /// 原生兜底 `R.drawable.shop_banner_stitch`（`OrderingScreen.kt:367`）
  Widget _fallback() => Image.asset(
        'assets/images/shop_banner_stitch.png',
        fit: BoxFit.cover,
        semanticLabel: '店铺封面',
      );
}

// ══════════════════════════════════════════════════════════════════════════════
//  分类栏 —— 原生 `CategoryRail`（`OrderingScreen.kt:435-477`）
// ══════════════════════════════════════════════════════════════════════════════

class _CategoryRail extends StatelessWidget {
  const _CategoryRail({
    required this.categories,
    required this.selectedCategory,
    required this.bottomClearance,
    required this.onSelect,
  });

  final List<_MenuCategory> categories;
  final String selectedCategory;
  final double bottomClearance;
  final ValueChanged<String> onSelect;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return SizedBox(
      width: 84,
      child: ListView.separated(
        // 原生：外层 padding(top=8, bottom=8) + contentPadding(top=4, bottom=clearance)
        padding: EdgeInsets.only(top: 12, bottom: bottomClearance + 8),
        itemCount: categories.length,
        separatorBuilder: (BuildContext context, int index) =>
            const SizedBox(height: 10),
        itemBuilder: (BuildContext context, int index) {
          final _MenuCategory category = categories[index];
          final bool selected = category.id == selectedCategory;
          return GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () => onSelect(category.id),
            child: Container(
              constraints: const BoxConstraints(minHeight: 48),
              alignment: Alignment.center,
              padding: const EdgeInsets.symmetric(horizontal: 8),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(999),
                color: selected
                    ? CozyPalette.secondaryContainer
                    : Colors.transparent,
                border: Border.all(
                  color: selected
                      ? CozyPalette.primary.withValues(alpha: 0.24)
                      : Colors.transparent,
                ),
              ),
              child: Text(
                category.decoratedName,
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: text.labelMedium!.copyWith(
                  fontWeight: selected ? FontWeight.w900 : FontWeight.w600,
                  color: selected
                      ? CozyPalette.primary
                      : CozyPalette.onSurfaceVariant,
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  菜品列表 —— 原生 `DishList`（`OrderingScreen.kt:488-521`）
// ══════════════════════════════════════════════════════════════════════════════

class _DishList extends StatelessWidget {
  const _DishList({
    required this.items,
    required this.canOrder,
    required this.showDescription,
    required this.bottomClearance,
    required this.addKeys,
    required this.quantities,
    required this.onAdd,
    required this.onDecrement,
    required this.onDishClick,
    required this.onManageMenuClick,
  });

  final List<MenuItem> items;
  final bool canOrder;
  final bool showDescription;
  final double bottomClearance;
  final Map<String, GlobalKey> addKeys;

  /// 【真机修正】菜品 id -> 购物篮数量。> 0 时卡片右侧渲染 `- n +` 步进器
  /// （对齐 docs/demos 设计稿），而不是只留一个「+」。
  final Map<String, int> quantities;

  final void Function(MenuItem item, GlobalKey key) onAdd;
  final ValueChanged<MenuItem> onDecrement;
  final ValueChanged<MenuItem> onDishClick;
  final VoidCallback onManageMenuClick;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return _EmptyMenu(
        bottomClearance: bottomClearance,
        onManageMenuClick: onManageMenuClick,
      );
    }

    return ListView.separated(
      padding: EdgeInsets.only(
        left: 20,
        top: 8,
        right: 0,
        bottom: bottomClearance + 14,
      ),
      itemCount: items.length,
      separatorBuilder: (BuildContext context, int index) =>
          const SizedBox(height: 10),
      itemBuilder: (BuildContext context, int index) {
        final MenuItem item = items[index];
        final GlobalKey key = addKeys.putIfAbsent(item.id, () => GlobalKey());
        // 原生 `CozyMotionVisibility(delayMillis = min(index,4) * 28)`
        return _SingleShopDishCard(
          item: item,
          canOrder: canOrder,
          showDescription: showDescription,
          addButtonKey: key,
          quantity: quantities[item.id] ?? 0,
          onClick: () => onDishClick(item),
          onAdd: () => onAdd(item, key),
          onDecrement: () => onDecrement(item),
        )
            .animate()
            .fadeIn(
              delay: ((index < 4 ? index : 4) * 28).ms,
              duration: 280.ms,
            )
            .slideY(begin: 0.08, end: 0);
      },
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  单个菜品卡 —— 原生 `SingleShopDishCard`（`OrderingScreen.kt:523-583`）
// ══════════════════════════════════════════════════════════════════════════════

class _SingleShopDishCard extends StatelessWidget {
  const _SingleShopDishCard({
    required this.item,
    required this.canOrder,
    required this.showDescription,
    required this.addButtonKey,
    required this.quantity,
    required this.onClick,
    required this.onAdd,
    required this.onDecrement,
  });

  final MenuItem item;
  final bool canOrder;
  final bool showDescription;
  final GlobalKey addButtonKey;
  final int quantity;
  final VoidCallback onClick;
  final VoidCallback onAdd;
  final VoidCallback onDecrement;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    // 紧凑横排：小缩略图 + 文案，价格与加减单独占一行铺满卡片宽度
    // （卡片只有 ~216dp 宽，价格与加减器并排放不下，会挤成两行）
    // 密度：缩略图 48dp + 8dp 内边距 + 菜名 16sp 一行 ⇒ 整卡 ~100dp，一页能放 3 道以上
    return CozyCard(
      radius: 16,
      padding: const EdgeInsets.all(8),
      onTap: onClick,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              SizedBox(
                width: 48,
                height: 48,
                child: _DishImage(
                  imageUrl: item.imageUrl,
                  name: item.name,
                  backgroundAlpha: 0.62,
                  radius: 10,
                  fit: BoxFit.cover,
                  iconSize: 22,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      item.name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      // 原生 17.sp；这里收到 16.sp，让「黄金蜜蜂脆皮炸鸡」这类 8 字菜名
                      // 在 Rail 84dp 后的卡片里保持一行，一页能多放一道
                      style: text.titleLarge!.copyWith(
                        fontSize: 16,
                        height: 22 / 16,
                        fontWeight: FontWeight.w900,
                        color: CozyPalette.onSurface,
                      ),
                    ),
                    if (showDescription) ...<Widget>[
                      const SizedBox(height: 2),
                      Text(
                        item.description.trim().isEmpty
                            ? '今天也很适合点这一道'
                            : item.description.trim(),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: text.labelSmall!.copyWith(
                          color: CozyPalette.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Expanded(
                child: Text(
                  _priceYuanText(item.price),
                  // 原生 fontSize 19.sp；与 16.sp 的菜名同比例收到 17.sp
                  style: text.titleLarge!.copyWith(
                    fontSize: 17,
                    fontWeight: FontWeight.w900,
                    color: CozyPalette.primary,
                  ),
                ),
              ),
              if (quantity > 0)
                _QuantityStepper(
                  quantity: quantity,
                  enabled: canOrder,
                  plusKey: addButtonKey,
                  onIncrement: onAdd,
                  onDecrement: onDecrement,
                )
              else
                _AddDishButton(
                  buttonKey: addButtonKey,
                  enabled: canOrder,
                  onTap: onAdd,
                ),
            ],
          ),
        ],
      ),
    );
  }
}

/// 菜品图：网络图 → emoji（本工程 `imageUrl` 沿用 emoji 当图的老约定）→ 占位。
class _DishImage extends StatelessWidget {
  const _DishImage({
    required this.imageUrl,
    required this.name,
    required this.backgroundAlpha,
    required this.radius,
    this.height,
    this.fit = BoxFit.cover,
    this.iconSize = 40,
    this.placeholderGap = 6,
  });

  final String imageUrl;
  final String name;
  final double backgroundAlpha;
  final double radius;
  final double? height;
  final BoxFit fit;
  final double iconSize;
  final double placeholderGap;

  @override
  Widget build(BuildContext context) {
    final String url = imageUrl.trim();

    Widget content;
    if (url.startsWith('http://') || url.startsWith('https://')) {
      content = Image.network(
        url,
        fit: fit,
        semanticLabel: name,
        errorBuilder: (_, _, _) => _placeholder(context),
        loadingBuilder:
            (BuildContext context, Widget child, ImageChunkEvent? progress) =>
                progress == null ? child : _placeholder(context),
      );
    } else if (url.isNotEmpty) {
      content = Center(
        child: Text(url, style: TextStyle(fontSize: iconSize * 1.5)),
      );
    } else {
      content = _placeholder(context);
    }

    Widget box = Container(
      decoration: BoxDecoration(
        color: CozyPalette.secondaryContainer.withValues(alpha: backgroundAlpha),
        borderRadius: BorderRadius.circular(radius),
      ),
      clipBehavior: Clip.antiAlias,
      child: content,
    );
    if (height != null) {
      box = SizedBox(width: double.infinity, height: height, child: box);
    }
    return box;
  }

  Widget _placeholder(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Icon(Icons.restaurant, color: CozyPalette.primary, size: iconSize),
          SizedBox(height: placeholderGap),
          Text(
            '暂无图片',
            style: Theme.of(context).textTheme.labelSmall!.copyWith(
                  fontWeight: FontWeight.w700,
                  color: CozyPalette.onSurfaceVariant,
                ),
          ),
        ],
      ),
    );
  }
}

/// 【真机修正】购物篮数量步进器（`- n +`）。
///
/// 真机与 `docs/demos/shots/redesign_5pages_sheet.png` 设计稿一致：菜品加入购物篮后，
/// 卡片右下角从单个「+」变成 `- n +`，让吃货能直接在同一张卡上加减。
/// `plusKey` 继续复用 `AddDishButton` 那个 GlobalKey，飞入购物篮动画的起点不受影响。
class _QuantityStepper extends StatelessWidget {
  const _QuantityStepper({
    required this.quantity,
    required this.enabled,
    required this.plusKey,
    required this.onIncrement,
    required this.onDecrement,
  });

  final int quantity;
  final bool enabled;
  final GlobalKey plusKey;
  final VoidCallback onIncrement;
  final VoidCallback onDecrement;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    final bool on = enabled;
    return Container(
      height: 36,
      padding: const EdgeInsets.symmetric(horizontal: 3),
      decoration: BoxDecoration(
        color: CozyPalette.secondaryContainer,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: CozyPalette.primary.withValues(alpha: 0.18)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          _StepperButton(
            icon: Icons.remove,
            enabled: on,
            onTap: onDecrement,
            semanticLabel: '减少一份',
          ),
          SizedBox(
            width: 22,
            child: Text(
              '$quantity',
              textAlign: TextAlign.center,
              style: text.labelLarge!.copyWith(
                fontWeight: FontWeight.w900,
                color: on ? CozyPalette.primary : CozyPalette.onSurfaceVariant,
              ),
            ),
          ),
          _StepperButton(
            key: plusKey,
            icon: Icons.add,
            enabled: on,
            onTap: onIncrement,
            semanticLabel: '再加一份',
          ),
        ],
      ),
    );
  }
}

class _StepperButton extends StatelessWidget {
  const _StepperButton({
    super.key,
    required this.icon,
    required this.enabled,
    required this.onTap,
    required this.semanticLabel,
  });

  final IconData icon;
  final bool enabled;
  final VoidCallback onTap;
  final String semanticLabel;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: semanticLabel,
      child: InkResponse(
        onTap: enabled ? onTap : null,
        radius: 18,
        child: SizedBox(
          width: 30,
          height: 30,
          child: Icon(
            icon,
            size: 17,
            color: enabled ? CozyPalette.primary : CozyPalette.onSurfaceVariant,
          ),
        ),
      ),
    );
  }
}

/// 原生 `AddDishButton`（`OrderingScreen.kt:745-775`）：36×36 / 圆 / 按下缩 0.96。
class _AddDishButton extends StatefulWidget {
  const _AddDishButton({
    required this.buttonKey,
    required this.enabled,
    required this.onTap,
  });

  final GlobalKey buttonKey;
  final bool enabled;
  final VoidCallback onTap;

  @override
  State<_AddDishButton> createState() => _AddDishButtonState();
}

class _AddDishButtonState extends State<_AddDishButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final bool on = widget.enabled;
    return SizedBox(
      key: widget.buttonKey,
      width: 36,
      height: 36,
      child: AnimatedScale(
        scale: _pressed && on ? 0.96 : 1.0,
        duration: const Duration(milliseconds: 120),
        curve: Curves.easeOut,
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTapDown: on ? (_) => setState(() => _pressed = true) : null,
          onTapUp: on ? (_) => setState(() => _pressed = false) : null,
          onTapCancel: on ? () => setState(() => _pressed = false) : null,
          onTap: on ? widget.onTap : null,
          child: Container(
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: on
                  ? CozyPalette.primary
                  : CozyPalette.onSurfaceVariant.withValues(alpha: 0.42),
            ),
            child: const Icon(
              Icons.add,
              color: CozyPalette.background,
              size: 20,
              semanticLabel: '加入购物篮',
            ),
          ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  空态 —— 原生 `EmptyMenu`（`OrderingScreen.kt:671-686`）
// ══════════════════════════════════════════════════════════════════════════════

class _EmptyMenu extends StatelessWidget {
  const _EmptyMenu({
    required this.bottomClearance,
    required this.onManageMenuClick,
  });

  final double bottomClearance;
  final VoidCallback onManageMenuClick;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return Padding(
      padding: EdgeInsets.only(
        left: 24,
        top: 24,
        right: 24,
        bottom: bottomClearance,
      ),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            const CozyIconBadge(
              icon: Icons.restaurant,
              background: CozyPalette.secondaryContainer,
              tint: CozyPalette.primary,
              size: 62,
            ),
            const SizedBox(height: 12),
            Text(
              '还没有菜品',
              style: text.titleMedium!.copyWith(
                fontWeight: FontWeight.w900,
                color: CozyPalette.onSurface,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              '先添加菜名、价格、图片和分类，然后就能开始下单。',
              textAlign: TextAlign.center,
              style: text.bodyMedium!.copyWith(
                color: CozyPalette.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: CozyPrimaryButton(
                text: '去设置菜品',
                onTap: onManageMenuClick,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  悬浮购物车条 —— 原生 `CartFloatingBar`（`OrderingScreen.kt:585-637`）
// ══════════════════════════════════════════════════════════════════════════════

class _CartFloatingBar extends StatelessWidget {
  const _CartFloatingBar({
    required this.basketKey,
    required this.count,
    required this.totalPrice,
    required this.onCartClick,
    required this.onCheckoutClick,
  });

  final GlobalKey basketKey;
  final int count;
  final double totalPrice;
  final VoidCallback onCartClick;
  final VoidCallback onCheckoutClick;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    // 【本轮改动】原来是一张不透明的 `CozyCard`（白底 + 圆角 999），压在菜品大图上
    // 就是一块白板；用户要求做成液态玻璃。这里换成 `GlassContainer`，参数与底部
    // Tab 胶囊（`lib/ui/shell/cozy_glass_dock.dart`）**逐项对齐**——同一块玻璃的观感
    // 才是连着的：premium 档（才有折射与色散）+ iOS 26 级中性白雾 0x1FFFFFFF +
    // blur 8 / thickness 24 / refractiveIndex 1.25 / chromaticAberration 0.03 /
    // saturation 1.20 / lightIntensity 0.65 / ambientRim 0。
    // 内容层（购物袋、价格、去结算）都是普通控件，不再套 Glass 控件
    // （包文档明确禁止 Glass 套 Glass：内层会被降级成不折射）。
    return GlassContainer(
      height: _kFloatingCartHeight,
      shape: const LiquidRoundedSuperellipse(
        borderRadius: GlassDefaults.capsuleRadius,
      ),
      quality: GlassQuality.premium,
      allowElevation: true,
      clipBehavior: Clip.antiAlias,
      settings: const LiquidGlassSettings(
        glassColor: Color(0x1FFFFFFF),
        blur: 8.0,
        thickness: 24.0,
        refractiveIndex: 1.25,
        chromaticAberration: 0.03,
        saturation: 1.20,
        lightIntensity: 0.65,
        ambientRim: 0.0,
      ),
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onCartClick,
        child: Padding(
          padding: const EdgeInsets.only(left: 26, right: 8),
          child: Row(
            children: <Widget>[
              SizedBox(
                key: basketKey,
                width: 38,
                height: 38,
                child: Stack(
                  clipBehavior: Clip.none,
                  children: <Widget>[
                    const Center(
                      child: Icon(
                        Icons.shopping_bag,
                        color: CozyPalette.primary,
                        size: 30,
                        semanticLabel: '购物篮',
                      ),
                    ),
                    Positioned(
                      top: -5,
                      right: -5,
                      child: Container(
                        width: 20,
                        height: 20,
                        alignment: Alignment.center,
                        decoration: const BoxDecoration(
                          shape: BoxShape.circle,
                          color: CozyPalette.primary,
                        ),
                        child: Text(
                          '$count',
                          style: text.labelSmall!.copyWith(
                            fontSize: 10,
                            fontWeight: FontWeight.w900,
                            color: CozyPalette.background,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(left: 14),
                  child: Text(
                    _priceYuanText(totalPrice),
                    // 原生 fontSize 22.sp
                    style: text.titleLarge!.copyWith(
                      fontSize: 22,
                      fontWeight: FontWeight.w900,
                      color: CozyPalette.onSurface,
                    ),
                  ),
                ),
              ),
              _SquishyCheckoutButton(text: '去结算', onTap: onCheckoutClick),
            ],
          ),
        ),
      ),
    );
  }
}

/// 原生 `SquishyCheckoutButton`（`OrderingScreen.kt:639-669`）：高 54 / 圆角 999 /
/// 按下缩到 0.96 / 文字 20.sp w900。
class _SquishyCheckoutButton extends StatefulWidget {
  const _SquishyCheckoutButton({required this.text, required this.onTap});

  final String text;
  final VoidCallback onTap;

  @override
  State<_SquishyCheckoutButton> createState() => _SquishyCheckoutButtonState();
}

class _SquishyCheckoutButtonState extends State<_SquishyCheckoutButton> {
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
        onTap: widget.onTap,
        child: Container(
          height: 54,
          padding: const EdgeInsets.symmetric(horizontal: 34),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(999),
            color: CozyPalette.primary,
          ),
          child: Text(
            widget.text,
            maxLines: 1,
            style: Theme.of(context).textTheme.titleLarge!.copyWith(
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                  color: CozyPalette.background,
                ),
          ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  加入购物篮的飞入动画 —— 原生 `CartFlyToBasketAnimation`（`OrderingScreen.kt:697-743`）
// ══════════════════════════════════════════════════════════════════════════════

class _CartFlyDot extends StatefulWidget {
  const _CartFlyDot({
    super.key,
    required this.start,
    required this.target,
    required this.onFinished,
  });

  final Offset start;
  final Offset target;
  final VoidCallback onFinished;

  @override
  State<_CartFlyDot> createState() => _CartFlyDotState();
}

class _CartFlyDotState extends State<_CartFlyDot>
    with SingleTickerProviderStateMixin {
  /// 原生 `CozyMotion.Standard = 220` / `CozyMotion.CartFly = 620`
  static const int _jumpMs = 220;
  static const int _flyMs = 620;
  static const double _radius = 14;

  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: _jumpMs + _flyMs),
  );

  @override
  void initState() {
    super.initState();
    _controller.addStatusListener((AnimationStatus status) {
      if (status == AnimationStatus.completed) widget.onFinished();
    });
    _controller.forward();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // 原生：先跳到 (start.x - 30, start.y - 72)，再飞向购物篮中心。
    final Offset peak =
        Offset(widget.start.dx - 30, widget.start.dy - 72);
    final double jumpEnd = _jumpMs / (_jumpMs + _flyMs);

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        final double t = _controller.value;
        final Offset position;
        final double scale;
        if (t <= jumpEnd) {
          final double p = Curves.easeOut.transform(t / jumpEnd);
          position = Offset.lerp(widget.start, peak, p)!;
          scale = 1 + 0.1 * p;
        } else {
          final double p =
              Curves.easeOut.transform((t - jumpEnd) / (1 - jumpEnd));
          position = Offset.lerp(peak, widget.target, p)!;
          scale = 1.1 - 0.38 * p;
        }
        return Align(
          alignment: Alignment.topLeft,
          child: Transform.translate(
            offset: Offset(position.dx - _radius, position.dy - _radius),
            child: Transform.scale(
              scale: scale,
              child: Container(
                width: _radius * 2,
                height: _radius * 2,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: CozyPalette.primary,
                  border: Border.all(
                    color: CozyPalette.background,
                    width: 2,
                  ),
                ),
                child: const Icon(
                  Icons.add,
                  color: CozyPalette.background,
                  size: 18,
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  菜品详情弹层 —— 原生 `OrderingDishDetailSheet`（`OrderingScreen.kt:806-852`）
// ══════════════════════════════════════════════════════════════════════════════

class _OrderingDishDetailSheet extends StatelessWidget {
  const _OrderingDishDetailSheet({
    required this.item,
    required this.canOrder,
    required this.showDescription,
    required this.onAdd,
    required this.onClose,
  });

  final MenuItem item;
  final bool canOrder;
  final bool showDescription;
  final VoidCallback onAdd;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return SafeArea(
      top: false,
      child: SingleChildScrollView(
        padding: const EdgeInsets.only(left: 20, right: 20, bottom: 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            _DishImage(
              imageUrl: item.imageUrl,
              name: item.name,
              backgroundAlpha: 1,
              radius: 24,
              height: 210,
              fit: BoxFit.contain,
              iconSize: 44,
              placeholderGap: 8,
            ),
            const SizedBox(height: 14),
            Text(
              item.name,
              style: text.headlineSmall!.copyWith(
                fontWeight: FontWeight.w900,
                color: CozyPalette.onSurface,
              ),
            ),
          if (showDescription) ...<Widget>[
            const SizedBox(height: 14),
            Text(
              item.description.trim().isEmpty ? '暂无描述' : item.description.trim(),
              style: text.bodyMedium!.copyWith(
                color: CozyPalette.onSurfaceVariant,
              ),
            ),
          ],
          const SizedBox(height: 14),
          // 原生这里是 Row[ CozyPill(item.categoryId, CozyTerracotta), Text("小店在售") ]。
          // 【真机修正】`MenuItem` 现在带上 `category` 了，把分类胶囊补回来。
          Row(
            children: <Widget>[
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: CozyPalette.primary.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  item.category.trim().isEmpty ? '未分类' : item.category.trim(),
                  style: text.labelMedium!.copyWith(
                    fontWeight: FontWeight.w700,
                    color: CozyPalette.primary,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                '小店在售',
                style: text.bodyMedium!.copyWith(
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            _priceYuanText(item.price),
            // 原生 fontSize 26.sp
            style: text.headlineSmall!.copyWith(
              fontSize: 26,
              fontWeight: FontWeight.w900,
              color: CozyPalette.primary,
            ),
          ),
          const SizedBox(height: 14),
          Row(
            children: <Widget>[
              Expanded(
                child: TextButton(
                  onPressed: onClose,
                  child: Text(
                    '关闭',
                    style: text.bodyLarge!.copyWith(
                      color: CozyPalette.onSurfaceVariant,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: CozyPrimaryButton(
                  text: canOrder ? '加入购物篮' : '吃货专属',
                  onTap: onAdd,
                  enabled: canOrder,
                ),
              ),
            ],
          ),
          ],
        ),
      ),
    );
  }
}
