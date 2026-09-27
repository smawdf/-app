import 'dart:async';

import 'package:flutter/foundation.dart';

import 'models.dart';
import 'order_errors.dart';
import 'supabase_api.dart';

/// 全局应用状态：承担 Session、数据缓存与云端实时同步（Supabase）
class AppState extends ChangeNotifier {
  AppState._();
  static final AppState instance = AppState._();

  final SupabaseApi _api = SupabaseApi.instance;

  AppUser? user;
  CouplePair? pair;
  Shop? shop;
  List<MenuItem> menu = [];
  List<Order> orders = [];
  List<CandyTransaction> transactions = [];

  bool busy = false;
  String? error;
  String? toast;

  bool get isLoggedIn => user != null;
  bool get isPaired => pair != null && (pair!.caretakerId.isNotEmpty || pair!.eaterId.isNotEmpty);
  bool get isCaretaker => user?.isCaretaker ?? false;
  int get candyCoins => pair?.candyCoins ?? 0;

  StreamSubscription? _rtSub;
  Timer? _reconnectTimer;
  Timer? _pollTimer;

  void _setBusy(bool v) {
    busy = v;
    notifyListeners();
  }

  void clearToast() {
    toast = null;
  }

  void notify() {
    notifyListeners();
  }

  Future<bool> deleteDish(String itemId) async {
    final res = await _guard(() async {
      await _api.deleteMenuItem(itemId);
      return true;
    });
    if (res == true) {
      await refreshMenu();
      return true;
    }
    return false;
  }

  /// 【真机修正】把某个分类下的菜品整体改名（分类是聚合出来的，见
  /// `SupabaseApi.renameDishCategory` 的说明）。
  Future<bool> renameDishCategory(String from, String to) async {
    final res = await _guard(() async {
      await _api.renameDishCategory(from, to);
      return true;
    });
    if (res == true) {
      await refreshMenu(silent: true);
      return true;
    }
    return false;
  }

  /// 【真机修正】删分类时把菜品挪到兜底分类（原生
  /// `MenuManagementViewModel.deleteCategory()` 同样先 take ids 再 moveToCategory）。
  Future<bool> moveDishesToCategory(List<String> itemIds, String to) async {
    final res = await _guard(() async {
      await _api.moveDishesToCategory(itemIds, to);
      return true;
    });
    if (res == true) {
      await refreshMenu(silent: true);
      return true;
    }
    return false;
  }

  Future<T?> _guard<T>(Future<T> Function() action, {bool silent = false}) async {
    if (!silent) _setBusy(true);
    error = null;
    try {
      final result = await action();
      return result;
    } on ApiException catch (e) {
      error = e.message;
      return null;
    } catch (e) {
      error = friendlyErrorText(e);
      return null;
    } finally {
      if (!silent) _setBusy(false);
      notifyListeners();
    }
  }

  // ---------------- 认证 ----------------

  /// App 启动引导：读取服务器地址 → 恢复上次登录 → 拉取档案
  Future<void> bootstrap() async {
    // 异步快速探测，不阻塞启动首帧
    unawaited(_api.probeReachableHost());

    // 云端未初始化（例如缺少配置的构建、或单测直接 pumpWidget）时不要抛出未捕获异常，
    // 按「没有可恢复的会话」处理，直接落在登录页。
    ({String token, String userId, String pairId, AppUser? user})? saved;
    try {
      saved = await _api.restoreSession();
    } catch (error) {
      debugPrint('bootstrap: 恢复登录态失败，按未登录处理 —— $error');
      saved = null;
    }
    if (saved == null) {
      notifyListeners();
      return;
    }
    _api.setSession(token: saved.token, userId: saved.userId, pairId: saved.pairId);
    user = saved.user;
    notifyListeners();
    // 用本地缓存的用户信息先渲染，再静默向后端校验/刷新
    await loadMe();
    connectRealtime();
  }

