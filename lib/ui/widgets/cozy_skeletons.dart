import 'package:flutter/material.dart';
import 'package:skeletonizer/skeletonizer.dart';

import '../theme/cozy_glass.dart';

/// 骨架屏外壳。
///
/// 用 `Skeletonizer.zone` 而不是普通 `Skeletonizer`：zone 只把 [Bone] 占位块
/// 涂成 shimmer，外壳卡片自己画的圆角/描边/底色保持真实 —— 否则整张卡会
/// 被涂成一块同色板砖，层次全丢。
///
/// 颜色全部走 CozyPalette（暖米色系），不用库默认的冷灰 #EBEBF4。
class CozySkeleton extends StatelessWidget {
  const CozySkeleton({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Skeletonizer.zone(
      enabled: true,
      effect: const ShimmerEffect(
        baseColor: CozyPalette.surfaceContainer,
        highlightColor: CozyPalette.surfaceContainerLow,
        duration: Duration(milliseconds: 1400),
      ),
      child: child,
    );
  }
}

/// 骨架屏里的一张「卡片外壳」—— 与真实卡片同规格（radius 20 + 发丝描边）。
class _SkeletonCard extends StatelessWidget {
  const _SkeletonCard({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: CozyPalette.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: CozyPalette.outlineVariant.withValues(alpha: 0.35),
        ),
      ),
      child: child,
    );
  }
}

Widget _bone({double? width, double? height, double radius = 8}) => Bone(
  width: width,
  height: height,
  uniRadius: radius,
);

/// 首帧加载：订单页列表（形状对齐 `_StitchOrderCard`）。
class CozyOrderSkeletonList extends StatelessWidget {
  const CozyOrderSkeletonList({super.key, this.count = 2});

  final int count;

  @override
  Widget build(BuildContext context) {
    return CozySkeleton(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
        child: Column(
          children: <Widget>[
            for (int i = 0; i < count; i++) ...<Widget>[
              if (i > 0) const SizedBox(height: 12),
              _SkeletonCard(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    _bone(width: 56, height: 56, radius: 14),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          _bone(width: 132, height: 15),
                          const SizedBox(height: 9),
                          _bone(width: 168, height: 12),
                          const SizedBox(height: 12),
                          _bone(width: 96, height: 13),
                        ],
                      ),
                    ),
                    const SizedBox(width: 10),
                    _bone(width: 46, height: 22, radius: 11),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// 首帧加载：点菜页菜品列表（形状对齐 `_SingleShopDishCard`）。
class CozyDishSkeletonList extends StatelessWidget {
  const CozyDishSkeletonList({
    super.key,
    this.count = 3,
    this.bottomClearance = 0,
  });

  final int count;
  final double bottomClearance;

  @override
  Widget build(BuildContext context) {
    return CozySkeleton(
      child: ListView.separated(
        padding: EdgeInsets.only(
          left: 0,
          top: 8,
          right: 4,
          bottom: bottomClearance + 14,
        ),
        physics: const NeverScrollableScrollPhysics(),
        itemCount: count,
        separatorBuilder: (_, _) => const SizedBox(height: 10),
        itemBuilder: (BuildContext context, int index) {
          return _SkeletonCard(
            child: Row(
              children: <Widget>[
                _bone(width: 84, height: 84, radius: 18),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      _bone(width: 118, height: 15),
                      const SizedBox(height: 10),
                      _bone(width: 152, height: 12),
                      const SizedBox(height: 14),
                      _bone(width: 64, height: 16),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                _bone(width: 34, height: 34, radius: 17),
              ],
            ),
          );
        },
      ),
    );
  }
}

/// 搜索中：发现页结果列表。
class CozySearchSkeletonList extends StatelessWidget {
  const CozySearchSkeletonList({super.key, this.count = 3});

  final int count;

  @override
  Widget build(BuildContext context) {
    return CozySkeleton(
      child: Column(
        children: <Widget>[
          for (int i = 0; i < count; i++) ...<Widget>[
            if (i > 0) const SizedBox(height: 10),
            _SkeletonCard(
              child: Row(
                children: <Widget>[
                  _bone(width: 68, height: 68, radius: 16),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        _bone(width: 124, height: 14),
                        const SizedBox(height: 9),
                        _bone(width: 168, height: 11),
                        const SizedBox(height: 11),
                        _bone(width: 78, height: 13),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
