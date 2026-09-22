import 'package:fluid_glass/fluid_glass.dart';
import 'package:flutter/material.dart';

import '../../data/app_state.dart';
import '../pages/ordering_page.dart';
import '../pages/orders_page.dart';
import '../pages/profile_page.dart';
import '../theme/backdrop_scope.dart';
import '../theme/cozy_glass.dart';

/// 主界面外壳：纯白底 + 基于 Kyant 移植版 FluidGlass 的真实折射水滴底栏
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _tab = 0;
  String? _lastToast;
  final LayerBackdrop _backdrop = LayerBackdrop();

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

        final pages = const [OrderingPage(), OrdersPage(), ProfilePage()];

        return BackdropScope(
          backdrop: _backdrop,
          child: Scaffold(
            backgroundColor: CozyTheme.pureWhite,
            body: SafeArea(
              bottom: false,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  // 1. 被折射的内容层（BackdropLayer，负责向玻璃输出实时采样纹理）
                  Positioned.fill(
                    child: BackdropLayer(
                      backdrop: _backdrop,
                      child: IndexedStack(index: _tab, children: pages),
                    ),
                  ),

                  // 2. 官方原生 FluidGlass LiquidBottomTabs 悬浮折射底栏
                  Positioned(
                    left: 20,
                    right: 20,
                    bottom: 24,
                    child: LiquidBottomTabs(
                      backdrop: _backdrop,
                      selectedTabIndex: _tab,
                      onTabSelected: _onTabTap,
                      tabsCount: 3,
                      children: [
                        LiquidBottomTab(
                          onPressed: () => _onTabTap(0),
                          children: const [
                            Icon(Icons.restaurant, size: 20, color: CozyTheme.sweetCocoa),
                            SizedBox(height: 2),
                            Text('点餐', style: TextStyle(color: CozyTheme.sweetCocoa, fontSize: 11.5, fontWeight: FontWeight.w800)),
                          ],
                        ),
                        LiquidBottomTab(
                          onPressed: () => _onTabTap(1),
                          children: const [
                            Icon(Icons.receipt_long, size: 20, color: CozyTheme.sweetCocoa),
                            SizedBox(height: 2),
                            Text('订单', style: TextStyle(color: CozyTheme.sweetCocoa, fontSize: 11.5, fontWeight: FontWeight.w800)),
                          ],
                        ),
                        LiquidBottomTab(
                          onPressed: () => _onTabTap(2),
                          children: const [
                            Icon(Icons.pets, size: 20, color: CozyTheme.sweetCocoa),
                            SizedBox(height: 2),
                            Text('我的', style: TextStyle(color: CozyTheme.sweetCocoa, fontSize: 11.5, fontWeight: FontWeight.w800)),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
