import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../theme/cozy_glass.dart';

/// 1:1 Dart port of `app/src/main/java/com/myorderapp/ui/auth/AuthVisuals.kt`
/// (300 lines). Nothing here is invented: every colour, size, radius, offset
/// and string is copied from the Kotlin source.
///
/// Native symbol map
///   AuthInk          -> AuthColors.ink
///   AuthMuted        -> AuthColors.muted
///   AuthPrimaryStart -> AuthColors.primaryStart
///   AuthPrimaryEnd   -> AuthColors.primaryEnd
///   AuthField        -> AuthColors.field
///   AuthFieldStroke  -> AuthColors.fieldStroke
///   AuthCream        -> AuthColors.cream      (private in Kotlin)
///   AuthPinkMist     -> AuthColors.pinkMist   (private in Kotlin)
///   AuthHeart        -> AuthColors.heart      (private in Kotlin)
///   AuthSurfaceVariant -> AuthColors.surfaceVariant
class AuthColors {
  const AuthColors._();

  static const Color ink = CozyPalette.onBackground; // AuthVisuals.kt: AuthInk
  static const Color muted = CozyPalette.onSurfaceVariant; // AuthMuted
  static const Color primaryStart = Color(0xFFFFA7BC); // AuthPrimaryStart
  static const Color primaryEnd = CozyPalette.primary; // AuthPrimaryEnd = Primary
  static const Color field = CozyPalette.surface; // AuthField = Surface
  static const Color fieldStroke = CozyPalette.outlineVariant; // AuthFieldStroke
  static const Color cream = Color(0xFFFFE8D3); // AuthCream (private)
  static const Color pinkMist = CozyPalette.primaryContainer; // AuthPinkMist
  static const Color heart = Color(0xFFEFA7B5); // AuthHeart
  static const Color surfaceVariant = CozyPalette.surfaceVariant;
}

/// `AuthVisuals.kt` -> `AuthDecoratedBackground`.
///
/// `Box.fillMaxSize().background(Color(0xFFFFFFFF))` + a full-bleed `Canvas`
/// painting four soft circles, three dots, two dashed bezier ribbons and
/// three hearts, with the caller's content drawn on top.
class AuthDecoratedBackground extends StatelessWidget {
  const AuthDecoratedBackground({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        const Positioned.fill(child: ColoredBox(color: Color(0xFFFFFFFF))),
        Positioned.fill(
          child: CustomPaint(painter: _AuthDecorationsPainter()),
        ),
        Positioned.fill(child: child),
      ],
    );
  }
}

