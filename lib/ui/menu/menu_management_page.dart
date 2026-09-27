import 'dart:async';

import 'package:flutter/material.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

// ══════════════════════════════════════════════════════════════════════════════
//  我的店铺 · 小店与菜单管理 (MenuManagementPage)
//  ──────────────────────────────────────────────────────────────────────────
//  布局唯一真相：
//    app/src/main/java/com/myorderapp/ui/menu/MenuManagementScreen.kt  (1592 行)
//  状态 / 动作语义：
//    app/src/main/java/com/myorderapp/ui/menu/MenuManagementViewModel.kt
//
//  原生页面内联色板（MenuManagementScreen.kt:101-110）已在设计系统里逐枚对齐：
//    PrimaryBlue     #894C5C → CozyPalette.primary
//    SuccessGreen    #F4A7B9 → CozyPalette.primaryContainer
//    SignatureOrange #F8A98E → CozyPalette.tertiaryContainer
//    TextPrimary     #1D1B18 → CozyPalette.onSurface
//    TextSecondary   #524346 → CozyPalette.onSurfaceVariant
//    PageBackground  #FFFFFF → CozyPalette.background
//    CardBackground  #FFFCF8 → CozyPalette.surface
//    HoverBackground #FFD1DC → CozyPalette.secondaryContainer
//    BorderColor     #D6C1C5 → CozyPalette.outlineVariant
//
//  顶层结构（MenuManagementScreen.kt:236-301）：
//    Box(纯白底)
//      Column
//        StoreTopBar()                      → CozyMainTopBar("我的店铺")
//        LazyColumn(h16/v10, bottom 128→CozyDock.clearance, spacedBy 12)
//          ShopSettingsStrip                :337
//          CategoryManagementBento          :476
//          DishManagementHeader             :642
//          MenuContent                      :723
//      FloatingAddDishButton(BottomEnd)      :679
//      AddDishSuccessToast(Center)           :318
// ══════════════════════════════════════════════════════════════════════════════

/// 原生 `DefaultShopAnnouncement`（MenuManagementViewModel.kt:21）
const String _defaultShopAnnouncement = '欢迎来到我们的温馨小店！今天有新鲜出炉的心形披萨哦~ 🐾';

/// 原生 `R.drawable.shop_banner_stitch`（MenuManagementScreen.kt:366、:1026）
const String _shopBannerAsset = 'assets/images/shop_banner_stitch.png';

/// 原生 `SHOP_MENU_REFRESH_INTERVAL_MS`（MenuManagementScreen.kt:111）
const Duration _refreshInterval = Duration(seconds: 10);

/// 原生屏幕内联的 `sp` 字号没有对应的 `Type.kt` 条目，
/// 这里以设计系统 `textTheme` 的条目作基准样式、只覆写字号 / 行高 / 字重：
/// 既拿到 1:1 的字号，又不需要逐处写 `fontFamily`。
TextStyle _fit(TextStyle? base, double size, double lineHeight, [FontWeight? weight]) =>
    base!.copyWith(fontSize: size, height: lineHeight / size, fontWeight: weight);

/// 原生 `MenuFilter`（MenuManagementViewModel.kt:23）
enum _MenuFilter { all, available, unavailable }

/// 原生 `MenuSortMode`（MenuManagementViewModel.kt:29）
enum _MenuSortMode { priceAsc, newest }

/// 原生 `DishEditorState`（MenuManagementViewModel.kt:34）
class _DishDraft {
  _DishDraft({
    this.id,
    this.name = '',
    this.price = '',
    this.originPrice = '',
    this.imageUrl = '',
    this.category = '',
    this.description = '',
    this.stock = '',
    this.isAvailable = true,
  });

  String? id;
  String name;
  String price;
  String originPrice;
  String imageUrl;
  String category;
  String description;
  String stock;
  bool isAvailable;

  _DishDraft clone() => _DishDraft(
        id: id,
        name: name,
        price: price,
        originPrice: originPrice,
        imageUrl: imageUrl,
        category: category,
        description: description,
        stock: stock,
        isAvailable: isAvailable,
      );
}

class MenuManagementPage extends StatefulWidget {
  const MenuManagementPage({super.key});

  @override
  State<MenuManagementPage> createState() => _MenuManagementPageState();
}

class _MenuManagementPageState extends State<MenuManagementPage> {
  // ── 原生 ViewModel 的 uiState（MenuManagementViewModel.kt:46-68） ──
  _MenuFilter _selectedFilter = _MenuFilter.all;
  final _MenuSortMode _sortMode = _MenuSortMode.newest;
  _DishDraft _editor = _DishDraft();

  /// 原生 `categories` 来自 `SingleShopRepository.getCategoryNames()`，
  /// 由 `menu_dishes.category` 聚合而来。
  /// 【真机修正】`MenuItem` 现在带 `category` 字段了，页面不再用会话内 map 顶替；
  /// 这里只保留「本地新建、但还没有菜品挂靠」的分类，与云端聚合结果合并展示。
  List<String> _categories = <String>[];
  String _selectedCategory = '';

  /// 原生 `isBatchMode` / `selectedDishIds`（本页没有入口切换，与原生一致，
  /// 保留结构以便 Lead 后续接批量操作）。
  final bool _isBatchMode = false;
  final Set<String> _selectedDishIds = <String>{};

  /// 原生 `isLoadingCategory`：`selectCategory()` 会置 true 并延迟 1500ms，
  /// 但本页（与原生一致）没有任何入口调用它，所以恒为 false。
  final bool _isLoadingCategory = false;

  /// 原生 ShopSettingsDialog 的草稿（shopNameDraft / shopAnnouncementDraft）
  final TextEditingController _shopNameCtrl = TextEditingController();
  final TextEditingController _shopAnnouncementCtrl = TextEditingController();

  /// 原生 `uiState.message == "已新增菜品"` 触发的居中提示（:298）
  bool _showAddSuccess = false;
  Timer? _refreshTimer;
  Timer? _successTimer;

  /// 【真机修正】悬浮「新增菜品」胶囊（bottom 20 / 高 50，见本文件 :498）是浮在列表之上的，
  /// 滚动到中途时它正好压住某张菜品卡右侧的上架开关 / ⋯ / 删除（原生同样如此，
  /// 但真机上「想切开关却按到新增菜品」的误触很实在）。
  /// 这里不搬动胶囊（保持与原生一致的位置与随时可点），改成滚动时自动让位：
  /// 向下滚隐藏、向上滚或到达列表两端时重新出现。
  final ScrollController _listCtrl = ScrollController();
  bool _pillVisible = true;
  double _lastListOffset = 0;

  @override
  void initState() {
    super.initState();
    // 原生 `LaunchedEffect(viewModel)` 的 10s 轮询（MenuManagementScreen.kt:131）
    _refreshTimer = Timer.periodic(_refreshInterval, (_) => _refreshShopAndMenu());
    _listCtrl.addListener(_onListScroll);
  }

  /// 列表滚动时决定悬浮胶囊是否可见（见 `_pillVisible` 注释）。
  void _onListScroll() {
    if (!_listCtrl.hasClients) return;
    final double offset = _listCtrl.offset;
    final double delta = offset - _lastListOffset;
    _lastListOffset = offset;
    final double tail = _listCtrl.position.maxScrollExtent;
    // 到顶部 / 到底部时始终显示：这两处不会遮住任何行的操作列。
    final bool atEdge = offset <= 2 || offset >= tail - 2;
    if (atEdge) {
      if (!_pillVisible) setState(() => _pillVisible = true);
      return;
    }
    if (delta > 2 && _pillVisible) {
      setState(() => _pillVisible = false);
    } else if (delta < -2 && !_pillVisible) {
      setState(() => _pillVisible = true);
    }
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    _successTimer?.cancel();
    _listCtrl.dispose();
    _shopNameCtrl.dispose();
    _shopAnnouncementCtrl.dispose();
    super.dispose();
  }

  // ────────────────────────── 动作（对照 ViewModel） ──────────────────────────

  /// 原生 `refreshShopAndMenuFromCloud()`（MenuManagementViewModel.kt:110）
  Future<void> _refreshShopAndMenu() async {
    await AppState.instance.loadMe();
    await AppState.instance.refreshMenu(silent: true);
  }

  /// 原生 `withVisibleDishes()` 的筛选 + 排序部分（MenuManagementViewModel.kt:524）。
  /// 注意原生也不按 `selectedCategory` 过滤列表，只用 filter / searchQuery。
  /// 【数据缺口】MenuDishEntity.updatedAt 在 Flutter 侧不存在，`Newest` 只保留
  /// 「已下架沉底」这一半语义（服务端已按 sort_order 返回）。
  List<MenuItem> _visibleDishes(List<MenuItem> dishes) {
    final List<MenuItem> filtered = dishes
        .where((MenuItem d) => switch (_selectedFilter) {
              _MenuFilter.all => true,
              _MenuFilter.available => d.isAvailable,
              _MenuFilter.unavailable => !d.isAvailable,
            })
        .toList();
    if (_sortMode == _MenuSortMode.priceAsc) {
      filtered.sort((MenuItem a, MenuItem b) => a.price.compareTo(b.price));
    } else {
      filtered.sort((MenuItem a, MenuItem b) {
        if (a.isAvailable == b.isAvailable) return 0;
        return a.isAvailable ? -1 : 1;
      });
    }
    return filtered;
  }

  /// 原生 `visibleDishes` 直接读 `dish.category`（MenuItem.category）。
  /// 空分类兜底成「未分类」，与编辑器默认值一致。
  String _categoryOf(MenuItem dish) {
    final String c = dish.category.trim();
    return c.isEmpty ? '未分类' : c;
  }

  /// 「分类管理」展示的清单 = 云端菜品聚合出的分类 ∪ 本次会话新建的分类。
  /// 原生 `getCategoryNames()` 正是从 `menu_dishes.category` 聚合的；此前 Flutter
  /// 只用了会话内 map，导致冷启动后分类区恒为空（只有「新增分类」虚线卡）。
  List<String> _allCategories(List<MenuItem> dishes) {
    final Set<String> merged = <String>{};
    for (final MenuItem dish in dishes) {
      final String c = dish.category.trim();
      if (c.isNotEmpty) merged.add(c);
    }
    merged.addAll(_categories.where((String c) => c.trim().isNotEmpty));
    final List<String> list = merged.toList()..sort();
    return list;
  }

