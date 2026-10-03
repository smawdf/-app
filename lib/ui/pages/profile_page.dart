import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';

import '../../data/app_state.dart';
import '../auth/auth_screen.dart';
import '../candy/candy_coins_page.dart';
import '../couple/anniversary_page.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';
import '../theme/couple_theme.dart';
import '../widgets/cozy_celebration.dart';
import '../widgets/cozy_count_up.dart';
import '../widgets/cozy_toast.dart';
import 'avatar_crop_page.dart';

/// 原生 `BuildConfig.VERSION_NAME` 的等值常量。
/// 【真机修正】pubspec.yaml:17 实际是 `version: 2.0.0+56`，这里却写着 3.0.1，
/// 「我的 → 版本与更新 / 关于」显示的是不存在的版本号。改回与 pubspec 一致；
/// 以后改版本号时这两处必须同步（未引入 package_info_plus，避免多加依赖）。
const String _appVersion = '2.0.0+61';

/// Compose 的 `fontSize.sp + lineHeight.sp` 在 Flutter 里要换算成 `height` 倍率。
/// 全页字号/行高一律走这里，字体族由全局 `ThemeData.textTheme` 提供（RomanticRound）。
TextStyle _ts(double size, double lineHeight, FontWeight weight, Color color) =>
    TextStyle(fontSize: size, height: lineHeight / size, fontWeight: weight, color: color);

