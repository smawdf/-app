import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

/// 品牌提示条：液态玻璃 Toast（替代 Material SnackBar）。
///
/// 全 App 统一走这里，好处是：
/// - 玻璃底 + 圆角胶囊，和底部玻璃 dock 同一套材质语言；
/// - `type` 直接映射成功/失败配色，不用每个调用点自己挑背景色；
/// - 从底部弹出、自动消失，不占布局、不推页面。
void showCozyToast(
  BuildContext context,
  String message, {
  bool error = false,
  Duration duration = const Duration(seconds: 2),
  String? actionLabel,
  VoidCallback? onAction,
}) {
  if (message.trim().isEmpty) return;
  GlassToast.show(
    context,
    message: message,
    type: error ? GlassToastType.error : GlassToastType.success,
    position: GlassToastPosition.bottom,
    duration: duration,
    quality: GlassQuality.premium,
    action: (actionLabel == null || onAction == null)
        ? null
        : GlassToastAction(label: actionLabel, onPressed: onAction),
  );
}
