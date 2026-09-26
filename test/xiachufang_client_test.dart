// 发现页「搜一搜」实时数据源（下厨房搜索页）的解析单测。
//
// 样例 HTML 就是站点真实结构（照抄一条菜卡，去掉无关属性），
// 这样「图/名成对」「菜名清洗」「图片统一成 400×400」这几件事都被钉住。

import 'package:flutter_test/flutter_test.dart';
import 'package:orderdisk_flutter/data/xiachufang_client.dart';

const String _samplePage = '''
<section class="search-content">
  <div class="wrapper mt0" id="add-more-container">
    <a href="/recipe/100343649/" class="recipe-96-horizon" data-ga-event="" data-log="">
      <div class="body flex">
        <div class="cover">
          <img width="140" height="95" layout="fixed" src="data:image/png;base64,iVBORw0KGgo="
               data-src="https://i2.chuimg.com/eadabba6885811e6b87c0242ac110003_650w_650h.jpg?imageView2/1/w/280/h/190/interlace/1/q/75"
               alt="饺子馅 饺子 包子馅 包子" title="饺子馅 饺子 包子馅 包子">
        </div>
        <div class="content">
          <header class="name font18">饺子馅 饺子 包子馅 🥟❗️</header>
          <div class="stat flex-1">评分 <span>8.3</span><span class="ml10">3328</span> 人做过</div>
        </div>
      </div>
    </a>
    <a href="/recipe/1041515/" class="recipe-96-horizon">
      <div class="body flex">
        <div class="cover">
          <img data-src="https://i2.chuimg.com/08b0f4d0_650w_650h.jpg" alt="生煎饺子" title="生煎饺子">
        </div>
        <div class="content"><header class="name font18">生煎饺子</header></div>
      </div>
    </a>
    <a href="/recipe/999/" class="recipe-96-horizon">
      <div class="body flex"><div class="content"><header class="name font18">没有图的那道</header></div></div>
    </a>
  </div>
</section>
''';

void main() {
  group('XiachufangClient.parseSearchPage', () {
    test('按卡片顺序解析出「菜名 + 成品图」', () {
      final List<XiachufangDish> dishes =
          XiachufangClient.parseSearchPage(_samplePage);
      expect(dishes.length, 2); // 第三条没有 data-src，被丢掉
      expect(dishes[0].recipeId, '100343649');
      expect(dishes[0].name, '饺子馅 饺子 包子馅');
      expect(dishes[1].recipeId, '1041515');
      expect(dishes[1].name, '生煎饺子');
    });

    test('菜名去掉 emoji 与结尾装饰符', () {
      final List<XiachufangDish> dishes =
          XiachufangClient.parseSearchPage(_samplePage);
      expect(dishes[0].name.contains('🥟'), isFalse);
      expect(dishes[0].name.endsWith('❗'), isFalse);
    });

    test('图片统一改写成 400×400 方图', () {
      final List<XiachufangDish> dishes =
          XiachufangClient.parseSearchPage(_samplePage);
      expect(
        dishes[0].imageUrl,
        'https://i2.chuimg.com/eadabba6885811e6b87c0242ac110003_650w_650h.jpg'
        '?imageView2/1/w/400/h/400/interlace/1/q/80',
      );
      expect(dishes[1].imageUrl.contains('?imageView2/1/w/400/h/400'), isTrue);
    });

    test('同名菜只留一条', () {
      final String twice = _samplePage + _samplePage;
      final List<XiachufangDish> dishes =
          XiachufangClient.parseSearchPage(twice);
      expect(dishes.length, 2);
    });

    test('验证码页 / 空页都返回空列表，不抛异常', () {
      expect(XiachufangClient.parseSearchPage(''), isEmpty);
      expect(
        XiachufangClient.parseSearchPage('<html><title>滑动验证</title></html>'),
        isEmpty,
      );
    });
  });

  group('菜名清洗与售价推算', () {
    test('cleanDishName 压空白、去装饰', () {
      expect(XiachufangClient.cleanDishName('  红烧肉   '), '红烧肉');
      expect(XiachufangClient.cleanDishName('巨好吃的红烧肉！！！'), '巨好吃的红烧肉');
    });

    test('suggestDishPrice 按主料分档（主料优先于做法）', () {
      expect(suggestDishPrice('清蒸鲍鱼'), 58);
      expect(suggestDishPrice('番茄牛腩'), 38);
      expect(suggestDishPrice('冬瓜蛋花汤'), 32);
      expect(suggestDishPrice('豆角焖面'), 22);
      expect(suggestDishPrice('珍珠奶茶'), 16);
      expect(suggestDishPrice('草莓慕斯'), 26);
      expect(suggestDishPrice('凉拌黄瓜'), 18);
      expect(suggestDishPrice('香煎豆腐'), 20);
      expect(suggestDishPrice('神秘料理'), 28);
    });
  });
}
