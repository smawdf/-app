import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../candy/candy_coins_page.dart';
import '../couple/anniversary_page.dart';
import '../menu/menu_management_page.dart';
import '../theme/couple_theme.dart';
import '../theme/cozy_glass.dart';
import '../widgets/cozy_dish_photo.dart';
import '../widgets/cozy_toast.dart';

/// 首页 —— 「我们的小饭桌」。
///
/// 这个 App 的本质不是「餐馆点单」，而是两个人共用的一张小饭桌：
/// 饲养员与吃货、糖糖币、一起吃饭的 N 天、纪念日、伴侣是否已经入座。
/// 所以这一版的首页把**关系**放成主角：
///
///   1. 顶部一句话标题（今天也要一起好好吃饭）
///   2. 小饭桌主卡：两个座位（头像/角色）+ 爱心 + 一起吃饭 N 天 + 纪念日入口
///   3. 糖糖币余额 / 最近一餐（关系进行中的两个数字）
///   4. 我的店铺、去点菜（降级为两个小入口）
///   5. 饲养员 / 吃货 身份切换
///
/// 功能一行不减：原先「纪念日」长卡进主卡、店铺与点菜改成小入口，
/// 其余入口与下拉刷新、身份切换行为保持不变。
class HomePage extends StatefulWidget {
  final ValueChanged<int> onNavigateTab;

