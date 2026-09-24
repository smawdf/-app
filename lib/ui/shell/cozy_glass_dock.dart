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

      // ── 对齐 Kyant 原版参数 ──
      magnification: 1.04,
      innerBlur: 0.0,
      settings: const LiquidGlassSettings(
        // 微量中性底色，保持浅色底上的物理边缘立体感
        glassColor: Color(0x12FFFFFF),
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
