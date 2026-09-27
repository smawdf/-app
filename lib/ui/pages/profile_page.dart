import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';

import '../../data/app_state.dart';
import '../candy/candy_coins_page.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';
import 'avatar_crop_page.dart';

/// 原生 `BuildConfig.VERSION_NAME` 的等值常量。
/// 【真机修正】pubspec.yaml:17 实际是 `version: 2.0.0+56`，这里却写着 3.0.1，
/// 「我的 → 版本与更新 / 关于」显示的是不存在的版本号。改回与 pubspec 一致；
/// 以后改版本号时这两处必须同步（未引入 package_info_plus，避免多加依赖）。
const String _appVersion = '2.0.0';

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

  /// 云端 `anniversaries.anniversary_at`（与首页同一口径），用来把身份卡第二行
  /// 换成「已绑定小饭桌 · 一起吃饭 N 天」这种用户真看得懂的信息。
  DateTime? _anniversaryDate;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshTransactions();
    });
    _loadAnniversary();
  }

  Future<void> _loadAnniversary() async {
    final Map<String, dynamic>? info = await AppState.instance.loadAnniversary();
    if (!mounted) return;
    setState(() {
      _anniversaryDate = _parseDateSafely((info?['anniversary_at'] as String?) ?? '');
    });
  }

  /// 一起吃饭的天数：起始日当天算第 1 天（与纪念日页口径一致）。
  int? get _daysTogether {
    final DateTime? start = _anniversaryDate;
    if (start == null) return null;
    final DateTime now = DateTime.now();
    final DateTime today = DateTime(now.year, now.month, now.day);
    final int days = today.difference(start).inDays + 1;
    return days < 1 ? 1 : days;
  }

  /// 原生 `CozyMainTopBar`（与「订单」「发现」等页同一规格）。
  Widget _topBar() {
    return CozyMainTopBar(
      title: const Text(
        '我的 - 小饭桌与设置',
        textAlign: TextAlign.center,
        maxLines: 2,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontSize: 20,
          height: 24 / 20,
          fontWeight: FontWeight.w900,
          letterSpacing: 0,
          color: CozyPalette.primary,
        ),
      ),
      leading: SizedBox(
        width: 44,
        child: Align(
          alignment: Alignment.centerLeft,
          child: Icon(Icons.favorite, size: 26, color: CozyPalette.primary.withValues(alpha: 0.82)),
        ),
      ),
      trailing: const SizedBox(
        width: 44,
        child: Align(
          alignment: Alignment.centerRight,
          child: Icon(Icons.notifications, size: 24, color: CozyPalette.onSurfaceVariant),
        ),
      ),
    );
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

        // 身份卡第二行：不再露 uuid 尾巴和无信息量的「资料已同步」，
        // 换成「绑没绑小饭桌 + 一起吃了多少天」这种用户真在意的事。
        // 天数单独做成小胶囊，和身份胶囊并排（Wrap 自适应，窄屏换行不溢出）。
        final int? days = _daysTogether;
        final String partnerName = (state.pair?.partnerName ?? '').trim();
        final String headerSubtitle = state.isPaired
            ? (partnerName.isNotEmpty ? '已和 $partnerName 绑定小饭桌' : '已绑定小饭桌')
            : '还没有绑定小饭桌 · 去「邀请对方」看看';
        final String? headerDays =
            (state.isPaired && days != null) ? '一起吃饭 $days 天' : null;

        return CozyPage(
          child: Column(
            children: <Widget>[
              _topBar(),
              Expanded(
                child: RefreshIndicator(
                  color: CozyPalette.primary,
                  onRefresh: () => state.refreshAll(),
                  child: ListView(
                    padding: EdgeInsets.only(bottom: CozyDock.clearanceOf(context)),
                    children: <Widget>[
                      // ---- ImmersiveProfileHeader (ProfileScreen.kt:321) ----
                      Padding(
                        padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
                        child: _ProfileHeader(
                          name: displayName,
                          avatarUrl: state.user?.avatarUrl ?? '',
                          subtitle: headerSubtitle,
                          roleText: roleText,
                          daysText: headerDays,
                          onTap: _openProfileEditor,
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
                      // 「我的店铺 / 订单记录」原来是与下面同类的两张 155×155 巨卡，
                      // 现在统一成同规格的列表行，整页只剩一种入口样式。
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: Column(
                          children: <Widget>[
                            _ActionRow(
                              icon: Icons.storefront,
                              title: '我的店铺',
                              onTap: _openDishManage,
                            ),
                            const SizedBox(height: 12),
                            _ActionRow(
                              icon: Icons.receipt_long_outlined,
                              title: '订单记录',
                              onTap: _openOrdersTab,
                            ),
                            const SizedBox(height: 12),
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
                                // 余额上面那张卡已经显示过一次，这里不再重复数字。
                                onTap: _openCandyCoins,
                              ),
                            const SizedBox(height: 12),
                            _ActionRow(
                              icon: Icons.person_add_alt_1,
                              title: state.isPaired ? '伴侣已绑定' : '邀请对方',
                              // 【真机修正】原来写死「对方」，现在显示真实伴侣昵称
                              // （`CouplePair.partnerName`）。
                              trailingText: state.isPaired
                                  ? (state.pair?.partnerName.isNotEmpty == true
                                      ? state.pair!.partnerName
                                      : '对方')
                                  : null,
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
              ),
            ],
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
    required this.subtitle,
    required this.roleText,
    required this.onTap,
    this.daysText,
  });

  final String name;
  final String avatarUrl;

  /// 第三行信息（绑没绑小饭桌）。
  /// 【真机修正】原来这里是 `ID: d20eeab`（uuid 尾巴）和「资料已同步」——
  /// 前者是调试信息，后者用户无法验证，已按用户反馈替换。
  final String subtitle;
  final String roleText;

  /// 「一起吃饭 N 天」小胶囊（未绑定时为 null，不占位）
  final String? daysText;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return CozyCard(
      onTap: onTap,
      radius: 24,
      padding: EdgeInsets.zero,
      color: Colors.white.withValues(alpha: 0.70),
      borderColor: Colors.white.withValues(alpha: 0.66),
      // 【真机修正】CozyCard 不撑满宽度：里面是 Column + Stack，会缩到最宽子项
      // （身份胶囊 ≈206 逻辑 px），导致这张卡比全页其他卡（328）窄一大截、
      // 居中悬在页面上。这里显式撑满。
      child: SizedBox(
        width: double.infinity,
        child: Stack(
          children: <Widget>[
            // 淡粉斜向渐变：让白卡有层次，不再是「一块白纸」
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: <Color>[
                      CozyPalette.secondaryContainer.withValues(alpha: 0.34),
                      CozyPalette.primaryContainer.withValues(alpha: 0.16),
                      Colors.white.withValues(alpha: 0.0),
                    ],
                    stops: const <double>[0.0, 0.55, 1.0],
                  ),
                ),
              ),
            ),
            // 【真机修正】原来左上角是 132×132 + radius 48 的**圆角方**，
            // 压到卡片圆角上会露出直角边（截图里那道月牙 + 硬边）。改成正圆。
            Positioned(left: -54, top: -58, child: _glow(150, CozyPalette.secondaryContainer, 0.20)),
            Positioned(right: -46, bottom: -56, child: _glow(170, CozyPalette.primaryContainer, 0.14)),
            // 右下角爪印水印（和首页小饭桌卡同一套语言）
            Positioned(
              right: -26,
              bottom: -30,
              child: Icon(
                Icons.pets,
                size: 156,
                color: Colors.white.withValues(alpha: 0.30),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 24, 20, 22),
              child: Column(
                children: <Widget>[
                  // 头像：白色渐变圆 + 玫瑰细边 + 右下编辑徽标
                  SizedBox(
                    width: 118,
                    height: 118,
                    child: Stack(
                      children: <Widget>[
                        Positioned.fill(
                          child: Container(
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              gradient: LinearGradient(
                                begin: Alignment.topCenter,
                                end: Alignment.bottomCenter,
                                colors: <Color>[
                                  Colors.white,
                                  CozyPalette.primaryContainer.withValues(alpha: 0.55),
                                ],
                              ),
                              boxShadow: CozyLight.cardShadow,
                            ),
                          ),
                        ),
                        Positioned.fill(
                          child: Padding(
                            padding: const EdgeInsets.all(9),
                            child: ClipOval(child: _avatar(avatarUrl, 46)),
                          ),
                        ),
                        Positioned(
                          right: 2,
                          bottom: 2,
                          child: Container(
                            width: 34,
                            height: 34,
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: CozyPalette.primary,
                              border: Border.all(color: Colors.white, width: 3),
                            ),
                            child: const Icon(Icons.edit, size: 16, color: Colors.white),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),
                  // 窄屏 / 平板都收在 420 内居中，不会拉成一条长线（适配）
                  Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 420),
                      child: Column(
                        children: <Widget>[
                          Text(
                            name,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            textAlign: TextAlign.center,
                            style: _ts(26, 34, FontWeight.w900, CozyPalette.onSurface),
                          ),
                          const SizedBox(height: 10),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            alignment: WrapAlignment.center,
                            children: <Widget>[
                              _chip(Icons.pets, '当前身份：$roleText', filled: true),
                              if (daysText != null)
                                _chip(Icons.favorite, daysText!, filled: false),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Text(
                            subtitle,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            textAlign: TextAlign.center,
                            style: _ts(12.5, 18, FontWeight.w400, CozyPalette.onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _glow(double size, Color color, double alpha) => Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: color.withValues(alpha: alpha),
        ),
      );

  /// 信息小胶囊（身份 / 一起吃饭 N 天）
  Widget _chip(IconData icon, String text, {required bool filled}) => Container(
        decoration: BoxDecoration(
          color: filled
              ? Colors.white.withValues(alpha: 0.66)
              : CozyPalette.primaryContainer.withValues(alpha: 0.34),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: CozyPalette.outlineVariant, width: 1),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Icon(icon, size: 15, color: CozyPalette.primary),
            const SizedBox(width: 5),
            Text(
              text,
              style: _ts(12, 16, FontWeight.w900, CozyPalette.onSurface),
            ),
          ],
        ),
      );

  /// 头像（云端可能是 http URL、data URI，或内置默认头像的 asset 标记）
  Widget _avatar(String url, double iconSize) =>
      CozyAvatar(url: url, size: 100, fallback: _paw(iconSize));

  Widget _paw(double size) => Container(
        color: const Color(0xFFFFFCF8),
        alignment: Alignment.center,
        child: Icon(Icons.pets, size: size, color: CozyPalette.primary),
      );
}

// ---------------------------------------------------------------------------
// ProfileFeatureTile (ProfileScreen.kt:713-735)
// ---------------------------------------------------------------------------
// 【真机修正】原生这两张 155×155 巨卡已被换成与下方同规格的 `_ActionRow`
// （用户反馈：同页两种入口样式、且和首页/底栏入口重复）。原来的 `_FeatureTile`
// 一并删除，避免留一个没人用的私有类。

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
                      '糖糖币余额',
                      style: _ts(16, 22, FontWeight.w900, CozyPalette.onSurface),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '点菜时真实扣减，是你们小饭桌的专属币',
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
                  // 【真机修正】原色是琥珀黄（0xFFFFE6A7 / 边 0xFFE8BE63），
                  // 是全局调色板里没有的第三种强调色，且和上面余额卡同一个数据的
                  // 粉色胶囊对不上。统一到 secondaryContainer。
                  decoration: BoxDecoration(
                    color: CozyPalette.secondaryContainer.withValues(alpha: 0.72),
                    borderRadius: BorderRadius.circular(999),
                    border: Border.all(
                      color: CozyPalette.outlineVariant.withValues(alpha: 0.62),
                      width: 1,
                    ),
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  child: Text(
                    trailing,
                    style: _ts(12, 16, FontWeight.w900, CozyPalette.secondary),
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

  /// 当前生效的头像（打开时=云端值；选图后=刚选的那张 data URI）
  late String _avatarUrl = widget.avatarUrl;
  bool _saving = false;
  String? _message;
  bool _failed = false;

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

  bool get _avatarChanged => _avatarUrl != widget.avatarUrl;

  /// 选图 / 裁剪期间禁止重复点击（保存另有 `_saving`）
  bool _picking = false;

  bool get _locked => _saving || _picking;

  /// 相册选图 → 裁剪页（方形取景 + 圆形预览，可拖可缩放）→ 256×256 JPEG q72
  /// → data URI 存 `profiles.avatar_url`（项目没有 storage 桶，
  /// 见 `SupabaseApi.updateProfile` 的说明）。
  Future<void> _pickAvatar() async {
    if (_locked) return;
    try {
      final XFile? picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        // 先在系统侧限一下尺寸（相册里动辄 4000px，裁剪页只用到 1600）
        maxWidth: 2000,
        maxHeight: 2000,
        imageQuality: 88,
      );
      if (picked == null) return; // 用户取消
      final Uint8List raw = await picked.readAsBytes();
      if (raw.isEmpty) {
        setState(() {
          _failed = true;
          _message = '这张图片读不出来，换一张试试';
        });
        return;
      }
      await _crop(raw);
    } catch (e) {
      setState(() {
        _failed = true;
        _message = '打开相册失败：$e';
      });
    }
  }

  /// 直接裁剪「当前这张」（刚选的 data URI，或云端存过的 data URI）。
  /// 默认头像 / 旧 http 图没有可裁的本地字节，这时给一句提示。
  Future<void> _cropCurrent() async {
    if (_locked) return;
    final String url = _avatarUrl;
    if (!CozyAvatar.isDataUri(url)) {
      setState(() {
        _failed = false;
        _message = '先点「更换」选一张照片，再裁剪';
      });
      return;
    }
    final Uint8List? bytes = CozyAvatar.decodeDataUri(url);
    if (bytes == null) {
      setState(() {
        _failed = true;
        _message = '这张头像解不开，请重新「更换」一张';
      });
      return;
    }
    await _crop(bytes);
  }

  Future<void> _crop(Uint8List raw) async {
    setState(() => _picking = true);
    Uint8List? result;
    try {
      result = await Navigator.of(context).push<Uint8List>(
        MaterialPageRoute<Uint8List>(
          builder: (_) => AvatarCropPage(bytes: raw),
        ),
      );
    } finally {
      if (mounted) setState(() => _picking = false);
    }
    if (result == null || result.isEmpty) return; // 裁剪页取消
    final Uint8List cropped = result;
    setState(() {
      _avatarUrl = 'data:image/jpeg;base64,${base64Encode(cropped)}';
      _failed = false;
      _message = '已裁好新头像（${(cropped.length / 1024).toStringAsFixed(1)} KB），点「保存资料」同步给小饭桌';
    });
  }

  /// 删除头像：写空串，全 App 回退成小爪印
  void _removeAvatar() {
    setState(() {
      _avatarUrl = '';
      _failed = false;
      _message = '保存后就只剩小爪印啦';
    });
  }

  /// 恢复默认：换成随 App 打包的默认头像（写 `asset:` 标记，双方都能显示）
  void _useDefaultAvatar() {
    setState(() {
      _avatarUrl = CozyAvatar.defaultAvatarMark;
      _failed = false;
      _message = '保存后用小饭桌的默认头像';
    });
  }

  Future<void> _save() async {
    if (_nameError != null || _locked) return;
    setState(() {
      _saving = true;
      _failed = false;
      _message = null;
    });
    final bool ok = await AppState.instance.updateProfile(
      nickname: _trimmed,
      avatarUrl: _avatarChanged ? _avatarUrl : null,
    );
    if (!mounted) return;
    if (!ok) {
      setState(() {
        _saving = false;
        _failed = true;
        _message = AppState.instance.error ?? '保存失败，请检查网络或稍后重试';
      });
      return;
    }
    Navigator.of(context).pop();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('资料已同步，伴侣那边也能看到新头像')),
    );
  }

  /// 头像操作小胶囊（更换 / 裁剪 / 删除 / 恢复默认）
  Widget _avatarAction(IconData icon, String label, VoidCallback onTap) {
    final bool on = !_locked;
    return GestureDetector(
      onTap: on ? onTap : null,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
        decoration: BoxDecoration(
          color: CozyPalette.secondaryContainer.withValues(alpha: on ? 0.34 : 0.14),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: CozyPalette.outlineVariant.withValues(alpha: on ? 0.7 : 0.35),
            width: 1,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Icon(icon, size: 14, color: CozyInk.rose),
            const SizedBox(width: 5),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                height: 16 / 12,
                fontWeight: FontWeight.w900,
                color: on ? CozyInk.rose : CozyInk.rose.withValues(alpha: 0.45),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // 标题 / 头像 / 四个操作 / 昵称 / 提示 / 按钮全部放进同一个滚动容器。
    // 曾用 AlertDialog 的 content + actions：内容一长（提示文案换成两行）时，
    // 最后一个 Text 的布局盒会压到 actions 之上并吃掉点击 —— 真机实测
    // 「保存资料 / 取消」在 y 1512~1599 全部点不动、y≥1600 才有反应。
    // 内容与按钮同处一个 SingleChildScrollView 后不再有兄弟节点重叠，
    // 并且弹层高度恒定：提示固定占一个 40px 槽、计数恒定显示、
    // 昵称错误也走提示槽（不用会撑高输入框的 errorText）、转圈挪进头像里。
    // 高度一变，真机上布局几何与绘制几何就会错位约 80px，按钮便点不动。
    return Dialog(
      backgroundColor: CozyPalette.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      insetPadding: const EdgeInsets.symmetric(horizontal: 36, vertical: 24),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 22, 24, 14),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text(
                '编辑个人资料',
                textAlign: TextAlign.center,
                style: _ts(18, 24, FontWeight.w900, CozyPalette.onSurface),
              ),
              const SizedBox(height: 10),
              // 头像：白圆底 + 玫瑰描边；点右下角小铅笔 = 更换（选图 → 裁剪）
              SizedBox(
                width: 112,
                height: 112,
                child: Stack(
                  children: <Widget>[
                    Positioned.fill(
                      child: Container(
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: Colors.white,
                          border: Border.all(
                            color: CozyPalette.primaryContainer.withValues(alpha: 0.72),
                            width: 3,
                          ),
                        ),
                        child: ClipOval(
                          child: CozyAvatar(
                            url: _avatarUrl,
                            size: 112,
                            fallback: const Icon(Icons.pets, size: 46, color: CozyPalette.primary),
                          ),
                        ),
                      ),
                    ),
                    Positioned(
                      right: 0,
                      bottom: 0,
                      child: GestureDetector(
                        onTap: _locked ? null : _pickAvatar,
                        child: Container(
                          width: 38,
                          height: 38,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: CozyInk.rose,
                            border: Border.all(color: Colors.white, width: 3),
                          ),
                          child: const Icon(Icons.edit, size: 17, color: Colors.white),
                        ),
                      ),
                    ),
                    // 选图 / 裁剪 / 保存共用同一个转圈覆盖层：放进头像里，
                    // 弹层高度不会因为「正在保存」而变化（高度一变，按钮就会跳）。
                    if (_picking || _saving)
                      const Positioned.fill(
                        child: ColoredBox(
                          color: Color(0x33FFFFFF),
                          child: Center(
                            child: SizedBox(
                              width: 22,
                              height: 22,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              // 四个头像操作：窄屏自动换行，不会挤出弹层（用户反馈过「适配」）
              Wrap(
                spacing: 8,
                runSpacing: 8,
                alignment: WrapAlignment.center,
                children: <Widget>[
                  _avatarAction(Icons.photo_library_outlined, '更换', _pickAvatar),
                  _avatarAction(Icons.crop, '裁剪', _cropCurrent),
                  _avatarAction(Icons.delete_outline, '删除', _removeAvatar),
                  _avatarAction(Icons.restore, '恢复默认', _useDefaultAvatar),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                '从相册选一张，拖动裁剪成头像；头像存在你们的小饭桌里',
                textAlign: TextAlign.center,
                style: _ts(11, 16, FontWeight.w400, CozyPalette.onSurfaceVariant),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _name,
                onChanged: (_) => setState(() => _message = null),
                decoration: cozyInputDecoration(labelText: '昵称'),
              ),
              const SizedBox(height: 6),
              // 字数恒定显示：不再用 errorText（它会撑高输入框，连带把按钮顶下去）
              Text(
                '${_trimmed.length}/12',
                style: _ts(12, 18, FontWeight.w400, CozyPalette.onSurfaceVariant),
              ),
              // 固定高度的提示槽：昵称错误 / 操作反馈都写在这里，
              // 出现与消失都不会改变弹层高度 ⇒ 按钮位置始终不变（真机点击才可靠）。
              SizedBox(
                height: 40,
                child: Center(
                  child: Text(
                    _nameError ?? _message ?? '',
                    maxLines: 2,
                    textAlign: TextAlign.center,
                    style: _ts(
                      12,
                      18,
                      FontWeight.w400,
                      _nameError != null || _failed
                          ? CozyPalette.error
                          : CozyPalette.onSurfaceVariant,
                    ),
                  ),
                ),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  TextButton(
                    onPressed: _locked ? null : () => Navigator.of(context).pop(),
                    child: Text('取消', style: _ts(14, 18, FontWeight.w500, CozyPalette.onSurfaceVariant)),
                  ),
                  const SizedBox(width: 10),
                  FilledButton(
                    onPressed: _nameError == null && !_locked ? _save : null,
                    style: FilledButton.styleFrom(
                      backgroundColor: CozyPalette.primary,
                      foregroundColor: Colors.white,
                      shape: const StadiumBorder(),
                    ),
                    child: const Text('保存资料'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
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
        // 【真机修正】原来写死「已和 对方 绑定」，现在用真实伴侣昵称。
        final String partnerName = state.pair?.partnerName.trim() ?? '';
        final String displayPartnerName = partnerName.isNotEmpty ? partnerName : '对方';

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
                        ? '已和 $displayPartnerName 绑定。你们正在共享情侣资料、店铺、菜单和订单。'
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

/// 与首页/纪念日页同一口径的日期解析（`anniversary_at` 可能带时间部分）。
DateTime? _parseDateSafely(String value) {
  if (value.isEmpty) return null;
  final DateTime? iso = DateTime.tryParse(value);
  if (iso != null) return DateTime(iso.year, iso.month, iso.day);
  final String head = value.length > 10 ? value.substring(0, 10) : value;
  final DateTime? dateOnly = DateTime.tryParse(head);
  if (dateOnly == null) return null;
  return DateTime(dateOnly.year, dateOnly.month, dateOnly.day);
}
