// 发现页 —— 1:1 移植自原生
//   D:\kaifa\myapp\OrderDisk\app\src\main\java\com\myorderapp\ui\discover\DiscoverScreen.kt
//
// 结构对照（原生行号见每处注释）：
//   CozyPage(decorative=false) → 单个 ListView（原生 LazyColumn，无 tab / 无分段）
//   标题块 → 搜索框 → 今日/减脂推荐行 → 部分网络错误 → 三态（空查询提示 / 搜索中 / 空结果 / 结果卡片）
//   结果卡片点击 → ModalBottomSheet 菜品详情（原生 L102-119 + DiscoverDishDetailSheet L863-909）
//
// 数据层沿用工程现有的 `AppState.instance`：
//   searchRemoteRecipes(keyword)（本地内置菜谱库）+ addDish(...)，未新增任何 AppState 能力。
//
// 【搜一搜】用户要求「搜索直接出结果，只放成品图 + 名称」+「实时爬取，不要手机缓存」：
//   搜索时**实时**请求下厨房搜索页（lib/data/xiachufang_client.dart），拿回
//   「成品图 + 名称」直接展示；手机本地不落任何索引/缓存。
//   索引没命中 / 站点限流时才回退内置菜谱库（保持离线可用与老行为）。
//   加入我的小店 → 仍然写进云端（Supabase menu_dishes），这是唯一的落库动作。

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../../data/category_placement.dart';
import '../../data/image_cache.dart';
import '../../data/xiachufang_client.dart';
import '../theme/cozy_glass.dart';
import '../widgets/cozy_dish_photo.dart';
import '../widgets/cozy_skeletons.dart';
import '../widgets/cozy_toast.dart';

// ─────────────────────────────────────────────────────────────────────────────
// 原生同名的页内私有常量（DiscoverScreen.kt L85-91）
// ─────────────────────────────────────────────────────────────────────────────
const Color _discoverPrimary = CozyPalette.primary; // Primary        #FF894C5C
const Color _discoverCard = CozyPalette.secondaryContainer; // #FFFFD1DC
const Color _discoverCardBorder = CozyPalette.secondary; // Secondary      #FF78555E
const Color _discoverInput = CozyPalette.surfaceVariant; // #FFE7E2DC
const Color _discoverCreamCard = CozyPalette.surface; // #FFFFFCF8
const String _kSearchPlaceholder = '搜索菜品、做法、食材';

// 原生里散落在各 @Composable 内的字面色，一并命名以保持一一对应
const Color _kThumbBg = Color(0xFFFFEAF0); // 缩略图 / 推荐图底
const Color _kSoftPinkBg = Color(0xFFFFE8EE); // 空态、提示图章底
const Color _kPlaceholderBg = Color(0xFFFFF6EF); // 「暂无图片」底
const Color _kSheetImageBg = Color(0xFFFFDDE7); // 详情弹层图底
const Color _kDisabledBg = Color(0xFFF0ECE4); // Squishy 按钮 disabled
const Color _kDisabledText = Color(0xFF8B7164); // Squishy 按钮 disabled 文字


