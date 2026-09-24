import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../../data/app_state.dart';
import '../pages/discover_page.dart';
import '../pages/home_page.dart';
import '../pages/ordering_page.dart';
import '../pages/orders_page.dart';
import '../pages/profile_page.dart';
import '../theme/cozy_glass.dart';
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

class _MainShellState extends State<MainShell> {
  int _tab = 0;
  String? _lastToast;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshAll();
    });
  }

  void _onTabTap(int index) {
    setState(() => _tab = index);
    final state = AppState.instance;
    if (index == 0) state.loadMe();
    if (index == 1) state.refreshMenu();
    if (index == 3) state.refreshOrders();
    if (index == 4) state.refreshTransactions();
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return ListenableBuilder(
      listenable: state,
      builder: (context, _) {
        // 实时推送提示
        final toast = state.toast;
        if (toast != null && toast != _lastToast) {
          _lastToast = toast;
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (!mounted) return;
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(toast, style: const TextStyle(fontWeight: FontWeight.w700)),
                backgroundColor: CozyPalette.onSurface,
                behavior: SnackBarBehavior.floating,
                margin: const EdgeInsets.fromLTRB(20, 0, 20, 130),
                duration: const Duration(seconds: 3),
              ),
            );
            AppState.instance.clearToast();
          });
        }

        final pages = <Widget>[
          HomePage(onNavigateTab: (idx) => _onTabTap(idx)),
          const OrderingPage(),
          DiscoverPage(onGoToOrdering: () => _onTabTap(1)),
          OrdersPage(onGoOrdering: () => _onTabTap(1)),
          ProfilePage(onNavigateTab: (idx) => _onTabTap(idx)),
        ];

        return GlassScaffold(
          backgroundColor: CozyPalette.background,
          statusBarStyle: GlassStatusBarStyle.dark,
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
              child: IndexedStack(index: _tab, children: pages),
            ),
          ),
        );
      },
    );
  }
}
