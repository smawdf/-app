import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../pages/ordering_page.dart';
import '../pages/orders_page.dart';
import '../pages/profile_page.dart';
import '../theme/cozy_glass.dart';

/// 主界面外壳：纯白底 + 悬浮水滴玻璃底栏
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _tab = 0;
  String? _lastToast;

  static const _tabs = [
    (emoji: '🍲', label: '点餐'),
    (emoji: '📋', label: '订单'),
    (emoji: '🐾', label: '我的'),
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshAll();
    });
  }

  void _onTabTap(int index) {
    HapticFeedback.selectionClick();
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
        // WebSocket 推送到达时，用 SnackBar 提示伴侣的动态
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

        return Scaffold(
          backgroundColor: CozyTheme.pureWhite,
          body: SafeArea(
            bottom: false,
            child: Stack(
              children: [
                // 页面内容
                Positioned.fill(
                  child: IndexedStack(index: _tab, children: pages),
                ),

                // 悬浮水滴玻璃底栏
                Positioned(
                  left: 20,
                  right: 20,
                  bottom: 20,
                  child: LiquidDropGlass(
                    height: 68,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: List.generate(_tabs.length, (i) {
                        final item = _tabs[i];
                        final active = _tab == i;
                        return GestureDetector(
                          onTap: () => _onTabTap(i),
                          behavior: HitTestBehavior.opaque,
                          child: AnimatedContainer(
                            duration: const Duration(milliseconds: 240),
                            curve: Curves.easeOutCubic,
                            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 9),
                            decoration: BoxDecoration(
                              color: active ? Colors.white : Colors.transparent,
                              borderRadius: BorderRadius.circular(22),
                              boxShadow: active
                                  ? [
                                      BoxShadow(
                                        color: Colors.black.withValues(alpha: 0.07),
                                        blurRadius: 12,
                                        offset: const Offset(0, 3),
                                      ),
                                    ]
                                  : null,
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(item.emoji, style: const TextStyle(fontSize: 18)),
                                if (active) ...[
                                  const SizedBox(width: 7),
                                  Text(
                                    item.label,
                                    style: const TextStyle(
                                      fontSize: 12.5,
                                      fontWeight: FontWeight.w900,
                                      color: CozyTheme.sweetCocoa,
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                        );
                      }),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
