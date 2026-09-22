import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../../data/app_state.dart';
import '../pages/discover_page.dart';
import '../pages/home_page.dart';
import '../pages/ordering_page.dart';
import '../pages/orders_page.dart';
import '../pages/profile_page.dart';
import '../theme/cozy_glass.dart';

/// 主界面外壳：纯白底 + 基于 liquid_glass_widgets 的 5 大完整 Tab 水滴液态玻璃底栏
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
        // WebSocket 实时推送提示
        final toast = state.toast;
        if (toast != null && toast != _lastToast) {
          _lastToast = toast;
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (!mounted) return;
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(toast, style: const TextStyle(fontWeight: FontWeight.w700)),
                backgroundColor: CozyTheme.sweetCocoa,
                behavior: SnackBarBehavior.floating,
                margin: const EdgeInsets.fromLTRB(20, 0, 20, 130),
                duration: const Duration(seconds: 3),
              ),
            );
            AppState.instance.clearToast();
          });
        }

        final pages = [
          HomePage(onNavigateTab: (idx) => _onTabTap(idx)),
          const OrderingPage(),
          DiscoverPage(onGoToOrdering: () => _onTabTap(1)),
          const OrdersPage(),
          const ProfilePage(),
        ];

        return GlassScaffold(
          backgroundColor: CozyTheme.pureWhite,
          statusBarStyle: GlassStatusBarStyle.dark,
          // 悬浮 5 大 Tab 水滴液态玻璃底栏
          bottomBar: Material(
            type: MaterialType.transparency,
            child: GlassTabBar.bottom(
              selectedIndex: _tab,
              onTabSelected: _onTabTap,
              tabs: const [
                GlassTab(icon: Icon(Icons.home_outlined), label: '首页'),
                GlassTab(icon: Icon(Icons.restaurant_outlined), label: '点餐'),
                GlassTab(icon: Icon(Icons.explore_outlined), label: '发现'),
                GlassTab(icon: Icon(Icons.receipt_long_outlined), label: '订单'),
                GlassTab(icon: Icon(Icons.pets_outlined), label: '我的'),
              ],
            ),
          ),
          // 外层包透明 Material，杜绝无 Material 祖先引起的红字黄线
          body: Material(
            type: MaterialType.transparency,
            child: SafeArea(
              bottom: false,
              child: IndexedStack(
                index: _tab,
                children: pages,
              ),
            ),
          ),
        );
      },
    );
  }
}
