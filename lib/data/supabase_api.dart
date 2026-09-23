import 'dart:async';
import 'dart:math';

import 'package:supabase_flutter/supabase_flutter.dart' as sb;

import 'local_store.dart';
import 'models.dart';

/// Supabase 云端地址与 anon key。
/// anon/publishable key 本就是设计为可公开嵌入客户端的，数据安全由 RLS 策略保证。
/// 可用 --dart-define=SUPABASE_URL=... --dart-define=SUPABASE_ANON_KEY=... 覆盖。
const String kSupabaseUrl = String.fromEnvironment(
  'SUPABASE_URL',
  defaultValue: 'https://dwncdcwsbgbouoemfvwt.supabase.co',
);
const String kSupabaseAnonKey = String.fromEnvironment(
  'SUPABASE_ANON_KEY',
  defaultValue: 'sb_publishable_8N_jUSyhvKOmWAPXGRAIhA__q96dF7a',
);

const String kEmptyPairId = '00000000-0000-0000-0000-000000000000';

/// 云端记账记录 id 生成器
String _newRecordId() {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  final rnd = Random();
  return 'rec-${List.generate(20, (_) => chars[rnd.nextInt(chars.length)]).join()}';
}

/// Supabase 云端数据访问层。
///
/// 刻意与 [ApiClient] 保持**完全相同的方法签名**，这样上层 AppState 与所有页面
/// 无需改动即可从自建 Go 后端切换到在线 Supabase。
class SupabaseApi {
  SupabaseApi._();
  static final SupabaseApi instance = SupabaseApi._();

  sb.SupabaseClient get _db => sb.Supabase.instance.client;
  sb.GoTrueClient get _auth => _db.auth;

  String _pairId = '';
  String _userId = '';
  AppUser? _cachedUser;

  /// 云端伴侣动态提醒（配对成功/解绑），由 AppState 取走后清空
  String _pendingNotice = '';
  String takePendingNotice() {
    final n = _pendingNotice;
    _pendingNotice = '';
    return n;
  }

  String get token => _auth.currentSession?.accessToken ?? '';
  String get userId => _userId;
  String get pairId => _pairId;

  static Future<void> initialize() async {
    // ignore: deprecated_member_use
    await sb.Supabase.initialize(url: kSupabaseUrl, anonKey: kSupabaseAnonKey);
  }

  // ---------------- 服务器配置（云端固定，保留同名方法以兼容上层）----------------

  Future<void> loadServerConfig() async {
    final store = LocalStore.instance;
    _pairId = await store.getString('cloud_pair_id') ?? '';
  }

  Future<void> configureServer({required String host, required int port}) async {}

  Future<bool> hasSavedServerConfig() async => true;

  Future<String?> probeReachableHost({Duration timeout = const Duration(seconds: 6)}) async {
    try {
      await _db.from('profiles').select('user_id').limit(1).timeout(timeout);
      return kSupabaseUrl;
    } catch (_) {
      return null;
    }
  }

  /// 连通性自检：只要云端有响应即视为可达
  Future<bool> ping() async {
    try {
      await _db.from('profiles').select('user_id').limit(1);
      return true;
    } catch (_) {
      return false;
    }
  }

  // ---------------- 会话 ----------------

  void setSession({required String token, required String userId, required String pairId}) {
    _userId = userId;
    _pairId = pairId;
  }

  void clearSession() {
    _userId = '';
    _pairId = '';
    _cachedUser = null;
  }

  Future<void> persistSession({required AppUser user, required String pairId}) async {
    _cachedUser = user;
    _pairId = pairId;
    await LocalStore.instance.setString('cloud_pair_id', pairId);
  }

  Future<void> updatePersistedPairId(String pairId) async {
    _pairId = pairId;
    await LocalStore.instance.setString('cloud_pair_id', pairId);
  }