class _AuthDecorationsPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final double w = size.width;
    final double h = size.height;

    void circle(Color color, double fx, double fy, double rf) {
      canvas.drawCircle(
        Offset(w * fx, h * fy),
        w * rf,
        Paint()..color = color,
      );
    }

    void dot(Color color, double fx, double fy, double r) {
      canvas.drawCircle(Offset(w * fx, h * fy), r, Paint()..color = color);
    }

    // --- four ambient circles -------------------------------------------------
    circle(AuthColors.cream.withValues(alpha: 0.16), 0.05, 0.12, 0.22);
    circle(AuthColors.pinkMist.withValues(alpha: 0.10), 0.86, 0.24, 0.17);
    circle(AuthColors.pinkMist.withValues(alpha: 0.08), 0.86, 0.80, 0.24);
    circle(AuthColors.cream.withValues(alpha: 0.14), 0.13, 0.68, 0.16);

    // --- three sprinkle dots --------------------------------------------------
    dot(AuthColors.primaryStart.withValues(alpha: 0.09), 0.73, 0.22, 4);
    dot(AuthColors.primaryEnd.withValues(alpha: 0.07), 0.78, 0.20, 2.5);
    dot(AuthColors.heart.withValues(alpha: 0.08), 0.88, 0.15, 3);

    // --- two dashed bezier ribbons -------------------------------------------
    final Path ribbonTop = Path()
      ..moveTo(w * 0.08, h * 0.30)
      ..cubicTo(w * 0.25, h * 0.24, w * 0.42, h * 0.25, w * 0.55, h * 0.31)
      ..cubicTo(w * 0.68, h * 0.37, w * 0.80, h * 0.37, w * 0.94, h * 0.30);
    _drawDashed(
      canvas,
      ribbonTop,
      const <double>[9, 11],
      Paint()
        ..color = AuthColors.primaryStart.withValues(alpha: 0.08)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5,
    );

    final Path ribbonBottom = Path()
      ..moveTo(w * 0.10, h * 0.72)
      ..cubicTo(w * 0.25, h * 0.67, w * 0.45, h * 0.68, w * 0.58, h * 0.74)
      ..cubicTo(w * 0.72, h * 0.80, w * 0.85, h * 0.80, w * 0.95, h * 0.73);
    _drawDashed(
      canvas,
      ribbonBottom,
      const <double>[8, 10],
      Paint()
        ..color = AuthColors.primaryStart.withValues(alpha: 0.07)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );

    // --- three hearts ---------------------------------------------------------
    _drawHeart(
      canvas,
      Offset(w * 0.86, h * 0.18),
      11,
      AuthColors.heart.withValues(alpha: 0.10),
    );
    _drawHeart(
      canvas,
      Offset(w * 0.14, h * 0.56),
      9,
      AuthColors.heart.withValues(alpha: 0.08),
    );
    _drawHeart(
      canvas,
      Offset(w * 0.88, h * 0.52),
      8,
      AuthColors.heart.withValues(alpha: 0.08),
      outline: true,
    );
  }

  /// `drawHeart(topLeft, sizePx, color, outline = false)` from AuthVisuals.kt.
  void _drawHeart(
    Canvas canvas,
    Offset topLeft,
    double sizePx,
    Color color, {
    bool outline = false,
  }) {
    double x(double v) => topLeft.dx + sizePx * v;
    double y(double v) => topLeft.dy + sizePx * v;

    final Path path = Path()
      ..moveTo(x(0.50), y(0.90))
      ..cubicTo(x(-0.05), y(0.48), x(0.02), y(0.08), x(0.30), y(0.12))
      ..cubicTo(x(0.42), y(0.14), x(0.49), y(0.23), x(0.50), y(0.31))
      ..cubicTo(x(0.51), y(0.23), x(0.58), y(0.14), x(0.70), y(0.12))
      ..cubicTo(x(0.98), y(0.08), x(1.05), y(0.48), x(0.50), y(0.90))
      ..close();

    if (outline) {
      canvas.drawPath(
        path,
        Paint()
          ..color = color
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.4,
      );
    } else {
      canvas.drawPath(path, Paint()..color = color);
    }
  }

  void _drawDashed(
    Canvas canvas,
    Path path,
    List<double> pattern,
    Paint paint,
  ) {
    for (final metric in path.computeMetrics()) {
      double distance = 0;
      int index = 0;
      while (distance < metric.length) {
        final double segment = pattern[index % pattern.length];
        final double next = math.min(distance + segment, metric.length);
        if (index.isEven) {
          canvas.drawPath(metric.extractPath(distance, next), paint);
        }
        distance = next;
        index++;
      }
    }
  }

  @override
  bool shouldRepaint(covariant _AuthDecorationsPainter oldDelegate) => false;
}

/// `AuthVisuals.kt` -> `AuthLogo`.
///
/// 132dp square, `Color(0xFFFFFAF6)` fill, radius 36, 1dp
/// `AuthFieldStroke@0.72` border, 6dp inner padding, artwork clipped to 30.
class AuthLogo extends StatelessWidget {
  const AuthLogo({super.key, this.size = 132});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: const Color(0xFFFFFAF6),
        borderRadius: BorderRadius.circular(36),
        border: Border.all(
          color: AuthColors.fieldStroke.withValues(alpha: 0.72),
        ),
      ),
      padding: const EdgeInsets.all(6),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(30),
        child: Image.asset(
          'assets/images/auth_dogs_artwork.png',
          fit: BoxFit.cover,
          width: double.infinity,
          height: double.infinity,
        ),
      ),
    );
  }
}

/// `AuthVisuals.kt` -> `AuthGlassCard`.
///
/// Radius 28, `Surface@0.98`, 1dp `AuthFieldStroke@0.72`, elevation 0,
/// inner padding 24.
class AuthGlassCard extends StatelessWidget {
  const AuthGlassCard({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AuthColors.field.withValues(alpha: 0.98),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(
          color: AuthColors.fieldStroke.withValues(alpha: 0.72),
        ),
      ),
      padding: const EdgeInsets.all(24),
      child: child,
    );
  }
}

