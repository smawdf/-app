import 'package:fluid_glass/fluid_glass.dart';
import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';

import 'data/app_state.dart';
import 'ui/auth/auth_screen.dart';
import 'ui/auth/pair_screen.dart';
import 'ui/shell/main_shell.dart';
import 'ui/theme/cozy_glass.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SemanticsBinding.instance.ensureSemantics();
  // 预热并编译 FluidGlass 全部 GPU 片段着色器 (refraction.frag, dispersion 等)
  await FluidGlass.ensureInitialized();
  runApp(const OrderDiskApp());
}

class OrderDiskApp extends StatelessWidget {
  const OrderDiskApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '高糖小食',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        scaffoldBackgroundColor: CozyTheme.pureWhite, // 纯白极简底色
        useMaterial3: true,
        splashFactory: NoSplash.splashFactory,
      ),
      home: const _RootRouter(),
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