  const HomePage({super.key, required this.onNavigateTab});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _quoteIndex = 0;
  DateTime? _anniversaryDate;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _bootstrap());
  }

  Future<void> _bootstrap() async {
    final state = AppState.instance;
    await state.loadMe();
    await state.refreshOrders(silent: true);
    final Map<String, dynamic>? info = await state.loadAnniversary();
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

  Future<void> _switchRole(String newRole) async {
    final state = AppState.instance;
    if (state.user == null) return;
    if (state.user!.role == newRole) return;

    HapticFeedback.mediumImpact();
    final ok = await state.updateRole(newRole);
    if (!mounted) return;

    showCozyToast(
      context,
      ok ? '已切换身份为【${state.user?.roleLabel ?? newRole}】' : '身份切换失败，请检查网络',
      error: !ok,
    );
  }

  void _openAnniversary() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const AnniversaryPage()),
    );
  }

  void _openShop() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const MenuManagementPage()),
    );
  }

  void _openCoins() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const CandyCoinsPage()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    // 【性能 Phase 1】首页读 user/pair/糖币/订单：订阅 profile+orders+candy 域，
    // 菜单变动（加菜/改价）不再让整页重建。
    return ListenableBuilder(
      listenable: Listenable.merge([
        state.listenFor(const {Domain.profile, Domain.orders, Domain.candy}),
        CoupleThemeManager.instance,
      ]),
      builder: (context, _) {
        final AppUser? user = state.user;
        final CouplePair? pair = state.pair;
        final bool isPaired = state.isPaired;
        final CoupleThemeSpec theme = context.coupleTheme;

        return DecoratedBox(
          decoration: BoxDecoration(gradient: theme.pageGradient),
          child: RefreshIndicator(
            color: theme.primary,
            backgroundColor: Colors.white,
            onRefresh: _bootstrap,
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.only(bottom: 120),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildTopBar(theme),

                  const SizedBox(height: 12),

                  // 1. 主角：我们的小饭桌（含相守天数胶囊 / 专属双人动画场景 / 角色切换栏）
                  _buildTableHero(
                    user: user,
                    pair: pair,
                    isPaired: isPaired,
                    theme: theme,
                  ),

                  const SizedBox(height: 14),

                  // 2. 今日主厨进行中（没有进行中的订单时自动收起，不留空壳）
                  ..._buildLiveOrderBlock(state),

                  // 3. 三张快捷卡：我的菜单 / 小店管理 / 纪念日（植入专属主题动态 GIF）
                  _buildQuickActions(isCaretaker: state.isCaretaker, theme: theme),

                  const SizedBox(height: 14),

                  // 4. 糖糖币储备横条（含「投喂对方」）
                  _buildCoinsBar(state: state, isPaired: isPaired, theme: theme),

                  const SizedBox(height: 18),

                  // 5. 饲养员私房招牌（店内菜品 Top 2，带专属大厨动态小吉祥物）
                  ..._buildChefPicks(state, theme),

                  const SizedBox(height: 16),

                  // 6. 专属情侣陪伴挂件（动态吉祥物 + 轮播专属情侣情话）
                  _buildThemeMascotBanner(theme),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  /// 顶部：左「OUR KITCHEN / 双人小饭桌」标签块 + 大标题 + 右铃铛
  Widget _buildTopBar(CoupleThemeSpec theme) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 6, 20, 0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: theme.primaryLight,
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(
                        'OUR KITCHEN',
                        style: TextStyle(
                          fontSize: 10,
                          height: 1.3,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.0,
                          color: theme.primary,
                        ),
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      '双人小饭桌 · ${theme.title}',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: theme.primary,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    const Flexible(
                      child: Text(
                        '今天也要一起好好吃饭',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 21,
                          height: 1.25,
                          fontWeight: FontWeight.w900,
                          color: CozyPalette.onSurface,
                          letterSpacing: -0.5,
                        ),
                      ),
                    ),
                    const SizedBox(width: 5),
                    Image.asset(
                      'assets/images/cooking.png',
                      width: 20,
                      height: 20,
                      filterQuality: FilterQuality.high,
                      errorBuilder: (context, error, stackTrace) =>
                          const Text('🍳', style: TextStyle(fontSize: 16)),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          GestureDetector(
            onTap: _openCoins,
            child: Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                color: CozyPalette.surface,
                shape: BoxShape.circle,
                border: Border.all(color: CozyPalette.outlineVariant),
                boxShadow: CozyLight.cardShadow,
              ),
              child: const Icon(
                Icons.notifications_none_rounded,
                color: CozyPalette.secondary,
                size: 20,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 小饭桌主卡：两个座位 + 一起吃饭 N 天 + 专属双人动画场景 + 角色切换  /// 小饭桌主卡：两个座位 + 一起吃饭 N 天 + 专属双人动画场景 + 角色切换
  Widget _buildTableHero({
    required AppUser? user,
    required CouplePair? pair,
    required bool isPaired,
    required CoupleThemeSpec theme,
  }) {
    final bool isCaretaker = user?.isCaretaker ?? true;
    final String defaultSelfName = isCaretaker ? theme.defaultChefName : theme.defaultEaterName;
    final String defaultPartnerName = isCaretaker ? theme.defaultEaterName : theme.defaultChefName;

    final String myName = (user?.nickname.isNotEmpty ?? false) ? user!.nickname : defaultSelfName;
    final String myRole = user?.roleLabel ?? (isCaretaker ? theme.chefTag : theme.eaterTag);

    // 伴侣昵称来自 `CouplePair.partnerName`
    final String rawPartnerName = pair?.partnerName.trim() ?? '';
    final String partnerName = !isPaired
        ? "等$defaultPartnerName入座"
        : (rawPartnerName.isNotEmpty ? rawPartnerName : defaultPartnerName);
    final String partnerRole = !isPaired
        ? "座位空着"
        : (isCaretaker ? theme.eaterTag : theme.chefTag);

    final String myFallbackAvatar = isCaretaker ? theme.defaultChefAvatar : theme.defaultEaterAvatar;
    final String partnerFallbackAvatar = isCaretaker ? theme.defaultEaterAvatar : theme.defaultChefAvatar;

    final String rawMyAvatar = user?.avatarUrl ?? '';
    final bool hasCustomMyAvatar = rawMyAvatar.isNotEmpty && !rawMyAvatar.startsWith('assets/');
    final String rawPartnerAvatar = pair?.partnerAvatarUrl ?? '';
    final bool hasCustomPartnerAvatar = rawPartnerAvatar.isNotEmpty && !rawPartnerAvatar.startsWith('assets/');

    final String myAvatarToUse = hasCustomMyAvatar ? rawMyAvatar : myFallbackAvatar;
    final String partnerAvatarToUse = hasCustomPartnerAvatar ? rawPartnerAvatar : partnerFallbackAvatar;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            theme.cardBg,
            theme.primaryLight.withValues(alpha: 0.55),
            theme.accentLight.withValues(alpha: 0.35),
          ],
        ),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: theme.cardBorder, width: 1.3),
        boxShadow: [
          BoxShadow(
            color: theme.shadowColor.withValues(alpha: 0.08),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: Stack(
          alignment: Alignment.center,
          children: [
            // 爪印水印只当纸纹
            Positioned(
              right: -30,
              bottom: -20,
              child: Icon(
                Icons.pets,
                size: 200,
                color: theme.primary.withValues(alpha: 0.05),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 14, 18, 12),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // 顶部行：相守天数胶囊 + 右侧「纪念相册 ›」（未配对时给状态 chip）
                  Row(
                    children: [
                      _buildDaysPill(isPaired: isPaired, theme: theme),
                      const Spacer(),
                      if (isPaired)
                        _buildAlbumLink()
                      else
                        _buildStatusChip(isPaired),
                    ],
                  ),
                  const SizedBox(height: 12),

                  // 专属双人情侣动画互动卡：对应每一个主题特定的双人动态
                  _buildDuoAnimationScene(theme),
                  const SizedBox(height: 14),

                  // 两个座位 + 中间跳动的心
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      _buildSeat(
                        avatarUrl: myAvatarToUse,
                        fallbackAsset: myFallbackAvatar,
                        name: myName,
                        tagText: myRole,
                        highlight: true,
                      ),
                      Container(
                        width: 42,
                        height: 42,
                        decoration: BoxDecoration(
                          color: theme.primaryLight.withValues(alpha: 0.6),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(Icons.favorite,
                                color: theme.primary, size: 20)
                            .animate(onPlay: (c) => c.repeat(reverse: true))
                            .scale(
                                duration: 1200.ms,
                                begin: const Offset(0.9, 0.9),
                                end: const Offset(1.15, 1.15)),
                      ),
                      _buildSeat(
                        avatarUrl: partnerAvatarToUse,
                        fallbackAsset: partnerFallbackAvatar,
                        fallbackText: isPaired ? "伴" : "+",
                        name: partnerName,
                        tagText: partnerRole,
                        highlight: isPaired,
                        onTap: isPaired ? null : () => widget.onNavigateTab(4),
                      ),
                    ],
                  ),
                  // 角色切换栏：仅在未配对时允许快速调试切换；已绑定情侣后锁定身份，禁止切换
                  if (!isPaired) ...[
                    const SizedBox(height: 14),
                    Container(height: 1, color: theme.cardBorder.withValues(alpha: 0.5)),
                    const SizedBox(height: 6),
                    _buildRoleSwitchBar(user: user),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 专属情侣双人动画场景：展示每个主题专属的双人动态与对话气泡
  Widget _buildDuoAnimationScene(CoupleThemeSpec theme) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        final quotes = theme.dialogQuotes;
        if (quotes.isNotEmpty) {
          _quoteIndex = (_quoteIndex + 1) % quotes.length;
          showCozyToast(context, quotes[_quoteIndex]);
        }
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: theme.primaryLight.withValues(alpha: 0.55),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: theme.cardBorder.withValues(alpha: 0.7),
            width: 1.2,
          ),
          boxShadow: [
            BoxShadow(
              color: theme.shadowColor.withValues(alpha: 0.05),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Row(
          children: [
            // 专属双人情侣动态 GIF（如一二布布吃饼干/线条小狗吸拉面/水豚噜噜牵手）
            ClipRRect(
              borderRadius: BorderRadius.circular(14),
              child: Container(
                width: 68,
                height: 68,
                color: Colors.white.withValues(alpha: 0.85),
                padding: const EdgeInsets.all(4),
                child: Image.asset(
                  theme.duoAnimationAsset,
                  fit: BoxFit.contain,
                  filterQuality: FilterQuality.medium,
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Text(
                        '${theme.title}专属双人小剧场',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w900,
                          color: theme.primary,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '✨ 点我互动',
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.w500,
                          color: theme.textSub,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    theme.duoAnimationSubtitle,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 11,
                      height: 1.35,
                      fontWeight: FontWeight.w600,
                      color: theme.textMain,
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

  /// demo 顶部「❤️ 恋爱相守 N 天」胶囊。
  Widget _buildDaysPill({required bool isPaired, CoupleThemeSpec? theme}) {
    final int? days = _daysTogether;
    final Color primary = theme?.primary ?? CozyPalette.primary;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
      decoration: BoxDecoration(
        color: CozyPalette.surface.withValues(alpha: 0.92),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: (theme?.cardBorder ?? CozyPalette.outlineVariant).withValues(alpha: 0.60),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.favorite, size: 12, color: primary),
          const SizedBox(width: 5),
          Text(
            '恋爱相守',
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              color: primary,
            ),
          ),
          const SizedBox(width: 4),
          Text(
            isPaired && days != null ? '$days' : '—',
            style: TextStyle(
              fontSize: 14,
              height: 1.1,
              fontWeight: FontWeight.w900,
              color: primary,
            ),
          ),
          const SizedBox(width: 2),
          Text(
            '天',
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              color: primary,
            ),
          ),
        ],
      ),
    );
  }

  /// demo 顶部右侧「纪念相册 ›」。
  Widget _buildAlbumLink() {
    return GestureDetector(
      onTap: _openAnniversary,
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            '纪念相册',
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: CozyPalette.onSurfaceVariant,
            ),
          ),
          Icon(Icons.chevron_right_rounded,
              size: 15, color: CozyPalette.onSurfaceVariant),
        ],
      ),
    );
  }

  /// demo `Role Quick Switch Bar`：一行「点击身份可切换体验角色 + 按钮」。
  Widget _buildRoleSwitchBar({required AppUser? user}) {
    final bool isCaretaker = user?.isCaretaker ?? false;
    return Row(
      children: [
        const Expanded(
          child: Text(
            '当前小饭桌视角：',
            style: TextStyle(fontSize: 11, color: CozyPalette.onSurfaceVariant),
          ),
        ),
        GestureDetector(
          onTap: () {
            HapticFeedback.lightImpact();
            _switchRole(isCaretaker ? 'eater' : 'caretaker');
          },
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
            decoration: BoxDecoration(
              color: CozyPalette.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: CozyPalette.outlineVariant),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.refresh_rounded,
                    size: 13, color: CozyPalette.primary),
                const SizedBox(width: 4),
                Text(
                  isCaretaker ? '切换为吃货视角' : '切换为饲养员视角',
                  style: const TextStyle(
                    fontSize: 11.5,
                    fontWeight: FontWeight.bold,
                    color: CozyPalette.primary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildStatusChip(bool isPaired) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: (isPaired ? CozyPalette.success : CozyPalette.tertiary)
            .withValues(alpha: 0.16),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: (isPaired ? CozyPalette.success : CozyPalette.tertiary)
              .withValues(alpha: 0.45),
          width: 0.9,
        ),
      ),
      child: Text(
        isPaired ? "已绑定" : "待绑定",
        style: TextStyle(
          fontSize: 10.5,
          fontWeight: FontWeight.w900,
          color: isPaired ? CozyPalette.success : CozyPalette.tertiary,
        ),
      ),
    );
  }

  /// demo `Active Cooking Status`：进行中的订单才出现，没有就整块收起。
  List<Widget> _buildLiveOrderBlock(AppState state) {
    final List<Order> active = state.orders
        .where((Order o) => o.isActive)
        .toList()
      ..sort((Order a, Order b) => b.createdAt.compareTo(a.createdAt));
    if (active.isEmpty) return const <Widget>[];

    final Order live = active.first;
    final String dish = live.items.isEmpty ? '一桌好菜' : live.items.first.name;

    return <Widget>[
      GestureDetector(
        onTap: () => widget.onNavigateTab(3),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          child: Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: CozyPalette.surface,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
              ),
              boxShadow: CozyLight.cardShadow,
            ),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: CozyPalette.tertiaryContainer.withValues(alpha: 0.55),
                    borderRadius: BorderRadius.circular(13),
                    border: Border.all(
                      color: CozyPalette.tertiary.withValues(alpha: 0.35),
                    ),
                  ),
                  child: Image.asset(
                    'assets/images/cooking.png',
                    width: 22,
                    height: 22,
                    filterQuality: FilterQuality.high,
                    errorBuilder: (context, error, stackTrace) =>
                        const Text('🍳', style: TextStyle(fontSize: 18)),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Text(
                            '今日主厨进行中',
                            style: TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w900,
                              color: CozyPalette.onSurface,
                            ),
                          ),
                          const SizedBox(width: 6),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 6,
                              vertical: 1,
                            ),
                            decoration: BoxDecoration(
                              color: CozyPalette.tertiaryContainer,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              live.statusLabel,
                              style: const TextStyle(
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                                color: CozyPalette.tertiary,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 3),
                      Text(
                        '正在做「$dish」，共 ${live.items.length} 道菜 ~',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 11,
                          color: CozyPalette.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(
                  Icons.chevron_right_rounded,
                  size: 18,
                  color: CozyPalette.onSurfaceVariant,
                ),
              ],
            ),
          ),
        ),
      ),
      const SizedBox(height: 14),
    ];
  }

  /// 一个座位：真头像（有就显示）/ 兜底图标或文字 + 昵称 + 身份标签
  /// `badgeAsset` 为右上角 Fluent 3D 身份角标（饲养员=厨师帽 / 吃货=瓷碗）。
  Widget _buildSeat({
    required String avatarUrl,
    String? fallbackAsset,
    IconData? fallbackIcon,
    String? fallbackText,
    required String name,
    required String tagText,
    bool highlight = false,
    VoidCallback? onTap,
    String? badgeAsset,
    Color? badgeColor,
  }) {
    final theme = context.coupleTheme;
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          SizedBox(
            width: 76,
            height: 76,
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                Container(
                  width: 76,
                  height: 76,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(22),
                    border: Border.all(
                      color: highlight
                          ? (badgeColor ?? theme.primary).withValues(alpha: 0.65)
                          : theme.cardBorder,
                      width: 2.0,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: theme.shadowColor.withValues(alpha: 0.12),
                        blurRadius: 8,
                        offset: const Offset(0, 3),
                      ),
                    ],
                  ),
                  padding: const EdgeInsets.all(3),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(18),
                    child: _seatFill(avatarUrl, fallbackAsset, fallbackIcon, fallbackText),
                  ),
                ),
                if (badgeAsset != null)
                  Positioned(
                    top: -2,
                    right: -2,
                    child: Container(
                      width: 24,
                      height: 24,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: badgeColor ?? CozyPalette.primary,
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white, width: 1.2),
                      ),
                      child: Image.asset(
                        badgeAsset,
                        width: 16,
                        height: 16,
                        filterQuality: FilterQuality.high,
                        errorBuilder: (context, error, stackTrace) =>
                            const SizedBox.shrink(),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 6),
          Text(
            name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w900,
              color: CozyPalette.onSurface,
            ),
          ),
          const SizedBox(height: 3),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 2),
            decoration: BoxDecoration(
              color: CozyPalette.surfaceContainer.withValues(alpha: 0.85),
              borderRadius: BorderRadius.circular(999),
              border: Border.all(color: CozyPalette.outlineVariant, width: 0.8),
            ),
            child: Text(
              tagText,
              style: const TextStyle(
                fontSize: 10.5,
                fontWeight: FontWeight.bold,
                color: CozyPalette.onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 座位里那张脸：对方换头像后，这里也要跟着变
  /// （伴侣侧的 data URI 会经 `current_pair_snapshot.partner_avatar_url` 带回来）
  Widget _seatFill(
    String avatarUrl,
    String? fallbackAsset,
    IconData? fallbackIcon,
    String? fallbackText,
  ) {
    if (avatarUrl.isEmpty && fallbackAsset != null && fallbackAsset.isNotEmpty) {
      return Image.asset(
        fallbackAsset,
        fit: BoxFit.cover,
        width: 84,
        height: 84,
        errorBuilder: (context, error, stackTrace) =>
            _seatFallback(fallbackIcon, fallbackText),
      );
    }
    return CozyAvatar(
      url: avatarUrl,
      size: 84,
      fallback: fallbackAsset != null && fallbackAsset.isNotEmpty
          ? Image.asset(
              fallbackAsset,
              fit: BoxFit.cover,
              errorBuilder: (context, error, stackTrace) =>
                  _seatFallback(fallbackIcon, fallbackText),
            )
          : _seatFallback(fallbackIcon, fallbackText),
    );
  }

  Widget _seatFallback(IconData? fallbackIcon, String? fallbackText) {
    return Center(
      child: fallbackIcon != null
          ? Icon(fallbackIcon, size: 36, color: CozyPalette.primary)
          : Text(
              fallbackText ?? "",
              style: const TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.w900,
                color: CozyPalette.primary,
              ),
            ),
    );
  }

  /// demo Candy Coins Fun Widget：糖糖币储备横条 + 右侧「投喂对方」
  /// 深度接入情侣主题主色调与光影
  Widget _buildCoinsBar({
    required AppState state,
    required bool isPaired,
    required CoupleThemeSpec theme,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.centerLeft,
            end: Alignment.centerRight,
            colors: [
              theme.cardBg,
              theme.primaryLight.withValues(alpha: 0.70),
            ],
          ),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: theme.cardBorder, width: 1.2),
          boxShadow: [
            BoxShadow(
              color: theme.shadowColor.withValues(alpha: 0.08),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                gradient: theme.primaryGradient,
                borderRadius: BorderRadius.circular(13),
                boxShadow: [
                  BoxShadow(
                    color: theme.primary.withValues(alpha: 0.35),
                    blurRadius: 6,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Image.asset(
                'assets/images/candy.png',
                width: 22,
                height: 22,
                filterQuality: FilterQuality.high,
                errorBuilder: (context, error, stackTrace) => const Icon(
                  Icons.monetization_on_rounded,
                  size: 20,
                  color: Colors.white,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        '糖糖币储备',
                        style: TextStyle(
                          fontSize: 12.5,
                          fontWeight: FontWeight.w900,
                          color: theme.textMain,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 1.5,
                        ),
                        decoration: BoxDecoration(
                          color: theme.primary.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: Text(
                          ' 币',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w900,
                            color: theme.primaryDark,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    isPaired ? '点菜消耗糖糖币 · 饲养员可撒糖投喂' : '先绑定小饭桌，才能开始点菜',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 11,
                      color: theme.textSub,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            GestureDetector(
              onTap: _openCoins,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 8),
                decoration: BoxDecoration(
                  gradient: theme.primaryGradient,
                  borderRadius: BorderRadius.circular(12),
                  boxShadow: [
                    BoxShadow(
                      color: theme.primary.withValues(alpha: 0.35),
                      blurRadius: 6,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: const Text(
                  '投喂对方',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// demo Quick Action Cards：三张竖版快捷卡（全面植入主题专属动态 GIF）
  /// 我的菜单（吃货等投喂动态）/ 小店管理（大厨掌勺动图）/ 纪念日（甜蜜比心动图）
  Widget _buildQuickActions({
    required bool isCaretaker,
    required CoupleThemeSpec theme,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        children: [
          Expanded(
            child: _buildQuickCard(
              animAsset: theme.eaterAnimAsset,
              fallbackEmoji: '📋',
              title: '我的菜单',
              subtitle: '等开饭',
              bg: theme.primaryLight,
              borderColor: theme.cardBorder,
              textColor: theme.textMain,
              subTextColor: theme.textSub,
              onTap: () => widget.onNavigateTab(1),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: _buildQuickCard(
              animAsset: theme.chefAnimAsset,
              fallbackEmoji: '🏪',
              title: '私厨',
              subtitle: isCaretaker ? '大厨备菜中' : '看看有什么',
              bg: theme.accentLight,
              borderColor: theme.cardBorder,
              textColor: theme.textMain,
              subTextColor: theme.textSub,
              onTap: _openShop,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: _buildQuickCard(
              animAsset: theme.loveAnimAsset,
              fallbackEmoji: '🎂',
              title: '甜蜜纪念',
              subtitle: ' ♥ ',
              bg: theme.primaryLight,
              borderColor: theme.cardBorder,
              textColor: theme.textMain,
              subTextColor: theme.textSub,
              onTap: _openAnniversary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickCard({
    required String? animAsset,
    required String fallbackEmoji,
    required String title,
    required String subtitle,
    required Color bg,
    required Color borderColor,
    required Color textColor,
    required Color subTextColor,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 6),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: borderColor.withValues(alpha: 0.80),
            width: 1.2,
          ),
          boxShadow: [
            BoxShadow(
              color: borderColor.withValues(alpha: 0.15),
              blurRadius: 8,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          children: [
            Container(
              width: 44,
              height: 44,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: bg,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: borderColor.withValues(alpha: 0.4)),
              ),
              clipBehavior: Clip.antiAlias,
              child: animAsset != null && animAsset.isNotEmpty
                  ? Image.asset(
                      animAsset,
                      width: 40,
                      height: 40,
                      fit: BoxFit.contain,
                      errorBuilder: (_, _, _) => Text(
                        fallbackEmoji,
                        style: const TextStyle(fontSize: 20),
                      ),
                    )
                  : Text(fallbackEmoji, style: const TextStyle(fontSize: 20)),
            ),
            const SizedBox(height: 7),
            Text(
              title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w900,
                color: textColor,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              subtitle,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 9.5,
                color: subTextColor,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// demo Today's Chef Recommendation Reel：饲养员私房招牌
  /// 带有当前大厨专属动态小吉祥物助阵！
  List<Widget> _buildChefPicks(AppState state, CoupleThemeSpec theme) {
    // 优先按历史真实「已点/销量」(salesCount) 从高到低排序，选出最具代表性的拿手 Top 2；
    // 若点单数相同或初始状态，保持菜品稳定展示
    final List<MenuItem> available = state.menu
        .where((MenuItem m) => m.isAvailable)
        .toList();
    available.sort((a, b) => b.salesCount.compareTo(a.salesCount));
    final List<MenuItem> picks = available.take(2).toList();
    if (picks.isEmpty) return const <Widget>[];

    return <Widget>[
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Row(
          children: [
            Expanded(
              child: Row(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Container(
                      width: 28,
                      height: 28,
                      padding: const EdgeInsets.all(2),
                      decoration: BoxDecoration(
                        color: theme.primaryLight,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: theme.cardBorder),
                      ),
                      child: Image.asset(
                        theme.cheerAnimAsset,
                        fit: BoxFit.contain,
                        errorBuilder: (_, _, _) => const Text('🌟', style: TextStyle(fontSize: 16)),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    '私房招牌',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w900,
                      color: theme.textMain,
                    ),
                  ),
                  const SizedBox(width: 5),
                  Text(
                    '(拿手 Top 2)',
                    style: TextStyle(
                      fontSize: 10,
                      color: theme.textSub,
                    ),
                  ),
                ],
              ),
            ),
            GestureDetector(
              onTap: () => widget.onNavigateTab(1),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '全部菜品',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: theme.primary,
                    ),
                  ),
                  Icon(Icons.chevron_right_rounded,
                      size: 14, color: theme.primary),
                ],
              ),
            ),
          ],
        ),
      ),
      const SizedBox(height: 10),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Row(
          children: [
            for (int i = 0; i < picks.length; i++) ...[
              if (i > 0) const SizedBox(width: 10),
              Expanded(child: _buildPickCard(picks[i], theme)),
            ],
          ],
        ),
      ),
    ];
  }

  Widget _buildPickCard(MenuItem dish, CoupleThemeSpec theme) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        widget.onNavigateTab(1);
      },
      child: Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: theme.cardBorder.withValues(alpha: 0.80),
            width: 1.2,
          ),
          boxShadow: [
            BoxShadow(
              color: theme.shadowColor.withValues(alpha: 0.06),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(14),
              child: SizedBox(
                width: double.infinity,
                height: 96,
                child: CozyDishPhoto(
                  url: dish.imageUrl,
                  cssWidth: 160,
                  fit: BoxFit.cover,
                  placeholderColor: theme.primaryLight,
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              dish.name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w900,
                color: theme.textMain,
              ),
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                Image.asset(
                  'assets/images/candy.png',
                  width: 14,
                  height: 14,
                  filterQuality: FilterQuality.high,
                  errorBuilder: (context, error, stackTrace) =>
                      const SizedBox.shrink(),
                ),
                const SizedBox(width: 3),
                Text(
                  dish.price.toStringAsFixed(0),
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w900,
                    color: theme.primary,
                  ),
                ),
                const Spacer(),
                Text(
                  '已点 ${dish.salesCount} 次',
                  style: TextStyle(
                    fontSize: 10,
                    color: theme.textSub,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 专属情侣陪伴挂件（动态吉祥物 + 轮播专属情侣情话）
  Widget _buildThemeMascotBanner(CoupleThemeSpec theme) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: GestureDetector(
        onTap: () {
          HapticFeedback.lightImpact();
          final quotes = theme.dialogQuotes;
          if (quotes.isNotEmpty) {
            _quoteIndex = (_quoteIndex + 1) % quotes.length;
            showCozyToast(context, quotes[_quoteIndex]);
          }
        },
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.92),
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: theme.cardBorder, width: 1.2),
            boxShadow: [
              BoxShadow(
                color: theme.shadowColor.withValues(alpha: 0.08),
                blurRadius: 12,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            children: [
              Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(
                  color: theme.primaryLight,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: theme.cardBorder, width: 1.2),
                ),
                padding: const EdgeInsets.all(3),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(11),
                  child: Image.asset(
                    theme.floatingAnimAsset,
                    fit: BoxFit.contain,
                    errorBuilder: (_, _, _) => Center(
                      child: Text(theme.emoji, style: const TextStyle(fontSize: 20)),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(
                          '${theme.defaultChefName} & ${theme.defaultEaterName}',
                          style: TextStyle(
                            fontSize: 12.5,
                            fontWeight: FontWeight.w900,
                            color: theme.primary,
                          ),
                        ),
                        const SizedBox(width: 6),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                          decoration: BoxDecoration(
                            color: theme.primaryLight,
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Text(
                            theme.stampTip,
                            style: TextStyle(
                              fontSize: 9.5,
                              fontWeight: FontWeight.w700,
                              color: theme.primaryDark,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 2),
                    Text(
                      theme.sweetSub,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 11,
                        color: theme.textSub,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.touch_app_rounded, size: 16, color: theme.primary),
            ],
          ),
        ),
      ),
    );
  }

}

/// 与纪念日页同一口径的日期解析（`anniversary_at` 可能带时间部分）。
DateTime? _parseDateSafely(String value) {
  if (value.isEmpty) return null;
  final DateTime? iso = DateTime.tryParse(value);
  if (iso != null) return DateTime(iso.year, iso.month, iso.day);
  final String head = value.length > 10 ? value.substring(0, 10) : value;
  final DateTime? dateOnly = DateTime.tryParse(head);
  if (dateOnly == null) return null;
  return DateTime(dateOnly.year, dateOnly.month, dateOnly.day);
}
