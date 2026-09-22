import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_animate/flutter_animate.dart';

import '../../data/app_state.dart';
import '../theme/cozy_glass.dart';

class DiscoverPage extends StatefulWidget {
  final VoidCallback onGoToOrdering;

  const DiscoverPage({super.key, required this.onGoToOrdering});

  @override
  State<DiscoverPage> createState() => _DiscoverPageState();
}

class _DiscoverPageState extends State<DiscoverPage> {
  final _searchCtrl = TextEditingController();
  String _keyword = "";

  // 灵感菜谱示例库（支持一键加入我的小店）
  final List<Map<String, dynamic>> _allRecipes = [
    {
      "name": "关东风味肥牛寿喜烧",
      "desc": "经典关东甜咸风味，热气腾腾，冬天情侣窝在一起吃最暖胃。",
      "time": "25 分钟",
      "difficulty": "简单",
      "price": 42.0,
      "emoji": "🥘",
      "bg": const Color(0xFFFFF4EC),
      "added": false,
    },
    {
      "name": "多汁白桃乌龙暴打冻饮",
      "desc": "手捣新鲜白桃果肉，配清香高山乌龙，0 卡糖清爽低负担。",
      "time": "10 分钟",
      "difficulty": "极快",
      "price": 12.0,
      "emoji": "🥤",
      "bg": const Color(0xFFEFF6FF),
      "added": false,
    },
    {
      "name": "鲜香滑嫩黑椒雪花牛肉粒",
      "desc": "外焦里嫩，浓郁黑椒黄油汁包裹杏鲍菇，下饭绝配。",
      "time": "20 分钟",
      "difficulty": "家常",
      "price": 38.0,
      "emoji": "🥩",
      "bg": const Color(0xFFFEF2F2),
      "added": false,
    },
    {
      "name": "焦糖海盐流心松饼塔",
      "desc": "松软云朵口感，淋上手熬微咸焦糖酱与香草冰淇淋。",
      "time": "18 分钟",
      "difficulty": "甜品",
      "price": 26.0,
      "emoji": "🥞",
      "bg": const Color(0xFFFFFBEB),
      "added": false,
    },
    {
      "name": "暖心鲜甜上汤娃娃菜",
      "desc": "皮蛋火腿慢火吊高汤，清润鲜甜，做饭方拿手好汤。",
      "time": "15 分钟",
      "difficulty": "快手",
      "price": 16.0,
      "emoji": "🥬",
      "bg": const Color(0xFFF0FDF4),
      "added": false,
    },
  ];

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _addToShop(Map<String, dynamic> recipe) async {
    HapticFeedback.mediumImpact();
    final state = AppState.instance;

    final ok = await state.addDish(
      name: recipe["name"] as String,
      price: recipe["price"] as double,
      description: recipe["desc"] as String,
      emoji: recipe["emoji"] as String,
    );

    if (!mounted) return;
    if (ok) {
      setState(() {
        recipe["added"] = true;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('已成功将【${recipe["name"]}】加入我的小店！'),
          backgroundColor: CozyTheme.sweetCocoa,
          action: SnackBarAction(
            label: '去点单',
            textColor: CozyTheme.primaryPink,
            onPressed: widget.onGoToOrdering,
          ),
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(state.error ?? '添加失败，请确认是否为饲养员身份')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _allRecipes.where((r) {
      if (_keyword.isEmpty) return true;
      final name = r["name"] as String;
      final desc = r["desc"] as String;
      return name.contains(_keyword) || desc.contains(_keyword);
    }).toList();

    return Scaffold(
      backgroundColor: CozyTheme.pureWhite,
      body: SafeArea(
        bottom: false,
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 大标题
                    const Text(
                      "发现灵感好菜 ✨",
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w900,
                        color: CozyTheme.sweetCocoa,
                        letterSpacing: -0.5,
                      ),
                    ),
                    const SizedBox(height: 12),

                    // 纯白极简搜索框
                    Container(
                      decoration: BoxDecoration(
                        color: const Color(0xFFF6F6F7),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: const Color(0x08000000)),
                      ),
                      child: TextField(
                        controller: _searchCtrl,
                        onChanged: (val) => setState(() => _keyword = val.trim()),
                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                        decoration: InputDecoration(
                          hintText: "搜索菜品、做法、食材、减脂餐...",
                          hintStyle: const TextStyle(fontSize: 13, color: CozyTheme.mutedText),
                          prefixIcon: const Icon(Icons.search, size: 20, color: CozyTheme.mutedText),
                          suffixIcon: _keyword.isNotEmpty
                              ? GestureDetector(
                                  onTap: () {
                                    _searchCtrl.clear();
                                    setState(() => _keyword = "");
                                  },
                                  child: const Icon(Icons.close, size: 18, color: CozyTheme.mutedText),
                                )
                              : null,
                          border: InputBorder.none,
                          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            // 菜谱列表流
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 120),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) {
                    final item = filtered[index];
                    final isAdded = item["added"] as bool;

                    return Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(22),
                        border: Border.all(color: const Color(0x0A000000)),
                        boxShadow: const [
                          BoxShadow(
                            color: Color(0x04000000),
                            blurRadius: 14,
                            offset: Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // 顶部大图展位
                          Container(
                            height: 100,
                            width: double.infinity,
                            decoration: BoxDecoration(
                              color: item["bg"] as Color,
                              borderRadius: const BorderRadius.vertical(top: Radius.circular(21)),
                            ),
                            child: Center(
                              child: Text(item["emoji"] as String, style: const TextStyle(fontSize: 48)),
                            ),
                          ),

                          // 详细信息
                          Padding(
                            padding: const EdgeInsets.all(14),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Expanded(
                                      child: Text(
                                        item["name"] as String,
                                        style: const TextStyle(
                                          fontSize: 15.5,
                                          fontWeight: FontWeight.w900,
                                          color: CozyTheme.sweetCocoa,
                                        ),
                                      ),
                                    ),
                                    Text(
                                      "${(item["price"] as double).toStringAsFixed(0)} 🍬",
                                      style: const TextStyle(
                                        fontSize: 14.5,
                                        fontWeight: FontWeight.w900,
                                        color: CozyTheme.primaryPink,
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                Text(
                                  item["desc"] as String,
                                  style: const TextStyle(
                                    fontSize: 12,
                                    color: CozyTheme.mutedText,
                                    height: 1.35,
                                  ),
                                ),
                                const SizedBox(height: 12),

                                // 底部标签与操作
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Row(
                                      children: [
                                        _tag("⏱️ ${item["time"]}"),
                                        const SizedBox(width: 6),
                                        _tag("🔥 ${item["difficulty"]}"),
                                      ],
                                    ),
                                    GestureDetector(
                                      onTap: isAdded ? null : () => _addToShop(item),
                                      child: Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
                                        decoration: BoxDecoration(
                                          color: isAdded ? const Color(0xFFF0FDF4) : CozyTheme.softPink,
                                          borderRadius: BorderRadius.circular(12),
                                          border: Border.all(
                                            color: isAdded
                                                ? const Color(0xFF86EFAC)
                                                : CozyTheme.primaryPink.withValues(alpha: 0.3),
                                          ),
                                        ),
                                        child: Row(
                                          children: [
                                            Icon(
                                              isAdded ? Icons.check : Icons.add,
                                              size: 14,
                                              color: isAdded ? const Color(0xFF16A34A) : CozyTheme.primaryPink,
                                            ),
                                            const SizedBox(width: 4),
                                            Text(
                                              isAdded ? "已在小店" : "加进小店",
                                              style: TextStyle(
                                                fontSize: 11.5,
                                                fontWeight: FontWeight.w800,
                                                color: isAdded ? const Color(0xFF16A34A) : CozyTheme.primaryPink,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ).animate().fadeIn(delay: (index * 40).ms, duration: 320.ms).slideY(begin: 0.08, end: 0);
                  },
                  childCount: filtered.length,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _tag(String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F7F8),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w600, color: CozyTheme.mutedText),
      ),
    );
  }
}
