import 'dart:async';

import 'package:dio/dio.dart';

/// 菜品图片适配层：把「没有图 / 只有 emoji」的菜品补成真实菜品照片。
///
/// 背景：`menu_dishes.image_url` 与内置菜谱库在历史上都拿 emoji 当图
/// （工程既有约定：长度 ≤ 4 的字符串当 emoji 渲染，见
/// `menu_management_page.dart:1518`），所以真机上菜品卡片只有 emoji。
///
/// 这里给出一条**离线、确定、无 key** 的「菜名 → 真实照片」映射：
/// 1. [kCuratedDishPhotos]：内置菜谱库那 8 道菜，精确菜名直配；
/// 2. [kKeywordDishPhotos]：中文关键词兜底（肉/鸡/鱼/蛋/菜/汤/面/饭/甜品/饮品…），
///    覆盖绝大多数家常菜名；
/// 3. [kFallbackDishPhotoUrl]：菜名完全判断不出来时的通用家常菜照片；
/// 4. [searchDishPhotoRemote]：以上都没命中时，用 TheMealDB 免费接口按菜名在线找一张。
///
/// 图源 TheMealDB（https://www.themealdb.com），免费、无需 key、
/// 提供真实菜品照片；实测雷电模拟器与宿主机均可直连（ping 178ms / HTTP 200）。

/// 判断一个值是不是可以直接交给 `Image.network` 的图片地址。
/// emoji（`🍲`）、空串、本地路径都返回 false。
bool isPhotoUrl(String value) {
  final String v = value.trim();
  if (v.length <= 4) return false;
  return v.startsWith('http://') || v.startsWith('https://');
}

/// 随机占位图床。这些地址能加载出图片，但内容与菜品无关（早期灌测试数据用的
/// `https://picsum.photos/seed/xxx/800/600` 就是这类），所以当成「没有图」。
const List<String> kPlaceholderPhotoHosts = <String>[
  'picsum.photos',
  'placehold.co',
  'placeholder.com',
  'via.placeholder.com',
  'dummyimage.com',
  'lorempixel.com',
  'loremflickr.com',
  'fakeimg.pl',
];

/// 是不是随机占位图（能加载但内容不对）。
bool isPlaceholderPhotoUrl(String value) {
  final String v = value.trim().toLowerCase();
  if (!isPhotoUrl(v)) return false;
  for (final String host in kPlaceholderPhotoHosts) {
    if (v.contains(host)) return true;
  }
  return false;
}

/// 这张图能不能当菜品图用：必须是真图片地址，且不是随机占位图。
bool isUsableDishPhoto(String value) => isPhotoUrl(value) && !isPlaceholderPhotoUrl(value);

// ---------------------------------------------------------------------------
// 图源常量
// ---------------------------------------------------------------------------

String _meal(String file) => 'https://www.themealdb.com/images/media/meals/$file';

final String _beefPot = _meal('ursuup1487348423.jpg'); // Beef Brisket Pot Roast
final String _chicken = _meal('feh9k21784665694.jpg'); // Extra crispy chicken wings
final String _souffle = _meal('twspvx1511784937.jpg'); // Chocolate Souffle
final String _beefStew = _meal('n1hcou1628770088.jpg'); // Croatian Goulash
final String _cheesecake = _meal('swttys1511385853.jpg'); // New York cheesecake
final String _drink = _meal('pjbaq11784731571.jpg'); // Creamy mango smoothie
final String _stirFryBeef = _meal('1529443236.jpg'); // Szechuan Beef
final String _vegSoup = _meal('60oc3k1699009846.jpg'); // Cabbage Soup
final String _fish = _meal('ysxwuq1487323065.jpg'); // Fish pie
final String _prawn = _meal('1525873040.jpg'); // Kung Po Prawns
final String _rice = _meal('j8c1d51782772399.jpg'); // Rice and Beans
final String _noodle = _meal('zry07j1763779321.jpg'); // Noodle bowl salad
final String _egg = _meal('47y6ii1765658818.jpg'); // Egg Foo Young
final String _tofu = _meal('1525874812.jpg'); // Ma Po Tofu
final String _pork = _meal('lwsnkl1604181187.jpg'); // Tonkatsu pork
final String _soup = _meal('1529446137.jpg'); // Egg Drop Soup
final String _dumpling = _meal('uyqrrv1511553350.jpg'); // Beef Dumpling Stew
final String _curry = _meal('tvttqv1504640475.jpg'); // Massaman Beef curry
final String _beefNoodle = _meal('pbzcrx1763765096.jpg'); // Beef pho

