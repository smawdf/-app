import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../candy/candy_coins_page.dart';
import '../couple/anniversary_page.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';

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

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(ok
            ? '已切换身份为【${state.user?.roleLabel ?? newRole}】'
            : '身份切换失败，请检查网络'),
        backgroundColor: ok ? CozyTheme.sweetCocoa : CozyPalette.error,
        duration: const Duration(seconds: 2),
      ),
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

    return ListenableBuilder(
      listenable: state,
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

                const SizedBox(height: 6),

                // 1. 主角：我们的小饭桌
                _buildTableHero(user: user, pair: pair, isPaired: isPaired),

                const SizedBox(height: 14),

                // 2. 关系进行中的两个数字：糖糖币余额 / 最近一餐
                _buildStatsRow(state: state, isPaired: isPaired),

                const SizedBox(height: 14),

                // 3. 两个小入口：我的店铺 / 去点菜
                _buildEntryRow(isCaretaker: state.isCaretaker),

                const SizedBox(height: 14),

                // 4. 今天谁在饭桌旁：饲养员 / 吃货
                _buildRoleSwitcher(
                  selectedRole: user?.role,
                  onCaretakerClick: () => _switchRole('caretaker'),
                  onEaterClick: () => _switchRole('eater'),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  /// 顶部：居中一句话 + 左心右铃（铃铛进糖糖币）
  Widget _buildTopBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
      child: Row(
        children: [
          const Icon(Icons.favorite, color: CozyPalette.primary, size: 24),
          const Expanded(
            child: Text(
              "今天也要一起好好吃饭",
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 21,
                fontWeight: FontWeight.w900,
                color: CozyPalette.onSurface,
                letterSpacing: -0.5,
              ),
            ),
          ),
          GestureDetector(
            onTap: _openCoins,
            child: const Icon(Icons.notifications_none_rounded,
                color: CozyPalette.onSurfaceVariant, size: 24),
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
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [CozyPalette.secondaryContainer, CozyPalette.primaryContainer],
        ),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: CozyPalette.primary.withValues(alpha: 0.30), width: 1.3),
        boxShadow: CozyLight.cardShadow,
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: Stack(
          alignment: Alignment.center,
          children: [
            // 爪印水印只当纸纹，不和「两个座位 + 天数」抢注意力
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
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 14),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.table_restaurant,
                          size: 15, color: CozyPalette.primary),
                      const SizedBox(width: 6),
                      const Text(
                        "我们的小饭桌",
                        style: TextStyle(
                          fontSize: 12.5,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.2,
                          color: CozyPalette.primary,
                        ),
                      ),
                      const Spacer(),
                      _buildStatusChip(isPaired),
                    ],
                  ),
                  const SizedBox(height: 12),

                  // 两个座位 + 中间的心
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
                        onTap: isPaired ? null : () => widget.onNavigateTab(4),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),

                  // 一起吃饭 N 天（真机此前是写死的 520，现在按云端纪念日算）
                  _buildDaysBlock(isPaired: isPaired),

                  const SizedBox(height: 12),
                  Container(height: 1, color: CozyLight.hairline),
                  const SizedBox(height: 8),

                  // 纪念日入口（原「纪念日」长卡的位置，现在归到关系卡里）
                  _buildAnniversaryRow(),
                ],
              ),
            ),
          ],
        ),
      ),
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

  Widget _buildDaysBlock({required bool isPaired}) {
    final int? days = _daysTogether;

    final String headline = !isPaired
        ? "等对方入座"
        : (days != null ? "一起吃饭 $days 天" : "一起吃饭的第 1 天");
    final String sub = !isPaired
        ? "把邀请码发给 TA，就能一起开饭"
        : (days != null ? "今天也想和你好好吃饭" : "纪念日还没设置，点下面去挑一天");

    return GestureDetector(
      onTap: _openAnniversary,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        decoration: BoxDecoration(
          color: CozyPalette.surfaceContainerLow.withValues(alpha: 0.72),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: CozyPalette.primary.withValues(alpha: 0.20)),
        ),
        child: Column(
          children: [
            Text(
              headline,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w900,
                color: CozyPalette.primary,
                letterSpacing: -0.3,
              ),
            ),
            const SizedBox(height: 3),
            Text(
              sub,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 11.5, color: CozyPalette.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAnniversaryRow() {
    final DateTime? date = _anniversaryDate;
    final String label = date == null
        ? "还没设纪念日"
        : "纪念日 · ${date.month} 月 ${date.day} 日";

    return GestureDetector(
      onTap: _openAnniversary,
      child: Row(
        children: [
          Container(
            width: 30,
            height: 30,
            decoration: BoxDecoration(
              color: CozyPalette.tertiary.withValues(alpha: 0.16),
              borderRadius: BorderRadius.circular(11),
            ),
            child: const Icon(Icons.event, color: CozyPalette.tertiary, size: 17),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "纪念日",
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w900,
                    color: CozyPalette.onSurface,
                  ),
                ),
                Text(
                  label,
                  style: const TextStyle(fontSize: 11, color: CozyPalette.onSurfaceVariant),
                ),
              ],
            ),
          ),
          const Icon(Icons.chevron_right_rounded,
              color: CozyPalette.onSurfaceVariant, size: 20),
        ],
      ),
    );
  }

  /// 一个座位：真头像（有就显示）/ 兜底图标或文字 + 昵称 + 身份标签
  Widget _buildSeat({
    required String avatarUrl,
    IconData? fallbackIcon,
    String? fallbackText,
    required String name,
    required String tagText,
    bool highlight = false,
    VoidCallback? onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
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
            child: ClipOval(child: _seatFill(avatarUrl, fallbackIcon, fallbackText)),
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

  Widget _seatFill(String avatarUrl, IconData? fallbackIcon, String? fallbackText) {
    if (avatarUrl.isNotEmpty) {
      return Image.network(
        avatarUrl,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => _seatFallback(fallbackIcon, fallbackText),
      );
    }
    return _seatFallback(fallbackIcon, fallbackText);
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

  /// 关系进行中的两个数字：糖糖币余额 + 最近一餐
  Widget _buildStatsRow({required AppState state, required bool isPaired}) {
    final List<Order> orders = [...state.orders]
      ..sort((Order a, Order b) => b.createdAt.compareTo(a.createdAt));
    final Order? latest = orders.isEmpty ? null : orders.first;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        children: [
          Expanded(
            child: _buildStatCard(
              caption: "糖糖币余额",
              value: "${state.candyCoins} 枚",
              hint: isPaired ? "点菜时会真实扣减" : "先绑定小饭桌",
              icon: Icons.monetization_on_rounded,
              accent: CozyPalette.primary,
              onTap: _openCoins,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _buildStatCard(
              caption: "最近一餐",
              value: latest == null
                  ? "还没开饭"
                  : (latest.items.isNotEmpty ? latest.items.first.name : "一餐"),
              hint: latest == null
                  ? "点菜后这里会留下记录"
                  : "${latest.statusLabel} · ${latest.candyCoinsSpent} 枚",
              icon: Icons.ramen_dining_rounded,
              accent: CozyPalette.tertiary,
              onTap: () => widget.onNavigateTab(3),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard({
    required String caption,
    required String value,
    required String hint,
    required IconData icon,
    required Color accent,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: CozyPalette.surface,
          borderRadius: BorderRadius.circular(22),
          border: Border.all(color: CozyPalette.outlineVariant.withValues(alpha: 0.72)),
          boxShadow: CozyLight.cardShadow,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 30,
                  height: 30,
                  decoration: BoxDecoration(
                    color: accent.withValues(alpha: 0.14),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(icon, color: accent, size: 17),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    caption,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: CozyPalette.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              value,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w900,
                color: accent,
              ),
            ),
            const SizedBox(height: 3),
            Text(
              hint,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 10, color: CozyPalette.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }

  /// 两个小入口：我的店铺 / 去点菜（原来它们是右侧两张扁卡 + 左侧纪念日长卡）
  Widget _buildEntryRow({required bool isCaretaker}) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        children: [
          Expanded(
            child: _buildEntryCard(
              title: "我的店铺",
              subtitle: isCaretaker ? "上传菜单，整理菜品" : "看看饭桌上有什么",
              icon: Icons.storefront,
              accent: CozyPalette.primary,
              onTap: _openShop,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _buildEntryCard(
              title: "去点菜",
              subtitle: "看看今天想吃什么",
              icon: Icons.restaurant_menu,
              accent: CozyPalette.tertiary,
              onTap: () => widget.onNavigateTab(1),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEntryCard({
    required String title,
    required String subtitle,
    required IconData icon,
    required Color accent,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
        decoration: BoxDecoration(
          color: accent.withValues(alpha: 0.10),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: accent.withValues(alpha: 0.28)),
        ),
        child: Row(
          children: [
            Icon(icon, color: accent, size: 22),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w900,
                      color: CozyPalette.onSurface,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 10, color: CozyPalette.onSurfaceVariant),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 今天谁在饭桌旁：饲养员 / 吃货
  Widget _buildRoleSwitcher({
    required String? selectedRole,
    required VoidCallback onCaretakerClick,
    required VoidCallback onEaterClick,
  }) {
    final bool isCaretaker = selectedRole == 'caretaker';
    final bool isEater = selectedRole == 'eater';

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.only(left: 4, bottom: 10),
            child: Text(
              "今天谁在饭桌旁",
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w900,
                color: CozyPalette.onSurface,
              ),
            ),
          ),
          Row(
            children: [
              Expanded(
                child: _buildRoleCardItem(
                  title: "饲养员",
                  subtitle: "上传菜单，照顾小饭桌",
                  icon: Icons.soup_kitchen,
                  selected: isCaretaker,
                  accent: CozyPalette.primary,
                  onTap: onCaretakerClick,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildRoleCardItem(
                  title: "吃货",
                  subtitle: "浏览菜单，准备开饭",
                  icon: Icons.restaurant,
                  selected: isEater,
                  accent: CozyPalette.tertiary,
                  onTap: onEaterClick,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildRoleCardItem({
    required String title,
    required String subtitle,
    required IconData icon,
    required bool selected,
    required Color accent,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: selected ? accent.withValues(alpha: 0.12) : CozyPalette.surface,
          borderRadius: BorderRadius.circular(22),
          border: Border.all(
            color: selected
                ? accent.withValues(alpha: 0.65)
                : CozyPalette.outlineVariant.withValues(alpha: 0.72),
            width: selected ? 1.5 : 1.0,
          ),
          boxShadow: CozyLight.cardShadow,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: accent.withValues(alpha: 0.14),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(icon, color: accent, size: 20),
                ),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w900,
                    color: selected ? accent : CozyPalette.onSurface,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              subtitle,
              style: const TextStyle(fontSize: 11, color: CozyPalette.onSurfaceVariant),
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
