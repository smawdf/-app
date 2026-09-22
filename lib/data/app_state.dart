import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import 'api_client.dart';
import 'models.dart';

/// 全局应用状态：承担 Session、数据缓存与 WebSocket 实时同步
class AppState extends ChangeNotifier {
  AppState._();
  static final AppState instance = AppState._();

  final ApiClient _api = ApiClient.instance;

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

  WebSocketChannel? _channel;
  StreamSubscription? _wsSub;
  Timer? _reconnectTimer;

  void _setBusy(bool v) {
    busy = v;
    notifyListeners();
  }

  void clearToast() {
    toast = null;
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
      error = e.toString();
      return null;
    } finally {
      if (!silent) _setBusy(false);
      notifyListeners();
    }
  }

  // ---------------- 认证 ----------------

  /// App 启动引导：读取服务器地址 → 恢复上次登录 → 拉取档案
  Future<void> bootstrap() async {
    await _api.loadServerConfig();

    // 首次启动（用户没手动配过地址）时自动探测：
    // 依次尝试 公网隧道 → 局域网，选第一个能通的存下来。
    if (!await _api.hasSavedServerConfig()) {
      final reachable = await _api.probeReachableHost();
      if (reachable != null) {
        await _api.configureServer(host: reachable, port: kDefaultApiPort);
      }
    }

    final saved = await _api.restoreSession();
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
  }) async {
    final res = await _guard(() => _api.register(
          username: username,
          password: password,
          nickname: nickname,
          role: role,
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
    final res = await _guard(() => _api.login(username: username, password: password));
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

  /// 切换后端服务器地址（真机 WiFi 网段可能与开发机不同）
  Future<bool> configureServer({required String host, required int port}) async {
    if (host.trim().isEmpty) return false;
    await _api.configureServer(host: host, port: port);
    final reachable = await _api.ping();
    notifyListeners();
    return reachable;
  }

  // ---------------- 档案 / 配对 ----------------

  Future<void> loadMe() async {
    final me = await _guard(() => _api.me(), silent: true);
    if (me == null) return;
    user = me.user ?? user;
    pair = me.pair;
    shop = me.shop;
    if (user != null) {
      _api.setSession(token: _api.token, userId: user!.id, pairId: pair?.id ?? '');
      await _api.persistSession(user: user!, pairId: pair?.id ?? '');
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
    await refreshMenu(silent: true);
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

  Future<void> refreshTransactions() async {
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
  }) async {
    final item = await _guard(() => _api.createMenuItem(
          name: name,
          price: price,
          description: description,
          imageUrl: emoji,
        ));
    if (item == null) return false;
    toast = '已上新：${item.name}';
    await refreshMenu(silent: true);
    return true;
  }

  // ---------------- WebSocket 实时同步 ----------------

  void connectRealtime() {
    _closeRealtime();
    if (!isPaired || user == null) return;

    try {
      _channel = WebSocketChannel.connect(Uri.parse(_api.wsUrl));
      _wsSub = _channel!.stream.listen(
        (raw) {
          try {
            final evt = jsonDecode(raw as String) as Map<String, dynamic>;
            _handleEvent(evt['type'] as String? ?? '');
          } catch (_) {}
        },
        onDone: _scheduleReconnect,
        onError: (_) => _scheduleReconnect(),
        cancelOnError: true,
      );
    } catch (_) {
      _scheduleReconnect();
    }
  }

  void _scheduleReconnect() {
    _reconnectTimer?.cancel();
    if (!isPaired) return;
    _reconnectTimer = Timer(const Duration(seconds: 3), () {
      if (isPaired) connectRealtime();
    });
  }

  void _closeRealtime() {
    _reconnectTimer?.cancel();
    _wsSub?.cancel();
    _wsSub = null;
    _channel?.sink.close();
    _channel = null;
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
