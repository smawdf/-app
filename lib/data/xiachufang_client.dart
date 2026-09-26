// 发现页「搜一搜」的实时数据源：下厨房（xiachufang.com）
//
// 用户要求（原话）：「实时爬取，如果加入我的店铺的要放云端，其余的也不要增加手机缓存」
//   —— 搜索时**实时**去下厨房取「成品图 + 名称」，手机里不落任何缓存/索引；
//   只有用户点「加入我的小店」时，那道菜才写进云端（Supabase `menu_dishes`）。
//
// 接口：`https://m.xiachufang.com/search/?keyword=<关键词>`
//   实测（雷电模拟器 / 本机）：200、约 25 KB、20 张菜卡；每张卡形如
//     <a href="/recipe/<id>/" class="recipe-96-horizon">
//       <img … data-src="https://i2.chuimg.com/<hash>_650w_650h.jpg?imageView2/…" alt="菜名">
//       <header class="name font18">菜名</header>
//       <div class="stat flex-1">评分 <span>8.3</span><span class="ml10">3328</span> 人做过</div>
//   必须带 Android Chrome UA + zh-CN，否则站点 WAF 会回 418 / 302。
//
// 边界与风险（都已实测，写在这里免得以后忘）：
//   * 站点 robots 的 `Disallow: /*keyword=*` 覆盖了搜索路径。这里按用户明确要求使用，
//     所以**只在用户主动搜索时**发一次请求（页面侧 300ms 防抖 + 同一关键词只请求一次），
//     不做批量爬取。若哪天要下线，删掉本文件与 discover_page 里的调用即可。
//   * 限流：站点对密集请求会回验证码页（实测约 4 次快请求即触发）。命中验证码 / 网络失败时
//     返回空列表，页面提示「稍后再试」。
//   * 详情页（做法、食材）在 www 站被验证码挡死、m 站也会限流，所以这里**只取搜索列表**：
//     成品图 + 名称，正是用户要的两项。

import 'dart:convert';

import 'package:dio/dio.dart';

/// 下厨房搜索列表里的一道菜（只有用户要的两项：成品图 + 名称）。
class XiachufangDish {
  const XiachufangDish({
    required this.recipeId,
    required this.name,
    required this.imageUrl,
  });

  /// 菜谱 id（点卡片想跳原页时用得上；目前只用于去重）
  final String recipeId;

  /// 菜名（已去掉 emoji / 多余空白）
  final String name;

  /// 成品图直链（已统一成 400×400 方图，热链 chuimg CDN）
  final String imageUrl;

  @override
  String toString() => 'XiachufangDish($recipeId, $name)';
}

class XiachufangClient {
  XiachufangClient._();

  /// 搜索页地址（m 站：轻、快，且实测不返回验证码）
  static const String _searchUrl = 'https://m.xiachufang.com/search/';

  /// 站点 WAF 认这个 UA；换成默认 Dart UA 会 418。
  static const String _ua =
      'Mozilla/5.0 (Linux; Android 9; SM-G960F) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36';

