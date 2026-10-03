import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../../data/app_state.dart';
import '../pages/discover_page.dart';
import '../pages/home_page.dart';
import '../pages/ordering_page.dart';
import '../pages/orders_page.dart';
import '../pages/profile_page.dart';
import '../theme/couple_theme.dart';
import 'cozy_glass_dock.dart';

/// 主界面外壳
///
/// 纯白底 + 3.0.0 的悬浮液态玻璃底栏（`liquid_glass_widgets`）。
/// 底栏配方已对齐原生 `MainActivity.kt` 的 `drawBackdrop(vibrancy + blur + lens)`：
///   · 填充从 38% 暖白实色压到 6% —— 这是「不透」的根因
///   · 折射变强（magnification / thickness / refractiveIndex 上调）
///   · 模糊降低 —— 玻璃质感靠「折」不靠「糊」
///   · 底栏后方补一层常驻暖色环境光，纯白卡片背景下也有颜色可折
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell>
    with SingleTickerProviderStateMixin {
  int _tab = 0;
  String? _lastToast;

  late final AnimationController _anim = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 220),
  );
  late final CurvedAnimation _bodyCurve = CurvedAnimation(
    parent: _anim,
    curve: Curves.easeOutCubic,
  );

  @override
  void initState() {
    super.initState();
    _anim.value = 1.0;
  }

  @override
  void dispose() {
    _anim.dispose();
    super.dispose();
  }

  void _onTabTap(int idx) {
    if (idx == _tab) return;
    setState(() => _tab = idx);
    _anim.forward(from: 0.0);
  }

  /// 供全页面共用的轻量 Toast
  void showShellToast(String msg) {
    if (!mounted) return;
    if (_lastToast == msg) return;
    _lastToast = msg;
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(msg),
        behavior: SnackBarBehavior.floating,
        duration: const Duration(milliseconds: 1400),
      ),
    );
    Future.delayed(const Duration(milliseconds: 1500), () {
      if (mounted && _lastToast == msg) _lastToast = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([AppState.instance, CoupleThemeManager.instance]),
      builder: (context, _) {
        // 5 个 Tab 页面（原生有 5 个：小饭桌 / 点餐 / 发现 / 订单 / 我的）
        final pages = <Widget>[
          HomePage(onNavigateTab: (idx) => _onTabTap(idx)),
          const OrderingPage(),
          DiscoverPage(onGoToOrdering: () => _onTabTap(1)),
          OrdersPage(onGoOrdering: () => _onTabTap(1)),
          ProfilePage(onNavigateTab: (idx) => _onTabTap(idx)),
        ];

        final theme = context.coupleTheme;

        return GlassScaffold(
          backgroundColor: theme.bgPage,
          statusBarStyle: GlassStatusBarStyle.dark,
          resizeToAvoidBottomInset: false,
          bottomBar: Material(
            type: MaterialType.transparency,
            child: CozyBottomBarLayer(
              selectedIndex: _tab,
              onTabSelected: _onTabTap,
            ),
          ),
          // 外层包透明 Material，杜绝无 Material 祖先引起的红字黄线
          body: Material(
            type: MaterialType.transparency,
            child: SafeArea(
              bottom: false,
              child: FadeTransition(
                opacity: _bodyCurve,
                child: SlideTransition(
                  position: Tween<Offset>(
                    begin: const Offset(0, 0.012),
                    end: Offset.zero,
                  ).animate(_bodyCurve),
                  child: IndexedStack(index: _tab, children: pages),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}
