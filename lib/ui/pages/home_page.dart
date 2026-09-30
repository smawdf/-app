import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../candy/candy_coins_page.dart';
import '../couple/anniversary_page.dart';
import '../menu/menu_management_page.dart';
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
  /// 云端 `anniversaries.anniversary_at`（纪念日起始日）。
  /// 真机此前「一起吃饭 520 天」是写死的，这里改成从云端算。
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
      listenable: state.listenFor(const {Domain.profile, Domain.orders, Domain.candy}),
      builder: (context, _) {
        final AppUser? user = state.user;
        final CouplePair? pair = state.pair;
        final bool isPaired = state.isPaired;

        return RefreshIndicator(
          color: CozyTheme.primaryPink,
          backgroundColor: CozyPalette.surface,
          onRefresh: _bootstrap,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.only(bottom: 120),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildTopBar(),

                const SizedBox(height: 10),

                // 1. 主角：我们的小饭桌（含相守天数胶囊 / 纪念相册 / 角色切换栏）
                _buildTableHero(user: user, pair: pair, isPaired: isPaired),

                const SizedBox(height: 14),

                // 2. 今日主厨进行中（没有进行中的订单时自动收起，不留空壳）
                ..._buildLiveOrderBlock(state),

                // 3. 三张快捷卡：我的菜单 / 小店管理 / 纪念日
                _buildQuickActions(isCaretaker: state.isCaretaker),

                const SizedBox(height: 14),

                // 4. 糖糖币储备横条（含「投喂对方」）
                _buildCoinsBar(state: state, isPaired: isPaired),

                const SizedBox(height: 18),

                // 5. 饲养员私房招牌（店内菜品 Top 2，对应 demo「糖糖最爱吃 Top 3」）
                ..._buildChefPicks(state),
              ],
            ),
          ),
        );
      },
    );
  }

  /// 顶部：左「OUR KITCHEN / 双人小饭桌」标签块 + 大标题 + 右铃铛
  /// （对齐 demo `<!-- Romantic Top Header -->`：标签胶囊 + 副标 + 标题 + 🔔）
  Widget _buildTopBar() {
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
                        color: CozyPalette.primaryContainer,
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: const Text(
                        'OUR KITCHEN',
                        style: TextStyle(
                          fontSize: 10,
                          height: 1.3,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.0,
                          color: CozyPalette.primary,
                        ),
                      ),
                    ),
                    const SizedBox(width: 6),
                    const Text(
                      '双人小饭桌',
                      style: TextStyle(
                        fontSize: 11,
                        color: CozyPalette.onSurfaceVariant,
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

  /// 小饭桌主卡：两个座位 + 一起吃饭 N 天 + 纪念日
  Widget _buildTableHero({
    required AppUser? user,
    required CouplePair? pair,
    required bool isPaired,
  }) {
    final String myName = (user?.nickname.isNotEmpty ?? false) ? user!.nickname : "我";
    final String myRole = user?.roleLabel ?? "选择身份";
    // 伴侣昵称来自 `CouplePair.partnerName`（`SupabaseApi.me()` 按 pair_id
    // 查对方那行 profile 时带回来的 nickname）。
    final String rawPartnerName = pair?.partnerName.trim() ?? '';
    final String partnerName = !isPaired
        ? "等对方入座"
        : (rawPartnerName.isNotEmpty ? rawPartnerName : "伴侣资料同步中");
    // 小饭桌只有两个角色，所以对方坐的一定是另一个身份。
    final String partnerRole = !isPaired
        ? "座位空着"
        : (user?.isCaretaker ?? false ? "吃货" : "饲养员");

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      decoration: BoxDecoration(
        // demo：`from-white via-cozy-pink-light/40 to-cozy-peach/30`
        // 浅色雾面渐变（旧版是重粉实色，和 demo 观感差得明显）。
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            CozyPalette.surface,
            CozyPalette.primaryContainer.withValues(alpha: 0.55),
            CozyPalette.tertiaryContainer.withValues(alpha: 0.35),
          ],
        ),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: Colors.white, width: 1.3),
        boxShadow: CozyLight.cardShadow,
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
                color: CozyPalette.primary.withValues(alpha: 0.05),
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
                      _buildDaysPill(isPaired: isPaired),
                      const Spacer(),
                      if (isPaired)
                        _buildAlbumLink()
                      else
                        _buildStatusChip(isPaired),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // 两个座位 + 中间跳动的心
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      _buildSeat(
                        avatarUrl: user?.avatarUrl ?? '',
                        fallbackIcon: Icons.pets,
                        name: myName,
                        tagText: myRole,
                        highlight: true,
                        badgeAsset: 'assets/images/chef.png',
                        badgeColor: CozyPalette.primary,
                      ),
                      Container(
                        width: 42,
                        height: 42,
                        decoration: const BoxDecoration(
                          color: CozyPalette.surfaceContainerLow,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.favorite,
                                color: CozyPalette.primary, size: 20)
                            .animate(onPlay: (c) => c.repeat(reverse: true))
                            .scale(
                                duration: 1200.ms,
                                begin: const Offset(0.9, 0.9),
                                end: const Offset(1.15, 1.15)),
                      ),
                      _buildSeat(
                        avatarUrl: pair?.partnerAvatarUrl ?? '',
                        fallbackText: isPaired ? "伴" : "+",
                        name: partnerName,
                        tagText: partnerRole,
                        highlight: isPaired,
                        badgeAsset: 'assets/images/bowl.png',
                        badgeColor: CozyPalette.tertiary,
                        onTap: isPaired ? null : () => widget.onNavigateTab(4),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  Container(height: 1, color: CozyLight.hairline),
                  const SizedBox(height: 6),

                  // 角色切换栏（demo `Role Quick Switch Bar`）
                  _buildRoleSwitchBar(user: user),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// demo 顶部「❤️ 恋爱相守 N 天」胶囊。
  Widget _buildDaysPill({required bool isPaired}) {
    final int? days = _daysTogether;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
      decoration: BoxDecoration(
        color: CozyPalette.surface.withValues(alpha: 0.92),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: CozyPalette.outlineVariant.withValues(alpha: 0.60),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.favorite, size: 12, color: Color(0xFFE8385A)),
          const SizedBox(width: 5),
          const Text(
            '恋爱相守',
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              color: CozyPalette.primary,
            ),
          ),
          const SizedBox(width: 4),
          Text(
            isPaired && days != null ? '$days' : '—',
            style: const TextStyle(
              fontSize: 14,
              height: 1.1,
              fontWeight: FontWeight.w900,
              color: CozyPalette.primary,
            ),
          ),
          const SizedBox(width: 2),
          const Text(
            '天',
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              color: CozyPalette.primary,
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
            '点击身份可切换体验角色：',
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
    IconData? fallbackIcon,
    String? fallbackText,
    required String name,
    required String tagText,
    bool highlight = false,
    VoidCallback? onTap,
    String? badgeAsset,
    Color? badgeColor,
  }) {
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
                    color: CozyPalette.surfaceContainer,
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: highlight
                          ? CozyPalette.primary.withValues(alpha: 0.45)
                          : CozyPalette.surfaceVariant,
                      width: 2,
                    ),
                    boxShadow: CozyLight.cardShadow,
                  ),
                  child:
                      ClipOval(child: _seatFill(avatarUrl, fallbackIcon, fallbackText)),
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
  Widget _seatFill(String avatarUrl, IconData? fallbackIcon, String? fallbackText) {
    return CozyAvatar(
      url: avatarUrl,
      size: 84,
      fallback: _seatFallback(fallbackIcon, fallbackText),
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

  /// demo `Candy Coins Fun Widget`：糖糖币储备横条 + 右侧「投喂对方」。
  /// （旧版是「糖糖币余额 / 最近一餐」两张统计卡，和 demo 结构不同）
  Widget _buildCoinsBar({required AppState state, required bool isPaired}) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.centerLeft,
            end: Alignment.centerRight,
            colors: [Color(0xFFFFF7ED), Color(0xFFFEF2F2)],
          ),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: const Color(0xFFF5D7A8)),
        ),
        child: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: const Color(0xFFF2B23E),
                borderRadius: BorderRadius.circular(13),
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
                      const Text(
                        '糖糖币储备',
                        style: TextStyle(
                          fontSize: 12.5,
                          fontWeight: FontWeight.w900,
                          color: Color(0xFF7A4A10),
                        ),
                      ),
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 1,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFCEBCF),
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: Text(
                          '${state.candyCoins} 币',
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w900,
                            color: Color(0xFFC77A14),
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
                    style: const TextStyle(
                      fontSize: 11,
                      color: Color(0xFF9A6A2E),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            GestureDetector(
              onTap: _openCoins,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFFE09A2B), Color(0xFFE8385A)],
                  ),
                  borderRadius: BorderRadius.circular(12),
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

  /// demo `Quick Action Cards`：三张竖版快捷卡
  /// 我的菜单（去点菜）/ 小店管理 / 纪念日。
  Widget _buildQuickActions({required bool isCaretaker}) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        children: [
          Expanded(
            child: _buildQuickCard(
              emoji: '📋',
              title: '我的菜单',
              subtitle: '即刻点菜',
              bg: const Color(0xFFFFE4EA),
              onTap: () => widget.onNavigateTab(1),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: _buildQuickCard(
              emoji: '🏪',
              title: '小店管理',
              subtitle: isCaretaker ? '菜品上下架' : '看看有什么',
              bg: const Color(0xFFFDF0D5),
              onTap: _openShop,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: _buildQuickCard(
              emoji: '🎂',
              title: '纪念日',
              subtitle: '浪漫里程碑',
              bg: const Color(0xFFF0E9FB),
              onTap: _openAnniversary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickCard({
    required String emoji,
    required String title,
    required String subtitle,
    required Color bg,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 6),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
          ),
          boxShadow: CozyLight.cardShadow,
        ),
        child: Column(
          children: [
            Container(
              width: 36,
              height: 36,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: bg,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(emoji, style: const TextStyle(fontSize: 18)),
            ),
            const SizedBox(height: 8),
            Text(
              title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w900,
                color: CozyPalette.onSurface,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              subtitle,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 9.5,
                color: CozyPalette.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// demo `Today's Chef Recommendation Reel`：饲养员私房招牌
  /// （店内可售菜品前 2 道：成品图 + 菜名 + 糖糖币价 + 已点次数）。
  List<Widget> _buildChefPicks(AppState state) {
    final List<MenuItem> picks = state.menu
        .where((MenuItem m) => m.isAvailable)
        .take(2)
        .toList();
    if (picks.isEmpty) return const <Widget>[];

    return <Widget>[
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Row(
          children: [
            const Expanded(
              child: Row(
                children: [
                  Text(
                    '🌟 饲养员私房招牌',
                    style: TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w900,
                      color: CozyPalette.onSurface,
                    ),
                  ),
                  SizedBox(width: 5),
                  Text(
                    '(拿手 Top 2)',
                    style: TextStyle(
                      fontSize: 10,
                      color: CozyPalette.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            GestureDetector(
              onTap: () => widget.onNavigateTab(1),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '全部菜品',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: CozyPalette.primary,
                    ),
                  ),
                  Icon(Icons.chevron_right_rounded,
                      size: 14, color: CozyPalette.primary),
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
              Expanded(child: _buildPickCard(picks[i])),
            ],
          ],
        ),
      ),
    ];
  }

  Widget _buildPickCard(MenuItem dish) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        widget.onNavigateTab(1);
      },
      child: Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
          ),
          boxShadow: CozyLight.cardShadow,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: SizedBox(
                width: double.infinity,
                height: 96,
                child: CozyDishPhoto(
                  url: dish.imageUrl,
                  cssWidth: 160,
                  fit: BoxFit.cover,
                  placeholderColor: CozyPalette.surfaceVariant,
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              dish.name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w900,
                color: CozyPalette.onSurface,
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
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w900,
                    color: Color(0xFFE8385A),
                  ),
                ),
                const Spacer(),
                Text(
                  '已点 ${dish.salesCount} 次',
                  style: const TextStyle(
                    fontSize: 10,
                    color: CozyPalette.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ],
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
