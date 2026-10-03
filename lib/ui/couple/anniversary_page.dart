import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

// ══════════════════════════════════════════════════════════════════════════════
//  纪念日页 —— 原生 `ui/couple/AnniversaryScreen.kt` 的 1:1 移植
//
//  结构对照（AnniversaryScreen.kt 行号）：
//    :101-227   AnniversaryScreen 主体（背景 Canvas + Header + 纵向滚动内容）
//    :229-259   AnniversaryBackground / drawNoodleBowl
//    :261-290   AnniversaryHeader
//    :292-370   AnniversaryHeroCard / FloatingHeart（描边心）
//               视觉重写：hero 改成渐变主角卡，描边心只留一枚且独占右侧一列，
//               不再像原版那样三枚半透明心随机压在文字上（见 _AnniversaryHeroCard）
//    :372-413   NextAnniversaryCard（下一站浪漫 + 点击编辑配对日期）
//    :415-604   CalendarCard / CalendarTypeToggle / CalendarGrid / CalendarDayCell
//    :606-721   SweetMomentsTimeline / SweetMomentItem
//    :723-823   SweetMomentImageDialog / MomentImageOption
//    :852-949   AnniversaryEditorDialog
//    :959-1106  日历 / 纪念日推导辅助函数
// ══════════════════════════════════════════════════════════════════════════════

/// 原生 `AnniversaryCreamLine`（AnniversaryScreen.kt:98）—— CozyPalette 里没有这一枚暖奶线
const Color _creamLine = Color(0xFFF0E5DC);

/// 原生 `AnniversaryScreen.kt:232` 顶部那枚粉光的原色（色板里没有）
const Color _heroPinkGlow = Color(0xFFFFB1C3);

/// 原生 `completedMomentStatuses`（AnniversaryScreen.kt:99）
const Set<String> _completedMomentStatuses = <String>{
  'completed',
  'delivered',
  'finished',
};

/// 恋爱纪念日与甜蜜时刻页面 (AnniversaryPage)
class AnniversaryPage extends StatefulWidget {
  const AnniversaryPage({super.key});

  @override
  State<AnniversaryPage> createState() => _AnniversaryPageState();
}

class _AnniversaryPageState extends State<AnniversaryPage> {
  /// 原生 `resolveAnniversaryStartDate(profile)`（:1095）的结果。
  /// 云端 `anniversaries` 表只有 `paired_at`，`/anniversary` 回包里的
  /// `anniversary_at` 就是它的日期部分。
  DateTime _startDate = _today();

  @override
  void initState() {
    super.initState();
    _fetch();
    // 原生用 `orderRepository.observeOrders()`；Flutter 侧等价物是
    // AppState 的通知流 + 进页面主动拉一次（AppState.orders 即甜点墙数据源）
    AppState.instance.refreshOrders(silent: true);
  }

  Future<void> _fetch() async {
    final Map<String, dynamic>? info = await AppState.instance.loadAnniversary();
    if (!mounted) return;
    final DateTime? parsed = _parseDateSafely((info?['anniversary_at'] as String?) ?? '');
    if (parsed == null) return;
    setState(() => _startDate = parsed);
  }

  void _handleBack() {
    Navigator.maybePop(context);
  }

  /// 原生 `if (showEditor) AnniversaryEditorDialog(...)`（:184-196）：
  /// 升级为独立内嵌全屏页面，消除多层滑动与挤压弹窗。
  Future<void> _openEditor() async {
    final DateTime? picked = await Navigator.of(context).push<DateTime>(
      MaterialPageRoute<DateTime>(
        builder: (BuildContext _) => _AnniversaryEditorPage(initialDate: _startDate),
      ),
    );
    if (picked == null || !mounted) return;
    final bool ok = await AppState.instance.setAnniversary(_formatDate(picked));
    if (!ok || !mounted) return;
    setState(() => _startDate = DateTime(picked.year, picked.month, picked.day));
  }