  /// 恢复上次登录态：Supabase SDK 自身已持久化 token，这里补齐 profile 信息
  Future<({String token, String userId, String pairId, AppUser? user})?> restoreSession() async {
    final session = _auth.currentSession;
    if (session == null) return null;

    _userId = session.user.id;

    // 读取 profile 补齐昵称/角色/配对
    AppUser user = AppUser(
      id: session.user.id,
      username: session.user.email ?? '',
      nickname: '',
      avatarUrl: '',
      role: 'eater',
      pairId: '',
    );

    try {
      final row = await _db.from('profiles').select().eq('user_id', session.user.id).maybeSingle();
      if (row != null) {
        _pairId = (row['pair_id'] as String?) ?? '';
        user = AppUser(
          id: session.user.id,
          username: session.user.email ?? '',
          nickname: (row['nickname'] as String?) ?? '',
          avatarUrl: (row['avatar_url'] as String?) ?? '',
          role: ((row['selected_role'] as String?)?.isEmpty ?? true)
              ? 'eater'
              : row['selected_role'] as String,
          pairId: _pairId,
        );
      }
    } catch (_) {
      // 网络抖动时先用本地缓存渲染
    }

    _cachedUser = user;
    return (
      token: session.accessToken,
      userId: session.user.id,
      pairId: _pairId,
      user: user,
    );
  }

  Future<void> clearPersistedSession() async {
    try {
      await _auth.signOut();
    } catch (_) {}
    _cachedUser = null;
    _pairId = '';
    _userId = '';
    await LocalStore.instance.remove('cloud_pair_id');
  }

  // ---------------- 认证（Supabase Auth 使用邮箱 + 密码）----------------

  Future<({String token, AppUser user})> register({
    required String email,
    required String password,
    required String nickname,
    required String role,
  }) async {
    final res = await _auth.signUp(email: email.trim(), password: password);
    final uid = res.user?.id;
    if (uid == null) {
      throw ApiException('注册失败：请确认邮箱格式与密码长度（至少 6 位）');
    }

    // 若项目开启了邮箱确认，此时没有 session
    if (res.session == null) {
      // 邮箱免确认已开启时通常直接给 session；否则尝试直接登录
      try {
        await _auth.signInWithPassword(email: email.trim(), password: password);
      } catch (_) {
        throw ApiException('注册成功但需要邮箱验证，请到邮箱确认后再登录');
      }
    }

    _userId = uid;

    // 创建/更新 profile，写入昵称与身份角色
    await _db.from('profiles').upsert({
      'user_id': uid,
      'pair_id': kEmptyPairId,
      'nickname': nickname.trim().isEmpty ? email.split('@').first : nickname.trim(),
      'avatar_url': '',
      'selected_role': role,
      'candy_coins': 66,
      'session_id': '',
      'session_updated_at': '',
    }, onConflict: 'user_id');

    final user = AppUser(
      id: uid,
      username: email.trim(),
      nickname: nickname.trim().isEmpty ? email.split('@').first : nickname.trim(),
      avatarUrl: '',
      role: role,
      pairId: kEmptyPairId,
    );
    _cachedUser = user;
    return (token: _auth.currentSession?.accessToken ?? '', user: user);
  }

  Future<({String token, AppUser user})> login({
    required String email,
    required String password,
  }) async {
    final res = await _auth.signInWithPassword(email: email.trim(), password: password);
    final uid = res.user?.id;
    if (uid == null) throw ApiException('登录失败：邮箱或密码不正确');

    _userId = uid;

    Map<String, dynamic>? row;
    try {
      row = await _db.from('profiles').select().eq('user_id', uid).maybeSingle();
    } catch (_) {}

    // 老账号可能没有 profile 行，补一条
    if (row == null) {
      await _db.from('profiles').upsert({
        'user_id': uid,
        'pair_id': kEmptyPairId,
        'nickname': email.split('@').first,
        'avatar_url': '',
        'selected_role': 'eater',
        'candy_coins': 66,
        'session_id': '',
        'session_updated_at': '',
      }, onConflict: 'user_id');
    }

    _pairId = (row?['pair_id'] as String?) ?? kEmptyPairId;

    final role = ((row?['selected_role'] as String?)?.isEmpty ?? true)
        ? 'eater'
        : row!['selected_role'] as String;

    final user = AppUser(
      id: uid,
      username: email.trim(),
      nickname: (row?['nickname'] as String?) ?? email.split('@').first,
      avatarUrl: (row?['avatar_url'] as String?) ?? '',
      role: role,
      pairId: _pairId,
    );
    _cachedUser = user;
    return (token: _auth.currentSession?.accessToken ?? '', user: user);
  }

  // ---------------- 档案聚合 ----------------