  static final Dio _dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 8),
      receiveTimeout: const Duration(seconds: 12),
      responseType: ResponseType.bytes,
      headers: <String, String>{
        'User-Agent': _ua,
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9',
      },
      validateStatus: (int? code) => code != null && code >= 200 && code < 400,
    ),
  );

  /// 本次搜索是否被站点限流 / 网络失败（页面据此提示，不弹错误）。
  static bool lastSearchFailed = false;

  /// 实时搜索：一次网络请求换一批「成品图 + 名称」。空关键词 / 失败 / 限流都返回空列表。
  static Future<List<XiachufangDish>> search(String keyword) async {
    final String q = keyword.trim();
    lastSearchFailed = false;
    if (q.isEmpty) return const <XiachufangDish>[];

    late final String body;
    try {
      final Response<List<int>> resp = await _dio.get<List<int>>(
        _searchUrl,
        queryParameters: <String, String>{'keyword': q},
      );
      body = utf8.decode(resp.data ?? const <int>[], allowMalformed: true);
    } on DioException catch (e) {
      // 站点限流时会直接 4xx/5xx；按「稍后再试」处理，不打断页面。
      lastSearchFailed = true;
      assert(() {
        // ignore: avoid_print
        print('XiachufangClient.search 失败：${e.type} ${e.message}');
        return true;
      }());
      return const <XiachufangDish>[];
    } catch (_) {
      lastSearchFailed = true;
      return const <XiachufangDish>[];
    }

    if (_looksLikeCaptcha(body)) {
      lastSearchFailed = true;
      return const <XiachufangDish>[];
    }
    return parseSearchPage(body);
  }

  // ───────────────────────────────────────────────────────────────────────────
  // 解析（纯函数，单测直接喂样例 HTML）
  // ───────────────────────────────────────────────────────────────────────────

  /// 从搜索页 HTML 里抽出菜卡。抽不到会返回空列表。
  static List<XiachufangDish> parseSearchPage(String html) {
    final List<XiachufangDish> out = <XiachufangDish>[];
    final Set<String> seen = <String>{};

    // 每张卡都以 <a href="/recipe/<id>/" 开头：先定位所有锚点，再逐段取卡内内容，
    // 这样「图/名」永远成对，不会把上一张卡的图配到下一张卡的名字上。
    final List<RegExpMatch> anchors =
        RegExp(r'<a\s+href="/recipe/(\d+)/"').allMatches(html).toList();
    for (int i = 0; i < anchors.length; i++) {
      final int start = anchors[i].start;
      final int end = i + 1 < anchors.length ? anchors[i + 1].start : html.length;
      final String chunk = html.substring(start, end);
      final String id = anchors[i].group(1) ?? '';

      final RegExpMatch? img = RegExp(r'data-src="([^"]+)"').firstMatch(chunk);
      final RegExpMatch? name =
          RegExp(r'class="name font18"[^>]*>([^<]*)<').firstMatch(chunk) ??
              RegExp(r'\balt="([^"]*)"').firstMatch(chunk);
      if (img == null || name == null) continue;

      final String cleanName = cleanDishName(_unescape(name.group(1) ?? ''));
      final String imgUrl = _square400(_unescape(img.group(1) ?? ''));
      if (cleanName.isEmpty || !imgUrl.contains('chuimg.com')) continue;
      if (!seen.add(cleanName)) continue;

      out.add(XiachufangDish(recipeId: id, name: cleanName, imageUrl: imgUrl));
    }
    return out;
  }

  /// 菜名清洗：去 emoji / 装饰符、压缩空白、去掉结尾的营销感叹号串。
  static String cleanDishName(String raw) {
    String s = raw.replaceAll(_emoji, '');
    s = s.replaceAll(RegExp(r'[\u0000-\u001F]'), ' ');
    s = s.replaceAll(RegExp(r'\s+'), ' ').trim();
    // 结尾的「❗️」「!!!」「~」这类装饰，去完 emoji 后还剩标点
    s = s.replaceAll(RegExp(r'[!！?？~～、,，。.]{2,}$'), '');
    return s.trim();
  }

  /// 图片统一成 400×400 方图（去掉站点自带的 imageView2 参数再拼我们自己的）。
  static String _square400(String raw) {
    final int q = raw.indexOf('?');
    final String base = q >= 0 ? raw.substring(0, q) : raw;
    return '$base?imageView2/1/w/400/h/400/interlace/1/q/80';
  }

  static bool _looksLikeCaptcha(String body) {
    if (body.contains('滑动验证') || body.contains('安全验证')) return true;
    return body.length < 3000 && !body.contains('class="name font18"');
  }

  static String _unescape(String s) => s
      .replaceAll('&amp;', '&')
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&quot;', '"')
      .replaceAll('&#39;', "'")
      .replaceAll('&nbsp;', ' ');

  /// emoji / 装饰符（含代理对区段与变体选择符、零宽连接符）
  static final RegExp _emoji = RegExp(
    r'[\u{1F000}-\u{1FAFF}\u{2190}-\u{21FF}\u{2300}-\u{27BF}\u{2B00}-\u{2BFF}\u{FE0F}\u{200D}]',
    unicode: true,
  );
}

/// 给抓来的菜谱推一个售价（下厨房只有菜谱、没有价格；加入店铺时用户还能自己改）。
/// 先看贵价食材，再看类目/主料 —— 与之前本地索引用的是同一套规则，行为保持一致。
int suggestDishPrice(String name, [String category = '']) {
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
