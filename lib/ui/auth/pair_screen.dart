import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../theme/cozy_glass.dart';

/// Pairing surface, ported 1:1 from the native `PairManagementDialog` at
/// `app/src/main/java/com/myorderapp/ui/profile/ProfileScreen.kt:839-991`.
///
/// **Deviation (structural, unavoidable):** the native app has no pairing
/// route — the dialog is raised from the profile screen. The Flutter shell
/// still gates on pairing (`lib/main.dart` renders this widget while
/// `!AppState.instance.isPaired`), so the native dialog body is presented as a
/// standalone page. Layout, spacing, colours, radii and every string come from
/// the Kotlin source; only the page chrome (scroll + SafeArea + 退出登录) is added.
///
/// **Second deviation:** native performs a two-phase preview
/// (`onPreviewPairInvite` -> `onConfirmPairInvite`). `AppState` exposes only a
/// single-shot `joinPair(code)`, so the preview card cannot be shown and the
/// confirm button reads `绑定伴侣` instead of the preview's `confirmText`.
class PairScreen extends StatefulWidget {
  const PairScreen({super.key});

  @override
  State<PairScreen> createState() => _PairScreenState();
}

class _PairScreenState extends State<PairScreen> {
  final TextEditingController _joinCode = TextEditingController();
  String _pairCode = '';

  @override
  void initState() {
    super.initState();
    _pairCode = AppState.instance.pair?.inviteCode ?? '';
    _joinCode.addListener(_onCodeChanged);
  }

  @override
  void dispose() {
    _joinCode.removeListener(_onCodeChanged);
    _joinCode.dispose();
    super.dispose();
  }

  void _onCodeChanged() => setState(() {});

  Future<void> _generate() async {
    final AppState state = AppState.instance;
    final bool ok = await state.createPair();
    if (!mounted) return;
    if (ok && state.pair != null) {
      setState(() => _pairCode = state.pair!.inviteCode);
    } else if (state.error != null) {
      _snack(state.error!);
    }
  }

  Future<void> _confirmPair() async {
    final AppState state = AppState.instance;
    final bool ok = await state.joinPair(_joinCode.text.trim());
    if (!mounted) return;
    if (!ok && state.error != null) _snack(state.error!);
  }

  /// `copyPairCode(context, code)` — clipboard + `Toast("已复制邀请码")`.
  Future<void> _copy() async {
    await Clipboard.setData(ClipboardData(text: _pairCode));
    if (!mounted) return;
    _snack('已复制邀请码');
  }

  Future<void> _unpair() async {
    // Native raises `onUnpair()`; AppState has no unpair capability yet.
    _snack('解绑能力待接入云端接口');
  }

  void _snack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final AppState state = AppState.instance;

