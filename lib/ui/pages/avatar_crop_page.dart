import 'dart:math' as math;
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image/image.dart' as img;

import '../theme/cozy_glass.dart';

/// 头像裁剪页。
///
/// 只有一种需求：**方形取景框 + 圆形预览**（头像到处都用 `ClipOval` 呈现），
/// 所以不引 `image_cropper`（要挂原生 uCrop 依赖），自己用「平移 + 缩放 +
/// 算回原图像素矩形」实现：
///
/// 1. 先把原图解码、烘正 EXIF 方向、长边压到 1600 以内（够 256 头像用了），
///    再用这份「工作图」重新编码给 `Image.memory` 显示 —— 显示与裁剪用的是
///    **同一份像素**，不会出现「看到的和裁出来的错位」。
/// 2. 取景框是边长 `S` 的正方形；图片以 cover 方式铺满后再乘用户缩放 `scale`
///    （1.0~4.0），`offset` 是图片左上角在取景框坐标系里的位置。
/// 3. 点完成时，取景框四角映射回原图：`src = (-offset) / (coverScale * scale)`，
///    裁出来 → 缩到 256×256 → JPEG q72（≈10 KB）→ 返回字节。
///    调用方再 base64 成 data URI 存 `profiles.avatar_url`。
///
/// 返回：裁剪后的 JPEG 字节（`Uint8List`）；用户取消返回 `null`。
class AvatarCropPage extends StatefulWidget {
  const AvatarCropPage({super.key, required this.bytes, this.outputSize = 256});

  final Uint8List bytes;
  final int outputSize;

  /// 工作图长边上限（原图动辄 4000px，裁 256 用不到，还费内存）
  static const int _maxWorkingEdge = 1600;

  @override
  State<AvatarCropPage> createState() => _AvatarCropPageState();
}

class _AvatarCropPageState extends State<AvatarCropPage> {
  /// 工作图（已烘正方向、已限尺寸）
  img.Image? _src;

  /// 给 `Image.memory` 显示的工作图字节（与 `_src` 同源同尺寸）
  Uint8List? _display;

  /// 解码失败原因
  String? _error;

  /// 用户缩放倍数（1.0 = 刚好铺满取景框）
  double _scale = 1.0;
  static const double _minScale = 1.0;
  static const double _maxScale = 4.0;

  /// 图片左上角在取景框内的位置（逻辑像素）
  Offset _offset = Offset.zero;

  /// 取景框边长（由布局给出）
  double _viewport = 0;

  /// 需要重新居中（首次布局 / 刚解码完 / 换了取景框尺寸时置位）
  bool _needsCenter = true;

  /// 手势起点
  double _startScale = 1.0;
  Offset _startOffset = Offset.zero;
  Offset _startFocal = Offset.zero;

  bool _working = false;

  @override
  void initState() {
    super.initState();
    _decode();
  }

