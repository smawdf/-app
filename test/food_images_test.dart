import 'package:flutter_test/flutter_test.dart';
import 'package:orderdisk_flutter/data/food_images.dart';

/// 菜品图适配层的纯逻辑测试：不联网、不依赖设备。
void main() {
  group('isUsableDishPhoto', () {
    test('emoji / 空串 / 本地路径都不算图', () {
      expect(isUsableDishPhoto('🍲'), isFalse);
      expect(isUsableDishPhoto(''), isFalse);
      expect(isUsableDishPhoto('/sdcard/a.png'), isFalse);
    });

    test('随机占位图不算图，真图片地址才算', () {
      expect(isUsableDishPhoto('https://picsum.photos/seed/pizza/800/600'), isFalse);
      expect(isUsableDishPhoto('https://i2.chuimg.com/a.jpg'), isTrue);
    });
  });

  group('resolveDishImage', () {
    test('库里已有的可用照片原样保留', () {
      expect(
        resolveDishImage('随便什么菜', current: 'https://example.com/a.jpg'),
        'https://example.com/a.jpg',
      );
    });

    test('占位图会被换成真实菜品照', () {
      final String url =
          resolveDishImage('炸鸡', current: 'https://picsum.photos/seed/chicken/800/600');
      expect(url, isNotEmpty);
      expect(url.contains('picsum'), isFalse);
    });

    test('内置菜谱库 8 道菜用人工挑选的照片，优先级最高', () {
      expect(resolveDishImage('暖胃浓汤番茄牛腩'), contains('n1hcou1628770088.jpg'));
      expect(resolveDishImage('法式巴斯克乳酪蛋糕'), contains('swttys1511385853.jpg'));
    });

    test('下厨房词典：与抓到的标题完全同名', () {
      final String url = resolveDishImage('番茄芝士焗饭');
      expect(url, contains('chuimg.com'));
    });

    test('下厨房词典：站点标题包含菜名（短菜名也能命中）', () {
      for (final String dish in <String>['饺子', '馒头', '炒面', '葱油拌面']) {
        expect(
          resolveDishImage(dish),
          contains('chuimg.com'),
          reason: '$dish 应该命中下厨房词典',
        );
      }
    });

    test('词典没命中时退回关键词图（玉菇浓汤 → 汤）', () {
      expect(resolveDishImage('玉菇浓汤'), contains('themealdb.com'));
      expect(resolveDishImage('玉菇浓汤'), contains('1529446137.jpg'));
    });

    test('完全认不出的菜名返回空串', () {
      expect(resolveDishImage('zzz not a dish'), '');
    });
  });

  group('resolveDishImageOrFallback', () {
    test('永不返回空串', () {
      expect(resolveDishImageOrFallback('zzz not a dish'), isNotEmpty);
      expect(resolveDishImageOrFallback(''), isNotEmpty);
      expect(resolveDishImageOrFallback('番茄芝士焗饭'), contains('chuimg.com'));
    });
  });
}
