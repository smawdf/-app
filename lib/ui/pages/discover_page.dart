import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

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
  List<Map<String, dynamic>> _recipes = [];
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _search("");
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _search(String kw) async {
    setState(() => _loading = true);
    final list = await AppState.instance.searchRemoteRecipes(kw);
    if (!mounted) return;
    setState(() {
      _recipes = list;
      _loading = false;
    });
  }

  Future<void> _addToShop(Map<String, dynamic> recipe) async {
    HapticFeedback.mediumImpact();
    final state = AppState.instance;

    final ok = await state.addDish(
      name: recipe["name"] as String,
      price: (recipe["price"] as num).toDouble(),
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
                    Container(
                      decoration: BoxDecoration(
                        color: const Color(0xFFF6F6F7),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: const Color(0x08000000)),
                      ),
                      child: TextField(
                        controller: _searchCtrl,
                        onSubmitted: _search,
                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                        decoration: InputDecoration(
                          hintText: "搜索菜品、做法、食材、减脂餐...",
                          hintStyle: const TextStyle(fontSize: 13, color: CozyTheme.mutedText),
                          prefixIcon: const Icon(Icons.search, size: 20, color: CozyTheme.mutedText),
                          suffixIcon: _searchCtrl.text.isNotEmpty
                              ? GestureDetector(
                                  onTap: () {
                                    _searchCtrl.clear();
                                    _search("");
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

            if (_loading)
              const SliverFillRemaining(
                child: Center(
                  child: CircularProgressIndicator(color: CozyTheme.primaryPink),
                ),
              )
            else if (_recipes.isEmpty)
              const SliverFillRemaining(
                child: Center(
                  child: Text("未搜到相关菜谱，换个关键词试试吧", style: TextStyle(color: CozyTheme.mutedText)),
                ),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 120),
                sliver: SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (context, index) {
                      final item = _recipes[index];
                      final isAdded = item["added"] == true;

                      return Container(
                        margin: const EdgeInsets.only(bottom: 16),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(22),
                          border: Border.all(color: const Color(0x0A000000)),
                          boxShadow: const [
                            BoxShadow(color: Color(0x04000000), blurRadius: 14, offset: Offset(0, 4)),
                          ],
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              height: 100,
                              width: double.infinity,
                              decoration: const BoxDecoration(
                                color: Color(0xFFFAFAFA),
                                borderRadius: BorderRadius.vertical(top: Radius.circular(21)),
                              ),
                              child: Center(
                                child: Text(item["emoji"] as String? ?? "🍲", style: const TextStyle(fontSize: 48)),
                              ),
                            ),
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
                                          item["name"] as String? ?? "",
                                          style: const TextStyle(
                                            fontSize: 15.5,
                                            fontWeight: FontWeight.w900,
                                            color: CozyTheme.sweetCocoa,
                                          ),
                                        ),
                                      ),
                                      Text(
                                        "${((item["price"] as num?) ?? 10).toStringAsFixed(0)} 🍬",
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
                                    item["desc"] as String? ?? "",
                                    style: const TextStyle(fontSize: 12, color: CozyTheme.mutedText, height: 1.35),
                                  ),
                                  const SizedBox(height: 12),
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                      Row(
                                        children: [
                                          _tag("⏱️ ${item["time"] ?? "20分钟"}"),
                                          const SizedBox(width: 6),
                                          _tag("🔥 ${item["difficulty"] ?? "家常"}"),
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
                      );
                    },
                    childCount: _recipes.length,
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