  Future<bool> register({
    required String username,
    required String password,
    required String nickname,
    required String role,
    String avatarUrl = '',
  }) async {
    final res = await _guard(() => _api.register(
          email: username,
          password: password,
          nickname: nickname,
          role: role,
          avatarUrl: avatarUrl,
        ));
    if (res == null) return false;
    _api.setSession(token: res.token, userId: res.user.id, pairId: res.user.pairId);
    user = res.user;
    await _api.persistSession(user: res.user, pairId: res.user.pairId);
    await loadMe();
    connectRealtime();
    return true;
  }

  Future<bool> login({required String username, required String password}) async {
    final res = await _guard(() => _api.login(email: username, password: password));
    if (res == null) return false;
    _api.setSession(token: res.token, userId: res.user.id, pairId: res.user.pairId);
    user = res.user;
    await _api.persistSession(user: res.user, pairId: res.user.pairId);
    await loadMe();
    connectRealtime();
    return true;
  }

  Future<void> logout() async {
    _closeRealtime();
    await _api.clearPersistedSession();
    _api.clearSession();
    user = null;
    pair = null;
    shop = null;
    menu = [];
    orders = [];
    transactions = [];
    notifyListeners();
  }

  /// 云端数据库地址固定，此方法保留仅为兼容旧界面调用
  Future<bool> configureServer({required String host, required int port}) async {
    return await _api.ping();
  }

  // ---------------- 档案 / 配对 ----------------

  Future<void> loadMe() async {
    final me = await _guard(() => _api.me(), silent: true);
    if (me == null) return;
    user = me.user ?? user;
    pair = me.pair;
    shop = me.shop;
    if (user != null) {
      // 未配对时 pair 为 null，但绝不能把会话里的 pair_id 清成空串：
      // 空串会让后续所有写入（菜单/店铺/订单）撞上 menu_dishes 的 RLS 42501，
      // 再被 friendlyErrorText 兜底成「请求失败，请检查网络或稍后重试」，看不出真因。
      // 注册/登录写的是哨兵值 kEmptyPairId，这里必须沿用同一哨兵（_api.pairId）。
      final String pairId = (pair?.id.isNotEmpty ?? false) ? pair!.id : _api.pairId;
      _api.setSession(token: _api.token, userId: user!.id, pairId: pairId);
      await _api.persistSession(user: user!, pairId: pairId);
    }
    if (isPaired) {
      await Future.wait([refreshMenu(silent: true), refreshOrders(silent: true)]);
    }
    notifyListeners();
  }

  Future<bool> createPair() async {
    final res = await _guard(() => _api.createPair());
    if (res == null) return false;
    pair = res.pair;
    if (res.token.isNotEmpty) {
      _api.setSession(token: res.token, userId: user!.id, pairId: res.pair.id);
    }
    await _api.updatePersistedPairId(res.pair.id);
    toast = '邀请码已生成：${res.pair.inviteCode}';
    await refreshMenu(silent: true);
    connectRealtime();
    return true;
  }

  Future<bool> joinPair(String inviteCode) async {
    final res = await _guard(() => _api.joinPair(inviteCode.trim().toUpperCase()));
    if (res == null) return false;
    pair = res.pair;
    if (res.token.isNotEmpty) {
      _api.setSession(token: res.token, userId: user!.id, pairId: res.pair.id);
    }
    await _api.updatePersistedPairId(res.pair.id);
    toast = '绑定成功，你们的小店已连通 💕';
    // 【真机修正】`joinPair` 的 RPC 只回邀请码，没有伴侣昵称/头像；绑定完成后再
    // 拉一次 `me()`，首页情侣卡和我的页才能立刻显示对方的名字（否则要等下次启动）。
    await loadMe();
    connectRealtime();
    return true;
  }

  // ---------------- 数据刷新 ----------------

