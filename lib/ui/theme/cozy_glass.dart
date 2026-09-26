import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

// ══════════════════════════════════════════════════════════════════════════════
//  高糖小食 · 纯白底光影设计系统
//  ──────────────────────────────────────────────────────────────────────────
//  核心原则：白底上不能再靠「颜色深浅」分层，只能靠「光」分层。
//  阴影 / 高光 / 折射 —— 三者缺一，卡片就会糊在白纸上，玻璃就会退化成白板。
//
//  色板 1:1 对齐原生 `app/src/main/java/com/myorderapp/ui/theme/Color.kt`
//  玻璃规格 1:1 对齐原生 `MainActivity.kt` 的 drawBackdrop + lens 配方
// ══════════════════════════════════════════════════════════════════════════════

/// ① 色板 —— 与原生 `Color.kt` 逐行对齐
class CozyPalette {
  const CozyPalette._();

  static const Color background = Color(0xFFFFFFFF); // Color.kt:6
  static const Color surface = Color(0xFFFFFCF8); // Color.kt:7
  static const Color surfaceVariant = Color(0xFFE7E2DC);
  static const Color surfaceContainer = Color(0xFFF3EDE7);
  static const Color surfaceContainerLow = Color(0xFFF8F3ED);
  static const Color onBackground = Color(0xFF1D1B18); // :12
  static const Color onSurface = Color(0xFF1D1B18); // :13
  static const Color onSurfaceVariant = Color(0xFF524346); // :14
  static const Color primary = Color(0xFF894C5C); // :16
  static const Color primaryContainer = Color(0xFFF4A7B9); // :18
  static const Color onPrimaryContainer = Color(0xFF733949);
  static const Color secondary = Color(0xFF78555E);
  static const Color secondaryContainer = Color(0xFFFFD1DC); // :23
  static const Color tertiary = Color(0xFF8B4E38); // :25
  static const Color tertiaryContainer = Color(0xFFF8A98E); // :26
  static const Color outline = Color(0xFF847376);
  static const Color outlineVariant = Color(0xFFD6C1C5); // :29
  static const Color error = Color(0xFFE54848);
  static const Color errorContainer = Color(0xFFFFE4E4);

  /// 账目/成功态用的绿（原版在糖币流水里用的就是这枚绿）
  static const Color success = Color(0xFF16A34A);
}

/// ② 字体 —— 1:1 对齐原生 `ui/theme/Type.kt`
///
/// 原生 `Typography` 的每一条都指定了 `RomanticRound`（浪漫雅圆，
/// `app/src/main/res/font/romantic_round.ttf`，184KB 子集）。
/// 这里把同样的字号 / 行高 / 字重复刻一遍，页面里直接用
/// `Theme.of(context).textTheme.xxx` 就能拿到和原版一模一样的排版。
class CozyType {
  const CozyType._();

  /// 与 pubspec.yaml 的 `fonts: family:` 一致
  static const String family = 'RomanticRound';

  static TextStyle _s(double size, double lineHeight, FontWeight weight) =>
      TextStyle(
        fontFamily: family,
        fontSize: size,
        height: lineHeight / size,
        fontWeight: weight,
        color: CozyPalette.onSurface,
      );

  static final TextTheme textTheme = TextTheme(
    // Type.kt: displayLarge
    displayLarge: _s(30, 36, FontWeight.w700),
    // headlineMedium
    headlineMedium: _s(22, 28, FontWeight.w700),
    // headlineSmall
    headlineSmall: _s(20, 26, FontWeight.w700),
    // titleLarge
    titleLarge: _s(18, 24, FontWeight.w600),
    // titleMedium
    titleMedium: _s(16, 22, FontWeight.w600),
    // titleSmall
    titleSmall: _s(14, 20, FontWeight.w600),
    // bodyLarge
    bodyLarge: _s(15, 22, FontWeight.w400),
    // bodyMedium
    bodyMedium: _s(14, 21, FontWeight.w400),
    // bodySmall
    bodySmall: _s(12, 18, FontWeight.w400),
    // labelLarge
    labelLarge: _s(14, 18, FontWeight.w500),
    // labelMedium
    labelMedium: _s(12, 16, FontWeight.w500),
    // labelSmall
    labelSmall: _s(11, 14, FontWeight.w500),
  );
}