// ─────────────────────────────────────────────────────────────────────────────
// 16 道地道纯正中式家常菜品库（确保去除 2 道每日推荐后依然稳健有 10+ 道精选菜谱）
// ─────────────────────────────────────────────────────────────────────────────
const List<Map<String, dynamic>> _kChineseSeeds = [
  {
    'name': '经典秘制糖醋排骨',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.4',
    'time': '25分钟',
    'diff': '经典硬菜',
    'category': '招牌热炒',
    'desc': '酸甜浓郁，酥脆多汁，伴侣连吃三碗米饭的秘密法宝。',
    'imageUrl': 'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=500&q=80',
    'price': 22.0,
  },
  {
    'name': '家常可乐鸡翅',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.3',
    'time': '20分钟',
    'diff': '小白必会',
    'category': '招牌热炒',
    'desc': '可乐浓汁包裹，鸡翅软烂脱骨，咸甜适中超治愈。',
    'imageUrl': 'https://images.unsplash.com/photo-1527477378370-17d47bf1b18d?auto=format&fit=crop&w=500&q=80',
    'price': 20.0,
  },
  {
    'name': '妈妈牌番茄炒蛋',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.6',
    'time': '10分钟',
    'diff': '国民家常',
    'category': '招牌热炒',
    'desc': '沙瓤番茄炒出浓郁红汤，土鸡蛋金黄蓬松，盖饭一绝。',
    'imageUrl': 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=500&q=80',
    'price': 16.0,
  },
  {
    'name': '鲜香浓郁麻婆豆腐',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.2',
    'time': '15分钟',
    'diff': '川味下饭',
    'category': '招牌热炒',
    'desc': '牛肉碎煸香，豆腐滑嫩如布丁，花椒面麻香扑鼻热气腾腾。',
    'imageUrl': 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=500&q=80',
    'price': 16.0,
  },
  {
    'name': '暖胃玉米排骨汤',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.5',
    'time': '45分钟',
    'diff': '滋补温润',
    'category': '暖心热汤',
    'desc': '甜玉米清甜，胡萝卜软糯，慢炖排骨汤清甜暖胃。',
    'imageUrl': 'https://images.unsplash.com/photo-1547592166-23ac45744acd?auto=format&fit=crop&w=500&q=80',
    'price': 24.0,
  },
  {
    'name': '浓香秘制红烧肉',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.4',
    'time': '40分钟',
    'diff': '招牌拿手',
    'category': '招牌热炒',
    'desc': '三层五花肉冰糖炒色，小火慢煨肥而不腻、入口即化。',
    'imageUrl': 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=500&q=80',
    'price': 26.0,
  },
  {
    'name': '香辣水煮牛肉',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.3',
    'time': '25分钟',
    'diff': '爽辣过瘾',
    'category': '招牌热炒',
    'desc': '滑嫩牛肉片垫底豆芽，淋上一勺热滚滚的刀口辣椒油。',
    'imageUrl': 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=500&q=80',
    'price': 28.0,
  },
  {
    'name': '蒜蓉清炒时蔬',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.1',
    'time': '8分钟',
    'diff': '快手素菜',
    'category': '素菜',
    'desc': '翠绿时蔬大火爆炒，蒜粒金黄出香，清脆爽口又解腻。',
    'imageUrl': 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=500&q=80',
    'price': 14.0,
  },
  {
    'name': '经典宫保鸡丁',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.2',
    'time': '18分钟',
    'diff': '酸甜微辣',
    'category': '招牌热炒',
    'desc': '鸡丁滑嫩入味，花生米香脆可口，糊辣荔枝味型超地道。',
    'imageUrl': 'https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=500&q=80',
    'price': 20.0,
  },
  {
    'name': '爽口酸辣土豆丝',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.5',
    'time': '10分钟',
    'diff': '百吃不厌',
    'category': '素菜',
    'desc': '手切细丝爽脆可口，干辣椒香醋炝锅，酸辣开胃停不下来。',
    'imageUrl': 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=500&q=80',
    'price': 12.0,
  },
  {
    'name': '地道金汤酸菜鱼',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.4',
    'time': '25分钟',
    'diff': '开胃金汤',
    'category': '招牌热炒',
    'desc': '黑鱼片薄嫩如纸，老坛酸菜酸爽过瘾，金汤浓郁泡饭一绝。',
    'imageUrl': 'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=500&q=80',
    'price': 28.0,
  },
  {
    'name': '广式滑蛋牛肉',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.3',
    'time': '12分钟',
    'diff': '滑嫩鲜香',
    'category': '招牌热炒',
    'desc': '牛里脊鲜嫩多汁，蛋液滑嫩如果冻，清淡温和超营养。',
    'imageUrl': 'https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?auto=format&fit=crop&w=500&q=80',
    'price': 24.0,
  },
  {
    'name': '香菇慢炖土鸡',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.5',
    'time': '50分钟',
    'diff': '鲜美滋补',
    'category': '暖心热汤',
    'desc': '干香菇充分泡发，小火慢煨鸡肉香气扑鼻，汤清味厚。',
    'imageUrl': 'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=500&q=80',
    'price': 32.0,
  },
  {
    'name': '干煸肉末四季豆',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.2',
    'time': '15分钟',
    'diff': '焦香下饭',
    'category': '素菜',
    'desc': '四季豆煸出虎皮微皱，肉碎芽菜干香扑鼻，米饭绝配。',
    'imageUrl': 'https://images.unsplash.com/photo-1587314168485-3236d6710814?auto=format&fit=crop&w=500&q=80',
    'price': 18.0,
  },
  {
    'name': '滋补山药牛腩煲',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.4',
    'time': '45分钟',
    'diff': '软烂浓香',
    'category': '招牌热炒',
    'desc': '牛腩软烂多汁，铁棍山药粉糯清甜，暖心暖胃一煲搞定。',
    'imageUrl': 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=500&q=80',
    'price': 30.0,
  },
  {
    'name': '下饭鱼香肉丝',
    'source': 'xiachufang',
    'sourceLabel': '下厨房',
    'score': '9.3',
    'time': '16分钟',
    'diff': '经典川味',
    'category': '招牌热炒',
    'desc': '猪里脊肉丝滑嫩，木耳笋丝爽脆，酸甜微辣咸鲜回味无穷。',
    'imageUrl': 'https://images.unsplash.com/photo-1541832676-9b763b0239ab?auto=format&fit=crop&w=500&q=80',
    'price': 20.0,
  },
];

class DiscoverPage extends StatefulWidget {
  final VoidCallback onGoToOrdering;

  const DiscoverPage({super.key, required this.onGoToOrdering});

  @override
  State<DiscoverPage> createState() => _DiscoverPageState();
}

class _DiscoverPageState extends State<DiscoverPage> {
  final _searchCtrl = TextEditingController();

  /// 原生 `DiscoverUiState.query` + `searchDebounceMs = 300L`
  Timer? _debounce;
  String _query = '';
  bool _isSearching = false;

  /// 并发搜索的代次：用户改词后，旧的响应直接丢掉，避免「后到的旧结果」覆盖新结果。
  int _searchSeq = 0;

  /// 上一次真正搜过的关键词（同一个词不重复请求站点）。
  String _lastKeyword = '';

  /// 原生 `DiscoverUiState.errorMessage != null`（网络部分失败提示条）
  bool _partialError = false;

  /// 原生 `DiscoverUiState.results`
  List<Map<String, dynamic>> _results = [];

  /// 原生 `buildRecommendation()` 的取菜来源（发现页首屏空查询时的推荐位）
  List<Map<String, dynamic>> _library = [];
  int _recOffset = 0;


  /// 原生 `addedMenuItemNames`。这里用本地集合记录，**不写回菜谱 Map**：
  /// `supabase_api.dart:855` 的 `_recipeLibrary` 是 `const`，其内层 Map 不可变，
  /// 旧代码里的 `recipe['added'] = true` 会在运行时抛
  /// `Unsupported operation: Cannot modify unmodifiable map`。
  final Set<String> _addedNames = {};

