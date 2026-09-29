import 'dart:async';

import 'package:flutter/widgets.dart';

import 'models.dart';
import 'order_errors.dart';
import 'supabase_api.dart';

/// _guard 的加载域：Phase 0 拆掉一把梭的全局 busy 后，每个非 silent 请求
/// 只点亮自己领域的 loading，别的页面不再跟着转圈。
enum _BusyDomain { auth, menu, orders, misc }

/// 【性能 Phase 1】重建域：页面只订阅自己真正读取的字段所在的域。
/// 加菜/糖币变动/订单推进时，无关页面零重建。
enum Domain { auth, profile, menu, orders, candy, toast }

/// 单个域的可监听信号。
class _DomainSignal extends ChangeNotifier {
  void ping() => notifyListeners();
}

/// 全局应用状态：承担 Session、数据缓存与云端实时同步（Supabase）
class AppState extends ChangeNotifier with WidgetsBindingObserver {
  AppState._() {
    // 【Phase 0】观察应用前后台：退后台停兜底轮询，回前台立即补刷。
    WidgetsBinding.instance.addObserver(this);
  }
  static final AppState instance = AppState._();

  final SupabaseApi _api = SupabaseApi.instance;

  // ——【性能 Phase 1】域级监听实现 ——
  // 全局 notifyListeners() 全部保留为兼容兜底：页面迁移到域级订阅后，
  // 全局通知已无监听者，近乎零开销。
  final Map<Domain, _DomainSignal> _signals = {
    for (final Domain d in Domain.values) d: _DomainSignal(),
  };

  /// 页面按需组合订阅，如 `state.listenFor(const {Domain.menu, Domain.orders})`。
  Listenable listenFor(Set<Domain> domains) => Listenable.merge(
        [for (final Domain d in domains) _signals[d]!],
      );

  /// 点亮一组域：只有订阅这些域的页面会重建。
  void _mark(Set<Domain> domains) {
    for (final Domain d in domains) {
      _signals[d]!.ping();
    }
  }

  /// 统一的 toast 出口：顺手点亮 toast 域，外壳只在这一域重建。
  void _showToast(String message) {
    toast = message;
    _signals[Domain.toast]!.ping();
  }

  AppUser? user;
  CouplePair? pair;
  Shop? shop;
  List<MenuItem> menu = [];
  List<Order> orders = [];
  List<CandyTransaction> transactions = [];

  String? error;
  String? toast;

  // ——【Phase 0】按域加载位，替代旧的全局 busy ——
  /// 登录 / 注册 / 配对请求进行中（只该锁认证相关按钮）。
  bool busyAuth = false;

  /// 菜单拉取进行中（点菜页骨架屏用）。
  bool loadingMenu = false;

  /// 订单拉取 / 下单 / 推进 / 取消进行中。
  bool loadingOrders = false;

  bool _busyMisc = false;

  /// 兼容旧调用的聚合 busy：任一域在忙即 true。
  bool get busy => busyAuth || loadingMenu || loadingOrders || _busyMisc;

  AppLifecycleState _lifecycle = AppLifecycleState.resumed;

  /// 本端最近一次本地写订单/糖币的时间：用来把「自己操作触发的 Realtime 回声」
  /// 与「对方真实动作」区分开，避免自己给自己弹 toast。
  DateTime _lastLocalOrderWrite = DateTime.fromMillisecondsSinceEpoch(0);
  DateTime _lastLocalCandyWrite = DateTime.fromMillisecondsSinceEpoch(0);

  /// 订单 id → 最近已知状态，Realtime 事件 diff 用。
  final Map<String, String> _lastKnownOrderStatus = <String, String>{};

  bool get _justWroteOrderLocally =>
      DateTime.now().difference(_lastLocalOrderWrite) < const Duration(seconds: 5);

  bool get _justWroteCandyLocally =>
      DateTime.now().difference(_lastLocalCandyWrite) < const Duration(seconds: 5);