/// ③ 光影令牌 —— 纯白底上的「分层」全靠这三件套
class CozyLight {
  const CozyLight._();

  /// 发丝描边 rgba(29,27,24,.06) —— 白底上定义边界的唯一手段
  static const Color hairline = Color(0x0F1D1B18);

  /// 环境投影：近距离，负责把边缘「描」出来
  static const BoxShadow ambient = BoxShadow(
    color: Color(0x0A1D1B18), // rgba(29,27,24,.04)
    blurRadius: 2,
    offset: Offset(0, 1),
  );

  /// 主投影：远距离大扩散，负责把卡片从纸面「托」起来
  static const BoxShadow key = BoxShadow(
    color: Color(0x141D1B18), // rgba(29,27,24,.08)
    blurRadius: 24,
    spreadRadius: -6,
    offset: Offset(0, 8),
  );

  /// 卡片标准双层投影
  static const List<BoxShadow> cardShadow = <BoxShadow>[ambient, key];

  /// 玻璃顶部高光刃 / 底部环境反光 / 左右侧缘反光
  static const Color specularTop = Color(0xFAFFFFFF);
  static const Color specularBottom = Color(0x85FFFFFF);
  static const Color specularSide = Color(0x4DFFFFFF);

  /// 底栏后的常驻暖色环境光：让玻璃在任何页面、任何滚动位置都有颜色可折。
  ///
  /// ★ 强度订正（22% → 12% → 8%）：这层雾就铺在胶囊正后方，而胶囊本体只有 8% 填充，
  /// 于是「透过玻璃看到的东西」几乎等于这层粉 —— 通透感被它换成了粉色塑料感。
  /// 参考 APK 根本没有这层雾，它的玻璃背后就是页面本身。
  /// 这里只保留更淡的一层当折射素材，不再让它当底色调。
  static const RadialGradient dockHaze = RadialGradient(
    center: Alignment(0, 1.12),
    radius: 1.35,
    colors: <Color>[
      Color(0x0FFFD1DC), // #FFD1DC  6%
      Color(0x06F8A98E), // #F8A98E  2.5%
      Color(0x00FFFFFF),
    ],
    stops: <double>[0.0, 0.44, 0.78],
  );

  /// 底栏胶囊的「合成斜面」——顶部一条亮刃、底部一点压暗、中段完全留空。
  ///
  /// 来源：参考 APK 的 `interactive_indicator.frag`（原作者注释）：
  /// 平 2D 环上每一点受光相同，看起来就是「假的」；真实 3D 倒角的顶部法线
  /// 朝向观察者、会 catch 更多环境光。它用
  /// `bevelGradient = -surfaceNormal.y * kBevelStrength`（kBevelStrength = 0.18）
  /// 模拟这个朝向差。
  ///
  /// 关键：**它不是描边** —— 上/下各一条软带、中段全透明，
  /// 所以不会再给胶囊添第二圈轮廓（那正是之前「好像有两侧」的来源）。
  static const LinearGradient dockBevel = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: <Color>[
      Color(0x33FFFFFF), // 顶部刃光（受光面）
      Color(0x00FFFFFF),
      Color(0x00FFFFFF),
      Color(0x121D1B18), // 底部微沉（背光面）
    ],
    stops: <double>[0.0, 0.24, 0.78, 1.0],
  );

  /// 页面内容末端的暖粉渐变（折射素材），高 120
  static const LinearGradient contentTailWarmth = LinearGradient(
    begin: Alignment.bottomCenter,
    end: Alignment.topCenter,
    colors: <Color>[Color(0x29FFD1DC), Color(0x14FFD1DC), Color(0x00FFD1DC)],
    stops: <double>[0.0, 0.45, 1.0],
  );
}

