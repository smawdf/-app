import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../theme/cozy_glass.dart';

/// 未绑定伴侣时的配对页：饲养员生成邀请码，吃货输入邀请码
class PairScreen extends StatefulWidget {
  const PairScreen({super.key});

  @override
  State<PairScreen> createState() => _PairScreenState();
}

class _PairScreenState extends State<PairScreen> {
  final _code = TextEditingController();
  String? _generatedCode;

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _create() async {
    final state = AppState.instance;
    final ok = await state.createPair();
    if (!mounted) return;
    if (ok && state.pair != null) {
      setState(() => _generatedCode = state.pair!.inviteCode);
    } else if (state.error != null) {
      _snack(state.error!);
    }
  }

  Future<void> _join() async {
    final state = AppState.instance;
    final code = _code.text.trim();
    if (code.length != 6) {
      _snack('请输入 6 位邀请码');
      return;
    }
    final ok = await state.joinPair(code);
    if (!mounted) return;
    if (!ok && state.error != null) _snack(state.error!);
  }

  void _snack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
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
              padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 40),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text('💕', textAlign: TextAlign.center, style: TextStyle(fontSize: 54)),
                  const SizedBox(height: 16),
                  const Text(
                    '还差一步就开饭啦',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '当前身份：${state.user?.nickname ?? ''} · ${state.user?.roleLabel ?? ''}',
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 13.5, color: CozyTheme.mutedText),
                  ),
                  const SizedBox(height: 36),

                  // ---- 饲养员：生成邀请码 ----
                  if (_generatedCode == null) ...[
                    const Text(
                      '由其中一方生成邀请码，另一方输入即可绑定',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 12.5, color: CozyTheme.mutedText),
                    ),
                    const SizedBox(height: 18),
                    LiquidDropGlass(
                      height: 56,
                      borderRadius: BorderRadius.circular(28),
                      onTap: state.busy ? null : _create,
                      child: const Center(
                        child: Text(
                          '生成我的邀请码',
                          style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
                        ),
                      ),
                    ),
                  ] else ...[
                    LiquidDropGlass(
                      height: 120,
                      borderRadius: BorderRadius.circular(28),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Text('把这串邀请码发给 Ta',
                              style: TextStyle(fontSize: 12.5, color: CozyTheme.mutedText)),
                          const SizedBox(height: 8),
                          Text(
                            _generatedCode!,
                            style: const TextStyle(
                              fontSize: 34,
                              fontWeight: FontWeight.w900,
                              letterSpacing: 8,
                              color: CozyTheme.primaryPink,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextButton.icon(
                      onPressed: () {
                        Clipboard.setData(ClipboardData(text: _generatedCode!));
                        _snack('邀请码已复制');
                      },
                      icon: const Icon(Icons.copy, size: 16),
                      label: const Text('复制邀请码'),
                    ),
                  ],

                  const SizedBox(height: 34),
                  const Row(children: [
                    Expanded(child: Divider(color: CozyTheme.cardStroke)),
                    Padding(
                      padding: EdgeInsets.symmetric(horizontal: 12),
                      child: Text('或者', style: TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
                    ),
                    Expanded(child: Divider(color: CozyTheme.cardStroke)),
                  ]),
                  const SizedBox(height: 24),

                  // ---- 另一方：输入邀请码 ----
                  const Text('输入 Ta 给你的 6 位邀请码',
                      style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: CozyTheme.mutedText)),
                  const SizedBox(height: 8),
                  Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFFF7F7F8),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: CozyTheme.cardStroke),
                    ),
                    child: TextField(
                      controller: _code,
                      textAlign: TextAlign.center,
                      maxLength: 6,
                      textCapitalization: TextCapitalization.characters,
                      style: const TextStyle(
                          fontSize: 22, fontWeight: FontWeight.w900, letterSpacing: 6, color: CozyTheme.sweetCocoa),
                      decoration: const InputDecoration(
                        counterText: '',
                        hintText: 'ABC123',
                        hintStyle: TextStyle(letterSpacing: 6, color: Color(0xFFC9C6CC), fontSize: 20),
                        border: InputBorder.none,
                        contentPadding: EdgeInsets.symmetric(vertical: 16),
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  LiquidDropGlass(
                    height: 56,
                    borderRadius: BorderRadius.circular(28),
                    onTap: state.busy ? null : _join,
                    child: const Center(
                      child: Text(
                        '绑定伴侣，开启小店',
                        style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
                      ),
                    ),
                  ),

                  const SizedBox(height: 30),
                  TextButton(
                    onPressed: () => AppState.instance.logout(),
                    child: const Text('退出登录', style: TextStyle(color: CozyTheme.mutedText, fontSize: 13)),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}