/// `AuthVisuals.kt` -> `AuthInputField`.
///
/// Height 60dp (82dp when [supportingText] is present), radius 22, leading
/// icon `Lock` when [isPassword] else `Person`, a visibility toggle on
/// password fields, focused fill white / unfocused `Color(0xFFFFF4F0)@0.72`,
/// focused border `AuthPinkMist`, unfocused border transparent,
/// cursor `AuthPrimaryEnd`.
class AuthInputField extends StatefulWidget {
  const AuthInputField({
    super.key,
    required this.controller,
    required this.label,
    this.placeholder,
    this.isPassword = false,
    this.leadingIcon,
    this.supportingText,
    this.floatingLabel = true,
    this.keyboardType,
    this.textInputAction,
    this.onSubmitted,
    this.enabled = true,
  });

  final TextEditingController controller;
  final String label;
  final String? placeholder;
  final bool isPassword;
  final IconData? leadingIcon;
  final String? supportingText;
  final bool floatingLabel;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onSubmitted;
  final bool enabled;

  @override
  State<AuthInputField> createState() => _AuthInputFieldState();
}

class _AuthInputFieldState extends State<AuthInputField> {
  final FocusNode _focus = FocusNode();
  bool _obscure = true;

  @override
  void initState() {
    super.initState();
    _focus.addListener(_onFocusChanged);
  }

  @override
  void dispose() {
    _focus.removeListener(_onFocusChanged);
    _focus.dispose();
    super.dispose();
  }

  void _onFocusChanged() => setState(() {});

  @override
  Widget build(BuildContext context) {
    final bool hasSupport = widget.supportingText != null;
    final bool focused = _focus.hasFocus;

    final Widget field = SizedBox(
      height: 60,
      child: TextField(
        controller: widget.controller,
        focusNode: _focus,
        enabled: widget.enabled,
        obscureText: widget.isPassword && _obscure,
        keyboardType: widget.keyboardType,
        textInputAction: widget.textInputAction,
        onSubmitted: widget.onSubmitted,
        cursorColor: AuthColors.primaryEnd,
        style: const TextStyle(fontSize: 15, color: AuthColors.ink),
        decoration: InputDecoration(
          isDense: true,
          labelText: widget.floatingLabel ? widget.label : null,
          labelStyle: const TextStyle(
            color: AuthColors.ink,
            fontWeight: FontWeight.w700,
          ),
          floatingLabelStyle: const TextStyle(
            color: AuthColors.ink,
            fontWeight: FontWeight.w700,
          ),
          hintText: widget.placeholder,
          hintStyle: TextStyle(
            color: AuthColors.muted.withValues(alpha: 0.72),
          ),
          filled: true,
          fillColor: focused
              ? Colors.white
              : const Color(0xFFFFF4F0).withValues(alpha: 0.72),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 18,
          ),
          prefixIcon: Icon(
            widget.leadingIcon ??
                (widget.isPassword
                    ? Icons.lock_outline
                    : Icons.person_outline),
            color: AuthColors.primaryEnd,
          ),
          suffixIcon: widget.isPassword
              ? IconButton(
                  onPressed: () => setState(() => _obscure = !_obscure),
                  icon: Icon(
                    _obscure
                        ? Icons.visibility_outlined
                        : Icons.visibility_off_outlined,
                    color: AuthColors.primaryEnd,
                  ),
                  tooltip: _obscure ? '显示密码' : '隐藏密码',
                )
              : null,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(22),
            borderSide: BorderSide.none,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(22),
            borderSide: const BorderSide(color: Colors.transparent),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(22),
            borderSide: const BorderSide(color: AuthColors.pinkMist),
          ),
        ),
      ),
    );

    if (!hasSupport) return field;

    return SizedBox(
      height: 82,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(child: field),
          Text(
            widget.supportingText!,
            textAlign: TextAlign.right,
            style: CozyType.textTheme.bodySmall!.copyWith(
              color: AuthColors.primaryEnd.withValues(alpha: 0.82),
            ),
          ),
        ],
      ),
    );
  }
}