/// ③ 玻璃规格 —— 对齐原生 `drawBackdrop(vibrancy + blur(8dp) + lens(24dp, chromaticAberration))`
///
/// 关键订正：原版 `onDrawSurface` 用的是 `#FFF7F2 @ 0.38` 暖白实色。
/// 0.38 的实色叠在纯白上合成 `#FFFBFA`，与纯白只差约 5 个色阶 ——
/// 整块折射被这层米色糊死，这才是「不透、只是模糊了」的真正原因。
/// 这里把填充压到 6%~7%，把「像玻璃」这件事交还给折射 / 色散 / 高光刃。
///
/// ★ 模拟器四轮 A/B 实测（内容压在玻璃后面看底层文字）：
///   blur 4.0 → 文字糊成一团灰糊（用户原话「只是模糊了」）
///   blur 2.0 → 同样糊，且 fill 到 10% 更奶白
///   blur 1.5 → 文字被拉成长条糊影
///   blur 0.5 → 已能看出底层文字发虚、胶囊轮廓变弱
///   blur 0.0 → 底层文字清晰可辨，只在边缘有轻微弯折重影 → **真玻璃**
///   ⇒ **blur 是主导项，`blur` 不是「更糊更玻璃」，而是「一糊就死」。**
class CozyGlassSpec {
  const CozyGlassSpec._();

  /// 填充：6% 暖白（原版 38% 实色 → 0.06）
  static const Color fill = Color(0x0FFFF7F2);

  /// 底栏玻璃填充：7% 暖白。
  /// 底栏比卡片需要多一点「存在感」，否则纯白底上整条胶囊会消失；
  /// 7% 是实测里既能立住轮廓、又不发奶的上限。
  static const Color dockFill = Color(0x12FFF7F2);

  /// 高斯模糊半径。
  /// 底栏走 `liquid_glass_widgets`（blur 0 + 折射），这里只服务 `LiquidDropGlass`
  /// 这类 `BackdropFilter` 卡片 —— 它没有折射可用，全靠这层模糊，
  /// 但同样不能重：4.0 是「看得出是一层玻璃、又还看得清底下」的折中。
  static const double blurSigma = 4.0;

  /// 背景饱和度提升（对应 CSS backdrop-filter: saturate(190%)）
  static const double saturation = 1.9;

  /// 玻璃内部高光
  static const double glow = 0.10;

  /// 边缘色散环（暖冷两道）
  static const Color lensWarm = Color(0x21FF9696);
  static const Color lensCool = Color(0x1A829BFF);

  /// 饱和度矩阵：s = 1.9 时把背景的暖粉/焦糖色拉出来，玻璃才「有色」
  static List<double> saturationMatrix([double s = saturation]) {
    const double lr = 0.2126, lg = 0.7152, lb = 0.0722;
    final double inv = 1.0 - s;
    return <double>[
      lr * inv + s, lg * inv, lb * inv, 0, 0, //
      lr * inv, lg * inv + s, lb * inv, 0, 0, //
      lr * inv, lg * inv, lb * inv + s, 0, 0, //
      0, 0, 0, 1, 0, //
    ];
  }

  /// 模糊 + 饱和 合成后的背板滤镜
  static ui.ImageFilter backdropFilter({
    double blurSigma = CozyGlassSpec.blurSigma,
    double saturation = CozyGlassSpec.saturation,
  }) {
    return ui.ImageFilter.compose(
      outer: ColorFilter.matrix(saturationMatrix(saturation)),
      inner: ui.ImageFilter.blur(sigmaX: blurSigma, sigmaY: blurSigma),
    );
  }
}

/// ④ 底栏度量 —— 对齐原生 `FloatingLiquidBottomBar`
class CozyDock {
  const CozyDock._();

  static const double height = 68; // MainActivity.kt height(68.dp)
  static const double radius = 36; // RoundedCornerShape(36.dp)
  static const double sideMargin = 20; // padding(horizontal = 20.dp)
  static const double bottomMargin = 14; // padding(bottom = navBars + 14.dp)
  static const double maxWidth = 430; // widthIn(max = 430.dp)

  /// 【关键】列表页内容底部留白。
  /// 原版各页留了 172~188dp，内容根本滑不到底栏后面，透镜无物可折。
  /// 收到 104 之后，末尾卡片才会真正滑进胶囊下方被弯折。
  static const double clearance = 104;

