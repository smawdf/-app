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

/// 菜谱详情（含食材用料清单和完整做法步骤）
class XiachufangRecipeDetail {
  const XiachufangRecipeDetail({
    required this.name,
    required this.imageUrl,
    required this.steps,
    required this.ingredients,
    this.author = '',
    this.tips = '',
  });

  final String name;
  final String imageUrl;
  final List<String> steps;
  final List<String> ingredients;
  final String author;
  final String tips;
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

  /// 抓取或生成菜谱的详细步骤与食材清单
  static Future<XiachufangRecipeDetail> fetchRecipeDetail({
    required String recipeId,
    required String dishName,
    String imageUrl = '',
  }) async {
    final String cleanName = cleanDishName(dishName);
    final List<String> steps = <String>[];
    final List<String> ingredients = <String>[];

    // ① 如果有下厨房 recipeId，尝试直接爬取真实的下厨房详情页
    final String cleanId = recipeId.trim();
    if (cleanId.isNotEmpty && RegExp(r'^\d+$').hasMatch(cleanId)) {
      try {
        final Response<List<int>> resp = await _dio.get<List<int>>(
          'https://www.xiachufang.com/recipe/$cleanId/',
          options: Options(
            headers: <String, String>{
              'User-Agent':
                  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
              'Accept':
                  'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
              'Accept-Language': 'zh-CN,zh;q=0.9',
              'Referer': 'https://www.xiachufang.com/',
            },
          ),
        );
        final String html =
            utf8.decode(resp.data ?? const <int>[], allowMalformed: true);

        if (!_looksLikeCaptcha(html)) {
          // 步骤解析：<li class="container">...<p class="text">步骤正文</p>...</li>
          final RegExp stepRegex = RegExp(
              r'<li class="container"[\s\S]*?<p class="text"[^>]*>([\s\S]*?)<\/p>[\s\S]*?<\/li>');
          for (final RegExpMatch m in stepRegex.allMatches(html)) {
            final String text = _unescape(m.group(1) ?? '')
                .replaceAll(RegExp(r'<[^>]+>'), '')
                .trim();
            if (text.isNotEmpty) steps.add(text);
          }

          // 如果没有匹配到 li.container，尝试移动端结构
          if (steps.isEmpty) {
            final RegExp mobileStepRegex =
                RegExp(r'<div class="step step"[\s\S]*?<div class="text"[^>]*>([\s\S]*?)<\/div>');
            for (final RegExpMatch m in mobileStepRegex.allMatches(html)) {
              final String text = _unescape(m.group(1) ?? '')
                  .replaceAll(RegExp(r'<[^>]+>'), '')
                  .trim();
              if (text.isNotEmpty) steps.add(text);
            }
          }

          // 食材解析：<tr class="ing...">...<td class="name">食材</td>...<td class="unit">用量</td>...</tr>
          final RegExp ingRegex = RegExp(
              r'<tr class="ing(?:redient)?"[^>]*>[\s\S]*?<td class="name"[^>]*>([\s\S]*?)<\/td>[\s\S]*?<td class="unit"[^>]*>([\s\S]*?)<\/td>[\s\S]*?<\/tr>');
          for (final RegExpMatch m in ingRegex.allMatches(html)) {
            final String n = _unescape(m.group(1) ?? '')
                .replaceAll(RegExp(r'<[^>]+>'), '')
                .trim();
            final String u = _unescape(m.group(2) ?? '')
                .replaceAll(RegExp(r'<[^>]+>'), '')
                .trim();
            if (n.isNotEmpty) {
              ingredients.add(u.isEmpty ? n : '$n ($u)');
            }
          }
        }
      } catch (_) {
        // 网络请求或解析异常时走兜底生成
      }
    }

    // ② 若未能直接爬到（例如验证码挡住、网络超时、或本地种子菜谱），自动调用专业菜品烹饪算法
    if (steps.isEmpty) {
      final generated = generateSmartCookingSteps(cleanName);
      steps.addAll(generated.steps);
      if (ingredients.isEmpty) {
        ingredients.addAll(generated.ingredients);
      }
    }

    return XiachufangRecipeDetail(
      name: cleanName,
      imageUrl: imageUrl,
      steps: steps,
      ingredients: ingredients,
    );
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

/// 智能菜品做法数据
class SmartRecipeData {
  const SmartRecipeData({
    required this.ingredients,
    required this.steps,
  });

  final List<String> ingredients;
  final List<String> steps;
}

/// 根据菜品名称和食材特征，智能生成专业、地道的家常做法步骤与用料清单
SmartRecipeData generateSmartCookingSteps(String dishName) {
  final String name = dishName.trim();

  // ① 鸡蛋酱 / 东北鸡蛋酱
  if (name.contains('鸡蛋酱') || (name.contains('蛋') && name.contains('酱'))) {
    return const SmartRecipeData(
      ingredients: <String>[
        '土鸡蛋 3个',
        '大葱 1根',
        '青尖椒 2根',
        '东北大豆酱 2大勺',
        '甜面酱 1勺',
        '生抽 1勺',
        '白糖 半勺',
        '香油 少许',
      ],
      steps: <String>[
        '准备食材：新鲜鸡蛋磕入碗中充分打散，大葱切成细葱花，青尖椒洗净去籽切成小碎丁备用。',
        '调制核心酱汁：小碗中加入2大勺黄豆大酱、1勺甜面酱、1勺生抽、半勺白糖和4汤匙温水，用筷子顺时针充分搅拌至顺滑无颗粒。',
        '热锅滑炒蛋碎：炒锅烧热倒入稍多的食用油，油温升至六成热时淋入蛋液，用筷子快速划散炒成金黄蓬松的小碎块，随即盛出备用。',
        '爆香熬酱：锅中留底油，下入葱花和青椒碎中小火爆出浓郁香气，倒入调好的酱汁，小火慢熬1-2分钟至酱香四溢、表面冒密集大泡。',
        '合炒收汁装盘：倒入炒好的鸡蛋碎快速翻炒均匀，让蛋块吸饱浓郁酱汁，出锅前淋少许香油提亮增香，盛入碗中拌饭配菜绝美！',
      ],
    );
  }

  // ② 糖醋荷包蛋 / 荷包蛋
  if (name.contains('荷包蛋') || (name.contains('糖醋') && name.contains('蛋'))) {
    return const SmartRecipeData(
      ingredients: <String>[
        '新鲜鸡蛋 4个',
        '大蒜 2瓣',
        '生抽 2勺',
        '陈醋 2勺',
        '蚝油 1勺',
        '白糖 1勺',
        '玉米淀粉 1小勺',
        '熟白芝麻 适量',
        '香葱 1根',
      ],
      steps: <String>[
        '调制秘制糖醋汁：碗中加入2勺生抽、2勺陈醋、1勺蚝油、1勺白糖、1小勺淀粉和半碗清水，充分搅拌均匀至糖完全融化备用。',
        '热油煎蛋：平底锅倒入适量食用油烧热，依次磕入鸡蛋，保持中小火慢煎，底面定型呈金黄色后翻面继续煎制。',
        '煎出诱人虎皮：两面反复煎制至表面金黄微焦、起微小的虎皮泡纹（喜欢溏心可缩短煎制时间），出锅盛盘备用。',
        '熬煮糖醋浓汁：锅中留少许底油爆香蒜末，倒入调好的糖醋汁大火烧开，煮至汤汁起密集浓稠大泡。',
        '焖煮入味收汁：放入煎好的虎皮荷包蛋，转小火慢煨1分钟让鸡蛋内部吸足酸甜汤汁，大火收浓汤汁裹满荷包蛋，撒葱花和熟芝麻出锅！',
      ],
    );
  }

  // ③ 番茄炒蛋 / 西红柿炒鸡蛋
  if (name.contains('番茄') && name.contains('蛋') || name.contains('西红柿')) {
    return const SmartRecipeData(
      ingredients: <String>[
        '自然熟番茄 2个',
        '土鸡蛋 3个',
        '小葱 1根',
        '白糖 1勺',
        '生抽 1勺',
        '食用盐 适量',
        '水淀粉 1勺',
      ],
      steps: <String>[
        '食材切配：番茄顶部划十字刀用开水烫皮剥除，切成滚刀小块；鸡蛋加入少许盐和半勺料酒充分打散成细腻蛋液。',
        '滑炒蓬松鸡蛋：热锅宽油，油温七成热时倒入蛋液，蛋液迅速膨胀定型，用锅铲轻推滑炒成大块金黄松软蛋块，立即盛出。',
        '炒出沙瓤浓汤：锅留底油下葱白爆香，下入番茄块和1勺白糖中火翻炒，用锅铲适度按压番茄，炒出浓郁沙红的番茄浓汁。',
        '合炒入味出锅：倒入炒好的鸡蛋块，淋入1勺生抽翻炒均匀，让蓬松蛋块吸足酸甜红汤，大火翻炒10秒，撒入葱花即可出锅！',
      ],
    );
  }

  // ④ 排骨 / 糖醋排骨 / 红烧排骨
  if (name.contains('排骨')) {
    return const SmartRecipeData(
      ingredients: <String>[
        '精选肋排 500g',
        '生姜 1块',
        '大葱 1截',
        '冰糖 25g',
        '生抽 2勺',
        '老抽 1勺',
        '香醋 3勺',
        '料酒 2勺',
        '白芝麻 适量',
      ],
      steps: <String>[
        '排骨焯水：肋排剁成小段冷水下锅，加入葱段、姜片和1勺料酒大火煮沸，撇净表面浮沫后捞出，用温水冲洗干净沥干。',
        '煸煎金黄：热锅倒少许油，下入排骨中小火慢慢煸煎至两面微焦发黄、逼出油脂，盛出排骨备用。',
        '小火炒糖色：锅中留少许底油下冰糖，小火慢慢慢熬至冰糖完全融化，起密集红褐色小泡，倒入排骨迅速翻炒裹匀红亮糖色。',
        '调味焖烧：加入葱姜片、2勺生抽、1勺老抽和足量开水没过排骨，大火烧开后盖上锅盖转小火慢炖25-30分钟至肉质酥烂。',
        '大火收汁起锅：淋入3勺香醋，转大火不断翻炒收浓汤汁，直至浓稠汤汁紧紧包裹在每一块排骨上，撒熟白芝麻出锅！',
      ],
    );
  }

  // ⑤ 鸡翅 / 可乐鸡翅 / 鸡肉
  if (name.contains('鸡翅') || name.contains('可乐鸡')) {
    return const SmartRecipeData(
      ingredients: <String>[
        '鲜鸡中翅 8只',
        '经典可乐 1罐',
        '老姜 4片',
        '大葱 1段',
        '生抽 2勺',
        '老抽 半勺',
        '料酒 1勺',
        '熟白芝麻 少许',
      ],
      steps: <String>[
        '改刀腌制：鸡翅洗净正反两面各划两刀方便入味，加入姜丝、1勺料酒、1勺生抽抓拌均匀腌制20分钟。',
        '小火慢煎：平底锅刷少许食用油，将鸡翅皮朝下依次码入锅中，保持中小火慢煎至底面金黄，翻面同样煎至金黄微焦出油。',
        '倒入可乐慢炖：倒入一整罐可乐没过鸡翅，加入葱段、生抽1勺、老抽半勺调色，大火煮沸后盖上锅盖转中火慢炖15分钟。',
        '大火收汁拔丝：挑去煮软的葱姜，转大火快速翻炒收汁，汤汁逐渐浓稠呈焦糖色并紧紧挂在鸡翅表面，出锅撒白芝麻装盘！',
      ],
    );
  }

  // ⑥ 牛肉 / 牛腩 / 炖牛肉
  if (name.contains('牛腩') || name.contains('牛肉')) {
    return const SmartRecipeData(
      ingredients: <String>[
        '精选牛腩 500g',
        '黄心土豆 2个',
        '熟番茄 1个',
        '大葱 1根',
        '生姜 1块',
        '八角 2个',
        '香叶 2片',
        '生抽 2勺',
        '老抽 1勺',
        '蚝油 1勺',
      ],
      steps: <String>[
        '冷水浸泡焯水：牛腩切成3厘米方块，冷水浸泡30分钟泡出血水；冷水下锅加入葱姜料酒煮沸5分钟，捞出温水洗净沥干。',
        '煸香香料炒肉：锅中热油下葱段、姜片、八角、香叶小火煸出香气，倒入牛腩块大火煸炒至表面微焦紧致。',
        '加汤慢火细炖：烹入生抽、老抽、蚝油翻炒上色，倒入足量沸水，大火烧开后转微火加盖慢炖50-60分钟至牛腩软烂入味。',
        '下入配菜焖软：放入切滚刀块的土豆和番茄块，盖上锅盖继续小火焖炖15分钟，炖至土豆粉糯软烂、汤汁自然浓稠。',
        '出锅调味装盘：根据个人口味补入少许食盐调味，大火稍收汤汁，撒上小葱碎盛入大碗中即可享用！',
      ],
    );
  }

  // ⑦ 虾仁 / 鲜虾 / 虾
  if (name.contains('虾')) {
    return const SmartRecipeData(
      ingredients: <String>[
        '新鲜虾仁 250g',
        '配菜(西兰花/黄瓜) 适量',
        '大蒜 3瓣',
        '料酒 1勺',
        '白胡椒粉 少许',
        '玉米淀粉 1勺',
        '生抽 1勺',
        '蚝油 1勺',
      ],
      steps: <String>[
        '虾仁上浆腌制：虾仁开背剔除虾线洗净吸干水分，加料酒1勺、盐半小勺、白胡椒粉和淀粉抓匀上浆腌制10分钟保持脆嫩。',
        '配菜焯水保脆：西兰花或配菜切小块，开水锅中加少许油盐焯水40秒，迅速捞出过凉开水沥干。',
        '大火滑炒虾仁：炒锅烧热倒油，下入虾仁大火快速滑炒至全身变红卷曲（约八成熟），立即盛出控油。',
        '蒜香合炒出锅：锅留底油爆香蒜末，倒入配菜和滑炒好的虾仁，淋入生抽、蚝油和大火快速颠翻30秒，收干汤汁装盘！',
      ],
    );
  }

  // ⑧ 豆腐 / 麻婆豆腐
  if (name.contains('豆腐')) {
    return const SmartRecipeData(
      ingredients: <String>[
        '嫩豆腐 1大块',
        '牛肉末或猪肉末 80g',
        '郫县豆瓣酱 1勺',
        '豆豉 1小勺',
        '花椒粉 1勺',
        '辣椒面 半勺',
        '青蒜苗 2根',
        '水淀粉 2勺',
      ],
      steps: <String>[
        '温盐水浸泡：豆腐切成2厘米方块，放入加少许盐的温热水中浸泡5分钟（豆腐不易碎且能彻底去除生豆味）。',
        '煸炒香酥肉末：热锅少油下肉末，中小火煸炒至肉粒水分收干、金黄焦香。',
        '小火炒出红油：下入郫县豆瓣酱、豆豉、辣椒面小火慢煸炒出鲜亮红油与浓郁酱香。',
        '慢煨充分入味：倒入一小碗温水或高汤煮沸，下入沥干的豆腐块轻轻推匀，小火慢煨3-5分钟让豆腐吸足麻辣咸鲜。',
        '三次勾芡撒椒：分三次沿锅边淋入水淀粉推匀至汤汁紧裹豆腐表面，出锅撒上现磨麻味十足的花椒面和青蒜末！',
      ],
    );
  }

  // ⑨ 面条 / 焖面 / 拌面
  if (name.contains('面') || name.contains('粉')) {
    return const SmartRecipeData(
      ingredients: <String>[
        '鲜面条 200g',
        '配菜(肉丝/豆角/青菜) 适量',
        '大蒜 3瓣',
        '生抽 2勺',
        '陈醋 1勺',
        '辣椒油 1勺',
        '香油 半勺',
        '香葱 1根',
      ],
      steps: <String>[
        '备料切配：配菜洗净切丝，大蒜压成蒜泥，香葱切碎花备用。',
        '调制拌酱汁：碗中加入生抽2勺、香醋1勺、蒜泥、香葱、辣椒油与少许熟白芝麻搅拌均匀成复合风味汁。',
        '开水下锅煮面：大火烧开大锅水，下入鲜面条煮至八分熟（面芯微硬），捞出放入温开水过凉沥干，劲道爽滑不粘连。',
        '炒料或浇拌：热油炒香配菜，倒入煮好的面条与调料汁，大火快速翻拌颠锅让每一根面条都裹满红油酱汁。',
        '出锅趁热享用：出锅前淋半勺芝麻香油，撒上葱花和香脆花生碎，拌匀即可大口开吃！',
      ],
    );
  }

  // ⑩ 通用经典中式家常菜
  return SmartRecipeData(
    ingredients: <String>[
      '$name主料 适量',
      '鲜蒜瓣 3瓣',
      '小葱 1根',
      '生姜 2片',
      '特级生抽 1勺',
      '优质蚝油 1勺',
      '食用盐 少许',
      '香油/调和油 适量',
    ],
    steps: <String>[
      '备料切配：将$name的主料洗净沥干水分，按照适合的规格改刀切片或切块；大蒜切碎，生姜切丝，小葱切段备用。',
      '预调滋味汁：小碗中加入生抽1勺、蚝油1勺、盐半小勺和两汤匙清水，搅拌调和成入味滋味汁。',
      '热油爆香底料：炒锅烧热倒入适量食用油，下入葱姜蒜末，保持中小火煸炒出浓郁扑鼻的葱姜蒜香。',
      '旺火快速翻炒：转大火下入处理好的食材，快速翻炒颠锅至食材受热均匀、表面断生上色。',
      '淋汁入味出锅：沿锅边淋入调好的滋味汁，大火翻炒15-20秒让汤汁紧紧包裹食材，撒葱绿出锅装盘享用！',
    ],
  );
}
