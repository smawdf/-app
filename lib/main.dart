import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import 'data/app_state.dart';
import 'ui/auth/auth_screen.dart';
import 'ui/auth/pair_screen.dart';
import 'ui/shell/main_shell.dart';
import 'ui/theme/cozy_glass.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // 强制构建语义树：让 uiautomator / 无障碍服务能识别控件，
  // 同时也是 UI 自动化测试能够定位元素的前提。
  SemanticsBinding.instance.ensureSemantics();
  // 预热液态玻璃着色器（纯内存 I/O，不阻塞首帧）
  // enablePerformanceMonitor 默认为 true，会在界面上绘制调试用的栅格监视层，正式包必须关掉
  await LiquidGlassWidgets.initialize(enablePerformanceMonitor: false);
  runApp(const OrderDiskApp());
}

class OrderDiskApp extends StatelessWidget {
  const OrderDiskApp({super.key});

  @override
  Widget build(BuildContext context) {
    return LiquidGlassWidgets.wrap(
      // 本 App 固定浅色（纯白底），直接给出亮度避免 MaterialApp 下解析不到 Theme
      brightnessResolver: (BuildContext context) => Brightness.light,
      child: MaterialApp(
        title: '高糖小食',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          scaffoldBackgroundColor: CozyTheme.pureWhite, // 纯白极简底色
          useMaterial3: true,
          splashFactory: NoSplash.splashFactory,
        ),
        home: const _RootRouter(),
      ),
    );
  }
}

/// 启动引导 → 按登录 / 配对状态切换顶层页面
class _RootRouter extends StatefulWidget {
  const _RootRouter();

  @override
  State<_RootRouter> createState() => _RootRouterState();
}

class _RootRouterState extends State<_RootRouter> {
  bool _booting = true;

  @override
  void initState() {
    super.initState();
    _boot();
  }

  Future<void> _boot() async {
    try {
      await AppState.instance.bootstrap();
    } finally {
      if (mounted) setState(() => _booting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    if (_booting) {
      return const Scaffold(
        backgroundColor: CozyTheme.pureWhite,
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('🍲', style: TextStyle(fontSize: 52)),
              SizedBox(height: 18),
              Text(
                '高糖小食',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w900,
                  color: CozyTheme.sweetCocoa,
                ),
              ),
              SizedBox(height: 22),
              SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2, color: CozyTheme.primaryPink),
              ),
            ],
          ),
        ),
      );
    }

    return ListenableBuilder(
      listenable: state,
      builder: (context, _) {
        if (!state.isLoggedIn) return const AuthScreen();
        if (!state.isPaired) return const PairScreen();
        return const MainShell();
      },
    );
  }
}
