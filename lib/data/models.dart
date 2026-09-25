/// 与 Go 后端 (orderdisk-server) 一一对应的数据模型
library;

import 'dart:convert';

/// 网络/业务异常统一类型（后端与 Supabase 层共用）
class ApiException implements Exception {
  final String message;
  final int? statusCode;
  ApiException(this.message, {this.statusCode});

  @override
  String toString() => message;
}

class AppUser {
  final String id;
  final String username;
  final String nickname;
  final String avatarUrl;
  final String role; // caretaker | eater
  final String pairId;

  AppUser({
    required this.id,
    required this.username,
    required this.nickname,
    required this.avatarUrl,
    required this.role,
    required this.pairId,
  });

  bool get isCaretaker => role == 'caretaker';

  String get roleLabel => isCaretaker ? '饲养员' : '吃货';

  AppUser copyWithRole(String newRole) => AppUser(
        id: id,
        username: username,
        nickname: nickname,
        avatarUrl: avatarUrl,
        role: newRole,
        pairId: pairId,
      );

  factory AppUser.fromJson(Map<String, dynamic> j) => AppUser(
        id: j['id'] ?? '',
        username: j['username'] ?? '',
        nickname: j['nickname'] ?? '',
        avatarUrl: j['avatar_url'] ?? '',
        role: j['role'] ?? 'eater',
        pairId: j['pair_id'] ?? '',
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'username': username,
        'nickname': nickname,
        'avatar_url': avatarUrl,
        'role': role,
        'pair_id': pairId,
      };

  String toJsonString() => jsonEncode(toJson());

  static AppUser? fromJsonString(String raw) {
    try {
      return AppUser.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (_) {
      return null;
    }
  }
}

class CouplePair {
  final String id;
  final String inviteCode;
  final String caretakerId;
  final String eaterId;
  final int candyCoins;

  /// 【真机修正】伴侣的昵称与头像。
  ///
  /// 真机首页一直显示「伴侣资料同步中」、我的页显示「对方」，是因为
  /// `SupabaseApi.me()` 虽然已经按 pair_id 查到了对方那一行
  /// （`profiles.select('user_id,nickname,candy_coins')`），却只取了 user_id 和糖币，
  /// 昵称被丢掉了，`CouplePair` 也没有地方放。原生 `partnerName.ifBlank { "对方" }`
  /// 因此永远走到兜底分支。这里把昵称/头像接出来。
  final String partnerName;
  final String partnerAvatarUrl;

  CouplePair({
    required this.id,
    required this.inviteCode,
    required this.caretakerId,
    required this.eaterId,
    required this.candyCoins,
    this.partnerName = '',
    this.partnerAvatarUrl = '',
  });

  bool get isFullyBound => caretakerId.isNotEmpty && eaterId.isNotEmpty;

  factory CouplePair.fromJson(Map<String, dynamic> j) => CouplePair(
        id: j['id'] ?? '',
        inviteCode: j['invite_code'] ?? '',
        caretakerId: j['caretaker_id'] ?? '',
        eaterId: j['eater_id'] ?? '',
        candyCoins: j['candy_coins'] ?? 0,
        partnerName: j['partner_name'] ?? '',
        partnerAvatarUrl: j['partner_avatar_url'] ?? '',
      );
}

class Shop {
  final String id;
  final String name;
  final String announcement;
  final String coverUrl;

  Shop({required this.id, required this.name, required this.announcement, required this.coverUrl});

  factory Shop.fromJson(Map<String, dynamic> j) => Shop(
        id: j['id'] ?? '',
        name: j['name'] ?? '',
        announcement: j['announcement'] ?? '',
        coverUrl: j['cover_url'] ?? '',
      );
}

class MenuItem {
  final String id;
  final String name;
  final String description;
  final double price;
  final String imageUrl;
  final int salesCount;
  final bool isAvailable;

  /// 【真机修正】菜品分类。
  /// 原生 `menu_dishes.category` 是单列文本，`categories` 本身是原生侧由
  /// `getCategoryNames()` 聚合出来的派生列表；Flutter 之前漏了这个字段，
  /// 只能用会话内的 `_categoryByDishName` 临时顶替，导致冷启动后
  /// 「分类管理」永远是空的、点餐页分类栏永远为空。这里补齐后彻底对齐原生。
  final String category;

  MenuItem({
    required this.id,
    required this.name,
    required this.description,
    required this.price,
    required this.imageUrl,
    required this.salesCount,
    required this.isAvailable,
    this.category = '',
  });

  factory MenuItem.fromJson(Map<String, dynamic> j) => MenuItem(
        id: j['id'] ?? '',
        name: j['name'] ?? '',
        description: j['description'] ?? '',
        price: (j['price'] as num?)?.toDouble() ?? 0,
        imageUrl: j['image_url'] ?? '',
        salesCount: j['sales_count'] ?? 0,
        isAvailable: j['is_available'] ?? true,
        category: (j['category'] as String?) ?? '',
      );
}

class OrderItem {
  final String id;
  final String name;
  final String imageUrl;
  final double unitPrice;
  final int quantity;
  final double subtotal;

