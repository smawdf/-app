import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../../data/app_state.dart';
import '../pages/ordering_page.dart';
import '../pages/orders_page.dart';
import '../pages/profile_page.dart';
import '../theme/cozy_glass.dart';

/// 主界面外壳：纯白底 + liquid_glass_widgets 的 iOS 26 风格液态玻璃底栏
///
/// 使用 GlassScaffold 统一接管背景采样、层级次序与边缘渐变，
/// 底栏由 GlassTabBar.bottom 渲染（库会自动将其提升到 premium 折射质量）。
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
    if (index == 0) state.refreshMenu();
    if (index == 1) state.refreshOrders();
    if (index == 2) state.refreshTransactions();
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return ListenableBuilder(
      listenable: state,
      builder: (context, _) {
        // WebSocket 推送到达时，用 SnackBar 提示伴侣动态
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

        return GlassScaffold(
          backgroundColor: CozyTheme.pureWhite,
          statusBarStyle: GlassStatusBarStyle.light,
          // 底栏同样需要 Material 祖先，否则 tab 文字会退化成红字+黄双下划线错误样式
          bottomBar: Material(
            type: MaterialType.transparency,
            child: GlassTabBar.bottom(
              selectedIndex: _tab,
              onTabSelected: _onTabTap,
              tabs: const [
                GlassTab(icon: Icon(Icons.restaurant), label: '点餐'),
                GlassTab(icon: Icon(Icons.receipt_long), label: '订单'),
                GlassTab(icon: Icon(Icons.pets), label: '我的'),
              ],
            ),
          ),
          // GlassScaffold 内部是 CupertinoPageScaffold，树里没有 Material 祖先，
          // 会导致 Text 退化成 Flutter 的「缺 Material 祖先」错误样式
          // （红字 + 黄色双下划线）。这里显式补一层透明 Material。
          body: Material(
            type: MaterialType.transparency,
            child: SafeArea(
              bottom: false,
              child: IndexedStack(
                index: _tab,
                children: const [OrderingPage(), OrdersPage(), ProfilePage()],
              ),
            ),
          ),
        );
      },
    );
  }
}
