import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../theme/cozy_glass.dart';
import '../theme/couple_theme.dart';

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
    GlassTab(
      icon: Icon(Icons.home_outlined),
      activeIcon: Icon(Icons.home_rounded),
      label: '首页',
    ),
    GlassTab(
      icon: Icon(Icons.restaurant_outlined),
      activeIcon: Icon(Icons.restaurant_rounded),
      label: '点餐',
    ),
    GlassTab(
      icon: Icon(Icons.explore_outlined),
      activeIcon: Icon(Icons.explore_rounded),
      label: '发现',
    ),
    GlassTab(
      icon: Icon(Icons.receipt_long_outlined),
      activeIcon: Icon(Icons.receipt_long_rounded),
      label: '订单',
    ),
    GlassTab(
      icon: Icon(Icons.pets_outlined),
      activeIcon: Icon(Icons.pets_rounded),
      label: '我的',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final coupleSpec = context.coupleTheme;

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
      quality: GlassQuality.premium,

      // ── 原生液态水滴玻璃底板：极低白雾 + 真实物理透光折射 ──
      // 关键：液态玻璃的核心是「折光」而非「白雾遮挡」。
      // 保持 glassColor 为微量白色透明（Color(0x18FFFFFF)，约 9% 白色透明），
      // 配合 thickness=24dp 与 refractiveIndex=1.25 的水滴透镜，
      // 底部划过的菜品、卡片和文字能清晰透过底栏并产生优雅的折射位移。
      magnification: 1.05,
      innerBlur: 0.0,
      settings: const LiquidGlassSettings(
        glassColor: Color(0x18FFFFFF), // 9% 纯白微光：真正透光的水滴液态玻璃
        blur: 7.0, // 降低毛玻璃高斯模糊，让透光轮廓更清晰纯净
        thickness: 24.0,
        refractiveIndex: 1.25,
        chromaticAberration: 0.03,
        saturation: 1.20,
        lightIntensity: 0.70,
        ambientRim: 0.12,
        shadowElevation: 1.0,
        shadow: <BoxShadow>[
          BoxShadow(
            color: Color(0x12000000),
            blurRadius: 16,
            offset: Offset(0, 5),
          ),
          BoxShadow(
            color: Color(0x08000000),
            blurRadius: 4,
            offset: Offset(0, 1),
          ),
        ],
      ),

      // ── 指示器：纯净微光液态透镜（不挡底色，纯透水滴感） ──
      showIndicator: true,
      indicatorColor: null, // 保持库原生自适应透镜
      indicatorSettings: const LiquidGlassSettings(
        glassColor: Color(0x2EFFFFFF), // 18% 微光提亮：清晰指示当前 Tab 但保持通透
        blur: 5.0,
        thickness: 16.0,
        refractiveIndex: 1.22,
        lightIntensity: 0.85,
        ambientRim: 0.20,
        shadowElevation: 0.8,
        shadow: <BoxShadow>[
          BoxShadow(
            color: Color(0x14000000),
            blurRadius: 6,
            offset: Offset(0, 2),
          ),
        ],
      ),
      indicatorPinchStrength: 0.38,

      // ── 图标与文字：高对比度纯净呈现 ──
      selectedIconColor: coupleSpec.primary,
      unselectedIconColor: const Color(0x993B2A1D),
      selectedLabelColor: coupleSpec.primary,
      unselectedLabelColor: const Color(0x993B2A1D),
      selectedLabelStyle: TextStyle(
        fontSize: 11,
        fontWeight: FontWeight.w700,
        color: coupleSpec.primary,
      ),
      unselectedLabelStyle: const TextStyle(
        fontSize: 11,
        fontWeight: FontWeight.w500,
        color: Color(0x993B2A1D),
      ),
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
