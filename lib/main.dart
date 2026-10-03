import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import 'data/app_state.dart';
import 'data/net_resilience.dart';
import 'data/supabase_api.dart';
import 'ui/auth/auth_screen.dart';
import 'ui/shell/main_shell.dart';
import 'ui/theme/cozy_glass.dart';
import 'ui/theme/couple_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // 强制构建语义树：让 uiautomator / 无障碍服务能识别控件，
  // 同时也是 UI 自动化测试能够定位元素的前提。
  SemanticsBinding.instance.ensureSemantics();
  // 预热液态玻璃着色器（纯内存 I/O，不阻塞首帧）
  // enablePerformanceMonitor 默认为 true，会在界面上绘制调试用的栅格监视层，正式包必须关掉
  await LiquidGlassWidgets.initialize(enablePerformanceMonitor: false);
  // 初始化情侣专属主题引擎
  await CoupleThemeManager.instance.init();
  // 本机到 Supabase 的 TLS 握手有约 50% 概率被瞬时中断，
  // 装上带退避重试的 connectionFactory（必须在任何 HttpClient 创建前）。
  installNetworkResilience();
  // 连接在线 Supabase 云端数据库
  await SupabaseApi.initialize();
  runApp(const OrderDiskApp());
}

class OrderDiskApp extends StatelessWidget {
  const OrderDiskApp({super.key});

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: CoupleThemeManager.instance,
      builder: (context, _) {
        final coupleSpec = CoupleThemeManager.instance.currentSpec;
        return CoupleThemeProvider(
          manager: CoupleThemeManager.instance,
          child: LiquidGlassWidgets.wrap(
            // 本 App 固定浅色（纯白底），直接给出亮度避免 MaterialApp 下解析不到 Theme
            brightnessResolver: (BuildContext context) => Brightness.light,
            child: MaterialApp(
            title: '高糖小食',
            debugShowCheckedModeBanner: false,
            theme: ThemeData(
              scaffoldBackgroundColor: coupleSpec.bgPage,
              useMaterial3: true,
              splashFactory: NoSplash.splashFactory,
              colorScheme: const ColorScheme.light().copyWith(
                primary: coupleSpec.primary,
                onPrimary: Colors.white,
                primaryContainer: coupleSpec.primaryLight,
                onPrimaryContainer: coupleSpec.primaryDark,
                secondary: coupleSpec.accent,
                secondaryContainer: coupleSpec.accentLight,
                tertiary: coupleSpec.primaryDark,
                tertiaryContainer: coupleSpec.primaryLight,
                error: CozyPalette.error,
                errorContainer: CozyPalette.errorContainer,
                surface: coupleSpec.cardBg,
                onSurface: coupleSpec.textMain,
                onSurfaceVariant: coupleSpec.textSub,
                outline: coupleSpec.cardBorder,
                outlineVariant: coupleSpec.cardBorder,
              ),
              fontFamily: CozyType.family,
              textTheme: CozyType.textTheme,
              dialogTheme: DialogThemeData(
                backgroundColor: coupleSpec.cardBg,
                surfaceTintColor: Colors.transparent,
                insetPadding:
                    const EdgeInsets.symmetric(horizontal: 36, vertical: 24),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                titleTextStyle: CozyType.textTheme.titleLarge!.copyWith(
                  fontWeight: FontWeight.w900,
                  color: coupleSpec.textMain,
                ),
                contentTextStyle: CozyType.textTheme.bodyMedium!.copyWith(
                  color: coupleSpec.textSub,
                ),
              ),
              textButtonTheme: TextButtonThemeData(
                style: TextButton.styleFrom(foregroundColor: coupleSpec.primary),
              ),
              filledButtonTheme: FilledButtonThemeData(
                style: FilledButton.styleFrom(
                  backgroundColor: coupleSpec.primary,
                  foregroundColor: Colors.white,
                ),
              ),
              pageTransitionsTheme: const PageTransitionsTheme(
                builders: <TargetPlatform, PageTransitionsBuilder>{
                  TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
                },
              ),
            ),
            home: const _RootRouter(),
          ),
        ),
      );
    },
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
    final coupleSpec = CoupleThemeManager.instance.currentSpec;

    if (_booting) {
      return Scaffold(
        backgroundColor: coupleSpec.bgPage,
        body: Center(
          child: TweenAnimationBuilder<double>(
            tween: Tween<double>(begin: 0, end: 1),
            duration: const Duration(milliseconds: 600),
            curve: Curves.easeOutCubic,
            builder: (BuildContext context, double t, Widget? child) {
              return Opacity(
                opacity: t,
                child: Transform.translate(
                  offset: Offset(0, (1 - t) * 16),
                  child: child,
                ),
              );
            },
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  width: 136,
                  height: 136,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(28),
                    border: Border.all(color: coupleSpec.cardBorder, width: 2),
                    boxShadow: [
                      BoxShadow(
                        color: coupleSpec.shadowColor.withValues(alpha: 0.18),
                        blurRadius: 20,
                        offset: const Offset(0, 8),
                      ),
                    ],
                  ),
                  padding: const EdgeInsets.all(10),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(20),
                    child: Image.asset(
                      coupleSpec.duoAnimationAsset,
                      fit: BoxFit.contain,
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                Text(
                  '高糖小食 · ${coupleSpec.title}',
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w900,
                    color: coupleSpec.textMain,
                    letterSpacing: -0.5,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  coupleSpec.duoAnimationSubtitle,
                  style: TextStyle(
                    fontSize: 12,
                    color: coupleSpec.textSub,
                  ),
                ),
                const SizedBox(height: 24),
                SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.2,
                    color: coupleSpec.primary,
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return ListenableBuilder(
      listenable: Listenable.merge([
        state.listenFor(const {Domain.auth}),
        CoupleThemeManager.instance,
      ]),
      builder: (context, _) {
        if (!state.isLoggedIn) return const AuthScreen();
        return const MainShell();
      },
    );
  }
}
