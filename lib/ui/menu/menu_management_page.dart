import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

/// 苹果风格小店与菜单管理页面 (MenuManagementPage)
class MenuManagementPage extends StatefulWidget {
  const MenuManagementPage({super.key});

  @override
  State<MenuManagementPage> createState() => _MenuManagementPageState();
}

class _MenuManagementPageState extends State<MenuManagementPage> {
  final _nameCtrl = TextEditingController();
  final _priceCtrl = TextEditingController();
  final _descCtrl = TextEditingController();

  void _showAddDishDialog() {
    _nameCtrl.clear();
    _priceCtrl.clear();
    _descCtrl.clear();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: const Text("上新美味菜品 🍳",
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _nameCtrl,
              decoration: const InputDecoration(labelText: "菜品名称", hintText: "例如：脆皮黄油烤鸡"),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _priceCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: "糖币价格", hintText: "例如：28"),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _descCtrl,
              decoration: const InputDecoration(labelText: "特色描述", hintText: "例如：外焦里嫩，秘制酱汁"),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("取消")),
          ElevatedButton(
            onPressed: () async {
              final name = _nameCtrl.text.trim();
              final price = double.tryParse(_priceCtrl.text.trim()) ?? 10.0;
              final desc = _descCtrl.text.trim();
              if (name.isEmpty) return;

              HapticFeedback.mediumImpact();
              Navigator.pop(ctx);

              final ok = await AppState.instance.addDish(
                name: name,
                price: price,
                description: desc,
                emoji: "🍽️",
              );
              if (ok && mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('已成功上架【$name】！')),
                );
              }
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: CozyTheme.primaryPink,
              elevation: 0,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
            child: const Text("确认上架", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  Future<void> _deleteDish(MenuItem item) async {
    HapticFeedback.lightImpact();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        title: Text("确认下架【${item.name}】？",
            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
        content: const Text("下架后吃货将无法继续在点餐页选择该菜品。"),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text("取消")),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text("确认下架", style: TextStyle(color: Color(0xFFDC2626))),
          ),
        ],
      ),
    );

    if (ok == true) {
      await AppState.instance.deleteDish(item.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('已下架【${item.name}】')),
        );
      }
    }
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
          "小店与菜单管理 🏪",
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 18, color: CozyTheme.sweetCocoa),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_circle_outline, color: CozyTheme.primaryPink),
            tooltip: "上新菜品",
            onPressed: _showAddDishDialog,
          ),
        ],
      ),
      body: ListenableBuilder(
        listenable: state,
        builder: (context, _) {
          final dishes = state.menu;

          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 40),
            children: [
              // 1. 小店名称与公告展示
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(22),
                  border: Border.all(color: const Color(0x0A000000)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Text("🏪", style: TextStyle(fontSize: 22)),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(state.shop?.name ?? "我们的小店",
                                  style: const TextStyle(
                                      fontSize: 16, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                              const SizedBox(height: 2),
                              Text(state.shop?.announcement ?? "今天也要一起好好吃饭",
                                  style: const TextStyle(fontSize: 11.5, color: CozyTheme.mutedText)),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 18),

              // 2. 菜单管理列表
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text("在售菜品清单 (${dishes.length} 道)",
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: CozyTheme.sweetCocoa)),
                  GestureDetector(
                    onTap: _showAddDishDialog,
                    child: const Text("+ 上新菜品",
                        style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w800, color: CozyTheme.primaryPink)),
                  ),
                ],
              ),
              const SizedBox(height: 12),

              ...dishes.map((dish) {
                return Container(
                  margin: const EdgeInsets.only(bottom: 12),
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: const Color(0x0A000000)),
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: const Color(0xFFFAFAFA),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Center(
                          child: Text(dish.imageUrl.isNotEmpty ? dish.imageUrl : "🍽️",
                              style: const TextStyle(fontSize: 22)),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(dish.name,
                                style: const TextStyle(
                                    fontSize: 14.5, fontWeight: FontWeight.w800, color: CozyTheme.sweetCocoa)),
                            Text("${dish.price.toStringAsFixed(0)} 🍬  ·  ${dish.description}",
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 11, color: CozyTheme.mutedText)),
                          ],
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.delete_outline, size: 20, color: Color(0xFFDC2626)),
                        onPressed: () => _deleteDish(dish),
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