  /// 【真机修正】页面内容底部留白 = 胶囊让位 104 + 系统导航栏 inset。
  /// 只给 104 而不管系统导航栏时，末页内容会被悬浮底栏压住
  /// （三键导航/手势条设备尤其明显），这是「末尾内容被底栏压住」的根因。
  static double clearanceOf(BuildContext context) =>
      clearance + MediaQuery.paddingOf(context).bottom;
}

/// ⑤ 兼容层 —— 旧代码里的 `CozyTheme.xxx` 全部转发到新令牌
class CozyTheme {
  const CozyTheme._();

  static const Color pureWhite = CozyPalette.background;
  static const Color cardSurface = CozyPalette.surface;
  static const Color cardStroke = CozyLight.hairline;
  static const Color primaryPink = CozyPalette.primary;
  static const Color softPink = Color(0xFFFFEFF3);
  static const Color sweetCocoa = CozyPalette.onSurface;
  static const Color mutedText = CozyPalette.onSurfaceVariant;

  /// 卡片投影快捷方式
  static const List<BoxShadow> cardShadow = CozyLight.cardShadow;
}

// ══════════════════════════════════════════════════════════════════════════════
//  组件
// ══════════════════════════════════════════════════════════════════════════════

/// 页面骨架：纯白满屏底 + 可选的暖色装饰圆点
class CozyPage extends StatelessWidget {
  const CozyPage({
    super.key,
    required this.child,
    this.decorative = false,
    this.safeTop = true,
  });

  final Widget child;
  final bool decorative;
  final bool safeTop;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(color: CozyPalette.background),
      child: Stack(
        children: <Widget>[
          if (decorative) const Positioned.fill(child: IgnorePointer(child: CozyDecorations())),
          SafeArea(top: safeTop, bottom: false, child: child),
        ],
      ),
    );
  }
}

/// 原版 `CozyDecorations()` 的等值实现：3 枚暖色圆 + 2 枚心形水印
class CozyDecorations extends StatelessWidget {
  const CozyDecorations({super.key});

  @override
  Widget build(BuildContext context) => CustomPaint(painter: _CozyDecorationsPainter());
}

class _CozyDecorationsPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    void circle(Offset c, double r, Color color) {
      canvas.drawCircle(c, r, Paint()..color = color..style = PaintingStyle.fill);
    }

    circle(Offset(size.width * 0.86, size.height * 0.08), size.width * 0.30,
        CozyPalette.secondaryContainer.withValues(alpha: 0.12));
    circle(Offset(size.width * 0.08, size.height * 0.30), size.width * 0.24,
        CozyPalette.tertiaryContainer.withValues(alpha: 0.08));
    circle(Offset(size.width * 0.72, size.height * 0.62), size.width * 0.22,
        CozyPalette.primaryContainer.withValues(alpha: 0.10));

    void heart(Offset center, double s, Color color) {
      final Path p = Path()
        ..moveTo(center.dx, center.dy + s * 0.75)
        ..cubicTo(center.dx - s * 1.35, center.dy - s * 0.15, center.dx - s * 0.45,
            center.dy - s * 0.95, center.dx, center.dy - s * 0.25)
        ..cubicTo(center.dx + s * 0.45, center.dy - s * 0.95, center.dx + s * 1.35,
            center.dy - s * 0.15, center.dx, center.dy + s * 0.75)
        ..close();
      canvas.drawPath(p, Paint()..color = color..style = PaintingStyle.fill);
    }

    heart(Offset(size.width * 0.14, size.height * 0.78), size.width * 0.10,
        CozyPalette.primaryContainer.withValues(alpha: 0.12));
    heart(Offset(size.width * 0.92, size.height * 0.44), size.width * 0.08,
        CozyPalette.tertiary.withValues(alpha: 0.06));
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// 卡片 —— 原版 `CozyCard` 的等值实现
///
/// 白底上「纸片浮在桌面上」的全部信息量都来自：
///   暖白填充 `#FFFCF8`（比页面白微暖一档）
/// + 6% 中性发丝边（不是粉色边——粉边在白底上会显脏）
/// + 双层柔和投影（近处描边 + 远处托起）
class CozyCard extends StatefulWidget {
  const CozyCard({
    super.key,
    required this.child,
    this.onTap,
    this.padding = const EdgeInsets.all(16),
    this.margin,
    this.radius = 28,
    this.color,
    this.borderColor,
    this.shadows,
    this.width,
    this.height,
  });