  OrderItem({
    required this.id,
    required this.name,
    required this.imageUrl,
    required this.unitPrice,
    required this.quantity,
    required this.subtotal,
  });

  factory OrderItem.fromJson(Map<String, dynamic> j) => OrderItem(
        id: j['id'] ?? '',
        name: j['name'] ?? '',
        imageUrl: j['image_url'] ?? '',
        unitPrice: (j['unit_price'] as num?)?.toDouble() ?? 0,
        quantity: j['quantity'] ?? 0,
        subtotal: (j['subtotal'] as num?)?.toDouble() ?? 0,
      );
}

class Order {
  final String id;
  final String buyerId;
  final String buyerName;
  final String status;
  final String buyerNote;
  final double totalPrice;
  final int candyCoinsSpent;
  final String momentImageUrl;
  final List<OrderItem> items;
  final DateTime createdAt;

  Order({
    required this.id,
    required this.buyerId,
    required this.buyerName,
    required this.status,
    required this.buyerNote,
    required this.totalPrice,
    required this.candyCoinsSpent,
    required this.momentImageUrl,
    required this.items,
    required this.createdAt,
  });

  /// 做饭流程中文文案
  String get statusLabel => switch (status) {
        'submitted' => '待饲养员接单',
        'confirmed' => '已接单，准备开工',
        'preparing' => '正在下锅烹饪中',
        'delivering' => '马上端盘上桌啦',
        'completed' => '开饭啦，全部吃光',
        'cancelled' => '订单已取消',
        _ => status,
      };

  /// 饲养员下一个可推进的状态。
  ///
  /// 对齐云端 `transition_order_status`（`table/35_caretaker_order_acceptance.sql`）
  /// 与原生 `OrdersViewModel.kt:62-66`：**跳过** `confirmed`/`delivering` 这两档过渡态 ——
  /// `submitted | confirmed → preparing`，`preparing | delivering → completed`。
  String? get nextStatus => switch (status) {
        'submitted' || 'confirmed' => 'preparing',
        'preparing' || 'delivering' => 'completed',
        _ => null,
      };

  Order copyWithStatus(String newStatus) => Order(
        id: id,
        buyerId: buyerId,
        buyerName: buyerName,
        status: newStatus,
        buyerNote: buyerNote,
        totalPrice: totalPrice,
        candyCoinsSpent: candyCoinsSpent,
        momentImageUrl: momentImageUrl,
        items: items,
        createdAt: createdAt,
      );

  bool get isActive => status != 'completed' && status != 'cancelled';

  factory Order.fromJson(Map<String, dynamic> j) => Order(
        id: j['id'] ?? '',
        buyerId: j['buyer_id'] ?? '',
        buyerName: j['buyer_name'] ?? '',
        status: j['status'] ?? 'submitted',
        buyerNote: j['buyer_note'] ?? '',
        totalPrice: (j['total_price'] as num?)?.toDouble() ?? 0,
        candyCoinsSpent: j['candy_coins_spent'] ?? 0,
        momentImageUrl: j['moment_image_url'] ?? '',
        items: ((j['items'] as List?) ?? const [])
            .map((e) => OrderItem.fromJson(e as Map<String, dynamic>))
            .toList(),
        createdAt: DateTime.tryParse(j['created_at'] ?? '') ?? DateTime.now(),
      );
}

class CandyTransaction {
  final String type;
  final int amount;
  final int balance;
  final String description;
  final DateTime createdAt;

  CandyTransaction({
    required this.type,
    required this.amount,
    required this.balance,
    required this.description,
    required this.createdAt,
  });

  String get typeLabel => switch (type) {
        'recharge' => '饲养员撒糖',
        'spend' => '点菜消费',
        'refund' => '取消退还',
        _ => type,
      };

  factory CandyTransaction.fromJson(Map<String, dynamic> j) => CandyTransaction(
        type: j['type'] ?? '',
        amount: j['amount'] ?? 0,
        balance: j['balance'] ?? 0,
        description: j['description'] ?? '',
        createdAt: DateTime.tryParse(j['created_at'] ?? '') ?? DateTime.now(),
      );
}

/// /me 聚合响应
class MeProfile {
  final AppUser? user;
  final CouplePair? pair;
  final Shop? shop;
  final bool paired;

  MeProfile({this.user, this.pair, this.shop, required this.paired});

  factory MeProfile.fromJson(Map<String, dynamic> j) => MeProfile(
        user: j['user'] == null ? null : AppUser.fromJson(j['user'] as Map<String, dynamic>),
        pair: j['pair'] == null ? null : CouplePair.fromJson(j['pair'] as Map<String, dynamic>),
        shop: j['shop'] == null ? null : Shop.fromJson(j['shop'] as Map<String, dynamic>),
        paired: j['paired'] ?? false,
      );
}
