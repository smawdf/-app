import 'package:dio/dio.dart';

import 'local_store.dart';
import 'models.dart';

/// 默认后端地址：优先使用公网隧道，手机在任何网络下都能连上。
/// 可在构建时覆盖：
/// flutter build apk --dart-define=API_HOST=192.168.1.6 --dart-define=API_PORT=8085
/// 运行时也可在登录页「服务器设置」里修改，并会持久化到本地。
const String kDefaultApiHost = String.fromEnvironment(
  'API_HOST',
  defaultValue: 'https://orderdisk-couple.loca.lt',
);
const int kDefaultApiPort = int.fromEnvironment('API_PORT', defaultValue: 8085);

/// 自动探测的后端候选地址（按优先级）：
/// 1. 公网隧道 —— 手机在外网/4G 也能连
/// 2. 局域网 IP —— 在家同 WiFi 时延迟最低、最稳定
const List<String> kApiHostCandidates = [
  'https://orderdisk-couple.loca.lt',
  '192.168.1.6',
];

const String _kPrefHost = 'server_host';
const String _kPrefPort = 'server_port';
const String _kPrefToken = 'session_token';
const String _kPrefUserId = 'session_user_id';
const String _kPrefPairId = 'session_pair_id';
const String _kPrefUserJson = 'session_user_json';

class ApiException implements Exception {
  final String message;
  final int? statusCode;
  ApiException(this.message, {this.statusCode});

  @override
  String toString() => message;
}