  final Widget child;
  final VoidCallback? onTap;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;
  final double radius;
  final Color? color;
  final Color? borderColor;
  final List<BoxShadow>? shadows;
  final double? width;
  final double? height;

  @override
  State<CozyCard> createState() => _CozyCardState();
}

class _CozyCardState extends State<CozyCard> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final BorderRadius radius = BorderRadius.circular(widget.radius);
    return AnimatedScale(
      scale: _pressed ? 0.975 : 1.0,
      duration: const Duration(milliseconds: 140),
      curve: Curves.easeOut,
      child: Container(
        width: widget.width,
        height: widget.height,
        margin: widget.margin,
        decoration: BoxDecoration(
          color: widget.color ?? CozyPalette.surface,
          borderRadius: radius,
          border: Border.all(color: widget.borderColor ?? CozyLight.hairline),
          boxShadow: widget.shadows ?? CozyLight.cardShadow,
        ),
        child: ClipRRect(
          borderRadius: radius,
          child: widget.onTap == null
              ? Padding(padding: widget.padding, child: widget.child)
              : GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTapDown: (_) => setState(() => _pressed = true),
                  onTapUp: (_) => setState(() => _pressed = false),
                  onTapCancel: () => setState(() => _pressed = false),
                  onTap: widget.onTap,
                  child: Padding(padding: widget.padding, child: widget.child),
                ),
        ),
      ),
    );
  }
}

/// 顶栏 —— 原版 `CozyMainTopBar` 的修正版
///
/// 原版底色是 `#FFFCF8 @ 94%`，叠在纯白上几乎不可见，反而把页面切出一块米色。
/// 这里改成纯白 + 仅在滚动时浮现 0.5px 发丝线。
class CozyMainTopBar extends StatelessWidget {
  const CozyMainTopBar({
    super.key,
    required this.title,
    this.leading,
    this.trailing,
    this.scrolled = false,
    this.height = 72,
  });

  final Widget title;
  final Widget? leading;
  final Widget? trailing;
  final bool scrolled;
  final double height;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: height,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(
        color: CozyPalette.background,
        border: Border(
          bottom: BorderSide(
            color: scrolled ? CozyLight.hairline : Colors.transparent,
            width: 0.5,
          ),
        ),
      ),
      child: Row(
        children: <Widget>[
          ?leading,
          Expanded(child: Center(child: title)),
          ?trailing,
        ],
      ),
    );
  }
}

/// 底栏后方的常驻暖色环境光。
/// 纯白卡片堆在底栏下面时，玻璃仍是「白得像块板」—— 这层光就是给它一点颜色可折。
class CozyDockHaze extends StatelessWidget {
  const CozyDockHaze({super.key, this.height = 140});

  final double height;

  @override
  Widget build(BuildContext context) => IgnorePointer(
        child: SizedBox(
          height: height,
          child: const DecoratedBox(
            decoration: BoxDecoration(gradient: CozyLight.dockHaze),
          ),
        ),
      );
}

/// 页面内容末端的暖粉渐变条 —— 保证滚动到底时底栏下方永远有折射素材
class CozyContentTail extends StatelessWidget {
  const CozyContentTail({super.key, this.height = 120});

  final double height;

  @override
  Widget build(BuildContext context) => IgnorePointer(
        child: SizedBox(
          height: height,
          child: const DecoratedBox(
            decoration: BoxDecoration(gradient: CozyLight.contentTailWarmth),
          ),
        ),
      );
}

