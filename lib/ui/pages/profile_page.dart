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
            padding: const EdgeInsets.fromLTRB(20, 14, 20, 130),
            children: [
              // ---- 用户卡片 ----
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: CozyTheme.cardStroke),
                  boxShadow: [
                    BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 16, offset: const Offset(0, 4)),
                  ],
                ),
                child: Row(
                  children: [
                    Container(
                      width: 52,
                      height: 52,
                      decoration: const BoxDecoration(color: CozyTheme.softPink, shape: BoxShape.circle),
                      child: Center(
                        child: Text(user?.isCaretaker == true ? '🍳' : '🍽️',
                            style: const TextStyle(fontSize: 24)),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(user?.nickname ?? '-',
                              style: const TextStyle(
                                  fontSize: 17, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                          const SizedBox(height: 4),
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                decoration: BoxDecoration(
                                  color: CozyTheme.softPink,
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Text(
                                  user?.roleLabel ?? '',
                                  style: const TextStyle(
                                      fontSize: 10.5, fontWeight: FontWeight.w800, color: CozyTheme.primaryPink),
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
              const SizedBox(height: 16),

              // ---- 糖糖币钱包（水滴玻璃） ----
              LiquidDropGlass(
                height: 112,
                borderRadius: BorderRadius.circular(26),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text('两人共有的糖糖币',
                        style: TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
                    const SizedBox(height: 6),
                    Text(
                      '🍬 ${state.candyCoins}',
                      style: const TextStyle(
                          fontSize: 30, fontWeight: FontWeight.w900, color: CozyTheme.primaryPink),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // ---- 饲养员专属：撒糖 ----
              if (state.isCaretaker) ...[
                const Padding(
                  padding: EdgeInsets.only(left: 4, bottom: 10),
                  child: Text('撒糖给 Ta 点菜（饲养员专属）',
                      style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w800, color: CozyTheme.mutedText)),
                ),
                Row(
                  children: [10, 50, 100].map((amount) {
                    return Expanded(
                      child: Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: GestureDetector(
                          onTap: () => _recharge(amount),
                          child: Container(
                            padding: const EdgeInsets.symmetric(vertical: 13),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: CozyTheme.cardStroke),
                            ),
                            child: Column(
                              children: [
                                const Text('🍬', style: TextStyle(fontSize: 17)),
                                const SizedBox(height: 3),
                                Text('+$amount',
                                    style: const TextStyle(
                                        fontSize: 13.5, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                              ],
                            ),
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
                const SizedBox(height: 20),
              ],

              // ---- 情侣绑定信息 ----
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(22),
                  border: Border.all(color: CozyTheme.cardStroke),
                ),
                child: Column(
                  children: [
                    Row(
                      children: [
                        const Text('💕', style: TextStyle(fontSize: 16)),
                        const SizedBox(width: 8),
                        const Expanded(
                          child: Text('情侣绑定',
                              style: TextStyle(
                                  fontSize: 14, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                        ),
                        Text(
                          pair != null && pair.caretakerId.isNotEmpty && pair.eaterId.isNotEmpty
                              ? '双方已就位'
                              : '等待对方加入',
                          style: const TextStyle(fontSize: 11.5, color: CozyTheme.mutedText),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        const Text('邀请码', style: TextStyle(fontSize: 12.5, color: CozyTheme.mutedText)),
                        const Spacer(),
                        Text(
                          pair?.inviteCode ?? '-',
                          style: const TextStyle(
                              fontSize: 17, fontWeight: FontWeight.w900, letterSpacing: 3, color: CozyTheme.primaryPink),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        const Text('饲养员', style: TextStyle(fontSize: 12.5, color: CozyTheme.mutedText)),
                        const Spacer(),
                        Text(
                          (pair?.caretakerId.isNotEmpty ?? false) ? '已绑定' : '空缺',
                          style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: CozyTheme.sweetCocoa),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        const Text('吃货', style: TextStyle(fontSize: 12.5, color: CozyTheme.mutedText)),
                        const Spacer(),
                        Text(
                          (pair?.eaterId.isNotEmpty ?? false) ? '已绑定' : '空缺',
                          style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: CozyTheme.sweetCocoa),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // ---- 糖币流水 ----
              const Padding(
                padding: EdgeInsets.only(left: 4, bottom: 10),
                child: Text('糖币流水',
                    style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w800, color: CozyTheme.mutedText)),
              ),
              if (state.transactions.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 16),
                  child: Center(
                    child: Text('暂无收支记录', style: TextStyle(fontSize: 12.5, color: CozyTheme.mutedText)),
                  ),
                )
              else
                ...state.transactions.map((t) {
                  final positive = t.amount > 0;
                  return Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: CozyTheme.cardStroke),
                    ),
                    child: Row(
                      children: [
                        Text(positive ? '🍬' : '🍽️', style: const TextStyle(fontSize: 16)),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(t.typeLabel,
                                  style: const TextStyle(
                                      fontSize: 13, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                              Text(t.description,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText)),
                            ],
                          ),
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              positive ? '+${t.amount}' : '${t.amount}',
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w900,
                                color: positive ? const Color(0xFF3DA96B) : CozyTheme.primaryPink,
                              ),
                            ),
                            Text('余额 ${t.balance}',
                                style: const TextStyle(fontSize: 10.5, color: CozyTheme.mutedText)),
                          ],
                        ),
                      ],
                    ),
                  );
                }),

              const SizedBox(height: 16),
              Center(
                child: TextButton(
                  onPressed: () => AppState.instance.logout(),
                  child: const Text('退出登录', style: TextStyle(color: CozyTheme.mutedText, fontSize: 13)),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
