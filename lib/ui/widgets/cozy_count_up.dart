import 'package:flutter/material.dart';

/// 数字滚动：把一个整数从 0 滚到目标值（值变化时从旧值滚到新值）。
///
/// 语义树上只暴露**最终值**（[Semantics.label] + [ExcludeSemantics]），
/// 这样 uiautomator / 无障碍读到的永远是稳定结果，不会抓到滚动中的中间数。
class CozyCountUp extends StatelessWidget {
  const CozyCountUp({
    super.key,
    required this.value,
    this.prefix = '',
    this.suffix = '',
    this.style,
    this.textAlign,
    this.duration = const Duration(milliseconds: 760),
    this.format,
  });

  /// 目标整数。
  final int value;

  /// 数字前固定不变的文字（例如「一起吃饭 」）。
  final String prefix;

  /// 数字后固定不变的文字（例如「 天」「 枚」）。
  final String suffix;

  final TextStyle? style;
  final TextAlign? textAlign;
  final Duration duration;

  /// 自定义数字格式，默认直接 `toString()`。
  final String Function(int value)? format;

  String _render(int n) => '$prefix${format?.call(n) ?? n}$suffix';

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: _render(value),
      child: ExcludeSemantics(
        child: TweenAnimationBuilder<double>(
          tween: Tween<double>(begin: 0, end: value.toDouble()),
          duration: duration,
          curve: Curves.easeOutCubic,
          builder: (BuildContext context, double v, Widget? _) {
            return Text(_render(v.round()), style: style, textAlign: textAlign);
          },
        ),
      ),
    );
  }
}