/// 晶莹液态水滴容器（页面内悬浮玻璃，如购物车条 / 登录面板）
///
/// 配方与底栏同源：低填充 + 高饱和背板 + 顶部高光刃 + 底部环境反光。
class LiquidDropGlass extends StatelessWidget {
  const LiquidDropGlass({
    super.key,
    required this.child,
    this.width,
    this.height = 70.0,
    this.borderRadius = const BorderRadius.all(Radius.circular(35.0)),
    this.padding = const EdgeInsets.symmetric(horizontal: 14.0),
    this.onTap,
    this.blurSigma = CozyGlassSpec.blurSigma,
  });

  final Widget child;
  final double? width;
  final double height;
  final BorderRadius borderRadius;
  final EdgeInsetsGeometry padding;
  final VoidCallback? onTap;
  final double blurSigma;

  @override
  Widget build(BuildContext context) {
    final Widget body = Stack(
      children: <Widget>[
        // 填充：6% 暖白（原来是 25%~60% 白的渐变，那正是「不透」的元凶）
        Positioned.fill(
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: CozyGlassSpec.fill,
              borderRadius: borderRadius,
            ),
          ),
        ),
        // 顶部高光刃：1.5px，两端淡出
        Positioned(
          top: 1,
          left: 22,
          right: 22,
          height: 1.5,
          child: DecoratedBox(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(2),
              gradient: LinearGradient(
                colors: <Color>[
                  CozyLight.specularTop.withValues(alpha: 0.0),
                  CozyLight.specularTop,
                  CozyLight.specularTop.withValues(alpha: 0.0),
                ],
                stops: const <double>[0.0, 0.5, 1.0],
              ),
            ),
          ),
        ),
        // 底部环境反光
        Positioned(
          bottom: 0,
          left: 26,
          right: 26,
          height: 1,
          child: DecoratedBox(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(2),
              gradient: LinearGradient(
                colors: <Color>[
                  CozyLight.specularBottom.withValues(alpha: 0.0),
                  CozyLight.specularBottom,
                  CozyLight.specularBottom.withValues(alpha: 0.0),
                ],
                stops: const <double>[0.0, 0.5, 1.0],
              ),
            ),
          ),
        ),
        // 凸透镜顶部月牙聚光弧（原版 sheen arch）
        Positioned(
          top: 1,
          left: 20,
          right: 20,
          height: height * 0.44,
          child: IgnorePointer(
            child: DecoratedBox(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.all(Radius.circular(height * 0.45)),
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: <Color>[
                    Colors.white.withValues(alpha: 0.34),
                    Colors.white.withValues(alpha: 0.0),
                  ],
                ),
              ),
            ),
          ),
        ),
        Center(child: child),
      ],
    );

    final Widget glass = DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: borderRadius,
        border: Border.all(color: CozyLight.specularSide, width: 1),
      ),
      child: body,
    );

    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        borderRadius: borderRadius,
        boxShadow: CozyLight.cardShadow,
      ),
      child: ClipRRect(
        borderRadius: borderRadius,
        child: BackdropFilter(
          filter: CozyGlassSpec.backdropFilter(blurSigma: blurSigma),
          child: onTap != null
              // opaque：整块玻璃都要能点。内部用 DecoratedBox 铺底，
              // 默认 deferToChild 时空白区域收不到指针事件。
              ? GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: onTap,
                  child: glass,
                )
              : glass,
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  共享组件 —— 1:1 对齐原生 `ui/components/StitchNativeComponents.kt`
//  页面里请一律用这些，不要自己拼 Surface + RoundedCornerShape，
//  否则间距 / 圆角 / 边框透明度会和原版走偏。
// ══════════════════════════════════════════════════════════════════════════════

/// 原生 `CozyTopBar`（StitchNativeComponents.kt:132）
///
/// 左圆形图标徽 + 标题/副标题 + 右侧自由插槽。
/// 用于二级页（小店管理 / 纪念日 / 糖币 …）。
class CozyTopBar extends StatelessWidget {
  const CozyTopBar({
    super.key,
    required this.title,
    this.subtitle,
    this.leadingIcon,
    this.trailing,
  });

  final String title;
  final String? subtitle;
  final IconData? leadingIcon;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
      child: Row(
        children: <Widget>[
          if (leadingIcon != null) ...<Widget>[
            Container(
              padding: const EdgeInsets.all(9),
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: Color(0xB3FFD1DC), // SecondaryContainer @ 0.7
              ),
              child: Icon(leadingIcon, size: 20, color: CozyPalette.primary),
            ),
            const SizedBox(width: 12),
          ],
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Text(
                  title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.headlineSmall!.copyWith(
                        fontWeight: FontWeight.w900,
                        color: CozyPalette.onSurface,
                      ),
                ),
                if (subtitle != null) ...<Widget>[
                  const SizedBox(height: 3),
                  Text(
                    subtitle!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall!.copyWith(
                          color: CozyPalette.onSurfaceVariant,
                        ),
                  ),
                ],
              ],
            ),
          ),
          if (trailing != null) ...<Widget>[
            const SizedBox(width: 12),
            trailing!,
          ],
        ],
      ),
    );
  }
}

