import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// 三对专属情侣主题枚举
enum CoupleTheme {
  /// 一二 & 布布：暖阳橙金 · 布布掌勺大厨 & 一二被宠吃货
  yierBubu,

  /// 小鸡毛 & 小白（线条小狗）：芝士奶黄 · 小鸡毛金毛掌勺 & 小白马尔济斯干饭
  linesPuppy,

  /// 水豚噜噜 & 噜妹：蜜桃草莓粉 · 噜噜草莓短裤大厨 & 噜妹宝宝裙吃货
  luluLumei,
}

/// 情侣主题详细配置规格
class CoupleThemeSpec {
  final CoupleTheme theme;
  final String title;
  final String subtitle;
  final String tag;
  final String emoji;
  final String particleEmoji;

  // 核心色彩（对比度饱满、氛围感鲜明，优雅舒适不荧光）
  final Color primary;
  final Color primaryDark;
  final Color primaryLight;
  final LinearGradient primaryGradient;
  final LinearGradient pageGradient;
  final Color accent;
  final Color accentLight;
  final Color bgPage;
  final Color cardBg;
  final Color cardBorder;
  final Color textMain;
  final Color textSub;
  final Color dockGlass;
  final Color dockBorder;
  final Color dockIndicator;
  final Color shadowColor;

  // 专属文案与角色
  final String greeting;
  final String sweetTitle;
  final String sweetSub;
  final String defaultChefAvatar;
  final String defaultEaterAvatar;
  final String defaultChefName;
  final String defaultEaterName;
  final String chefTag;
  final String eaterTag;
  final String signature;
  final String stampTip;
  final List<String> dialogQuotes;

  // 专属动图系列（统一来源于 theme_pack 素材库）
  final String duoAnimationAsset;
  final String duoAnimationSubtitle;
  final String chefAnimAsset;
  final String eaterAnimAsset;
  final String loveAnimAsset;
  final String cheerAnimAsset;
  final String floatingAnimAsset;

  const CoupleThemeSpec({
    required this.theme,
    required this.title,
    required this.subtitle,
    required this.tag,
    required this.emoji,
    required this.particleEmoji,
    required this.primary,
    required this.primaryDark,
    required this.primaryLight,
    required this.primaryGradient,
    required this.pageGradient,
    required this.accent,
    required this.accentLight,
    required this.bgPage,
    required this.cardBg,
    required this.cardBorder,
    required this.textMain,
    required this.textSub,
    required this.dockGlass,
    required this.dockBorder,
    required this.dockIndicator,
    required this.shadowColor,
    required this.greeting,
    required this.sweetTitle,
    required this.sweetSub,
    required this.defaultChefAvatar,
    required this.defaultEaterAvatar,
    required this.defaultChefName,
    required this.defaultEaterName,
    required this.chefTag,
    required this.eaterTag,
    required this.signature,
    required this.stampTip,
    required this.dialogQuotes,
    required this.duoAnimationAsset,
    required this.duoAnimationSubtitle,
    required this.chefAnimAsset,
    required this.eaterAnimAsset,
    required this.loveAnimAsset,
    required this.cheerAnimAsset,
    required this.floatingAnimAsset,
  });