  bool get isLoggedIn => user != null;
  bool get isPaired => pair != null && (pair!.caretakerId.isNotEmpty || pair!.eaterId.isNotEmpty);
  bool get isCaretaker => user?.isCaretaker ?? false;
  int get candyCoins => pair?.candyCoins ?? 0;

  StreamSubscription? _rtSub;
  Timer? _reconnectTimer;
  Timer? _pollTimer;

  void _setBusy(_BusyDomain domain, bool v) {
    if (domain == _BusyDomain.auth) {
      busyAuth = v;
      _mark(const {Domain.auth});
    } else if (domain == _BusyDomain.menu) {
      loadingMenu = v;
      _mark(const {Domain.menu});
    } else if (domain == _BusyDomain.orders) {
      loadingOrders = v;
      _mark(const {Domain.orders});
    } else {
      _busyMisc = v;
    }
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

  Future<T?> _guard<T>(
    Future<T> Function() action, {
    bool silent = false,
    _BusyDomain domain = _BusyDomain.misc,
  }) async {
    if (!silent) _setBusy(domain, true);
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
      if (!silent) _setBusy(domain, false);
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
      _mark(const {Domain.auth});
      return;
    }
    _api.setSession(token: saved.token, userId: saved.userId, pairId: saved.pairId);
    user = saved.user;
    notifyListeners();
    _mark(const {Domain.auth, Domain.profile});
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
    final res = await _guard(
      () => _api.register(
        email: username,
        password: password,
        nickname: nickname,
        role: role,
        avatarUrl: avatarUrl,
      ),
      domain: _BusyDomain.auth,
    );
    if (res == null) return false;
    _api.setSession(token: res.token, userId: res.user.id, pairId: res.user.pairId);
    user = res.user;
    _mark(const {Domain.auth, Domain.profile});
    await _api.persistSession(user: res.user, pairId: res.user.pairId);
    await loadMe();
    connectRealtime();
    return true;
  }

  Future<bool> login({required String username, required String password}) async {
    final res = await _guard(
      () => _api.login(email: username, password: password),
      domain: _BusyDomain.auth,
    );
    if (res == null) return false;
    _api.setSession(token: res.token, userId: res.user.id, pairId: res.user.pairId);
    user = res.user;
    _mark(const {Domain.auth, Domain.profile});
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
    _mark(const {Domain.auth, Domain.profile, Domain.menu, Domain.orders, Domain.candy});
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
    // 【性能 Phase 1】档案/糖币域各归各：只重建真正读取 user/pair 的页面。
    _mark(const {Domain.profile, Domain.candy});
  }

  Future<bool> createPair() async {
    final res = await _guard(() => _api.createPair(), domain: _BusyDomain.auth);
    if (res == null) return false;
    pair = res.pair;
    _mark(const {Domain.profile});
    if (res.token.isNotEmpty) {
      _api.setSession(token: res.token, userId: user!.id, pairId: res.pair.id);
    }
    await _api.updatePersistedPairId(res.pair.id);
    _showToast('邀请码已生成：${res.pair.inviteCode}');
    await refreshMenu(silent: true);
    connectRealtime();
    return true;
  }

  Future<bool> joinPair(String inviteCode) async {
    final res = await _guard(
      () => _api.joinPair(inviteCode.trim().toUpperCase()),
      domain: _BusyDomain.auth,
    );
    if (res == null) return false;
    pair = res.pair;
    _mark(const {Domain.profile});
    if (res.token.isNotEmpty) {
      _api.setSession(token: res.token, userId: user!.id, pairId: res.pair.id);
    }
    await _api.updatePersistedPairId(res.pair.id);
    _showToast('绑定成功，你们的小店已连通 💕');
    // 【真机修正】`joinPair` 的 RPC 只回邀请码，没有伴侣昵称/头像；绑定完成后再
    // 拉一次 `me()`，首页情侣卡和我的页才能立刻显示对方的名字（否则要等下次启动）。
    await loadMe();
    connectRealtime();
    return true;
  }

