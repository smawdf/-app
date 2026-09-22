import 'package:flutter/material.dart';

import '../theme/cozy_glass.dart';

/// 恋爱纪念日与时光记录页面 (AnniversaryPage)
class AnniversaryPage extends StatefulWidget {
  const AnniversaryPage({super.key});

  @override
  State<AnniversaryPage> createState() => _AnniversaryPageState();
}

class _AnniversaryPageState extends State<AnniversaryPage> {
  // 模拟做饭甜蜜时光墙相册
  final List<Map<String, String>> _moments = [
    {
      "dish": "蜜汁可乐小鸡翅",
      "date": "2026-09-21 19:30",
      "note": "今日份幸福开饭！火候恰到好处，鸡翅香甜脱骨~",
      "emoji": "🍗",
      "chef": "饲养员 小金毛",
    },
    {
      "dish": "草莓生巧舒芙蕾",
      "date": "2026-09-20 20:15",
      "note": "周末特调小甜品，两只小狗一起吃饱饱！",
      "emoji": "🥞",
      "chef": "饲养员 小金毛",
    },
    {
      "dish": "暖胃浓汤番茄牛腩",
      "date": "2026-09-18 18:45",
      "note": "秋风渐凉，砂锅慢火炖足两小时，汤底酸甜浓郁。",
      "emoji": "🍲",
      "chef": "饲养员 小金毛",
    },
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: CozyTheme.pureWhite,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        title: const Text(
          "恋爱纪念日 💕",
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 18, color: CozyTheme.sweetCocoa),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 10, 20, 40),
        children: [
          // 1. 顶部恋爱天数大卡片
          Container(
            padding: const EdgeInsets.symmetric(vertical: 28, horizontal: 20),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFFFFF0F3), Color(0xFFFFF9FA)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(26),
              border: Border.all(color: const Color(0xFFFFD1DC)),
              boxShadow: const [
                BoxShadow(color: Color(0x06000000), blurRadius: 18, offset: Offset(0, 4)),
              ],
            ),
            child: Column(
              children: [
                const Text("相恋开饭至今", style: TextStyle(fontSize: 13, color: CozyTheme.mutedText)),
                const SizedBox(height: 6),
                const Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Text("520",
                        style: TextStyle(
                            fontSize: 48, fontWeight: FontWeight.w900, color: CozyTheme.primaryPink, letterSpacing: -1)),
                    SizedBox(width: 4),
                    Text("天",
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: CozyTheme.primaryPink)),
                  ],
                ),
                const SizedBox(height: 8),
                const Text("始于 2025 年 4 月 20 日",
                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: CozyTheme.sweetCocoa)),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // 2. 做饭回忆墙
          const Row(
            children: [
              Text("出锅时光回忆墙 📸",
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
              Spacer(),
              Text("共 3 次甜蜜记录", style: TextStyle(fontSize: 12, color: CozyTheme.mutedText)),
            ],
          ),
          const SizedBox(height: 12),

          ..._moments.map((m) {
            return Container(
              margin: const EdgeInsets.only(bottom: 16),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: const Color(0x0A000000)),
                boxShadow: const [
                  BoxShadow(color: Color(0x03000000), blurRadius: 12, offset: Offset(0, 3)),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: const Color(0xFFFAFAFA),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Center(child: Text(m["emoji"]!, style: const TextStyle(fontSize: 24))),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(m["dish"]!,
                                style: const TextStyle(
                                    fontSize: 15, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                            const SizedBox(height: 2),
                            Text(m["date"]!, style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText)),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFF4EC),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(m["chef"]!,
                            style: const TextStyle(
                                fontSize: 10, fontWeight: FontWeight.w700, color: Color(0xFFFF7A00))),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF9F9FB),
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: Text(
                      m["note"]!,
                      style: const TextStyle(fontSize: 12.5, color: Color(0xFF444444), height: 1.4),
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }
}
