// 发现页「加入我的小店」时，新菜品该落进哪个分类。
//
// 【为什么单独拎出来】原生 `DiscoverViewModel.addToMenu`
// （D:\kaifa\myapp\OrderDisk\app\src\main\java\com\myorderapp\ui\discover\DiscoverViewModel.kt:151-154）
// 有一条明确的落位规则：
//
//     val currentCategories = _uiState.value.categories
//     val category = currentCategories.firstOrNull {
//         it.equals(item.category.trim(), ignoreCase = true)
//     } ?: currentCategories.firstOrNull() ?: "未分类"
//
// Flutter 移植版当时把 `category` 整个丢了（`_addToShop` 调 `addDish` 时没传），
// `SupabaseApi.createMenuItem` 会把空串规范化成「未分类」，于是**从发现页加进来的菜
// 永远落在「未分类」**；而店铺页「分类管理」的分类卡是按分类名升序排的，
// 「未分类」（U+672A）排在「招牌必吃」（U+62DB）、「暖心硬菜」（U+6696）之后，
// 也就是永远排在最后一屏外——用户加完菜在店铺首页找不到它，
// 只能去下面的菜品列表里翻。
//
// 规则本身很小，但它是「分类归属」的唯一定义处，所以拆成纯函数：
//   ① 菜谱自带分类与店铺已有分类同名（忽略大小写）→ 用它（保留店铺里的写法）
//   ② 否则 → 店铺的第一个分类（与店铺页分类卡的首位一致，保证加完就看得见）
//   ③ 店铺一个分类都没有 → 「未分类」（原生同为兜底值）
library;

/// 按原生规则算出新菜品该落的分类。
///
/// [shopCategories] 是店铺当前已有的分类（调用方负责去重；
/// 这里会再按分类名升序排一次，与店铺页「分类管理」的卡片顺序一致）。
/// [recipeCategory] 是菜谱/搜索结果自带的分类，可为空。
String resolveDishCategory({
  required Iterable<String> shopCategories,
  String recipeCategory = '',
}) {
  final List<String> known = shopCategories
      .map((String c) => c.trim())
      .where((String c) => c.isNotEmpty)
      .toSet()
      .toList()
    ..sort();

  final String wanted = recipeCategory.trim();
  if (wanted.isNotEmpty) {
    for (final String category in known) {
      if (category.toLowerCase() == wanted.toLowerCase()) return category;
    }
  }
  return known.isNotEmpty ? known.first : '未分类';
}