  Future<void> _decode() async {
    try {
      final img.Image? raw = img.decodeImage(widget.bytes);
      if (raw == null) {
        setState(() => _error = '这张图片解不开，换一张试试');
        return;
      }
      img.Image work = img.bakeOrientation(raw);
      final int longEdge = math.max(work.width, work.height);
      if (longEdge > AvatarCropPage._maxWorkingEdge) {
        final double k = AvatarCropPage._maxWorkingEdge / longEdge;
        work = img.copyResize(
          work,
          width: (work.width * k).round(),
          height: (work.height * k).round(),
        );
      }
      final Uint8List display = Uint8List.fromList(img.encodeJpg(work, quality: 92));
      if (!mounted) return;
      setState(() {
        _src = work;
        _display = display;
        _needsCenter = true;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = '这张图片解不开：$e');
    }
  }

  /// cover 铺满取景框所需的基础倍数（缩放 1.0 时）
  double get _coverScale {
    final img.Image? src = _src;
    if (src == null || _viewport <= 0) return 1;
    return math.max(_viewport / src.width, _viewport / src.height);
  }

  /// 当前图片在取景框里的显示尺寸
  Size get _drawSize {
    final img.Image? src = _src;
    if (src == null) return Size.zero;
    final double k = _coverScale * _scale;
    return Size(src.width * k, src.height * k);
  }

  /// 把图片框在取景框内（边缘不留白）
  void _clampOffset() {
    final Size d = _drawSize;
    if (d.isEmpty || _viewport <= 0) return;
    final double minX = math.min(0.0, _viewport - d.width);
    final double minY = math.min(0.0, _viewport - d.height);
    _offset = Offset(
      _offset.dx.clamp(minX, 0.0),
      _offset.dy.clamp(minY, 0.0),
    );
  }

  /// 首次布局 / 解码完成 / 取景框尺寸变化时把图片居中
  void _centerIfNeeded(double viewport) {
    if (!_needsCenter && _viewport == viewport) return;
    _viewport = viewport;
    _needsCenter = false;
    final Size d = _drawSize;
    _offset = Offset((viewport - d.width) / 2, (viewport - d.height) / 2);
    _clampOffset();
  }

  /// 以某个焦点缩放（焦点处的像素保持不动）
  void _zoomTo(double next, Offset focal) {
    final double clamped = next.clamp(_minScale, _maxScale);
    if (clamped == _scale) return;
    final double ratio = clamped / _scale;
    _offset = focal - (focal - _offset) * ratio;
    _scale = clamped;
    _clampOffset();
  }

  Future<void> _confirm() async {
    final img.Image? src = _src;
    if (src == null || _working) return;
    setState(() => _working = true);
    final double k = _coverScale * _scale;
    final double x = (-_offset.dx / k).clamp(0.0, (src.width - 1).toDouble());
    final double y = (-_offset.dy / k).clamp(0.0, (src.height - 1).toDouble());
    final int w = math.min((_viewport / k).round(), src.width - x.floor());
    final int h = math.min((_viewport / k).round(), src.height - y.floor());
    img.Image cropped = img.copyCrop(
      src,
      x: x.floor(),
      y: y.floor(),
      width: math.max(1, w),
      height: math.max(1, h),
    );
    cropped = img.copyResize(
      cropped,
      width: widget.outputSize,
      height: widget.outputSize,
      interpolation: img.Interpolation.cubic,
    );
    final Uint8List out = Uint8List.fromList(img.encodeJpg(cropped, quality: 72));
    if (!mounted) return;
    Navigator.of(context).pop(out);
  }

  @override
  Widget build(BuildContext context) {
    return CozyPage(
      child: Column(
        children: <Widget>[
          _topBar(),
          Expanded(
            child: _error != null
                ? Center(
                    child: Padding(
                      padding: const EdgeInsets.all(32),
                      child: Text(
                        _error!,
                        textAlign: TextAlign.center,
                        style: _ts(14, 20, FontWeight.w600, CozyPalette.onSurfaceVariant),
                      ),
                    ),
                  )
                : _body(),
          ),
          _bottomBar(),
        ],
      ),
    );
  }

  Widget _topBar() {
    return SizedBox(
      height: 64,
      child: Row(
        children: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(null),
            child: Text('取消', style: _ts(15, 20, FontWeight.w600, CozyPalette.onSurfaceVariant)),
          ),
          Expanded(
            child: Text(
              '裁剪头像',
              textAlign: TextAlign.center,
              style: _ts(18, 24, FontWeight.w900, CozyPalette.primary),
            ),
          ),
          TextButton(
            onPressed: _src == null || _working ? null : _confirm,
            child: Text(
              '完成',
              style: _ts(
                15,
                20,
                FontWeight.w900,
                _src == null ? CozyPalette.outlineVariant : CozyPalette.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _body() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: LayoutBuilder(
          builder: (BuildContext context, BoxConstraints c) {
            // 取景框取「可用宽 / 可用高」的较小值，宽屏窄屏都居中不溢出
            final double side = math.min(c.maxWidth, c.maxHeight - 24);
            _centerIfNeeded(side);
            return Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                GestureDetector(
                  onScaleStart: (ScaleStartDetails d) {
                    _startScale = _scale;
                    _startOffset = _offset;
                    _startFocal = d.localFocalPoint;
                  },
                  onScaleUpdate: (ScaleUpdateDetails d) {
                    setState(() {
                      // 手势起点下的那个图像点跟着手指走：
                      //   O = f - (f0 - O0) * r      （f0 为手势起点焦点，r = 缩放比）
                      // 单指拖动时 r == 1 ⇒ O = O0 + (f - f0)，即纯平移跟随手指。
                      // 【真机修正】原来 f 与 f0 都取当前焦点，r == 1 时恒等于 O0，
                      // 图片根本拖不动（证据 S10-crop-zoom.png 与 S9-crop.png 逐字节几乎相同）。
                      _scale = (_startScale * d.scale).clamp(_minScale, _maxScale);
                      final double ratio = _scale / _startScale;
                      _offset = d.localFocalPoint - (_startFocal - _startOffset) * ratio;
                      _clampOffset();
                    });
                  },
                  child: _viewportWidget(side),
                ),
                const SizedBox(height: 18),
                Row(
                  children: <Widget>[
                    const Icon(Icons.photo_size_select_small, size: 18, color: CozyPalette.onSurfaceVariant),
                    Expanded(
                      child: Slider(
                        value: _scale,
                        min: _minScale,
                        max: _maxScale,
                        activeColor: CozyPalette.primary,
                        inactiveColor: CozyPalette.outlineVariant,
                        onChanged: (double v) => setState(
                          () => _zoomTo(v, Offset(side / 2, side / 2)),
                        ),
                      ),
                    ),
                    const Icon(Icons.photo_size_select_large, size: 22, color: CozyPalette.onSurfaceVariant),
                  ],
                ),
                Text(
                  '拖动调整位置，双指或上面的滑杆缩放',
                  style: _ts(12, 18, FontWeight.w400, CozyPalette.onSurfaceVariant),
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _viewportWidget(double side) {
    final Uint8List? display = _display;
    final Size d = _drawSize;
    return SizedBox(
      width: side,
      height: side,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: Stack(
          children: <Widget>[
            Positioned(
              left: _offset.dx,
              top: _offset.dy,
              width: d.width,
              height: d.height,
              child: display == null
                  ? const ColoredBox(color: CozyPalette.surfaceContainer)
                  : Image.memory(display, fit: BoxFit.fill, gaplessPlayback: true),
            ),
            Positioned.fill(
              child: IgnorePointer(
                child: CustomPaint(painter: _CropMaskPainter()),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _bottomBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 10, 24, 22),
      child: Column(
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              TextButton.icon(
                onPressed: () => setState(() {
                  _scale = 1.0;
                  _centerIfNeeded(_viewport);
                }),
                icon: const Icon(Icons.center_focus_strong, size: 18),
                label: const Text('重置'),
                style: TextButton.styleFrom(foregroundColor: CozyPalette.onSurfaceVariant),
              ),
            ],
          ),
          const SizedBox(height: 4),
          SizedBox(
            width: double.infinity,
            child: CozyPrimaryButton(
              text: _working ? '处理中…' : '完成',
              onTap: _confirm,
              enabled: _src != null && !_working,
            ),
          ),
        ],
      ),
    );
  }
}

/// 取景遮罩：圆外压暗 + 一圈玫瑰细边（头像到处是 `ClipOval`，所以预览给圆形）
class _CropMaskPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final Offset center = Offset(size.width / 2, size.height / 2);
    final double radius = math.min(size.width, size.height) / 2 - 2;
    final Path outside = Path.combine(
      PathOperation.difference,
      Path()..addRect(Offset.zero & size),
      Path()..addOval(Rect.fromCircle(center: center, radius: radius)),
    );
    canvas.drawPath(
      outside,
      Paint()..color = CozyPalette.onSurface.withValues(alpha: 0.42),
    );
    canvas.drawCircle(
      center,
      radius,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2
        ..color = Colors.white.withValues(alpha: 0.92),
    );
    canvas.drawCircle(
      center,
      radius - 3,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1
        ..color = CozyPalette.primary.withValues(alpha: 0.34),
    );
  }

  @override
  bool shouldRepaint(covariant _CropMaskPainter oldDelegate) => false;
}

TextStyle _ts(double size, double height, FontWeight weight, Color color) =>
    TextStyle(fontSize: size, height: height / size, fontWeight: weight, color: color);
