import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';

/// 菜品图片的**磁盘缓存**。
///
/// 背景（真机 + PC 实测 2026-09-28）：图床 `i*.chuimg.com` 单张 40 KB 的
/// 400×400 图要 2.2–10.6 s（TCP 握手只有 6 ms，慢在传输/图床限速），而
/// Flutter 原生 `Image.network` 只有内存缓存 —— 重启 App、或换个关键词再搜，
/// 同一张图都得重新下载，于是「搜出来的图要等好几秒才出来」。
///
/// 这里补一层持久化缓存：
/// * 目录 `Directory.systemTemp/cozy_dish_img`（Android 上就是 App 的 cache
///   目录，卸载即清，因此不需要 path_provider）；
/// * 文件名用 FNV-1a 64 位哈希（图片 URL 有 100+ 字符，不能直接当文件名）；
/// * 先写 `.part` 再 rename，避免半张图被后续读出来；
/// * 同一张图并发请求只下载一次（in-flight 去重）；
/// * 文件数超过 [_maxFiles] 时按修改时间裁到 [_trimTo]。
class CozyImageCache {
  CozyImageCache._();

  static final CozyImageCache instance = CozyImageCache._();

  static const String _folder = 'cozy_dish_img';
  static const int _maxFiles = 400;
  static const int _trimTo = 300;

  static const String _ua =
      'Mozilla/5.0 (Linux; Android 9; SM-G960F) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36';

