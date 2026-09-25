import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../theme/cozy_glass.dart';

/// 悬浮 5 大 Tab 的液态玻璃底栏。
///
/// 1:1 对齐 Kyant 开源液态玻璃方案（Kyant0/AndroidLiquidGlass · io.github.kyant0:backdrop）
/// 原生 FloatingLiquidBottomBar 配方：
///   · lens(refractionHeight = 24dp, refractionAmount = 24dp, depthEffect = true, chromaticAberration = true)
///   · blur(8dp) + vibrancy()
///   · highlight(1dp, blur = 1dp)
///   · 彻底剔除人工粉雾、人工渐变斜面、实心粉盘，呈现纯净透明水滴透镜
class CozyGlassDock extends StatelessWidget {
  const CozyGlassDock({
    super.key,
    required this.selectedIndex,
    required this.onTabSelected,
  });

  final int selectedIndex;
  final ValueChanged<int> onTabSelected;

  static const List<GlassTab> tabs = <GlassTab>[
    GlassTab(icon: Icon(Icons.home_outlined), label: '首页'),
    GlassTab(icon: Icon(Icons.restaurant_outlined), label: '点餐'),
    GlassTab(icon: Icon(Icons.explore_outlined), label: '发现'),
    GlassTab(icon: Icon(Icons.receipt_long_outlined), label: '订单'),
    GlassTab(icon: Icon(Icons.pets_outlined), label: '我的'),
  ];

  @override
  Widget build(BuildContext context) {
    return GlassTabBar.bottom(
      selectedIndex: selectedIndex,
      onTabSelected: onTabSelected,
      tabs: tabs,

      // ── 度量：对齐原生 FloatingLiquidBottomBar ──
      barHeight: CozyDock.height,
      barBorderRadius: GlassDefaults.capsuleRadius,
      horizontalPadding: CozyDock.sideMargin,
      verticalPadding: CozyDock.bottomMargin,

      // ── 渲染档位：必须显式要 premium ──
      // 【真机修正】`quality` 不传时，包内默认 `GlassQuality.standard`
      // （`glass_tab_bar.dart:915-919`：null → 继承或默认 standard），
      // 而 standard 走的是轻量着色器、没有折射与色散。真机上底栏压到菜品大图上
      // 就成了一块糊掉的灰板子，也就是「液态玻璃效果不对」。
      // `premium` 才是本文件开头注释声称对齐的 Kyant 配方
      // （lens 折射 + chromaticAberration + 镜面高光），且底栏是静态页脚，
      // 正落在包文档给 premium 划定的适用场景（静态 header/footer）。
      quality: GlassQuality.premium,

      // ── 对齐 Kyant 原版参数 ──
      magnification: 1.04,
      innerBlur: 0.0,
      settings: const LiquidGlassSettings(
        // 【真机修正】0x12FFFFFF 只有 7% 白，压在菜品大图上几乎全被底下的糊图
        // 吃掉，观感偏灰。改用包内自己的 iOS 26 级中性白雾
        // （`glass_sheet_defaults.dart:19` = 0x1FFFFFFF ≈ 12%），
        // 让胶囊在任何底色上都读起来是「透明玻璃」而不是一块灰板。
        glassColor: Color(0x1FFFFFFF),
        // 对齐 Kyant blur(8.dp)
        blur: 8.0,
        // 对齐 Kyant refraction 24dp
        thickness: 24.0,
        refractiveIndex: 1.25,
        chromaticAberration: 0.03,
        saturation: 1.20,
        // 对齐 Kyant Highlight
        lightIntensity: 0.65,
        ambientRim: 0.0,
      ),

      // ── 指示器：纯净透镜，不糊实心粉色圆盘 ──
      showIndicator: true,
      indicatorColor: null, // 默认 10% 微光透镜，静止时不挡底色
      indicatorPinchStrength: 0.4,

      // ── 文字/图标高亮：选中变暖粉/深可可，未选灰 ──
      selectedIconColor: CozyPalette.primary,
      unselectedIconColor: const Color(0xFF8C8480),
      selectedLabelColor: CozyPalette.primary,
      unselectedLabelColor: const Color(0xFF8C8480),
      iconSize: 23,
      labelFontSize: 11,

      // ── 移除外部人工光晕，杜绝假双边 ──
      interactionGlowColor: Colors.transparent,
      glowOpacity: 0.0,
      interactionBehavior: GlassInteractionBehavior.full,
    );
  }
}

/// 底栏整层（去除人工粉雾后，直接呈现纯净通透的悬浮玻璃底栏）
class CozyBottomBarLayer extends StatelessWidget {
  const CozyBottomBarLayer({
    super.key,
    required this.selectedIndex,
    required this.onTabSelected,
  });

  final int selectedIndex;
  final ValueChanged<int> onTabSelected;

  @override
  Widget build(BuildContext context) {
    return CozyGlassDock(
      selectedIndex: selectedIndex,
      onTabSelected: onTabSelected,
    );
  }
}