  Future<MeProfile> me() async {
    final uid = _userId.isNotEmpty ? _userId : (_auth.currentUser?.id ?? '');
    if (uid.isEmpty) return MeProfile(paired: false);

    Map<String, dynamic>? profile;
    try {
      profile = await _db.from('profiles').select().eq('user_id', uid).maybeSingle();
    } catch (_) {}

    final pairId = (profile?['pair_id'] as String?) ?? _pairId;
    _pairId = pairId;
    final role = ((profile?['selected_role'] as String?)?.isEmpty ?? true)
        ? 'eater'
        : profile!['selected_role'] as String;

    final user = AppUser(
      id: uid,
      username: _auth.currentUser?.email ?? '',
      nickname: (profile?['nickname'] as String?) ?? '',
      avatarUrl: (profile?['avatar_url'] as String?) ?? '',
      role: role,
      pairId: pairId,
    );
    _cachedUser = user;

    final paired = pairId.isNotEmpty && pairId != kEmptyPairId;

    // 伴侣信息 + 糖币余额：优先用云端权威快照函数
    String partnerId = '';
    int candy = (profile?['candy_coins'] as int?) ?? 66;

    if (paired) {
      // 同 pair 的另一位成员
      try {
        final partnerRows = await _db
            .from('profiles')
            .select('user_id,nickname,candy_coins')
            .eq('pair_id', pairId)
            .neq('user_id', uid)
            .limit(1);
        if (partnerRows.isNotEmpty) {
          final p = partnerRows.first;
          partnerId = (p['user_id'] as String?) ?? '';
          candy = (p['candy_coins'] as int?) ?? candy;
        }
      } catch (_) {}

      // 权威共享糖币余额与伴侣提醒（current_pair_snapshot）
      try {
        final snap = await _db.rpc('current_pair_snapshot');
        final s = _firstRow(snap);
        if (s != null) {
          final sharedCandy = s['partner_candy_coins'];
          if (sharedCandy is int) candy = sharedCandy;

          final notice = (s['notice_message'] as String?) ?? '';
          if (notice.isNotEmpty) _pendingNotice = notice;
        }
      } catch (_) {}
    }

    // 小店设置
    Shop? shop;
    if (paired) {
      try {
        final s = await _db.from('shop_settings').select().eq('pair_id', pairId).maybeSingle();
        if (s != null) {
          shop = Shop(
            id: pairId,
            name: (s['name'] as String?) ?? '我的小店',
            announcement: (s['announcement'] as String?) ?? '',
            coverUrl: (s['image_url'] as String?) ?? '',
          );
        }
      } catch (_) {}
    }

    CouplePair? pair;
    if (paired) {
      pair = CouplePair(
        id: pairId,
        inviteCode: '',
        caretakerId: role == 'caretaker' ? uid : partnerId,
        eaterId: role == 'eater' ? uid : partnerId,
        candyCoins: candy,
      );
    }

    return MeProfile(user: user, pair: pair, shop: shop, paired: paired);
  }

  // ---------------- 情侣配对 ----------------

  /// 生成 6 位邀请码。
  /// 云端 create_pair_invite 返回的是 `[{"pair_code":"XXXXXX","selected_role":"..."}]`。
  Future<({CouplePair pair, String token})> createPair() async {
    final role = _cachedUser?.role ?? 'eater';
    final res = await _db.rpc('create_pair_invite', params: {'inviter_role': role});
    final code = _extractPairCode(res);

    final pair = CouplePair(
      id: _pairId,
      inviteCode: code,
      caretakerId: role == 'caretaker' ? _userId : '',
      eaterId: role == 'eater' ? _userId : '',
      candyCoins: 66,
    );
    return (pair: pair, token: token);
  }

  /// 输入邀请码加入伴侣。
  /// 加入成功后云端会把**双方** profile.pair_id 都设为该 6 位邀请码。
  Future<({CouplePair pair, String token})> joinPair(String inviteCode) async {
    final myRole = _cachedUser?.role ?? 'eater';
    final res = await _db.rpc('join_pair_invite', params: {'invite_code': inviteCode.trim()});
    final code = _extractPairCode(res);

    final newPairId = code.isNotEmpty ? code : inviteCode.trim();
    if (newPairId.isNotEmpty) {
      _pairId = newPairId;
      await updatePersistedPairId(newPairId);
    }

    // 回读 profile 确认云端真实 pair_id
    try {
      final row = await _db.from('profiles').select('pair_id').eq('user_id', _userId).maybeSingle();
      final pid = (row?['pair_id'] as String?) ?? '';
      if (pid.isNotEmpty && pid != kEmptyPairId) _pairId = pid;
    } catch (_) {}

    final pair = CouplePair(
      id: _pairId,
      inviteCode: newPairId,
      caretakerId: myRole == 'caretaker' ? _userId : '',
      eaterId: myRole == 'eater' ? _userId : '',
      candyCoins: 66,
    );
    return (pair: pair, token: token);
  }

