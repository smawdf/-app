import 'dart:ui';
import 'package:flutter/material.dart';

/// 高糖小食 · 纯白极简底色 + 晶莹液态水滴设计系统规范
class CozyTheme {
  // 核心底色：纯白无瑕
  static const Color pureWhite = Color(0xFFFFFFFF);
  static const Color cardSurface = Color(0xFFFFFFFF);
  static const Color cardStroke = Color(0x0F000000); // 极轻微轮廓线

  // 高糖暖粉点缀色
  static const Color primaryPink = Color(0xFFFF5A79);
  static const Color softPink = Color(0xFFFFF0F3);
  static const Color sweetCocoa = Color(0xFF1D1B20);
  static const Color mutedText = Color(0xFF79747E);
}

/// 纯白背景上的晶莹液态水滴容器 (Ultra-Clear Liquid Glass Container)
class LiquidDropGlass extends StatelessWidget {
  final Widget child;
  final double? width;
  final double height;
  final BorderRadius borderRadius;
  final EdgeInsetsGeometry padding;
  final VoidCallback? onTap;

  const LiquidDropGlass({
    super.key,
    required this.child,
    this.width,
    this.height = 70.0,
    this.borderRadius = const BorderRadius.all(Radius.circular(35.0)),
    this.padding = const EdgeInsets.symmetric(horizontal: 14.0),
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    Widget content = Container(
      width: width,
      height: height,
      padding: padding,
      decoration: BoxDecoration(
        borderRadius: borderRadius,
        // 1. 超轻水光基底：在纯白底色上呈现高透微光 (42% 白色渐变)
        gradient: const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Color(0x99FFFFFF), // 顶部高透白 60%
            Color(0x40FFFFFF), // 中部透光 25%
            Color(0x73FFFFFF), // 底部回弹白 45%
          ],
        ),
        // 2. 物理高光刀刃：顶部 100% 极亮纯白，底部柔和轮廓
        border: Border.all(
          color: Colors.white.withValues(alpha: 0.95),
          width: 1.5,
        ),
        boxShadow: [
          // 3. 纯白底脱模核心：极柔和的环境漫反射阴影，将玻璃从纸面优雅托起
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.08),
            blurRadius: 32,
            offset: const Offset(0, 14),
          ),
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          // 4. 凸透镜顶部水滴月牙聚光弧 (Sheen Arch)
          Positioned(
            top: 1,
            left: 20,
            right: 20,
            height: height * 0.44,
            child: IgnorePointer(
              child: Container(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.all(Radius.circular(height * 0.45)),
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.white.withValues(alpha: 0.85),
                      Colors.white.withValues(alpha: 0.0),
                    ],
                  ),
                ),
              ),
            ),
          ),
          // 真正内容承载
          Center(child: child),
        ],
      ),
    );

    // 双层硬件高斯模糊滤镜 (BackdropFilter)
    return ClipRRect(
      borderRadius: borderRadius,
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 24, sigmaY: 24),
        child: onTap != null
            // 关键：opaque 让整块玻璃都可点击。
            // 内部 Container 使用 decoration(渐变) 渲染为 DecoratedBox，
            // 默认 deferToChild 时空白区域不会响应点击。
            ? GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: onTap,
                child: content,
              )
            : content,
      ),
    );
  }
}
