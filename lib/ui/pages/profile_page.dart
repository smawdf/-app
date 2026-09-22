import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../theme/cozy_glass.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshTransactions();
    });
  }

  Future<void> _recharge(int amount) async {
    HapticFeedback.mediumImpact();
    final state = AppState.instance;
    final ok = await state.recharge(amount: amount, reason: '饲养员投喂');
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? (state.toast ?? '撒糖成功') : (state.error ?? '撒糖失败'))),
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

        return RefreshIndicator(
          color: CozyTheme.primaryPink,
          onRefresh: () => state.refreshAll(),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 140),
            children: [
              // 大标题
              const Text(
                "个人与小店 🐾",
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w900,
                  color: CozyTheme.sweetCocoa,
                  letterSpacing: -0.5,
                ),
              ),
              const SizedBox(height: 14),

              // ---- 1. 用户头像卡片 ----
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(22),
                  border: Border.all(color: const Color(0x0A000000)),
                  boxShadow: const [
                    BoxShadow(color: Color(0x04000000), blurRadius: 14, offset: Offset(0, 3)),
                  ],
                ),
                child: Row(
                  children: [
                    Container(
                      width: 52,
                      height: 52,
                      decoration: const BoxDecoration(color: Color(0xFFFAFAFA), shape: BoxShape.circle),
                      child: Center(
                        child: Text(user?.isCaretaker == true ? '🐶' : '🐩',
                            style: const TextStyle(fontSize: 26)),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(user?.nickname ?? '未登录',
                              style: const TextStyle(
                                  fontSize: 16.5, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                          const SizedBox(height: 4),
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2.5),
                                decoration: BoxDecoration(
                                  color: user?.isCaretaker == true ? CozyTheme.softPink : const Color(0xFFFFF4EC),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Text(
                                  user?.roleLabel ?? '',
                                  style: TextStyle(
                                    fontSize: 10,
                                    fontWeight: FontWeight.w800,
                                    color: user?.isCaretaker == true ? CozyTheme.primaryPink : const Color(0xFFFF7A00),
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text('@${user?.username ?? ''}',
                                  style: const TextStyle(fontSize: 11.5, color: CozyTheme.mutedText)),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // ---- 2. 糖糖币钱包卡片 ----
              Container(
                padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: const Color(0x0A000000)),
                  boxShadow: const [
                    BoxShadow(color: Color(0x04000000), blurRadius: 16, offset: Offset(0, 4)),
                  ],
                ),
                child: Column(
                  children: [
                    const Text('小铺共有糖糖币', style: TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
                    const SizedBox(height: 6),
                    Text(
                      '🍬 ${state.candyCoins}',
                      style: const TextStyle(
                        fontSize: 32,
                        fontWeight: FontWeight.w900,
                        color: CozyTheme.primaryPink,
                      ),
                    ),
                    if (state.isCaretaker) ...[
                      const SizedBox(height: 12),
                      const Text('饲养员快捷投喂：', style: TextStyle(fontSize: 11, color: CozyTheme.mutedText)),
                      const SizedBox(height: 8),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [10, 50, 100].map((amount) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 5),
                            child: GestureDetector(
                              onTap: () => _recharge(amount),
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                                decoration: BoxDecoration(
                                  color: CozyTheme.softPink,
                                  borderRadius: BorderRadius.circular(14),
                                ),
                                child: Text('+$amount 🍬',
                                    style: const TextStyle(
                                        fontSize: 12, fontWeight: FontWeight.w800, color: CozyTheme.primaryPink)),
                              ),
                            ),
                          );
                        }).toList(),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // ---- 3. Apple 风格分组菜单 (Grouped Table) ----
              Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(22),
                  border: Border.all(color: const Color(0x0A000000)),
                ),
                child: Column(
                  children: [
                    _menuRow(
                      icon: "🏪",
                      label: "管理小店菜单与价格",
                      trailing: const Icon(Icons.chevron_right, size: 18, color: Color(0xFFC7C7CC)),
                      onTap: () {},
                    ),
                    const Divider(height: 1, indent: 46, color: Color(0xFFF2F2F7)),
                    _menuRow(
                      icon: "💕",
                      label: "专属情侣邀请码",
                      trailing: Text(
                        pair?.inviteCode ?? "未生成",
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 2,
                          color: CozyTheme.primaryPink,
                        ),
                      ),
                      onTap: () {},
                    ),
                    const Divider(height: 1, indent: 46, color: Color(0xFFF2F2F7)),
                    _menuRow(
                      icon: "🎂",
                      label: "恋爱纪念日",
                      trailing: const Icon(Icons.chevron_right, size: 18, color: Color(0xFFC7C7CC)),
                      onTap: () {},
                    ),
                    const Divider(height: 1, indent: 46, color: Color(0xFFF2F2F7)),
                    _menuRow(
                      icon: "🚪",
                      label: "退出当前登录",
                      trailing: const Icon(Icons.chevron_right, size: 18, color: Color(0xFFC7C7CC)),
                      onTap: () => AppState.instance.logout(),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // ---- 4. 糖币流水 ----
              if (state.transactions.isNotEmpty) ...[
                const Padding(
                  padding: EdgeInsets.only(left: 4, bottom: 8),
                  child: Text('收支流水账本',
                      style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                ),
                ...state.transactions.map((t) {
                  final positive = t.amount > 0;
                  return Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: const Color(0x08000000)),
                    ),
                    child: Row(
                      children: [
                        Text(positive ? '🍬' : '🍽️', style: const TextStyle(fontSize: 15)),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(t.typeLabel,
                                  style: const TextStyle(
                                      fontSize: 12.5, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                              Text(t.description,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(fontSize: 10.5, color: CozyTheme.mutedText)),
                            ],
                          ),
                        ),
                        Text(
                          positive ? '+${t.amount}' : '${t.amount}',
                          style: TextStyle(
                            fontSize: 13.5,
                            fontWeight: FontWeight.w900,
                            color: positive ? const Color(0xFF16A34A) : CozyTheme.primaryPink,
                          ),
                        ),
                      ],
                    ),
                  );
                }),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _menuRow({
    required String icon,
    required String label,
    required Widget trailing,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
        child: Row(
          children: [
            Text(icon, style: const TextStyle(fontSize: 16)),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                label,
                style: const TextStyle(fontSize: 13.5, fontWeight: FontWeight.w600, color: Color(0xFF111111)),
              ),
            ),
            trailing,
          ],
        ),
      ),
    );
  }
}