class ApiClient {
  ApiClient._() {
    _dio = Dio(BaseOptions(
      baseUrl: 'http://$host:$port/api/v1',
      connectTimeout: const Duration(seconds: 8),
      receiveTimeout: const Duration(seconds: 12),
      contentType: Headers.jsonContentType,
      // 自己处理非 2xx，便于读取后端的业务错误文案
      validateStatus: (s) => s != null && s < 500,
    ));

    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        if (_token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer $_token';
        }
        handler.next(options);
      },
    ));
  }

  static final ApiClient instance = ApiClient._();
  late final Dio _dio;

  String _host = kDefaultApiHost;
  int _port = kDefaultApiPort;

  String get host => _host;
  int get port => _port;
  String get baseUrl {
    if (_host.startsWith('http://') || _host.startsWith('https://')) {
      return '$_host/api/v1';
    }
    return 'http://$_host:$_port/api/v1';
  }

  String _token = '';
  String _userId = '';
  String _pairId = '';

  String get token => _token;
  String get wsUrl {
    if (_host.startsWith('https://')) {
      final domain = _host.replaceFirst('https://', '');
      return 'wss://$domain/ws?pair_id=$_pairId&user_id=$_userId';
    } else if (_host.startsWith('http://')) {
      final domain = _host.replaceFirst('http://', '');
      return 'ws://$domain/ws?pair_id=$_pairId&user_id=$_userId';
    }
    return 'ws://$_host:$_port/ws?pair_id=$_pairId&user_id=$_userId';
  }

  /// 从本地读取服务器地址（真机 WiFi 网段可能与开发机不同）
  Future<void> loadServerConfig() async {
    final store = LocalStore.instance;
    _host = await store.getString(_kPrefHost) ?? kDefaultApiHost;
    _port = await store.getInt(_kPrefPort) ?? kDefaultApiPort;
    _dio.options.baseUrl = baseUrl;
  }

  /// 是否已经保存过用户手动配置的地址
  Future<bool> hasSavedServerConfig() async {
    final saved = await LocalStore.instance.getString(_kPrefHost);
    return saved != null && saved.isNotEmpty;
  }

  /// 依次探测候选地址，返回第一个可达的；全部失败返回 null。
  /// 用于首次启动时自动选择「公网隧道」或「局域网」。
  Future<String?> probeReachableHost({Duration timeout = const Duration(seconds: 4)}) async {
    for (final candidate in kApiHostCandidates) {
      final probe = Dio(BaseOptions(
        baseUrl: candidate.startsWith('http') ? '$candidate/api/v1' : 'http://$candidate:$kDefaultApiPort/api/v1',
        connectTimeout: timeout,
        receiveTimeout: timeout,
        contentType: Headers.jsonContentType,
        validateStatus: (s) => s != null && s < 500,
      ));
      try {
        await probe.post('/auth/login', data: {'username': '__probe__', 'password': '__probe__'});
        return candidate;
      } catch (_) {
        // 试下一个候选地址
      }
    }
    return null;
  }

  /// 运行时切换服务器地址并持久化
  Future<void> configureServer({required String host, required int port}) async {
    _host = host.trim();
    _port = port;
    _dio.options.baseUrl = baseUrl;
    final store = LocalStore.instance;
    await store.setString(_kPrefHost, _host);
    await store.setInt(_kPrefPort, _port);
  }

  /// 连通性自检：命中一个必然返回业务错误的公开路由，只要拿到 HTTP 响应即视为可达
  Future<bool> ping() async {
    try {
      await _dio.post('/auth/login', data: {'username': '__ping__', 'password': '__ping__'});
      return true;
    } on DioException {
      return false;
    } catch (_) {
      return true;
    }
  }

  void setSession({required String token, required String userId, required String pairId}) {
    _token = token;
    _userId = userId;
    _pairId = pairId;
  }

  void clearSession() {
    _token = '';
    _userId = '';
    _pairId = '';
  }

  // ---------------- 本地会话持久化 ----------------

  Future<void> persistSession({required AppUser user, required String pairId}) async {
    final store = LocalStore.instance;
    await store.setString(_kPrefToken, _token);
    await store.setString(_kPrefUserId, _userId);
    await store.setString(_kPrefPairId, pairId);
    await store.setString(_kPrefUserJson, user.toJsonString());
  }

  Future<void> updatePersistedPairId(String pairId) async {
    await LocalStore.instance.setString(_kPrefPairId, pairId);
  }

  /// 恢复上次登录态，返回 (token, userId, pairId, user)
  Future<({String token, String userId, String pairId, AppUser? user})?> restoreSession() async {
    final store = LocalStore.instance;
    final token = await store.getString(_kPrefToken) ?? '';
    final userId = await store.getString(_kPrefUserId) ?? '';
    if (token.isEmpty || userId.isEmpty) return null;
    final rawUser = await store.getString(_kPrefUserJson) ?? '';
    return (
      token: token,
      userId: userId,
      pairId: await store.getString(_kPrefPairId) ?? '',
      user: rawUser.isEmpty ? null : AppUser.fromJsonString(rawUser),
    );
  }

  Future<void> clearPersistedSession() async {
    final store = LocalStore.instance;
    await store.remove(_kPrefToken);
    await store.remove(_kPrefUserId);
    await store.remove(_kPrefPairId);
    await store.remove(_kPrefUserJson);
  }

  /// 统一解析响应：2xx 返回 data，否则抛出带友好文案的异常
  dynamic _unwrap(Response res) {
    final data = res.data;
    if (res.statusCode != null && res.statusCode! >= 200 && res.statusCode! < 300) {
      return data;
    }
    String msg = '请求失败 (${res.statusCode})';
    if (data is Map) {
      final friendly = data['message'];
      final raw = data['error'];
      if (friendly is String && friendly.isNotEmpty) {
        msg = friendly;
      } else if (raw is String && raw.isNotEmpty) {
        msg = raw;
      }
    }
    throw ApiException(msg, statusCode: res.statusCode);
  }

  Future<dynamic> _post(String path, Map<String, dynamic> body) async {
    try {
      return _unwrap(await _dio.post(path, data: body));
    } on DioException catch (e) {
      throw ApiException(_networkMessage(e));
    }
  }

  Future<dynamic> _get(String path) async {
    try {
      return _unwrap(await _dio.get(path));
    } on DioException catch (e) {
      throw ApiException(_networkMessage(e));
    }
  }

  String _networkMessage(DioException e) {
    return switch (e.type) {
      DioExceptionType.connectionTimeout => '连接超时，请确认后端服务已启动（$baseUrl）',
      DioExceptionType.receiveTimeout => '服务器响应超时',
      DioExceptionType.connectionError => '无法连接后端（$baseUrl），请检查 WiFi 与服务器地址设置',
      _ => e.message ?? '网络异常',
    };
  }

  // ---------------- 认证 ----------------

  Future<({String token, AppUser user})> register({
    required String username,
    required String password,
    required String nickname,
    required String role,
  }) async {
    final data = await _post('/auth/register', {
      'username': username,
      'password': password,
      'nickname': nickname,
      'role': role,
    }) as Map<String, dynamic>;
    return (
      token: data['token'] as String? ?? '',
      user: AppUser.fromJson(data['user'] as Map<String, dynamic>),
    );
  }

  Future<({String token, AppUser user})> login({
    required String username,
    required String password,
  }) async {
    final data = await _post('/auth/login', {
      'username': username,
      'password': password,
    }) as Map<String, dynamic>;
    return (
      token: data['token'] as String? ?? '',
      user: AppUser.fromJson(data['user'] as Map<String, dynamic>),
    );
  }

  Future<MeProfile> me() async {
    final data = await _get('/me') as Map<String, dynamic>;
    return MeProfile.fromJson(data);
  }

  // ---------------- 配对 ----------------

  Future<({CouplePair pair, String token})> createPair() async {
    final data = await _post('/pairs', {}) as Map<String, dynamic>;
    return (
      pair: CouplePair.fromJson(data),
      token: data['token'] as String? ?? '',
    );
  }

  Future<({CouplePair pair, String token})> joinPair(String inviteCode) async {
    final data = await _post('/pairs/join', {'invite_code': inviteCode}) as Map<String, dynamic>;
    return (
      pair: CouplePair.fromJson(data),
      token: data['token'] as String? ?? '',
    );
  }

  // ---------------- 菜单 ----------------

  Future<List<MenuItem>> menu() async {
    final data = await _get('/menu') as Map<String, dynamic>;
    return ((data['items'] as List?) ?? const [])
        .map((e) => MenuItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<MenuItem> createMenuItem({
    required String name,
    required double price,
    String description = '',
    String imageUrl = '',
  }) async {
    final data = await _post('/menu', {
      'name': name,
      'price': price,
      'description': description,
      'image_url': imageUrl,
    }) as Map<String, dynamic>;
    return MenuItem.fromJson(data);
  }

  // ---------------- 订单 ----------------

  Future<List<Order>> listOrders() async {
    final data = await _get('/orders') as Map<String, dynamic>;
    return ((data['orders'] as List?) ?? const [])
        .map((e) => Order.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<Order> submitOrder({
    required String note,
    required List<MenuItem> dishes,
  }) async {
    final data = await _post('/orders', {
      'buyer_note': note,
      'items': dishes
          .map((d) => {
                'menu_id': d.id,
                'name': d.name,
                'image_url': d.imageUrl,
                'unit_price': d.price,
                'quantity': 1,
              })
          .toList(),
    }) as Map<String, dynamic>;
    return Order.fromJson(data);
  }

  Future<Order> advanceOrder(String orderId, String newStatus) async {
    // 后端为 PUT /orders/:order_id/status
    final data = await put('/orders/$orderId/status', {'new_status': newStatus})
        as Map<String, dynamic>;
    return Order.fromJson(data);
  }

  Future<void> cancelOrder(String orderId) async {
    await _post('/orders/$orderId/cancel', {});
  }

  // ---------------- 糖糖币 ----------------

  Future<int> rechargeCandy({required int amount, String reason = ''}) async {
    final data = await _post('/candy/recharge', {
      'amount': amount,
      'reason': reason,
    }) as Map<String, dynamic>;
    return data['candy_coins'] as int? ?? 0;
  }

  Future<List<CandyTransaction>> candyTransactions() async {
    final data = await _get('/candy/transactions') as Map<String, dynamic>;
    return ((data['transactions'] as List?) ?? const [])
        .map((e) => CandyTransaction.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// 用于 PUT 的便捷封装（dio 的 put 与 post 在此项目里用法一致）
  Future<dynamic> put(String path, Map<String, dynamic> body) async {
    try {
      return _unwrap(await _dio.put(path, data: body));
    } on DioException catch (e) {
      throw ApiException(_networkMessage(e));
    }
  }
}