  @override
  void initState() {
    super.initState();
    _loadLibrary();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadLibrary() async {
    // 直接注入 16 道纯正中式家常菜种子，免去网络与数据库延迟
    if (!mounted) return;
    setState(() => _library = List<Map<String, dynamic>>.from(_kChineseSeeds));
  }

  void _shuffleRecs() {
    HapticFeedback.lightImpact();
    setState(() {
      _recOffset = (_recOffset + 2) % _kChineseSeeds.length;
    });
    _showToast('已更新今日小饭桌推荐，精选菜谱已联动去重 🎲');
  }

  // 对应原生 onQueryChanged：立即更新 query（清 errorMessage），300ms 后再搜。
  void _onQueryChanged(String value) {
    setState(() {
      _query = value;
      _partialError = false;
    });
    _debounce?.cancel();
    _debounce = Timer(
      const Duration(milliseconds: 300),
      () => _performSearch(value),
    );
  }

  // 对应原生 performSearch / clearResults
  Future<void> _performSearch(String keyword) async {
    final String q = keyword.trim();
    if (q.isEmpty) {
      if (!mounted) return;
      _searchSeq++; // 让还在飞的请求作废
      _lastKeyword = '';
      setState(() {
        _results = <Map<String, dynamic>>[];
        _isSearching = false;
        _partialError = false;
      });
      return;
    }

    // 同一个词已经搜过就不重复打扰站点（「实时」= 每次新关键词都真去抓，而不是每次都重复抓同一个词）。
    if (q == _lastKeyword && _results.isNotEmpty) return;

    final int seq = ++_searchSeq;
    setState(() => _isSearching = true);

    // ① 实时抓下厨房搜索页：只取「成品图 + 名称」，手机里不落任何缓存。
    final List<XiachufangDish> hits = await XiachufangClient.search(q);
    if (!mounted || seq != _searchSeq) return; // 用户又改了关键词，丢掉这次结果

    if (hits.isNotEmpty) {
      setState(() {
        _results = <Map<String, dynamic>>[
          for (final XiachufangDish dish in hits) _liveRecipe(dish),
        ];
        _lastKeyword = q;
        _isSearching = false;
        _partialError = false;
      });
      _warmThumbs(_results);
      return;
    }

    // ② 站点限流 / 没命中：回退内置菜谱库（保持离线可用与老行为）
    final list = await AppState.instance.searchRemoteRecipes(q);
    if (!mounted || seq != _searchSeq) return;
    setState(() {
      _results = list;
      _lastKeyword = q;
      _isSearching = false;
      _partialError = (list.isEmpty && AppState.instance.error != null) ||
          XiachufangClient.lastSearchFailed;
    });
    _warmThumbs(list);
  }

  /// 结果一落地就把前几张图预热进磁盘缓存：用户翻到卡片时图已经在本地。
  ///
  /// 图床单张图要 2–10 s（实测），如果等卡片自己懒加载，就得一张一张排队；
  /// 并发预热 3 张，前几张基本能「先占位、随即淡入」。
  void _warmThumbs(List<Map<String, dynamic>> list) {
    final double dpr = MediaQuery.maybeOf(context)?.devicePixelRatio ?? 3;
    final List<String> urls = <String>[];
    for (final Map<String, dynamic> recipe in list) {
      final Object? raw = recipe['imageUrl'] ?? recipe['image_url'];
      if (raw is! String || raw.trim().isEmpty) continue;
      urls.add(CozyDishPhoto.thumbUrl(raw, cssWidth: 84, dpr: dpr));
      if (urls.length >= 8) break;
    }
    if (urls.isEmpty) return;
    unawaited(CozyImageCache.instance.prefetch(urls, concurrency: 3));
  }

  /// 实时抓来的菜 → 结果卡用的菜谱 Map（只带成品图与菜名，价格按时长/主料推算）。
  static Map<String, dynamic> _liveRecipe(XiachufangDish dish) {
    return <String, dynamic>{
      'name': dish.name,
      'imageUrl': dish.imageUrl,
      'category': '下厨房',
      'price': suggestDishPrice(dish.name),
      'desc': '来自下厨房的实时结果',
      'source': 'xiachufang',
      'fromIndex': true,
    };
  }

  bool _isAdded(Map<String, dynamic> recipe) {
    final name = _name(recipe);
    if (_addedNames.contains(name)) return true;
    // 原生 addedMenuItemNames：已在店铺的菜直接算「已加入」
    return AppState.instance.menu.any((item) => item.name == name);
  }

  // 对应原生 addToMenu(item) + 旧版页面的接入动作。
  Future<void> _addToShop(Map<String, dynamic> recipe) async {
    final name = _name(recipe);
    HapticFeedback.mediumImpact();

    if (_isAdded(recipe)) {
      // 原生："已在我的小店：${item.name}"
      _showToast('已在我的小店：$name');
      return;
    }

    final state = AppState.instance;
    final rawPrice = (recipe['price'] as num?)?.toDouble() ?? 12.0;
    // 原生 `DiscoverViewModel.kt:151-154` 的落位规则：菜谱分类能对上店铺已有分类就用它，
    // 否则落进店铺的第一个分类（= 店铺页分类卡的第一张，加完立刻看得见），
    // 一个分类都没有才用「未分类」。实现与来龙去脉见 `lib/data/category_placement.dart`。
    final String category = resolveDishCategory(
      shopCategories: state.menu.map((item) => item.category),
      recipeCategory: (recipe['category'] as String?) ?? '',
    );
    final ok = await state.addDish(
      name: name,
      // 原生：price <= 0 时用 12.0
      price: rawPrice <= 0 ? 12.0 : rawPrice,
      description: (recipe['desc'] as String?) ?? '',
      emoji: (recipe['emoji'] as String?) ?? '🍽️',
      category: category,
    );

    if (!mounted) return;
    if (ok) {
      setState(() {
        _addedNames.add(name);
      });
      // 原生："已加入我的小店：${item.name}"；补上分类名，用户不用去店铺里翻。
      _showToast('已加入我的小店：$name · 分类「$category」', actionLabel: '去点单');
    } else {
      _showToast(state.error ?? '添加失败，请确认是否为饲养员身份');
    }
  }

  // 原生 DiscoverToastSnackbar（L382-416）→ 统一改走玻璃 Toast：
  // 原生自绘的奶白胶囊 + 圆形 ✓ 徽，现在由 `showCozyToast` + GlassToastAction
  // 承担（底部同样的 dock 净空、同样的自动消失节奏），全 App 一套材质。
  void _showToast(String message, {String? actionLabel}) {
    showCozyToast(
      context,
      message,
      duration: const Duration(milliseconds: 2200),
      actionLabel: actionLabel,
      onAction: actionLabel == null ? null : widget.onGoToOrdering,
    );
  }

  // 原生 openVideoAppSearch（L807-861）：跳抖音 / 哔站客户端内搜索。
  // 本工程未依赖 url_launcher（也不允许新增依赖），无法拉起外部 App，故只提示。
  void _openVideo(String platform, String query) {
    _showToast('暂不支持跳转$platform，可在$platform里搜索「$query」');
  }

  // 原生 DiscoverDishDetailSheet（L863-909）
  Future<void> _showDishDetail(Map<String, dynamic> recipe) async {
    final theme = Theme.of(context);
    final name = _name(recipe);
    final subtitle = _subtitle(recipe);
    final price = ((recipe['price'] as num?) ?? 12).toDouble();

    await showModalBottomSheet<void>(
      context: context,
      // 【真机修正】这里原本没开 `isScrollControlled`，`showModalBottomSheet` 默认
      // 只给 9/16 屏高，而弹层内容（图 190 + 菜名 + 描述 + 售价 + 两个按钮）约 460dp，
      // 于是底部「关闭 / 加入我的小店」被挤出屏外，而且弹层到顶就只能回缩、不能上拉。
      // 开成可滚动 + 包一层滚动容器后，内容按需撑高、超出时可滚，按钮永远可达。
      isScrollControlled: true,
      // 【玻璃】弹层底色交给 `CozyGlassSheet`，这里必须透明。
      backgroundColor: Colors.transparent,
      // 遮罩调浅：背后太黑 -> 玻璃没有东西可折，只会变灰雾。
      barrierColor: Colors.black.withValues(alpha: 0.18),
      showDragHandle: false,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (sheetContext) {
        return ListenableBuilder(
          listenable: AppState.instance,
          builder: (context, _) {
            final added = _isAdded(recipe);
            return CozyGlassSheet(
              padding: const EdgeInsets.fromLTRB(18, 4, 18, 20),
              child: SafeArea(
                top: false,
                child: SingleChildScrollView(
                  child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: double.infinity,
                    height: 190,
                    clipBehavior: Clip.antiAlias,
                    decoration: BoxDecoration(
                      color: _kSheetImageBg,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: _DishImageOrPlaceholder(
                      recipe: recipe,
                      fit: BoxFit.cover,
                      cssWidth: 320,
                      emojiSize: 56,
                    ),
                  ),
                  const SizedBox(height: 14),
                  Text(
                    name,
                    style: theme.textTheme.headlineSmall?.copyWith(
                      color: CozyPalette.onSurface,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 14),
                  Text(
                    subtitle.isEmpty ? '暂无描述' : subtitle,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: CozyPalette.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 14),
                  CozyPill(
                    text: '建议售价 ¥${price.toStringAsFixed(2)}',
                    color: _discoverPrimary,
                  ),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      Expanded(
                        child: TextButton(
                          onPressed: () => Navigator.of(sheetContext).maybePop(),
                          child: const Text('关闭'),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: FilledButton(
                          onPressed: added ? null : () => _addToShop(recipe),
                          style: FilledButton.styleFrom(
                            backgroundColor: _discoverPrimary,
                            foregroundColor: Colors.white,
                            disabledBackgroundColor: _kDisabledBg,
                            disabledForegroundColor: _kDisabledText,
                            minimumSize: const Size(0, 44),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                            ),
                          ),
                          child: Text(added ? '已在我的小店' : '加入我的小店'),
                        ),
                      ),
                    ],
                  ),
                ],
                ),
              ),
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return CozyPage(
      // 原生 `CozyPage(decorative = false)`（DiscoverScreen.kt L93）
      decorative: false,
      child: ListenableBuilder(
        listenable: AppState.instance,
        builder: (context, _) {
          final items = _buildItems(context);
          return ListView.separated(
            // 原生 contentPadding(start 20, top 24, end 20, bottom 172) → 底栏收敛为 clearance
            padding: EdgeInsets.fromLTRB(20, 24, 20, CozyDock.clearanceOf(context)),
            // 原生 verticalArrangement = Arrangement.spacedBy(24.dp)，这里收到 16 让一屏装得下
            separatorBuilder: (_, _) => const SizedBox(height: 16),
            itemCount: items.length,
            itemBuilder: (_, index) => items[index],
          );
        },
      ),
    );
  }

  /// 原生 LazyColumn 的 item 顺序，一一对应。
  List<Widget> _buildItems(BuildContext context) {
    final theme = Theme.of(context);
    final recommendations = _buildRecommendations();

    return <Widget>[
      // ① 标题块（L130-149）
      _buildTitleBlock(theme),
      // ② 搜索框（L151-160）
      _buildSearchField(theme),
      // ③ 推荐行（L162-171，仅空查询且推荐非空）
      if (_query.trim().isEmpty && recommendations.isNotEmpty)
        _buildRecommendationRow(context, recommendations),
      // ④ 部分网络错误（L173-181）
      if (_partialError)
        Text(
          '部分网络结果暂不可用，先展示可用结果',
          style: theme.textTheme.bodySmall?.copyWith(
            color: CozyPalette.onSurfaceVariant,
          ),
        ),
      // ⑤ 三态分支（L183-232）
      ..._buildStateItems(context),
    ];
  }

  // 原生 L130-149
  Widget _buildTitleBlock(ThemeData theme) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '发现 - 探索新菜谱',
          style: TextStyle(
            color: _discoverPrimary,
            fontSize: 26,
            height: 34 / 26,
            fontWeight: FontWeight.w900,
            letterSpacing: 0,
          ),
        ),
        const SizedBox(height: 6),
        Row(
          children: [
            const Text(
              '搜一搜，给你们的小饭桌加点新菜 ',
              style: TextStyle(
                color: CozyPalette.onSurfaceVariant,
                fontSize: 15,
                height: 22 / 15,
              ),
            ),
            Image.asset(
              'assets/images/cooking.png',
              width: 20,
              height: 20,
              errorBuilder: (context, error, stackTrace) => const Text('🍳'),
            ),
          ],
        ),
      ],
    );
  }