  // ---------------- 数据刷新 ----------------

  Future<void> refreshMenu({bool silent = false}) async {
    final list = await _guard(() => _api.menu(), silent: silent, domain: _BusyDomain.menu);
    // 【性能 Phase 1】轮询/Realtime 回来内容没变 → 不赋值不点亮，页面零重建。
    if (list != null && !_sameMenu(menu, list)) {
      menu = list;
      _mark(const {Domain.menu});
    }
  }

  Future<void> refreshOrders({bool silent = false}) async {
    final list = await _guard(() => _api.listOrders(), silent: silent, domain: _BusyDomain.orders);
    if (list != null && !_sameOrders(orders, list)) {
      orders = list;
      _mark(const {Domain.orders});
    }
  }

  Future<void> refreshTransactions({bool silent = false}) async {
    final list = await _guard(() => _api.candyTransactions(), silent: true);
    if (list != null && !_sameTransactions(transactions, list)) {
      transactions = list;
      _mark(const {Domain.candy});
    }
  }

  Future<void> refreshAll() async {
    await loadMe();
    await refreshTransactions();
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
    final order = await _guard(
      () => _api.submitOrder(note: note, dishes: dishes),
      domain: _BusyDomain.orders,
    );
    if (order == null) return false;
    _lastLocalOrderWrite = DateTime.now();
    _showToast('点单成功！已消费 ${order.candyCoinsSpent} 糖币');
    await Future.wait([refreshOrders(silent: true), loadMe()]);
    return true;
  }

  Future<bool> advanceOrder(Order order) async {
    final next = order.nextStatus;
    if (next == null) return false;
    final updated = await _guard(
      () => _api.advanceOrder(order.id, next),
      domain: _BusyDomain.orders,
    );
    if (updated == null) return false;
    _lastLocalOrderWrite = DateTime.now();
    await refreshOrders(silent: true);
    return true;
  }

  Future<bool> cancelOrder(Order order) async {
    final ok = await _guard(
      () async {
        await _api.cancelOrder(order.id);
        return true;
      },
      domain: _BusyDomain.orders,
    );
    if (ok == null) return false;
    _lastLocalOrderWrite = DateTime.now();
    _showToast('订单已取消，糖币已退还');
    await Future.wait([refreshOrders(silent: true), loadMe()]);
    return true;
  }

  Future<bool> recharge({required int amount, String reason = ''}) async {
    final balance = await _guard(() => _api.rechargeCandy(amount: amount, reason: reason));
    if (balance == null) return false;
    _lastLocalCandyWrite = DateTime.now();
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
    _mark(const {Domain.candy, Domain.profile});
    _showToast('撒糖成功，对方已收到 $amount 糖币 🍬');
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
    final item = await _guard(
      () => _api.createMenuItem(
        name: name,
        price: price,
        description: description,
        imageUrl: emoji,
        category: category,
      ),
      domain: _BusyDomain.menu,
    );
    if (item == null) return false;
    _showToast('已上新：${item.name}');
    await refreshMenu(silent: true);
    return true;
  }