  /// 从 RPC 返回中提取首行（兼容 数组/对象 两种形态）
  Map<String, dynamic>? _firstRow(dynamic res) {
    if (res is List && res.isNotEmpty) {
      final first = res.first;
      if (first is Map) return Map<String, dynamic>.from(first);
    }
    if (res is Map) return Map<String, dynamic>.from(res);
    return null;
  }

  /// 从 RPC 返回中提取 pair_code（兼容 数组/对象/纯字符串 三种形态）
  String _extractPairCode(dynamic res) {
    if (res == null) return '';
    if (res is String) return res;
    if (res is List && res.isNotEmpty) {
      final first = res.first;
      if (first is Map && first['pair_code'] != null) return first['pair_code'].toString();
      if (first is String) return first;
    }
    if (res is Map && res['pair_code'] != null) return res['pair_code'].toString();
    return res.toString();
  }

  /// 更新当前用户身份角色
  Future<void> updateRole(String role) async {
    if (_userId.isEmpty) return;
    await _db.from('profiles').update({'selected_role': role}).eq('user_id', _userId);
  }

  // ---------------- 小店设置 ----------------

  Future<Shop> updateShop({required String name, required String announcement}) async {
    await _db.from('shop_settings').upsert({
      'pair_id': _pairId,
      'name': name,
      'announcement': announcement,
    }, onConflict: 'pair_id');
    return Shop(id: _pairId, name: name, announcement: announcement, coverUrl: '');
  }

  // ---------------- 菜单 ----------------

  Future<List<MenuItem>> menu() async {
    if (_pairId.isEmpty || _pairId == kEmptyPairId) return [];
    final rows = await _db
        .from('menu_dishes')
        .select()
        .eq('pair_id', _pairId)
        .eq('is_available', true)
        .order('sort_order');

    return (rows as List).map((e) {
      final m = e as Map<String, dynamic>;
      return MenuItem(
        id: (m['id'] as String?) ?? '',
        name: (m['name'] as String?) ?? '',
        description: (m['description'] as String?) ?? '',
        price: ((m['price'] as num?) ?? 0).toDouble(),
        imageUrl: (m['image_url'] as String?) ?? '',
        salesCount: (m['monthly_sales'] as int?) ?? 0,
        isAvailable: (m['is_available'] as bool?) ?? true,
      );
    }).toList();
  }

  Future<MenuItem> createMenuItem({
    required String name,
    required double price,
    String description = '',
    String imageUrl = '',
  }) async {
    final id = _newRecordId();
    await _db.from('menu_dishes').insert({
      'id': id,
      'pair_id': _pairId,
      'name': name,
      'price': price,
      'image_url': imageUrl,
      'category': '其他',
      'description': description,
      'is_available': true,
      'sort_order': 0,
      'monthly_sales': 0,
      'stock': 0,
    });
    return MenuItem(
      id: id,
      name: name,
      description: description,
      price: price,
      imageUrl: imageUrl,
      salesCount: 0,
      isAvailable: true,
    );
  }

  Future<void> deleteMenuItem(String itemId) async {
    await _db.from('menu_dishes').delete().eq('id', itemId).eq('pair_id', _pairId);
  }

  // ---------------- 订单 ----------------

