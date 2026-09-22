import 'package:fluid_glass/fluid_glass.dart';
import 'package:flutter/material.dart';

/// 全局背景采样共享控制器，供悬浮液态玻璃底栏与悬浮玻璃条进行实时透镜折射
class BackdropScope extends InheritedWidget {
  final LayerBackdrop backdrop;

  const BackdropScope({
    super.key,
    required this.backdrop,
    required super.child,
  });

  static LayerBackdrop of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<BackdropScope>();
    return scope?.backdrop ?? LayerBackdrop();
  }

  @override
  bool updateShouldNotify(BackdropScope oldWidget) => backdrop != oldWidget.backdrop;
}
