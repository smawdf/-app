import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';

import '../../data/app_state.dart';
import '../candy/candy_coins_page.dart';
import '../couple/anniversary_page.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';
import '../widgets/cozy_celebration.dart';
import '../widgets/cozy_count_up.dart';
import '../widgets/cozy_toast.dart';
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

  /// demo「纪念日日历」设置项：原页面没有这个入口（只在首页有），按 demo 补上。
  void _openAnniversary() {
    HapticFeedback.lightImpact();
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const AnniversaryPage()),
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
                      // ---- demo「Couple Connection Tile」（本页原先没有这一块）----
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: _PairTile(
                          isPaired: state.isPaired,
                          isFullyBound: state.pair?.isFullyBound ?? false,
                          inviteCode: inviteCode,
                          onTap: _openPairDialog,
                        ),
                      ),
                      const SizedBox(height: 12),
                      // ---- demo「Settings List」：一张白卡 + 三行 ----
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: _SettingsCard(
                          rows: <Widget>[
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
// demo「Couple Connection Tile」伴侣绑定卡（邀请码 + 复制）
// ---------------------------------------------------------------------------

class _PairTile extends StatelessWidget {
  const _PairTile({
    required this.isPaired,
    required this.isFullyBound,
    required this.inviteCode,
    required this.onTap,
  });

  final bool isPaired;
  final bool isFullyBound;
  final String inviteCode;
  final VoidCallback onTap;

  /// 邀请码行：有码显示码 + 真实绑定状态，没有码就是引导文案（块不消失）。
  String get _codeLine {
    if (inviteCode.isEmpty) return '还没有邀请码 · 点右侧生成一个';
    if (!isPaired) return '#$inviteCode （等待绑定）';
    return isFullyBound ? '#$inviteCode （双人绑定成功）' : '#$inviteCode （等待对方输入）';
  }

  void _copy(BuildContext context) {
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
                    '伴侣邀请码 (Pair ID)',
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
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: CozyPalette.secondaryContainer),
                  boxShadow: CozyLight.cardShadow,
                ),
                child: Text(
                  inviteCode.isEmpty ? '去生成' : '复制',
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
    showCozyToast(context, '资料已同步，伴侣那边也能看到新头像', duration: const Duration(seconds: 3));
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
