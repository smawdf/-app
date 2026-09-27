import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import 'data/app_state.dart';
import 'data/net_resilience.dart';
import 'data/supabase_api.dart';
import 'ui/auth/auth_screen.dart';
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
          // 【自查修正】原来一个 colorScheme 都没给，Material 默认件（对话框按钮、
          // 开关、输入光标、日期选择器…）全落在 M3 基线紫上。页面里的粉/玫瑰色都
          // 来自 CozyPalette，这里只是把「默认值」也换成同一套品牌色板。
          colorScheme: const ColorScheme.light().copyWith(
            primary: CozyPalette.primary,
            onPrimary: CozyPalette.surface,
            primaryContainer: CozyPalette.primaryContainer,
            onPrimaryContainer: CozyPalette.onPrimaryContainer,
            secondary: CozyPalette.secondary,
            secondaryContainer: CozyPalette.secondaryContainer,
            tertiary: CozyPalette.tertiary,
            tertiaryContainer: CozyPalette.tertiaryContainer,
            error: CozyPalette.error,
            errorContainer: CozyPalette.errorContainer,
            surface: CozyPalette.surface,
            onSurface: CozyPalette.onSurface,
            onSurfaceVariant: CozyPalette.onSurfaceVariant,
            outline: CozyPalette.outline,
            outlineVariant: CozyPalette.outlineVariant,
          ),
          // 「浪漫雅圆」+ 与原生 Type.kt 逐条对齐的字号/行高
          fontFamily: CozyType.family,
          textTheme: CozyType.textTheme,
          // 【自查修正】对话框统一成品牌规格：原来只有「编辑个人资料」是自绘的，
          // 其余 AlertDialog 走 Material 默认的 40px 内边距 + 紫色调阴影。
          dialogTheme: DialogThemeData(
            backgroundColor: CozyPalette.surface,
            surfaceTintColor: Colors.transparent,
            insetPadding:
                const EdgeInsets.symmetric(horizontal: 36, vertical: 24),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
            titleTextStyle: CozyType.textTheme.titleLarge!.copyWith(
              fontWeight: FontWeight.w900,
              color: CozyPalette.onSurface,
            ),
            contentTextStyle: CozyType.textTheme.bodyMedium!.copyWith(
              color: CozyPalette.onSurfaceVariant,
            ),
          ),
          // 【自查修正】漏网的按钮也有品牌色兜底，不会再出现 Material 紫。
          textButtonTheme: TextButtonThemeData(
            style: TextButton.styleFrom(foregroundColor: CozyPalette.primary),
          ),
          filledButtonTheme: FilledButtonThemeData(
            style: FilledButton.styleFrom(
              backgroundColor: CozyPalette.primary,
              foregroundColor: CozyPalette.surface,
            ),
          ),
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
        // 与原生的路由结构一致：`NavGraph.kt` 的 startDestination 是 HOME，
        // 没有独立的配对路由 —— 未配对用户照样进主壳，配对由「我的」页里的
        // 伴侣绑定卡片（原生 `ProfileScreen.kt:839-991` 的 PairManagementDialog）
        // 完成。之前在这里加 `!isPaired -> PairScreen` 硬闸是我引入的死锁：
        // 那一页既没有身份选择器，底栏外壳又必须配对后才可达。
        return const MainShell();
      },
    );
  }
}