/// `AuthVisuals.kt` -> `AuthPrimaryButton`.
///
/// Height 60, radius 999, `Color(0xFFFF9FB7)` fill, disabled
/// `AuthFieldStroke@0.70`, white `titleMedium` at weight Black, and a 0.98
/// press scale.
class AuthPrimaryButton extends StatefulWidget {
  const AuthPrimaryButton({
    super.key,
    required this.text,
    this.onTap,
    this.enabled = true,
  });

  final String text;
  final VoidCallback? onTap;
  final bool enabled;

  @override
  State<AuthPrimaryButton> createState() => _AuthPrimaryButtonState();
}

class _AuthPrimaryButtonState extends State<AuthPrimaryButton> {
  bool _pressed = false;

  void _setPressed(bool value) {
    if (!mounted || _pressed == value) return;
    setState(() => _pressed = value);
  }

  @override
  Widget build(BuildContext context) {
    final bool active = widget.enabled && widget.onTap != null;

    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: active ? (_) => _setPressed(true) : null,
      onTapUp: active ? (_) => _setPressed(false) : null,
      onTapCancel: active ? () => _setPressed(false) : null,
      onTap: active ? widget.onTap : null,
      child: AnimatedScale(
        scale: _pressed ? 0.98 : 1.0,
        duration: const Duration(milliseconds: 120),
        child: Container(
          height: 60,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: active
                ? const Color(0xFFFF9FB7)
                : AuthColors.fieldStroke.withValues(alpha: 0.70),
            borderRadius: BorderRadius.circular(999),
          ),
          child: Text(
            widget.text,
            style: CozyType.textTheme.titleMedium!.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.w900,
            ),
          ),
        ),
      ),
    );
  }
}

/// `AuthVisuals.kt` -> `AuthBottomLink`.
class AuthBottomLink extends StatelessWidget {
  const AuthBottomLink({
    super.key,
    this.prefix = '',
    required this.actionText,
    required this.onTap,
    this.top = 0,
  });

  final String prefix;
  final String actionText;
  final VoidCallback onTap;
  final double top;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(top: top),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (prefix.isNotEmpty)
            Text(
              prefix,
              style: CozyType.textTheme.bodySmall!.copyWith(
                color: AuthColors.muted,
              ),
            ),
          if (prefix.isNotEmpty) const SizedBox(width: 4),
          TextButton(
            onPressed: onTap,
            style: TextButton.styleFrom(
              minimumSize: Size.zero,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            child: Text(
              actionText,
              style: CozyType.textTheme.bodySmall!.copyWith(
                color: AuthColors.primaryEnd,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// `OnboardingScreen.kt` -> `DashedAvatarPlaceholder`.
///
/// Dashed circle of radius `min/2 - 3dp`, 2dp stroke, dash `[10dp, 8dp]`,
/// `AuthPrimaryEnd@0.82`, with a 42dp `Add` icon in the centre.
class DashedAvatarPlaceholder extends StatelessWidget {
  const DashedAvatarPlaceholder({super.key, this.size = 128});

  final double size;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      size: Size.square(size),
      painter: _DashedCirclePainter(),
      child: SizedBox(
        width: size,
        height: size,
        child: const Icon(
          Icons.add,
          size: 42,
          color: AuthColors.primaryEnd,
          semanticLabel: '添加头像',
        ),
      ),
    );
  }
}

class _DashedCirclePainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final double radius = size.shortestSide / 2 - 3;
    final Path circle = Path()
      ..addOval(
        Rect.fromCircle(
          center: Offset(size.width / 2, size.height / 2),
          radius: radius,
        ),
      );

    final Paint paint = Paint()
      ..color = AuthColors.primaryEnd.withValues(alpha: 0.82)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;

    for (final metric in circle.computeMetrics()) {
      double distance = 0;
      int index = 0;
      const List<double> pattern = <double>[10, 8];
      while (distance < metric.length) {
        final double segment = pattern[index % pattern.length];
        final double next = math.min(distance + segment, metric.length);
        if (index.isEven) {
          canvas.drawPath(metric.extractPath(distance, next), paint);
        }
        distance = next;
        index++;
      }
    }
  }

  @override
  bool shouldRepaint(covariant _DashedCirclePainter oldDelegate) => false;
}