  // 原生 StitchDiscoverSearchField（L562-613）：60 高、全圆角、无清除按钮
  
  Widget _buildHotTags() {
    final tags = [
      ('糖醋排骨', 'assets/images/meat.png', '🍖'),
      ('可乐鸡翅', 'assets/images/poultry.png', '🍗'),
      ('番茄炒蛋', 'assets/images/tomato.png', '🍅'),
      ('麻婆豆腐', 'assets/images/pepper.png', '🌶️'),
      ('玉米排骨汤', 'assets/images/pot.png', '🍲'),
    ];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      physics: const BouncingScrollPhysics(),
      child: Row(
        children: [
          const Text(
            '灵感热搜：',
            style: TextStyle(
              color: CozyPalette.onSurfaceVariant,
              fontSize: 11,
              fontWeight: FontWeight.bold,
            ),
          ),
          for (final t in tags)
            Padding(
              padding: const EdgeInsets.only(right: 6),
              child: GestureDetector(
                onTap: () {
                  _searchCtrl.text = t.$1;
                  _onQueryChanged(t.$1);
                },
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(999),
                    border: Border.all(
                      color: _discoverCardBorder.withValues(alpha: 0.25),
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        t.$1,
                        style: const TextStyle(
                          color: CozyPalette.onSurface,
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Image.asset(
                        t.$2,
                        width: 14,
                        height: 14,
                        errorBuilder: (context, error, stackTrace) => Text(t.$3, style: const TextStyle(fontSize: 10)),
                      ),
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildSearchField(ThemeData theme) {
    final placeholder = _query.trim().isEmpty
        ? '想吃点什么？例如：糖醋排骨...'
        : _kSearchPlaceholder;

    return Container(
      height: 54,
      decoration: BoxDecoration(
        color: _discoverInput,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: _discoverCardBorder.withValues(alpha: 0.32),
          width: 2,
        ),
      ),
      child: Row(
        children: [
          const SizedBox(width: 22),
          const Icon(Icons.search, size: 22, color: CozyPalette.outline),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              controller: _searchCtrl,
              onChanged: _onQueryChanged,
              onSubmitted: (value) {
                _debounce?.cancel();
                _performSearch(value);
              },
              maxLines: 1,
              textInputAction: TextInputAction.search,
              cursorColor: _discoverPrimary,
              style: const TextStyle(
                color: CozyPalette.onSurface,
                fontSize: 17,
                height: 25 / 17,
              ),
              decoration: InputDecoration(
                isCollapsed: true,
                border: InputBorder.none,
                hintText: placeholder,
                hintStyle: const TextStyle(
                  color: CozyPalette.outline,
                  fontSize: 17,
                  height: 25 / 17,
                ),
              ),
            ),
          ),
          const SizedBox(width: 22),
        ],
      ),
    );
  }

  // 原生三态分支（L183-232）
  List<Widget> _buildStateItems(BuildContext context) {
    final theme = Theme.of(context);

    if (_query.trim().isEmpty) {
      // 1. 获取当前每日推荐的 2 道菜名，用于严格去重
      final recs = _buildRecommendations();
      final recNames = recs.map((r) => _name(r.recipe)).toSet();

      // 2. 从 16 道种子库中彻底排除每日推荐中的 2 道菜，严格取 10 道精选菜谱！
      final pool = _library.where((r) {
        final img = r['imageUrl'] as String?;
        final hasImg = img != null && img.trim().isNotEmpty;
        final notInRec = !recNames.contains(_name(r));
        return hasImg && notInRec;
      }).toList();

      final featuredTen = pool.take(10).toList();

      return <Widget>[
        // 常搜热搜词标签
        _buildHotTags(),
        _buildSearchPrompt(theme),
        // 精选 10 道菜标题
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              '精选菜谱 (10道)',
              style: TextStyle(
                color: _discoverPrimary,
                fontSize: 17,
                fontWeight: FontWeight.w900,
              ),
            ),
            Text(
              '下厨房精选 · 已去重',
              style: theme.textTheme.bodySmall?.copyWith(
                color: CozyPalette.onSurfaceVariant,
              ),
            ),
          ],
        ),
        for (final recipe in featuredTen)
          _buildResultCard(context, recipe),
      ];
    }

    if (_isSearching) {
      return <Widget>[
        Text(
          '正在搜索菜品...',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: CozyPalette.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 12),
        // 搜索期间铺骨架屏（形状对齐结果卡），比一行转圈更不像「卡住了」
        const CozySearchSkeletonList(),
      ];
    }

    if (_results.isEmpty) {
      return <Widget>[_buildEmptyState(theme)];
    }

    // 原生 CozyMotionVisibility(delayMillis = index.coerceAtMost(5) * 28)
    return <Widget>[
      for (var index = 0; index < _results.length; index++)
        _CozyMotionVisibility(
          delayMillis: (index > 5 ? 5 : index) * 28,
          child: _buildResultCard(context, _results[index]),
        ),
    ];
  }

  // 原生 DiscoverSearchPrompt（L669-705）：原生是竖排居中大卡，太占高度，这里改横排一行
  Widget _buildSearchPrompt(ThemeData theme) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _discoverCreamCard.withValues(alpha: 0.84),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: _discoverCardBorder.withValues(alpha: 0.32),
          width: 2,
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: _kSoftPinkBg,
              borderRadius: BorderRadius.circular(18),
            ),
            child: const Icon(Icons.search, size: 24, color: _discoverPrimary),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '搜一搜新菜谱',
                  style: theme.textTheme.titleMedium?.copyWith(
                    color: CozyPalette.onSurface,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  '输入菜名、食材或做法，找到合适的菜后加入我的小店。',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: CozyPalette.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // 原生 DiscoverEmptyState（L707-747）：同样由竖排大卡改横排，省掉约 120dp 高度
  Widget _buildEmptyState(ThemeData theme) {
    final query = _query.trim();
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _discoverCreamCard.withValues(alpha: 0.92),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: _discoverCardBorder.withValues(alpha: 0.45),
          width: 2,
        ),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              Container(
                width: 48,
                height: 48,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: _kSoftPinkBg,
                  borderRadius: BorderRadius.circular(18),
                ),
                child: Text(
                  '菜',
                  style: theme.textTheme.bodyLarge?.copyWith(
                    color: _discoverPrimary,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '没有找到相关菜品',
                      style: theme.textTheme.titleMedium?.copyWith(
                        color: CozyPalette.onSurface,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '换个更准确的菜名试试，或者直接加入我的小店后再编辑',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: CozyPalette.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (query.isNotEmpty) ...[
            const SizedBox(height: 10),
            _RecipeVideoLinkIcons(
              query: query,
              compact: false,
              onOpen: _openVideo,
            ),
          ],
        ],
      ),
    );
  }