  final Dio _dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 8),
      receiveTimeout: const Duration(seconds: 25),
      responseType: ResponseType.bytes,
      headers: const <String, String>{
        'User-Agent': _ua,
        'Accept': 'image/*,*/*;q=0.8',
      },
      validateStatus: (int? code) => code != null && code >= 200 && code < 400,
    ),
  );

  Directory? _dir;
  final Map<String, Future<Uint8List?>> _inflight = <String, Future<Uint8List?>>{};
  bool _pruned = false;

  /// 取图片字节：磁盘命中直接读，否则下载后落盘。
  Future<Uint8List?> bytes(String url) {
    final String key = url.trim();
    if (key.isEmpty) return Future<Uint8List?>.value();
    final Future<Uint8List?>? running = _inflight[key];
    if (running != null) return running;
    final Future<Uint8List?> fut = _load(key);
    _inflight[key] = fut;
    return fut.whenComplete(() => _inflight.remove(key));
  }

  /// 预热：搜索一有结果就并发把前几张拉进盘，用户翻到时已经在本地。
  Future<void> prefetch(Iterable<String> urls, {int concurrency = 4}) async {
    final List<String> pending = <String>[];
    for (final String raw in urls) {
      final String url = raw.trim();
      if (url.isNotEmpty && !pending.contains(url)) pending.add(url);
    }
    if (pending.isEmpty) return;
    int next = 0;
    Future<void> worker() async {
      while (next < pending.length) {
        final String url = pending[next];
        next += 1;
        await bytes(url);
      }
    }

    await Future.wait<void>(
      List<Future<void>>.generate(concurrency, (_) => worker()),
    );
  }

  Future<Uint8List?> _load(String url) async {
    final File? file = await _fileFor(url);
    if (file != null) {
      try {
        if (await file.exists()) {
          final Uint8List cached = await file.readAsBytes();
          if (cached.isNotEmpty) return cached;
          await file.delete();
        }
      } catch (_) {
        // 读盘失败就当没缓存，往下走网络。
      }
    }
    return _download(url, file);
  }

  Future<Uint8List?> _download(String url, File? file) async {
    try {
      final Response<List<int>> resp = await _dio.get<List<int>>(url);
      final List<int>? data = resp.data;
      if (data == null || data.isEmpty) return null;
      final Uint8List out = Uint8List.fromList(data);
      if (file != null) {
        try {
          final File tmp = File('${file.path}.part');
          await tmp.writeAsBytes(out, flush: false);
          await tmp.rename(file.path);
        } catch (_) {
          // 落盘失败不影响本次显示。
        }
      }
      _pruneIfNeeded();
      return out;
    } catch (_) {
      return null;
    }
  }

  Future<File?> _fileFor(String url) async {
    try {
      final Directory dir = await _ensureDir();
      return File('${dir.path}/${_hash(url)}.bin');
    } catch (_) {
      return null;
    }
  }

  Future<Directory> _ensureDir() async {
    final Directory? cached = _dir;
    if (cached != null) return cached;
    final Directory base = await _baseDir();
    final Directory dir = Directory('${base.path}/$_folder');
    if (!await dir.exists()) await dir.create(recursive: true);
    _dir = dir;
    return dir;
  }

  /// App 私有缓存目录。
  ///
  /// **Android 上既没有 TMPDIR 也没有 /tmp**（真机实测 `ls /tmp` → No such file
  /// or directory），`Directory.systemTemp` 因此指向一个建不出来的 `/tmp`，
  /// 落盘会静默失败 —— 磁盘缓存等于没开。这里不引 path_provider（会多带 14 个
  /// 依赖），直接从 `/proc/self` 问出自己是谁：
  /// * `/proc/self/cmdline` → 包名；
  /// * `/proc/self/status` 的 `Uid:` → `userId = uid ~/ 100000`（工作资料会是 10/11…）。
  ///
  /// 非 Android 平台仍用 `Directory.systemTemp`（桌面端就是正常的临时目录）。
  static Future<Directory> _baseDir() async {
    if (Platform.isAndroid) {
      final String pkg = await _androidPackage();
      if (pkg.isNotEmpty) {
        final int userId = await _androidUserId();
        for (final String root in <String>[
          '/data/user/$userId/$pkg/cache',
          '/data/data/$pkg/cache',
        ]) {
          try {
            final Directory dir = Directory(root);
            if (await dir.exists()) return dir;
          } catch (_) {}
        }
      }
    }
    return Directory.systemTemp;
  }

  static Future<String> _androidPackage() async {
    try {
      final String raw = await File('/proc/self/cmdline').readAsString();
      for (final String part in raw.split('\u0000')) {
        final String name = part.trim();
        if (name.isNotEmpty && name.contains('.')) return name;
      }
    } catch (_) {}
    return '';
  }

  static Future<int> _androidUserId() async {
    try {
      final String raw = await File('/proc/self/status').readAsString();
      for (final String line in raw.split('\n')) {
        if (!line.startsWith('Uid:')) continue;
        final List<String> parts = line.substring(4).trim().split(RegExp(r'\s+'));
        if (parts.isEmpty) break;
        final int? uid = int.tryParse(parts.first);
        if (uid != null) return uid ~/ 100000;
      }
    } catch (_) {}
    return 0;
  }

  void _pruneIfNeeded() {
    if (_pruned) return;
    _pruned = true;
    unawaited(_prune());
  }

  Future<void> _prune() async {
    try {
      final Directory dir = await _ensureDir();
      final List<File> files = <File>[];
      await for (final FileSystemEntity entity in dir.list()) {
        if (entity is File) files.add(entity);
      }
      if (files.length <= _maxFiles) return;
      final List<MapEntry<DateTime, File>> stats = <MapEntry<DateTime, File>>[];
      for (final File file in files) {
        try {
          stats.add(MapEntry<DateTime, File>((await file.stat()).modified, file));
        } catch (_) {}
      }
      stats.sort((MapEntry<DateTime, File> a, MapEntry<DateTime, File> b) =>
          a.key.compareTo(b.key));
      final int remove = stats.length - _trimTo;
      for (int i = 0; i < remove && i < stats.length; i++) {
        try {
          await stats[i].value.delete();
        } catch (_) {}
      }
    } catch (_) {}
  }

  /// FNV-1a 64 位（取低 63 位保证非负）。
  static String _hash(String input) {
    int h = 0xcbf29ce484222325;
    for (final int unit in input.codeUnits) {
      h ^= unit;
      h *= 0x100000001b3;
      h &= 0x7FFFFFFFFFFFFFFF;
    }
    return h.toRadixString(16).padLeft(16, '0');
  }
}
