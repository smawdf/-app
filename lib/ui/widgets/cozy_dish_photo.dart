import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../../data/food_images.dart';
import '../../data/image_cache.dart';
import '../theme/cozy_glass.dart';

/// 走 [CozyImageCache] 的图片 provider：磁盘缓存命中就秒开，未命中才走网络。
///
/// 为什么不直接用 `NetworkImage`：它只有内存缓存（`PaintingBinding.imageCache`），
/// 进程一重启就全丢；而图床单张图要 2–10 s，重复下载就是「图加载很慢」的来源。
class CozyNetworkImage extends ImageProvider<CozyNetworkImage> {
  const CozyNetworkImage(this.url, {this.scale = 1.0});

  final String url;
  final double scale;

  @override
  Future<CozyNetworkImage> obtainKey(ImageConfiguration configuration) =>
      SynchronousFuture<CozyNetworkImage>(this);

  @override
  ImageStreamCompleter loadImage(CozyNetworkImage key, ImageDecoderCallback decode) {
    return MultiFrameImageStreamCompleter(
      codec: _load(key, decode),
      scale: key.scale,
      debugLabel: key.url,
    );
  }

  Future<ui.Codec> _load(CozyNetworkImage key, ImageDecoderCallback decode) async {
    final Uint8List? bytes = await CozyImageCache.instance.bytes(key.url);
    if (bytes == null || bytes.isEmpty) {
      throw NetworkImageLoadException(statusCode: 404, uri: Uri.parse(key.url));
    }
    final ui.ImmutableBuffer buffer = await ui.ImmutableBuffer.fromUint8List(bytes);
    return decode(buffer);
  }

  @override
  bool operator ==(Object other) =>
      other is CozyNetworkImage && other.url == url && other.scale == scale;

  @override
  int get hashCode => Object.hash(url, scale);

  @override
  String toString() => 'CozyNetworkImage("$url", scale: $scale)';
}

/// 菜品图统一入口。
///
/// 解决三件事：
/// 1. **按显示尺寸要图** —— 卡片只有 84 dp，却每次都向图床要 400×400 的图，
///    白下 2.5 倍字节；这里按 `cssWidth × devicePixelRatio` 选图床档位
///    （下厨房图床支持 `imageView2/…/w/<px>/…/format/webp`）。
/// 2. **磁盘缓存** —— 见 [CozyNetworkImage]，同一张图二次显示不等网络。
/// 3. **不跳版** —— 图没到时先铺同尺寸占位块，图到了 240 ms 淡入，
///    而不是先留白再「啪」地出现。
class CozyDishPhoto extends StatelessWidget {
  const CozyDishPhoto({
    super.key,
    required this.url,
    required this.cssWidth,
    this.fit = BoxFit.cover,
    this.fallback,
    this.placeholderColor,
  });

  final String url;

  /// 控件在界面上占的宽度（dp），用来决定向图床要多大、解码成多大。
  final double cssWidth;

  final BoxFit fit;

  /// 加载失败时显示什么（默认与占位块一致）。
  final Widget? fallback;

  final Color? placeholderColor;

  /// 图床档位：只挑几个固定值，避免同一张图因几 px 差异被缓存成多份。
  static int pixelSize(double cssWidth, double dpr) {
    final int px = (cssWidth * dpr).round();
    if (px <= 160) return 160;
    if (px <= 240) return 240;
    if (px <= 320) return 320;
    if (px <= 480) return 480;
    return 720;
  }

  /// 按显示尺寸给出图床缩略图地址（也用于 [CozyImageCache.prefetch] 预热）。
  static String thumbUrl(String url, {required double cssWidth, double dpr = 3}) =>
      dishThumbUrl(url, px: pixelSize(cssWidth, dpr));

  @override
  Widget build(BuildContext context) {
    final double dpr = MediaQuery.maybeOf(context)?.devicePixelRatio ?? 2;
    final int px = pixelSize(cssWidth, dpr);
    final Widget placeholder = _PhotoPlaceholder(color: placeholderColor);
    if (!isPhotoUrl(url)) {
      return fallback ?? placeholder;
    }
    return Image(
      image: ResizeImage(
        CozyNetworkImage(dishThumbUrl(url, px: px)),
        width: px,
        height: px,
        policy: ResizeImagePolicy.fit,
      ),
      fit: fit,
      gaplessPlayback: true,
      frameBuilder: (BuildContext context, Widget child, int? frame, bool wasSynchronouslyLoaded) {
        if (wasSynchronouslyLoaded) return child;
        return Stack(
          fit: StackFit.expand,
          children: <Widget>[
            if (frame == null) placeholder,
            AnimatedOpacity(
              opacity: frame == null ? 0 : 1,
              duration: const Duration(milliseconds: 240),
              curve: Curves.easeOut,
              child: child,
            ),
          ],
        );
      },
      errorBuilder: (BuildContext context, Object error, StackTrace? stackTrace) =>
          fallback ?? placeholder,
    );
  }
}

class _PhotoPlaceholder extends StatelessWidget {
  const _PhotoPlaceholder({this.color});

  final Color? color;

  /// 加载中只铺一块同尺寸底色：不写字、不跳动，图到了原地淡入。
  /// 「暂无图片」之类的文案只在真失败时由 `errorBuilder` 显示。
  @override
  Widget build(BuildContext context) {
    return ColoredBox(
      color: color ?? CozyPalette.surfaceContainer,
      child: const SizedBox.expand(),
    );
  }
}