  /// 1. 一二 & 布布 主题规格（布布=棕熊·男·大厨 / 一二=白熊·女·吃货）
  /// 暖阳橙金风格：焦糖暖阳金与温润米杏，既有标志性主题辨识度，又温馨耐看
  static const CoupleThemeSpec yierBubu = CoupleThemeSpec(
    theme: CoupleTheme.yierBubu,
    title: '一二布布',
    subtitle: '暖阳橙金 · 布布掌勺大厨 & 一二被宠吃货',
    tag: '一二布布',
    emoji: '🍳',
    particleEmoji: '🍯',
    primary: Color(0xFFD97706),
    primaryDark: Color(0xFFB45309),
    primaryLight: Color(0xFFFEF3C7),
    primaryGradient: LinearGradient(
      colors: [Color(0xFFF59E0B), Color(0xFFD97706)],
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
    ),
    pageGradient: LinearGradient(
      colors: [Color(0xFFFDFBF7), Color(0xFFFBF4EB)],
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
    ),
    accent: Color(0xFFEA580C),
    accentLight: Color(0xFFFFEDD5),
    bgPage: Color(0xFFFDFBF7),
    cardBg: Color(0xFFFFFFFF),
    cardBorder: Color(0xFFFDE68A),
    textMain: Color(0xFF2C2420),
    textSub: Color(0xFF82746C),
    dockGlass: Color(0xF8FDFBF7),
    dockBorder: Color(0x40D97706),
    dockIndicator: Color(0xFFD97706),
    shadowColor: Color(0x18D97706),
    greeting: '布布大厨负责掌勺做好吃的，一二负责吃饱饱！今天也要好好吃饭呀~',
    sweetTitle: '布布往一二的碗里夹了一大块肉肉 ✨',
    sweetSub: '“今天想吃什么呀？本熊大厨随叫随到~”',
    defaultChefAvatar: 'assets/images/theme_pack/yier_bubu/chef_avatar.png',
    defaultEaterAvatar: 'assets/images/theme_pack/yier_bubu/eater_avatar.png',
    defaultChefName: '布布',
    defaultEaterName: '一二',
    chefTag: '棕熊大厨',
    eaterTag: '白熊吃货',
    signature: '布布负责做好吃的，一二负责夸夸大厨~',
    stampTip: '一二等投喂中~✨',
    dialogQuotes: [
      '“布布：今天想吃什么呀？本熊大厨随叫随到~”',
      '“一二：想吃糖醋小排和大米饭，大厨快上菜！”',
      '“布布：好嘞！热腾腾的饭菜马上出锅，绝不饿着一二！”',
    ],
    duoAnimationAsset: 'assets/images/theme_pack/yier_bubu/duo_scene.gif',
    duoAnimationSubtitle: '布布大厨正在给一二准备热腾腾的大餐~ 🍲',
    chefAnimAsset: 'assets/images/theme_pack/yier_bubu/chef_bubu_act.gif',
    eaterAnimAsset: 'assets/images/theme_pack/yier_bubu/eater_yier_act.gif',
    loveAnimAsset: 'assets/images/theme_pack/yier_bubu/duo_love.gif',
    cheerAnimAsset: 'assets/images/theme_pack/yier_bubu/mascot_cheer.gif',
    // 陪伴小挂件：一二等投喂中，显示一二动态
    floatingAnimAsset: 'assets/images/theme_pack/yier_bubu/eater_yier_act.gif',
  );

