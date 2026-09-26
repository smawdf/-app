import 'dart:convert';

import 'package:flutter/services.dart' show rootBundle;

/// 本地菜谱大索引：把「菜名 + 成品图」直接查给发现页搜索用。
///
/// 数据来源与合规说明：
/// - 索引由 `_tools/crawl_xiachufang_index.ps1` 按 robots.txt 的
///   `Crawl-delay: 10` 与 `Allow: /category/*/?ref=*` 抓取下厨房**类目页**的
///   「成品菜名 + 封面成品图」；站点禁用的 `/*keyword=*` 与 `/search/` 一律未碰，
///   搜索完全在本地做。
/// - 结果只展示**成品图 + 菜名**（用户要求），图片走 `i*.chuimg.com` 直链热链。
/// - 仅供自用学习，不做商用；站点若加防盗链则热链会失效（此时会退回占位图）。
///
/// 索引文件是 `assets/data/dish_index.json`，紧凑格式（省体积）：
/// `[["菜名","图片URL","类目"], ...]`
class DishIndexEntry {
  const DishIndexEntry({
    required this.name,
    required this.image,
    required this.category,
  });

  final String name;
  final String image;
  final String category;

  /// 依据类目/菜名给一个合理售价（下厨房只有菜谱，没有价格）。
  int get suggestedPrice => suggestDishPrice(name, category);
}

/// 给抓来的菜谱推一个售价：先看贵价食材，再看类目/主料。
int suggestDishPrice(String name, String category) {
  final String key = '$category $name';
  if (RegExp(r'鲍鱼|龙虾|帝王蟹|海参|和牛|牛排|羊排|佛跳墙').hasMatch(key)) return 58;
  if (RegExp(r'虾|蟹|鱼|排骨|牛|羊|鸡翅|鸡腿|鸭|肉').hasMatch(key)) return 38;
  if (RegExp(r'汤|羹|煲|砂锅|炖').hasMatch(key)) return 32;
  if (RegExp(r'面|粉|饭|粥|饺|馄饨|饼|馒|包|馍').hasMatch(key)) return 22;
  if (RegExp(r'饮|奶茶|咖啡|果汁|酸梅|豆浆|冰|茶|酒').hasMatch(key)) return 16;
  if (RegExp(r'蛋糕|甜|布丁|慕斯|派|酥|饼干|吐司|面包|奶').hasMatch(key)) return 26;
  if (RegExp(r'沙拉|凉菜|凉拌|拌菜').hasMatch(key)) return 18;
  if (RegExp(r'素|青菜|菜|豆腐|菌|菇|蛋').hasMatch(key)) return 20;
  return 28;
}

/// 索引入口（静态单例，全 App 共用一份）。
class DishIndex {
  DishIndex._();

  static List<DishIndexEntry> _entries = const <DishIndexEntry>[];
  static bool _loaded = false;
  static Future<void>? _loading;
  static Object? _error;

  static const String assetPath = 'assets/data/dish_index.json';

  static bool get isReady => _loaded && _entries.isNotEmpty;
  static int get length => _entries.length;
  static Object? get error => _error;

  /// 懒加载一次；重复调用共用同一个 Future。
  static Future<void> ensureLoaded() {
    if (_loaded) return Future<void>.value();
    return _loading ??= _load();
  }

  static Future<void> _load() async {
    try {
      final String raw = await rootBundle.loadString(assetPath);
      final List<dynamic> list = jsonDecode(raw) as List<dynamic>;
      final List<DishIndexEntry> parsed = <DishIndexEntry>[];
      for (final dynamic row in list) {
        if (row is! List || row.length < 2) continue;
        final String name = (row[0] as String?)?.trim() ?? '';
        final String image = (row[1] as String?)?.trim() ?? '';
        if (name.isEmpty || image.isEmpty) continue;
        parsed.add(
          DishIndexEntry(
            name: name,
            image: image,
            category: row.length > 2 ? ((row[2] as String?) ?? '') : '',
          ),
        );
      }
      _entries = parsed;
      _loaded = true;
    } catch (e) {
      // 索引缺失不应该让发现页崩：退化成「只有内置菜谱库」的老行为。
      _error = e;
      _entries = const <DishIndexEntry>[];
      _loaded = true;
    } finally {
      _loading = null;
    }
  }

  /// 本地搜索：完全命中 > 前缀命中 > 包含，命中越靠前、名字越短排越前。
  static List<DishIndexEntry> search(String query, {int limit = 60}) {
    final String q = query.trim();
    if (q.isEmpty || _entries.isEmpty) return const <DishIndexEntry>[];
    final List<DishIndexEntry> exact = <DishIndexEntry>[];
    final List<DishIndexEntry> prefix = <DishIndexEntry>[];
    final List<DishIndexEntry> contains = <DishIndexEntry>[];
    for (final DishIndexEntry e in _entries) {
      if (e.name == q) {
        exact.add(e);
      } else if (e.name.startsWith(q)) {
        prefix.add(e);
      } else if (e.name.contains(q)) {
        contains.add(e);
      }
      if (exact.length + prefix.length + contains.length >= limit * 4) break;
    }
    int byName(DishIndexEntry a, DishIndexEntry b) {
      final int c = a.name.length.compareTo(b.name.length);
      return c != 0 ? c : a.name.compareTo(b.name);
    }

    exact.sort(byName);
    prefix.sort(byName);
    contains.sort(byName);
    final List<DishIndexEntry> out = <DishIndexEntry>[
      ...exact,
      ...prefix,
      ...contains,
    ];
    return out.length > limit ? out.sublist(0, limit) : out;
  }
}
