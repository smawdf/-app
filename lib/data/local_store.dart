import 'dart:convert';

import 'package:flutter/services.dart';

/// 基于原生 MethodChannel 的极简键值存储（零第三方插件依赖）。
///
/// 数据以单个 JSON 文件保存在 App 私有目录，用于持久化
/// 服务器地址与登录会话，保证真机重启 App 后无需重新登录。
class LocalStore {
  LocalStore._();
  static final LocalStore instance = LocalStore._();

  static const _channel = MethodChannel('com.myorderapp.orderdisk_flutter/store');

  Map<String, dynamic> _cache = {};
  bool _loaded = false;

  Future<void> _ensureLoaded() async {
    if (_loaded) return;
    try {
      final raw = await _channel.invokeMethod<String>('read') ?? '';
      if (raw.isNotEmpty) {
        final decoded = jsonDecode(raw);
        if (decoded is Map<String, dynamic>) _cache = decoded;
      }
    } catch (_) {
      _cache = {};
    }
    _loaded = true;
  }

  Future<void> _flush() async {
    try {
      await _channel.invokeMethod<bool>('write', {'content': jsonEncode(_cache)});
    } catch (_) {
      // 存储失败不应阻断主流程
    }
  }

  Future<String?> getString(String key) async {
    await _ensureLoaded();
    final v = _cache[key];
    return v is String ? v : null;
  }

  Future<int?> getInt(String key) async {
    await _ensureLoaded();
    final v = _cache[key];
    return v is int ? v : null;
  }

  Future<void> setString(String key, String value) async {
    await _ensureLoaded();
    _cache[key] = value;
    await _flush();
  }

  Future<void> setInt(String key, int value) async {
    await _ensureLoaded();
    _cache[key] = value;
    await _flush();
  }

  Future<void> remove(String key) async {
    await _ensureLoaded();
    _cache.remove(key);
    await _flush();
  }

  Future<void> clear() async {
    _cache = {};
    _loaded = true;
    try {
      await _channel.invokeMethod<bool>('clear');
    } catch (_) {}
  }
}