  /// 2. 小鸡毛 & 小白（线条小狗）主题规格（小鸡毛=金毛·男·大厨 / 小白=纯白·女·吃货）
  /// 芝士奶黄风格：明亮治愈的奶酪黄，可爱纯粹
  static const CoupleThemeSpec linesPuppy = CoupleThemeSpec(
    theme: CoupleTheme.linesPuppy,
    title: '线条小狗',
    subtitle: '芝士奶黄 · 小鸡毛金毛掌勺 & 小白马尔济斯干饭',
    tag: '线条小狗',
    emoji: '🐶',
    particleEmoji: '🦴',
    primary: Color(0xFFCA8A04),
    primaryDark: Color(0xFFA16207),
    primaryLight: Color(0xFFFEF9C3),
    primaryGradient: LinearGradient(
      colors: [Color(0xFFEAB308), Color(0xFFCA8A04)],
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
    ),
    pageGradient: LinearGradient(
      colors: [Color(0xFFFDFCF7), Color(0xFFFAF6E8)],
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
    ),
    accent: Color(0xFFB45309),
    accentLight: Color(0xFFFEF3C7),
    bgPage: Color(0xFFFDFCF7),
    cardBg: Color(0xFFFFFFFF),
    cardBorder: Color(0xFFFEF08A),
    textMain: Color(0xFF2B2621),
    textSub: Color(0xFF80776D),
    dockGlass: Color(0xF8FDFCF7),
    dockBorder: Color(0x40CA8A04),
    dockIndicator: Color(0xFFCA8A04),
    shadowColor: Color(0x18CA8A04),
    greeting: '汪！小鸡毛大厨已就位，骨头拉面全管饱，小白负责幸福干饭 🐶',
    sweetTitle: '小鸡毛把最香的骨头牛排都端给小白啦 ✨',
    sweetSub: '“汪！骨头牛排全管饱，小白别急~”',
    defaultChefAvatar: 'assets/images/theme_pack/lines_puppy/chef_avatar.png',
    defaultEaterAvatar: 'assets/images/theme_pack/lines_puppy/eater_avatar.png',
    defaultChefName: '小鸡毛',
    defaultEaterName: '小白',
    chefTag: '金毛大厨',
    eaterTag: '小白吃货',
    signature: '摇尾巴就是对大厨最崇高的赞赏！',
    stampTip: '小白敲碗等开饭~🦴',
    dialogQuotes: [
      '“小鸡毛：汪！骨头拉面全管饱，小白别急~”',
      '“小白：汪呜！大厨做饭太香了，本汪已经迫不及待啦！”',
      '“小鸡毛：小白乖乖坐好，最热乎的一碗马上端给你～”',
    ],
    duoAnimationAsset: 'assets/images/theme_pack/lines_puppy/duo_scene.gif',
    duoAnimationSubtitle: '小鸡毛大厨与小白吃货正在欢快吸溜拉面~ 🍜',
    chefAnimAsset: 'assets/images/theme_pack/lines_puppy/chef_puppy_act.gif',
    eaterAnimAsset: 'assets/images/theme_pack/lines_puppy/eater_puppy_act.gif',
    loveAnimAsset: 'assets/images/theme_pack/lines_puppy/duo_love.gif',
    cheerAnimAsset: 'assets/images/theme_pack/lines_puppy/mascot_cheer.gif',
    // 陪伴小挂件：小白等开饭，显示小白动态
    floatingAnimAsset: 'assets/images/theme_pack/lines_puppy/eater_puppy_act.gif',
  );