/// 原生 `CozyPill`（StitchNativeComponents.kt:259）
class CozyPill extends StatelessWidget {
  const CozyPill({
    super.key,
    required this.text,
    this.selected = false,
    this.color = CozyPalette.primary,
    this.onTap,
  });

  final String text;
  final bool selected;
  final Color color;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final Widget pill = Container(
      padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 7),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(999),
        color: selected ? color : color.withValues(alpha: 0.12),
        border: Border.all(
          color: color.withValues(alpha: selected ? 0.0 : 0.22),
        ),
      ),
      child: Text(
        text,
        style: Theme.of(context).textTheme.labelMedium!.copyWith(
              fontWeight: FontWeight.w700,
              color: selected ? CozyPalette.surface : color,
            ),
      ),
    );
    if (onTap == null) return pill;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: pill,
    );
  }
}

/// 原生 `CozyPrimaryButton`（StitchNativeComponents.kt:283）
///
/// 高 56 / 全圆角 / 主色实心 / 按下缩到 0.97。
class CozyPrimaryButton extends StatefulWidget {
  const CozyPrimaryButton({
    super.key,
    required this.text,
    required this.onTap,
    this.enabled = true,
  });

  final String text;
  final VoidCallback onTap;
  final bool enabled;

  @override
  State<CozyPrimaryButton> createState() => _CozyPrimaryButtonState();
}

class _CozyPrimaryButtonState extends State<CozyPrimaryButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final bool on = widget.enabled;
    return AnimatedScale(
      scale: _pressed && on ? 0.97 : 1.0,
      duration: const Duration(milliseconds: 120),
      curve: Curves.easeOut,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: on ? (_) => setState(() => _pressed = true) : null,
        onTapUp: on ? (_) => setState(() => _pressed = false) : null,
        onTapCancel: on ? () => setState(() => _pressed = false) : null,
        onTap: on ? widget.onTap : null,
        child: Container(
          height: 56,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(999),
            color: on
                ? CozyPalette.primary
                : CozyPalette.primary.withValues(alpha: 0.45),
          ),
          child: Text(
            widget.text,
            style: Theme.of(context).textTheme.titleMedium!.copyWith(
                  fontWeight: FontWeight.w900,
                  color: on
                      ? CozyPalette.surface
                      : CozyPalette.surface.withValues(alpha: 0.8),
                ),
          ),
        ),
      ),
    );
  }
}

/// 原生 `CozyIconBadge`（StitchNativeComponents.kt:301）：44×44 / 圆角 16 / 图标 23
class CozyIconBadge extends StatelessWidget {
  const CozyIconBadge({
    super.key,
    required this.icon,
    this.background = const Color(0x9EFFD1DC), // SecondaryContainer @ 0.62
    this.tint = CozyPalette.primary,
    this.size = 44,
  });

  final IconData icon;
  final Color background;
  final Color tint;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Icon(icon, size: 23, color: tint),
    );
  }
}