/// 内置菜谱库那 8 道菜的精确匹配照片（`supabase_api.dart` 的 `_recipeLibrary`）。
final Map<String, String> _curated = <String, String>{
  '关东风味肥牛寿喜烧': _beefPot,
  '蜜汁可乐小鸡翅': _chicken,
  '草莓生巧舒芙蕾': _souffle,
  '暖胃浓汤番茄牛腩': _beefStew,
  '法式巴斯克乳酪蛋糕': _cheesecake,
  '多汁白桃乌龙暴打冻饮': _drink,
  '鲜香滑嫩黑椒雪花牛肉粒': _stirFryBeef,
  '暖心鲜甜上汤娃娃菜': _vegSoup,
};

/// 关键词 → 照片。**按声明顺序匹配，先命中先用**，所以顺序即优先级：
/// 甜品/饮品 → 具体食材 → 泛化品类。
final List<MapEntry<String, String>> _keywords = <MapEntry<String, String>>[
  // 甜品 / 烘焙
  MapEntry<String, String>('巴斯克', _cheesecake),
  MapEntry<String, String>('乳酪', _cheesecake),
  MapEntry<String, String>('芝士', _cheesecake),
  MapEntry<String, String>('蛋糕', _cheesecake),
  MapEntry<String, String>('舒芙蕾', _souffle),
  MapEntry<String, String>('慕斯', _souffle),
  MapEntry<String, String>('布丁', _souffle),
  MapEntry<String, String>('甜点', _souffle),
  // 饮品
  MapEntry<String, String>('奶茶', _drink),
  MapEntry<String, String>('奶昔', _drink),
  MapEntry<String, String>('果汁', _drink),
  MapEntry<String, String>('乌龙', _drink),
  MapEntry<String, String>('气泡', _drink),
  MapEntry<String, String>('咖啡', _drink),
  MapEntry<String, String>('可乐', _drink),
  MapEntry<String, String>('冰沙', _drink),
  MapEntry<String, String>('饮料', _drink),
  MapEntry<String, String>('冻饮', _drink),
  MapEntry<String, String>('茶', _drink),
  MapEntry<String, String>('汁', _drink),
  // 具体食材
  MapEntry<String, String>('豆腐', _tofu),
  MapEntry<String, String>('娃娃菜', _vegSoup),
  MapEntry<String, String>('西兰花', _vegSoup),
  MapEntry<String, String>('菠菜', _vegSoup),
  MapEntry<String, String>('生菜', _vegSoup),
  MapEntry<String, String>('娃娃', _vegSoup),
  MapEntry<String, String>('上汤', _vegSoup),
  MapEntry<String, String>('饺子', _dumpling),
  MapEntry<String, String>('馄饨', _dumpling),
  MapEntry<String, String>('虾', _prawn),
  MapEntry<String, String>('蟹', _prawn),
  MapEntry<String, String>('海鲜', _prawn),
  MapEntry<String, String>('扇贝', _prawn),
  MapEntry<String, String>('鱼', _fish),
  MapEntry<String, String>('翅', _chicken),
  MapEntry<String, String>('炸鸡', _chicken),
  MapEntry<String, String>('鸡腿', _chicken),
  MapEntry<String, String>('鸡块', _chicken),
  MapEntry<String, String>('鸡', _chicken),
  MapEntry<String, String>('牛腩', _beefStew),
  MapEntry<String, String>('肥牛', _beefPot),
  MapEntry<String, String>('寿喜', _beefPot),
  MapEntry<String, String>('牛排', _beefPot),
  MapEntry<String, String>('牛肉', _stirFryBeef),
  MapEntry<String, String>('牛', _beefPot),
  MapEntry<String, String>('排骨', _pork),
  MapEntry<String, String>('五花', _pork),
  MapEntry<String, String>('里脊', _pork),
  MapEntry<String, String>('咖喱', _curry),
  MapEntry<String, String>('猪', _pork),
  MapEntry<String, String>('蛋', _egg),
  MapEntry<String, String>('面', _noodle),
  MapEntry<String, String>('粉', _noodle),
  MapEntry<String, String>('米线', _noodle),
  MapEntry<String, String>('河粉', _beefNoodle),
  MapEntry<String, String>('饭', _rice),
  MapEntry<String, String>('粥', _soup),
  MapEntry<String, String>('汤', _soup),
  MapEntry<String, String>('羹', _soup),
  MapEntry<String, String>('煲', _beefStew),
  MapEntry<String, String>('沙拉', _vegSoup),
  MapEntry<String, String>('蔬', _vegSoup),
  MapEntry<String, String>('菜', _vegSoup),
  MapEntry<String, String>('菌', _vegSoup),
  MapEntry<String, String>('菇', _vegSoup),
  // 泛化：中式炒菜
  MapEntry<String, String>('炒', _stirFryBeef),
  MapEntry<String, String>('肉', _stirFryBeef),
];

