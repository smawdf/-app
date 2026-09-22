/// 与 Go 后端 (orderdisk-server) 一一对应的数据模型
library;

import 'dart:convert';

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

  CouplePair({
    required this.id,
    required this.inviteCode,
    required this.caretakerId,
    required this.eaterId,
    required this.candyCoins,
  });

  bool get isFullyBound => caretakerId.isNotEmpty && eaterId.isNotEmpty;

  factory CouplePair.fromJson(Map<String, dynamic> j) => CouplePair(
        id: j['id'] ?? '',
        inviteCode: j['invite_code'] ?? '',
        caretakerId: j['caretaker_id'] ?? '',
        eaterId: j['eater_id'] ?? '',
        candyCoins: j['candy_coins'] ?? 0,
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

  MenuItem({
    required this.id,
    required this.name,
    required this.description,
    required this.price,
    required this.imageUrl,
    required this.salesCount,
    required this.isAvailable,
  });

  factory MenuItem.fromJson(Map<String, dynamic> j) => MenuItem(
        id: j['id'] ?? '',
        name: j['name'] ?? '',
        description: j['description'] ?? '',
        price: (j['price'] as num?)?.toDouble() ?? 0,
        imageUrl: j['image_url'] ?? '',
        salesCount: j['sales_count'] ?? 0,
        isAvailable: j['is_available'] ?? true,
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

  /// 饲养员下一个可推进的状态
  String? get nextStatus => switch (status) {
        'submitted' => 'confirmed',
        'confirmed' => 'preparing',
        'preparing' => 'delivering',
        'delivering' => 'completed',
        _ => null,
      };

  String? get nextStatusLabel => switch (status) {
        'submitted' => '接单',
        'confirmed' => '开始做饭',
        'preparing' => '端盘上桌',
        'delivering' => '开饭完成',
        _ => null,
      };

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
