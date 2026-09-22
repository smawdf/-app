import 'package:fluid_glass/fluid_glass.dart';
import 'package:flutter/material.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await FluidGlass.ensureInitialized();
  runApp(const FluidGlassTestApp());
}

class FluidGlassTestApp extends StatelessWidget {
  const FluidGlassTestApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: FluidGlassPreviewPage(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class FluidGlassPreviewPage extends StatefulWidget {
  const FluidGlassPreviewPage({super.key});

  @override
  State<FluidGlassPreviewPage> createState() => _FluidGlassPreviewPageState();
}

class _FluidGlassPreviewPageState extends State<FluidGlassPreviewPage> {
  final LayerBackdrop _backdrop = LayerBackdrop();
  int _selectedTab = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white, // 纯白底色
      body: Stack(
        clipBehavior: Clip.none,
        children: [
          // 1. 被折射的内容层（BackdropLayer）
          Positioned.fill(
            child: BackdropLayer(
              backdrop: _backdrop,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(20, 50, 20, 160),
                children: [
                  const Text(
                    "高糖小食 · 纯白真实折射测试",
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: Color(0xFF1D1B20)),
                  ),
                  const SizedBox(height: 16),
                  _demoCard("🍗 蜜汁可乐小鸡翅", "18 🍬 糖币", const Color(0xFFFFF4EC)),
                  _demoCard("🥞 草莓生巧舒芙蕾", "22 🍬 糖币", const Color(0xFFFFF0F5)),
                  _demoCard("🍲 暖胃浓汤番茄牛腩", "36 🍬 糖币", const Color(0xFFF0FDF4)),
                  _demoCard("🍰 法式巴斯克乳酪", "15 🍬 糖币", const Color(0xFFFEFCE8)),
                  _demoCard("🥘 日式肥牛寿喜烧", "42 🍬 糖币", const Color(0xFFFAF5FF)),
                ],
              ),
            ),
          ),

          // 2. 官方原生 Kyant 移植版 LiquidBottomTabs 真实折射底栏
          Positioned(
            left: 20,
            right: 20,
            bottom: 24,
            child: LiquidBottomTabs(
              backdrop: _backdrop,
              selectedTabIndex: _selectedTab,
              onTabSelected: (int index) => setState(() => _selectedTab = index),
              tabsCount: 4,
              children: [
                LiquidBottomTab(
                  onPressed: () => setState(() => _selectedTab = 0),
                  children: const [
                    Icon(Icons.restaurant, size: 22, color: Colors.black),
                    Text('点餐', style: TextStyle(color: Colors.black, fontSize: 11, fontWeight: FontWeight.bold)),
                  ],
                ),
                LiquidBottomTab(
                  onPressed: () => setState(() => _selectedTab = 1),
                  children: const [
                    Icon(Icons.explore, size: 22, color: Colors.black),
                    Text('发现', style: TextStyle(color: Colors.black, fontSize: 11, fontWeight: FontWeight.bold)),
                  ],
                ),
                LiquidBottomTab(
                  onPressed: () => setState(() => _selectedTab = 2),
                  children: const [
                    Icon(Icons.receipt_long, size: 22, color: Colors.black),
                    Text('订单', style: TextStyle(color: Colors.black, fontSize: 11, fontWeight: FontWeight.bold)),
                  ],
                ),
                LiquidBottomTab(
                  onPressed: () => setState(() => _selectedTab = 3),
                  children: const [
                    Icon(Icons.pets, size: 22, color: Colors.black),
                    Text('我的', style: TextStyle(color: Colors.black, fontSize: 11, fontWeight: FontWeight.bold)),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _demoCard(String title, String price, Color bg) {
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: const Color(0x0F000000)),
        boxShadow: const [
          BoxShadow(color: Color(0x08000000), blurRadius: 16, offset: Offset(0, 4)),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 70,
            height: 70,
            decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(16)),
          ),
          const SizedBox(width: 16),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
              const SizedBox(height: 6),
              Text(price, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: Color(0xFFFF5A79))),
            ],
          ),
        ],
      ),
    );
  }
}