  Future<bool> updateShopInfo({required String name, required String announcement}) async {
    final updated = await _guard(() => _api.updateShop(name: name, announcement: announcement));
    if (updated != null) {
      shop = updated;
      _mark(const {Domain.profile});
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
    _mark(const {Domain.profile});
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
    _mark(const {Domain.profile});
    return true;
  }

  // ---------------- Supabase 云端实时同步 ----------------

  /// 订阅云端变更：优先用 Supabase Realtime，同时保留 8 秒兜底轮询
  /// （仅前台运行，见 [_ensurePollTimer]），
  /// 即使项目未把表加入 realtime publication，界面也不会停止刷新。
  void connectRealtime() {
    _closeRealtime();
    if (!isPaired || user == null) return;

    // 以当前内存里的订单为 diff 基线：否则 Realtime 首个快照里全是「新行」，
    // 会把历史订单误报成对方刚点的新单。
    _lastKnownOrderStatus
      ..clear()
      ..addEntries(orders.map((o) => MapEntry(o.id, o.status)));

    try {
      _rtSub = _api.realtimeEvents().listen(
            _onRealtimeEvent,
            onError: (_) => _scheduleReconnect(),
          );
    } catch (_) {
      _scheduleReconnect();
    }

    _ensurePollTimer();
  }

  /// 【Phase 0】兜底轮询只在前台跑：App 退后台后 Flutter UI 被冻结，
  /// 8s 一次的 orders+transactions 拉取纯属耗电耗流量；Realtime 通道保持，
  /// 云端有推送时系统会唤醒处理。
  void _ensurePollTimer() {
    _pollTimer?.cancel();
    _pollTimer = null;
    if (!isPaired || _lifecycle != AppLifecycleState.resumed) return;
    _pollTimer = Timer.periodic(const Duration(seconds: 8), (_) {
      if (!isPaired || _lifecycle != AppLifecycleState.resumed) return;
      refreshOrders(silent: true);
      refreshTransactions(silent: true);
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final bool wasResumed = _lifecycle == AppLifecycleState.resumed;
    _lifecycle = state;
    if (state == AppLifecycleState.resumed && !wasResumed) {
      // 回前台：立即补一次刷新，再恢复兜底轮询。
      if (isPaired) {
        refreshOrders(silent: true);
        refreshTransactions(silent: true);
      }
      _ensurePollTimer();
    } else if (state != AppLifecycleState.resumed) {
      _pollTimer?.cancel();
      _pollTimer = null;
    }
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

  /// Realtime 事件入口：按来源表分发。
  void _onRealtimeEvent(RealtimeEvent event) {
    switch (event.table) {
      case 'orders':
        _diffOrders(event.rows);
        break;
      case 'candy_coin_records':
        _onCandyRecords(event.rows);
        break;
      case 'menu_dishes':
        // 饲养员上新/改价/下架 → 对方端静默刷新，无需打扰。
        refreshMenu(silent: true);
        break;
      case 'profiles':
        _detectPairJoined();
        break;
    }
    // 【性能 Phase 1】事件内部的刷新/toast 已各自点亮对应域，不再全量通知。
  }

  /// 订单表 diff：把「哪张单从什么状态变成什么状态」还原成伴侣提示。
  void _diffOrders(List<Map<String, dynamic>> rows) {
    if (rows.isEmpty) return;
    final String myId = user?.id ?? '';
    bool sawChange = false;

    for (final row in rows) {
      final String id = (row['id'] as String?) ?? '';
      final String status = (row['status'] as String?) ?? '';
      final String buyerId = (row['user_id'] as String?) ?? '';
      if (id.isEmpty || status.isEmpty) continue;

      final String? old = _lastKnownOrderStatus[id];
      if (old == status) continue;

      if (old == null) {
        // 从没见过的行：要么是对方刚点的单，要么是重连后的历史快照。
        // 只有「对方新下的单」才提示，自己刚点的由 submitOrder 的 toast 负责。
        if (buyerId.isNotEmpty &&
            buyerId != myId &&
            status == 'submitted' &&
            !_justWroteOrderLocally) {
          _showToast('🔔 对方点菜啦！');
        }
      } else if (status == 'cancelled') {
        // 【Phase 0 修复】旧版 listen 硬编码 'order_updated'，取消提示永远不可达。
        if (buyerId != myId && !_justWroteOrderLocally) {
          _showToast('对方取消了订单，糖币已退还');
        }
      } else if (buyerId != myId && !_justWroteOrderLocally) {
        // 对方在推进订单（吃货端看到做饭进度）。
        switch (status) {
          case 'confirmed':
            _showToast('🍳 对方接单啦，准备开饭！');
            break;
          case 'preparing':
            _showToast('🍳 对方正在做饭啦！');
            break;
          case 'completed':
            _showToast('🍽️ 对方上菜啦，开饭！');
            break;
          default:
            _showToast('订单状态有更新');
        }
      }
      _lastKnownOrderStatus[id] = status;
      sawChange = true;
    }

    if (sawChange) refreshOrders(silent: true);
  }

  /// 糖币记录 diff：只有「对方充值的记录」才弹撒糖提示；
  /// 吃货的消费/退款记录静默刷新（订单 diff 已有提示，避免双重打扰）。
  void _onCandyRecords(List<Map<String, dynamic>> rows) {
    if (rows.isEmpty) return;
    refreshTransactions(silent: true);

    final bool hasPartnerRecharge = rows.any((r) {
      final String type = (r['type'] as String?) ?? '';
      final bool isRecharge = type == 'recharge' || type == 'partner_recharge' || type == 'add';
      if (!isRecharge) return false;
      // 记录行尽量核对发起人：自己刚充的不提示（本地 toast 已覆盖）。
      final dynamic actor = r['user_id'] ?? r['actor_id'] ?? r['created_by'];
      if (actor is String && actor.isNotEmpty) {
        return actor != (user?.id ?? '');
      }
      return !_justWroteCandyLocally;
    });

    if (hasPartnerRecharge) {
      _showToast('🍬 对方给你撒糖啦！');
      loadMe();
    }
  }

  /// profiles 变更：伴侣绑定完成（从不完整 → 完整）时提示并重拉档案。
  void _detectPairJoined() {
    final bool wasFullyBound = pair?.isFullyBound ?? false;
    loadMe().then((_) {
      if (!wasFullyBound && (pair?.isFullyBound ?? false)) {
        _showToast('💕 伴侣已绑定成功');
      }
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _closeRealtime();
    super.dispose();
  }
}

// ——【性能 Phase 1】内容去重 helper ——
// 轮询/Realtime 回来的数据没变时，refreshX 直接跳过赋值与域点亮，
// 页面零重建。逐字段比对（模型无 == 覆写，default 是引用比较）。

bool _sameMenu(List<MenuItem> a, List<MenuItem> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    final MenuItem x = a[i], y = b[i];
    if (x.id != y.id ||
        x.name != y.name ||
        x.description != y.description ||
        x.price != y.price ||
        x.imageUrl != y.imageUrl ||
        x.salesCount != y.salesCount ||
        x.isAvailable != y.isAvailable ||
        x.category != y.category) {
      return false;
    }
  }
  return true;
}

bool _sameOrders(List<Order> a, List<Order> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    final Order x = a[i], y = b[i];
    if (x.id != y.id ||
        x.buyerId != y.buyerId ||
        x.buyerName != y.buyerName ||
        x.status != y.status ||
        x.buyerNote != y.buyerNote ||
        x.totalPrice != y.totalPrice ||
        x.candyCoinsSpent != y.candyCoinsSpent ||
        x.momentImageUrl != y.momentImageUrl ||
        x.createdAt != y.createdAt ||
        !_sameOrderItems(x.items, y.items)) {
      return false;
    }
  }
  return true;
}

bool _sameOrderItems(List<OrderItem> a, List<OrderItem> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    final OrderItem x = a[i], y = b[i];
    if (x.id != y.id ||
        x.name != y.name ||
        x.imageUrl != y.imageUrl ||
        x.unitPrice != y.unitPrice ||
        x.quantity != y.quantity ||
        x.subtotal != y.subtotal) {
      return false;
    }
  }
  return true;
}

bool _sameTransactions(List<CandyTransaction> a, List<CandyTransaction> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    final CandyTransaction x = a[i], y = b[i];
    if (x.type != y.type ||
        x.amount != y.amount ||
        x.balance != y.balance ||
        x.description != y.description ||
        x.createdAt != y.createdAt) {
      return false;
    }
  }
  return true;
}
