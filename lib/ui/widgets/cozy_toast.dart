import 'dart:async';

import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../theme/cozy_glass.dart';

/// 品牌提示条：液态玻璃 Toast（替代 Material SnackBar）。
///
/// 全 App 统一走这里，好处是：
/// - 玻璃底 + 圆角胶囊，和底部玻璃 dock 同一套材质语言；
/// - `type` 直接映射成功/失败配色，不用每个调用点自己挑背景色；
/// - 从底部弹出、自动消失，不占布局、不推页面。
///
/// 【为什么不用 `GlassToast.show`】(2026-09-27)
/// 库自带的定位是 `bottom: 安全区 + 16`，正好压在底部 dock 上：提示条和
/// 五个 tab 图标叠在一起，两边都看不清（用户截图反馈「这个弹出是不是有遮挡」）。
/// 库的定位写死在私有 `_GlassToastOverlay` 里、没有偏移参数，所以这里改成
/// 自己插 OverlayEntry —— 视觉仍然用库的 `GlassToast` widget，只把落点抬到
/// dock 之上（`CozyDock.clearanceOf`），并且左右各留 20 内边距（库的
/// `maxWidth: 400` 在 360dp 宽的机子上会顶到屏幕两边被裁掉）。
void showCozyToast(
  BuildContext context,
  String message, {
  bool error = false,
  Duration duration = const Duration(seconds: 2),
  String? actionLabel,
  VoidCallback? onAction,
}) {
  final String text = message.trim();
  if (text.isEmpty) return;

  final OverlayState? overlay = Overlay.maybeOf(context, rootOverlay: true);
  if (overlay == null) return;

  // 新的提示直接顶掉旧的：两条同时出现在同一个位置会糊成一团。
  _dismissActiveToasts();

  final GlassToastAction? action = (actionLabel == null || onAction == null)
      ? null
      : GlassToastAction(label: actionLabel, onPressed: onAction);

  late final OverlayEntry entry;
  bool removed = false;
  void removeEntry() {
    if (removed) return;
    removed = true;
    _activeToasts.remove(removeEntry);
    if (entry.mounted) entry.remove();
  }

  entry = OverlayEntry(
    builder: (BuildContext overlayContext) => _CozyToastLayer(
      message: text,
      error: error,
      action: action,
      duration: duration,
      onDismissed: removeEntry,
    ),
  );
  _activeToasts.add(removeEntry);
  overlay.insert(entry);
}

final List<VoidCallback> _activeToasts = <VoidCallback>[];

void _dismissActiveToasts() {
  for (final VoidCallback dismiss in List<VoidCallback>.of(_activeToasts)) {
    dismiss();
  }
  _activeToasts.clear();
}

/// 自己定位的提示层：抬到 dock 之上，进出各 200ms 淡入 + 上滑。
class _CozyToastLayer extends StatefulWidget {
  const _CozyToastLayer({
    required this.message,
    required this.error,
    required this.duration,
    required this.onDismissed,
    this.action,
  });

  final String message;
  final bool error;
  final Duration duration;
  final VoidCallback onDismissed;
  final GlassToastAction? action;

  @override
  State<_CozyToastLayer> createState() => _CozyToastLayerState();
}

class _CozyToastLayerState extends State<_CozyToastLayer>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 200),
  );
  late final Animation<double> _fade = CurvedAnimation(
    parent: _controller,
    curve: Curves.easeOut,
  );
  Timer? _timer;
  bool _dismissing = false;

  @override
  void initState() {
    super.initState();
    _controller.forward();
    _timer = Timer(widget.duration, _dismiss);
  }

  @override
  void dispose() {
    _timer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  Future<void> _dismiss() async {
    if (_dismissing || !mounted) return;
    _dismissing = true;
    _timer?.cancel();
    await _controller.reverse();
    if (mounted) widget.onDismissed();
  }

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: 0,
      right: 0,
      // dock 之上：clearance 已经包含底部安全区，再留 6 的空隙。
      bottom: CozyDock.clearanceOf(context) + 6,
      child: FadeTransition(
        opacity: _fade,
        child: SlideTransition(
          position: Tween<Offset>(
            begin: const Offset(0, 0.35),
            end: Offset.zero,
          ).animate(_fade),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Center(
              child: GlassToast(
                message: widget.message,
                type: widget.error
                    ? GlassToastType.error
                    : GlassToastType.success,
                quality: GlassQuality.premium,
                dismissible: false,
                action: widget.action == null
                    ? null
                    : GlassToastAction(
                        label: widget.action!.label,
                        onPressed: () {
                          widget.action!.onPressed();
                          unawaited(_dismiss());
                        },
                      ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