  Future<void> refreshMenu({bool silent = false}) async {
    final list = await _guard(() => _api.menu(), silent: silent);
    if (list != null) menu = list;
  }

  Future<void> refreshOrders({bool silent = false}) async {
    final list = await _guard(() => _api.listOrders(), silent: silent);
    if (list != null) orders = list;
  }

  Future<void> refreshTransactions({bool silent = false}) async {
    final list = await _guard(() => _api.candyTransactions(), silent: true);
    if (list != null) transactions = list;
  }

  Future<void> refreshAll() async {
    await loadMe();
    await refreshTransactions();
    notifyListeners();
  }

  // ---------------- 业务动作 ----------------

  Future<bool> submitOrder({required List<MenuItem> dishes, String note = ''}) async {
    if (dishes.isEmpty) {
      error = '还没有选菜哦';
      notifyListeners();
      return false;
    }
    // 【真机修正】原生 `CheckoutViewModel.kt:87-99` 的两道前置校验，Flutter 侧漏了：
    // ① 只有吃货能下单；② 糖币不够先拦下。少了②，请求一路打到
    // `spend_eater_candy_coins` 被拒（`insufficient candy coins`），
    // 界面只看到 `friendlyErrorText` 的兜底网络文案。
    final int cost = dishes.fold<double>(0, (sum, d) => sum + d.price).ceil();
    final String? blocked = orderSubmitBlockedReason(
      isCaretaker: isCaretaker,
      candyBalance: candyCoins,
      candyCost: cost,
    );
    if (blocked != null) {
      error = blocked;
      notifyListeners();
      return false;
    }
    final order = await _guard(() => _api.submitOrder(note: note, dishes: dishes));
    if (order == null) return false;
    toast = '点单成功！已消费 ${order.candyCoinsSpent} 糖币';
    await Future.wait([refreshOrders(silent: true), loadMe()]);
    return true;
  }

  Future<bool> advanceOrder(Order order) async {
    final next = order.nextStatus;
    if (next == null) return false;
    final updated = await _guard(() => _api.advanceOrder(order.id, next));
    if (updated == null) return false;
    await refreshOrders(silent: true);
    return true;
  }

  Future<bool> cancelOrder(Order order) async {
    final ok = await _guard(() async {
      await _api.cancelOrder(order.id);
      return true;
    });
    if (ok == null) return false;
    toast = '订单已取消，糖币已退还';
    await Future.wait([refreshOrders(silent: true), loadMe()]);
    return true;
  }

  Future<bool> recharge({required int amount, String reason = ''}) async {
    final balance = await _guard(() => _api.rechargeCandy(amount: amount, reason: reason));
    if (balance == null) return false;
    if (pair != null) {
      pair = CouplePair(
        id: pair!.id,
        inviteCode: pair!.inviteCode,
        caretakerId: pair!.caretakerId,
        eaterId: pair!.eaterId,
        candyCoins: balance,
        // 保留伴侣昵称/头像，别在撒糖后丢掉（见 `CouplePair.partnerName` 注释）。
        partnerName: pair!.partnerName,
        partnerAvatarUrl: pair!.partnerAvatarUrl,
      );
    }
    toast = '撒糖成功，对方已收到 $amount 糖币 🍬';
    await refreshTransactions();
    return true;
  }

  Future<bool> addDish({
    required String name,
    required double price,
    String description = '',
    String emoji = '🍽️',
    String category = '',
  }) async {
    final item = await _guard(() => _api.createMenuItem(
          name: name,
          price: price,
          description: description,
          imageUrl: emoji,
          category: category,
        ));
    if (item == null) return false;
    toast = '已上新：${item.name}';
    await refreshMenu(silent: true);
    return true;
  }

  Future<bool> updateShopInfo({required String name, required String announcement}) async {
    final updated = await _guard(() => _api.updateShop(name: name, announcement: announcement));
    if (updated != null) {
      shop = updated;
      notifyListeners();
      return true;
    }
    return false;
  }