    return Scaffold(
      backgroundColor: CozyPalette.background,
      body: SafeArea(
        child: ListenableBuilder(
          listenable: state,
          builder: (BuildContext context, Widget? _) {
            final bool isPaired = state.isPaired;
            // `CouplePair` carries no partner display name yet, so the native
            // `partnerName.ifBlank { "对方" }` fallback is always taken. Wiring
            // the real name needs a new AppState/data-layer field.
            const String partnerName = '';
            final bool hasCode = _pairCode.isNotEmpty;

            return SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  // ---- dialog title -------------------------------------
                  Text(
                    isPaired ? '伴侣已绑定' : '邀请对方',
                    textAlign: TextAlign.center,
                    style: CozyType.textTheme.titleLarge!.copyWith(
                      color: CozyPalette.onSurface,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const SizedBox(height: 18),

                  // ---- description --------------------------------------
                  Text(
                    isPaired
                        ? '已和 ${partnerName.isNotEmpty ? partnerName : '对方'} 绑定。你们正在共享情侣资料、店铺、菜单和订单。'
                        : '请先在首页选择身份。饲养员邀请对方去点餐；吃货邀请对方去做饭，确认后才会绑定。',
                    textAlign: TextAlign.center,
                    style: CozyType.textTheme.bodyMedium!.copyWith(
                      color: CozyPalette.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 14),

                  // ---- invite code surface ------------------------------
                  if (!isPaired) ...<Widget>[
                    Surface(
                      radius: 22,
                      color: CozyPalette.primaryContainer.withValues(
                        alpha: 0.72,
                      ),
                      borderColor: CozyPalette.primary.withValues(alpha: 0.22),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 18,
                        vertical: 18,
                      ),
                      child: Column(
                        children: <Widget>[
                          Text(
                            hasCode ? '把这个邀请码发给对方' : '我的邀请码',
                            textAlign: TextAlign.center,
                            style: CozyType.textTheme.labelLarge!.copyWith(
                              color: CozyPalette.onSurface,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            hasCode ? _pairCode : '点击生成',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: CozyPalette.primary,
                              fontSize: hasCode ? 38 : 26,
                              height: (hasCode ? 44 : 32) / (hasCode ? 38 : 26),
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          if (hasCode) ...<Widget>[
                            const SizedBox(height: 12),
                            Text(
                              '等待对方输入后才会完成绑定',
                              textAlign: TextAlign.center,
                              style: CozyType.textTheme.bodySmall!.copyWith(
                                color: CozyPalette.onSurfaceVariant,
                              ),
                            ),
                          ],
                          const SizedBox(height: 12),
                          SizedBox(
                            height: 44,
                            child: FilledButton(
                              onPressed: state.busy ? null : _generate,
                              style: FilledButton.styleFrom(
                                backgroundColor: CozyPalette.primary,
                                shape: const StadiumBorder(),
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 26,
                                ),
                              ),
                              child: Text(
                                hasCode ? '重新生成' : '生成邀请码',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w900,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 14),
                    Center(
                      child: TextButton(
                        onPressed: hasCode ? _copy : null,
                        child: Text(
                          '复制邀请码',
                          style: TextStyle(
                            color: CozyPalette.onSurface,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                  ],

                  // ---- join field ---------------------------------------
                  SizedBox(
                    height: 60,
                    child: TextField(
                      controller: _joinCode,
                      maxLines: 1,
                      textCapitalization: TextCapitalization.characters,
                      inputFormatters: <TextInputFormatter>[
                        LengthLimitingTextInputFormatter(6),
                      ],
                      style: const TextStyle(fontSize: 15),
                      decoration: cozyInputDecoration(
                        labelText: '输入对方邀请码',
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),

                  if (state.error != null)
                    Text(
                      state.error!,
                      textAlign: TextAlign.center,
                      style: CozyType.textTheme.bodySmall!.copyWith(
                        color: CozyPalette.primary,
                      ),
                    ),
                  if (state.toast != null)
                    Text(
                      state.toast!,
                      textAlign: TextAlign.center,
                      style: CozyType.textTheme.bodySmall!.copyWith(
                        color: CozyPalette.primary,
                      ),
                    ),

                  const SizedBox(height: 18),

                  // ---- confirm ------------------------------------------
                  if (isPaired)
                    Center(
                      child: TextButton(
                        onPressed: _unpair,
                        child: const Text(
                          '解除绑定',
                          style: TextStyle(
                            color: CozyPalette.error,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                    )
                  else
                    SizedBox(
                      height: 48,
                      child: FilledButton(
                        onPressed: _joinCode.text.length == 6 && !state.busy
                            ? _confirmPair
                            : null,
                        style: FilledButton.styleFrom(
                          backgroundColor: CozyPalette.primary,
                          shape: const StadiumBorder(),
                          disabledBackgroundColor: CozyPalette.primary
                              .withValues(alpha: 0.45),
                        ),
                        child: Text(
                          '绑定伴侣',
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                    ),

                  const SizedBox(height: 22),

                  // ---- page chrome (not in the native dialog) -----------
                  Center(
                    child: TextButton(
                      onPressed: () => AppState.instance.logout(),
                      child: Text(
                        '退出登录',
                        style: CozyType.textTheme.bodySmall!.copyWith(
                          color: CozyPalette.onSurfaceVariant,
                        ),
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
}

/// Native counterpart: `Surface(shape = RoundedCornerShape(radius.dp),
/// color = color, border = BorderStroke(1.dp, borderColor))` wrapped around a
/// padded `Column`.
class Surface extends StatelessWidget {
  const Surface({
    super.key,
    required this.radius,
    required this.color,
    required this.borderColor,
    required this.padding,
    required this.child,
  });

  final double radius;
  final Color color;
  final Color borderColor;
  final EdgeInsetsGeometry padding;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: borderColor),
      ),
      padding: padding,
      child: child,
    );
  }
}
