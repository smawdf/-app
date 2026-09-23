import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../candy/candy_coins_page.dart';
import '../couple/anniversary_page.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';

/// 忠实还原原生 Android CoupleMenuScreen 布局与暖调色彩，
/// 背景纯白，卡片与结构保持原状，无擅自删改。
class HomePage extends StatefulWidget {
  final ValueChanged<int> onNavigateTab;

  const HomePage({super.key, required this.onNavigateTab});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.loadMe();
    });
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
        backgroundColor: ok ? CozyTheme.sweetCocoa : const Color(0xFFD64545),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return ListenableBuilder(
      listenable: state,
      builder: (context, _) {
        final user = state.user;
        final pair = state.pair;
        final isPaired = state.isPaired;
        final selectedRole = user?.role;

        return RefreshIndicator(
          color: CozyTheme.primaryPink,
          onRefresh: () => state.loadMe(),
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.only(bottom: 120),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 1. 原生顶部标题栏 (CozyMainTopBar 原型)
                _buildOriginalTopBar(),

                const SizedBox(height: 10),

                // 2. 原生 RelationshipCard 暖粉色主卡片
                _buildOriginalRelationshipCard(
                  user: user,
                  pair: pair,
                  isPaired: isPaired,
                  onAnniversaryClick: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const AnniversaryPage()),
                    );
                  },
                ),

                const SizedBox(height: 14),

                // 3. 原生 QuickActionGrid 三卡片非对称排版 (纪念日左高，我的店铺+去点菜右叠)
                _buildOriginalQuickActionGrid(),

                const SizedBox(height: 14),

                // 4. 原生 RoleSwitcher (饲养员 + 吃货横排卡片)
                _buildOriginalRoleSwitcher(
                  selectedRole: selectedRole,
                  onCaretakerClick: () => _switchRole("caretaker"),
                  onEaterClick: () => _switchRole("eater"),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  /// 原生 CozyMainTopBar: 居中大字标题 + 左右心形与铃铛
  Widget _buildOriginalTopBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      child: Row(
        children: [
          const Icon(Icons.favorite, color: Color(0xFF894C5C), size: 24),
          const Expanded(
            child: Text(
              "今天也要一起好好吃饭",
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 21,
                fontWeight: FontWeight.w900,
                color: Color(0xFF1D1B18),
                letterSpacing: -0.5,
              ),
            ),
          ),
          GestureDetector(
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const CandyCoinsPage()),
              );
            },
            child: const Icon(Icons.notifications_none_rounded, color: Color(0xFF524346), size: 24),
          ),
        ],
      ),
    );
  }

  /// 原生 RelationshipCard: 暖粉色背景底、爪印大水印、情侣双头像 + 爱心连接器 + 椭圆开饭胶囊
  Widget _buildOriginalRelationshipCard({
    required dynamic user,
    required dynamic pair,
    required bool isPaired,
    required VoidCallback onAnniversaryClick,
  }) {
    final myName = user?.nickname?.isNotEmpty == true ? user.nickname : "我";
    final myRole = user?.roleLabel ?? "选择身份";
    final partnerName = isPaired ? "伴侣资料同步中" : "邀请对方";

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      decoration: BoxDecoration(
        color: const Color(0xFFFFD1DC).withValues(alpha: 0.35),
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: const Color(0xFFFFD1DC).withValues(alpha: 0.55), width: 1.2),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(26),
        child: Stack(
          alignment: Alignment.center,
          children: [
            // 背景爪印大水印
            Positioned(
              child: Icon(
                Icons.pets,
                size: 190,
                color: Colors.white.withValues(alpha: 0.35),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // 头像与心动连接器
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      // 左侧：当前用户
                      _buildSlotAvatar(
                        fallbackIcon: Icons.pets,
                        name: myName,
                        tagText: "当前角色:$myRole",
                        isHighlight: true,
                      ),

                      // 中间：心形连接器
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.92),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.favorite, color: Color(0xFF894C5C), size: 22)
                            .animate(onPlay: (c) => c.repeat(reverse: true))
                            .scale(duration: 1200.ms, begin: const Offset(0.9, 0.9), end: const Offset(1.15, 1.15)),
                      ),

                      // 右侧：伴侣位
                      _buildSlotAvatar(
                        fallbackText: isPaired ? "伴" : "+",
                        name: partnerName,
                        tagText: isPaired ? "已绑定" : "点击配对",
                        isHighlight: isPaired,
                        onTap: isPaired ? null : () => widget.onNavigateTab(4),
                      ),
                    ],
                  ),
                  const SizedBox(height: 18),

                  // 椭圆开饭天数胶囊 (Surface with pill)
                  GestureDetector(
                    onTap: onAnniversaryClick,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 26, vertical: 10),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.92),
                        borderRadius: BorderRadius.circular(999),
                        border: Border.all(color: const Color(0xFF894C5C).withValues(alpha: 0.20)),
                      ),
                      child: Column(
                        children: [
                          Text(
                            isPaired ? "一起吃饭 520 天" : "一起吃饭 1 天",
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w900,
                              color: Color(0xFF894C5C),
                            ),
                          ),
                          const SizedBox(height: 2),
                          const Text(
                            "今天也想和你好好吃饭",
                            style: TextStyle(fontSize: 11.5, color: Color(0xFF524346)),
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

  Widget _buildSlotAvatar({
    IconData? fallbackIcon,
    String? fallbackText,
    required String name,
    required String tagText,
    bool isHighlight = false,
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
              color: Colors.white,
              shape: BoxShape.circle,
              border: Border.all(
                color: isHighlight ? const Color(0xFF894C5C).withValues(alpha: 0.35) : const Color(0xFFE7E2DC),
                width: 2,
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.04),
                  blurRadius: 10,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: Center(
              child: fallbackIcon != null
                  ? Icon(fallbackIcon, size: 34, color: const Color(0xFF894C5C))
                  : Text(
                      fallbackText ?? "",
                      style: const TextStyle(
                        fontSize: 26,
                        fontWeight: FontWeight.w900,
                        color: Color(0xFF894C5C),
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 6),
          Text(
            name,
            style: const TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w900,
              color: Color(0xFF1D1B18),
            ),
          ),
          const SizedBox(height: 4),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(999),
              border: Border.all(color: const Color(0xFFD6C1C5), width: 0.8),
            ),
            child: Text(
              tagText,
              style: const TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFF524346)),
            ),
          ),
        ],
      ),
    );
  }

  /// 原生 QuickActionGrid: 左侧高卡片「纪念日」，右侧两张扁卡片「我的店铺」与「去点菜」
  Widget _buildOriginalQuickActionGrid() {
    const double cardHeight = 84;
    const double gridHeight = cardHeight * 2 + 12;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 左侧：纪念日垂直长卡片
          Expanded(
            child: _buildCozyCard(
              height: gridHeight,
              title: "纪念日",
              subtitle: "记录我们一起吃饭的日子",
              icon: Icons.event,
              tint: const Color(0xFF8B4E38),
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const AnniversaryPage()),
                );
              },
            ),
          ),
          const SizedBox(width: 12),

          // 右侧：上下两张扁卡片
          Expanded(
            child: Column(
              children: [
                _buildCozyCard(
                  height: cardHeight,
                  title: "我的店铺",
                  subtitle: "上传菜单，整理菜品",
                  icon: Icons.storefront,
                  tint: const Color(0xFF894C5C),
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const MenuManagementPage()),
                    );
                  },
                ),
                const SizedBox(height: 12),
                _buildCozyCard(
                  height: cardHeight,
                  title: "去点菜",
                  subtitle: "看看今天想吃什么",
                  icon: Icons.restaurant_menu,
                  tint: const Color(0xFF8B4E38),
                  onTap: () => widget.onNavigateTab(1),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCozyCard({
    required double height,
    required String title,
    required String subtitle,
    required IconData icon,
    required Color tint,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      child: Container(
        height: height,
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: const Color(0xFFFFFCF8),
          borderRadius: BorderRadius.circular(22),
          border: Border.all(color: const Color(0xFFD6C1C5).withValues(alpha: 0.72)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.02),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: tint.withValues(alpha: 0.13),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: tint, size: 20),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w900,
                    color: Color(0xFF1D1B18),
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 10.5, color: Color(0xFF524346)),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// 原生 RoleSwitcher: 饲养员卡片 + 吃货卡片并排
  Widget _buildOriginalRoleSwitcher({
    required String? selectedRole,
    required VoidCallback onCaretakerClick,
    required VoidCallback onEaterClick,
  }) {
    final isCaretaker = selectedRole == 'caretaker';
    final isEater = selectedRole == 'eater';

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        children: [
          Expanded(
            child: _buildRoleCardItem(
              title: "饲养员",
              subtitle: "上传菜单，照顾小饭桌",
              icon: Icons.soup_kitchen,
              selected: isCaretaker,
              accent: const Color(0xFF894C5C),
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
              accent: const Color(0xFF8B4E38),
              onTap: onEaterClick,
            ),
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
          color: selected ? accent.withValues(alpha: 0.12) : const Color(0xFFFFFCF8),
          borderRadius: BorderRadius.circular(22),
          border: Border.all(
            color: selected ? accent.withValues(alpha: 0.65) : const Color(0xFFD6C1C5).withValues(alpha: 0.72),
            width: selected ? 1.5 : 1.0,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.02),
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
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
                    color: selected ? accent : const Color(0xFF1D1B18),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              subtitle,
              style: const TextStyle(fontSize: 11, color: Color(0xFF524346)),
            ),
          ],
        ),
      ),
    );
  }
}