  Future<Map<String, dynamic>?> loadAnniversary() async {
    return _guard(() => _api.anniversary(), silent: true);
  }

  Future<bool> setAnniversary(String date) async {
    final res = await _guard(() => _api.updateAnniversary(date));
    return res != null;
  }

  Future<List<Map<String, dynamic>>> searchRemoteRecipes(String keyword) async {
    final res = await _guard(() => _api.searchRecipes(keyword), silent: true);
    return res ?? [];
  }

  /// 切换当前用户身份（饲养员 / 吃货），并持久化到云端 profiles.selected_role
  Future<bool> updateRole(String role) async {
    final ok = await _guard(() async {
      await _api.updateRole(role);
      return true;
    });
    if (ok != true) return false;
    if (user != null) {
      user = user!.copyWithRole(role);
    }
    notifyListeners();
    return true;
  }

  /// 改昵称 / 头像（存 `profiles.nickname` 与 `profiles.avatar_url`）。
  ///
  /// `avatarUrl` 可以是 `data:image/jpeg;base64,…`：这个项目没有任何 storage
  /// 桶（建桶要 service_role），所以头像直接进 text 列，见
  /// `SupabaseApi.updateProfile` 的注释。写成功后本地 copyWith 立即刷新，
  /// 不再回读一次云端。
  Future<bool> updateProfile({String? nickname, String? avatarUrl}) async {
    final ok = await _guard(() async {
      await _api.updateProfile(nickname: nickname, avatarUrl: avatarUrl);
      return true;
    });
    if (ok != true) return false;
    if (user != null) {
      user = user!.copyWith(nickname: nickname, avatarUrl: avatarUrl);
    }
    notifyListeners();
    return true;
  }

  // ---------------- Supabase 云端实时同步 ----------------

  /// 订阅云端变更：优先用 Supabase Realtime，同时保留 8 秒兜底轮询，
  /// 即使项目未把表加入 realtime publication，界面也不会停止刷新。
  void connectRealtime() {
    _closeRealtime();
    if (!isPaired || user == null) return;

    try {
      _rtSub = _api.realtimeEvents().listen(
        (_) => _handleEvent('order_updated'),
        onError: (_) => _scheduleReconnect(),
      );
    } catch (_) {
      _scheduleReconnect();
    }

    _pollTimer = Timer.periodic(const Duration(seconds: 8), (_) {
      if (!isPaired) return;
      refreshOrders(silent: true);
      refreshTransactions(silent: true);
    });
  }

  void _scheduleReconnect() {
    _reconnectTimer?.cancel();
    if (!isPaired) return;
    _reconnectTimer = Timer(const Duration(seconds: 4), () {
      if (isPaired) connectRealtime();
    });
  }

  void _closeRealtime() {
    _reconnectTimer?.cancel();
    _pollTimer?.cancel();
    _pollTimer = null;
    _rtSub?.cancel();
    _rtSub = null;
  }

  /// 伴侣端的动作，这里实时落库并刷新 UI
  void _handleEvent(String type) {
    switch (type) {
      case 'order_created':
        toast = '🔔 对方点菜啦！';
        refreshOrders(silent: true);
        break;
      case 'order_updated':
        refreshOrders(silent: true);
        break;
      case 'order_cancelled':
        toast = '对方取消了订单，糖币已退还';
        refreshOrders(silent: true);
        loadMe();
        break;
      case 'candy_changed':
        toast = '🍬 对方给你撒糖啦！';
        loadMe();
        refreshTransactions();
        break;
      case 'pair_joined':
        toast = '💕 伴侣已绑定成功';
        loadMe();
        break;
      case 'menu_updated':
        refreshMenu(silent: true);
        break;
    }
    notifyListeners();
  }

  @override
  void dispose() {
    _closeRealtime();
    super.dispose();
  }
}
