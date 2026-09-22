import 'package:flutter/material.dart';

import '../../data/api_client.dart';
import '../../data/app_state.dart';
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
    // 演示账号预填，方便一键联调
    _username.text = 'eat2';
    _password.text = '123';
  }

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    _nickname.dispose();
    super.dispose();
  }

  void _fillDemo({required String u, required String nick, required String role}) {
    setState(() {
      _isRegister = false;
      _username.text = u;
      _password.text = '123';
      _nickname.text = nick;
      _role = role;
    });
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

                  _field(controller: _username, label: '账号', hint: '请输入账号'),
                  const SizedBox(height: 14),
                  _field(controller: _password, label: '密码', hint: '请输入密码', obscure: true),
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
                    '演示账号（前后端已联调通过）',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 12, color: CozyTheme.mutedText),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: _demoButton('吃货 小马', () => _fillDemo(u: 'eat2', nick: '小马', role: 'eater')),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _demoButton('饲养员 小金毛', () => _fillDemo(u: 'cook2', nick: '小金毛', role: 'caretaker')),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  // 服务器地址：真机 WiFi 网段可能与开发机不同，可点此修改
                  GestureDetector(
                    onTap: _openServerSettings,
                    behavior: HitTestBehavior.opaque,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.dns_outlined, size: 13, color: CozyTheme.mutedText),
                          const SizedBox(width: 6),
                          Text(
                            '服务器 ${ApiClient.instance.baseUrl}',
                            style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText),
                          ),
                          const SizedBox(width: 4),
                          const Text('修改', style: TextStyle(fontSize: 11, color: CozyTheme.primaryPink)),
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

  /// 服务器地址设置：方便真机在不同 WiFi 下切换后端
  Future<void> _openServerSettings() async {
    final api = ApiClient.instance;
    final hostCtrl = TextEditingController(text: api.host);
    final portCtrl = TextEditingController(text: api.port.toString());

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        title: const Text('后端服务器地址', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '填运行 orderdisk-server 的那台电脑的局域网 IP，手机需与它连同一个 WiFi。',
              style: TextStyle(fontSize: 11.5, color: CozyTheme.mutedText),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: hostCtrl,
              decoration: const InputDecoration(labelText: '主机 IP', hintText: '192.168.1.6'),
            ),
            const SizedBox(height: 6),
            TextField(
              controller: portCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: '端口', hintText: '8085'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('取消')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('保存并测试')),
        ],
      ),
    );

    if (confirmed != true) return;
    final port = int.tryParse(portCtrl.text.trim()) ?? 8085;
    final ok = await AppState.instance.configureServer(host: hostCtrl.text.trim(), port: port);
    if (!mounted) return;
    setState(() {});
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? '服务器已保存，连接正常 ✅' : '地址已保存，但暂时连不上，请确认后端已启动')),
    );
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

  Widget _demoButton(String label, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 11),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: CozyTheme.cardStroke),
        ),
        child: Text(
          label,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: CozyTheme.sweetCocoa),
        ),
      ),
    );
  }
}
