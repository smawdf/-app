import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../candy/candy_coins_page.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';

/// 原生 `BuildConfig.VERSION_NAME` 的等值常量。
/// pubspec.yaml:17 → `version: 3.0.1+55`
const String _appVersion = '3.0.1';

/// Compose 的 `fontSize.sp + lineHeight.sp` 在 Flutter 里要换算成 `height` 倍率。
/// 全页字号/行高一律走这里，字体族由全局 `ThemeData.textTheme` 提供（RomanticRound）。
TextStyle _ts(double size, double lineHeight, FontWeight weight, Color color) =>
    TextStyle(fontSize: size, height: lineHeight / size, fontWeight: weight, color: color);

/// 个人与小店 —— 原生 `ui/profile/ProfileScreen.kt` 的 1:1 移植。
///
/// 模块顺序（对照原生行号）：
///   · `ImmersiveProfileHeader`       ProfileScreen.kt:321-374
///   · `ProfileHeader`                ProfileScreen.kt:376-492
///   · `SimulatedCurrencyBalanceCard` ProfileScreen.kt:509-559
///   · 5 行 `ProfileActionRow`        ProfileScreen.kt:268-314
///   · `LogoutButton`                 ProfileScreen.kt:813-837
/// 弹窗：`ProfileEditDialog`:1185 / `HelpDialog`:1126 / `VersionInfoDialog`:1006 /
///       `PairManagementDialog`:839 / 解除绑定确认:204 / `LogoutConfirmDialog`:1156
class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key, this.onNavigateTab});

  /// 原生 `ProfileScreen(onOrdersClick = ...)`（ProfileScreen.kt:256）：
  /// 主壳切 Tab 回调，供「订单记录」切到底部「订单」。
  final ValueChanged<int>? onNavigateTab;

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  static const String _versionLabel = _appVersion;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshTransactions();
    });
  }

  void _snack(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _openProfileEditor() async {
    final state = AppState.instance;
    await showDialog<void>(
      context: context,
      builder: (_) => _ProfileEditDialog(
        name: state.user?.nickname ?? '',
        avatarUrl: state.user?.avatarUrl ?? '',
      ),
    );
  }

  Future<void> _openHelpDialog() => showDialog<void>(
        context: context,
        builder: (_) => const _HelpDialog(),
      );

  Future<void> _openVersionDialog() => showDialog<void>(
        context: context,
        builder: (_) => const _VersionInfoDialog(),
      );

  Future<void> _openPairDialog() async {
    AppState.instance.loadMe();
    await showDialog<void>(
      context: context,
      builder: (_) => const _PairManagementDialog(),
    );
  }

  Future<void> _openLogoutConfirm() => showDialog<void>(
        context: context,
        builder: (_) => const _LogoutConfirmDialog(),
      );

  void _openDishManage() {
    HapticFeedback.lightImpact();
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const MenuManagementPage()),
    );
  }

  void _openCandyCoins() {
    HapticFeedback.lightImpact();
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const CandyCoinsPage()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return ListenableBuilder(
      listenable: state,
      builder: (context, _) {
        final String nickname = (state.user?.nickname ?? '').trim();
        final String displayName = nickname.isEmpty ? '糯米小狗' : nickname;
        final String roleKey = state.user?.role ?? '';
        final String roleText = switch (roleKey) {
          'caretaker' => '饲养员',
          'eater' => '吃货',
          _ => '待选择',
        };
        final int balance = state.candyCoins;

        return CozyPage(
          child: RefreshIndicator(
            color: CozyPalette.primary,
            onRefresh: () => state.refreshAll(),
            child: ListView(
                padding: const EdgeInsets.only(bottom: CozyDock.clearance),
                children: <Widget>[
                  // ---- ImmersiveProfileHeader (ProfileScreen.kt:321) ----
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
                    child: Column(
                      children: <Widget>[
                        _ProfileHeader(
                          name: displayName,
                          avatarUrl: state.user?.avatarUrl ?? '',
                          userId: state.user?.id ?? '',
                          isSynced: state.isLoggedIn,
                          roleText: roleText,
                          onTap: _openProfileEditor,
                        ),
                        const SizedBox(height: 16),
                        Row(
                          children: <Widget>[
                            Expanded(
                              child: _FeatureTile(
                                icon: Icons.storefront,
                                title: '我的店铺',
                                onTap: _openDishManage,
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: _FeatureTile(
                                icon: Icons.receipt_long_outlined,
                                title: '订单记录',
                                onTap: _openOrdersTab,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 10),
                  // ---- SimulatedCurrencyBalanceCard (ProfileScreen.kt:509) ----
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: _BalanceCard(balance: balance),
                  ),
                  const SizedBox(height: 10),
                  // ---- ProfileActionRow 列表 (ProfileScreen.kt:268) ----
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: Column(
                      children: <Widget>[
                        _ActionRow(
                          icon: Icons.settings,
                          title: '账号设置',
                          onTap: _openProfileEditor,
                        ),
                        const SizedBox(height: 12),
                        if (roleKey == 'caretaker')
                          _ActionRow(
                            icon: Icons.pets,
                            title: '糖糖币专属管理',
                            trailingText: '吃货 $balance 枚',
                            onTap: _openCandyCoins,
                          )
                        else
                          _ActionRow(
                            icon: Icons.pets,
                            title: '糖糖币明细',
                            trailingText: '$balance 枚',
                            onTap: _openCandyCoins,
                          ),
                        const SizedBox(height: 12),
                        _ActionRow(
                          icon: Icons.person_add_alt_1,
                          title: state.isPaired ? '伴侣已绑定' : '邀请对方',
                          trailingText: state.isPaired ? '对方' : null,
                          onTap: _openPairDialog,
                        ),
                        const SizedBox(height: 12),
                        _ActionRow(
                          icon: Icons.info,
                          title: '版本与更新',
                          trailingText: _versionLabel,
                          onTap: _openVersionDialog,
                        ),
                        const SizedBox(height: 12),
                        _ActionRow(
                          icon: Icons.support_agent,
                          title: '帮助与客服',
                          onTap: _openHelpDialog,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 10),
                  // ---- LogoutButton (ProfileScreen.kt:813) ----
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: _LogoutButton(onTap: _openLogoutConfirm),
                  ),
                ],
              ),
            ),
        );
      },
    );
  }

  /// 「订单记录」在原版是切到主壳的订单 Tab（`onOrdersClick`，ProfileScreen.kt:256）。
  void _openOrdersTab() {
    HapticFeedback.lightImpact();
    final ValueChanged<int>? navigate = widget.onNavigateTab;
    if (navigate == null) {
      _snack('订单记录请在底部「订单」查看');
      return;
    }
    navigate(3);
  }
}

// ---------------------------------------------------------------------------
// ProfileHeader (ProfileScreen.kt:376-492)
// ---------------------------------------------------------------------------

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({
    required this.name,
    required this.avatarUrl,
    required this.userId,
    required this.isSynced,
    required this.roleText,
    required this.onTap,
  });

  final String name;
  final String avatarUrl;
  final String userId;
  final bool isSynced;
  final String roleText;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final String trimmedId = userId.trim();
    final String displayUserId = trimmedId.isEmpty
        ? '未同步'
        : trimmedId.substring(trimmedId.length >= 7 ? trimmedId.length - 7 : 0);
    final String syncText = isSynced ? '资料已同步' : '等待云端同步';

    return CozyCard(
      onTap: onTap,
      radius: 18,
      padding: EdgeInsets.zero,
      color: const Color(0xFFFFF8FA).withValues(alpha: 0.72),
      borderColor: Colors.white.withValues(alpha: 0.62),
      child: Stack(
        children: <Widget>[
          Positioned(
            left: -36,
            top: -38,
            child: Container(
              width: 132,
              height: 132,
              decoration: BoxDecoration(
                color: CozyPalette.secondaryContainer.withValues(alpha: 0.16),
                borderRadius: BorderRadius.circular(48),
              ),
            ),
          ),
          Positioned(
            right: -44,
            bottom: -42,
            child: Container(
              width: 160,
              height: 160,
              decoration: BoxDecoration(
                color: CozyPalette.primaryContainer.withValues(alpha: 0.10),
                shape: BoxShape.circle,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 22),
            child: Column(
              children: <Widget>[
                SizedBox(
                  width: 102,
                  height: 102,
                  child: Stack(
                    children: <Widget>[
                      Container(
                        width: 96,
                        height: 96,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: Colors.white,
                          border: Border.all(color: const Color(0xFFFFFCF8), width: 3),
                        ),
                        child: ClipOval(child: _avatar(avatarUrl, 42)),
                      ),
                      Positioned(
                        right: 0,
                        bottom: 0,
                        child: Container(
                          width: 32,
                          height: 32,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: CozyPalette.primary,
                            border: Border.all(color: CozyPalette.outlineVariant, width: 1),
                          ),
                          child: const Icon(Icons.edit, size: 18, color: Colors.white),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  name,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                  style: _ts(24, 32, FontWeight.w900, CozyPalette.onSurface),
                ),
                const SizedBox(height: 4),
                Text(
                  'ID: $displayUserId',
                  style: _ts(14, 21, FontWeight.w400, CozyPalette.onSurfaceVariant),
                ),
                Text(
                  syncText,
                  style: _ts(11, 14, FontWeight.w400, CozyPalette.onSurfaceVariant),
                ),
                const SizedBox(height: 10),
                Container(
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.62),
                    borderRadius: BorderRadius.circular(999),
                    border: Border.all(color: CozyPalette.outlineVariant, width: 1),
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      const Icon(Icons.pets, size: 18, color: CozyPalette.primary),
                      const SizedBox(width: 6),
                      Text(
                        '当前身份：$roleText',
                        style: _ts(12, 16, FontWeight.w900, CozyPalette.onSurface),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _avatar(String url, double iconSize) {
    if (url.isNotEmpty) {
      return Image.network(
        url,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => _paw(iconSize),
      );
    }
    return _paw(iconSize);
  }

  Widget _paw(double size) => Container(
        color: const Color(0xFFFFFCF8),
        alignment: Alignment.center,
        child: Icon(Icons.pets, size: size, color: CozyPalette.primary),
      );
}

// ---------------------------------------------------------------------------
// ProfileFeatureTile (ProfileScreen.kt:713-735)
// ---------------------------------------------------------------------------

class _FeatureTile extends StatelessWidget {
  const _FeatureTile({required this.icon, required this.title, required this.onTap});

  final IconData icon;
  final String title;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return AspectRatio(
      aspectRatio: 1,
      child: CozyCard(
        onTap: onTap,
        radius: 16,
        padding: EdgeInsets.zero,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Container(
                width: 52,
                height: 52,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: CozyPalette.primaryContainer.withValues(alpha: 0.72),
                ),
                child: Icon(icon, size: 28, color: CozyPalette.primary),
              ),
              const SizedBox(height: 10),
              Text(
                title,
                style: _ts(14, 20, FontWeight.w900, CozyPalette.onSurface),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// SimulatedCurrencyBalanceCard (ProfileScreen.kt:509-559)
// ---------------------------------------------------------------------------

class _BalanceCard extends StatelessWidget {
  const _BalanceCard({required this.balance});

  final int balance;

  @override
  Widget build(BuildContext context) {
    return CozyCard(
      onTap: () {},
      radius: 16,
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      '模拟货币余额',
                      style: _ts(16, 22, FontWeight.w900, CozyPalette.onSurface),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '糖糖币不是金币，但点菜时会真实扣减',
                      style: _ts(12, 18, FontWeight.w400, CozyPalette.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Container(
                decoration: BoxDecoration(
                  color: CozyPalette.primaryContainer.withValues(alpha: 0.72),
                  borderRadius: BorderRadius.circular(999),
                ),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    const _CandyCoinIcon(size: 24),
                    const SizedBox(width: 5),
                    Text(
                      '$balance 枚',
                      style: _ts(15, 22, FontWeight.w900, CozyPalette.primary),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            '余额不足时，需要饲养员增加糖糖币后才能提交点菜',
            style: _ts(12, 18, FontWeight.w400, CozyPalette.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ProfileActionRow (ProfileScreen.kt:737-787)
// ---------------------------------------------------------------------------

class _ActionRow extends StatelessWidget {
  const _ActionRow({
    required this.icon,
    required this.title,
    this.trailingText,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String? trailingText;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final String? trailing = trailingText;
    return SizedBox(
      height: 68,
      child: CozyCard(
        onTap: onTap,
        radius: 16,
        padding: EdgeInsets.zero,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: <Widget>[
              Container(
                width: 42,
                height: 42,
                alignment: Alignment.center,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: CozyPalette.surfaceVariant,
                ),
                child: Icon(icon, size: 24, color: CozyPalette.onSurfaceVariant),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  title,
                  style: _ts(16, 22, FontWeight.w700, CozyPalette.onSurface),
                ),
              ),
              if (trailing != null && trailing.isNotEmpty)
                Container(
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFE6A7).withValues(alpha: 0.78),
                    borderRadius: BorderRadius.circular(999),
                    border: Border.all(
                      color: const Color(0xFFE8BE63).withValues(alpha: 0.38),
                      width: 1,
                    ),
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  child: Text(
                    trailing,
                    style: _ts(12, 16, FontWeight.w900, const Color(0xFF7A5320)),
                  ),
                )
              else
                const Icon(Icons.keyboard_arrow_right, color: CozyPalette.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// CandyCoinIcon (ui/components/CandyCoinIcon.kt)
// ---------------------------------------------------------------------------

class _CandyCoinIcon extends StatelessWidget {
  const _CandyCoinIcon({this.size = 24});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      'assets/images/candy_coin.png',
      width: size,
      height: size,
      fit: BoxFit.contain,
      errorBuilder: (_, _, _) => Text('🍬', style: TextStyle(fontSize: size * 0.84)),
    );
  }
}

// ---------------------------------------------------------------------------
// LogoutButton (ProfileScreen.kt:813-837)
// ---------------------------------------------------------------------------

class _LogoutButton extends StatelessWidget {
  const _LogoutButton({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 58,
      child: Material(
        color: CozyPalette.error.withValues(alpha: 0.08),
        shape: StadiumBorder(
          side: BorderSide(color: CozyPalette.error.withValues(alpha: 0.34), width: 1),
        ),
        child: InkWell(
          customBorder: const StadiumBorder(),
          onTap: onTap,
          child: Center(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                const Icon(Icons.logout, color: CozyPalette.error),
                const SizedBox(width: 8),
                Text(
                  '退出登录',
                  style: _ts(18, 26, FontWeight.w900, CozyPalette.error),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ProfileEditDialog (ProfileScreen.kt:1185-1308)
// ---------------------------------------------------------------------------

class _ProfileEditDialog extends StatefulWidget {
  const _ProfileEditDialog({required this.name, required this.avatarUrl});

  final String name;
  final String avatarUrl;

  @override
  State<_ProfileEditDialog> createState() => _ProfileEditDialogState();
}

class _ProfileEditDialogState extends State<_ProfileEditDialog> {
  late final TextEditingController _name = TextEditingController(text: widget.name);
  String? _message;

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  String get _trimmed => _name.text.trim();

  String? get _nameError {
    if (_trimmed.isEmpty) return '请输入昵称';
    if (_trimmed.length > 12) return '昵称最多 12 个字';
    return null;
  }

  void _notWired() {
    setState(() {
      _message = '暂未接入云端资料接口（需要 AppState.updateNickname / updateAvatar）';
    });
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      title: Text(
        '编辑个人资料',
        textAlign: TextAlign.center,
        style: _ts(18, 24, FontWeight.w900, CozyPalette.onSurface),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              SizedBox(
                width: 104,
                height: 104,
                child: Stack(
                  children: <Widget>[
                    Positioned.fill(
                      child: Container(
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: CozyInk.cherry,
                          border: Border.all(
                            color: CozyInk.rose.withValues(alpha: 0.26),
                            width: 2,
                          ),
                        ),
                        child: ClipOval(
                          child: widget.avatarUrl.isNotEmpty
                              ? Image.network(
                                  widget.avatarUrl,
                                  fit: BoxFit.cover,
                                  errorBuilder: (_, _, _) => const Icon(
                                    Icons.local_dining,
                                    size: 42,
                                    color: CozyInk.rose,
                                  ),
                                )
                              : const Icon(Icons.local_dining, size: 42, color: CozyInk.rose),
                        ),
                      ),
                    ),
                    Positioned(
                      right: 0,
                      bottom: 0,
                      child: GestureDetector(
                        onTap: _notWired,
                        child: Container(
                          width: 48,
                          height: 48,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: CozyInk.rose,
                            border: Border.all(color: CozyPalette.surface, width: 3),
                          ),
                          child: const Icon(Icons.edit, size: 18, color: Colors.white),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              TextButton(
                onPressed: _notWired,
                child: Text(
                  '更换头像',
                  style: _ts(14, 18, FontWeight.w900, CozyInk.rose),
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _name,
                onChanged: (_) => setState(() => _message = null),
                decoration: cozyInputDecoration(
                  labelText: '昵称',
                  errorText: _nameError,
                ),
              ),
              if (_nameError == null) ...<Widget>[
                const SizedBox(height: 6),
                Text(
                  '${_trimmed.length}/12',
                  style: _ts(12, 18, FontWeight.w400, CozyPalette.onSurfaceVariant),
                ),
              ],
              if (_message != null) ...<Widget>[
                const SizedBox(height: 16),
                Text(
                  _message!,
                  textAlign: TextAlign.center,
                  style: _ts(12, 18, FontWeight.w400, CozyPalette.error),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text('取消', style: _ts(14, 18, FontWeight.w500, CozyPalette.onSurfaceVariant)),
        ),
        FilledButton(
          onPressed: _nameError == null ? _notWired : null,
          style: FilledButton.styleFrom(
            backgroundColor: CozyPalette.primary,
            foregroundColor: Colors.white,
            shape: const StadiumBorder(),
          ),
          child: const Text('保存资料'),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// HelpDialog (ProfileScreen.kt:1126-1154)
// ---------------------------------------------------------------------------

class _HelpDialog extends StatelessWidget {
  const _HelpDialog();

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      title: Text(
        '帮助与客服',
        textAlign: TextAlign.center,
        style: _ts(18, 24, FontWeight.w900, CozyPalette.onSurface),
      ),
      content: Text(
        '请联系最帅的管理员。',
        textAlign: TextAlign.center,
        style: _ts(15, 22, FontWeight.w400, CozyPalette.onSurfaceVariant),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(
            '知道了',
            style: _ts(14, 18, FontWeight.w900, CozyPalette.primary),
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// VersionInfoDialog (ProfileScreen.kt:1006-1124)
// ---------------------------------------------------------------------------

class _VersionInfoDialog extends StatelessWidget {
  const _VersionInfoDialog();

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      title: Text(
        '高糖小食 $_appVersion',
        textAlign: TextAlign.center,
        style: _ts(18, 24, FontWeight.w900, CozyPalette.onSurface),
      ),
      content: Text(
        '当前版本：$_appVersion',
        style: _ts(15, 22, FontWeight.w400, CozyPalette.onSurfaceVariant),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text('关闭', style: _ts(14, 18, FontWeight.w500, CozyPalette.onSurfaceVariant)),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(
            '重新检查',
            style: _ts(14, 18, FontWeight.w900, CozyInk.rose),
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// PairManagementDialog (ProfileScreen.kt:839-991)
// ---------------------------------------------------------------------------

class _PairManagementDialog extends StatefulWidget {
  const _PairManagementDialog();

  @override
  State<_PairManagementDialog> createState() => _PairManagementDialogState();
}

class _PairManagementDialogState extends State<_PairManagementDialog> {
  final TextEditingController _joinCode = TextEditingController();
  String? _message;

  @override
  void dispose() {
    _joinCode.dispose();
    super.dispose();
  }

  String get _code => _joinCode.text.trim().toUpperCase();

  Future<void> _generate() async {
    final state = AppState.instance;
    final bool ok = await state.createPair();
    if (!mounted) return;
    setState(() {
      _message = ok ? null : (state.error ?? '邀请码生成失败，请检查网络后重试');
    });
  }

  Future<void> _join() async {
    final state = AppState.instance;
    final bool ok = await state.joinPair(_code);
    if (!mounted) return;
    if (!ok && state.error != null) {
      setState(() => _message = state.error);
    } else {
      Navigator.of(context).pop();
    }
  }

  void _copyCode(String code) {
    if (code.isEmpty) return;
    Clipboard.setData(ClipboardData(text: code));
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已复制邀请码')));
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: AppState.instance,
      builder: (context, _) {
        final state = AppState.instance;
        final bool paired = state.isPaired;
        final String inviteCode = state.pair?.inviteCode ?? '';

        return AlertDialog(
          backgroundColor: CozyPalette.surface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
          title: Text(
            paired ? '伴侣已绑定' : '邀请对方',
            textAlign: TextAlign.center,
            style: _ts(18, 24, FontWeight.w900, CozyPalette.onSurface),
          ),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: Column(
                children: <Widget>[
                  Text(
                    paired
                        ? '已和 对方 绑定。你们正在共享情侣资料、店铺、菜单和订单。'
                        : '请先在首页选择身份。饲养员邀请对方去点餐；吃货邀请对方去做饭，确认后才会绑定。',
                    textAlign: TextAlign.center,
                    style: _ts(15, 22, FontWeight.w400, CozyPalette.onSurfaceVariant),
                  ),
                  if (!paired) ...<Widget>[
                    const SizedBox(height: 14),
                    CozyCard(
                      radius: 22,
                      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 18),
                      color: CozyPalette.primaryContainer.withValues(alpha: 0.72),
                      borderColor: CozyPalette.primary.withValues(alpha: 0.22),
                      child: Column(
                        children: <Widget>[
                          Text(
                            inviteCode.isEmpty ? '我的邀请码' : '把这个邀请码发给对方',
                            textAlign: TextAlign.center,
                            style: _ts(14, 18, FontWeight.w900, CozyPalette.onSurface),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            inviteCode.isEmpty ? '点击生成' : inviteCode,
                            textAlign: TextAlign.center,
                            style: _ts(
                              inviteCode.isEmpty ? 26 : 38,
                              inviteCode.isEmpty ? 32 : 44,
                              FontWeight.w900,
                              CozyPalette.primary,
                            ),
                          ),
                          if (inviteCode.isNotEmpty) ...<Widget>[
                            const SizedBox(height: 12),
                            Text(
                              '等待对方输入后才会完成绑定',
                              textAlign: TextAlign.center,
                              style: _ts(12, 18, FontWeight.w400, CozyPalette.onSurfaceVariant),
                            ),
                          ],
                          const SizedBox(height: 12),
                          FilledButton(
                            onPressed: _generate,
                            style: FilledButton.styleFrom(
                              backgroundColor: CozyPalette.primary,
                              foregroundColor: Colors.white,
                              shape: const StadiumBorder(),
                            ),
                            child: Text(inviteCode.isEmpty ? '生成邀请码' : '重新生成'),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 6),
                    TextButton(
                      onPressed: inviteCode.isEmpty ? null : () => _copyCode(inviteCode),
                      child: const Text('复制邀请码'),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: _joinCode,
                      onChanged: (_) => setState(() => _message = null),
                      inputFormatters: <TextInputFormatter>[LengthLimitingTextInputFormatter(6)],
                      decoration: cozyInputDecoration(labelText: '输入对方邀请码'),
                    ),
                  ],
                  if (_message != null) ...<Widget>[
                    const SizedBox(height: 10),
                    Text(
                      _message!,
                      textAlign: TextAlign.center,
                      style: _ts(12, 18, FontWeight.w400, CozyPalette.primary),
                    ),
                  ],
                ],
              ),
            ),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: Text('关闭', style: _ts(14, 18, FontWeight.w500, CozyPalette.onSurfaceVariant)),
            ),
            if (paired)
              TextButton(
                onPressed: () {
                  final NavigatorState navigator = Navigator.of(context);
                  navigator.pop();
                  showDialog<void>(
                    context: navigator.context,
                    builder: (_) => const _UnpairConfirmDialog(),
                  );
                },
                child: Text(
                  '解除绑定',
                  style: _ts(14, 18, FontWeight.w900, CozyPalette.error),
                ),
              )
            else
              FilledButton(
                onPressed: _code.length == 6 ? _join : null,
                style: FilledButton.styleFrom(
                  backgroundColor: CozyPalette.primary,
                  foregroundColor: Colors.white,
                  shape: const StadiumBorder(),
                ),
                child: const Text('绑定'),
              ),
          ],
        );
      },
    );
  }
}

// ---------------------------------------------------------------------------
// 解除绑定确认 (ProfileScreen.kt:204-219)
// ---------------------------------------------------------------------------

class _UnpairConfirmDialog extends StatelessWidget {
  const _UnpairConfirmDialog();

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      title: Text(
        '确认解除绑定？',
        style: _ts(18, 24, FontWeight.w900, CozyPalette.onSurface),
      ),
      content: Text(
        '解除后双方将停止同步新内容，共同订单、店铺和纪念日会各自保留。对方会收到解绑通知。',
        style: _ts(15, 22, FontWeight.w400, CozyPalette.onSurfaceVariant),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('暂不解除'),
        ),
        TextButton(
          onPressed: () {
            Navigator.of(context).pop();
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('解除绑定需要 AppState.unpair（尚未接入）')),
            );
          },
          child: Text(
            '确认解除',
            style: _ts(14, 18, FontWeight.w900, CozyPalette.error),
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// LogoutConfirmDialog (ProfileScreen.kt:1156-1183)
// ---------------------------------------------------------------------------

class _LogoutConfirmDialog extends StatelessWidget {
  const _LogoutConfirmDialog();

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      icon: Container(
        width: 64,
        height: 64,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: CozyPalette.error.withValues(alpha: 0.12),
          border: Border.all(color: CozyPalette.outlineVariant, width: 1),
        ),
        child: const Icon(Icons.warning, size: 34, color: CozyPalette.error),
      ),
      title: Text(
        '确认要离开吗？',
        textAlign: TextAlign.center,
        style: _ts(18, 24, FontWeight.w900, CozyPalette.onSurface),
      ),
      content: Text(
        '小狗会想念你的哦...',
        textAlign: TextAlign.center,
        style: _ts(15, 22, FontWeight.w400, CozyPalette.onSurfaceVariant),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text('再留一会', style: _ts(14, 18, FontWeight.w500, CozyPalette.onSurfaceVariant)),
        ),
        FilledButton(
          onPressed: () async {
            Navigator.of(context).pop();
            await AppState.instance.logout();
          },
          style: FilledButton.styleFrom(
            backgroundColor: CozyPalette.error,
            foregroundColor: Colors.white,
            shape: const StadiumBorder(),
          ),
          child: const Text('狠心退出'),
        ),
      ],
    );
  }
}
