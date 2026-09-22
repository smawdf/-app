import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../theme/cozy_glass.dart';

/// 苹果风格糖糖币流水中心与大额撒糖页 (CandyCoinsPage)
class CandyCoinsPage extends StatefulWidget {
  const CandyCoinsPage({super.key});

  @override
  State<CandyCoinsPage> createState() => _CandyCoinsPageState();
}

class _CandyCoinsPageState extends State<CandyCoinsPage> {
  final _amountCtrl = TextEditingController();
  final _reasonCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshTransactions();
    });
  }

  @override
  void dispose() {
    _amountCtrl.dispose();
    _reasonCtrl.dispose();
    super.dispose();
  }

  void _showCustomRechargeDialog() {
    _amountCtrl.clear();
    _reasonCtrl.clear();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: const Text("专属爱心撒糖 🍬",
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _amountCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: "投喂糖币数量", hintText: "例如：66 或 520"),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _reasonCtrl,
              decoration: const InputDecoration(labelText: "甜蜜撒糖理由", hintText: "例如：今天辛苦啦，请吃大餐！"),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("取消")),
          ElevatedButton(
            onPressed: () async {
              final amount = int.tryParse(_amountCtrl.text.trim()) ?? 0;
              final reason = _reasonCtrl.text.trim();
              if (amount <= 0) return;

              HapticFeedback.mediumImpact();
              Navigator.pop(ctx);

              final ok = await AppState.instance.recharge(
                amount: amount,
                reason: reason.isNotEmpty ? reason : "饲养员爱心投喂",
              );
              if (ok && mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('成功投喂 $amount 糖糖币！对方已收到通知 💕')),
                );
              }
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: CozyTheme.primaryPink,
              elevation: 0,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
            child: const Text("甜蜜确认", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return Scaffold(
      backgroundColor: CozyTheme.pureWhite,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        title: const Text(
          "糖糖币钱包 🍬",
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 18, color: CozyTheme.sweetCocoa),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: ListenableBuilder(
        listenable: state,
        builder: (context, _) {
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 40),
            children: [
              // 1. 余额总览大卡片
              Container(
                padding: const EdgeInsets.symmetric(vertical: 26, horizontal: 20),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFFFFF0F3), Color(0xFFFFF9FA)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(26),
                  border: Border.all(color: const Color(0xFFFFD1DC)),
                ),
                child: Column(
                  children: [
                    const Text("小铺当前可用糖糖币", style: TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
                    const SizedBox(height: 6),
                    Text(
                      "🍬 ${state.candyCoins}",
                      style: const TextStyle(
                        fontSize: 42,
                        fontWeight: FontWeight.w900,
                        color: CozyTheme.primaryPink,
                        letterSpacing: -1,
                      ),
                    ),
                    if (state.isCaretaker) ...[
                      const SizedBox(height: 16),
                      ElevatedButton.icon(
                        onPressed: _showCustomRechargeDialog,
                        icon: const Icon(Icons.favorite, size: 16, color: Colors.white),
                        label: const Text("给吃货自定义撒糖", style: TextStyle(fontWeight: FontWeight.w800, color: Colors.white)),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: CozyTheme.sweetCocoa,
                          elevation: 0,
                          padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 12),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // 2. 详细流水账本
              const Text("收支明细流水",
                  style: TextStyle(fontSize: 14.5, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
              const SizedBox(height: 12),

              if (state.transactions.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 40),
                  child: Center(
                    child: Text("暂无糖币变动记录", style: TextStyle(fontSize: 13, color: CozyTheme.mutedText)),
                  ),
                )
              else
                ...state.transactions.map((t) {
                  final positive = t.amount > 0;
                  return Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: const Color(0x0A000000)),
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 40,
                          height: 40,
                          decoration: BoxDecoration(
                            color: positive ? const Color(0xFFF0FDF4) : CozyTheme.softPink,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Center(
                            child: Text(positive ? "🍬" : "🍽️", style: const TextStyle(fontSize: 18)),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(t.typeLabel,
                                  style: const TextStyle(
                                      fontSize: 13.5, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                              const SizedBox(height: 2),
                              Text(t.description,
                                  style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText)),
                              Text(t.createdAt.toLocal().toString().substring(0, 16),
                                  style: const TextStyle(fontSize: 9.5, color: Color(0xFFB0B0B0))),
                            ],
                          ),
                        ),
                        Text(
                          positive ? "+${t.amount}" : "${t.amount}",
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w900,
                            color: positive ? const Color(0xFF16A34A) : CozyTheme.primaryPink,
                          ),
                        ),
                      ],
                    ),
                  );
                }),
            ],
          );
        },
      ),
    );
  }
}