  Future<List<Order>> listOrders() async {
    if (_pairId.isEmpty || _pairId == kEmptyPairId) return [];
    final rows = await _db
        .from('orders')
        .select()
        .eq('pair_id', _pairId)
        .order('created_at', ascending: false)
        .limit(50);

    final orders = <Order>[];
    for (final r in (rows as List)) {
      final m = r as Map<String, dynamic>;
      final orderId = (m['id'] as String?) ?? '';

      List<OrderItem> items = [];
      try {
        final itemRows = await _db
            .from('order_items')
            .select()
            .eq('order_id', orderId);
        items = (itemRows as List).map((e) {
          final i = e as Map<String, dynamic>;
          return OrderItem(
            id: (i['id'] as String?) ?? '',
            name: (i['menu_item_name'] as String?) ?? '',
            imageUrl: (i['menu_item_image_url'] as String?) ?? '',
            unitPrice: ((i['unit_price'] as num?) ?? 0).toDouble(),
            quantity: (i['quantity'] as int?) ?? 1,
            subtotal: ((i['subtotal'] as num?) ?? 0).toDouble(),
          );
        }).toList();
      } catch (_) {}

      orders.add(Order(
        id: orderId,
        buyerId: (m['user_id'] as String?) ?? '',
        buyerName: (m['buyer_name'] as String?) ?? '',
        status: _normalizeStatus((m['status'] as String?) ?? 'submitted'),
        buyerNote: (m['buyer_note'] as String?) ?? '',
        totalPrice: ((m['total_price'] as num?) ?? 0).toDouble(),
        candyCoinsSpent: (m['candy_coins_spent'] as int?) ?? 0,
        momentImageUrl: (m['moment_image_url'] as String?) ?? '',
        items: items,
        createdAt: DateTime.tryParse((m['created_at'] as String?) ?? '')?.toLocal() ?? DateTime.now(),
      ));
    }
    return orders;
  }

  String _normalizeStatus(String s) => s;

  Future<Order> submitOrder({
    required String note,
    required List<MenuItem> dishes,
  }) async {
    if (dishes.isEmpty) throw ApiException('请先选择菜品');

    final total = dishes.fold<double>(0, (sum, d) => sum + d.price);
    final cost = total.ceil();
    final orderId = _uidV4();
    final recordId = orderId; // spend_eater_candy_coins 需要 uuid

    // 1. 扣减吃货糖币（云端原子事务）
    await _db.rpc('spend_eater_candy_coins', params: {
      'amount': cost,
      'record_id': recordId,
    });

    // 2. 写入订单主表
    try {
      await _db.from('orders').insert({
        'id': orderId,
        'user_id': _userId,
        'pair_id': _pairId,
        'shop_id': _pairId,
        'shop_name': _cachedUser?.nickname ?? '我们的小店',
        'shop_cover_url': '',
        'status': 'submitted',
        'address_snapshot': '家中餐桌',
        'buyer_note': note,
        'buyer_name': _cachedUser?.nickname ?? '',
        'subtotal': total,
        'delivery_fee': 0,
        'total_price': total,
        'candy_coins_spent': cost,
        'moment_image_url': '',
      });

      // 3. 写入明细
      await _db.from('order_items').insert(
            dishes
                .map((d) => {
                      'id': _uidV4(),
                      'order_id': orderId,
                      'menu_item_id': d.id,
                      'menu_item_name': d.name,
                      'menu_item_image_url': d.imageUrl,
                      'unit_price': d.price,
                      'quantity': 1,
                      'subtotal': d.price,
                    })
                .toList(),
          );
    } catch (e) {
      // 订单写入失败则退回糖币，避免吃货白扣
      try {
        await _db.rpc('refund_eater_candy_coins', params: {
          'amount': cost,
          'record_id': recordId,
        });
      } catch (_) {}
      rethrow;
    }

    return Order(
      id: orderId,
      buyerId: _userId,
      buyerName: _cachedUser?.nickname ?? '',
      status: 'submitted',
      buyerNote: note,
      totalPrice: total,
      candyCoinsSpent: cost,
      momentImageUrl: '',
      items: dishes
          .map((d) => OrderItem(
                id: d.id,
                name: d.name,
                imageUrl: d.imageUrl,
                unitPrice: d.price,
                quantity: 1,
                subtotal: d.price,
              ))
          .toList(),
      createdAt: DateTime.now(),
    );
  }

  /// 推进订单状态。云端只接受 preparing / completed 两个目标态，
  /// 这里把 UI 的中间态映射到云端合法值。
  Future<Order> advanceOrder(String orderId, String newStatus) async {
    final target = switch (newStatus) {
      'confirmed' => 'preparing',
      'preparing' => 'preparing',
      'delivering' => 'completed',
      'completed' => 'completed',
      _ => newStatus,
    };

    final res = await _db.rpc('transition_order_status', params: {
      'target_order_id': orderId,
      'new_status': target,
    });

    final applied = (res is String ? res : target);
    return Order(
      id: orderId,
      buyerId: '',
      buyerName: '',
      status: applied,
      buyerNote: '',
      totalPrice: 0,
      candyCoinsSpent: 0,
      momentImageUrl: '',
      items: const [],
      createdAt: DateTime.now(),
    );
  }