/// 原生 `cozyTextFieldColors()`（StitchNativeComponents.kt:316）
///
/// 用法：`InputDecoration(..., **cozyInputDecoration(...))` 不好展开，
/// 所以这里直接给一个返回 `InputDecoration` 的工厂。
InputDecoration cozyInputDecoration({
  String? labelText,
  String? hintText,
  Widget? prefixIcon,
  Widget? suffixIcon,
  bool filled = true,
  EdgeInsetsGeometry? contentPadding,
  String? errorText,
}) {
  return InputDecoration(
    labelText: labelText,
    hintText: hintText,
    prefixIcon: prefixIcon,
    suffixIcon: suffixIcon,
    errorText: errorText,
    filled: filled,
    fillColor: const Color(0xE0F8F3ED), // SurfaceContainerLow @ 0.88
    contentPadding: contentPadding ??
        const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: Color(0xB8D6C1C5)), // OutlineVariant @ .72
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: Color(0xB8D6C1C5)),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: CozyPalette.primaryContainer, width: 1.6),
    ),
    errorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: CozyPalette.error),
    ),
    focusedErrorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: CozyPalette.error, width: 1.6),
    ),
    labelStyle: const TextStyle(
      fontFamily: CozyType.family,
      color: CozyPalette.onSurfaceVariant,
    ),
    floatingLabelStyle: const TextStyle(
      fontFamily: CozyType.family,
      color: CozyPalette.primary,
    ),
    hintStyle: const TextStyle(
      fontFamily: CozyType.family,
      color: CozyPalette.onSurfaceVariant,
    ),
  );
}

/// 原生 `StitchNativeComponents.kt` 顶部那组别名（CozyCream / CozyPink / …）
///
/// 已存在的页面用的是这套名字，保留下来免得逐处替换。
class CozyInk {
  const CozyInk._();

  static const Color cream = CozyPalette.background;
  static const Color surface = CozyPalette.surface;
  static const Color pink = CozyPalette.primaryContainer;
  static const Color rose = CozyPalette.primary;
  static const Color cherry = CozyPalette.secondaryContainer;
  static const Color cocoa = CozyPalette.onSurface;
  static const Color muted = CozyPalette.onSurfaceVariant;
  static const Color terracotta = CozyPalette.tertiary;
  static const Color terracottaSoft = CozyPalette.tertiaryContainer;
  static const Color border = CozyPalette.outlineVariant;
}

/// ⑥ 头像 —— 三种来源统一处理（首页两个座位 / 我的页 / 编辑弹层 / 注册页都用它）
///
/// ① 空串          → `fallback`（调用方给爪子、餐具等兜底图标）
/// ② `data:image/…` → `Image.memory`：本地选的头像，base64 存在 `profiles.avatar_url`
/// ③ `http(s)://…`  → `Image.network`：历史字段与将来的 storage 桶 URL
///
/// 为什么头像会以 data URI 形式存在库里：这个 Supabase 项目
/// `GET /storage/v1/bucket` 返回空数组（一个桶都没有），建桶必须 service_role，
/// 而 service_role 不能出现在客户端。详见 `SupabaseApi.updateProfile` 注释。
/// 这里统一解码，是因为伴侣侧读到的 `partner_avatar_url` 也可能是 data URI。
class CozyAvatar extends StatelessWidget {
  const CozyAvatar({
    super.key,
    required this.url,
    required this.size,
    required this.fallback,
    this.fit = BoxFit.cover,
  });

  final String url;
  final double size;
  final Widget fallback;
  final BoxFit fit;

  static bool isDataUri(String value) => value.startsWith('data:image');

  static Uint8List? decodeDataUri(String value) {
    if (!isDataUri(value)) return null;
    final int comma = value.indexOf(',');
    if (comma < 0) return null;
    try {
      return base64Decode(value.substring(comma + 1));
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final Uint8List? bytes = decodeDataUri(url);
    if (bytes != null && bytes.isNotEmpty) {
      return Image.memory(
        bytes,
        width: size,
        height: size,
        fit: fit,
        gaplessPlayback: true,
        errorBuilder: (_, _, _) => fallback,
      );
    }
    if (url.startsWith('http')) {
      return Image.network(
        url,
        width: size,
        height: size,
        fit: fit,
        errorBuilder: (_, _, _) => fallback,
      );
    }
    return fallback;
  }
}