  /// 3. 水豚噜噜 & 噜妹 主题规格（噜噜=穿草莓短裤·男·大厨 / 噜妹=红蝴蝶结草莓宝宝衣服·女·吃货）
  /// 蜜桃草莓粉风格：浪漫梦幻的草莓蜜桃粉，少女心甜蜜
  static const CoupleThemeSpec luluLumei = CoupleThemeSpec(
    theme: CoupleTheme.luluLumei,
    title: '水豚噜噜',
    subtitle: '蜜桃草莓粉 · 噜噜草莓短裤大厨 & 噜妹宝宝裙吃货',
    tag: '水豚噜噜',
    emoji: '🍊',
    particleEmoji: '🍓',
    primary: Color(0xFFE11D48),
    primaryDark: Color(0xFFBE123C),
    primaryLight: Color(0xFFFCE7F3),
    primaryGradient: LinearGradient(
      colors: [Color(0xFFF43F5E), Color(0xFFE11D48)],
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
    ),
    pageGradient: LinearGradient(
      colors: [Color(0xFFFDF8F9), Color(0xFFFAF0F2)],
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
    ),
    accent: Color(0xFFDB2777),
    accentLight: Color(0xFFFDF2F8),
    bgPage: Color(0xFFFDF8F9),
    cardBg: Color(0xFFFFFFFF),
    cardBorder: Color(0xFFFBCFE8),
    textMain: Color(0xFF2C2225),
    textSub: Color(0xFF827075),
    dockGlass: Color(0xF8FDF8F9),
    dockBorder: Color(0x40E11D48),
    dockIndicator: Color(0xFFE11D48),
    shadowColor: Color(0x18E11D48),
    greeting: '不怕别人，就怕噜妹到家说饿！水豚噜噜大厨正在加速掌勺 🍊',
    sweetTitle: '噜噜给穿宝宝裙的噜妹准备了草莓大餐 ✨',
    sweetSub: '“不怕别人，就怕噜妹到家说饿！大厨上菜~”',
    defaultChefAvatar: 'assets/images/theme_pack/lulu_lumei/chef_avatar.png',
    defaultEaterAvatar: 'assets/images/theme_pack/lulu_lumei/eater_avatar.png',
    defaultChefName: '噜噜',
    defaultEaterName: '噜妹',
    chefTag: '水豚大厨',
    eaterTag: '宝宝小噜妹',
    signature: '爱意都藏在每一口热气腾腾的饭菜里~',
    stampTip: '噜妹抱着大碗等投喂~🎀',
    dialogQuotes: [
      '“噜噜：不怕别人，就怕噜妹到家说饿！大厨上菜~”',
      '“噜妹：噜妹抱着大碗等投喂，大厨快点嘛～”',
      '“噜噜：穿草莓短裤的大厨正在全力翻炒，马上喂饱小噜妹！”',
    ],
    duoAnimationAsset: 'assets/images/theme_pack/lulu_lumei/duo_scene.gif',
    duoAnimationSubtitle: '噜噜大厨牵着穿宝宝裙的小噜妹等开饭~ 🍓',
    chefAnimAsset: 'assets/images/theme_pack/lulu_lumei/chef_lulu_act.gif',
    eaterAnimAsset: 'assets/images/theme_pack/lulu_lumei/eater_lumei_act.gif',
    loveAnimAsset: 'assets/images/theme_pack/lulu_lumei/duo_love.gif',
    cheerAnimAsset: 'assets/images/theme_pack/lulu_lumei/mascot_cheer.gif',
    // 陪伴小挂件：噜妹抱着大碗等投喂，显示噜妹动态
    floatingAnimAsset: 'assets/images/theme_pack/lulu_lumei/eater_lumei_act.gif',
  );

  static CoupleThemeSpec fromTheme(CoupleTheme theme) {
    switch (theme) {
      case CoupleTheme.yierBubu:
        return yierBubu;
      case CoupleTheme.linesPuppy:
        return linesPuppy;
      case CoupleTheme.luluLumei:
        return luluLumei;
    }
  }
}

/// 全局情侣主题管理器 (ChangeNotifier 单例)
class CoupleThemeManager extends ChangeNotifier {
  static final CoupleThemeManager instance = CoupleThemeManager._();
  CoupleThemeManager._();

  static const String _prefKey = 'selected_couple_theme';
  CoupleTheme _currentTheme = CoupleTheme.yierBubu;

  CoupleTheme get currentTheme => _currentTheme;
  CoupleThemeSpec get currentSpec => CoupleThemeSpec.fromTheme(_currentTheme);

  Future<void> init() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedIndex = prefs.getInt(_prefKey);
      if (savedIndex != null &&
          savedIndex >= 0 &&
          savedIndex < CoupleTheme.values.length) {
        _currentTheme = CoupleTheme.values[savedIndex];
        notifyListeners();
      }
    } catch (_) {}
  }

  Future<void> setTheme(CoupleTheme theme) async {
    if (_currentTheme == theme) return;
    _currentTheme = theme;
    notifyListeners();
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt(_prefKey, theme.index);
    } catch (_) {}
  }
}

/// 情侣主题全局响应式 Provider（InheritedNotifier）
class CoupleThemeProvider extends InheritedNotifier<CoupleThemeManager> {
  const CoupleThemeProvider({
    super.key,
    required CoupleThemeManager manager,
    required super.child,
  }) : super(notifier: manager);

  static CoupleThemeSpec of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<CoupleThemeProvider>();
    return scope?.notifier?.currentSpec ?? CoupleThemeManager.instance.currentSpec;
  }
}

/// BuildContext 扩展，便捷获取当前情侣主题（自动建立响应式监听依赖）
extension CoupleThemeContext on BuildContext {
  CoupleThemeSpec get coupleTheme => CoupleThemeProvider.of(this);
}