  /// 原生 `newDish()`（MenuManagementViewModel.kt:304）
  void _newDish() {
    final List<String> known = _allCategories(AppState.instance.menu);
    final String category = _selectedCategory.isNotEmpty
        ? _selectedCategory
        : (known.isNotEmpty ? known.first : '未分类');
    _editor = _DishDraft(category: category);
    _openDishEditorSheet();
  }

  /// 原生 `editDish()`（MenuManagementViewModel.kt:318）
  void _editDish(MenuItem dish) {
    _editor = _DishDraft(
      id: dish.id,
      name: dish.name,
      price: dish.price.toString(),
      originPrice: '',
      imageUrl: dish.imageUrl.length <= 4 ? '' : dish.imageUrl,
      category: _categoryOf(dish),
      description: dish.description,
      stock: '',
      isAvailable: dish.isAvailable,
    );
    _openDishEditorSheet();
  }

  /// 原生 Screen 里的 `DishEditorDialog`（MenuManagementScreen.kt:161）+ ModalBottomSheet
  void _openDishEditorSheet() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      // 【玻璃】底色交给 `CozyGlassSheet`（半径 30 与原 shape 一致），这里必须透明。
      backgroundColor: Colors.transparent,
      // 遮罩调浅：背后太黑 -> 玻璃没有东西可折，只会变灰雾（原值 0.32）。
      barrierColor: Colors.black.withValues(alpha: 0.18),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
      ),
      builder: (BuildContext _) => CozyGlassSheet(
        radius: 30,
        // 编辑弹层自己画了把手（56×6，比原生默认 32×4 宽），别再画一条。
        showHandle: false,
        child: _DishEditorSheet(
          initial: _editor.clone(),
          // 分类候选 = 云端菜品聚合 ∪ 会话内新建，否则编辑已有菜时
          // 看不到云端已有的分类（原生 getCategoryNames() 就是这个聚合）。
          categories: _allCategories(AppState.instance.menu),
          onSave: _saveDish,
          onPickImage: () => _openImageSourcePicker('选择菜品图片'),
        ),
      ),
    );
  }

  /// 原生 `ImageSourcePickerDialog(visible = showDishImageSourcePicker)`（:145）
  void _openImageSourcePicker(String title) {
    showImageSourcePickerDialog(context, title: title);
  }

  /// 原生 `resetShopSettingsDrafts()`（MenuManagementViewModel.kt:142）+ 打开弹层
  void _openShopSettings() {
    final Shop? shop = AppState.instance.shop;
    _shopNameCtrl.text = shop?.name ?? '';
    _shopAnnouncementCtrl.text = shop?.announcement ?? _defaultShopAnnouncement;
    showShopSettingsDialog(
      context,
      shopNameCtrl: _shopNameCtrl,
      announcementCtrl: _shopAnnouncementCtrl,
      coverUrl: shop?.coverUrl ?? '',
      onPickImage: () => _openImageSourcePicker('选择店铺封面'),
      onSave: () => _saveShopSettings(),
    );
  }

  /// 原生 `saveShopSettings()`（MenuManagementViewModel.kt:152）
  Future<void> _saveShopSettings() async {
    final String name =
        _shopNameCtrl.text.trim().isEmpty ? '我的小店' : _shopNameCtrl.text.trim();
    final String announcement = _shopAnnouncementCtrl.text.trim().isEmpty
        ? _defaultShopAnnouncement
        : _shopAnnouncementCtrl.text.trim();
    await AppState.instance.updateShopInfo(name: name, announcement: announcement);
  }

  /// 原生 `createCategory()`（MenuManagementViewModel.kt:243）
  void _createCategory(String name) {
    final String normalized = name.trim();
    if (normalized.isEmpty) return;
    setState(() {
      _categories = <String>{..._categories, normalized}.toList();
      _selectedCategory = normalized;
    });
  }

  /// 原生 `renameCategory()`（MenuManagementViewModel.kt:260）
  Future<void> _renameCategory(String oldName, String newName) async {
    final String oldCategory = oldName.trim();
    final String newCategory = newName.trim();
    if (oldCategory.isEmpty || newCategory.isEmpty) return;
    if (oldCategory == newCategory) return;
    setState(() {
      _categories = _categories
          .map((String c) => c == oldCategory ? newCategory : c)
          .toSet()
          .toList();
      if (_selectedCategory == oldCategory) _selectedCategory = newCategory;
    });
    // 【真机修正】分类名是菜品 `category` 列的聚合，改名必须落到菜品本身，
    // 否则刷新后旧分类名会从菜品那边再长回来（原生走 menuRepository.renameCategory）。
    await AppState.instance.renameDishCategory(oldCategory, newCategory);
  }

  /// 原生 `deleteCategory()`（MenuManagementViewModel.kt:284）
  Future<void> _deleteCategory(String category) async {
    final String target = category.trim();
    if (target.isEmpty) return;
    final List<String> next = _categories.where((String c) => c != target).toList();
    final String fallback = next.isNotEmpty ? next.first : '未分类';
    final List<String> saved = next.isNotEmpty ? next : <String>[fallback];
    // 原生先 take ids 再 moveToCategory，这里同样要在刷新把菜品列表换掉之前取。
    final List<String> affected = AppState.instance.menu
        .where((MenuItem d) => d.category.trim() == target)
        .map((MenuItem d) => d.id)
        .toList();
    setState(() {
      _categories = saved;
      if (_selectedCategory == target) _selectedCategory = fallback;
    });
    await AppState.instance.moveDishesToCategory(affected, fallback);
  }

  /// 原生 `saveDishInternal()`（MenuManagementViewModel.kt:388）。
  /// 返回 null 表示保存成功（编辑器弹层可以关闭），否则是校验 / 失败提示文案。
  Future<String?> _saveDish(_DishDraft draft,
      {required bool createMissingCategory}) async {
    final String enteredCategory = draft.category.trim();
    final String? existingCategory =
        _firstWhereIgnoreCase(_categories, enteredCategory);
    final String normalizedCategory = existingCategory ?? enteredCategory;
    final double? price = double.tryParse(draft.price);
    if (draft.name.trim().isEmpty ||
        price == null ||
        price <= 0.0 ||
        normalizedCategory.isEmpty) {
      return '请填写菜名、有效价格和分类';
    }
    if (existingCategory == null && !createMissingCategory) {
      return '请确认是否创建新分类';
    }
    if (existingCategory == null) {
      setState(() {
        _categories = <String>{..._categories, normalizedCategory}.toList();
        _selectedCategory = normalizedCategory;
      });
    }
    if (draft.id == null) {
      // 原生 `menuRepository.saveDish(MenuDishDraft(...))`
      // category 必须原样传给数据层：早先 AppState.addDish 没有这个参数，
      // 用户选的分类会被静默丢弃（服务端写死 '其他'）。
      final bool ok = await AppState.instance.addDish(
        name: draft.name.trim(),
        price: price,
        description: draft.description.trim(),
        emoji: '🍽️',
        category: normalizedCategory,
      );
      if (!ok) return AppState.instance.error ?? '上架失败';
      if (!mounted) return null;
      setState(() {
        _editor = _DishDraft(category: normalizedCategory);
      });
      _showSuccessToast();
      return null;
    }
    // 【数据缺口】AppState 还没有 updateDish（见交付说明）。这里如实回报缺口，
    // 不用 delete + add 之类的破坏性替代。
    return '当前数据层还不支持修改已有菜品（需要 AppState.updateDish）';
  }

  /// 原生 `AddDishSuccessToast` + `LaunchedEffect(uiState.message)` 的 1200ms 自动消失
  void _showSuccessToast() {
    _successTimer?.cancel();
    setState(() => _showAddSuccess = true);
    _successTimer = Timer(const Duration(milliseconds: 1200), () {
      if (mounted) setState(() => _showAddSuccess = false);
    });
  }

  /// 原生 `deleteDish()`（MenuManagementViewModel.kt:458）
  Future<void> _deleteDish(MenuItem dish) async {
    await AppState.instance.deleteDish(dish.id);
    if (!mounted) return;
    setState(() => _selectedDishIds.remove(dish.id));
  }

  /// 原生 Screen 里的 `dishDeleteTarget` + `DeleteDishDialog`（:303）
  Future<void> _confirmDeleteDish(MenuItem dish) async {
    final bool? ok = await showDeleteDishDialog(
      context,
      title: '删除菜品？',
      body: '删除后会从我的店铺移除「${dish.name}」，购物车里对应菜品也会失效。这个操作不能撤回。',
      confirmText: '删除菜品',
    );
    if (ok == true) await _deleteDish(dish);
  }

  /// 原生 Screen 里的 `categoryDeleteTarget` + `DeleteDishDialog`（:202）
  Future<void> _confirmDeleteCategory(String category) async {
    final bool? ok = await showDeleteDishDialog(
      context,
      title: '删除分类？',
      body: '该分类下的菜品会移动到其他分类，不会删除菜品。',
      confirmText: '删除分类',
    );
    if (ok == true) _deleteCategory(category);
  }

  /// 原生 `toggleDishAvailability()`（MenuManagementViewModel.kt:471）
  Future<void> _toggleDishAvailability(MenuItem dish) async {
    // 【数据缺口】AppState 还没有 setAvailability（见交付说明），无操作。
    // ignore: avoid_print
    assert(() {
      debugPrint('【缺口】切换上下架需要 AppState.setDishAvailability(${dish.id})');
      return true;
    }());
  }

  // ─────────────────────────────── 界面 ───────────────────────────────

  @override
  Widget build(BuildContext context) {
    final AppState state = AppState.instance;
    return Scaffold(
      backgroundColor: CozyPalette.background,
      body: CozyPage(
        child: ListenableBuilder(
          listenable: state,
          builder: (BuildContext context, Widget? _) {
            final List<MenuItem> dishes = state.menu;
            final List<MenuItem> visible = _visibleDishes(dishes);
            // 分类清单合并云端聚合，冷启动后也能列出已有分类。
            final List<String> categories = _allCategories(dishes);
            final Map<String, int> dishCountByCategory = <String, int>{};
            for (final MenuItem dish in dishes) {
              final String category = _categoryOf(dish);
              dishCountByCategory[category] =
                  (dishCountByCategory[category] ?? 0) + 1;
            }
            return Stack(
              children: <Widget>[
                // 玻璃顶栏是浮层：列表从 y=0 起滚、内容穿过玻璃（顶部预留
                // `CozyGlassTopBar.reserved`），这样玻璃才有东西可折。
                Positioned.fill(
                  child: ListView(
                    controller: _listCtrl,
                    padding: EdgeInsets.fromLTRB(
                      16,
                      10 + CozyGlassTopBar.reserved,
                      16,
                      CozyDock.clearanceOf(context),
                    ),
                        children: <Widget>[
                          _ShopSettingsStrip(
                            shopName: state.shop?.name ?? '',
                            shopImageUrl: state.shop?.coverUrl ?? '',
                            announcement: state.shop?.announcement ?? '',
                            onEdit: _openShopSettings,
                          ),
                          const SizedBox(height: 12),
                          _CategoryManagementBento(
                            categories: categories,
                            dishCountByCategory: dishCountByCategory,
                            onManageCategoriesClick: _openCategoryManager,
                            onCreateCategoryClick: _openNewCategoryDialog,
                          ),
                          const SizedBox(height: 12),
                          _DishManagementHeader(
                            selectedFilter: _selectedFilter,
                            onFilterSelected: (_MenuFilter filter) =>
                                setState(() => _selectedFilter = filter),
                          ),
                          const SizedBox(height: 12),
                          _MenuContent(
                            isLoadingCategory: _isLoadingCategory,
                            visibleDishes: visible,
                            isBatchMode: _isBatchMode,
                            selectedDishIds: _selectedDishIds,
                            categoryOf: _categoryOf,
                            onToggleSelection: (MenuItem dish) => setState(() {
                              if (!_selectedDishIds.remove(dish.id)) {
                                _selectedDishIds.add(dish.id);
                              }
                            }),
                            onToggleAvailability: _toggleDishAvailability,
                            onEdit: _editDish,
                            onDelete: _confirmDeleteDish,
                            onShowAddSheet: _newDish,
                          ),
                        ],
                      ),
                    ),
                Positioned(
                  left: 0,
                  right: 0,
                  top: 0,
                  child: const _StoreTopBar(),
                ),
                Positioned(
                  right: 20,
                  bottom: 20 + MediaQuery.paddingOf(context).bottom,
                  // 向下滚动时让位（见 `_pillVisible`），避免压住菜品行的操作列。
                  child: IgnorePointer(
                    ignoring: !_pillVisible,
                    child: AnimatedSlide(
                      offset: _pillVisible ? Offset.zero : const Offset(0, 1.6),
                      duration: const Duration(milliseconds: 180),
                      curve: Curves.easeOut,
                      child: AnimatedOpacity(
                        opacity: _pillVisible ? 1 : 0,
                        duration: const Duration(milliseconds: 180),
                        child: _FloatingAddDishButton(onTap: _newDish),
                      ),
                    ),
                  ),
                ),
                if (_showAddSuccess)
                  const Positioned.fill(
                    child: Center(child: _AddDishSuccessToast()),
                  ),
              ],
            );
          },
        ),
      ),
    );
  }

  /// 原生 `showCategoryDialog`（:181）
  void _openNewCategoryDialog() {
    showNewCategoryDialog(context, onSave: _createCategory);
  }

  /// 原生 `showCategoryManagerDialog`（:191）
  void _openCategoryManager() {
    showCategoryManagerDialog(
      context,
      // 同上：管理器必须列出云端已有分类，改名/删除才有对象。
      categories: _allCategories(AppState.instance.menu),
      selectedCategory: _selectedCategory,
      onCreate: _createCategory,
      onRename: _renameCategory,
      onDelete: _confirmDeleteCategory,
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  StoreTopBar（MenuManagementScreen.kt:441）
//  原生 `CozyMainTopBar(title = "我的店铺")` —— 后面那段 Row 在 return 之后，
//  是死代码，这里只还原真正生效的那一行（StitchNativeComponents.kt:176）。
// ══════════════════════════════════════════════════════════════════════════════
class _StoreTopBar extends StatelessWidget {
  const _StoreTopBar();

  @override
  Widget build(BuildContext context) {
    return CozyGlassTopBar(
      title: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4),
        child: Text(
          '我的店铺',
          textAlign: TextAlign.center,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: _fit(Theme.of(context).textTheme.headlineSmall, 20, 24,
                  FontWeight.w900)
              .copyWith(
            color: CozyPalette.primary,
            letterSpacing: 0,
          ),
        ),
      ),
      leading: SizedBox(
        width: 44,
        child: Align(
          alignment: Alignment.centerLeft,
          child: Icon(
            Icons.favorite,
            size: 26,
            color: CozyPalette.primary.withValues(alpha: 0.82),
          ),
        ),
      ),
      trailing: const SizedBox(
        width: 44,
        child: Align(
          alignment: Alignment.centerRight,
          child: Icon(
            Icons.notifications,
            size: 24,
            color: CozyPalette.onSurfaceVariant,
          ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ShopSettingsStrip（MenuManagementScreen.kt:336）
// ══════════════════════════════════════════════════════════════════════════════
class _ShopSettingsStrip extends StatelessWidget {
  const _ShopSettingsStrip({
    required this.shopName,
    required this.shopImageUrl,
    required this.announcement,
    required this.onEdit,
  });

  final String shopName;
  final String shopImageUrl;
  final String announcement;
  final VoidCallback onEdit;

  @override
  Widget build(BuildContext context) {
    return CozyCard(
      radius: 20,
      margin: const EdgeInsets.only(bottom: 10),
      padding: EdgeInsets.zero,
      child: Column(
        children: <Widget>[
          // 封面 154dp（AsyncImage + 兜底 painterResource）
          SizedBox(
            width: double.infinity,
            height: 154,
            child: shopImageUrl.isNotEmpty
                ? Image.network(
                    shopImageUrl,
                    fit: BoxFit.cover,
                    errorBuilder: (_, _, _) =>
                        Image.asset(_shopBannerAsset, fit: BoxFit.cover),
                  )
                : Image.asset(_shopBannerAsset, fit: BoxFit.cover),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Column(
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Expanded(
                      child: Text(
                        shopName.isEmpty ? '我的小店' : shopName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: _fit(Theme.of(context).textTheme.headlineMedium,
                            21, 27, FontWeight.bold),
                      ),
                    ),
                    const SizedBox(width: 12),
                    // 原生这里是自绘 Surface（不是 CozyPill），按原生色值还原
                    _PressScale(
                      onTap: onEdit,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 12, vertical: 8),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(999),
                          color: CozyPalette.primary.withValues(alpha: 0.10),
                          border: Border.all(
                            color: CozyPalette.primary.withValues(alpha: 0.22),
                          ),
                        ),
                        child: Row(
                          children: <Widget>[
                            const Icon(
                              Icons.edit_outlined,
                              size: 17,
                              color: CozyPalette.primary,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              '编辑店铺',
                              style: _fit(Theme.of(context).textTheme.labelLarge,
                                      13, 18, FontWeight.bold)
                                  .copyWith(color: CozyPalette.primary),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                // 公告条（原生 #FFF0F4，取设计系统最接近的软粉令牌）
                Container(
                  width: double.infinity,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    color: CozyTheme.softPink,
                    border: Border.all(
                      color: CozyPalette.primary.withValues(alpha: 0.12),
                    ),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      const Padding(
                        padding: EdgeInsets.only(top: 1),
                        child: Icon(
                          Icons.campaign,
                          size: 18,
                          color: CozyPalette.primary,
                        ),
                      ),
                      const SizedBox(width: 9),
                      Expanded(
                        child: Text(
                          announcement.isEmpty ? '欢迎光临' : announcement,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style:
                              _fit(Theme.of(context).textTheme.bodyMedium, 14, 20)
                                  .copyWith(
                            color: CozyPalette.onSurfaceVariant,
                          ),
                        ),
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

// ══════════════════════════════════════════════════════════════════════════════
//  CategoryManagementBento（MenuManagementScreen.kt:475）
// ══════════════════════════════════════════════════════════════════════════════
class _CategoryManagementBento extends StatelessWidget {
  const _CategoryManagementBento({
    required this.categories,
    required this.dishCountByCategory,
    required this.onManageCategoriesClick,
    required this.onCreateCategoryClick,
  });

  final List<String> categories;
  final Map<String, int> dishCountByCategory;
  final VoidCallback onManageCategoriesClick;
  final VoidCallback onCreateCategoryClick;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Row(
          children: <Widget>[
            Expanded(
              child: Text(
                '分类管理',
                style: _fit(Theme.of(context).textTheme.headlineMedium, 24, 32,
                    FontWeight.bold),
              ),
            ),
            // 原生 M3 TextButton（默认 contentPadding h12 / v8）
            TextButton(
              onPressed: onManageCategoriesClick,
              style: TextButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              child: Row(
                children: <Widget>[
                  Text(
                    '管理全部分类',
                    style: _fit(Theme.of(context).textTheme.labelLarge, 14, 18,
                            FontWeight.bold)
                        .copyWith(color: CozyPalette.primary),
                  ),
                  const SizedBox(width: 4),
                  Text(
                    '›',
                    style: _fit(Theme.of(context).textTheme.headlineMedium, 22,
                            28, FontWeight.bold)
                        .copyWith(color: CozyPalette.primary),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.only(right: 4),
          child: Row(
            children: <Widget>[
              for (final String category in categories) ...<Widget>[
                _CategoryBentoCard(
                  isCreate: false,
                  category: category,
                  dishCount: dishCountByCategory[category] ?? 0,
                ),
                const SizedBox(width: 12),
              ],
              _CategoryBentoCard(
                isCreate: true,
                category: '',
                dishCount: 0,
                onTap: onCreateCategoryClick,
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// 原生 `CategoryBentoCard`（MenuManagementScreen.kt:525）
///
/// 这两个卡片是自绘 Surface（新增卡是 1.5dp 虚线描边、分类卡是 2dp 主色描边），
/// `CozyCard` 只能画固定 1px 实线发丝边，所以这里按原生自绘 + 设计系统的卡片投影。
class _CategoryBentoCard extends StatelessWidget {
  const _CategoryBentoCard({
    required this.isCreate,
    required this.category,
    required this.dishCount,
    this.onTap,
  });

  final bool isCreate;
  final String category;
  final int dishCount;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final Widget card = Container(
      width: 154,
      height: 152,
      decoration: BoxDecoration(
        color: CozyPalette.surface,
        borderRadius: BorderRadius.circular(14),
        boxShadow: CozyLight.cardShadow,
      ),
      child: CustomPaint(
        foregroundPainter: isCreate
            ? _DashedBorderPainter(
                color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
                radius: 14,
              )
            : _SolidBorderPainter(
                color: CozyPalette.primary.withValues(alpha: 0.74),
                radius: 14,
                width: 2,
              ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Container(
                width: 48,
                height: 48,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: isCreate
                      ? CozyPalette.surface
                      : _categoryAccent(category).withValues(alpha: 0.72),
                  border: isCreate
                      ? Border.all(color: CozyPalette.primary, width: 2)
                      : null,
                ),
                child: Icon(
                  isCreate ? Icons.add : _categoryIcon(category),
                  size: isCreate ? 26 : 23,
                  color: CozyPalette.primary,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                isCreate ? '新增分类' : category,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: _fit(Theme.of(context).textTheme.titleLarge, 15, 20,
                        FontWeight.bold)
                    .copyWith(color: CozyPalette.primary),
              ),
              if (!isCreate) ...<Widget>[
                const SizedBox(height: 3),
                Text(
                  '$dishCount款菜品',
                  style: _fit(Theme.of(context).textTheme.bodySmall, 12, 18)
                      .copyWith(color: CozyPalette.onSurfaceVariant),
                ),
              ],
            ],
          ),
        ),
      ),
    );
    if (onTap == null) return card;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: card,
    );
  }
}

/// 原生 `Modifier.dashedCategoryBorder`（MenuManagementScreen.kt:569）：
/// 1.5dp、dash [10dp, 7dp]、圆角 14。
class _DashedBorderPainter extends CustomPainter {
  const _DashedBorderPainter({required this.color, required this.radius});

  final Color color;
  final double radius;

  @override
  void paint(Canvas canvas, Size size) {
    const double strokeWidth = 1.5;
    const double inset = strokeWidth / 2;
    final Paint paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;
    final Path path = Path()
      ..addRRect(RRect.fromRectAndRadius(
        Rect.fromLTWH(
            inset, inset, size.width - strokeWidth, size.height - strokeWidth),
        Radius.circular(radius),
      ));
    for (final metric in path.computeMetrics()) {
      double distance = 0;
      while (distance < metric.length) {
        final double next = distance + 10;
        final double end = next < metric.length ? next : metric.length;
        canvas.drawPath(metric.extractPath(distance, end), paint);
        distance = next + 7;
      }
    }
  }

  @override
  bool shouldRepaint(_DashedBorderPainter oldDelegate) =>
      oldDelegate.color != color || oldDelegate.radius != radius;
}

/// 原生 `Surface(border = BorderStroke(width, color))` 的实线描边（可指定宽度）
class _SolidBorderPainter extends CustomPainter {
  const _SolidBorderPainter({
    required this.color,
    required this.radius,
    required this.width,
  });

  final Color color;
  final double radius;
  final double width;

  @override
  void paint(Canvas canvas, Size size) {
    final double inset = width / 2;
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(inset, inset, size.width - width, size.height - width),
        Radius.circular(radius),
      ),
      Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = width,
    );
  }

  @override
  bool shouldRepaint(_SolidBorderPainter oldDelegate) =>
      oldDelegate.color != color ||
      oldDelegate.radius != radius ||
      oldDelegate.width != width;
}

/// 原生 `categoryIcon()`（MenuManagementScreen.kt:627）只认三档：
/// 披萨/主食、蛋糕/甜、饮/咖啡/茶，其余一律落到 `restaurant_outlined`，
/// 所以「招牌必吃」「暖心硬菜」这类常见分类在原生里也是同一把叉勺。
/// 【审查修正】这里把关键词铺细，未命中的再按名字哈希取一枚，避免整排卡片同图。
IconData _categoryIcon(String category) {
  final String c = category.trim();
  if (c.contains('披萨') || c.contains('主食')) {
    return Icons.local_pizza_outlined;
  }
  if (c.contains('面包') || c.contains('烘焙')) {
    return Icons.bakery_dining_outlined;
  }
  if (c.contains('蛋糕') || c.contains('甜') || c.contains('点')) {
    return Icons.cake_outlined;
  }
  if (c.contains('饮') || c.contains('咖啡') || c.contains('茶') || c.contains('酒')) {
    return Icons.local_cafe_outlined;
  }
  if (c.contains('面') || c.contains('粉')) {
    return Icons.ramen_dining_outlined;
  }
  if (c.contains('饭') || c.contains('中餐') || c.contains('家常')) {
    return Icons.rice_bowl_outlined;
  }
  if (c.contains('汤') || c.contains('煲') || c.contains('炖') || c.contains('粥')) {
    return Icons.soup_kitchen_outlined;
  }
  if (c.contains('火锅')) {
    return Icons.local_fire_department_outlined;
  }
  if (c.contains('烧烤') || c.contains('烤') || c.contains('炸')) {
    return Icons.outdoor_grill_outlined;
  }
  if (c.contains('海鲜') || c.contains('鱼') || c.contains('虾') || c.contains('蟹')) {
    return Icons.set_meal_outlined;
  }
  if (c.contains('西餐') || c.contains('牛排') || c.contains('意')) {
    return Icons.lunch_dining_outlined;
  }
  if (c.contains('素') || c.contains('凉') || c.contains('沙拉')) {
    return Icons.eco_outlined;
  }
  if (c.contains('冰') || c.contains('雪糕')) {
    return Icons.icecream_outlined;
  }
  if (c.contains('招牌') || c.contains('推荐') || c.contains('必吃')) {
    return Icons.local_fire_department_outlined;
  }
  if (c.contains('硬菜') || c.contains('暖')) {
    return Icons.soup_kitchen_outlined;
  }
  return _kFoodIcons[_categorySeed(c) % _kFoodIcons.length];
}

/// 关键词都没命中时的备选图标（按名字哈希取一枚，同一名字永远同一枚）。
const List<IconData> _kFoodIcons = <IconData>[
  Icons.restaurant_outlined,
  Icons.dinner_dining_outlined,
  Icons.ramen_dining_outlined,
  Icons.rice_bowl_outlined,
  Icons.soup_kitchen_outlined,
  Icons.local_pizza_outlined,
  Icons.set_meal_outlined,
  Icons.bakery_dining_outlined,
  Icons.icecream_outlined,
  Icons.local_cafe_outlined,
];

/// 分类名的稳定哈希（不随列表顺序变，同一分类每次渲染都同图同色）。
int _categorySeed(String category) {
  int seed = 7;
  for (final int unit in category.trim().codeUnits) {
    seed = (seed * 31 + unit) % 1000003;
  }
  return seed;
}

/// 原生 `categoryAccent()`（MenuManagementScreen.kt:634）同样只有三档。
/// 【审查修正】这里跟着上面选出来的图标走：同类同色，未命中的按同一个哈希取色。
Color _categoryAccent(String category) {
  final IconData icon = _categoryIcon(category);
  if (icon == Icons.cake_outlined ||
      icon == Icons.icecream_outlined ||
      icon == Icons.bakery_dining_outlined ||
      icon == Icons.local_cafe_outlined ||
      icon == Icons.eco_outlined) {
    return CozyPalette.secondaryContainer; // #FFD1DC
  }
  if (icon == Icons.soup_kitchen_outlined ||
      icon == Icons.outdoor_grill_outlined ||
      icon == Icons.set_meal_outlined ||
      icon == Icons.lunch_dining_outlined) {
    return CozyPalette.tertiaryContainer; // #F8A98E
  }
  if (_kFoodIcons.contains(icon)) {
    const List<Color> tints = <Color>[
      CozyPalette.primaryContainer,
      CozyPalette.secondaryContainer,
      CozyPalette.tertiaryContainer,
    ];
    return tints[_categorySeed(category) % tints.length];
  }
  return CozyPalette.primaryContainer; // #F4A7B9
}

// ══════════════════════════════════════════════════════════════════════════════
//  DishManagementHeader（MenuManagementScreen.kt:641）
// ══════════════════════════════════════════════════════════════════════════════
class _DishManagementHeader extends StatelessWidget {
  const _DishManagementHeader({
    required this.selectedFilter,
    required this.onFilterSelected,
  });

  final _MenuFilter selectedFilter;
  final ValueChanged<_MenuFilter> onFilterSelected;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          '菜品管理',
          style: _fit(
              Theme.of(context).textTheme.headlineMedium, 24, 32, FontWeight.bold),
        ),
        const SizedBox(height: 12),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: <Widget>[
              // 【自查修正】原来这三个筛选用本文件自绘的 _FilterChip（带 7×7
              // 前导圆点、选中是 12% 淡底 + 彩字），订单页却用共享的 CozyPill
              // （无圆点、选中是实心 + 白字）。同一件事两种长相，统一到 CozyPill。
              CozyPill(
                text: '全部',
                selected: selectedFilter == _MenuFilter.all,
                onTap: () => onFilterSelected(_MenuFilter.all),
              ),
              const SizedBox(width: 8),
              CozyPill(
                text: '已上架',
                selected: selectedFilter == _MenuFilter.available,
                onTap: () => onFilterSelected(_MenuFilter.available),
              ),
              const SizedBox(width: 8),
              CozyPill(
                text: '已下架',
                selected: selectedFilter == _MenuFilter.unavailable,
                color: CozyPalette.onSurfaceVariant,
                onTap: () => onFilterSelected(_MenuFilter.unavailable),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  FloatingAddDishButton（MenuManagementScreen.kt:678）
// ══════════════════════════════════════════════════════════════════════════════
class _FloatingAddDishButton extends StatelessWidget {
  const _FloatingAddDishButton({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return _PressScale(
      onTap: onTap,
      // 【审查修正】原来是宽约 378px 的文字胶囊（原生同款），悬浮在右下角时会
      // 压住第二张分类卡的「N 款菜品」。改成 56×56 圆形 FAB：只占右下角一小块，
      // 不再盖住任何文字；配色也从「粉底白字」换成应用主按钮同款的「玫瑰底白字」
      // （对比度更高）。文字入口仍在它打开的「新增菜品」弹层标题上，另外补一条
      // 语义标签，让图标按钮在无障碍/自动化里仍然叫得出名字。
      child: Semantics(
        label: '新增菜品',
        button: true,
        child: Container(
          width: 56,
          height: 56,
          alignment: Alignment.center,
          decoration: const BoxDecoration(
            shape: BoxShape.circle,
            color: CozyPalette.primary,
            boxShadow: CozyLight.cardShadow,
          ),
          child: const Icon(Icons.add, size: 26, color: CozyPalette.surface),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  MenuContent（MenuManagementScreen.kt:722）
// ══════════════════════════════════════════════════════════════════════════════
class _MenuContent extends StatelessWidget {
  const _MenuContent({
    required this.isLoadingCategory,
    required this.visibleDishes,
    required this.isBatchMode,
    required this.selectedDishIds,
    required this.categoryOf,
    required this.onToggleSelection,
    required this.onToggleAvailability,
    required this.onEdit,
    required this.onDelete,
    required this.onShowAddSheet,
  });

  final bool isLoadingCategory;
  final List<MenuItem> visibleDishes;
  final bool isBatchMode;
  final Set<String> selectedDishIds;
  final String Function(MenuItem dish) categoryOf;
  final ValueChanged<MenuItem> onToggleSelection;
  final ValueChanged<MenuItem> onToggleAvailability;
  final ValueChanged<MenuItem> onEdit;
  final ValueChanged<MenuItem> onDelete;
  final VoidCallback onShowAddSheet;

  @override
  Widget build(BuildContext context) {
    if (isLoadingCategory) return const _SkeletonDishList();
    if (visibleDishes.isEmpty) {
      return _EmptyMenuCard(onShowAddSheet: onShowAddSheet);
    }
    return Column(
      children: <Widget>[
        for (final MenuItem dish in visibleDishes) ...<Widget>[
          _DishManageCard(
            dish: dish,
            category: categoryOf(dish),
            selected: selectedDishIds.contains(dish.id),
            isBatchMode: isBatchMode,
            onToggleSelection: () => onToggleSelection(dish),
            onToggleAvailability: () => onToggleAvailability(dish),
            onEdit: () => onEdit(dish),
            onDelete: () => onDelete(dish),
          ),
          const SizedBox(height: 16),
        ],
        const SizedBox(height: 4),
      ],
    );
  }
}

/// 原生 `SkeletonDishList`（MenuManagementScreen.kt:766）
class _SkeletonDishList extends StatelessWidget {
  const _SkeletonDishList();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        for (int i = 0; i < 4; i++) ...<Widget>[
          SizedBox(
            height: 120,
            child: CozyCard(
              radius: 14,
              padding: const EdgeInsets.all(20),
              child: Row(
                children: <Widget>[
                  Container(
                    width: 80,
                    height: 80,
                    decoration: BoxDecoration(
                      color: CozyPalette.secondaryContainer,
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  const SizedBox(width: 16),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        _SkeletonBar(widthFactor: 0.42, height: 16),
                        SizedBox(height: 10),
                        _SkeletonBar(widthFactor: 0.30, height: 18),
                        SizedBox(height: 10),
                        _SkeletonBar(widthFactor: 0.52, height: 12),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
        ],
      ],
    );
  }
}

class _SkeletonBar extends StatelessWidget {
  const _SkeletonBar({required this.widthFactor, required this.height});

  final double widthFactor;
  final double height;

  @override
  Widget build(BuildContext context) {
    return FractionallySizedBox(
      alignment: Alignment.centerLeft,
      widthFactor: widthFactor,
      child: Container(
        height: height,
        decoration: BoxDecoration(
          color: CozyPalette.secondaryContainer,
          borderRadius: BorderRadius.circular(6),
        ),
      ),
    );
  }
}

/// 原生 `EmptyMenuCard`（MenuManagementScreen.kt:795）
class _EmptyMenuCard extends StatelessWidget {
  const _EmptyMenuCard({required this.onShowAddSheet});

  final VoidCallback onShowAddSheet;

  @override
  Widget build(BuildContext context) {
    // 原生外层 Box 还有 fillMaxHeight()，但在 LazyColumn item 里高度无界；
    // Flutter 的 ListView 同样无界，所以只保留 28 内边距 + 居中。
    return Padding(
      padding: const EdgeInsets.all(28),
      child: _OrderGuidanceEmptyState(
        title: '当前分类暂无菜品',
        subtitle: '点击添加菜品，补充菜名、价格、分类和图片后即可上架。',
        actionText: '+ 添加菜品',
        onAction: onShowAddSheet,
      ),
    );
  }
}

/// 原生 `OrderGuidanceEmptyState`（ui/components/OrderDesignSystem.kt:65）
class _OrderGuidanceEmptyState extends StatelessWidget {
  const _OrderGuidanceEmptyState({
    required this.title,
    required this.subtitle,
    this.actionText,
    this.onAction,
  });

  final String title;
  final String subtitle;
  final String? actionText;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final TextTheme t = Theme.of(context).textTheme;
    return Column(
      children: <Widget>[
        Container(
          width: 88,
          height: 88,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: CozyPalette.primaryContainer,
            borderRadius: BorderRadius.circular(28),
          ),
          child: const Icon(
            Icons.restaurant_outlined,
            size: 36,
            color: CozyPalette.primary,
          ),
        ),
        // OrderDiskSpacing.sm = 8
        const SizedBox(height: 8),
        Text(
          title,
          textAlign: TextAlign.center,
          style: t.titleLarge!.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Text(
          subtitle,
          textAlign: TextAlign.center,
          style: t.bodyMedium!.copyWith(color: CozyPalette.onSurfaceVariant),
        ),
        if (actionText != null && onAction != null) ...<Widget>[
          // OrderDiskSpacing.xs = 4
          const SizedBox(height: 4),
          _OrderPrimaryButton(text: actionText!, onTap: onAction!),
        ],
      ],
    );
  }
}

/// 原生 `OrderPrimaryButton`（OrderDesignSystem.kt:40）：M3 Button / 圆角 18 /
/// 主色实心 / 按下缩到 0.97（tween 200ms）。
class _OrderPrimaryButton extends StatelessWidget {
  const _OrderPrimaryButton({required this.text, required this.onTap});

  final String text;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return _PressScale(
      onTap: onTap,
      scale: 0.97,
      duration: const Duration(milliseconds: 200),
      child: Container(
        height: 40,
        padding: const EdgeInsets.symmetric(horizontal: 24),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: CozyPalette.primary,
          borderRadius: BorderRadius.circular(18),
        ),
        child: Text(
          text,
          style: Theme.of(context).textTheme.labelLarge!.copyWith(
                fontWeight: FontWeight.w600,
                color: CozyPalette.surface,
              ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DishManageCard（MenuManagementScreen.kt:813）
// ══════════════════════════════════════════════════════════════════════════════
class _DishManageCard extends StatelessWidget {
  const _DishManageCard({
    required this.dish,
    required this.category,
    required this.selected,
    required this.isBatchMode,
    required this.onToggleSelection,
    required this.onToggleAvailability,
    required this.onEdit,
    required this.onDelete,
  });

  final MenuItem dish;
  final String category;
  final bool selected;
  final bool isBatchMode;
  final VoidCallback onToggleSelection;
  final VoidCallback onToggleAvailability;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final bool available = dish.isAvailable;
    final Color activeColor =
        available ? CozyPalette.primary : CozyPalette.onSurfaceVariant;
    final TextTheme t = Theme.of(context).textTheme;
    return CozyCard(
      radius: 16,
      height: 112,
      padding: EdgeInsets.zero,
      color: available
          ? CozyPalette.surface
          : CozyPalette.surface.withValues(alpha: 0.58),
      // 这条描边编码「在售 / 已下架」状态，是原生语义的一部分，保留原色
      borderColor: available
          ? CozyPalette.primary.withValues(alpha: 0.20)
          : CozyPalette.outlineVariant.withValues(alpha: 0.54),
      child: Container(
        color: selected
            ? CozyPalette.primary.withValues(alpha: 0.06)
            : CozyPalette.surface,
        // 【度量】上下留白收到 7dp：卡片高 112dp ⇒ 内容区 98dp，
        // 刚好容纳右侧动作列（22 + 5 + 32 + 5 + 32 = 96dp）。
        // 原写法 all(12) 只给内容区 88dp，动作列 110dp 溢出 22dp，
        // 删除按钮被卡片圆角裁掉一半。
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
        child: Row(
          children: <Widget>[
            if (isBatchMode) ...<Widget>[
              Checkbox(
                value: selected,
                onChanged: (_) => onToggleSelection(),
                activeColor: CozyPalette.primary,
                checkColor: CozyPalette.surface,
              ),
              const SizedBox(width: 10),
            ],
            _DishImage(dish: dish),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    dish.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: _fit(t.titleMedium, 16, 20, FontWeight.bold)
                        .copyWith(color: activeColor),
                  ),
                  Row(
                    children: <Widget>[
                      _StatusTag(text: category, color: activeColor),
                      const SizedBox(width: 6),
                      _StatusTag(
                        text: available ? '在售' : '已下架',
                        color: activeColor,
                      ),
                    ],
                  ),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: <Widget>[
                      Text(
                        _yuanText(dish.price),
                        style: _fit(t.titleLarge, 18, 24, FontWeight.bold)
                            .copyWith(color: activeColor),
                      ),
                      // 原生这里还有 originPrice > price 时的划线原价。
                      // 【数据缺口】MenuItem 没有 originPrice（见交付说明），故不渲染。
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            _DishActionArea(
              checked: available,
              onToggleAvailability: onToggleAvailability,
              onEdit: onEdit,
              onDelete: onDelete,
            ),
          ],
        ),
      ),
    );
  }
}

/// 原生 `DishImage`（MenuManagementScreen.kt:889）
class _DishImage extends StatelessWidget {
  const _DishImage({required this.dish});

  final MenuItem dish;

  @override
  Widget build(BuildContext context) {
    final Widget placeholder = Container(
      width: 76,
      height: 76,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: CozyPalette.primary.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(12),
      ),
      child: const Icon(
        Icons.restaurant_outlined,
        size: 30,
        color: CozyPalette.primary,
      ),
    );
    // Flutter 侧沿用工程既有约定：imageUrl 是 ≤4 字符的 emoji 时当图标渲染，
    // 否则当成真正的图片地址（与 ordering_page.dart / orders_page.dart 一致）。
    if (dish.imageUrl.length <= 4) {
      if (dish.imageUrl.isEmpty) return placeholder;
      return SizedBox(
        width: 76,
        height: 76,
        child: Center(
          child: Text(dish.imageUrl, style: const TextStyle(fontSize: 34)),
        ),
      );
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: Image.network(
        dish.imageUrl,
        width: 76,
        height: 76,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => placeholder,
      ),
    );
  }
}

/// 原生 `StatusTag`（MenuManagementScreen.kt:913）
class _StatusTag extends StatelessWidget {
  const _StatusTag({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(999),
        color: color.withValues(alpha: 0.12),
      ),
      child: Text(
        text,
        style:
            _fit(Theme.of(context).textTheme.labelMedium, 12, 16, FontWeight.w500)
                .copyWith(color: color),
      ),
    );
  }
}

/// 原生 `DishActionArea`（MenuManagementScreen.kt:945）
class _DishActionArea extends StatelessWidget {
  const _DishActionArea({
    required this.checked,
    required this.onToggleAvailability,
    required this.onEdit,
    required this.onDelete,
  });

  final bool checked;
  final VoidCallback onToggleAvailability;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    // 【布局约束】卡片高 112dp、上下留白 7dp ⇒ 内容区 98dp。
    // 本列合计必须 ≤ 98dp：22(开关) + 5 + 32 + 5 + 32 = 96dp。
    // 原写法 22 + 8 + 36 + 8 + 36 = 110dp 塞进 88dp，末尾删除按钮被卡片圆角裁掉。
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        _AvailabilitySwitch(checked: checked, onTap: onToggleAvailability),
        const SizedBox(height: 5),
        GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: onEdit,
          child: Container(
            width: 32,
            height: 32,
            alignment: Alignment.center,
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
              color: CozyPalette.surfaceContainerLow,
            ),
            child: const Icon(
              Icons.more_horiz,
              size: 19,
              color: CozyPalette.primary,
            ),
          ),
        ),
        const SizedBox(height: 5),
        GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: onDelete,
          child: Container(
            width: 32,
            height: 32,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: CozyPalette.error.withValues(alpha: 0.10),
              border: Border.all(
                color: CozyPalette.error.withValues(alpha: 0.22),
              ),
            ),
            child: const Icon(
              Icons.delete_outline,
              size: 18,
              color: CozyPalette.error,
            ),
          ),
        ),
      ],
    );
  }
}

/// 原生 `AvailabilitySwitch`（MenuManagementScreen.kt:926）：38×22 自绘开关
class _AvailabilitySwitch extends StatelessWidget {
  const _AvailabilitySwitch({required this.checked, required this.onTap});

  final bool checked;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: Container(
        width: 38,
        height: 22,
        padding: const EdgeInsets.all(3),
        alignment: checked ? Alignment.centerRight : Alignment.centerLeft,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(999),
          color: checked
              ? CozyPalette.primaryContainer
              : CozyPalette.surfaceVariant,
        ),
        child: Container(
          width: 16,
          height: 16,
          decoration: const BoxDecoration(
            shape: BoxShape.circle,
            color: CozyPalette.surface,
          ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  AddDishSuccessToast（MenuManagementScreen.kt:317）
// ══════════════════════════════════════════════════════════════════════════════
class _AddDishSuccessToast extends StatelessWidget {
  const _AddDishSuccessToast();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 16),
      decoration: BoxDecoration(
        color: CozyPalette.surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: CozyPalette.outlineVariant),
        boxShadow: CozyLight.cardShadow,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          const Icon(Icons.check_circle, size: 24, color: CozyPalette.primary),
          const SizedBox(width: 10),
          Text(
            '已新增菜品',
            style:
                _fit(Theme.of(context).textTheme.titleSmall, 17, 22, FontWeight.bold),
          ),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DishEditorDialog（MenuManagementScreen.kt:1255）—— 原生是 ModalBottomSheet
// ══════════════════════════════════════════════════════════════════════════════
class _DishEditorSheet extends StatefulWidget {
  const _DishEditorSheet({
    required this.initial,
    required this.categories,
    required this.onSave,
    required this.onPickImage,
  });

  final _DishDraft initial;
  final List<String> categories;
  final Future<String?> Function(_DishDraft draft,
      {required bool createMissingCategory}) onSave;
  final VoidCallback onPickImage;

  @override
  State<_DishEditorSheet> createState() => _DishEditorSheetState();
}

class _DishEditorSheetState extends State<_DishEditorSheet> {
  late _DishDraft _draft;
  String? _message;
  bool _saving = false;

  final TextEditingController _nameCtrl = TextEditingController();
  final TextEditingController _priceCtrl = TextEditingController();
  final TextEditingController _originPriceCtrl = TextEditingController();
  final TextEditingController _categoryCtrl = TextEditingController();
  final TextEditingController _stockCtrl = TextEditingController();
  final TextEditingController _descCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _draft = widget.initial;
    _nameCtrl.text = _draft.name;
    _priceCtrl.text = _draft.price;
    _originPriceCtrl.text = _draft.originPrice;
    _categoryCtrl.text = _draft.category;
    _stockCtrl.text = _draft.stock;
    _descCtrl.text = _draft.description;
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _priceCtrl.dispose();
    _originPriceCtrl.dispose();
    _categoryCtrl.dispose();
    _stockCtrl.dispose();
    _descCtrl.dispose();
    super.dispose();
  }

  /// 原生 `filterDecimal()`：只留数字和小数点
  static String _onlyDecimal(String value) => value
      .split('')
      .where((String c) => c == '.' || '0123456789'.contains(c))
      .join();

  /// 原生 `filter { it.isDigit() }`
  static String _onlyDigits(String value) =>
      value.split('').where((String c) => '0123456789'.contains(c)).join();

  /// 原生 DishEditorDialog 里的 `requestSave`（MenuManagementScreen.kt:1275）
  Future<void> _requestSave() async {
    final String normalizedCategory = _draft.category.trim();
    final String? existing =
        _firstWhereIgnoreCase(widget.categories, normalizedCategory);
    if (normalizedCategory.isEmpty) {
      await _commit(createMissingCategory: false);
      return;
    }
    if (existing != null) {
      if (existing != _draft.category) {
        setState(() {
          _draft.category = existing;
          _categoryCtrl.text = existing;
        });
      }
      await _commit(createMissingCategory: false);
      return;
    }
    // 原生 `pendingCategoryCreation`（MenuManagementScreen.kt:1516-1551）：
    // 「创建新分类？」确认框，确认后自动建分类并继续保存菜品。
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        backgroundColor: CozyPalette.surface,
        surfaceTintColor: Colors.transparent,
        title: Text(
          '创建新分类？',
          style: Theme.of(ctx).textTheme.titleLarge!.copyWith(
                fontWeight: FontWeight.bold,
                color: CozyPalette.onSurface,
              ),
        ),
        content: Text(
          '“$normalizedCategory”还不是现有分类。确认后会自动创建该分类，并继续保存菜品。',
          style: Theme.of(ctx)
              .textTheme
              .bodyMedium!
              .copyWith(color: CozyPalette.onSurfaceVariant),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(
              '返回修改',
              style: TextStyle(color: CozyPalette.onSurfaceVariant),
            ),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: FilledButton.styleFrom(
              backgroundColor: CozyPalette.primary,
              foregroundColor: const Color(0xFFFAFCFF),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            ),
            child: const Text('创建并保存'),
          ),
        ],
      ),
    );
    if (!mounted || confirmed != true) return;
    await _commit(createMissingCategory: true);
  }

  Future<void> _commit({required bool createMissingCategory}) async {
    if (_saving) return;
    setState(() => _saving = true);
    final String? error = await widget.onSave(
      _draft.clone(),
      createMissingCategory: createMissingCategory,
    );
    if (!mounted) return;
    setState(() => _saving = false);
    if (error == null) {
      Navigator.of(context).pop();
    } else {
      setState(() => _message = error);
    }
  }

  @override
  Widget build(BuildContext context) {
    final TextTheme t = Theme.of(context).textTheme;
    final double screenHeight = MediaQuery.sizeOf(context).height;
    final double viewInset = MediaQuery.viewInsetsOf(context).bottom;
    final double bottomInset = MediaQuery.paddingOf(context).bottom;
    return SizedBox(
      height: screenHeight * 0.9,
      child: Padding(
        padding: EdgeInsets.only(bottom: viewInset),
        child: Column(
          children: <Widget>[
            // 原生 dragHandle：56×6 / 圆角 999 / BorderColor，padding top16 bottom10
            Container(
              width: 56,
              height: 6,
              margin: const EdgeInsets.only(top: 16, bottom: 10),
              decoration: BoxDecoration(
                color: CozyPalette.outlineVariant,
                borderRadius: BorderRadius.circular(999),
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(left: 28, right: 24, bottom: 20),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          _draft.id == null ? '新增菜品' : '编辑菜品',
                          style: _fit(t.displayLarge, 32, 38, FontWeight.bold)
                              .copyWith(color: CozyPalette.primary),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          '给你们的小饭桌添一道新菜',
                          style: _fit(t.titleLarge, 18, 26),
                        ),
                      ],
                    ),
                  ),
                  GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () => Navigator.of(context).pop(),
                    child: Container(
                      width: 48,
                      height: 48,
                      alignment: Alignment.center,
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        color: CozyPalette.surface,
                      ),
                      child: const Icon(
                        Icons.close,
                        size: 28,
                        color: CozyPalette.onSurface,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Container(
              height: 1,
              color: CozyPalette.outlineVariant.withValues(alpha: 0.60),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(28, 24, 28, 16),
                children: <Widget>[
                  // 菜品图（原生 214dp，点击选图）
                  GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: widget.onPickImage,
                    child: Container(
                      width: double.infinity,
                      height: 214,
                      clipBehavior: Clip.antiAlias,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(20),
                        color:
                            CozyPalette.primaryContainer.withValues(alpha: 0.46),
                        border: Border.all(
                          color: CozyPalette.primary.withValues(alpha: 0.28),
                          width: 2,
                        ),
                      ),
                      child: _draft.imageUrl.isNotEmpty
                          ? Image.network(
                              _draft.imageUrl,
                              fit: BoxFit.cover,
                              errorBuilder: (_, _, _) =>
                                  _imagePlaceholder(context),
                            )
                          : _imagePlaceholder(context),
                    ),
                  ),
                  const SizedBox(height: 16),
                  _LabeledEditorField(
                    label: '名称',
                    child: _EditorTextField(
                      controller: _nameCtrl,
                      label: '比如 爱心披萨',
                      onChanged: (String v) => _draft.name = v,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Expanded(
                        child: _LabeledEditorField(
                          label: '售价 (¥)',
                          child: _EditorTextField(
                            controller: _priceCtrl,
                            label: '比如 28.00',
                            keyboardType: const TextInputType.numberWithOptions(
                                decimal: true),
                            onChanged: (String v) => _draft.price = _onlyDecimal(v),
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _LabeledEditorField(
                          label: '原价 (可选)',
                          child: _EditorTextField(
                            controller: _originPriceCtrl,
                            label: '可不填',
                            keyboardType: const TextInputType.numberWithOptions(
                                decimal: true),
                            onChanged: (String v) =>
                                _draft.originPrice = _onlyDecimal(v),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      // 原生 weight 1.35f / 0.65f
                      Expanded(
                        flex: 135,
                        child: _LabeledEditorField(
                          label: '分类',
                          child: _EditorTextField(
                            controller: _categoryCtrl,
                            label: '选择或新建分类',
                            onChanged: (String v) =>
                                setState(() => _draft.category = v),
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        flex: 65,
                        child: _LabeledEditorField(
                          label: '库存',
                          child: _EditorTextField(
                            controller: _stockCtrl,
                            label: '32',
                            keyboardType: TextInputType.number,
                            onChanged: (String v) => _draft.stock = _onlyDigits(v),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  // 分类快选（原生 LazyRow，spacedBy 8）
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: <Widget>[
                        for (final String category
                            in widget.categories.toSet()) ...<Widget>[
                          _EditorCategoryPill(
                            text: category,
                            selected: category == _draft.category,
                            onTap: () => setState(() {
                              _draft.category = category;
                              _categoryCtrl.text = category;
                            }),
                          ),
                          const SizedBox(width: 8),
                        ],
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  _LabeledEditorField(
                    label: '描述',
                    child: TextField(
                      controller: _descCtrl,
                      minLines: 3,
                      maxLines: null,
                      onChanged: (String v) => _draft.description = v,
                      decoration: cozyInputDecoration(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              '是否上架',
                              style: _fit(t.titleMedium, 16, 22, FontWeight.bold),
                            ),
                            Text(
                              '立即在小店展示',
                              style: _fit(t.labelLarge, 13, 18)
                                  .copyWith(color: CozyPalette.onSurfaceVariant),
                            ),
                          ],
                        ),
                      ),
                      Switch(
                        value: _draft.isAvailable,
                        onChanged: (bool v) =>
                            setState(() => _draft.isAvailable = v),
                        activeTrackColor: CozyPalette.primary,
                        activeThumbColor: CozyPalette.surface,
                        inactiveTrackColor: CozyPalette.surfaceVariant,
                        inactiveThumbColor: CozyPalette.outline,
                      ),
                    ],
                  ),
                  if (_message != null) ...<Widget>[
                    const SizedBox(height: 16),
                    Text(
                      _message!,
                      style: t.bodySmall!.copyWith(color: CozyPalette.primary),
                    ),
                  ],
                ],
              ),
            ),
            // 底部双按钮（原生 weight 1f / 1.55f，高 58）
            // 【审查修正】这里原来只给「保存菜品」写了 `flex: 155`，没给「取消」写
            // `flex: 100`，于是 Flutter 按 1 : 155 分配宽度 —— 真机上「取消」被压成
            // 6px 宽、两个字竖着叠在一起，还被右边的胶囊盖住，等于点不到。
            // 补上 100 之后才和原生一样是 1 : 1.55（≈336px : 522px）。
            Container(
              width: double.infinity,
              color: CozyPalette.surface.withValues(alpha: 0.96),
              padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 18),
              child: Row(
                children: <Widget>[
                  Expanded(
                    flex: 100,
                    child: _PressScale(
                      onTap: () => Navigator.of(context).pop(),
                      child: Container(
                        height: 58,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(999),
                          color: CozyPalette.surface,
                          border:
                              Border.all(color: CozyPalette.primary, width: 2),
                        ),
                        child: Text(
                          '取消',
                          style: _fit(t.titleLarge, 18, 24, FontWeight.bold)
                              .copyWith(color: CozyPalette.primary),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 18),
                  Expanded(
                    flex: 155,
                    child: _PressScale(
                      onTap: _requestSave,
                      child: Container(
                        height: 58,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(999),
                          color: CozyPalette.primary,
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: <Widget>[
                            const Icon(Icons.check,
                                size: 24, color: CozyPalette.surface),
                            const SizedBox(width: 10),
                            Text(
                              '保存菜品',
                              style: _fit(t.titleLarge, 18, 24, FontWeight.bold)
                                  .copyWith(color: CozyPalette.surface),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: bottomInset),
          ],
        ),
      ),
    );
  }

  Widget _imagePlaceholder(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        Container(
          width: 72,
          height: 72,
          alignment: Alignment.center,
          decoration: const BoxDecoration(
            shape: BoxShape.circle,
            color: CozyPalette.surface,
          ),
          child: const Icon(
            Icons.add_photo_alternate_outlined,
            size: 34,
            color: CozyPalette.primary,
          ),
        ),
        const SizedBox(height: 10),
        Text(
          '选择或拍摄菜品图',
          style: _fit(Theme.of(context).textTheme.titleLarge, 18, 24,
                  FontWeight.bold)
              .copyWith(color: CozyPalette.onSurfaceVariant),
        ),
      ],
    );
  }
}

/// 原生 `LabeledEditorField`（MenuManagementScreen.kt:1554）
class _LabeledEditorField extends StatelessWidget {
  const _LabeledEditorField({required this.label, required this.child});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          label,
          style: _fit(
              Theme.of(context).textTheme.titleMedium, 16, 22, FontWeight.bold),
        ),
        const SizedBox(height: 8),
        child,
      ],
    );
  }
}

/// 原生 `EditorTextField`（MenuManagementScreen.kt:1566）
class _EditorTextField extends StatelessWidget {
  const _EditorTextField({
    required this.controller,
    required this.label,
    required this.onChanged,
    this.keyboardType,
  });

  final TextEditingController controller;
  final String label;
  final ValueChanged<String> onChanged;
  final TextInputType? keyboardType;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      keyboardType: keyboardType,
      onChanged: onChanged,
      decoration: cozyInputDecoration(labelText: label),
    );
  }
}

/// 原生 DishEditorDialog 里的分类快选药丸（MenuManagementScreen.kt:1430）
class _EditorCategoryPill extends StatelessWidget {
  const _EditorCategoryPill({
    required this.text,
    required this.selected,
    required this.onTap,
  });

  final String text;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(999),
          color: selected ? CozyPalette.primary : CozyPalette.surface,
          border: Border.all(
            color: selected ? CozyPalette.primary : CozyPalette.outlineVariant,
          ),
        ),
        child: Text(
          text,
          style: Theme.of(context).textTheme.labelLarge!.copyWith(
                fontWeight: FontWeight.bold,
                color: selected ? CozyPalette.surface : CozyPalette.onSurface,
              ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  其它弹层
//  NewCategoryDialog（:1086） / CategoryManagerDialog（:1122） /
//  DeleteDishDialog（:1226） / ShopSettingsDialog（:981） /
//  ImageSourcePickerDialog（ui/components/ImageSourcePicker.kt:40）
// ══════════════════════════════════════════════════════════════════════════════

/// 原生 `NewCategoryDialog`（MenuManagementScreen.kt:1086）
Future<void> showNewCategoryDialog(
  BuildContext context, {
  required ValueChanged<String> onSave,
}) {
  return showDialog<void>(
    context: context,
    builder: (BuildContext ctx) => _NewCategoryDialog(onSave: onSave),
  );
}

class _NewCategoryDialog extends StatefulWidget {
  const _NewCategoryDialog({required this.onSave});

  final ValueChanged<String> onSave;

  @override
  State<_NewCategoryDialog> createState() => _NewCategoryDialogState();
}

class _NewCategoryDialogState extends State<_NewCategoryDialog> {
  final TextEditingController _ctrl = TextEditingController();

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      title: Text(
        '新建分类',
        style: Theme.of(context).textTheme.titleLarge!.copyWith(
              fontWeight: FontWeight.bold,
              color: CozyPalette.onSurface,
            ),
      ),
      content: TextField(
        controller: _ctrl,
        onChanged: (_) => setState(() {}),
        decoration: cozyInputDecoration(labelText: '分类名称'),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('取消',
              style: TextStyle(color: CozyPalette.onSurfaceVariant)),
        ),
        _DialogConfirmButton(
          text: '保存分类',
          enabled: _ctrl.text.trim().isNotEmpty,
          onTap: () {
            Navigator.of(context).pop();
            widget.onSave(_ctrl.text);
          },
        ),
      ],
    );
  }
}

/// 原生 `DeleteDishDialog`（MenuManagementScreen.kt:1226）—— 删除分类也复用它
Future<bool?> showDeleteDishDialog(
  BuildContext context, {
  required String title,
  required String body,
  required String confirmText,
}) {
  return showDialog<bool>(
    context: context,
    builder: (BuildContext ctx) => AlertDialog(
      backgroundColor: CozyPalette.surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      title: Text(
        title,
        style: Theme.of(ctx).textTheme.titleLarge!.copyWith(
              fontWeight: FontWeight.bold,
              color: CozyPalette.onSurface,
            ),
      ),
      content: Text(
        body,
        style: _fit(Theme.of(ctx).textTheme.bodyMedium, 14, 20)
            .copyWith(color: CozyPalette.onSurfaceVariant),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(false),
          child: const Text('取消',
              style: TextStyle(color: CozyPalette.onSurfaceVariant)),
        ),
        _DialogConfirmButton(
          text: confirmText,
          danger: true,
          onTap: () => Navigator.of(ctx).pop(true),
        ),
      ],
    ),
  );
}

/// 原生 `CategoryManagerDialog`（MenuManagementScreen.kt:1122）
Future<void> showCategoryManagerDialog(
  BuildContext context, {
  required List<String> categories,
  required String selectedCategory,
  required ValueChanged<String> onCreate,
  required void Function(String oldName, String newName) onRename,
  required ValueChanged<String> onDelete,
}) {
  return showDialog<void>(
    context: context,
    builder: (BuildContext _) => _CategoryManagerDialog(
      categories: categories,
      selectedCategory: selectedCategory,
      onCreate: onCreate,
      onRename: onRename,
      onDelete: onDelete,
    ),
  );
}

class _CategoryManagerDialog extends StatefulWidget {
  const _CategoryManagerDialog({
    required this.categories,
    required this.selectedCategory,
    required this.onCreate,
    required this.onRename,
    required this.onDelete,
  });

  final List<String> categories;
  final String selectedCategory;
  final ValueChanged<String> onCreate;
  final void Function(String oldName, String newName) onRename;
  final ValueChanged<String> onDelete;

  @override
  State<_CategoryManagerDialog> createState() => _CategoryManagerDialogState();
}

class _CategoryManagerDialogState extends State<_CategoryManagerDialog> {
  late List<String> _categories = widget.categories;
  final TextEditingController _newCtrl = TextEditingController();
  final TextEditingController _editingCtrl = TextEditingController();
  String? _editingCategory;

  @override
  void dispose() {
    _newCtrl.dispose();
    _editingCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final TextTheme t = Theme.of(context).textTheme;
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      title: Text(
        '分类管理',
        style: t.titleLarge!.copyWith(
          fontWeight: FontWeight.bold,
          color: CozyPalette.onSurface,
        ),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxHeight: 440),
          child: ListView(
            shrinkWrap: true,
            children: <Widget>[
              Row(
                children: <Widget>[
                  Expanded(
                    child: TextField(
                      controller: _newCtrl,
                      onChanged: (_) => setState(() {}),
                      decoration: cozyInputDecoration(labelText: '新分类'),
                    ),
                  ),
                  IconButton(
                    onPressed: _newCtrl.text.trim().isEmpty
                        ? null
                        : () {
                            widget.onCreate(_newCtrl.text);
                            setState(() {
                              _categories = <String>{
                                ..._categories,
                                _newCtrl.text.trim(),
                              }.toList();
                              _newCtrl.clear();
                            });
                          },
                    icon: const Icon(Icons.add, color: CozyPalette.primary),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              for (final String category in _categories) ...<Widget>[
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    color: category == widget.selectedCategory
                        ? CozyPalette.primary.withValues(alpha: 0.10)
                        : CozyPalette.surfaceContainerLow,
                    border: Border.all(color: CozyPalette.outlineVariant),
                  ),
                  child: Row(
                    children: <Widget>[
                      if (_editingCategory == category) ...<Widget>[
                        Expanded(
                          child: TextField(
                            controller: _editingCtrl,
                            decoration: cozyInputDecoration(),
                          ),
                        ),
                        IconButton(
                          onPressed: () {
                            final String next = _editingCtrl.text;
                            widget.onRename(category, next);
                            setState(() {
                              _categories = _categories
                                  .map((String c) =>
                                      c == category ? next.trim() : c)
                                  .toSet()
                                  .toList();
                              _editingCategory = null;
                            });
                          },
                          icon: const Icon(Icons.check,
                              color: CozyPalette.primary),
                        ),
                      ] else ...<Widget>[
                        Expanded(
                          child: Text(
                            category,
                            style: _fit(
                              t.titleLarge,
                              15,
                              20,
                              category == widget.selectedCategory
                                  ? FontWeight.bold
                                  : FontWeight.w500,
                            ),
                          ),
                        ),
                        IconButton(
                          onPressed: () => setState(() {
                            _editingCategory = category;
                            _editingCtrl.text = category;
                          }),
                          icon: const Icon(Icons.edit_outlined,
                              color: CozyPalette.onSurfaceVariant),
                        ),
                        IconButton(
                          onPressed: () {
                            widget.onDelete(category);
                            setState(() {
                              _categories = _categories
                                  .where((String c) => c != category)
                                  .toList();
                            });
                          },
                          icon: const Icon(Icons.delete_outline,
                              color: CozyPalette.error),
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 10),
              ],
            ],
          ),
        ),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('完成', style: TextStyle(color: CozyPalette.primary)),
        ),
      ],
    );
  }
}

/// 原生 `ShopSettingsDialog`（MenuManagementScreen.kt:981）
Future<void> showShopSettingsDialog(
  BuildContext context, {
  required TextEditingController shopNameCtrl,
  required TextEditingController announcementCtrl,
  required String coverUrl,
  required VoidCallback onPickImage,
  required Future<void> Function() onSave,
}) {
  return showDialog<void>(
    context: context,
    builder: (BuildContext ctx) => StatefulBuilder(
      builder: (BuildContext ctx, void Function(void Function()) setLocal) {
        return AlertDialog(
          backgroundColor: CozyPalette.surface,
          surfaceTintColor: Colors.transparent,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
          title: SizedBox(
            width: double.infinity,
            child: Text(
              '编辑店铺资料',
              textAlign: TextAlign.center,
              style: Theme.of(ctx).textTheme.titleLarge!.copyWith(
                    fontWeight: FontWeight.bold,
                    color: CozyPalette.onSurface,
                  ),
            ),
          ),
          content: SizedBox(
            width: double.maxFinite,
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 520),
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    ClipRRect(
                      borderRadius: BorderRadius.circular(18),
                      child: SizedBox(
                        width: double.infinity,
                        height: 142,
                        child: Stack(
                          children: <Widget>[
                            Positioned.fill(
                              child: coverUrl.isNotEmpty
                                  ? Image.network(
                                      coverUrl,
                                      fit: BoxFit.cover,
                                      errorBuilder: (_, _, _) => Image.asset(
                                          _shopBannerAsset,
                                          fit: BoxFit.cover),
                                    )
                                  : Image.asset(_shopBannerAsset,
                                      fit: BoxFit.cover),
                            ),
                            Positioned(
                              right: 10,
                              bottom: 10,
                              child: GestureDetector(
                                behavior: HitTestBehavior.opaque,
                                onTap: onPickImage,
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 12, vertical: 8),
                                  decoration: BoxDecoration(
                                    borderRadius: BorderRadius.circular(999),
                                    color: CozyPalette.surface
                                        .withValues(alpha: 0.96),
                                    border: Border.all(
                                        color: CozyPalette.outlineVariant),
                                  ),
                                  child: Row(
                                    children: <Widget>[
                                      const Icon(
                                        Icons.add_photo_alternate_outlined,
                                        size: 18,
                                        color: CozyPalette.primary,
                                      ),
                                      const SizedBox(width: 6),
                                      Text(
                                        '更换封面',
                                        style: Theme.of(ctx)
                                            .textTheme
                                            .labelLarge!
                                            .copyWith(
                                              fontWeight: FontWeight.bold,
                                              color: CozyPalette.primary,
                                            ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: shopNameCtrl,
                      onChanged: (_) => setLocal(() {}),
                      decoration: cozyInputDecoration(labelText: '店铺名称'),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: announcementCtrl,
                      minLines: 3,
                      maxLines: 5,
                      decoration: cozyInputDecoration(labelText: '店铺公告'),
                    ),
                  ],
                ),
              ),
            ),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('取消',
                  style: TextStyle(color: CozyPalette.onSurfaceVariant)),
            ),
            _DialogConfirmButton(
              text: '保存资料',
              enabled: shopNameCtrl.text.trim().isNotEmpty,
              onTap: () {
                Navigator.of(ctx).pop();
                onSave();
              },
            ),
          ],
        );
      },
    ),
  );
}

/// 原生 `ImageSourcePickerDialog`（ui/components/ImageSourcePicker.kt:40）
///
/// 结构 1:1，但 Flutter 侧还没有选图能力（工程未装 image_picker，
/// 且不允许新增依赖），两个入口只关闭弹层。见交付说明第 4 条的缺口清单。
Future<void> showImageSourcePickerDialog(
  BuildContext context, {
  required String title,
}) {
  return showDialog<void>(
    context: context,
    builder: (BuildContext ctx) => AlertDialog(
      backgroundColor: CozyPalette.surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      title: Text(
        title,
        style: Theme.of(ctx).textTheme.titleLarge!.copyWith(
              fontWeight: FontWeight.bold,
              color: CozyPalette.onSurface,
            ),
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          _ImageSourceOption(
            icon: Icons.photo_library_outlined,
            text: '从相册选择',
            onTap: () => Navigator.of(ctx).pop(),
          ),
          const SizedBox(height: 10),
          _ImageSourceOption(
            icon: Icons.photo_camera_outlined,
            text: '拍照上传',
            onTap: () => Navigator.of(ctx).pop(),
          ),
        ],
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(),
          child: const Text('取消', style: TextStyle(color: CozyPalette.primary)),
        ),
      ],
    ),
  );
}

/// 原生 `ImageSourceOption`（ImageSourcePicker.kt:126）
class _ImageSourceOption extends StatelessWidget {
  const _ImageSourceOption({
    required this.icon,
    required this.text,
    required this.onTap,
  });

  final IconData icon;
  final String text;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: CozyPalette.surfaceContainerLow,
          border: Border.all(color: CozyPalette.outlineVariant),
        ),
        child: Row(
          children: <Widget>[
            Icon(icon, color: CozyPalette.onSurface),
            const SizedBox(width: 12),
            Text(
              text,
              style: Theme.of(context)
                  .textTheme
                  .bodyLarge!
                  .copyWith(fontWeight: FontWeight.w600),
            ),
          ],
        ),
      ),
    );
  }
}

/// 原生弹层里的 `Button(containerColor = PrimaryBlue / DangerRed,
/// contentColor = #FAFCFF)`（MenuManagementScreen.kt:1071、:1241）
class _DialogConfirmButton extends StatelessWidget {
  const _DialogConfirmButton({
    required this.text,
    required this.onTap,
    this.enabled = true,
    this.danger = false,
  });

  final String text;
  final VoidCallback onTap;
  final bool enabled;
  final bool danger;

  @override
  Widget build(BuildContext context) {
    final Color background = danger ? CozyPalette.error : CozyPalette.primary;
    return _PressScale(
      onTap: enabled ? onTap : () {},
      child: Container(
        height: 40,
        padding: const EdgeInsets.symmetric(horizontal: 24),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: enabled ? background : background.withValues(alpha: 0.45),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Text(
          text,
          style: Theme.of(context).textTheme.labelLarge!.copyWith(
                fontWeight: FontWeight.w600,
                color: CozyPalette.surface,
              ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  小工具
// ══════════════════════════════════════════════════════════════════════════════

/// 原生 `CozyMotion` 的按压缩放（CozyCard 140ms / OrderPrimaryButton 200ms）
class _PressScale extends StatefulWidget {
  const _PressScale({
    required this.child,
    required this.onTap,
    this.scale = 0.975,
    this.duration = const Duration(milliseconds: 140),
  });

  final Widget child;
  final VoidCallback onTap;
  final double scale;
  final Duration duration;

  @override
  State<_PressScale> createState() => _PressScaleState();
}

class _PressScaleState extends State<_PressScale> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    return AnimatedScale(
      scale: _pressed ? widget.scale : 1.0,
      duration: widget.duration,
      curve: Curves.easeOut,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: (_) => setState(() => _pressed = true),
        onTapUp: (_) => setState(() => _pressed = false),
        onTapCancel: () => setState(() => _pressed = false),
        onTap: widget.onTap,
        child: widget.child,
      ),
    );
  }
}

/// 原生 `yuanText()`（ui/util/PriceText.kt:3）：`"¥" + "%.2f".format(value)`
String _yuanText(double value) => '¥${value.toStringAsFixed(2)}';

/// 原生到处用 `firstOrNull { it.trim().equals(x, ignoreCase = true) }`
String? _firstWhereIgnoreCase(List<String> list, String value) {
  final String target = value.toLowerCase();
  for (final String item in list) {
    if (item.trim().toLowerCase() == target) return item;
  }
  return null;
}
