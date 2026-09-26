import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../data/app_state.dart';
import '../theme/cozy_glass.dart';
import 'auth_visuals.dart';

/// Faithful Flutter port of the two native auth surfaces:
///
///   * login    -> `app/src/main/java/com/myorderapp/ui/auth/AuthScreen.kt` (183 lines)
///   * register -> `app/src/main/java/com/myorderapp/ui/onboarding/OnboardingScreen.kt` (348 lines),
///                 a two-step flow: account credentials, then profile.
///
/// The only structural deviation from the Kotlin sources is routing: native has
/// two separate destinations (`Routes.AUTH` and `Routes.ONBOARDING`) while the
/// Flutter shell renders both from this one widget, toggled by the
/// `还没有账号？ 去注册` / `已有账号？ 去登录` bottom links.
///
/// Also intentionally dropped (native has neither):
///   * the old `登录 / 注册` mode chips,
///   * the role picker. Native writes `"selected_role" to ""` at registration
///     (`AuthViewModel.kt:428`); the role is chosen later on the home page.
class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final TextEditingController _email = TextEditingController();
  final TextEditingController _password = TextEditingController();
  final TextEditingController _confirmPassword = TextEditingController();
  final TextEditingController _nickname = TextEditingController();

  /// `false` renders `AuthScreen.kt`, `true` renders `OnboardingScreen.kt`.
  bool _register = false;

  /// Register flow step: 1 = account credentials, 2 = profile.
  int _step = 1;

  /// 注册第二步选的头像（`data:image/jpeg;base64,…`）。
  ///
  /// 注册时 profile 行还不存在，所以先在本地攒着，`register()` 里随昵称一起
  /// 写进 `profiles.avatar_url`。存 data URI 而不是 storage URL 的原因见
  /// `SupabaseApi.updateProfile` 注释（这个项目一个 storage 桶都没有）。
  String _avatarDataUri = '';
  bool _pickingAvatar = false;

  /// `AuthScreen.kt` `rememberCredentials`.
  bool _remember = true;

  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    _nickname.dispose();
    super.dispose();
  }

  // ---------------------------------------------------------------- actions

  /// `AuthViewModel.kt` login path.
  Future<void> _submitLogin() async {
    final AppState state = AppState.instance;
    if (_email.text.trim().isEmpty || _password.text.isEmpty) {
      setState(() => _error = '请输入邮箱和密码');
      return;
    }
    setState(() => _error = null);
    final bool ok = await state.login(
      username: _email.text.trim(),
      password: _password.text,
    );
    if (!ok && mounted) {
      setState(
        () => _error = state.error ?? '账号或密码不正确，请检查后重试。',
      );
    }
  }

  /// `OnboardingViewModel.kt:73-81` — validates step 1 and advances to step 2.
  void _goToStep2() {
    if (_email.text.trim().isEmpty || _password.text.isEmpty) {
      setState(() => _error = '请输入邮箱和密码');
      return;
    }
    if (_password.text.length < 6) {
      setState(() => _error = '密码至少6位');
      return;
    }
    if (_password.text != _confirmPassword.text) {
      setState(() => _error = '两次密码输入不一致');
      return;
    }
    setState(() {
      _error = null;
      _step = 2;
    });
  }

  /// `OnboardingViewModel.kt:105` + `completeRegistration`.
  ///
  /// Native persists `selected_role = ""`; the home page role switcher is what
  /// actually sets it later, so an empty role is passed on purpose.
  Future<void> _completeRegistration() async {
    final AppState state = AppState.instance;
    if (_nickname.text.trim().isEmpty) {
      setState(() => _error = '请输入昵称');
      return;
    }
    setState(() => _error = null);
    final bool ok = await state.register(
      username: _email.text.trim(),
      password: _password.text,
      nickname: _nickname.text.trim(),
      role: '',
      avatarUrl: _avatarDataUri,
    );
    if (!ok && mounted) {
      setState(() => _error = state.error ?? '请求失败，请检查网络或稍后重试');
    }
  }

  /// 选头像：压到 256×256 / q72（≈9 KB），转成 data URI 存本地待提交。
  Future<void> _pickAvatar() async {
    if (_pickingAvatar) return;
    setState(() => _pickingAvatar = true);
    try {
      final XFile? picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        maxWidth: 256,
        maxHeight: 256,
        imageQuality: 72,
      );
      if (picked == null) return; // 用户取消
      final Uint8List bytes = await picked.readAsBytes();
      if (bytes.isEmpty) {
        if (mounted) setState(() => _error = '这张图片读不出来，换一张试试');
        return;
      }
      if (mounted) {
        setState(() {
          _avatarDataUri = 'data:image/jpeg;base64,${base64Encode(bytes)}';
          _error = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = '打开相册失败：$e');
    } finally {
      if (mounted) setState(() => _pickingAvatar = false);
    }
  }

  // ------------------------------------------------------------------ build

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: AppState.instance,
      builder: (BuildContext context, Widget? _) {
        return Scaffold(
          backgroundColor: Colors.white,
          resizeToAvoidBottomInset: true,
          body: _register ? _buildRegister() : _buildLogin(),
        );
      },
    );
  }

  /// `AuthScreen.kt` — vertically centred login card over the decorated white
  /// background.
  Widget _buildLogin() {
    final bool busy = AppState.instance.busy;

    return AuthDecoratedBackground(
      child: _centeredScroll(
        children: <Widget>[
          const Center(child: AuthLogo()),
          const SizedBox(height: 20),
          _displayTitle('欢迎回来', AuthColors.ink),
          const SizedBox(height: 6),
          _mutedLine('今天也一起好好吃饭吧'),
          const SizedBox(height: 28),
          AuthGlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                AuthInputField(
                  controller: _email,
                  label: '账号 / 邮箱',
                  placeholder: '账号 / 邮箱',
                  floatingLabel: false,
                  keyboardType: TextInputType.emailAddress,
                  textInputAction: TextInputAction.next,
                ),
                const SizedBox(height: 8),
                _mutedSmall('可在多台设备登录，同步你们的小店和订单。'),
                const SizedBox(height: 14),
                AuthInputField(
                  controller: _password,
                  label: '密码',
                  placeholder: '密码',
                  isPassword: true,
                  floatingLabel: false,
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => _submitLogin(),
                ),
                const SizedBox(height: 18),
                _rememberRow(),
                if (_error != null) ...<Widget>[
                  const SizedBox(height: 10),
                  _errorLine(_error!),
                ],
                const SizedBox(height: 18),
                AuthPrimaryButton(
                  text: busy ? '登录中...' : '登录',
                  enabled: !busy,
                  onTap: _submitLogin,
                ),
              ],
            ),
          ),
          AuthBottomLink(
            prefix: '还没有账号？',
            actionText: '去注册',
            top: 10,
            onTap: () => setState(() {
              _register = true;
              _step = 1;
              _error = null;
            }),
          ),
        ],
      ),
    );
  }

  /// `OnboardingScreen.kt` — `Color(0xFFFFFCF8)` background, a faint artwork
  /// watermark top-right, and the two-step registration column.
  Widget _buildRegister() {
    return Stack(
      children: <Widget>[
        const Positioned.fill(
          child: ColoredBox(color: CozyPalette.surface), // #FFFCF8
        ),
        if (_step == 1)
          Positioned(
            top: MediaQuery.of(context).padding.top + 12,
            right: 20,
            child: IgnorePointer(
              child: Opacity(
                opacity: 0.10,
                child: Image.asset(
                  'assets/images/auth_dogs_artwork.png',
                  width: 138,
                  height: 138,
                ),
              ),
            ),
          )
        else
          Positioned(
            top: MediaQuery.of(context).padding.top + 178,
            right: 24,
            child: IgnorePointer(
              child: Icon(
                Icons.favorite_border,
                size: 82,
                color: AuthColors.primaryEnd.withValues(alpha: 0.18),
              ),
            ),
          ),
        Positioned.fill(
          child: _step == 1 ? _buildRegisterStep1() : _buildRegisterStep2(),
        ),
      ],
    );
  }

  /// `RegisterAccountScreen` — 账号 / 邮箱, 密码, 确认密码, 下一步.
  Widget _buildRegisterStep1() {
    final bool busy = AppState.instance.busy;

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 34),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            const SizedBox(height: 92),
            _displayTitle('创建你们的小饭桌', AuthColors.ink),
            const SizedBox(height: 6),
            _mutedLine('一起记录每一次想吃什么'),
            const SizedBox(height: 28),
            AuthGlassCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  AuthInputField(
                    controller: _email,
                    label: '账号 / 邮箱',
                    placeholder: '账号 / 邮箱',
                    floatingLabel: false,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),
                  AuthInputField(
                    controller: _password,
                    label: '密码',
                    placeholder: '密码',
                    isPassword: true,
                    floatingLabel: false,
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 12),
                  AuthInputField(
                    controller: _confirmPassword,
                    label: '确认密码',
                    placeholder: '确认密码',
                    isPassword: true,
                    floatingLabel: false,
                    textInputAction: TextInputAction.done,
                    onSubmitted: (_) => _goToStep2(),
                  ),
                  if (_error != null) ...<Widget>[
                    const SizedBox(height: 10),
                    _errorLine(_error!),
                  ],
                  const SizedBox(height: 18),
                  AuthPrimaryButton(
                    text: busy ? '请稍候...' : '下一步',
                    enabled: !busy,
                    onTap: _goToStep2,
                  ),
                ],
              ),
            ),
            AuthBottomLink(
              prefix: '已有账号？',
              actionText: '去登录',
              top: 10,
              onTap: () => setState(() {
                _register = false;
                _error = null;
              }),
            ),
          ],
        ),
      ),
    );
  }

  /// `Step2Profile` — avatar, 昵称, 开启甜蜜点菜之旅.
  Widget _buildRegisterStep2() {
    final bool busy = AppState.instance.busy;

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 34),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            const SizedBox(height: 114),
            _displayTitle('完善个人资料', AuthColors.primaryEnd),
            const SizedBox(height: 6),
            _mutedLine('让对方一眼认出你'),
            const SizedBox(height: 28),
            Column(
              children: <Widget>[
                GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: _pickAvatar,
                  child: Container(
                    width: 128,
                    height: 128,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: AuthColors.primaryEnd.withValues(alpha: 0.06),
                    ),
                    child: ClipOval(
                      child: CozyAvatar(
                        url: _avatarDataUri,
                        size: 128,
                        fallback: const DashedAvatarPlaceholder(size: 128),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 26),
                GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: _pickAvatar,
                  child: Text(
                    _avatarDataUri.isEmpty ? '选择头像照片' : '换一张头像',
                    style: CozyType.textTheme.titleSmall!.copyWith(
                      color: AuthColors.primaryEnd,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ),
                const SizedBox(height: 26),
                AuthInputField(
                  controller: _nickname,
                  label: '昵称',
                  placeholder: '昵称',
                  floatingLabel: false,
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => _completeRegistration(),
                ),
                if (_error != null) ...<Widget>[
                  const SizedBox(height: 10),
                  _errorLine(_error!),
                ],
                const SizedBox(height: 26),
                AuthPrimaryButton(
                  text: busy ? '注册中...' : '开启甜蜜点菜之旅',
                  enabled: !busy,
                  onTap: _completeRegistration,
                ),
              ],
            ),
            AuthBottomLink(
              actionText: '返回上一步',
              top: 10,
              onTap: () => setState(() {
                _step = 1;
                _error = null;
              }),
            ),
          ],
        ),
      ),
    );
  }

  // ------------------------------------------------------------- fragments

  /// `Column(verticalArrangement = Arrangement.Center)` inside a scrolling box.
  Widget _centeredScroll({required List<Widget> children}) {
    return SafeArea(
      child: LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
          return SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 30),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minHeight: (constraints.maxHeight - 60).clamp(0, double.infinity),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: children,
              ),
            ),
          );
        },
      ),
    );
  }

  /// `MaterialTheme.typography.displayLarge` + `ExtraBold`.
  Widget _displayTitle(String text, Color color) {
    return Text(
      text,
      textAlign: TextAlign.center,
      style: CozyType.textTheme.displayLarge!.copyWith(
        color: color,
        fontWeight: FontWeight.w800,
      ),
    );
  }

  /// `bodyMedium` in `AuthMuted`.
  Widget _mutedLine(String text) {
    return Text(
      text,
      textAlign: TextAlign.center,
      style: CozyType.textTheme.bodyMedium!.copyWith(color: AuthColors.muted),
    );
  }

  /// `bodySmall` in `AuthMuted`.
  Widget _mutedSmall(String text) {
    return Text(
      text,
      style: CozyType.textTheme.bodySmall!.copyWith(color: AuthColors.muted),
    );
  }

  /// Error text — `bodySmall`, `MaterialTheme.colorScheme.error`, centred.
  Widget _errorLine(String text) {
    return Text(
      text,
      textAlign: TextAlign.center,
      style: CozyType.textTheme.bodySmall!.copyWith(color: CozyPalette.error),
    );
  }

  /// `AuthScreen.kt` `记住账号密码` checkbox row.
  Widget _rememberRow() {
    return Row(
      children: <Widget>[
        Expanded(
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: () => setState(() => _remember = !_remember),
            child: Row(
              children: <Widget>[
                SizedBox(
                  width: 24,
                  height: 24,
                  child: Checkbox(
                    value: _remember,
                    onChanged: (bool? v) =>
                        setState(() => _remember = v ?? false),
                    activeColor: AuthColors.primaryEnd,
                    checkColor: const Color(0xFFFFFBF5),
                    side: BorderSide(
                      color: AuthColors.muted.withValues(alpha: 0.70),
                    ),
                    materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                ),
                const SizedBox(width: 4),
                Text(
                  '记住账号密码',
                  style: CozyType.textTheme.bodySmall!.copyWith(
                    color: AuthColors.ink,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