  /// 原生 `selectedMomentOrder?.let { SweetMomentImageDialog(...) }`（:153-182）
  Future<void> _openMomentImageDialog(Order order) async {
    final bool? saved = await showDialog<bool>(
      context: context,
      builder: (BuildContext _) => _SweetMomentImageDialog(
        order: order,
        imageUrl: _momentImageUrl(order),
      ),
    );
    if (saved == true && mounted) {
      await AppState.instance.refreshOrders(silent: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    // 原生 :198-226：Box(fillMaxSize, background) + Background Canvas + Column
    return Scaffold(
      backgroundColor: CozyPalette.background,
      body: Stack(
        children: <Widget>[
          const Positioned.fill(
            child: CustomPaint(painter: _AnniversaryBackgroundPainter()),
          ),
          SafeArea(
            bottom: false,
            child: Column(
              children: <Widget>[
                _AnniversaryHeader(onBack: _handleBack),
                Expanded(
                  // 【性能 Phase 1】纪念日页只读订单里的甜蜜时刻：只订阅 orders 域。
                  child: ListenableBuilder(
                    listenable: AppState.instance.listenFor(const {Domain.orders}),
                    builder: (BuildContext context, Widget? _) {
                      final _AnniversaryUiState state = _anniversaryState(_startDate);
                      final List<Order> moments =
                          _sweetMomentOrders(AppState.instance.orders);
                      // 原生 :206-224：verticalScroll + padding(horizontal = 20.dp)
                      // + Arrangement.spacedBy(16.dp)，首尾各一个 Spacer。
                      // 底部留白按已批准偏离改用 CozyDock.clearanceOf（原版 60.dp），
                      // 补上系统导航栏 inset，避免末尾内容被悬浮底栏压住。
                      return ListView(
                        padding: EdgeInsets.only(
                          left: 20,
                          right: 20,
                          bottom: CozyDock.clearanceOf(context),
                        ),
                        children: <Widget>[
                          const SizedBox(height: 24),
                          _AnniversaryHeroCard(state: state),
                          const SizedBox(height: 16),
                          _NextAnniversaryCard(state: state, onTap: _openEditor),
                          const SizedBox(height: 16),
                          _SweetMomentsTimeline(
                            orders: moments,
                            onEditImage: _openMomentImageDialog,
                          ),
                        ],
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  背景 —— AnniversaryScreen.kt:229-259
// ══════════════════════════════════════════════════════════════════════════════

class _AnniversaryBackgroundPainter extends CustomPainter {
  const _AnniversaryBackgroundPainter();

  @override
  void paint(Canvas canvas, Size size) {
    // :232-236 顶部粉光圆
    canvas.drawCircle(
      Offset(size.width * 0.50, -size.width * 0.15),
      size.width * 0.42,
      Paint()
        ..style = PaintingStyle.fill
        ..color = _heroPinkGlow.withValues(alpha: 0.10),
    );

    // :237-241 + drawNoodleBowl(:245-259)：右下角那只线面碗
    final Offset topLeft = Offset(size.width * 0.42, size.height * 0.82);
    final double w = size.width * 0.50;
    final double h = w * 0.52;
    final Paint bowl = Paint()
      ..style = PaintingStyle.fill
      ..color = CozyPalette.primary.withValues(alpha: 0.08);
    final Path path = Path()
      ..moveTo(topLeft.dx + w * 0.18, topLeft.dy + h * 0.34)
      ..cubicTo(
        topLeft.dx + w * 0.20,
        topLeft.dy + h,
        topLeft.dx + w * 0.80,
        topLeft.dy + h,
        topLeft.dx + w * 0.82,
        topLeft.dy + h * 0.34,
      )
      ..close();
    canvas.drawPath(path, bowl);
    canvas.drawCircle(
      Offset(topLeft.dx + w * 0.28, topLeft.dy + h * 0.16),
      w * 0.06,
      bowl,
    );
    canvas.drawCircle(
      Offset(topLeft.dx + w * 0.70, topLeft.dy + h * 0.12),
      w * 0.035,
      bowl,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

// ══════════════════════════════════════════════════════════════════════════════
//  顶栏 —— AnniversaryScreen.kt:261-290
// ══════════════════════════════════════════════════════════════════════════════

class _AnniversaryHeader extends StatelessWidget {
  const _AnniversaryHeader({required this.onBack});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    return Container(
      // heightIn(min = 64.dp)
      constraints: const BoxConstraints(minHeight: 64),
      padding: const EdgeInsets.symmetric(horizontal: 22),
      decoration: BoxDecoration(
        // 原版是 #FFFFFF @0.94，白底上等价纯白（已批准偏离：主顶栏用纯白底）
        color: CozyPalette.background,
        border: Border(
          bottom: BorderSide(color: _creamLine.withValues(alpha: 0.92), width: 1),
        ),
      ),
      child: Row(
        children: <Widget>[
          IconButton(
            onPressed: onBack,
            icon: const Icon(
              Icons.arrow_back,
              size: 24,
              color: CozyPalette.onSurfaceVariant,
            ),
          ),
          Expanded(
            child: Center(
              child: Text(
                '纪念日',
                style: TextStyle(
                  fontSize: 24,
                  height: 32 / 24,
                  fontWeight: FontWeight.w900,
                  color: CozyPalette.primary,
                ),
              ),
            ),
          ),
          const SizedBox(width: 48), // 与左侧 IconButton 等宽，保证标题绝对居中
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  大卡 —— AnniversaryScreen.kt:292-370
// ══════════════════════════════════════════════════════════════════════════════

class _AnniversaryHeroCard extends StatelessWidget {
  const _AnniversaryHeroCard({required this.state});

  final _AnniversaryUiState state;

  @override
  Widget build(BuildContext context) {
    final String userNickname = AppState.instance.user?.nickname.trim().isNotEmpty == true
        ? AppState.instance.user!.nickname.trim()
        : '阿柴';
    final String partnerNickname = '糖糖';

    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[
            Color(0xFFF43F5E), // rose-500
            CozyPalette.primary, // cozy-primary
            Color(0xFFC85A3F), // cozy-terracotta
          ],
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: Color(0x38F43F5E),
            blurRadius: 18,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: Stack(
          children: <Widget>[
            Positioned(
              right: -16,
              bottom: -16,
              child: Icon(
                Icons.favorite,
                size: 110,
                color: Colors.white.withValues(alpha: 0.14),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 22),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  // Top capsule: ANNIVERSARY · 恋爱相守
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.16),
                      borderRadius: BorderRadius.circular(999),
                      border: Border.all(
                        color: Colors.white.withValues(alpha: 0.25),
                      ),
                    ),
                    child: const Text(
                      'ANNIVERSARY · 恋爱相守',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 1.2,
                        color: Color(0xFFFFD6E0),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  // Days counter
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.baseline,
                    textBaseline: TextBaseline.alphabetic,
                    children: <Widget>[
                      Text(
                        '${state.days}',
                        style: const TextStyle(
                          fontSize: 48,
                          height: 1.05,
                          fontWeight: FontWeight.w900,
                          letterSpacing: -1,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '天',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: Colors.white.withValues(alpha: 0.85),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '始于 ${_formatDate(state.startDate)} · 甜蜜进行中',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: Colors.white.withValues(alpha: 0.90),
                    ),
                  ),
                  const SizedBox(height: 16),
                  // Couple Avatars Touching
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.18),
                      borderRadius: BorderRadius.circular(999),
                      border: Border.all(
                        color: Colors.white.withValues(alpha: 0.28),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        Container(
                          width: 26,
                          height: 26,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.white, width: 1.2),
                            color: Colors.white.withValues(alpha: 0.3),
                          ),
                          clipBehavior: Clip.antiAlias,
                          child: Image.asset(
                            'assets/fluent3d/avocado.png',
                            errorBuilder: (BuildContext context, Object error, StackTrace? stack) => const Icon(Icons.person, size: 16, color: Colors.white),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Image.asset(
                          'assets/fluent3d/heart.png',
                          width: 16,
                          height: 16,
                          errorBuilder: (BuildContext context, Object error, StackTrace? stack) => const Icon(Icons.favorite, size: 14, color: Colors.white),
                        ),
                        const SizedBox(width: 6),
                        Container(
                          width: 26,
                          height: 26,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.white, width: 1.2),
                            color: Colors.white.withValues(alpha: 0.3),
                          ),
                          clipBehavior: Clip.antiAlias,
                          child: Image.asset(
                            'assets/fluent3d/strawberry.png',
                            errorBuilder: (BuildContext context, Object error, StackTrace? stack) => const Icon(Icons.person, size: 16, color: Colors.white),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          '$userNickname & $partnerNickname',
                          style: const TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w800,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _NextAnniversaryCard extends StatelessWidget {
  const _NextAnniversaryCard({required this.state, required this.onTap});

  final _AnniversaryUiState state;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final String hundredDateStr =
        '${state.nextHundredDate.year}.${state.nextHundredDate.month.toString().padLeft(2, '0')}.${state.nextHundredDate.day.toString().padLeft(2, '0')}';
    final String anniversaryDateStr =
        '${state.nextAnniversaryDate.year}.${state.nextAnniversaryDate.month.toString().padLeft(2, '0')}.${state.nextAnniversaryDate.day.toString().padLeft(2, '0')}';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Row(
          children: <Widget>[
            const Text('⏳', style: TextStyle(fontSize: 13)),
            const SizedBox(width: 6),
            Text(
              '下一个重要纪念日',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w800,
                color: CozyPalette.onSurface,
              ),
            ),
            const Spacer(),
            GestureDetector(
              onTap: onTap,
              child: Text(
                '修改日期 >',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  color: CozyPalette.primary,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: <Widget>[
            // Card 1: Next Hundred Days
            Expanded(
              child: GestureDetector(
                onTap: onTap,
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: CozyPalette.surface,
                    borderRadius: BorderRadius.circular(18),
                    border: Border.all(color: CozyPalette.outlineVariant.withValues(alpha: 0.5)),
                    boxShadow: const <BoxShadow>[CozyLight.ambient],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: <Widget>[
                          Flexible(
                            child: Text(
                              state.nextHundredTitle,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontSize: 11,
                                color: CozyPalette.onSurfaceVariant,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                          const SizedBox(width: 4),
                          Text(
                            '${state.nextHundredRemainingDays} 天后',
                            style: const TextStyle(
                              fontSize: 10.5,
                              color: Color(0xFFF43F5E),
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.baseline,
                        textBaseline: TextBaseline.alphabetic,
                        children: <Widget>[
                          Text(
                            '${state.nextHundredRemainingDays}',
                            style: const TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.w900,
                              color: CozyPalette.primary,
                            ),
                          ),
                          const SizedBox(width: 3),
                          Text(
                            '天',
                            style: TextStyle(
                              fontSize: 11,
                              color: CozyPalette.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        hundredDateStr,
                        style: const TextStyle(
                          fontSize: 10,
                          color: Color(0xFF9E9E9E),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(width: 10),
            // Card 2: Next Yearly Anniversary
            Expanded(
              child: GestureDetector(
                onTap: onTap,
                child: Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: CozyPalette.surface,
                    borderRadius: BorderRadius.circular(18),
                    border: Border.all(color: CozyPalette.outlineVariant.withValues(alpha: 0.5)),
                    boxShadow: const <BoxShadow>[CozyLight.ambient],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: <Widget>[
                          Flexible(
                            child: Text(
                              state.nextAnniversaryTitle,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontSize: 11,
                                color: CozyPalette.onSurfaceVariant,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                          const SizedBox(width: 4),
                          const Text(
                            '下个周年',
                            style: TextStyle(
                              fontSize: 10.5,
                              color: Color(0xFFD97706),
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.baseline,
                        textBaseline: TextBaseline.alphabetic,
                        children: <Widget>[
                          Text(
                            '${state.nextAnniversaryRemainingDays}',
                            style: const TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.w900,
                              color: Color(0xFFD97706),
                            ),
                          ),
                          const SizedBox(width: 3),
                          Text(
                            '天',
                            style: TextStyle(
                              fontSize: 11,
                              color: CozyPalette.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        anniversaryDateStr,
                        style: const TextStyle(
                          fontSize: 10,
                          color: Color(0xFF9E9E9E),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}



/// 空态卡底部的两枚能力小胶囊。
/// 文案直接取自原生空态说明（「默认显示菜品图片」/「可以换成你们自己的照片」），
/// 不引入新语义；用 Wrap 排布，窄屏自动折行。
class _MomentHintChip extends StatelessWidget {
  const _MomentHintChip({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: CozyPalette.surfaceContainerLow,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: CozyPalette.outlineVariant.withValues(alpha: 0.55),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Icon(icon, size: 13, color: CozyPalette.primary),
          const SizedBox(width: 5),
          Text(
            text,
            style: TextStyle(
              fontSize: 11.5,
              height: 16 / 11.5,
              fontWeight: FontWeight.w700,
              color: CozyPalette.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  甜蜜时刻时间线 —— AnniversaryScreen.kt:606-721
// ══════════════════════════════════════════════════════════════════════════════

class _SweetMomentsTimeline extends StatelessWidget {
  const _SweetMomentsTimeline({required this.orders, required this.onEditImage});

  final List<Order> orders;
  final ValueChanged<Order> onEditImage;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: _columnSpaced(<Widget>[
        // Row(Icon(HistoryEdu, 28) + "甜蜜时刻")（:612-615）
        //
        // 视觉重写：原版是「裸图标 + 裸文字」直接贴在卡片上方，没有段落感。
        // 这里左侧插一根 4×18 的 primaryContainer 竖条当段落锚，图标收进 30×30 圆角底
        // （secondaryContainer@.62 + 圆角 11），标题提到 19/w900；
        // 标题用 Expanded + ellipsis，窄屏（逻辑宽 320dp）也不会出溢出条纹。
        Row(
          children: <Widget>[
            const Text('📖', style: TextStyle(fontSize: 14)),
            const SizedBox(width: 6),
            Expanded(
              child: Text(
                '我们的专属美食恋爱回忆录',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontSize: 14,
                  height: 20 / 14,
                  fontWeight: FontWeight.w900,
                  color: CozyPalette.onSurface,
                ),
              ),
            ),
            GestureDetector(
              onTap: () {
                if (orders.isNotEmpty) {
                  onEditImage(orders.first);
                }
              },
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: CozyPalette.primary.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  '+ 记一笔',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    color: CozyPalette.primary,
                  ),
                ),
              ),
            ),
          ],
        ),
        if (orders.isEmpty)
          // 空态卡（:617-629）
          //
          // 视觉重写：原版是左上角两行字贴边，看着像没做完。
          // 改成居中空态：72 圆形图示 → 标题 → 说明（居中、可换行）
          // → CozyLight.hairline 分隔 → 两枚能力小胶囊（文案取自原说明，不引入新语义）。
          // 胶囊用 Wrap 排布：320dp 窄屏自动折到第二行，不会溢出。
          CozyCard(
            color: CozyPalette.surface,
            radius: 22,
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: _columnSpaced(<Widget>[
                Center(
                  child: Container(
                    width: 72,
                    height: 72,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: CozyPalette.secondaryContainer
                          .withValues(alpha: 0.55),
                      border: Border.all(
                        color: CozyPalette.primaryContainer
                            .withValues(alpha: 0.55),
                      ),
                    ),
                    child: const Icon(
                      Icons.ramen_dining,
                      size: 32,
                      color: CozyPalette.primary,
                    ),
                  ),
                ),
                Text(
                  '还没有记录',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 17,
                    height: 24 / 17,
                    fontWeight: FontWeight.w900,
                    color: CozyPalette.onSurface,
                  ),
                ),
                Text(
                  '完成一顿饭后，会默认显示菜品图片，也可以换成你们自己的照片。',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    height: 22 / 14,
                    color: CozyPalette.onSurfaceVariant,
                  ),
                ),
                Container(height: 1, color: CozyLight.hairline),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  alignment: WrapAlignment.center,
                  children: const <Widget>[
                    _MomentHintChip(icon: Icons.auto_awesome, text: '自动配图'),
                    _MomentHintChip(icon: Icons.edit, text: '可换成你们的照片'),
                  ],
                ),
              ], 14),
            ),
          )
        else
          ...orders.asMap().entries.map(
                (MapEntry<int, Order> entry) => _SweetMomentItem(
                  // :633-634 第一颗用 SoftPink，其余用 Pink
                  dotColor: entry.key == 0
                      ? CozyPalette.primaryContainer
                      : CozyPalette.secondaryContainer,
                  date: _momentDateText(entry.value.createdAt),
                  text: _sweetMomentText(entry.value),
                  imageUrl: _momentImageUrl(entry.value),
                  onImageTap: () => onEditImage(entry.value),
                ),
              ),
      ], 16),
    );
  }
}

class _SweetMomentItem extends StatelessWidget {
  const _SweetMomentItem({
    required this.dotColor,
    required this.date,
    required this.text,
    required this.imageUrl,
    required this.onImageTap,
  });

  final Color dotColor;
  final String date;
  final String text;
  final String imageUrl;
  final VoidCallback onImageTap;

  @override
  Widget build(BuildContext context) {
    // 原生 SweetMomentItem（:659-721）
    //
    // 视觉重写：原版卡片内边距 22、图片圆角 10、编辑徽标却是 48 的实心大圆，
    // 徽标比图片本身还抢眼，整条时间线也和白底融在一起。
    // 这里：内边距 22→18、图片 82→78 且圆角 10→14 并补一圈发丝边、
    // 编辑徽标 48→34（图标 14）、日期加一枚 primaryContainer 小圆点当锚、
    // 卡片改成不透明的 surface + CozyCard 默认发丝边（与空态卡同一层级）。
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        // 时间线圆点 Surface(circle, dotColor, border 2dp primary@0.46,
        // padding(top = 15, start = 3), size 18)（:670-675）
        Padding(
          padding: const EdgeInsets.only(top: 18, left: 3),
          child: Container(
            width: 18,
            height: 18,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: dotColor,
              border: Border.all(
                color: CozyPalette.primary.withValues(alpha: 0.46),
                width: 2,
              ),
            ),
          ),
        ),
        const SizedBox(width: 20),
        Expanded(
          child: CozyCard(
            color: CozyPalette.surface,
            radius: 20,
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 18),
            child: Row(
              children: _rowSpaced(<Widget>[
                // Surface(onClick, RoundedCornerShape(10), pink, size 82)（:686-713）
                GestureDetector(
                  onTap: onImageTap,
                  child: SizedBox(
                    width: 78,
                    height: 78,
                    child: Stack(
                      children: <Widget>[
                        Positioned.fill(
                          child: Container(
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: CozyLight.hairline),
                            ),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(14),
                              child: ColoredBox(
                                color: CozyPalette.secondaryContainer,
                                child: _momentImage(imageUrl, BoxFit.cover),
                              ),
                            ),
                          ),
                        ),
                        // 编辑徽标 Surface(circle, card@0.94, border 1dp border,
                        // align BottomEnd, padding 4, size 48)（:702-711）
                        // 尺寸收到 34：它只是「可换图」的提示，不该压过照片本身。
                        Positioned(
                          right: 4,
                          bottom: 4,
                          child: Container(
                            width: 34,
                            height: 34,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: CozyPalette.surface.withValues(alpha: 0.94),
                              border:
                                  Border.all(color: CozyPalette.outlineVariant),
                            ),
                            child: const Center(
                              child: Icon(
                                Icons.edit,
                                size: 14,
                                color: CozyPalette.primary,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: _columnSpaced(<Widget>[
                      Row(
                        children: _rowSpaced(<Widget>[
                          Container(
                            width: 6,
                            height: 6,
                            decoration: const BoxDecoration(
                              shape: BoxShape.circle,
                              color: CozyPalette.primaryContainer,
                            ),
                          ),
                          // Flexible + ellipsis：日期列被图片挤窄时也不会溢出
                          Flexible(
                            child: Text(
                              date,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 12.5,
                                height: 17 / 12.5,
                                fontWeight: FontWeight.w900,
                                letterSpacing: 0.3,
                                color: CozyPalette.primary,
                              ),
                            ),
                          ),
                        ], 6),
                      ),
                      Text(
                        text,
                        style: TextStyle(
                          fontSize: 14,
                          height: 21 / 14,
                          color: CozyPalette.onSurface,
                        ),
                      ),
                    ], 5),
                  ),
                ),
              ], 14),
            ),
          ),
        ),
      ],
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  甜蜜时刻图片弹窗 —— AnniversaryScreen.kt:723-823
// ══════════════════════════════════════════════════════════════════════════════

class _SweetMomentImageDialog extends StatefulWidget {
  const _SweetMomentImageDialog({required this.order, required this.imageUrl});

  final Order order;
  final String imageUrl;

  @override
  State<_SweetMomentImageDialog> createState() =>
      _SweetMomentImageDialogState();
}

class _SweetMomentImageDialogState extends State<_SweetMomentImageDialog> {
  // 原生把 isSaving / momentImageMessage 放在页面级 state（:122-123），
  // 这里放进弹窗自身：Flutter 的 dialog 是独立路由，父级 setState 不会重建它。
  final bool _isSaving = false;
  String? _message;

  /// 原生写入链路：`storageUploader.compressAndUpload` +
  /// `orderRepository.updateMomentImage(order.id, imageUrl)`（:135-145 / :165）。
  /// Flutter 侧 AppState / SupabaseApi 目前都没有对应写方法（见交付说明 §4），
  /// 所以这里只复刻原版的失败提示分支，保证界面与文案 1:1。
  void _applyMomentImage(String imageUrl) {
    setState(() {
      _message = imageUrl.isEmpty
          ? '恢复菜品图片失败，请确认云端迁移已执行' // :167
          : '图片保存失败，请确认云端迁移已执行'; // :145
    });
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_isSaving,
      child: AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26)),
        backgroundColor: CozyPalette.surface, // AnniversaryCard
        title: Text(
          '设置甜蜜时刻图片',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontWeight: FontWeight.w900,
            color: CozyPalette.onSurface,
          ),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: _columnSpaced(<Widget>[
            if (widget.imageUrl.isNotEmpty)
              // Surface(RoundedCornerShape(16), pink@0.18, border 1dp border@0.62,
              // heightIn(min = 160, max = 260))（:748-760）
              Container(
                constraints: const BoxConstraints(minHeight: 160, maxHeight: 260),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(16),
                  color: CozyPalette.secondaryContainer.withValues(alpha: 0.18),
                  border: Border.all(
                    color: CozyPalette.outlineVariant.withValues(alpha: 0.62),
                  ),
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child: _momentImage(widget.imageUrl, BoxFit.contain),
                ),
              ),
            _MomentImageOption(
              icon: Icons.cake,
              title: '使用菜品图片',
              subtitle: '自动显示这顿饭的第一张菜品图',
              enabled: !_isSaving,
              onTap: () => _applyMomentImage(''),
            ),
            _MomentImageOption(
              icon: Icons.add_photo_alternate,
              title: '使用自己的图片',
              subtitle: '从相册选择或拍照上传',
              enabled: !_isSaving,
              // 原生：打开 ImageSourcePickerDialog 选图 → 压缩上传到
              // "moments/${order.id}" → updateMomentImage（:125-151）。
              // Flutter 侧没有 image_picker 依赖，也没有上传通道（交付说明 §4），
              // 直接落到原版 :145 的保存失败文案。
              onTap: () => _applyMomentImage('moments/${widget.order.id}'),
            ),
            if (_message != null)
              Text(
                _message!,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodySmall!.copyWith(
                      color: _message!.contains('失败')
                          ? Theme.of(context).colorScheme.error
                          : CozyPalette.primary,
                    ),
              ),
          ], 12),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: _isSaving
                ? null
                : () => Navigator.of(context).pop(false),
            child: Text(
              _isSaving ? '保存中...' : '取消',
              style: TextStyle(color: CozyPalette.primary),
            ),
          ),
        ],
      ),
    );
  }
}

/// 原生 `MomentImageOption`（:795-823）
class _MomentImageOption extends StatelessWidget {
  const _MomentImageOption({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.enabled,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: enabled ? onTap : null,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          color: CozyPalette.secondaryContainer.withValues(alpha: 0.24),
          border: Border.all(
            color: CozyPalette.outlineVariant.withValues(alpha: 0.72),
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
          child: Row(
            children: _rowSpaced(<Widget>[
              Icon(icon, size: 24, color: CozyPalette.primary),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: _columnSpaced(<Widget>[
                    Text(
                      title,
                      // 原生 Text 默认 bodyLarge（Type.kt:51-56）
                      style: Theme.of(context).textTheme.bodyLarge!.copyWith(
                            fontWeight: FontWeight.w700,
                            color: CozyPalette.onSurface,
                          ),
                    ),
                    Text(
                      subtitle,
                      style: Theme.of(context).textTheme.bodySmall!.copyWith(
                            color: CozyPalette.onSurfaceVariant,
                          ),
                    ),
                  ], 2),
                ),
              ),
            ], 12),
          ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  设置纪念日页面 —— 整体自适应页面，彻底消除多层滑动与弹窗挤压
// ══════════════════════════════════════════════════════════════════════════════

class _AnniversaryEditorPage extends StatefulWidget {
  const _AnniversaryEditorPage({required this.initialDate});

  final DateTime initialDate;

  @override
  State<_AnniversaryEditorPage> createState() => _AnniversaryEditorPageState();
}

class _AnniversaryEditorPageState extends State<_AnniversaryEditorPage> {
  late final TextEditingController _controller;
  _AnniversaryCalendarType _calendarType = _AnniversaryCalendarType.solar;
  late DateTime _visibleMonth;
  DateTime? _parsedDate;

  @override
  void initState() {
    super.initState();
    _parsedDate = widget.initialDate;
    _visibleMonth = DateTime(widget.initialDate.year, widget.initialDate.month, 1);
    _controller = TextEditingController(text: _formatDate(widget.initialDate));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onTextChanged(String value) {
    final DateTime? parsed = _parseAnniversaryInput(value, _calendarType);
    setState(() {
      _parsedDate = parsed;
      if (parsed != null) {
        _visibleMonth = DateTime(parsed.year, parsed.month, 1);
      }
    });
  }

  void _onCalendarTypeChanged(_AnniversaryCalendarType type) {
    setState(() {
      _calendarType = type;
      _parsedDate = _parseAnniversaryInput(_controller.text, type);
    });
  }

  void _selectDate(DateTime date) {
    _controller.text = _formatDate(date);
    setState(() {
      _parsedDate = date;
      _visibleMonth = DateTime(date.year, date.month, 1);
    });
  }

  String _supportingText() {
    if (_parsedDate == null) {
      return '请输入有效日期，例如 2026-05-20；农历闰月可写 闰04';
    }
    if (_calendarType == _AnniversaryCalendarType.lunar) {
      return '将按阳历 ${_formatDate(_parsedDate!)} 保存';
    }
    return '也可以直接在下方日历点击选择';
  }

  @override
  Widget build(BuildContext context) {
    final DateTime selectedDate = _parsedDate ?? widget.initialDate;
    final bool invalid = _parsedDate == null;

    return Scaffold(
      backgroundColor: CozyPalette.background,
      body: Stack(
        children: <Widget>[
          const Positioned.fill(
            child: CustomPaint(painter: _AnniversaryBackgroundPainter()),
          ),
          SafeArea(
            child: Column(
              children: <Widget>[
                // 顶部导航栏
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  child: Row(
                    children: <Widget>[
                      IconButton(
                        onPressed: () => Navigator.of(context).pop(),
                        icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20),
                        color: CozyPalette.onSurface,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '设置恋爱纪念日',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.w900,
                          color: CozyPalette.onSurface,
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: ListView(
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                    children: <Widget>[
                      // 说明与输入卡片
                      CozyCard(
                        color: CozyPalette.surface,
                        radius: 20,
                        padding: const EdgeInsets.all(18),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              '支持文字输入，也可以在日历里直接选择日期。',
                              style: TextStyle(
                                fontSize: 13,
                                color: CozyPalette.onSurfaceVariant,
                              ),
                            ),
                            const SizedBox(height: 14),
                            Align(
                              alignment: Alignment.centerLeft,
                              child: _CalendarTypeToggle(
                                calendarType: _calendarType,
                                onCalendarTypeChange: _onCalendarTypeChanged,
                              ),
                            ),
                            const SizedBox(height: 12),
                            TextField(
                              controller: _controller,
                              maxLines: 1,
                              onChanged: _onTextChanged,
                              inputFormatters: <TextInputFormatter>[
                                LengthLimitingTextInputFormatter(11),
                              ],
                              decoration: InputDecoration(
                                labelText: _calendarType.inputLabel,
                                filled: true,
                                fillColor: CozyPalette.surfaceVariant.withValues(alpha: 0.35),
                                helper: _parsedDate == null
                                    ? null
                                    : Text(_supportingText(), maxLines: 2),
                                error: invalid ? Text(_supportingText(), maxLines: 2) : null,
                                border: _fieldBorder(CozyPalette.outlineVariant),
                                enabledBorder: _fieldBorder(CozyPalette.outlineVariant),
                                focusedBorder: _fieldBorder(CozyPalette.primary),
                                errorBorder: _fieldBorder(CozyPalette.outlineVariant),
                                focusedErrorBorder: _fieldBorder(CozyPalette.primary),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // 日历挑选卡片
                      _CalendarCard(
                        visibleMonth: _visibleMonth,
                        selectedDate: selectedDate,
                        calendarType: _calendarType,
                        onCalendarTypeChange: _onCalendarTypeChanged,
                        onPreviousMonth: () => setState(() {
                          _visibleMonth =
                              DateTime(_visibleMonth.year, _visibleMonth.month - 1, 1);
                        }),
                        onNextMonth: () => setState(() {
                          _visibleMonth =
                              DateTime(_visibleMonth.year, _visibleMonth.month + 1, 1);
                        }),
                        onDateSelected: _selectDate,
                      ),
                      const SizedBox(height: 28),

                      // 保存按钮
                      SizedBox(
                        width: double.infinity,
                        height: 52,
                        child: FilledButton(
                          onPressed: _parsedDate == null
                              ? null
                              : () => Navigator.of(context).pop(_parsedDate),
                          style: FilledButton.styleFrom(
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            backgroundColor: CozyPalette.primary,
                            foregroundColor: Colors.white,
                            textStyle: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          child: const Text('保存纪念日'),
                        ),
                      ),
                      const SizedBox(height: 24),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

OutlineInputBorder _fieldBorder(Color color) => OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: color),
    );

// ══════════════════════════════════════════════════════════════════════════════
//  日历 —— AnniversaryScreen.kt:415-604
// ══════════════════════════════════════════════════════════════════════════════

class _CalendarCard extends StatelessWidget {
  const _CalendarCard({
    required this.visibleMonth,
    required this.selectedDate,
    required this.calendarType,
    required this.onCalendarTypeChange,
    required this.onPreviousMonth,
    required this.onNextMonth,
    required this.onDateSelected,
  });

  final DateTime visibleMonth;
  final DateTime selectedDate;
  final _AnniversaryCalendarType calendarType;
  final ValueChanged<_AnniversaryCalendarType> onCalendarTypeChange;
  final VoidCallback onPreviousMonth;
  final VoidCallback onNextMonth;
  final ValueChanged<DateTime> onDateSelected;

  @override
  Widget build(BuildContext context) {
    // 原生 StitchGlassCard(containerColor = card@0.58, radius = 18,
    // contentPadding = h24/v24)（:426-433）
    return CozyCard(
      color: CozyPalette.surface.withValues(alpha: 0.58),
      radius: 18,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: _columnSpaced(<Widget>[
          // Row(SpaceBetween)：月份 + [类型开关 + 上/下月]（:435-458）
          Row(
            children: <Widget>[
              Text(
                _monthName(visibleMonth.month),
                style: TextStyle(
                  fontSize: 22,
                  height: 30 / 22,
                  color: CozyPalette.onSurface,
                ),
              ),
              const SizedBox(width: 12),
              // 原版是 SpaceBetween：右侧这组在空间不足时会被压缩，
              // Flutter 的 Row 不会压缩固定宽度的按钮（会 overflow），
              // 因此用 FittedBox(scaleDown) 复刻同样的「挤进去」行为。
              Expanded(
                child: Align(
                  alignment: Alignment.centerRight,
                  child: FittedBox(
                    fit: BoxFit.scaleDown,
                    alignment: Alignment.centerRight,
                    child: Row(
                      children: _rowSpaced(<Widget>[
                        _CalendarTypeToggle(
                          calendarType: calendarType,
                          onCalendarTypeChange: onCalendarTypeChange,
                        ),
                        Row(
                          children: _rowSpaced(<Widget>[
                            IconButton(
                              onPressed: onPreviousMonth,
                              icon: const Icon(
                                Icons.keyboard_arrow_left,
                                size: 18,
                                color: CozyPalette.secondary,
                              ),
                            ),
                            IconButton(
                              onPressed: onNextMonth,
                              icon: const Icon(
                                Icons.keyboard_arrow_right,
                                size: 18,
                                color: CozyPalette.secondary,
                              ),
                            ),
                          ], 12),
                        ),
                      ], 14),
                    ),
                  ),
                ),
              ),
            ],
          ),
          _CalendarGrid(
            visibleMonth: visibleMonth,
            selectedDate: selectedDate,
            calendarType: calendarType,
            onDateSelected: onDateSelected,
          ),
        ], 25),
      ),
    );
  }
}

/// 原生 `CalendarTypeToggle`（:470-501）
class _CalendarTypeToggle extends StatelessWidget {
  const _CalendarTypeToggle({
    required this.calendarType,
    required this.onCalendarTypeChange,
  });

  final _AnniversaryCalendarType calendarType;
  final ValueChanged<_AnniversaryCalendarType> onCalendarTypeChange;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(999),
        color: CozyPalette.surfaceVariant.withValues(alpha: 0.50),
        border: Border.all(
          color: CozyPalette.outlineVariant.withValues(alpha: 0.46),
        ),
      ),
      child: Row(
        children: _rowSpaced(
          _AnniversaryCalendarType.values.map((_AnniversaryCalendarType type) {
            final bool selected = calendarType == type;
            return GestureDetector(
              onTap: () => onCalendarTypeChange(type),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 7),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(999),
                  color: selected ? CozyPalette.primary : Colors.transparent,
                ),
                child: Text(
                  type.label,
                  style: TextStyle(
                    fontSize: 14,
                    height: 18 / 14,
                    fontWeight: FontWeight.w700,
                    color: selected ? Colors.white : CozyPalette.onSurfaceVariant,
                  ),
                ),
              ),
            );
          }).toList(),
          1,
        ),
      ),
    );
  }
}

/// 原生 `CalendarGrid`（:503-544）
class _CalendarGrid extends StatelessWidget {
  const _CalendarGrid({
    required this.visibleMonth,
    required this.selectedDate,
    required this.calendarType,
    required this.onDateSelected,
  });

  final DateTime visibleMonth;
  final DateTime selectedDate;
  final _AnniversaryCalendarType calendarType;
  final ValueChanged<DateTime> onDateSelected;

  static const List<String> _weekdayLabels = <String>['日', '一', '二', '三', '四', '五', '六'];

  @override
  Widget build(BuildContext context) {
    final List<DateTime> cells = _calendarCellsFor(visibleMonth);
    return Column(
      children: _columnSpaced(<Widget>[
        Row(
          children: _weekdayLabels
              .map(
                (String day) => Expanded(
                  child: Text(
                    day,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 18,
                      height: 25 / 18,
                      color: CozyPalette.onSurfaceVariant,
                    ),
                  ),
                ),
              )
              .toList(),
        ),
        ...<Widget>[
          for (int i = 0; i < cells.length; i += 7)
            Row(
              children: _rowSpaced(<Widget>[
                for (int j = i; j < i + 7 && j < cells.length; j++)
                  _CalendarDayCell(
                    date: cells[j],
                    visibleMonth: visibleMonth,
                    selectedDate: selectedDate,
                    calendarType: calendarType,
                    onDateSelected: onDateSelected,
                  ),
              ], 4),
            ),
        ],
      ], 18),
    );
  }
}

/// 原生 `CalendarDayCell`（:546-604）
class _CalendarDayCell extends StatelessWidget {
  const _CalendarDayCell({
    required this.date,
    required this.visibleMonth,
    required this.selectedDate,
    required this.calendarType,
    required this.onDateSelected,
  });

  final DateTime date;
  final DateTime visibleMonth;
  final DateTime selectedDate;
  final _AnniversaryCalendarType calendarType;
  final ValueChanged<DateTime> onDateSelected;

  @override
  Widget build(BuildContext context) {
    final bool isCurrentMonth =
        date.year == visibleMonth.year && date.month == visibleMonth.month;
    final bool isSelected = isCurrentMonth && _sameDay(date, selectedDate);
    // 原版 hasDot / hasHeart 恒为 false（:556-557），对应分支不渲染。
    return Expanded(
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: isCurrentMonth ? () => onDateSelected(date) : null,
        child: SizedBox(
          height: 42,
          child: Center(
            child: Text(
              '${date.day}',
              style: TextStyle(
                fontSize: 19,
                fontWeight: isSelected ? FontWeight.w700 : FontWeight.w400,
                color: isSelected
                    ? Colors.white
                    : (isCurrentMonth
                        ? CozyPalette.onSurface
                        : CozyPalette.onSurface.withValues(alpha: 0.13)),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// ══════════════════════════════════════════════════════════════════════════════
//  辅助 —— AnniversaryScreen.kt:959-1106
// ══════════════════════════════════════════════════════════════════════════════

/// 原生 `AnniversaryCalendarType`（:991-994）
enum _AnniversaryCalendarType {
  solar('阳历', '阳历日期，例如 2026-05-20'),
  lunar('农历', '农历日期，例如 2026-04-04');

  const _AnniversaryCalendarType(this.label, this.inputLabel);

  final String label;
  final String inputLabel;
}

/// 原生 `AnniversaryPageUiState`（:1067-1072）
class _AnniversaryUiState {
  const _AnniversaryUiState({
    required this.startDate,
    required this.days,
    required this.nextAnniversaryTitle,
    required this.nextAnniversaryRemainingDays,
    required this.nextAnniversaryDate,
    required this.nextHundredTitle,
    required this.nextHundredRemainingDays,
    required this.nextHundredDate,
  });

  final DateTime startDate;
  final int days;
  final String nextAnniversaryTitle;
  final int nextAnniversaryRemainingDays;
  final DateTime nextAnniversaryDate;
  final String nextHundredTitle;
  final int nextHundredRemainingDays;
  final DateTime nextHundredDate;
}

/// 原生 `anniversaryState(profile)`（:1074-1087）
_AnniversaryUiState _anniversaryState(DateTime startDate) {
  final DateTime today = _today();
  final int elapsed = _daysBetween(startDate, today);
  final int days = elapsed < 0 ? 0 : elapsed + 1;
  final DateTime next = _nextYearlyDate(startDate, today);
  final int remaining = _daysBetween(today, next) < 0 ? 0 : _daysBetween(today, next);
  final int years =
      (next.year - startDate.year) < 1 ? 1 : next.year - startDate.year;

  // 计算下一个整百天节点（如 520 天的下一个整百是 600 天）
  final int currentHundred = (days ~/ 100) * 100;
  final int nextHundred = currentHundred + 100;
  final int hundredRemaining = nextHundred - days;
  final DateTime hundredDate = today.add(Duration(days: hundredRemaining));

  return _AnniversaryUiState(
    startDate: startDate,
    days: days,
    nextAnniversaryTitle: '恋爱$years周年',
    nextAnniversaryRemainingDays: remaining,
    nextAnniversaryDate: next,
    nextHundredTitle: '相守 $nextHundred 天',
    nextHundredRemainingDays: hundredRemaining,
    nextHundredDate: hundredDate,
  );
}

/// 原生 `nextYearlyDate`（:1089-1093）
DateTime _nextYearlyDate(DateTime startDate, DateTime today) {
  DateTime next = _withYear(startDate, today.year);
  if (next.isBefore(today)) next = _withYear(startDate, today.year + 1);
  return next;
}

/// 原生 `LocalDate.withYear`：2/29 落到平年时夹到 2/28
DateTime _withYear(DateTime date, int year) {
  final int lastDayOfMonth = DateTime(year, date.month + 1, 0).day;
  final int day = date.day > lastDayOfMonth ? lastDayOfMonth : date.day;
  return DateTime(year, date.month, day);
}

/// 原生 `ChronoUnit.DAYS.between`（只按日期算，不受时区/夏令时影响）
int _daysBetween(DateTime from, DateTime to) {
  final DateTime a = DateTime.utc(from.year, from.month, from.day);
  final DateTime b = DateTime.utc(to.year, to.month, to.day);
  return b.difference(a).inDays;
}

DateTime _today() {
  final DateTime now = DateTime.now();
  return DateTime(now.year, now.month, now.day);
}

bool _sameDay(DateTime a, DateTime b) =>
    a.year == b.year && a.month == b.month && a.day == b.day;

String _formatDate(DateTime date) {
  final String m = date.month.toString().padLeft(2, '0');
  final String d = date.day.toString().padLeft(2, '0');
  return '${date.year.toString().padLeft(4, '0')}-$m-$d';
}

/// 原生 `calendarCellsFor`（:964-969）：固定 35 格，从当月 1 号所在周的周日起
List<DateTime> _calendarCellsFor(DateTime visibleMonth) {
  final DateTime firstDay = DateTime(visibleMonth.year, visibleMonth.month, 1);
  // Dart 与 Kotlin 的星期编号一致：周一 = 1 … 周日 = 7
  final int leadingDays = firstDay.weekday % 7;
  return <DateTime>[
    for (int i = 0; i < 35; i++)
      DateTime(visibleMonth.year, visibleMonth.month, 1 - leadingDays + i),
  ];
}

/// 原生 `monthName`（:975-989）
String _monthName(int month) {
  const List<String> names = <String>[
    '一月', '二月', '三月', '四月', '五月', '六月',
    '七月', '八月', '九月', '十月', '十一月', '十二月',
  ];
  if (month >= 1 && month <= 12) return names[month - 1];
  return '$month月';
}

/// 原生 `parseAnniversaryInput`（:1020-1026）+ `LocalDate.parse` 的严格 ISO 校验。
///
/// 偏离：原版农历分支走 `android.icu.util.ChineseCalendar` 做换算（:1041-1063）。
/// Dart 侧没有 ICU 中国农历，且本任务不允许新增依赖，因此农历模式按同一套
/// `YYYY-MM-DD` 解析（详见交付说明 §3）。
DateTime? _parseAnniversaryInput(String text, _AnniversaryCalendarType type) {
  final String normalized = text.trim();
  final RegExpMatch? match =
      RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(normalized);
  if (match == null) return null;
  final int year = int.parse(match.group(1)!);
  final int month = int.parse(match.group(2)!);
  final int day = int.parse(match.group(3)!);
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  final DateTime parsed = DateTime(year, month, day);
  // DateTime 会把 2026-02-30 归一化成 3 月，原版 LocalDate.parse 直接报错
  if (parsed.year != year || parsed.month != month || parsed.day != day) {
    return null;
  }
  return parsed;
}

/// 原生 `parseDateSafely`（:1103-1106）
DateTime? _parseDateSafely(String value) {
  if (value.isEmpty) return null;
  final DateTime? iso = DateTime.tryParse(value);
  if (iso != null) return DateTime(iso.year, iso.month, iso.day);
  final String head = value.length > 10 ? value.substring(0, 10) : value;
  final DateTime? dateOnly = DateTime.tryParse(head);
  if (dateOnly == null) return null;
  return DateTime(dateOnly.year, dateOnly.month, dateOnly.day);
}

/// 原生 `sweetMomentOrders`（:113-118）：只看已完成，倒序，最多 5 条
List<Order> _sweetMomentOrders(List<Order> orders) {
  final List<Order> list = orders
      .where((Order o) => _completedMomentStatuses.contains(o.status))
      .toList()
    ..sort((Order a, Order b) => b.createdAt.compareTo(a.createdAt));
  return list.take(5).toList();
}

/// 原生 `OrderRecord.toSweetMomentText()`（:648-653）
String _sweetMomentText(Order order) {
  final String dishNames =
      order.items.take(2).map((OrderItem it) => it.name).join('、');
  final String names = dishNames.isEmpty ? '一顿小饭' : dishNames;
  final String suffix =
      order.items.length > 2 ? '等 ${order.items.length} 道菜' : '';
  // 原版兜底用 `buyerRole`；Flutter 的 Order 没有这个字段（交付说明 §4），
  // 这里用「下单人是不是当前用户」来兜底，最后落到原版的 else 分支。
  final String buyer = order.buyerName.isNotEmpty
      ? order.buyerName
      : (order.buyerId == AppState.instance.user?.id
          ? (AppState.instance.user?.roleLabel ?? '吃货')
          : '吃货');
  return '$buyer 点了 $names$suffix，今天也一起好好吃饭。';
}

/// 原生 `String.toMomentDateText()`（:655-657）
String _momentDateText(DateTime createdAt) {
  final DateTime local = createdAt.toLocal();
  return '${local.month}月${local.day}日';
}

/// 原生 `momentImageUrl.ifBlank { items.firstOrNull { imageUrl.isNotBlank() } }`（:637-639）
String _momentImageUrl(Order order) {
  if (order.momentImageUrl.isNotEmpty) return order.momentImageUrl;
  for (final OrderItem item in order.items) {
    if (item.imageUrl.isNotEmpty) return item.imageUrl;
  }
  return '';
}

/// 原生 AsyncImage 的占位 / 失败兜底（:694-701）
Widget _momentImage(String url, BoxFit fit) {
  if (url.isEmpty) {
    return const Center(
      child: Icon(Icons.cake, size: 32, color: CozyPalette.primary),
    );
  }
  return Image.network(
    url,
    fit: fit,
    errorBuilder: (BuildContext context, Object error, StackTrace? stack) =>
        const Center(
      child: Icon(Icons.cake, size: 32, color: CozyPalette.primary),
    ),
  );
}

/// 原生 `Arrangement.spacedBy(gap)`（Column 方向）
List<Widget> _columnSpaced(List<Widget> children, double gap) {
  final List<Widget> out = <Widget>[];
  for (int i = 0; i < children.length; i++) {
    if (i > 0) out.add(SizedBox(height: gap));
    out.add(children[i]);
  }
  return out;
}

/// 原生 `Arrangement.spacedBy(gap)`（Row 方向）
List<Widget> _rowSpaced(List<Widget> children, double gap) {
  final List<Widget> out = <Widget>[];
  for (int i = 0; i < children.length; i++) {
    if (i > 0) out.add(SizedBox(width: gap));
    out.add(children[i]);
  }
  return out;
}