  Future<void> cancelOrder(String orderId) async {
    await _db.rpc('cancel_order_and_refund', params: {'target_order_id': orderId});
  }

  // ---------------- 糖糖币 ----------------

  Future<int> rechargeCandy({required int amount, String reason = ''}) async {
    // add_partner_candy_coins_with_record 直接返回变动后的余额
    final res = await _db.rpc('add_partner_candy_coins_with_record', params: {
      'amount': amount,
      'record_id': _newRecordId(),
      'record_note': reason.isEmpty ? '饲养员投喂' : reason,
    });

    if (res is int) return res;
    if (res is num) return res.toInt();

    // 兜底：回读余额
    try {
      final rows = await _db
          .from('profiles')
          .select('candy_coins')
          .eq('pair_id', _pairId)
          .neq('user_id', _userId)
          .limit(1);
      if (rows.isNotEmpty) return (rows.first['candy_coins'] as int?) ?? 0;
    } catch (_) {}
    return 0;
  }

  Future<List<CandyTransaction>> candyTransactions() async {
    if (_pairId.isEmpty || _pairId == kEmptyPairId) return [];
    final rows = await _db
        .from('candy_coin_records')
        .select()
        .eq('pair_id', _pairId)
        .order('created_at', ascending: false)
        .limit(100);

    return (rows as List).map((e) {
      final m = e as Map<String, dynamic>;
      final rawType = (m['type'] as String?) ?? '';
      return CandyTransaction(
        type: switch (rawType) {
          'recharge' || 'partner_recharge' || 'add' => 'recharge',
          'spend' || 'order_spend' => 'spend',
          'refund' || 'order_refund' => 'refund',
          _ => rawType,
        },
        amount: (m['amount'] as int?) ?? 0,
        balance: (m['balance_after'] as int?) ?? 0,
        description: (m['note'] as String?) ?? '',
        createdAt: DateTime.tryParse((m['created_at'] as String?) ?? '')?.toLocal() ?? DateTime.now(),
      );
    }).toList();
  }

  // ---------------- 纪念日 ----------------

  Future<Map<String, dynamic>> anniversary() async {
    int days = 1;
    String pairedAt = '';

    if (_pairId.isNotEmpty && _pairId != kEmptyPairId) {
      try {
        final row = await _db.from('anniversaries').select().eq('pair_id', _pairId).maybeSingle();
        pairedAt = (row?['paired_at'] as String?) ?? '';
      } catch (_) {}
    }

    if (pairedAt.isNotEmpty) {
      final d = DateTime.tryParse(pairedAt);
      if (d != null) {
        final diff = DateTime.now().difference(d).inDays + 1;
        days = diff < 1 ? 1 : diff;
      }
    }

    // 出锅回忆墙：取已完成的订单
    final moments = <Map<String, dynamic>>[];
    try {
      final rows = await _db
          .from('orders')
          .select()
          .eq('pair_id', _pairId)
          .eq('status', 'completed')
          .order('created_at', ascending: false)
          .limit(20);
      for (final r in (rows as List)) {
        final m = r as Map<String, dynamic>;
        moments.add({
          'dish_name': (m['buyer_note'] as String?)?.isNotEmpty == true ? m['buyer_note'] : '甜蜜一餐',
          'note': (m['buyer_note'] as String?) ?? '',
          'emoji': '🍽️',
          'chef_name': '饲养员',
          'cooked_at': (m['created_at'] as String?) ?? '',
          'image_url': (m['moment_image_url'] as String?) ?? '',
        });
      }
    } catch (_) {}

    return {
      'anniversary_at': pairedAt.isEmpty ? '2025-04-20' : pairedAt.split('T').first,
      'days_together': days,
      'moments': moments,
    };
  }

  Future<Map<String, dynamic>> updateAnniversary(String date) async {
    await _db.from('anniversaries').upsert({
      'pair_id': _pairId,
      'paired_at': date,
      'updated_at': DateTime.now().toUtc().toIso8601String(),
    }, onConflict: 'pair_id');
    return anniversary();
  }

  // ---------------- 菜谱检索（云端无此表，用本地菜谱库）----------------

  Future<List<Map<String, dynamic>>> searchRecipes(String keyword) async {
    final kw = keyword.trim().toLowerCase();
    if (kw.isEmpty) return _recipeLibrary;
    return _recipeLibrary.where((r) {
      final name = (r['name'] as String).toLowerCase();
      final desc = (r['desc'] as String).toLowerCase();
      return name.contains(kw) || desc.contains(kw);
    }).toList();
  }

