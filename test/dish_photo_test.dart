import 'package:flutter_test/flutter_test.dart';
import 'package:orderdisk_flutter/data/food_images.dart';
import 'package:orderdisk_flutter/ui/widgets/cozy_dish_photo.dart';

// 「搜索出来的图很慢」的修复里，真正决定下多少字节的是这两个纯函数：
// * dishThumbUrl —— 下厨房图床支持 imageView2 实时裁图，400×400 的 43 KB 换成
//   320×320 的 WebP 只有 ~20 KB（PC 实测 23 KB / 12.5 KB / 9 KB 三档）；
// * pixelSize —— 卡片只有 84–96 dp，没必要按 400 px 解码。
// 这两个函数一旦回归（例如忘了去掉原 query、忘了 format/webp），图会重新变慢但不报错，
// 所以在这里钉死。
void main() {
  group('dishThumbUrl', () {
    test('下厨房图床按尺寸重写成 WebP，并去掉原有 query', () {
      expect(
        dishThumbUrl(
          'https://i2.chuimg.com/e9ad8d2b426a4161b8f14ffa0b85de2c_1242w_1654h.jpg'
          '?imageView2/1/w/400/h/400/interlace/1/q/80',
          px: 320,
        ),
        'https://i2.chuimg.com/e9ad8d2b426a4161b8f14ffa0b85de2c_1242w_1654h.jpg'
        '?imageView2/1/w/320/h/320/interlace/1/q/72/format/webp',
      );
    });

    test('没有 query 的图床地址也能加参数', () {
      expect(
        dishThumbUrl('https://i1.chuimg.com/abc_800w_600h.jpg', px: 240),
        'https://i1.chuimg.com/abc_800w_600h.jpg'
        '?imageView2/1/w/240/h/240/interlace/1/q/72/format/webp',
      );
    });

    test('尺寸夹在 120–1080 之间', () {
      expect(dishThumbUrl('https://i2.chuimg.com/a.jpg', px: 50), contains('/w/120/h/120/'));
      expect(dishThumbUrl('https://i2.chuimg.com/a.jpg', px: 5000), contains('/w/1080/h/1080/'));
    });

    test('动图保持原样，不会被压成静态图', () {
      const String gif = 'https://i2.chuimg.com/a.gif?imageView2/1/w/400/h/400';
      expect(dishThumbUrl(gif, px: 240), gif);
    });

    test('其它图源原样返回（TheMealDB 不认 imageView2）', () {
      const String meal = 'https://www.themealdb.com/images/media/meals/abc.jpg';
      expect(dishThumbUrl(meal, px: 240), meal);
      expect(dishThumbUrl('', px: 240), '');
    });
  });

  group('CozyDishPhoto.pixelSize', () {
    test('按显示宽度 × dpr 选档，只落在固定几档', () {
      expect(CozyDishPhoto.pixelSize(84, 3), 320); // 搜索结果卡 84 dp
      expect(CozyDishPhoto.pixelSize(96, 3), 320); // 索引卡 96 dp
      expect(CozyDishPhoto.pixelSize(320, 3), 720); // 详情大图
      expect(CozyDishPhoto.pixelSize(40, 3), 160); // 小缩略图
    });
  });

  group('CozyDishPhoto.thumbUrl', () {
    test('与控件里实际请求的地址一致（预热才能命中同一份缓存）', () {
      expect(
        CozyDishPhoto.thumbUrl('https://i2.chuimg.com/b.jpg?imageView2/1/w/400/h/400', cssWidth: 84),
        'https://i2.chuimg.com/b.jpg'
        '?imageView2/1/w/320/h/320/interlace/1/q/72/format/webp',
      );
    });
  });
}