/// 菜名完全判断不出来时的通用家常菜照片（最后兜底，保证「每个菜都有图」）。
final String kFallbackDishPhotoUrl = _stirFryBeef;

/// 离线解析：菜名 → 真实照片 URL。命中不了返回空串。
///
/// [current] 是库里已有的值（可能是 emoji，也可能是 picsum 这类随机占位图），
/// 只有当它本身就是一张可用的菜品照片时才原样返回。
String resolveDishImage(String name, {String current = ''}) {
  if (isUsableDishPhoto(current)) return current.trim();
  final String n = name.trim();
  if (n.isEmpty) return '';
  final String? exact = _curated[n];
  if (exact != null) return exact;
  for (final MapEntry<String, String> e in _keywords) {
    if (n.contains(e.key)) return e.value;
  }
  return '';
}

/// 离线解析，且永不返回空串（兜底到 [kFallbackDishPhotoUrl]）。
String resolveDishImageOrFallback(String name, {String current = ''}) {
  final String hit = resolveDishImage(name, current: current);
  return hit.isEmpty ? kFallbackDishPhotoUrl : hit;
}

// ---------------------------------------------------------------------------
// 在线兜底（TheMealDB 免费检索，无需 key）
// ---------------------------------------------------------------------------

final Dio _dio = Dio(
  BaseOptions(
    connectTimeout: const Duration(seconds: 6),
    receiveTimeout: const Duration(seconds: 8),
    validateStatus: (int? code) => code != null && code >= 200 && code < 400,
  ),
);

final Map<String, String?> _remoteCache = <String, String?>{};

/// 按菜名在线找一张真实菜品照片；找不到 / 网络失败返回 null。
/// 结果按菜名缓存（含 null 结果），一个菜最多只请求一次。
Future<String?> searchDishPhotoRemote(String name) async {
  final String q = name.trim();
  if (q.isEmpty) return null;
  if (_remoteCache.containsKey(q)) return _remoteCache[q];
  try {
    final Response<dynamic> resp = await _dio.get<dynamic>(
      'https://www.themealdb.com/api/json/v1/1/search.php',
      queryParameters: <String, String>{'s': q},
    );
    final List<dynamic> meals =
        ((resp.data as Map<String, dynamic>?)?['meals'] as List<dynamic>?) ?? const <dynamic>[];
    if (meals.isEmpty) {
      _remoteCache[q] = null;
      return null;
    }
    final String? thumb = (meals.first as Map<String, dynamic>)['strMealThumb'] as String?;
    final String? url = isPhotoUrl(thumb ?? '') ? thumb : null;
    _remoteCache[q] = url;
    return url;
  } catch (_) {
    // 网络不可用时不缓存，下次再试。
    return null;
  }
}