  // ---------------- 实时订阅 ----------------

  /// 订阅本情侣的订单/糖币/菜单变更，触发上层刷新。
  /// 若云端未开启 Realtime 发布，上层还有兜底轮询，不会因此失联。
  Stream<void> realtimeEvents() {
    if (_pairId.isEmpty || _pairId == kEmptyPairId) return const Stream.empty();

    final controller = StreamController<void>();
    final subs = <StreamSubscription>[];

    void listen(String table, String filterColumn, String filterValue) {
      try {
        final s = _db
            .from(table)
            .stream(primaryKey: const ['id'])
            .eq(filterColumn, filterValue)
            .listen((_) => controller.add(null), onError: (_) {});
        subs.add(s);
      } catch (_) {}
    }

    listen('orders', 'pair_id', _pairId);
    listen('candy_coin_records', 'pair_id', _pairId);

    controller.onCancel = () async {
      for (final s in subs) {
        await s.cancel();
      }
    };

    return controller.stream;
  }
}

/// 生成 uuid v4（订单与明细主键为 uuid 类型）
String _uidV4() {
  final rnd = Random.secure();
  final bytes = List<int>.generate(16, (_) => rnd.nextInt(256));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  String hex(int b) => b.toRadixString(16).padLeft(2, '0');
  final h = bytes.map(hex).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}

/// 内置菜谱库（云端未提供菜谱表，发现页使用本地库）
const List<Map<String, dynamic>> _recipeLibrary = [
  {
    'name': '关东风味肥牛寿喜烧',
    'desc': '热气腾腾关东风味甜咸寿喜锅，大片肥牛裹上无菌生蛋液，冬天情侣窝在一起吃最幸福！',
    'time': '25 分钟',
    'difficulty': '简单',
    'price': 42,
    'emoji': '🥘',
  },
  {
    'name': '蜜汁可乐小鸡翅',
    'desc': '火候恰到好处，鸡翅香甜脱骨，浓郁可乐焦糖裹满每一寸鸡皮，下饭绝对一绝！',
    'time': '20 分钟',
    'difficulty': '家常',
    'price': 18,
    'emoji': '🍗',
  },
  {
    'name': '草莓生巧舒芙蕾',
    'desc': '吃货点名必吃榜 TOP 1！云朵般轻盈绵软，淋上微苦丝滑生巧酱与清甜草莓粒。',
    'time': '30 分钟',
    'difficulty': '甜品',
    'price': 22,
    'emoji': '🥞',
  },
  {
    'name': '暖胃浓汤番茄牛腩',
    'desc': '砂锅慢火煨足两小时，番茄熬煮融化进浓郁牛汤，酸甜开胃，汤汁拌饭能炫三碗！',
    'time': '60 分钟',
    'difficulty': '中等',
    'price': 36,
    'emoji': '🍲',
  },
  {
    'name': '法式巴斯克乳酪蛋糕',
    'desc': '重度芝士爱好者的本命甜点，焦黑外皮包裹着冰淇淋般半熟流心，浓醇奶香久久不散。',
    'time': '35 分钟',
    'difficulty': '甜品',
    'price': 15,
    'emoji': '🍰',
  },
  {
    'name': '多汁白桃乌龙暴打冻饮',
    'desc': '手捣新鲜多汁白桃果肉，配清香高山冷萃乌龙，0 卡糖清爽低负担，夏日解腻神器。',
    'time': '10 分钟',
    'difficulty': '极快',
    'price': 12,
    'emoji': '🥤',
  },
  {
    'name': '鲜香滑嫩黑椒雪花牛肉粒',
    'desc': '外焦里嫩爆汁，黄油爆香蒜粒与杏鲍菇，大粒现磨黑胡椒激发牛肉原香。',
    'time': '15 分钟',
    'difficulty': '快手',
    'price': 38,
    'emoji': '🥩',
  },
  {
    'name': '暖心鲜甜上汤娃娃菜',
    'desc': '皮蛋火腿慢火吊出奶白浓汤，娃娃菜吸饱鲜味，清润鲜甜，做饭方拿手暖心汤菜。',
    'time': '12 分钟',
    'difficulty': '简单',
    'price': 16,
    'emoji': '🥬',
  },
];
