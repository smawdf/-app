import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../candy/candy_coins_page.dart';
import '../couple/anniversary_page.dart';
import '../menu/menu_management_page.dart';
import '../theme/cozy_glass.dart';

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
    // 持久化身份到云端 profiles.selected_role，再刷新本地状态
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
        final isCaretaker = user?.isCaretaker ?? true;

        return RefreshIndicator(
          color: CozyTheme.primaryPink,
          onRefresh: () => state.loadMe(),
          child: CustomScrollView(
            slivers: [
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 12, 20, 130),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // 顶部大标题
                      const Text(
                        "今天也要一起好好吃饭 💕",
                        style: TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.w900,
                          color: CozyTheme.sweetCocoa,
                          letterSpacing: -0.5,
                        ),
                      ),
                      const SizedBox(height: 14),

                      // 1. Apple 风格情侣关系主画板
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.symmetric(vertical: 22, horizontal: 16),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFAFAFA),
                          borderRadius: BorderRadius.circular(26),
                          border: Border.all(color: const Color(0x0A000000)),
                          boxShadow: const [
                            BoxShadow(
                              color: Color(0x06000000),
                              blurRadius: 20,
                              offset: Offset(0, 4),
                            ),
                          ],
                        ),
                        child: Column(
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                // 当前用户头像
                                _buildDogAvatar(
                                  emoji: isCaretaker ? "🐶" : "🐩",
                                  name: user?.nickname.isNotEmpty == true ? user!.nickname : "我",
                                  roleTag: user?.roleLabel ?? "角色",
                                  tagColor: CozyTheme.primaryPink,
                                  tagBg: CozyTheme.softPink,
                                ),

                                // 中间爱心动效
                                Padding(
                                  padding: const EdgeInsets.symmetric(horizontal: 18),
                                  child: const Text("❤️", style: TextStyle(fontSize: 24))
                                      .animate(onPlay: (c) => c.repeat(reverse: true))
                                      .scale(
                                        duration: 1200.ms,
                                        begin: const Offset(0.9, 0.9),
                                        end: const Offset(1.15, 1.15),
                                        curve: Curves.easeInOut,
                                      ),
                                ),

                                // 伴侣头像位（未绑定时为空心邀请位）
                                if (isPaired && pair!.isFullyBound)
                                  _buildDogAvatar(
                                    emoji: isCaretaker ? "🐩" : "🐶",
                                    name: isCaretaker ? "吃货伴侣" : "做饭伴侣",
                                    roleTag: isCaretaker ? "吃货" : "饲养员",
                                    tagColor: const Color(0xFFFF7A00),
                                    tagBg: const Color(0xFFFFF4EC),
                                  )
                                else
                                  GestureDetector(
                                    onTap: () => widget.onNavigateTab(4), // 跳到我的/配对
                                    child: Column(
                                      children: [
                                        Container(
                                          width: 64,
                                          height: 64,
                                          decoration: BoxDecoration(
                                            color: Colors.white,
                                            shape: BoxShape.circle,
                                            border: Border.all(
                                              color: CozyTheme.primaryPink.withValues(alpha: 0.5),
                                              width: 1.5,
                                              strokeAlign: BorderSide.strokeAlignInside,
                                            ),
                                          ),
                                          child: const Center(
                                            child: Icon(Icons.add, color: CozyTheme.primaryPink, size: 26),
                                          ),
                                        ),
                                        const SizedBox(height: 6),
                                        const Text(
                                          "邀请伴侣",
                                          style: TextStyle(
                                            fontSize: 12,
                                            fontWeight: FontWeight.w800,
                                            color: CozyTheme.primaryPink,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                              ],
                            ),
                            const SizedBox(height: 14),

                            // 在一起的天数胶囊（点击可进入纪念日时光墙）
                            GestureDetector(
                              onTap: () {
                                HapticFeedback.lightImpact();
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(builder: (_) => const AnniversaryPage()),
                                );
                              },
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                                decoration: BoxDecoration(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.circular(20),
                                  border: Border.all(color: const Color(0x0A000000)),
                                  boxShadow: const [
                                    BoxShadow(
                                      color: Color(0x04000000),
                                      blurRadius: 8,
                                      offset: Offset(0, 2),
                                    ),
                                  ],
                                ),
                                child: Text(
                                  isPaired ? "一起开饭的第 520 天 💕" : "绑定伴侣后开启小家小铺",
                                  style: const TextStyle(
                                    fontSize: 11.5,
                                    fontWeight: FontWeight.w800,
                                    color: Color(0xFF333333),
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 18),

                      // 2. 身份选择切换
                      const Text(
                        "身份选择",
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w800,
                          color: CozyTheme.sweetCocoa,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Row(
                        children: [
                          Expanded(
                            child: _buildRoleCard(
                              title: "我是饲养员 🍳",
                              subtitle: "掌勺做饭、上传菜单与推进状态",
                              isSelected: isCaretaker,
                              onTap: () => _switchRole("caretaker"),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _buildRoleCard(
                              title: "我是吃货 🍽️",
                              subtitle: "挑选美味、提交订单与开怀享用",
                              isSelected: !isCaretaker,
                              onTap: () => _switchRole("eater"),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 20),

                      // 3. 苹果风格四格快捷入口
                      const Text(
                        "快捷入口",
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w800,
                          color: CozyTheme.sweetCocoa,
                        ),
                      ),
                      const SizedBox(height: 10),
                      GridView.count(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        crossAxisCount: 2,
                        mainAxisSpacing: 10,
                        crossAxisSpacing: 10,
                        childAspectRatio: 1.45,
                        children: [
                          _buildQuickCard(
                            emoji: "🍲",
                            title: "进店点餐",
                            desc: "挑选今天想吃的好菜",
                            onTap: () => widget.onNavigateTab(1),
                          ),
                          _buildQuickCard(
                            emoji: "🏪",
                            title: "小店管理",
                            desc: "上新菜品与维护菜单",
                            onTap: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(builder: (_) => const MenuManagementPage()),
                              );
                            },
                          ),
                          _buildQuickCard(
                            emoji: "🎂",
                            title: "恋爱纪念日",
                            desc: "恋爱天数与做饭回忆墙",
                            onTap: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(builder: (_) => const AnniversaryPage()),
                              );
                            },
                          ),
                          _buildQuickCard(
                            emoji: "🍬",
                            title: "糖币钱包",
                            desc: "甜蜜撒糖与收支明细",
                            onTap: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(builder: (_) => const CandyCoinsPage()),
                              );
                            },
                          ),
                        ],
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

  Widget _buildDogAvatar({
    required String emoji,
    required String name,
    required String roleTag,
    required Color tagColor,
    required Color tagBg,
  }) {
    return Column(
      children: [
        Container(
          width: 64,
          height: 64,
          decoration: const BoxDecoration(
            color: Colors.white,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: Color(0x0A000000),
                blurRadius: 12,
                offset: Offset(0, 4),
              ),
            ],
          ),
          child: Center(
            child: Text(emoji, style: const TextStyle(fontSize: 32)),
          ),
        ),
        const SizedBox(height: 6),
        Text(
          name,
          style: const TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w800,
            color: Color(0xFF111111),
          ),
        ),
        const SizedBox(height: 2),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
          decoration: BoxDecoration(
            color: tagBg,
            borderRadius: BorderRadius.circular(8),
          ),
          child: Text(
            roleTag,
            style: TextStyle(
              fontSize: 9.5,
              fontWeight: FontWeight.w800,
              color: tagColor,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildRoleCard({
    required String title,
    required String subtitle,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 220),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFFFFF9FA) : Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isSelected ? CozyTheme.primaryPink : const Color(0x0F000000),
            width: isSelected ? 1.6 : 1.0,
          ),
          boxShadow: const [
            BoxShadow(
              color: Color(0x04000000),
              blurRadius: 10,
              offset: Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: TextStyle(
                fontSize: 13.5,
                fontWeight: FontWeight.w900,
                color: isSelected ? CozyTheme.primaryPink : const Color(0xFF111111),
              ),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              style: const TextStyle(
                fontSize: 10.5,
                color: CozyTheme.mutedText,
                height: 1.3,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildQuickCard({
    required String emoji,
    required String title,
    required String desc,
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
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: const Color(0x0A000000)),
          boxShadow: const [
            BoxShadow(
              color: Color(0x04000000),
              blurRadius: 12,
              offset: Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(emoji, style: const TextStyle(fontSize: 22)),
            const SizedBox(height: 6),
            Text(
              title,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w900,
                color: Color(0xFF111111),
              ),
            ),
            Text(
              desc,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 10,
                color: CozyTheme.mutedText,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