  /// 【搜一搜】本地索引的结果卡：只有**成品图 + 菜名**（用户要求），
  /// 点一下进菜品详情，在那里决定要不要加入我的小店。
  Widget _buildIndexResultCard(
    BuildContext context,
    Map<String, dynamic> recipe,
  ) {
    final name = _name(recipe);
    return _Pressable(
      onTap: () => _showDishDetail(recipe),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: _discoverCard,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: _discoverCardBorder, width: 2),
        ),
        child: Row(
          children: [
            SizedBox(
              width: 96,
              height: 96,
              child: Container(
                clipBehavior: Clip.antiAlias,
                decoration: BoxDecoration(
                  color: _kThumbBg,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: _discoverCardBorder, width: 2),
                ),
                child: _DishImageOrPlaceholder(
                  recipe: recipe,
                  fit: BoxFit.cover,
                  cssWidth: 96,
                  emojiSize: 34,
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Text(
                  name,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: CozyPalette.onPrimaryContainer,
                    fontSize: 18,
                    height: 24 / 18,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // 原生 DiscoverResultCard（L418-515）
  Widget _buildResultCard(BuildContext context, Map<String, dynamic> recipe) {
    // 【搜一搜】索引结果只放成品图 + 名称（用户要求），点卡片进详情再决定要不要加店铺。
    if (recipe['fromIndex'] == true) {
      return _buildIndexResultCard(context, recipe);
    }

    final theme = Theme.of(context);
    final name = _name(recipe);
    final subtitle = _subtitle(recipe);
    final added = _isAdded(recipe);

    return _Pressable(
      onTap: () => _showDishDetail(recipe),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: _discoverCard,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: _discoverCardBorder, width: 2),
        ),
        child: Row(
          children: [
            SizedBox(
              width: 84,
              height: 84,
              child: Container(
                clipBehavior: Clip.antiAlias,
                decoration: BoxDecoration(
                  color: _kThumbBg,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: _discoverCardBorder, width: 2),
                ),
                child: _DishImageOrPlaceholder(
                  recipe: recipe,
                  fit: BoxFit.cover,
                  cssWidth: 96,
                  emojiSize: 34,
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: CozyPalette.onPrimaryContainer,
                        fontSize: 19,
                        height: 25 / 19,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        _DiscoverSourceChip(text: _displaySourceName(recipe)),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            subtitle.isEmpty ? '暂无描述' : subtitle,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: CozyPalette.onSurfaceVariant,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    _SquishyDiscoverButton(
                      text: added ? '已在我的小店' : '加入我的小店',
                      enabled: !added,
                      onTap: () => _addToShop(recipe),
                    ),
                    const SizedBox(height: 7),
                    _RecipeVideoLinkIcons(
                      query: name,
                      compact: true,
                      onOpen: _openVideo,
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // 原生 DiscoverRecommendationRow（L247-312）
  Widget _buildRecommendationRow(
    BuildContext context,
    List<_DiscoverRecommendation> recommendations,
  ) {
    final children = <Widget>[];
    for (var index = 0; index < recommendations.length; index++) {
      if (index > 0) children.add(const SizedBox(width: 12));
      children.add(
        Expanded(
          child: _CozyMotionVisibility(
            delayMillis: index * 40,
            child: _buildRecommendationCard(context, recommendations[index]),
          ),
        ),
      );
    }
    if (recommendations.length == 1) {
      children.add(const SizedBox(width: 12));
      children.add(const Expanded(child: SizedBox.shrink()));
    }

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              '🌟 今日小饭桌精选',
              style: TextStyle(
                color: CozyPalette.onSurface,
                fontSize: 13,
                fontWeight: FontWeight.w900,
              ),
            ),
            GestureDetector(
              onTap: _shuffleRecs,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: CozyPalette.primary.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Image.asset(
                      'assets/images/die.png',
                      width: 14,
                      height: 14,
                      errorBuilder: (context, error, stackTrace) => const Text('🎲', style: TextStyle(fontSize: 10)),
                    ),
                    const SizedBox(width: 4),
                    const Text(
                      '换一换推荐',
                      style: TextStyle(
                        color: _discoverPrimary,
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: children),
      ],
    );
  }

  // 原生推荐卡（L313-361）
  Widget _buildRecommendationCard(
    BuildContext context,
    _DiscoverRecommendation recommendation,
  ) {
    final theme = Theme.of(context);
    final recipe = recommendation.recipe;
    final name = _name(recipe);
    final added = _isAdded(recipe);

    return _Pressable(
      onTap: () => _showDishDetail(recipe),
      child: Container(
        width: double.infinity,
        constraints: const BoxConstraints(minHeight: 214),
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: _discoverCreamCard.withValues(alpha: 0.96),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: _discoverCardBorder.withValues(alpha: 0.45),
            width: 2,
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: double.infinity,
              height: 68,
              clipBehavior: Clip.antiAlias,
              decoration: BoxDecoration(
                color: _kThumbBg,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(
                  color: _discoverCardBorder.withValues(alpha: 0.34),
                  width: 1,
                ),
              ),
              child: _DishImageOrPlaceholder(
                recipe: recipe,
                fit: BoxFit.cover,
                cssWidth: 200,
                emojiSize: 30,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              recommendation.title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.labelLarge?.copyWith(
                color: _discoverPrimary,
                fontWeight: FontWeight.w900,
              ),
            ),
            const SizedBox(height: 5),
            // 固定两行菜名位：两张推荐卡菜名长短不一（「法式巴斯克乳酪蛋糕」两行 /
            // 「暖胃浓汤番茄牛腩」一行），留出同样高度的槽位，按钮与 chip 才能左右对齐
            SizedBox(
              height: 40,
              child: Text(
                name,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: CozyPalette.onSurface,
                  fontSize: 15,
                  height: 20 / 15,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
            const SizedBox(height: 2),
            // 固定一行副标题位，同上
            SizedBox(
              height: 18,
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  recommendation.subtitle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: CozyPalette.onSurfaceVariant,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10),
            _SquishyDiscoverButton(
              text: added ? '已在店铺' : '加入店铺',
              enabled: !added,
              onTap: () => _addToShop(recipe),
            ),
            const SizedBox(height: 8),
            // 原生 RecommendationVideoLinks：文案「抖音」/「哔站」，间距 6
            // 卡片只有 ~140dp 内宽，用 dense 让两个 chip 排在一行而不是折成两行
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                _VideoSearchChip(
                  text: '抖音',
                  dense: true,
                  onTap: () => _openVideo('抖音', name),
                ),
                _VideoSearchChip(
                  text: '哔站',
                  dense: true,
                  onTap: () => _openVideo('哔站', name),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // 原生 DiscoverViewModel.buildRecommendation()：今日推荐 / 减脂推荐（空查询时展示）
  // Flutter 数据层没有对应接口，这里从本地菜谱库里就地取两条，文案与原生一致。
  List<_DiscoverRecommendation> _buildRecommendations() {
    final pool = _library.where((r) {
      final img = r['imageUrl'] as String?;
      return img != null && img.trim().isNotEmpty;
    }).toList();
    if (pool.length < 2) return const <_DiscoverRecommendation>[];

    final item1 = pool[_recOffset % pool.length];
    final item2 = pool[(_recOffset + 1) % pool.length];

    return <_DiscoverRecommendation>[
      _DiscoverRecommendation(
        title: '今日推荐',
        subtitle: '每日随机更新',
        recipe: item1,
      ),
      _DiscoverRecommendation(
        title: '减脂推荐',
        subtitle: '轻一点，也很好吃',
        recipe: item2,
      ),
    ];
  }

  static String _name(Map<String, dynamic> recipe) =>
      (recipe['name'] as String?) ?? '';

  static String _subtitle(Map<String, dynamic> recipe) {
    final raw = (recipe['subtitle'] as String?) ?? (recipe['desc'] as String?) ?? '';
    return raw.trim();
  }

  // 原生 String.displaySourceName()（L794-805）
  static String _displaySourceName(Map<String, dynamic> recipe) {
    final source =
        (recipe['sourceLabel'] as String?) ?? (recipe['source'] as String?) ?? 'builtin';
    switch (source) {
      case 'xiachufang':
        return '下厨房';
      case 'bimissing':
        return '中文菜谱';
      case 'bing':
        return '网络图片';
      case 'builtin':
        return '下厨房';
      case 'tian':
        return '天行';
      case 'local':
        return '我的小店';
      case 'menu':
        return '我的小店';
      default:
        return '天行';
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// 私有组件（对应原生 DiscoverScreen.kt 内的私有 @Composable）
// ─────────────────────────────────────────────────────────────────────────────

class _DiscoverRecommendation {
  const _DiscoverRecommendation({
    required this.title,
    required this.subtitle,
    required this.recipe,
  });

  final String title;
  final String subtitle;
  final Map<String, dynamic> recipe;
}

/// 原生：`Modifier.scale(按下 0.98f)`（卡片）/ `0.96f`（按钮）+ 点击回调。
class _Pressable extends StatefulWidget {
  const _Pressable({
    required this.child,
    this.onTap,
    this.pressedScale = 0.98,
  });

  final Widget child;
  final VoidCallback? onTap;
  final double pressedScale;

  @override
  State<_Pressable> createState() => _PressableState();
}

class _PressableState extends State<_Pressable> {
  bool _down = false;

  void _setDown(bool value) {
    if (!mounted || _down == value) return;
    setState(() => _down = value);
  }

  @override
  Widget build(BuildContext context) {
    final tappable = widget.onTap != null;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: tappable ? (_) => _setDown(true) : null,
      onTapUp: tappable ? (_) => _setDown(false) : null,
      onTapCancel: tappable ? () => _setDown(false) : null,
      onTap: widget.onTap,
      child: AnimatedScale(
        scale: _down ? widget.pressedScale : 1.0,
        duration: const Duration(milliseconds: 140),
        curve: Curves.easeOut,
        child: widget.child,
      ),
    );
  }
}

/// 原生 `CozyMotionVisibility(delayMillis = ...)`：按延迟做一次淡入 + 上浮。
class _CozyMotionVisibility extends StatefulWidget {
  const _CozyMotionVisibility({
    required this.child,
    required this.delayMillis,
  });

  final Widget child;
  final int delayMillis;

  @override
  State<_CozyMotionVisibility> createState() => _CozyMotionVisibilityState();
}

class _CozyMotionVisibilityState extends State<_CozyMotionVisibility> {
  bool _visible = false;

  @override
  void initState() {
    super.initState();
    if (widget.delayMillis <= 0) {
      _visible = true;
      return;
    }
    Future<void>.delayed(Duration(milliseconds: widget.delayMillis), () {
      if (!mounted) return;
      setState(() => _visible = true);
    });
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedOpacity(
      opacity: _visible ? 1 : 0,
      duration: const Duration(milliseconds: 260),
      curve: Curves.easeOut,
      child: AnimatedSlide(
        offset: _visible ? Offset.zero : const Offset(0, 0.06),
        duration: const Duration(milliseconds: 320),
        curve: Curves.easeOutCubic,
        child: widget.child,
      ),
    );
  }
}

/// 原生 DiscoverSourceChip（L615-632）
class _DiscoverSourceChip extends StatelessWidget {
  const _DiscoverSourceChip({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final isMine = text == '我的小店';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: isMine ? CozyPalette.primaryContainer : _discoverPrimary,
        borderRadius: BorderRadius.circular(999),
        border: isMine
            ? Border.all(color: _discoverPrimary, width: 1)
            : null,
      ),
      child: Text(
        text,
        maxLines: 1,
        style: TextStyle(
          color: isMine ? CozyPalette.onPrimaryContainer : Colors.white,
          fontSize: 10,
          height: 14 / 10,
          fontWeight: FontWeight.w900,
        ),
      ),
    );
  }
}

/// 原生 SquishyDiscoverButton（L634-667）：按下缩 0.96，minHeight 38。
class _SquishyDiscoverButton extends StatelessWidget {
  const _SquishyDiscoverButton({
    required this.text,
    required this.enabled,
    this.onTap,
  });

  final String text;
  final bool enabled;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return _Pressable(
      onTap: enabled ? onTap : null,
      pressedScale: 0.96,
      child: Container(
        constraints: const BoxConstraints(minHeight: 34),
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 7),
        decoration: BoxDecoration(
          color: enabled ? _discoverPrimary : _kDisabledBg,
          borderRadius: BorderRadius.circular(999),
        ),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: enabled ? Colors.white : _kDisabledText,
                fontWeight: FontWeight.w900,
              ),
        ),
      ),
    );
  }
}

/// 原生 RecipeVideoLinkIcons（L749-770）+ VideoSearchChip（L772-792）
class _RecipeVideoLinkIcons extends StatelessWidget {
  const _RecipeVideoLinkIcons({
    required this.query,
    required this.onOpen,
    this.compact = true,
  });

  final String query;
  final void Function(String platform, String query) onOpen;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    // 原生用 Row；这里换 Wrap，窄屏（≤330dp 逻辑宽）时避免 Flutter 的黄黑溢出条
    return Wrap(
      spacing: 8,
      runSpacing: 6,
      alignment: compact ? WrapAlignment.start : WrapAlignment.center,
      children: [
        _VideoSearchChip(
          text: compact ? '抖音视频' : '去抖音看看',
          onTap: () => onOpen('抖音', query),
        ),
        _VideoSearchChip(
          text: compact ? '哔站视频' : '去哔站看看',
          onTap: () => onOpen('哔站', query),
        ),
      ],
    );
  }
}

class _VideoSearchChip extends StatelessWidget {
  const _VideoSearchChip({
    required this.text,
    required this.onTap,
    this.dense = false,
  });

  final String text;
  final VoidCallback onTap;
  final bool dense;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: Container(
        padding: EdgeInsets.symmetric(
          horizontal: dense ? 7 : 10,
          vertical: dense ? 4 : 7,
        ),
        decoration: BoxDecoration(
          color: CozyPalette.surface,
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: _discoverCardBorder.withValues(alpha: 0.38),
            width: 1,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.play_circle_outline,
              size: dense ? 12 : 15,
              color: _discoverPrimary,
            ),
            SizedBox(width: dense ? 3 : 5),
            Text(
              text,
              maxLines: 1,
              style: TextStyle(
                color: _discoverPrimary,
                fontSize: dense ? 10 : 12,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 原生 DishImageOrPlaceholder（L911-946）
///
/// 原生这里放 Coil `AsyncImage(imageUrl)`；Flutter 侧菜谱没有图片 URL，
/// 数据层把 `emoji` 当作图片载体（`AppState.addDish(emoji: → imageUrl)`），
/// 因此：http 图片 → 网络图；无 URL 但有 emoji → 居中放大 emoji；两者皆无 → 原生同款「暂无图片」。
class _DishImageOrPlaceholder extends StatelessWidget {
  const _DishImageOrPlaceholder({
    required this.recipe,
    required this.fit,
    required this.emojiSize,
    this.cssWidth = 84,
  });

  final Map<String, dynamic> recipe;
  final BoxFit fit;
  final double emojiSize;

  /// 控件在界面上占的宽度（dp）—— 交给 [CozyDishPhoto] 决定要多大、解码多大。
  final double cssWidth;

  @override
  Widget build(BuildContext context) {
    final url = (recipe['imageUrl'] as String?) ?? (recipe['image_url'] as String?);
    final emoji = (recipe['emoji'] as String?) ?? '';

    if (url != null && (url.startsWith('http://') || url.startsWith('https://'))) {
      // 走带磁盘缓存 / 按尺寸取图 / 先占位后淡入的统一入口（见 CozyDishPhoto）。
      // 占位色用容器自己的米色，避免「图没到时先闪一块别的颜色」。
      return CozyDishPhoto(
        url: url,
        cssWidth: cssWidth,
        fit: fit,
        placeholderColor: _kThumbBg,
        fallback: _placeholder(context),
      );
    }
    if (emoji.trim().isNotEmpty) {
      return Center(
        child: Text(emoji, style: TextStyle(fontSize: emojiSize)),
      );
    }
    return _placeholder(context);
  }

  Widget _placeholder(BuildContext context) {
    return Container(
      color: _kPlaceholderBg,
      alignment: Alignment.center,
      child: Text(
        '暂无图片',
        style: Theme.of(context).textTheme.labelMedium?.copyWith(
              color: _discoverPrimary,
              fontWeight: FontWeight.w700,
            ),
      ),
    );
  }

  
}
