import 'package:confetti/confetti.dart';
import 'package:flutter/material.dart';

import '../theme/cozy_glass.dart';

/// 撒花庆祝：在最上层 overlay 播一次品牌色纸屑，播完自动移除。
///
/// 用 overlay 而不是往页面里塞 Stack，是为了让调用方保持一行：
/// `showCozyConfetti(context);` —— 页面结构不用为了动效改层级。
Future<void> showCozyConfetti(BuildContext context) async {
  final OverlayState? overlay = Overlay.maybeOf(context, rootOverlay: true);
  if (overlay == null) return;

  final OverlayEntry entry = OverlayEntry(
    builder: (BuildContext _) => const _CozyConfettiLayer(),
  );
  overlay.insert(entry);
  await Future<void>.delayed(const Duration(milliseconds: 2200));
  if (entry.mounted) entry.remove();
}

class _CozyConfettiLayer extends StatefulWidget {
  const _CozyConfettiLayer();

  @override
  State<_CozyConfettiLayer> createState() => _CozyConfettiLayerState();
}

class _CozyConfettiLayerState extends State<_CozyConfettiLayer> {
  // 2s 正好覆盖撒花从爆开到落下的全过程，与 showCozyConfetti 的移除时机一致
  final ConfettiController _ctrl =
      ConfettiController(duration: const Duration(seconds: 2));

  @override
  void initState() {
    super.initState();
    _ctrl.play();
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: ConfettiWidget(
        confettiController: _ctrl,
        blastDirectionality: BlastDirectionality.explosive,
        emissionFrequency: 0.04,
        numberOfParticles: 16,
        maxBlastForce: 24,
        minBlastForce: 9,
        gravity: 0.22,
        minimumSize: const Size(9, 6),
        maximumSize: const Size(16, 10),
        // 只用品牌色板里的颜色 + 一点暖金，避免撒出彩虹色
        colors: const <Color>[
          CozyPalette.primary,
          CozyPalette.primaryContainer,
          CozyPalette.secondaryContainer,
          CozyPalette.tertiaryContainer,
          Color(0xFFF2C14E),
        ],
        child: const SizedBox.expand(),
      ),
    );
  }
}
