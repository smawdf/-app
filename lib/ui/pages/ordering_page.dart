import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../../data/xiachufang_client.dart';
import '../candy/candy_coins_page.dart';
import '../cart/cart_sheet.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';
import '../theme/couple_theme.dart';
import '../widgets/cozy_celebration.dart';
import '../widgets/cozy_skeletons.dart';
import '../widgets/cozy_toast.dart';

// ══════════════════════════════════════════════════════════════════════════════
//  常量
// ══════════════════════════════════════════════════════════════════════════════

/// 原生 `FloatingCartHeight = 66.dp`。
///
/// demo 的 `#floating-cart-bar` 是 `h-[52px]`，这里**故意保留 66**：
/// `CozyDock.clearanceOf()` 的让位公式是围着这个高度算出来的，
/// 改成 52 会让「购物车条 + 列表底部留白」整体少 14dp，
/// 低端机上末尾卡片会滑到系统导航栏底下（真机上踩过）。
const double _kFloatingCartHeight = 66;

/// 原生 `FloatingCartGap = 8.dp`（购物车条与底栏之间的缝）。
const double _kFloatingCartGap = 8;

/// 「我的店铺」还没拉到数据时的招牌名（UI 骨架文案，不是数据）。
const String _kDefaultShopName = '我们的小饭桌';

/// 左侧分类栏第一项的 id。
///
/// demo 的 `selectCategory('all')` 对应这里：空串同时就是 `_selectedCategory`
/// 的「不筛选」语义，所以两者天然同值，不用再加一层映射。
const String _kAllCategoryId = '';

/// demo `text-rose-600` / `bg-rose-600`：糖币价的强调红。
///
/// `CozyPalette` 里没有这一档玫红；`home_page.dart` 的糖币价用的就是这枚字面值。
/// 抽成具名常量，免得页面里散落魔法色。
const Color _kCoinRose = Color(0xFFE8385A);

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
    // 分类名是用户自己取的，且这里 Rail 宽 80 + Text(maxLines: 2, ellipsis)
    // 已能优雅降级，故只保留原生的语义归类（主食/饮品/甜点），不再硬截断。
    return name;
  }
}

/// demo 菜品行的「🍬 数字」。
///
/// 价格本来就是糖糖币（CLAUDE.md：`candyCoinsCost() = ceil(totalPrice)`），
/// 所以不再显示「¥」——整币不拖小数点，零头保留两位。
String _coinText(double value) {
  final int cents = (value * 100).round();
  if (cents % 100 == 0) return '${cents ~/ 100}';
  return value.toStringAsFixed(2);
}

