import 'package:flutter/material.dart';

import '../../data/app_state.dart';
import '../../data/supabase_api.dart';
import '../theme/cozy_glass.dart';

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final _username = TextEditingController();
  final _password = TextEditingController();
  final _nickname = TextEditingController();

  bool _isRegister = false;
  String _role = 'eater'; // eater | caretaker

  @override
  void initState() {
    super.initState();
    // 云端 Supabase 使用邮箱登录，这里留空由用户输入
  }

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    _nickname.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final state = AppState.instance;
    final ok = _isRegister
        ? await state.register(
            username: _username.text.trim(),
            password: _password.text,
            nickname: _nickname.text.trim().isEmpty ? _username.text.trim() : _nickname.text.trim(),
            role: _role,
          )
        : await state.login(username: _username.text.trim(), password: _password.text);

    if (!ok && mounted && state.error != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(state.error!)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return Scaffold(
      backgroundColor: CozyTheme.pureWhite,
      body: SafeArea(
        child: ListenableBuilder(
          listenable: state,
          builder: (context, _) {
            return SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 32),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 20),
                  const Text(
                    '高糖小食',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 32, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    '情侣两个人的过家家小餐厅',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 13.5, color: CozyTheme.mutedText),
                  ),
                  const SizedBox(height: 36),

                  // 切换 登录 / 注册
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      _modeChip('登录', !_isRegister, () => setState(() => _isRegister = false)),
                      const SizedBox(width: 10),
                      _modeChip('注册', _isRegister, () => setState(() => _isRegister = true)),
                    ],
                  ),
                  const SizedBox(height: 24),

                  if (_isRegister) ...[
                    _field(controller: _nickname, label: '昵称', hint: '小金毛 / 小马尔济斯'),
                    const SizedBox(height: 14),
                    _rolePicker(),
                    const SizedBox(height: 14),
                  ],

                  _field(controller: _username, label: '邮箱', hint: '请输入邮箱，例如 xx@example.com'),
                  const SizedBox(height: 14),
                  _field(controller: _password, label: '密码', hint: '请输入密码（至少 6 位）', obscure: true),
                  const SizedBox(height: 26),

                  LiquidDropGlass(
                    height: 56,
                    borderRadius: BorderRadius.circular(28),
                    onTap: state.busy ? null : _submit,
                    child: Center(
                      child: state.busy
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFFFF5A79)),
                            )
                          : Text(
                              _isRegister ? '注册并进入' : '登录',
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w900,
                                color: CozyTheme.sweetCocoa,
                              ),
                            ),
                    ),
                  ),

                  const SizedBox(height: 30),
                  const Text(
                    '首次使用请点上方「注册」创建账号',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 12, color: CozyTheme.mutedText),
                  ),
                  const SizedBox(height: 20),
                  // 云端数据库地址（Supabase），点击可查看
                  GestureDetector(
                    onTap: _openServerSettings,
                    behavior: HitTestBehavior.opaque,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.cloud_done_outlined, size: 13, color: CozyTheme.mutedText),
                          const SizedBox(width: 6),
                          Text(
                            '云端数据库 ${kSupabaseUrl.replaceFirst('https://', '')}',
                            style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText),
                          ),
                          const SizedBox(width: 4),
                          const Text('详情', style: TextStyle(fontSize: 11, color: CozyTheme.primaryPink)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  /// 云端数据库信息（Supabase 地址固定，无需用户配置）
  Future<void> _openServerSettings() async {
    await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        title: const Text('云端数据库', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '本版本数据全部保存在你自己的在线 Supabase 数据库中，无需手动配置地址，也不依赖电脑开着后端服务。',
              style: TextStyle(fontSize: 12, color: CozyTheme.mutedText, height: 1.5),
            ),
            const SizedBox(height: 14),
            Text(
              kSupabaseUrl.replaceFirst('https://', ''),
              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: CozyTheme.sweetCocoa),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () async {
              Navigator.pop(ctx, true);
            },
            child: const Text('测试连接'),
          ),
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('关闭')),
        ],
      ),
    ).then((probe) async {
      if (probe != true || !mounted) return;
      final ok = await AppState.instance.configureServer(host: kSupabaseUrl, port: 0);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? '云端连接正常 ✅' : '云端暂时连不上，请检查手机网络（可能需要科学上网）')),
      );
    });
  }

  Widget _modeChip(String label, bool active, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 220),
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 9),
        decoration: BoxDecoration(
          color: active ? CozyTheme.sweetCocoa : CozyTheme.softPink,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 13.5,
            fontWeight: FontWeight.w800,
            color: active ? Colors.white : CozyTheme.primaryPink,
          ),
        ),
      ),
    );
  }

  Widget _rolePicker() {
    Widget option(String role, String emoji, String label) {
      final active = _role == role;
      return Expanded(
        child: GestureDetector(
          onTap: () => setState(() => _role = role),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 220),
            padding: const EdgeInsets.symmetric(vertical: 12),
            decoration: BoxDecoration(
              color: active ? CozyTheme.softPink : Colors.transparent,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: active ? CozyTheme.primaryPink.withValues(alpha: 0.5) : CozyTheme.cardStroke,
                width: 1.2,
              ),
            ),
            child: Column(
              children: [
                Text(emoji, style: const TextStyle(fontSize: 20)),
                const SizedBox(height: 4),
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w800,
                    color: active ? CozyTheme.primaryPink : CozyTheme.mutedText,
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Row(
      children: [
        option('eater', '🍽️', '吃货（点菜方）'),
        const SizedBox(width: 10),
        option('caretaker', '🍳', '饲养员（做饭方）'),
      ],
    );
  }

  Widget _field({
    required TextEditingController controller,
    required String label,
    required String hint,
    bool obscure = false,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: CozyTheme.mutedText)),
        const SizedBox(height: 7),
        Container(
          decoration: BoxDecoration(
            color: const Color(0xFFF7F7F8),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: CozyTheme.cardStroke),
          ),
          child: TextField(
            controller: controller,
            obscureText: obscure,
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
            decoration: InputDecoration(
              hintText: hint,
              hintStyle: const TextStyle(color: Color(0xFFB5B2B8), fontSize: 14),
              border: InputBorder.none,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
            ),
          ),
        ),
      ],
    );
  }

}
