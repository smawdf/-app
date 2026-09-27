import 'package:flutter_test/flutter_test.dart';
import 'package:orderdisk_flutter/data/category_placement.dart';

// 发现页「加入我的小店」的落位规则（原生 DiscoverViewModel.kt:151-154 的等价实现）。
// 这条规则曾经在 Flutter 移植里整段丢失，导致从发现页加进来的菜永远落在「未分类」，
// 而「未分类」在店铺页分类卡里排最后 → 用户加完菜在店铺首页看不到它。
void main() {
  group('resolveDishCategory', () {
    test('菜谱分类与店铺分类同名（忽略大小写）时沿用店铺写法', () {
      expect(
        resolveDishCategory(
          shopCategories: ['招牌必吃', '汤羹', '暖心硬菜'],
          recipeCategory: '汤羹',
        ),
        '汤羹',
      );
      expect(
        resolveDishCategory(
          shopCategories: ['drinks', '招牌必吃'],
          recipeCategory: 'DRINKS',
        ),
        'drinks',
      );
    });

    test('菜谱分类对不上任何店铺分类时，落进第一个分类（= 分类卡第一张）', () {
      expect(
        resolveDishCategory(
          shopCategories: ['暖心硬菜', '招牌必吃'],
          recipeCategory: '下厨房',
        ),
        '招牌必吃', // 按名字升序：招(U+62DB) < 暖(U+6696)
      );
      expect(
        resolveDishCategory(
          shopCategories: ['暖心硬菜', '招牌必吃'], // 传参顺序不影响结果
          recipeCategory: '',
        ),
        '招牌必吃',
      );
    });

    test('店铺一个分类都没有时兜底「未分类」', () {
      expect(resolveDishCategory(shopCategories: const []), '未分类');
      expect(
        resolveDishCategory(shopCategories: const ['', '  '], recipeCategory: '下厨房'),
        '未分类',
      );
    });

    test('分类去重并忽略空白', () {
      expect(
        resolveDishCategory(shopCategories: const [' 汤羹 ', '汤羹', '', '  ']),
        '汤羹',
      );
    });
  });
}