// ══════════════════════════════════════════════════════════════════════════════
//  页面 —— 骨架对齐 demo `#tab-ordering`
//
//  demo 的点餐页只有两块：
//    ① Shop Header Banner：招牌行（icon + 店名 + 营业状态 + 副标题 + 角色视角）
//                           + 公告条
//    ② 双栏：左「分类栏」（全部美味 + N 个分类）/ 右「菜品列表」
//  购物车条是 demo 里天然的**条件渲染**块（`totalQty > 0` 才浮出），
//  这里同样保持条件渲染。
//  工程既有的「搜索菜品」「管理店铺」「封面图」不在 demo 骨架上，
//  但都是有数据支撑的真实能力，分别收进招牌头（紧凑搜索行）与公告条（右端箭头）。
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
  String _selectedCategory = _kAllCategoryId;

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
    final bool isEater = !AppState.instance.isCaretaker;
    if (!isEater) {
      showCozyToast(context, '👨‍🍳 饲养员负责掌勺做菜，由吃货负责点单加菜哦~');
      return;
    }
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
    final bool isEater = !AppState.instance.isCaretaker;
    if (!isEater) {
      showCozyToast(context, '👨‍🍳 饲养员负责掌勺做菜，吃货加菜后可在订单中查看~');
      return;
    }
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      barrierColor: Colors.black.withValues(alpha: 0.28),
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
  void _openDishDetail(MenuItem item) {
    HapticFeedback.selectionClick();
    final bool isEater = !AppState.instance.isCaretaker;
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      barrierColor: Colors.black.withValues(alpha: 0.35),
      showDragHandle: false,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (BuildContext sheetContext) => Container(
        height: MediaQuery.of(sheetContext).size.height * 0.85,
        decoration: const BoxDecoration(
          color: Color(0xFFFBF8F5),
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
          boxShadow: <BoxShadow>[
            BoxShadow(
              color: Color(0x33000000),
              blurRadius: 20,
              offset: Offset(0, -4),
            ),
          ],
        ),
        padding: const EdgeInsets.fromLTRB(18, 4, 18, 16),
        child: _OrderingDishDetailSheet(
          item: item,
          canOrder: isEater,
          showDescription: true,
          onAdd: () {
            Navigator.of(sheetContext).pop();
            _add(item);
          },
          onClose: () => Navigator.of(sheetContext).pop(),
        ),
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

  /// demo 里饲养员视角的购物车按钮是「管理/撒糖」——落到糖糖币页。
  void _openCandy() {
    HapticFeedback.selectionClick();
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => const CandyCoinsPage()),
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
      // 下单成功撒一次品牌色纸屑（overlay 层，不改变页面结构）
      unawaited(showCozyConfetti(context));
      showCozyToast(
        context,
        buyerNote.isNotEmpty ? '点单成功！悄悄话已传递给伴侣 💕' : (state.toast ?? '点单成功'),
        duration: const Duration(seconds: 3),
      );
    } else if (state.error != null) {
      showCozyToast(context, state.error!, error: true, duration: const Duration(seconds: 3));
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return ListenableBuilder(
      listenable: state,
      builder: (context, _) {
        final bool caretakerMode = state.isCaretaker;
        final bool isEater = !caretakerMode;
        final List<MenuItem> items = _visibleItems(state.menu);
        final bool cartEmpty = _cart.isEmpty;

        // 原生 `OrderingScreen.kt:187-194`。
        // 原生 floatingNavClearance = navBottom + FloatingBottomNavMargin(14) +
        // FloatingBottomNavHeight(68) = 82dp；外壳底栏（cozy_glass_dock.dart）已经
        // 按 CozyDock 摆好，这里统一用 CozyDock.clearanceOf(context)
        // = 104 + 系统导航栏 inset —— 原版公式里的 navBottom 不能丢，
        // 丢了购物车条/列表末尾就会被系统导航栏和悬浮底栏一起压住。
        final double dockClearance = CozyDock.clearanceOf(context);
        final double cartBottomOffset = dockClearance + _kFloatingCartGap;
        final double bottomClearance = cartEmpty
            ? dockClearance + 16
            : cartBottomOffset + _kFloatingCartHeight + 18;

        // 招牌头上的「营业状态」——由菜单里有没有在售菜推出来，不是写死的文案。
        final String statusText;
        final Color statusColor;
        if (state.menu.isEmpty) {
          statusText = '待上新';
          statusColor = CozyPalette.onSurfaceVariant;
        } else if (state.menu.any((MenuItem m) => m.isAvailable)) {
          statusText = '营业中';
          statusColor = CozyPalette.success;
        } else {
          statusText = '歇业中';
          statusColor = CozyPalette.tertiary;
        }

        final String shopName = (state.shop?.name ?? '').trim();
        final String announcement = (state.shop?.announcement ?? '').trim();

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
                  // ① demo `Shop Header Banner`：招牌行 + 公告条（+ 工程既有的搜索行）
                  _ShopHeaderBanner(
                    shopName: shopName.isEmpty ? _kDefaultShopName : shopName,
                    coverUrl: state.shop?.coverUrl ?? '',
                    statusText: statusText,
                    statusColor: statusColor,
                    roleTagText: caretakerMode ? '饲养员视角' : '吃货视角',
                    announcement: announcement,
                    announcementPlaceholder: caretakerMode
                        ? '还没有公告 · 点这里给吃货留句话'
                        : '饲养员还没写公告',
                    canManageShop: !isEater,
                    onManageShopClick: _openManageMenu,
                    searchField: _ShopSearchField(
                      controller: _searchCtrl,
                      onChanged: (String value) =>
                          setState(() => _searchQuery = value),
                      onClear: () {
                        _searchCtrl.clear();
                        setState(() => _searchQuery = '');
                      },
                    ),
                  ),

                  // ② demo 双栏：左分类栏（常驻）+ 右菜品列表
                  Expanded(
                    child: LayoutBuilder(
                      builder: (context, constraints) {
                        final double menuMaxWidth =
                            constraints.maxWidth >= 600 ? 760 : constraints.maxWidth;
                        return Center(
                          child: ConstrainedBox(
                            constraints: BoxConstraints(maxWidth: menuMaxWidth),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: <Widget>[
                                // demo 的分类栏是无数据也在的一块骨架：
                                // 分类聚合为空时它仍常驻，只是内容回到引导文案。
                                _CategoryRail(
                                  categories: _categories,
                                  selectedCategory: _selectedCategory,
                                  bottomClearance: bottomClearance,
                                  onSelect: (String id) =>
                                      setState(() => _selectedCategory = id),
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
                                        loading: state.loadingMenu && items.isEmpty,
                                        addKeys: _addKeys,
                                        quantities: _qty,
                                        onAdd: (MenuItem item, GlobalKey key) {
                                          if (caretakerMode) {
                                            showCozyToast(context, '👨‍🍳 饲养员负责掌勺做菜，由吃货负责点单加菜哦~');
                                            return;
                                          }
                                          _add(item, fromKey: key);
                                        },
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
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),

            // 原生 `AnimatedVisibility(visible = !cartState.isEmpty)` +
            // `CartFloatingBar`（`OrderingScreen.kt:266-283` / `:585-637`）。
            // demo 侧同样是条件渲染（`totalQty > 0` 才浮出），所以空车时整条不画。
            if (!cartEmpty)
              Positioned(
                left: 20,
                right: 20,
                bottom: cartBottomOffset,
                child: _CartFloatingBar(
                  basketKey: _basketKey,
                  count: _count,
                  totalPrice: _total,
                  // demo：`cart-submit-btn` 的文案随角色变（吃货=去结算 / 饲养员=管理·撒糖）
                  checkoutLabel: caretakerMode ? '管理 · 撒糖' : '去结算',
                  onCartClick: () {
                    if (caretakerMode) {
                      showCozyToast(context, '👨‍🍳 饲养员负责掌勺做菜，吃货加菜后可在订单中查看~');
                      return;
                    }
                    _openCartSheet();
                  },
                  onCheckoutClick: () {
                    if (state.loadingOrders) return;
                    if (caretakerMode) {
                      _openCandy();
                    } else {
                      _openCheckoutDialog();
                    }
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
//  搜索行 —— 原生 `OrderingSearchBar`（`OrderingScreen.kt:296-323`）
//
//  demo 的点餐页骨架里没有搜索（搜索在发现页），但这是工程既有的真实能力，
//  收进招牌头最后一行，不额外占一条独立横带，骨架仍与 demo 同为「头部 + 双栏」。
// ══════════════════════════════════════════════════════════════════════════════

class _ShopSearchField extends StatelessWidget {
  const _ShopSearchField({
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
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
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
//  店铺招牌头 —— demo `Shop Header Banner`（`#tab-ordering` 顶部整块）
//
//  demo 结构：
//    Row[ 40×40 圆角主色徽（🏡） | 店名 + 营业状态胶囊 + 副标题 | 角色视角胶囊 ]
//    Row[ 公告条：📢 + 单行公告 ]
//  底色是 `from-cozy-peach/50 via-cozy-pink-light/30 to-cozy-bg` 的浅渐变 + 底部描边。
// ══════════════════════════════════════════════════════════════════════════════

class _ShopHeaderBanner extends StatelessWidget {
  const _ShopHeaderBanner({
    required this.shopName,
    required this.coverUrl,
    required this.statusText,
    required this.statusColor,
    required this.roleTagText,
    required this.announcement,
    required this.announcementPlaceholder,
    required this.canManageShop,
    required this.onManageShopClick,
    required this.searchField,
  });

  final String shopName;
  final String coverUrl;
  final String statusText;
  final Color statusColor;
  final String roleTagText;
  final String announcement;
  final String announcementPlaceholder;
  final bool canManageShop;
  final VoidCallback onManageShopClick;
  final Widget searchField;

  @override
  Widget build(BuildContext context) {
    final bool hasAnnouncement = announcement.isNotEmpty;
    final theme = context.coupleTheme;
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: <Color>[
            theme.primaryLight.withValues(alpha: 0.65),
            theme.accentLight.withValues(alpha: 0.35),
            theme.bgPage,
          ],
          stops: const <double>[0.0, 0.55, 1.0],
        ),
      ),
      child: DecoratedBox(
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: CozyLight.hairline)),
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 6, 20, 10),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 840),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      _CrestAvatar(url: coverUrl, size: 40),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: <Widget>[
                            Row(
                              children: <Widget>[
                                Flexible(
                                  child: Text(
                                    shopName,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                      fontSize: 14,
                                      height: 1.3,
                                      fontWeight: FontWeight.w900,
                                      color: CozyPalette.onSurface,
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 6),
                                _StatusPill(
                                  text: statusText,
                                  color: statusColor,
                                ),
                              ],
                            ),
                            const SizedBox(height: 3),
                            const Text(
                              '饲养员掌勺 · 专属吃货的点单乐园',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontSize: 11,
                                color: CozyPalette.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      _RoleTag(text: roleTagText),
                    ],
                  ),
                  const SizedBox(height: 10),
                  _AnnouncementBar(
                    text: hasAnnouncement ? announcement : announcementPlaceholder,
                    isPlaceholder: !hasAnnouncement,
                    canManage: canManageShop,
                    onTap: onManageShopClick,
                  ),
                  const SizedBox(height: 8),
                  searchField,
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// demo 招牌徽：40×40 圆角主色块。
///
/// 工程里有 `Shop.coverUrl` 这个真实字段（历史数据里也存过 emoji），
/// 所以位置对齐 demo 的 40×40 徽位，内容优先用真实封面，缺图退 3D 爪印。
class _CrestAvatar extends StatelessWidget {
  const _CrestAvatar({required this.url, this.size = 40});

  final String url;
  final double size;

  @override
  Widget build(BuildContext context) {
    final theme = context.coupleTheme;
    final String trimmed = url.trim();
    final Widget content;
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      content = Image.network(
        trimmed,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => _fallback(theme),
        loadingBuilder:
            (BuildContext context, Widget child, ImageChunkEvent? progress) =>
                progress == null ? child : _fallback(theme),
      );
    } else if (trimmed.isNotEmpty && trimmed.runes.length <= 2) {
      // 只有极短的内容才当表情渲染，长文本直接走兜底，避免撑破 40dp 徽位。
      content = Center(
        child: Text(trimmed, style: TextStyle(fontSize: size * 0.5)),
      );
    } else {
      content = _fallback(theme);
    }

    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: theme.primaryLight,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: theme.cardBorder, width: 1.2),
        boxShadow: [
          BoxShadow(
            color: theme.shadowColor.withValues(alpha: 0.12),
            blurRadius: 6,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      padding: const EdgeInsets.all(2),
      clipBehavior: Clip.antiAlias,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(11),
        child: content is SizedBox ? _fallback(theme) : content,
      ),
    );
  }

  Widget _fallback(CoupleThemeSpec theme) => Center(
        child: Image.asset(
          theme.chefAnimAsset,
          fit: BoxFit.contain,
          errorBuilder: (context, error, stackTrace) => Image.asset(
            'assets/images/paw.png',
            width: 24,
            height: 24,
            filterQuality: FilterQuality.high,
            errorBuilder: (_, _, _) => const Text('🏡', style: TextStyle(fontSize: 18)),
          ),
        ),
      );
}

/// demo 店名右侧的营业状态胶囊（`bg-emerald-100 text-emerald-800`）。
/// 配色走首页 `_buildStatusChip` 的同一套写法：底色 16% / 描边 45% / 文字实色。
class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1.5),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.16),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withValues(alpha: 0.45), width: 0.8),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 9.5,
          fontWeight: FontWeight.w900,
          color: color,
        ),
      ),
    );
  }
}

/// demo 招牌行右端 `#ordering-role-tag`（饲养员视角 / 吃货视角）。
/// 写法与首页顶部的 `OUR KITCHEN` 标签胶囊一致。
class _RoleTag extends StatelessWidget {
  const _RoleTag({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: CozyPalette.primaryContainer.withValues(alpha: 0.55),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: CozyPalette.primary.withValues(alpha: 0.18),
        ),
      ),
      child: Text(
        text,
        maxLines: 1,
        style: const TextStyle(
          fontSize: 10,
          height: 1.3,
          fontWeight: FontWeight.w900,
          color: CozyPalette.primary,
        ),
      ),
    );
  }
}

/// demo 公告条（`bg-white/80 rounded-xl px-2.5 py-1.5` + 📢 + 单行截断）。
///
/// 没有公告时**不塌成空块**，换成引导文案（饲养员可点进小店管理写一条）。
class _AnnouncementBar extends StatelessWidget {
  const _AnnouncementBar({
    required this.text,
    required this.isPlaceholder,
    required this.canManage,
    required this.onTap,
  });

  final String text;
  final bool isPlaceholder;
  final bool canManage;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return CozyCard(
      radius: 16,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      onTap: canManage ? onTap : null,
      child: Row(
        children: <Widget>[
          Image.asset(
            'assets/images/sparkles.png',
            width: 14,
            height: 14,
            filterQuality: FilterQuality.high,
            errorBuilder: (context, error, stackTrace) =>
                const Text('📢', style: TextStyle(fontSize: 12)),
          ),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              text,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 11,
                color: isPlaceholder
                    ? CozyPalette.onSurfaceVariant
                    : CozyPalette.secondary,
              ),
            ),
          ),
          if (canManage) ...<Widget>[
            const SizedBox(width: 6),
            const Icon(
              Icons.chevron_right_rounded,
              size: 15,
              color: CozyPalette.onSurfaceVariant,
            ),
          ],
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  分类栏 —— demo `Category Sidebar`（`w-20` + 每项 `border-l-4`）
//
//  与旧版的差别是结构性的：
//    ① 首项固定「全部美味」（demo 的 `selectCategory('all')`），
//       不再是「再点一次取消筛选」这种没有出口的隐式交互；
//    ② 选中态从「胶囊」改成 demo 的「整行粉底 + 左侧 4px 主色竖条」；
//    ③ 分类聚合为空时**整栏不消失**，回到引导文案。
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
    return Container(
      width: 80,
      decoration: BoxDecoration(
        color: CozyPalette.surface.withValues(alpha: 0.72),
        border: const Border(
          right: BorderSide(color: CozyLight.hairline),
        ),
      ),
      child: ListView(
        // demo：分类栏自身 `py-2` + `pb-36`（给悬浮购物车条让位）
        padding: EdgeInsets.only(top: 8, bottom: bottomClearance + 8),
        children: <Widget>[
          _CategoryTab(
            label: '全部美味',
            selected: selectedCategory.isEmpty,
            onTap: () => onSelect(_kAllCategoryId),
          ),
          for (final _MenuCategory category in categories)
            _CategoryTab(
              label: category.decoratedName,
              selected: category.id == selectedCategory,
              onTap: () => onSelect(category.id),
            ),
          if (categories.isEmpty)
            const Padding(
              padding: EdgeInsets.fromLTRB(8, 10, 8, 0),
              child: Text(
                '还没有分类\n去小店管理加一个',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 9.5,
                  height: 1.45,
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _CategoryTab extends StatelessWidget {
  const _CategoryTab({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: Container(
        constraints: const BoxConstraints(minHeight: 48),
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 10),
        decoration: BoxDecoration(
          color: selected
              ? context.coupleTheme.primaryLight.withValues(alpha: 0.75)
              : Colors.transparent,
          border: Border(
            left: BorderSide(
              color: selected ? context.coupleTheme.primary : Colors.transparent,
              width: 4,
            ),
          ),
        ),
        child: Text(
          label,
          textAlign: TextAlign.center,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            fontSize: 11.5,
            height: 1.3,
            fontWeight: selected ? FontWeight.w900 : FontWeight.w600,
            color: selected
                ? context.coupleTheme.primary
                : CozyPalette.onSurfaceVariant,
          ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  菜品列表 —— demo `#dish-list-container`（`p-3 space-y-3`）
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
    this.loading = false,
  });

  final List<MenuItem> items;
  final bool canOrder;
  final bool showDescription;
  final double bottomClearance;
  final Map<String, GlobalKey> addKeys;

  /// 首页/冷启动拉菜单期间为 true：列表还是空的，但先铺骨架屏，
  /// 不要立刻弹「还没添加菜品」的空态（那会让用户以为真的一道菜都没有）。
  final bool loading;

  /// 菜品 id -> 购物篮数量。> 0 时卡片右下角渲染 `- n +` 步进器
  /// （对齐 demo 菜品行右下角的加减）。
  final Map<String, int> quantities;

  final void Function(MenuItem item, GlobalKey key) onAdd;
  final ValueChanged<MenuItem> onDecrement;
  final ValueChanged<MenuItem> onDishClick;
  final VoidCallback onManageMenuClick;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      if (loading) {
        return CozyDishSkeletonList(bottomClearance: bottomClearance);
      }
      return _EmptyMenu(
        bottomClearance: bottomClearance,
        onManageMenuClick: onManageMenuClick,
      );
    }

    return ListView.separated(
      padding: EdgeInsets.only(
        left: 12,
        top: 8,
        right: 12,
        bottom: bottomClearance + 14,
      ),
      itemCount: items.length,
      separatorBuilder: (BuildContext context, int index) =>
          const SizedBox(height: 12),
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
//  单个菜品卡 —— demo `renderDishes()` 里的卡片模板
//
//  demo 结构：
//    Row[ 80×80 方图（左上角可挂标签） | Column[ 菜名 / 描述 / 🍬价格 + 已点N次 /
//                                          右下角 [- n +] ] ]
//  demo 里的 `dish.tag` 工程数据层没有对应字段，故不渲染该角标。
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
    return CozyCard(
      radius: 18,
      padding: const EdgeInsets.all(10),
      onTap: onClick,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: <Widget>[
          SizedBox(
            width: 80,
            height: 80,
            child: _DishImage(
              imageUrl: item.imageUrl,
              name: item.name,
              backgroundAlpha: 0.62,
              radius: 12,
              fit: BoxFit.cover,
              iconSize: 26,
              placeholderGap: 4,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  item.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 13.5,
                    height: 1.35,
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
                    style: const TextStyle(
                      fontSize: 10.5,
                      color: CozyPalette.onSurfaceVariant,
                    ),
                  ),
                ],
                const SizedBox(height: 6),
                // demo：`🍬 价格` + `已点N次` 同一行
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
                        color: _kCoinRose,
                      ),
                    ),
                    const SizedBox(width: 3),
                    Text(
                      _coinText(item.price),
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w900,
                        color: _kCoinRose,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Flexible(
                      child: Text(
                        '已点 ${item.salesCount} 次',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 10,
                          color: CozyPalette.onSurfaceVariant,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                // demo：加减行右对齐；`- 数量` 只在已加入购物篮时出现
                Align(
                  alignment: Alignment.centerRight,
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      if (quantity > 0) ...<Widget>[
                        _StepButton(
                          icon: Icons.remove,
                          filled: false,
                          enabled: canOrder,
                          onTap: onDecrement,
                          semanticLabel: '减少一份',
                        ),
                        SizedBox(
                          width: 24,
                          child: Text(
                            '$quantity',
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w900,
                              color: CozyPalette.onSurface,
                            ),
                          ),
                        ),
                      ],
                      _StepButton(
                        key: addButtonKey,
                        icon: Icons.add,
                        filled: true,
                        enabled: canOrder,
                        onTap: onAdd,
                        semanticLabel: '加入购物篮',
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// demo 加减按钮：26 圆形，`+` 主色实心白字、`-` 白底描边主色字，
/// 按下缩到 0.9（demo `active:scale-90`）。
class _StepButton extends StatelessWidget {
  const _StepButton({
    super.key,
    required this.icon,
    required this.filled,
    required this.enabled,
    required this.onTap,
    required this.semanticLabel,
  });

  final IconData icon;
  final bool filled;
  final bool enabled;
  final VoidCallback onTap;
  final String semanticLabel;

  @override
  Widget build(BuildContext context) {
    final Color background = filled
        ? (enabled
            ? CozyPalette.primary
            : CozyPalette.onSurfaceVariant.withValues(alpha: 0.35))
        : CozyPalette.surface;
    final Color foreground = !filled
        ? (enabled ? CozyPalette.primary : CozyPalette.onSurfaceVariant)
        : CozyPalette.surface;

    return Semantics(
      button: true,
      label: semanticLabel,
      child: CozyPressable(
        onTap: enabled ? onTap : null,
        scale: 0.90,
        child: Container(
          width: 26,
          height: 26,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: background,
            border: filled
                ? null
                : Border.all(color: CozyPalette.outlineVariant),
          ),
          child: Icon(icon, size: 15, color: foreground),
        ),
      ),
    );
  }
}

/// 菜品图：网络图 → emoji（本工程 `imageUrl` 沿用 emoji 当图的老约定）→ 3D 碗兜底。
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
    } else if (url.startsWith('assets/')) {
      content = Image.asset(
        url,
        fit: fit,
        semanticLabel: name,
        errorBuilder: (_, _, _) => _placeholder(context),
      );
    } else if (url.isNotEmpty && url.runes.length <= 4) {
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
          Image.asset(
            'assets/images/bowl.png',
            width: iconSize,
            height: iconSize,
            filterQuality: FilterQuality.high,
            errorBuilder: (context, error, stackTrace) => Icon(
              Icons.restaurant,
              color: CozyPalette.primary,
              size: iconSize,
            ),
          ),
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

// ══════════════════════════════════════════════════════════════════════════════
//  空态 —— 原生 `EmptyMenu`（`OrderingScreen.kt:671-686`）
//
//  demo 里菜品列表是 JS 渲染的、永远有数据，没有空态分支；
//  这里保留「列表容器 + 引导卡」的骨架，不整块消失。
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
            _AssetBadge(
              asset: 'assets/images/pot.png',
              fallback: const Icon(
                Icons.restaurant,
                size: 26,
                color: CozyPalette.primary,
              ),
              size: 62,
              radius: 20,
              iconSize: 34,
            ),
            const SizedBox(height: 12),
            Text(
              '小饭桌还没有菜',
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

/// 3D 图标徽（`assets/images/*.png`）。
///
/// 统一在这里吞掉「图缺失」的异常：`errorBuilder` 退到 Material 图标，
/// 布局不会因为少一张 PNG 而塌。缺图时**不**报红色错误框。
class _AssetBadge extends StatelessWidget {
  const _AssetBadge({
    required this.asset,
    required this.fallback,
    required this.size,
    required this.radius,
    required this.iconSize,
  });

  final String asset;
  final Widget fallback;
  final double size;
  final double radius;
  final double iconSize;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: const Color(0x9EFFD1DC), // SecondaryContainer @ 0.62
        borderRadius: BorderRadius.circular(radius),
      ),
      child: Image.asset(
        asset,
        width: iconSize,
        height: iconSize,
        filterQuality: FilterQuality.high,
        errorBuilder: (context, error, stackTrace) => fallback,
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  悬浮购物车条 —— demo `#floating-cart-bar`
//
//  demo 结构：
//    左：32 圆形渐变徽（shopping 3D 图 + 右上角计数徽）
//        + Column[ 总计: 🍬 N | 副标一行 ]
//    右：胶囊结算按钮（文案随角色变：去结算 / 管理 · 撒糖）
//  高度沿用原生 66.dp 常量（见文件头 `_kFloatingCartHeight` 说明）。
// ══════════════════════════════════════════════════════════════════════════════

class _CartFloatingBar extends StatelessWidget {
  const _CartFloatingBar({
    required this.basketKey,
    required this.count,
    required this.totalPrice,
    required this.checkoutLabel,
    required this.onCartClick,
    required this.onCheckoutClick,
  });

  final GlobalKey basketKey;
  final int count;
  final double totalPrice;
  final String checkoutLabel;
  final VoidCallback onCartClick;
  final VoidCallback onCheckoutClick;

  @override
  Widget build(BuildContext context) {
    // 整条是一块液态玻璃，参数直接复用全局配方 `kCozyGlassSettings`
    // （与底栏胶囊、玻璃弹层同一块玻璃：premium 档 + iOS 26 级中性白雾）。
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
      settings: kCozyGlassSettings,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onCartClick,
        child: Padding(
          padding: const EdgeInsets.only(left: 22, right: 8),
          child: Row(
            children: <Widget>[
              SizedBox(
                key: basketKey,
                width: 34,
                height: 34,
                child: Stack(
                  clipBehavior: Clip.none,
                  children: <Widget>[
                    Container(
                      width: 34,
                      height: 34,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        // demo：`bg-gradient-to-tr from-rose-400 to-cozy-primary`
                        gradient: const LinearGradient(
                          begin: Alignment.bottomLeft,
                          end: Alignment.topRight,
                          colors: <Color>[
                            CozyPalette.primaryContainer,
                            CozyPalette.primary,
                          ],
                        ),
                        boxShadow: CozyLight.cardShadow,
                      ),
                      child: Image.asset(
                        'assets/images/shopping.png',
                        width: 19,
                        height: 19,
                        filterQuality: FilterQuality.high,
                        semanticLabel: '购物篮',
                        errorBuilder: (context, error, stackTrace) =>
                            const Icon(
                          Icons.shopping_bag,
                          color: CozyPalette.surface,
                          size: 18,
                        ),
                      ),
                    ),
                    Positioned(
                      top: -3,
                      right: -3,
                      child: Container(
                        width: 17,
                        height: 17,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: _kCoinRose,
                          border: Border.all(color: Colors.white, width: 1),
                        ),
                        child: Text(
                          '$count',
                          style: const TextStyle(
                            fontSize: 9,
                            height: 1.1,
                            fontWeight: FontWeight.w900,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        const Text(
                          '总计:',
                          style: TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w900,
                            color: CozyPalette.onSurface,
                          ),
                        ),
                        const SizedBox(width: 3),
                        Image.asset(
                          'assets/images/candy.png',
                          width: 14,
                          height: 14,
                          filterQuality: FilterQuality.high,
                          errorBuilder: (context, error, stackTrace) =>
                              const SizedBox.shrink(),
                        ),
                        const SizedBox(width: 2),
                        Flexible(
                          child: Text(
                            _coinText(totalPrice),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 14,
                              height: 1.15,
                              fontWeight: FontWeight.w900,
                              color: _kCoinRose,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const Text(
                      '小店自取 · 做好就叫你',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 9.5,
                        color: CozyPalette.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              _SquishyCheckoutButton(
                text: checkoutLabel,
                onTap: onCheckoutClick,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// demo `#cart-submit-btn`：胶囊渐变（`from-cozy-primary to-rose-500`）+ 文案 + 箭头，
/// 按下缩到 0.95。
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
      scale: _pressed ? 0.95 : 1.0,
      duration: const Duration(milliseconds: 120),
      curve: Curves.easeOut,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: (_) => setState(() => _pressed = true),
        onTapUp: (_) => setState(() => _pressed = false),
        onTapCancel: () => setState(() => _pressed = false),
        onTap: widget.onTap,
        child: Container(
          height: 44,
          padding: const EdgeInsets.symmetric(horizontal: 18),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(999),
            gradient: const LinearGradient(
              colors: <Color>[CozyPalette.primary, _kCoinRose],
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text(
                widget.text,
                maxLines: 1,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w900,
                  color: CozyPalette.surface,
                ),
              ),
              const SizedBox(width: 4),
              const Icon(
                Icons.arrow_forward_rounded,
                size: 14,
                color: CozyPalette.surface,
              ),
            ],
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
    final syncRecipe = generateSmartCookingSteps(item.name);
    final ings = syncRecipe.ingredients;
    final steps = syncRecipe.steps;

    return SafeArea(
      top: false,
      child: Column(
        children: <Widget>[
          // 顶部拖动小把手
          Center(
            child: Container(
              width: 40,
              height: 4.5,
              margin: const EdgeInsets.only(top: 8, bottom: 12),
              decoration: BoxDecoration(
                color: CozyPalette.onSurface.withValues(alpha: 0.22),
                borderRadius: BorderRadius.circular(3),
              ),
            ),
          ),
          Expanded(
            child: SingleChildScrollView(
              physics: const BouncingScrollPhysics(),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  _DishImage(
                    imageUrl: item.imageUrl,
                    name: item.name,
                    backgroundAlpha: 1,
                    radius: 24,
                    height: 210,
                    fit: BoxFit.cover,
                    iconSize: 44,
                    placeholderGap: 8,
                  ),
                  const SizedBox(height: 14),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: <Widget>[
                      Expanded(
                        child: Text(
                          item.name,
                          style: text.headlineSmall!.copyWith(
                            fontWeight: FontWeight.w700,
                            color: CozyPalette.onSurface,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: CozyPalette.primary.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(
                            color: CozyPalette.primary.withValues(alpha: 0.3),
                            width: 0.8,
                          ),
                        ),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.restaurant_menu_rounded, size: 13, color: CozyPalette.primary),
                            SizedBox(width: 3),
                            Text(
                              '下厨房菜谱',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.w700,
                                color: CozyPalette.primary,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  if (showDescription) ...<Widget>[
                    const SizedBox(height: 6),
                    Text(
                      item.description.trim().isEmpty ? '今天也很适合吃这道美味家常菜 ✨' : item.description.trim(),
                      style: text.bodyMedium!.copyWith(
                        color: CozyPalette.onSurfaceVariant,
                        height: 1.35,
                      ),
                    ),
                  ],
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    children: <Widget>[
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: CozyPalette.primary.withValues(alpha: 0.10),
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: Text(
                          item.category.trim().isEmpty ? '经典家常' : item.category.trim(),
                          style: text.labelMedium!.copyWith(
                            fontWeight: FontWeight.w700,
                            color: CozyPalette.primary,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: CozyPalette.surfaceVariant.withValues(alpha: 0.7),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.timer_outlined, size: 13, color: CozyPalette.onSurfaceVariant),
                            SizedBox(width: 4),
                            Text(
                              '约 15-25 分钟',
                              style: TextStyle(
                                fontSize: 12,
                                color: CozyPalette.onSurfaceVariant,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: _kCoinRose.withValues(alpha: 0.10),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Image.asset(
                              'assets/images/candy.png',
                              width: 15,
                              height: 15,
                              filterQuality: FilterQuality.high,
                              errorBuilder: (context, error, stackTrace) => const Icon(
                                Icons.monetization_on_rounded,
                                size: 14,
                                color: _kCoinRose,
                              ),
                            ),
                            const SizedBox(width: 4),
                            Text(
                              '${_coinText(item.price)} 糖币',
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w700,
                                color: _kCoinRose,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  const Divider(height: 1, color: Color(0x18000000)),
                  const SizedBox(height: 14),

                  // ① 用料与食材清单
                  Row(
                    children: [
                      Container(
                        width: 3.5,
                        height: 15,
                        decoration: BoxDecoration(
                          color: CozyPalette.primary,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                      const SizedBox(width: 8),
                      const Text(
                        '用料与食材清单',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                          color: CozyPalette.onSurface,
                        ),
                      ),
                      const Spacer(),
                      Text(
                        '${ings.length} 项主辅料',
                        style: const TextStyle(
                          fontSize: 12,
                          color: CozyPalette.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final ing in ings)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: const Color(0x18000000),
                              width: 0.8,
                            ),
                          ),
                          child: Text(
                            ing,
                            style: const TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w600,
                              color: CozyPalette.onSurface,
                            ),
                          ),
                        ),
                    ],
                  ),

                  const SizedBox(height: 20),

                  // ② 烹饪制作过程
                  Row(
                    children: [
                      Container(
                        width: 3.5,
                        height: 15,
                        decoration: BoxDecoration(
                          color: CozyPalette.primary,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                      const SizedBox(width: 8),
                      const Text(
                        '烹饪制作过程',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                          color: CozyPalette.onSurface,
                        ),
                      ),
                      const Spacer(),
                      Text(
                        '共 ${steps.length} 个步骤',
                        style: const TextStyle(
                          fontSize: 12,
                          color: CozyPalette.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Column(
                    children: [
                      for (int i = 0; i < steps.length; i++)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: const Color(0x14000000),
                                width: 0.8,
                              ),
                            ),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Container(
                                  width: 22,
                                  height: 22,
                                  alignment: Alignment.center,
                                  decoration: const BoxDecoration(
                                    color: CozyPalette.primary,
                                    shape: BoxShape.circle,
                                  ),
                                  child: Text(
                                    '${i + 1}',
                                    style: const TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.w700,
                                      color: Colors.white,
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Text(
                                    steps[i],
                                    style: const TextStyle(
                                      fontSize: 13.5,
                                      height: 1.45,
                                      color: CozyPalette.onSurface,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 16),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: <Widget>[
              Expanded(
                child: OutlinedButton(
                  onPressed: onClose,
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 46),
                    side: const BorderSide(color: Color(0x28000000)),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ),
                  child: const Text('关闭'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                flex: 2,
                child: FilledButton(
                  onPressed: canOrder ? onAdd : null,
                  style: FilledButton.styleFrom(
                    backgroundColor: CozyPalette.primary,
                    foregroundColor: Colors.white,
                    disabledBackgroundColor: CozyPalette.outlineVariant.withValues(alpha: 0.35),
                    disabledForegroundColor: CozyPalette.onSurfaceVariant.withValues(alpha: 0.60),
                    minimumSize: const Size(0, 46),
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ),
                  child: Text(
                    canOrder ? '加入购物篮' : '吃货专属点单',
                    style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