/// 个人与小店 —— 结构对齐 `demo/index.html` 的 `#tab-profile`（原生行为全部保留）。
///
/// 骨架顺序（1:1 照 demo 的三块）：
///   · Profile Card           资料卡：头像 + 昵称 + 「编辑资料」 + 身份/糖糖币标签
///   · Couple Connection Tile 伴侣邀请码 (Pair ID) + 复制
///   · Settings List          我的店铺管理 / 纪念日日历 / 糖糖币充值·撒糖中心
/// 之后是 demo 没有、页面本来就有的三个入口（订单记录 / 版本与更新 / 帮助与客服）
/// 与「退出登录」，用同一套设置行样式收尾，功能一行不减。
/// 弹窗：`ProfileEditDialog` / `HelpDialog` / `VersionInfoDialog` /
///       `PairManagementDialog` / 解除绑定确认 / `LogoutConfirmDialog`（均未改动）
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

  void _snack(String message) {
    showCozyToast(context, message);
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

  /// 打开【情侣空间 · 主题工坊】弹窗
  void _openCoupleThemeModal() {
    HapticFeedback.lightImpact();
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (_) => const _CoupleThemeModal(),
    );
  }

  /// demo「纪念日日历」设置项：原页面没有这个入口（只在首页有），按 demo 补上。
  void _openAnniversary() {
    HapticFeedback.lightImpact();
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const AnniversaryPage()),
    );
  }

  void _openAuth({bool register = false}) {
    HapticFeedback.lightImpact();
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => AuthScreen(initialRegister: register)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    // 本页只读 user / pair / 糖糖币：订阅 profile+candy 域（与首页同一套域订阅）。
    return ListenableBuilder(
      listenable: state.listenFor(const {Domain.profile, Domain.candy}),
      builder: (context, _) {
        // 昵称：空就是空，用引导文案占住位置（不编「糯米小狗」这种假名字）。
        final String nickname = (state.user?.nickname ?? '').trim();
        final bool hasNickname = nickname.isNotEmpty;
        final String roleKey = state.user?.role ?? '';
        final String roleText = switch (roleKey) {
          'caretaker' => '饲养员',
          'eater' => '吃货',
          _ => '还没有选择身份',
        };
        final int balance = state.candyCoins;
        // 相守天数：原来挂在资料卡的身份胶囊旁（demo 的资料卡没有这一格），
        // 结构对齐后挪到「纪念日日历」行的行尾；仍然是真实数据，没有就不显示。
        final int? days = state.isPaired ? _daysTogether : null;
        final String inviteCode = (state.pair?.inviteCode ?? '').trim();

        return CozyPage(
          // demo `#tab-profile` 没有页标题区：资料卡直接开场。
          // 原先的液态玻璃浮层顶栏（含装饰性爱心/铃铛）已按 demo 移除，
          // 顶部留白从 `CozyGlassTopBar.reserved` 收到 16。
          child: Stack(
            children: <Widget>[
              Positioned.fill(
                child: RefreshIndicator(
                  color: CozyPalette.primary,
                  onRefresh: () => state.refreshAll(),
                  child: ListView(
                    padding: EdgeInsets.only(
                      top: 16,
                      bottom: CozyDock.clearanceOf(context),
                    ),
                    children: <Widget>[
                      // ---- demo「Profile Card」：头像 + 昵称 + 编辑资料 + 身份/糖糖币标签 ----
                      Padding(
                        padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                        child: _ProfileCard(
                          nickname: nickname,
                          hasNickname: hasNickname,
                          avatarUrl: state.user?.avatarUrl ?? '',
                          roleKey: roleKey,
                          roleText: roleText,
                          coins: balance,
                          onTap: _openProfileEditor,
                        ),
                      ),
                      const SizedBox(height: 12),
                      // ---- 云端同步与账号登录/注册卡片 ----
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: _CloudAuthCard(
                          isGuest: state.isGuest,
                          isLoggedIn: state.isLoggedIn && !state.isGuest,
                          email: state.user?.username ?? '',
                          onLogin: () => _openAuth(register: false),
                          onRegister: () => _openAuth(register: true),
                        ),
                      ),
                      const SizedBox(height: 12),
                      // ---- demo「Couple Connection Tile」（本页原先没有这一块）----
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: _PairTile(
                          isPaired: state.isPaired,
                          isFullyBound: state.pair?.isFullyBound ?? false,
                          partnerName: state.pair?.partnerName ?? '',
                          inviteCode: inviteCode,
                          onTap: _openPairDialog,
                        ),
                      ),
                      const SizedBox(height: 12),
                      // ---- demo「Settings List」：一张白卡 + 四行 ----
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: _SettingsCard(
                          rows: <Widget>[
                            _SettingRow(
                              icon: _RowIcon(
                                background: context.coupleTheme.primaryLight,
                                child: ClipRRect(
                                  borderRadius: BorderRadius.circular(8),
                                  child: Image.asset(
                                    context.coupleTheme.loveAnimAsset,
                                    width: 26,
                                    height: 26,
                                    fit: BoxFit.contain,
                                    errorBuilder: (_, _, _) => const Text('🎨', style: TextStyle(fontSize: 18)),
                                  ),
                                ),
                              ),
                              title: '情侣空间 · 主题工坊',
                              subtitle: '一二布布 / 线条小狗 / 噜噜噜妹',
                              trailing: _TrailingPill(
                                text: context.coupleTheme.tag,
                              ),
                              onTap: _openCoupleThemeModal,
                            ),
                            _SettingRow(
                              icon: _RowIcon(
                                background: const Color(0xFFFDF0D5),
                                child: _cozy3d('assets/images/shopping.png', 22, '🏪'),
                              ),
                              title: '我的店铺管理',
                              subtitle: '编辑自营小饭桌的菜品、单价与分类',
                              onTap: _openDishManage,
                            ),
                            _SettingRow(
                              icon: _RowIcon(
                                background: const Color(0xFFF0E9FB),
                                child: _cozy3d('assets/images/heart.png', 22, '🎂'),
                              ),
                              title: '纪念日日历',
                              subtitle: '恋爱情侣相守天数与里程碑提醒',
                              trailing: days == null
                                  ? null
                                  : _TrailingPill(text: '一起 $days 天'),
                              onTap: _openAnniversary,
                            ),
                            _SettingRow(
                              icon: _RowIcon(
                                background: const Color(0xFFFFE4EA),
                                child: const _CandyCoinIcon(size: 22),
                              ),
                              title: '糖糖币充值 / 撒糖中心',
                              subtitle: '饲养员可随时给对方补充额度',
                              trailing: state.isPaired
                                  ? _TrailingPill(text: '$balance 枚')
                                  : null,
                              onTap: _openCandyCoins,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 12),
                      // ---- demo 里没有、但原页面本来就有的入口 ----
                      // 订单记录 / 版本与更新 / 帮助与客服：套同一套设置行样式收在第二组，
                      // 功能一行不减（点菜页 / 订单页仍各有入口）。
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: _SettingsCard(
                          rows: <Widget>[
                            _SettingRow(
                              icon: _RowIcon(
                                background: CozyPalette.primaryContainer.withValues(alpha: 0.38),
                                child: const Icon(
                                  Icons.receipt_long_outlined,
                                  size: 20,
                                  color: CozyPalette.primary,
                                ),
                              ),
                              title: '订单记录',
                              subtitle: '你们一起点过的每一顿',
                              onTap: _openOrdersTab,
                            ),
                            _SettingRow(
                              icon: _RowIcon(
                                background: CozyPalette.primaryContainer.withValues(alpha: 0.38),
                                child: const Icon(
                                  Icons.info_outline,
                                  size: 20,
                                  color: CozyPalette.primary,
                                ),
                              ),
                              title: '版本与更新',
                              subtitle: '检查有没有新版本',
                              trailing: const _TrailingPill(text: _versionLabel),
                              onTap: _openVersionDialog,
                            ),
                            _SettingRow(
                              icon: _RowIcon(
                                background: CozyPalette.primaryContainer.withValues(alpha: 0.38),
                                child: const Icon(
                                  Icons.support_agent_outlined,
                                  size: 20,
                                  color: CozyPalette.primary,
                                ),
                              ),
                              title: '帮助与客服',
                              subtitle: '遇到问题找小饭桌管理员',
                              onTap: _openHelpDialog,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 18),
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
// demo「Profile Card」资料卡（横排：头像 + 昵称/签名 + 编辑入口 + 两个标签）
// ---------------------------------------------------------------------------

/// 3D 图标（`assets/images/*.png`）+ emoji 兜底：资源缺失时不留空白。
Widget _cozy3d(String asset, double size, String fallbackEmoji) => Image.asset(
      asset,
      width: size,
      height: size,
      filterQuality: FilterQuality.high,
      errorBuilder: (_, _, _) =>
          Text(fallbackEmoji, style: TextStyle(fontSize: size * 0.82)),
    );

class _ProfileCard extends StatelessWidget {
  const _ProfileCard({
    required this.nickname,
    required this.hasNickname,
    required this.avatarUrl,
    required this.roleKey,
    required this.roleText,
    required this.coins,
    required this.onTap,
  });

  final String nickname;
  final bool hasNickname;
  final String avatarUrl;
  final String roleKey;
  final String roleText;
  final int coins;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      behavior: HitTestBehavior.opaque,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
          ),
          boxShadow: CozyLight.cardShadow,
        ),
        child: Row(
          children: <Widget>[
            _avatar(),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: Text(
                          // 空态：位置留给引导文案，不编假昵称。
                          hasNickname ? nickname : '还没有设置昵称',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: _ts(
                            16,
                            24,
                            FontWeight.w900,
                            hasNickname
                                ? CozyPalette.onSurface
                                : CozyPalette.onSurfaceVariant,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      _editEntry(),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '今天也要一起好好吃饭~',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: _ts(12, 18, FontWeight.w400, CozyPalette.onSurfaceVariant),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    children: <Widget>[
                      _tag(
                        text: '当前身份: $roleText',
                        background: CozyPalette.primaryContainer,
                        foreground: CozyPalette.primary,
                        border: CozyPalette.primary.withValues(alpha: 0.18),
                      ),
                      _coinTag(),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 头像：暖金→樱花粉渐变环 + 白边 + 右下角身份角标（demo `w-16 h-16` + `w-5 h-5`）。
  Widget _avatar() {
    return SizedBox(
      width: 64,
      height: 64,
      child: Stack(
        clipBehavior: Clip.none,
        children: <Widget>[
          Container(
            width: 64,
            height: 64,
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: <Color>[Color(0xFFF2B23E), CozyPalette.primaryContainer],
              ),
              boxShadow: CozyLight.cardShadow,
            ),
            child: Padding(
              padding: const EdgeInsets.all(2),
              child: Container(
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(color: Colors.white, width: 2),
                ),
                child: ClipOval(
                  child: CozyAvatar(
                    url: avatarUrl,
                    size: 60,
                    fallback: const ColoredBox(
                      color: CozyPalette.surface,
                      child: Center(
                        child: Icon(Icons.pets, size: 28, color: CozyPalette.primary),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
          Positioned(bottom: 0, right: 0, child: _roleBadge()),
        ],
      ),
    );
  }

  /// 身份角标：饲养员=厨师帽 / 吃货=瓷碗 / 还没选身份=`?`（三种状态都不留空）。
  Widget _roleBadge() {
    final Widget mark = switch (roleKey) {
      'caretaker' => _cozy3d('assets/images/chef.png', 13, '🍳'),
      'eater' => _cozy3d('assets/images/bowl.png', 13, '🍚'),
      _ => const Text(
          '?',
          style: TextStyle(
            fontSize: 11,
            height: 1.1,
            fontWeight: FontWeight.w900,
            color: Colors.white,
          ),
        ),
    };
    return Container(
      width: 20,
      height: 20,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: const Color(0xFFF2B23E),
        border: Border.all(color: Colors.white, width: 1.4),
      ),
      child: mark,
    );
  }

  /// 「编辑资料」小按钮（demo `bg-cozy-bg` + `text-[10px]`）——
  /// 整个资料卡都可点，这里只是把入口显式画出来，空态也在。
  Widget _editEntry() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: CozyPalette.surfaceContainerLow,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          const Icon(Icons.edit_outlined, size: 12, color: CozyPalette.primary),
          const SizedBox(width: 3),
          Text('编辑资料', style: _ts(10, 14, FontWeight.w900, CozyPalette.primary)),
        ],
      ),
    );
  }

  /// 小标签（demo `rounded-md px-2 py-0.5`）。
  Widget _tag({
    required String text,
    required Color background,
    required Color foreground,
    Color? border,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: border ?? Colors.transparent),
      ),
      child: Text(text, style: _ts(11, 15, FontWeight.w700, foreground)),
    );
  }

  /// 糖糖币标签（demo 的 amber-50/200/700），沿用首页糖糖币条的琥珀色。
  Widget _coinTag() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFFCEBCF),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: const Color(0xFFF5D7A8)),
      ),
      child: CozyCountUp(
        value: coins,
        prefix: '糖糖币 ',
        suffix: ' 枚',
        style: _ts(11, 15, FontWeight.w900, const Color(0xFFC77A14)),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// 云端同步与账号登录/注册卡片
// ---------------------------------------------------------------------------

class _CloudAuthCard extends StatelessWidget {
  const _CloudAuthCard({
    required this.isGuest,
    required this.isLoggedIn,
    required this.email,
    required this.onLogin,
    required this.onRegister,
  });

  final bool isGuest;
  final bool isLoggedIn;
  final String email;
  final VoidCallback onLogin;
  final VoidCallback onRegister;

  @override
  Widget build(BuildContext context) {
    if (isLoggedIn) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: CozyPalette.primary.withValues(alpha: 0.18),
            width: 0.8,
          ),
          boxShadow: <BoxShadow>[
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.03),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: <Widget>[
            Container(
              width: 38,
              height: 38,
              decoration: const BoxDecoration(
                color: Color(0xFFE8F5E9),
                shape: BoxShape.circle,
              ),
              child: const Center(
                child: Icon(Icons.cloud_done_rounded, color: Color(0xFF2E7D32), size: 20),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  const Row(
                    children: <Widget>[
                      Text(
                        '云端实时同步已开启',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w800,
                          color: CozyPalette.onSurface,
                        ),
                      ),
                      SizedBox(width: 4),
                      Text('🟢', style: TextStyle(fontSize: 10)),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    email.isNotEmpty ? email : '数据已安全同步至云端小饭桌',
                    style: TextStyle(
                      fontSize: 11,
                      color: CozyPalette.onSurfaceVariant,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: onLogin,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: CozyPalette.surfaceContainerLow,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: CozyPalette.outlineVariant.withValues(alpha: 0.4),
                  ),
                ),
                child: const Text(
                  '切换账号',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: CozyPalette.primary,
                  ),
                ),
              ),
            ),
          ],
        ),
      );
    }

    // Guest / Not logged in banner
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: <Color>[
            Color(0xFFFFF7ED),
            Color(0xFFFFF1F2),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: CozyPalette.primary.withValues(alpha: 0.25),
          width: 1,
        ),
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: CozyPalette.primary.withValues(alpha: 0.05),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: CozyPalette.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Center(
                  child: Icon(
                    Icons.cloud_sync_rounded,
                    color: CozyPalette.primary,
                    size: 20,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      '登录云端小店 · 开启双人同步',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w900,
                        color: CozyPalette.onSurface,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      '跨设备实时同步菜单、订单与甜蜜纪念日',
                      style: TextStyle(
                        fontSize: 11,
                        color: CozyPalette.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: <Widget>[
              Expanded(
                child: GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: onLogin,
                  child: Container(
                    height: 38,
                    decoration: BoxDecoration(
                      color: CozyPalette.primary,
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: <BoxShadow>[
                        BoxShadow(
                          color: CozyPalette.primary.withValues(alpha: 0.28),
                          blurRadius: 6,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    alignment: Alignment.center,
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        Icon(Icons.login_rounded, size: 16, color: Colors.white),
                        SizedBox(width: 6),
                        Text(
                          '账号登录',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w800,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: onRegister,
                  child: Container(
                    height: 38,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: CozyPalette.primary.withValues(alpha: 0.45),
                        width: 1,
                      ),
                    ),
                    alignment: Alignment.center,
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        Icon(Icons.person_add_alt_1_rounded, size: 16, color: CozyPalette.primary),
                        SizedBox(width: 6),
                        Text(
                          '新店注册',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w800,
                            color: CozyPalette.primary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// demo「Couple Connection Tile」伴侣绑定卡（邀请码 + 复制）
// ---------------------------------------------------------------------------

class _PairTile extends StatelessWidget {
  const _PairTile({
    required this.isPaired,
    required this.isFullyBound,
    required this.inviteCode,
    required this.partnerName,
    required this.onTap,
  });

  final bool isPaired;
  final bool isFullyBound;
  final String inviteCode;
  final String partnerName;
  final VoidCallback onTap;

  String get _titleLine {
    if (isPaired) {
      return partnerName.isNotEmpty ? '伴侣小饭桌（已绑定 $partnerName）' : '伴侣小饭桌（已绑定）';
    }
    return '伴侣邀请码 (Pair ID)';
  }

  /// 邀请码行：已绑定时明确提示绑定成功，不再展示生成邀请码文案；未绑定时引导生成。
  String get _codeLine {
    if (isPaired) {
      return '双人小饭桌已成功绑定，数据实时同步 💕';
    }
    if (inviteCode.isEmpty) return '还没有邀请码 · 点右侧生成一个';
    return '#$inviteCode （等待对方输入）';
  }

  void _copy(BuildContext context) {
    if (isPaired) {
      onTap();
      return;
    }
    if (inviteCode.isEmpty) {
      onTap();
      return;
    }
    Clipboard.setData(ClipboardData(text: inviteCode));
    showCozyToast(context, '已复制伴侣邀请码');
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      behavior: HitTestBehavior.opaque,
      child: Container(
        padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
        decoration: BoxDecoration(
          // demo：`from-rose-50 to-pink-50` —— 极淡的一层粉压在纯白页面上。
          gradient: LinearGradient(
            begin: Alignment.centerLeft,
            end: Alignment.centerRight,
            colors: <Color>[
              CozyPalette.secondaryContainer.withValues(alpha: 0.42),
              CozyPalette.primaryContainer.withValues(alpha: 0.22),
            ],
          ),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: CozyPalette.secondaryContainer.withValues(alpha: 0.90),
          ),
        ),
        child: Row(
          children: <Widget>[
            Container(
              width: 40,
              height: 40,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(13),
                boxShadow: CozyLight.cardShadow,
              ),
              child: const Text('💑', style: TextStyle(fontSize: 18, height: 1.2)),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Text(
                    _titleLine,
                    style: _ts(12, 16, FontWeight.w900, CozyPalette.onSurface),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    _codeLine,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: _ts(11, 16, FontWeight.w900, CozyPalette.primary)
                        .copyWith(letterSpacing: 0.5),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            GestureDetector(
              onTap: () => _copy(context),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: isPaired ? CozyPalette.primary.withValues(alpha: 0.12) : Colors.white,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                    color: isPaired ? CozyPalette.primary.withValues(alpha: 0.3) : CozyPalette.secondaryContainer,
                  ),
                  boxShadow: isPaired ? null : CozyLight.cardShadow,
                ),
                child: Text(
                  isPaired ? '已绑定' : (inviteCode.isEmpty ? '去生成' : '复制'),
                  style: _ts(12, 16, FontWeight.w900, CozyPalette.primary),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// demo「Settings List」：一张白卡 + divide-y 行
// ---------------------------------------------------------------------------

class _SettingsCard extends StatelessWidget {
  const _SettingsCard({required this.rows});

  final List<Widget> rows;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
        ),
        boxShadow: CozyLight.cardShadow,
      ),
      child: Column(
        children: <Widget>[
          for (int i = 0; i < rows.length; i++) ...<Widget>[
            if (i > 0)
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 10),
                child: Divider(height: 1, thickness: 1, color: CozyLight.hairline),
              ),
            rows[i],
          ],
        ],
      ),
    );
  }
}

class _SettingRow extends StatelessWidget {
  const _SettingRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.trailing,
  });

  final Widget icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      behavior: HitTestBehavior.opaque,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 10),
        child: Row(
          children: <Widget>[
            icon,
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: _ts(12.5, 17, FontWeight.w900, CozyPalette.onSurface),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: _ts(10.5, 15, FontWeight.w400, CozyPalette.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            trailing ??
                const Icon(
                  Icons.chevron_right_rounded,
                  size: 18,
                  color: CozyPalette.onSurfaceVariant,
                ),
          ],
        ),
      ),
    );
  }
}

/// 行首 36×36 圆角图标位（demo `w-9 h-9 rounded-xl`）。
class _RowIcon extends StatelessWidget {
  const _RowIcon({required this.background, required this.child});

  final Color background;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 36,
      height: 36,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(12),
      ),
      child: child,
    );
  }
}

/// 行尾小胶囊：只装真实数值（相守天数 / 糖糖币 / 版本号）。
class _TrailingPill extends StatelessWidget {
  const _TrailingPill({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: CozyPalette.secondaryContainer.withValues(alpha: 0.72),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: CozyPalette.outlineVariant.withValues(alpha: 0.62),
        ),
      ),
      child: Text(text, style: _ts(12, 16, FontWeight.w900, CozyPalette.secondary)),
    );
  }
}

// ---------------------------------------------------------------------------
// 糖糖币余额大卡 / 155×155 巨卡 / 68 高单行列表（原 ProfileActionRow）已按 demo
// 结构移除：
//   · 余额 → 资料卡的「糖糖币 N 枚」标签 + 「糖糖币充值 / 撒糖中心」行的行尾胶囊
//   · 店铺 / 订单 / 编辑资料 / 邀请对方 → 收进上面的两组设置行（编辑资料进了资料卡）
// 余额与流水本身仍在 `CandyCoinsPage`（该页未改动）。
// ---------------------------------------------------------------------------


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

class _PresetAvatar {
  const _PresetAvatar({
    required this.name,
    required this.category,
    required this.assetPath,
    required this.roleHint,
    this.tag,
  });

  final String name;
  final String category;
  final String assetPath;
  final String roleHint;
  final String? tag;
}

const List<_PresetAvatar> _kPresetAvatars = <_PresetAvatar>[
  // 一二布布（布布大厨·男 / 一二吃货·女）
  _PresetAvatar(
    name: '布布 (官方大厨)',
    category: '一二布布',
    assetPath: 'asset:assets/images/avatars/theme_chef_bubu.png',
    roleHint: '👦 掌勺大厨推荐',
    tag: '布布',
  ),
  _PresetAvatar(
    name: '一二 (官方吃货)',
    category: '一二布布',
    assetPath: 'asset:assets/images/avatars/theme_eater_yier.png',
    roleHint: '👧 首席吃货推荐',
    tag: '一二',
  ),
  _PresetAvatar(
    name: '布布 (可爱经典)',
    category: '一二布布',
    assetPath: 'asset:assets/images/avatars/couple_bubu.jpg',
    roleHint: '👦 棕熊大厨',
    tag: '布布',
  ),
  _PresetAvatar(
    name: '一二 (萌萌经典)',
    category: '一二布布',
    assetPath: 'asset:assets/images/avatars/couple_yier.jpg',
    roleHint: '👧 白熊吃货',
    tag: '一二',
  ),

  // 小鸡毛 & 小白（小鸡毛金毛大厨·男 / 小白纯白吃货·女）
  _PresetAvatar(
    name: '小鸡毛 (官方大厨)',
    category: '小鸡毛&小白',
    assetPath: 'asset:assets/images/avatars/theme_chef_xiaojimao.png',
    roleHint: '🐶 金毛大厨推荐',
    tag: '小鸡毛',
  ),
  _PresetAvatar(
    name: '小白 (官方吃货)',
    category: '小鸡毛&小白',
    assetPath: 'asset:assets/images/avatars/theme_eater_xiaobai.png',
    roleHint: '🐩 纯白吃货推荐',
    tag: '小白',
  ),
  _PresetAvatar(
    name: '小鸡毛 (开心经典)',
    category: '小鸡毛&小白',
    assetPath: 'asset:assets/images/avatars/couple_xiaojimao.jpg',
    roleHint: '🐶 掌勺修勾',
    tag: '小鸡毛',
  ),
  _PresetAvatar(
    name: '小白 (甜心经典)',
    category: '小鸡毛&小白',
    assetPath: 'asset:assets/images/avatars/couple_xiaobai.jpg',
    roleHint: '🐩 傲娇吃货',
    tag: '小白',
  ),

  // 噜噜 & 噜妹（水豚噜噜大厨·男 / 水豚噜妹宝宝裙·女）
  _PresetAvatar(
    name: '噜噜 (官方大厨)',
    category: '噜噜&噜妹',
    assetPath: 'asset:assets/images/avatars/theme_chef_lulu.png',
    roleHint: '🍊 水豚大厨推荐',
    tag: '噜噜',
  ),
  _PresetAvatar(
    name: '噜妹 (宝宝衣服吃货)',
    category: '噜噜&噜妹',
    assetPath: 'asset:assets/images/avatars/theme_eater_lumei.png',
    roleHint: '🎀 宝宝小噜妹推荐',
    tag: '噜妹',
  ),
  _PresetAvatar(
    name: '噜噜 (抱抱经典)',
    category: '噜噜&噜妹',
    assetPath: 'asset:assets/images/avatars/lulu_2.jpg',
    roleHint: '🐷 噜噜大厨',
    tag: '噜噜',
  ),
  _PresetAvatar(
    name: '噜妹 (爱心经典)',
    category: '噜噜&噜妹',
    assetPath: 'asset:assets/images/avatars/lumei_2.jpg',
    roleHint: '🌸 噜妹吃货',
    tag: '噜妹',
  ),

  // 萌宠治愈
  _PresetAvatar(
    name: '奶茶喵 (男款)',
    category: '萌宠治愈',
    assetPath: 'asset:assets/images/avatars/cat_boy.jpg',
    roleHint: '🐱 喵大厨',
    tag: '小猫',
  ),
  _PresetAvatar(
    name: '奶茶喵 (女款)',
    category: '萌宠治愈',
    assetPath: 'asset:assets/images/avatars/cat_girl.jpg',
    roleHint: '🐱 喵吃货',
    tag: '小猫',
  ),
  _PresetAvatar(
    name: '呆萌柴 (男款)',
    category: '萌宠治愈',
    assetPath: 'asset:assets/images/avatars/shiba_boy.jpg',
    roleHint: '🐕 柴柴',
    tag: '柴犬',
  ),
  _PresetAvatar(
    name: '呆萌柴 (女款)',
    category: '萌宠治愈',
    assetPath: 'asset:assets/images/avatars/shiba_girl.jpg',
    roleHint: '🐕 柴柴',
    tag: '柴犬',
  ),
];

class _ProfileEditDialog extends StatefulWidget {
  const _ProfileEditDialog({required this.name, required this.avatarUrl});

  final String name;
  final String avatarUrl;

  @override
  State<_ProfileEditDialog> createState() => _ProfileEditDialogState();
}

class _ProfileEditDialogState extends State<_ProfileEditDialog> {
  late final TextEditingController _name = TextEditingController(text: widget.name);

  /// 当前生效的头像（打开时=云端值；选图后=刚选的那张 data URI / asset URI）
  late String _avatarUrl = widget.avatarUrl;
  String _selectedCategory = '全部';
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

  /// 相册选图 → 裁剪页
  Future<void> _pickAvatar() async {
    if (_locked) return;
    try {
      final XFile? picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        maxWidth: 2000,
        maxHeight: 2000,
        imageQuality: 88,
      );
      if (picked == null) return;
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

  /// 直接裁剪「当前这张」
  Future<void> _cropCurrent() async {
    if (_locked) return;
    final String url = _avatarUrl;
    if (!CozyAvatar.isDataUri(url)) {
      setState(() {
        _failed = false;
        _message = '先点「相册」选一张照片，再裁剪哦';
      });
      return;
    }
    final Uint8List? bytes = CozyAvatar.decodeDataUri(url);
    if (bytes == null) {
      setState(() {
        _failed = true;
        _message = '这张头像解不开，请重新「相册」选一张';
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
    if (result == null || result.isEmpty) return;
    final Uint8List cropped = result;
    setState(() {
      _avatarUrl = 'data:image/jpeg;base64,${base64Encode(cropped)}';
      _failed = false;
      _message = '已裁好新头像，点「保存资料」同步给小饭桌';
    });
  }

  /// 选中预设表情包头像
  void _selectPresetAvatar(_PresetAvatar preset) {
    if (_locked) return;
    setState(() {
      _avatarUrl = preset.assetPath;
      _failed = false;
      _message = '已选【${preset.name}】，点「保存资料」同步给伴侣';
    });
  }

  /// 删除头像：写空串，全 App 回退成小爪印
  void _removeAvatar() {
    setState(() {
      _avatarUrl = '';
      _failed = false;
      _message = '保存后展示温暖的小爪印 🐾';
    });
  }

  /// 恢复默认：换成随 App 打包的默认头像
  void _useDefaultAvatar() {
    setState(() {
      _avatarUrl = CozyAvatar.defaultAvatarMark;
      _failed = false;
      _message = '保存后使用小饭桌专属狗狗头像 🐶';
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
    showCozyToast(context, '资料已同步，伴侣端也能实时看到新头像 ✨', duration: const Duration(seconds: 3));
  }

  /// 头像快捷小药丸按钮
  Widget _quickAvatarButton({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
    Color? color,
  }) {
    final bool on = !_locked;
    final Color tint = color ?? CozyInk.rose;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: on ? onTap : null,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: tint.withValues(alpha: on ? 0.08 : 0.04),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: tint.withValues(alpha: on ? 0.28 : 0.12),
              width: 1,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Icon(icon, size: 13, color: tint),
              const SizedBox(width: 4),
              Text(
                label,
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: on ? tint : tint.withValues(alpha: 0.4),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final List<String> categories = <String>['全部', '一二布布', '小鸡毛&小白', '噜噜&噜妹', '萌宠治愈'];
    final List<_PresetAvatar> filteredAvatars = _selectedCategory == '全部'
        ? _kPresetAvatars
        : _kPresetAvatars.where((_PresetAvatar a) => a.category == _selectedCategory).toList();

    return Dialog(
      backgroundColor: const Color(0xFFFFFDF8),
      elevation: 12,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 440, maxHeight: 680),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              // 顶部标题栏
              Row(
                children: <Widget>[
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF1F2),
                      shape: BoxShape.circle,
                      border: Border.all(color: const Color(0xFFFFE4E6)),
                    ),
                    child: const Center(
                      child: Text('✨', style: TextStyle(fontSize: 16)),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        const Text(
                          '编辑个人资料',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w900,
                            color: Color(0xFF2C1810),
                            letterSpacing: -0.2,
                          ),
                        ),
                        Text(
                          '定制你在小饭桌里的专属形象与情头',
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w500,
                            color: const Color(0xFF8C7E74).withValues(alpha: 0.85),
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close_rounded, size: 20, color: Color(0xFF8C7E74)),
                    onPressed: _locked ? null : () => Navigator.of(context).pop(),
                    tooltip: '关闭',
                    splashRadius: 20,
                  ),
                ],
              ),
              const SizedBox(height: 14),

              Expanded(
                child: SingleChildScrollView(
                  physics: const BouncingScrollPhysics(),
                  child: Column(
                    children: <Widget>[
                      // 1. 头像大预览区
                      Center(
                        child: SizedBox(
                          width: 104,
                          height: 104,
                          child: Stack(
                            children: <Widget>[
                              Positioned.fill(
                                child: Container(
                                  padding: const EdgeInsets.all(3.5),
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    gradient: const LinearGradient(
                                      colors: <Color>[Color(0xFFFDA4AF), Color(0xFFFDE68A), Color(0xFFF43F5E)],
                                      begin: Alignment.topLeft,
                                      end: Alignment.bottomRight,
                                    ),
                                    boxShadow: <BoxShadow>[
                                      BoxShadow(
                                        color: const Color(0xFFF43F5E).withValues(alpha: 0.22),
                                        blurRadius: 14,
                                        offset: const Offset(0, 4),
                                      ),
                                    ],
                                  ),
                                  child: Container(
                                    decoration: const BoxDecoration(
                                      shape: BoxShape.circle,
                                      color: Colors.white,
                                    ),
                                    padding: const EdgeInsets.all(2),
                                    child: ClipOval(
                                      child: CozyAvatar(
                                        url: _avatarUrl,
                                        size: 96,
                                        fallback: const Icon(Icons.pets, size: 42, color: Color(0xFFF43F5E)),
                                      ),
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
                                    width: 34,
                                    height: 34,
                                    alignment: Alignment.center,
                                    decoration: BoxDecoration(
                                      shape: BoxShape.circle,
                                      gradient: const LinearGradient(
                                        colors: <Color>[Color(0xFFFB7185), Color(0xFFE11D48)],
                                      ),
                                      border: Border.all(color: Colors.white, width: 2.5),
                                      boxShadow: <BoxShadow>[
                                        BoxShadow(
                                          color: Colors.black.withValues(alpha: 0.15),
                                          blurRadius: 6,
                                          offset: const Offset(0, 2),
                                        ),
                                      ],
                                    ),
                                    child: const Icon(Icons.camera_alt_rounded, size: 16, color: Colors.white),
                                  ),
                                ),
                              ),
                              if (_picking || _saving)
                                Positioned.fill(
                                  child: ClipOval(
                                    child: Container(
                                      color: Colors.black.withValues(alpha: 0.35),
                                      child: const Center(
                                        child: SizedBox(
                                          width: 24,
                                          height: 24,
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2.5,
                                            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                                          ),
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 10),

                      // 头像快捷操作按钮栏
                      Wrap(
                        spacing: 8,
                        runSpacing: 6,
                        alignment: WrapAlignment.center,
                        children: <Widget>[
                          _quickAvatarButton(
                            icon: Icons.photo_library_rounded,
                            label: '相册选图',
                            onTap: _pickAvatar,
                            color: const Color(0xFFE11D48),
                          ),
                          _quickAvatarButton(
                            icon: Icons.crop_rounded,
                            label: '重新裁剪',
                            onTap: _cropCurrent,
                            color: const Color(0xFFD97706),
                          ),
                          _quickAvatarButton(
                            icon: Icons.favorite_rounded,
                            label: '默认狗狗',
                            onTap: _useDefaultAvatar,
                            color: const Color(0xFF059669),
                          ),
                          _quickAvatarButton(
                            icon: Icons.delete_outline_rounded,
                            label: '清除头像',
                            onTap: _removeAvatar,
                            color: const Color(0xFF6B7280),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),

                      // 2. 专属情侣表情包头像选择库
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFDF7F2),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: const Color(0xFFF3E8E2)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Row(
                              children: <Widget>[
                                const Text('💖', style: TextStyle(fontSize: 14)),
                                const SizedBox(width: 6),
                                const Text(
                                  '情侣专属表情包头像库',
                                  style: TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w900,
                                    color: Color(0xFF2C1810),
                                  ),
                                ),
                                const Spacer(),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFFFE4E6),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: const Text(
                                    '一键换上',
                                    style: TextStyle(
                                      fontSize: 10,
                                      fontWeight: FontWeight.w700,
                                      color: Color(0xFFE11D48),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),

                            // 分类切换 Pill
                            SingleChildScrollView(
                              scrollDirection: Axis.horizontal,
                              physics: const BouncingScrollPhysics(),
                              child: Row(
                                children: categories.map((String cat) {
                                  final bool active = _selectedCategory == cat;
                                  return GestureDetector(
                                    onTap: () => setState(() => _selectedCategory = cat),
                                    child: AnimatedContainer(
                                      duration: const Duration(milliseconds: 200),
                                      margin: const EdgeInsets.only(right: 6),
                                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                      decoration: BoxDecoration(
                                        color: active ? const Color(0xFFE11D48) : Colors.white,
                                        borderRadius: BorderRadius.circular(12),
                                        border: Border.all(
                                          color: active ? const Color(0xFFE11D48) : const Color(0xFFE5E7EB),
                                        ),
                                        boxShadow: active
                                            ? <BoxShadow>[
                                                BoxShadow(
                                                  color: const Color(0xFFE11D48).withValues(alpha: 0.25),
                                                  blurRadius: 4,
                                                  offset: const Offset(0, 2),
                                                ),
                                              ]
                                            : null,
                                      ),
                                      child: Text(
                                        cat,
                                        style: TextStyle(
                                          fontSize: 11,
                                          fontWeight: active ? FontWeight.w800 : FontWeight.w600,
                                          color: active ? Colors.white : const Color(0xFF6B7280),
                                        ),
                                      ),
                                    ),
                                  );
                                }).toList(),
                              ),
                            ),
                            const SizedBox(height: 10),

                            // 头像网格
                            GridView.builder(
                              shrinkWrap: true,
                              physics: const NeverScrollableScrollPhysics(),
                              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 4,
                                crossAxisSpacing: 8,
                                mainAxisSpacing: 8,
                                childAspectRatio: 0.76,
                              ),
                              itemCount: filteredAvatars.length,
                              itemBuilder: (BuildContext context, int index) {
                                final _PresetAvatar avatar = filteredAvatars[index];
                                final bool isSelected = _avatarUrl == avatar.assetPath;
                                return GestureDetector(
                                  onTap: () => _selectPresetAvatar(avatar),
                                  child: AnimatedContainer(
                                    duration: const Duration(milliseconds: 200),
                                    decoration: BoxDecoration(
                                      color: Colors.white,
                                      borderRadius: BorderRadius.circular(14),
                                      border: Border.all(
                                        color: isSelected ? const Color(0xFFE11D48) : const Color(0xFFE5E7EB),
                                        width: isSelected ? 2.2 : 1,
                                      ),
                                      boxShadow: isSelected
                                          ? <BoxShadow>[
                                              BoxShadow(
                                                color: const Color(0xFFE11D48).withValues(alpha: 0.22),
                                                blurRadius: 6,
                                                offset: const Offset(0, 2),
                                              ),
                                            ]
                                          : <BoxShadow>[
                                              BoxShadow(
                                                color: Colors.black.withValues(alpha: 0.03),
                                                blurRadius: 3,
                                                offset: const Offset(0, 1),
                                              ),
                                            ],
                                    ),
                                    padding: const EdgeInsets.all(4),
                                    child: Column(
                                      mainAxisAlignment: MainAxisAlignment.center,
                                      children: <Widget>[
                                        Expanded(
                                          child: Stack(
                                            children: <Widget>[
                                              ClipRRect(
                                                borderRadius: BorderRadius.circular(10),
                                                child: Image.asset(
                                                  avatar.assetPath.substring('asset:'.length),
                                                  width: double.infinity,
                                                  height: double.infinity,
                                                  fit: BoxFit.cover,
                                                  errorBuilder: (_, _, _) => const Center(
                                                    child: Icon(Icons.pets, size: 20, color: Color(0xFFFDA4AF)),
                                                  ),
                                                ),
                                              ),
                                              if (isSelected)
                                                Positioned(
                                                  right: 2,
                                                  top: 2,
                                                  child: Container(
                                                    padding: const EdgeInsets.all(2),
                                                    decoration: const BoxDecoration(
                                                      color: Color(0xFFE11D48),
                                                      shape: BoxShape.circle,
                                                    ),
                                                    child: const Icon(Icons.check, size: 10, color: Colors.white),
                                                  ),
                                                ),
                                            ],
                                          ),
                                        ),
                                        const SizedBox(height: 3),
                                        Text(
                                          avatar.name,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          textAlign: TextAlign.center,
                                          style: TextStyle(
                                            fontSize: 9.5,
                                            fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
                                            color: isSelected ? const Color(0xFFE11D48) : const Color(0xFF374151),
                                          ),
                                        ),
                                        Text(
                                          avatar.roleHint,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          textAlign: TextAlign.center,
                                          style: TextStyle(
                                            fontSize: 8.5,
                                            fontWeight: FontWeight.w500,
                                            color: isSelected
                                                ? const Color(0xFFE11D48).withValues(alpha: 0.85)
                                                : const Color(0xFF9CA3AF),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                );
                              },
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // 3. 昵称输入框
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: const Color(0xFFE5E7EB)),
                          boxShadow: <BoxShadow>[
                            BoxShadow(
                              color: Colors.black.withValues(alpha: 0.02),
                              blurRadius: 4,
                              offset: const Offset(0, 1),
                            ),
                          ],
                        ),
                        child: Row(
                          children: <Widget>[
                            const Icon(Icons.edit_note_rounded, size: 22, color: Color(0xFFE11D48)),
                            const SizedBox(width: 8),
                            Expanded(
                              child: TextField(
                                controller: _name,
                                onChanged: (_) => setState(() => _message = null),
                                style: const TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.w700,
                                  color: Color(0xFF2C1810),
                                ),
                                decoration: const InputDecoration(
                                  border: InputBorder.none,
                                  hintText: '输入你的小饭桌昵称...',
                                  hintStyle: TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w400,
                                    color: Color(0xFF9CA3AF),
                                  ),
                                  isDense: true,
                                  contentPadding: EdgeInsets.symmetric(vertical: 10),
                                ),
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: const Color(0xFFF3F4F6),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Text(
                                '${_trimmed.length}/12',
                                style: const TextStyle(
                                  fontSize: 10.5,
                                  fontWeight: FontWeight.w600,
                                  color: Color(0xFF6B7280),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),

                      // 提示槽：展示当前选中或错误
                      Container(
                        height: 32,
                        alignment: Alignment.center,
                        child: Text(
                          _nameError ?? _message ?? '支持相册自选裁剪，或一键切换可爱表情包情侣头像',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w500,
                            color: _nameError != null || _failed
                                ? const Color(0xFFEF4444)
                                : const Color(0xFF8C7E74),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 8),
              // 4. 底部操作按钮
              Row(
                children: <Widget>[
                  Expanded(
                    flex: 2,
                    child: OutlinedButton(
                      onPressed: _locked ? null : () => Navigator.of(context).pop(),
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        side: const BorderSide(color: Color(0xFFE5E7EB)),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      ),
                      child: const Text(
                        '取消',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFF6B7280),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    flex: 3,
                    child: Container(
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: <Color>[Color(0xFFFB7185), Color(0xFFE11D48)],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: BorderRadius.circular(16),
                        boxShadow: <BoxShadow>[
                          BoxShadow(
                            color: const Color(0xFFE11D48).withValues(alpha: 0.32),
                            blurRadius: 8,
                            offset: const Offset(0, 3),
                          ),
                        ],
                      ),
                      child: ElevatedButton(
                        onPressed: _nameError == null && !_locked ? _save : null,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.transparent,
                          shadowColor: Colors.transparent,
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                        ),
                        child: _saving
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2.2,
                                  valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                                ),
                              )
                            : const Text(
                                '保存资料',
                                style: TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.w800,
                                  color: Colors.white,
                                  letterSpacing: 0.5,
                                ),
                              ),
                      ),
                    ),
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
      // 绑定成功：撒一次花再关弹层（这一下是全 App 最值得庆祝的时刻）
      unawaited(showCozyConfetti(context));
      Navigator.of(context).pop();
    }
  }

  void _copyCode(String code) {
    if (code.isEmpty) return;
    Clipboard.setData(ClipboardData(text: code));
    showCozyToast(context, '已复制邀请码');
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
            showCozyToast(context, '解除绑定需要 AppState.unpair（尚未接入）', error: true);
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

/// 🎨 【情侣空间 · 主题工坊】底部选择抽屉
class _CoupleThemeModal extends StatefulWidget {
  const _CoupleThemeModal();

  @override
  State<_CoupleThemeModal> createState() => _CoupleThemeModalState();
}

class _CoupleThemeModalState extends State<_CoupleThemeModal> {
  late CoupleTheme _selectedTheme;

  @override
  void initState() {
    super.initState();
    _selectedTheme = CoupleThemeManager.instance.currentTheme;
  }

  void _onSelect(CoupleTheme theme) {
    HapticFeedback.mediumImpact();
    setState(() => _selectedTheme = theme);
  }

  Future<void> _applyTheme() async {
    HapticFeedback.heavyImpact();
    await CoupleThemeManager.instance.setTheme(_selectedTheme);
    if (!mounted) return;
    Navigator.pop(context);
    final spec = CoupleThemeSpec.fromTheme(_selectedTheme);
    showCozyToast(context, '🎉 已换上【${spec.title}】专属情侣主题 ✨');
  }

  @override
  Widget build(BuildContext context) {
    final currentSpec = CoupleThemeSpec.fromTheme(_selectedTheme);

    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
        boxShadow: [
          BoxShadow(
            color: Colors.black12,
            blurRadius: 24,
            offset: Offset(0, -4),
          ),
        ],
      ),
      padding: EdgeInsets.fromLTRB(
        24,
        16,
        24,
        MediaQuery.paddingOf(context).bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 拖拽把手
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.black12,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 标题栏
          Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: currentSpec.primaryLight,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Center(
                  child: Text('🎨', style: TextStyle(fontSize: 20)),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '情侣空间 · 主题工坊',
                      style: _ts(17, 22, FontWeight.w900, const Color(0xFF1F2937)),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '一键切换专属情侣表情包与甜蜜氛围',
                      style: _ts(11, 15, FontWeight.w500, const Color(0xFF6B7280)),
                    ),
                  ],
                ),
              ),
              IconButton(
                icon: const Icon(Icons.close, size: 20, color: Color(0xFF9CA3AF)),
                onPressed: () => Navigator.pop(context),
              ),
            ],
          ),
          const SizedBox(height: 20),

          // 3 大情侣主题卡片列表
          ...CoupleTheme.values.map((theme) {
            final spec = CoupleThemeSpec.fromTheme(theme);
            final isSelected = _selectedTheme == theme;

            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: GestureDetector(
                onTap: () => _onSelect(theme),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 250),
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: isSelected ? spec.primaryLight.withValues(alpha: 0.5) : Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: isSelected ? spec.primary : const Color(0xFFF3F4F6),
                      width: isSelected ? 2 : 1,
                    ),
                    boxShadow: isSelected
                        ? [
                            BoxShadow(
                              color: spec.shadowColor,
                              blurRadius: 16,
                              offset: const Offset(0, 4),
                            ),
                          ]
                        : null,
                  ),
                  child: Row(
                    children: [
                      // 主题动图徽章
                      Container(
                        width: 50,
                        height: 50,
                        decoration: BoxDecoration(
                          color: spec.primaryLight,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: spec.cardBorder, width: 1.2),
                          boxShadow: [
                            BoxShadow(
                              color: spec.shadowColor,
                              blurRadius: 8,
                              offset: const Offset(0, 2),
                            ),
                          ],
                        ),
                        padding: const EdgeInsets.all(2),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(13),
                          child: Image.asset(
                            spec.duoAnimationAsset,
                            fit: BoxFit.contain,
                            errorBuilder: (_, _, _) => Center(
                              child: Text(spec.emoji, style: const TextStyle(fontSize: 24)),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 14),

                      // 主题信息
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Text(
                                  spec.title,
                                  style: _ts(15, 20, FontWeight.w900, const Color(0xFF1F2937)),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: spec.primaryLight,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    spec.tag,
                                    style: _ts(10, 12, FontWeight.w800, spec.primaryDark),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 4),
                            Text(
                              spec.subtitle,
                              style: _ts(11, 15, FontWeight.w500, const Color(0xFF6B7280)),
                            ),
                          ],
                        ),
                      ),

                      // 选中勾选指示
                      if (isSelected)
                        Container(
                          width: 22,
                          height: 22,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: spec.primary,
                          ),
                          child: const Icon(Icons.check, size: 14, color: Colors.white),
                        )
                      else
                        Container(
                          width: 22,
                          height: 22,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(color: const Color(0xFFD1D5DB)),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            );
          }),

          const SizedBox(height: 12),

          // 确认应用按钮
          GestureDetector(
            onTap: _applyTheme,
            child: Container(
              height: 48,
              decoration: BoxDecoration(
                gradient: currentSpec.primaryGradient,
                borderRadius: BorderRadius.circular(24),
                boxShadow: [
                  BoxShadow(
                    color: currentSpec.shadowColor,
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Center(
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text('✨', style: TextStyle(fontSize: 14)),
                    const SizedBox(width: 6),
                    Text(
                      '应用【${currentSpec.title}】情侣主题',
                      style: _ts(14, 18, FontWeight.w900, Colors.white),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
