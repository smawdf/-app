import 'dart:math' as math;

import 'package:flutter/foundation.dart' show listEquals;
import 'package:flutter/material.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';

/// 糖糖币管理页 —— 1:1 对照原生 `ui/candy/CandyCoinsScreen.kt`（共 448 行）
///
/// 私有 @Composable → 私有 Widget 的对应关系（层级与命名语义保持一致）：
///
///   CandyCoinsScreen()    → CandyCoinsPage / _CandyCoinsPageState   (:83)
///   CandyTopBar           → _CandyTopBar                            (:166)
///   CandyHeroCard         → _CandyHeroCard                          (:188)
///   RechargePanel         → _RechargePanel                          (:219)
///   RechargeButton        → _RechargeButton                         (:263)
///   ChartControlCard      → _ChartControlCard                       (:287)
///   SegmentedRow          → _SegmentedRow                           (:322)
///   CandyChart            → _CandyChartPainter                      (:346)
///   LedgerListCard        → _LedgerListCard + _LedgerRow            (:377)
///   RechargeConfirmDialog → _RechargeConfirmDialog                  (:404)
///   uiState.message 的 AlertDialog → _CandyMessageDialog            (:116)
///
/// 页面骨架、间距、圆角、配色、文案全部照抄原版；卡片容器换成设计系统的
/// `CozyCard`（原版 CandyCard 描边 → hairline、无投影 → 双层投影，是被批准的偏离）。
class CandyCoinsPage extends StatefulWidget {
  const CandyCoinsPage({super.key});

  @override
  State<CandyCoinsPage> createState() => _CandyCoinsPageState();
}

/// 原版页面顶部三个内联色常量（CandyCoinsScreen.kt:78-80）：
///   CandySurface #FFFFFF → CozyPalette.background（CozyPage 已铺）
///   CandyCard    #FFFCF8 → CozyCard 默认底色 CozyPalette.surface
///   CandyLine    #D6C1C5 → CozyPalette.outlineVariant
/// 只有这枚浅粉底在原版是内联字面量，保留同值常量。
const Color _kCandyPink = Color(0xFFFFF3F6);

class _CandyCoinsPageState extends State<CandyCoinsPage> {
  /// 原版 `var chartMode by remember { mutableStateOf(ChartMode.Bar) }`（:94）
  _ChartMode _chartMode = _ChartMode.bar;

  /// 原版 `var period by remember { mutableStateOf(ChartPeriod.Week) }`（:95）
  _ChartPeriod _period = _ChartPeriod.week;

  /// 原版 `var customAmount by remember { mutableStateOf("") }`（:97）
  /// 受控输入：`it.filter(Char::isDigit).take(4)`（:141）
  final TextEditingController _customAmountCtrl = TextEditingController();
  String _customAmount = '';

  @override
  void initState() {
    super.initState();
    // 保留本文件原有的取数调用
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshTransactions();
    });
  }

  @override
  void dispose() {
    _customAmountCtrl.dispose();
    super.dispose();
  }

  /// 原版 `pendingRecharge?.let { RechargeConfirmDialog(...) }`（:104-113）
  void _showRechargeConfirm(int amount) {
    showDialog<void>(
      context: context,
      builder: (BuildContext ctx) => _RechargeConfirmDialog(
        amount: amount,
        onDismiss: () => Navigator.of(ctx).pop(),
        onConfirm: () {
          Navigator.of(ctx).pop();
          _recharge(amount);
        },
      ),
    );
  }

  /// 原版 `viewModel.recharge(amount)`（CandyCoinsViewModel.kt:56-69）
  /// 动作仍走 `AppState.instance.recharge`，提示文案逐字照抄原版：
  ///   成功 → "已给吃货充值 $amount 枚糖糖币"
  ///   失败 → "充值失败，请确认已登录、已绑定，并已执行糖糖币数据库脚本"
  Future<void> _recharge(int amount) async {
    final bool ok = await AppState.instance.recharge(amount: amount);
    if (!mounted) return;
    final String message = ok
        ? '已给吃货充值 $amount 枚糖糖币'
        : '充值失败，请确认已登录、已绑定，并已执行糖糖币数据库脚本';
    await showDialog<void>(
      context: context,
      builder: (BuildContext ctx) => _CandyMessageDialog(
        message: message,
        onDismiss: () => Navigator.of(ctx).pop(),
      ),
    );
  }

  /// 原版 `onCustomAmountChange = { customAmount = it.filter(Char::isDigit).take(4) }`（:141）
  void _handleCustomAmountChange(String value) {
    final String digits = value.replaceAll(RegExp(r'[^0-9]'), '');
    final String next = digits.length > 4 ? digits.substring(0, 4) : digits;
    _customAmountCtrl.value = TextEditingValue(
      text: next,
      selection: TextSelection.collapsed(offset: next.length),
    );
    setState(() => _customAmount = next);
  }

  /// 原版 `onCustomRecharge = { customAmount.toIntOrNull()?.takeIf { it > 0 }?.let { pendingRecharge = it } }`（:143）
  void _handleCustomRecharge() {
    final int? amount = int.tryParse(_customAmount);
    if (amount == null || amount <= 0) return;
    _showRechargeConfirm(amount);
  }

  @override
  Widget build(BuildContext context) {
    final AppState state = AppState.instance;

    return ListenableBuilder(
      listenable: state,
      builder: (BuildContext context, Widget? _) {
        // 原版：`selectedRole == "caretaker"`（:93）
        final bool isCaretaker = state.isCaretaker;
        // 原版 `CandyCoinsUiState.walletBalance` 默认值 66（profiles.candy_coins 默认 66）
        final int balance = state.pair?.candyCoins ?? 66;
        // 原版 `visibleRecords`：饲养员只看 recharge，吃货看 recharge 以外的全部（:99-101）
        final List<CandyTransaction> visibleRecords = state.transactions
            .where((CandyTransaction r) =>
                isCaretaker ? r.type == 'recharge' : r.type != 'recharge')
            .toList(growable: false);
        // 原版 `visibleRecords.toChartPoints(period)`（:102）
        final List<_ChartPoint> chartPoints = _toChartPoints(visibleRecords, _period);

        // 原版 LazyColumn 的 item 顺序（:134-159）
        final List<Widget> cards = <Widget>[
          _CandyHeroCard(balance: balance, isCaretaker: isCaretaker),
          if (isCaretaker)
            _RechargePanel(
              customAmount: _customAmount,
              controller: _customAmountCtrl,
              onCustomAmountChange: _handleCustomAmountChange,
              onSelectAmount: _showRechargeConfirm,
              onCustomRecharge: _handleCustomRecharge,
            ),
          _ChartControlCard(
            chartMode: _chartMode,
            period: _period,
            onChartModeChange: (_ChartMode mode) => setState(() => _chartMode = mode),
            onPeriodChange: (_ChartPeriod period) => setState(() => _period = period),
            chartPoints: chartPoints,
            isCaretaker: isCaretaker,
          ),
          _LedgerListCard(records: visibleRecords, isCaretaker: isCaretaker),
        ];

        // 原版 Box(background(CandySurface)) + Column { CandyTopBar; LazyColumn(weight(1f)) }（:126-161）
        return CozyPage(
          child: Column(
            children: <Widget>[
              _CandyTopBar(onBack: () => Navigator.of(context).maybePop()),
              Expanded(
                child: ListView.separated(
                  // 原版 contentPadding = PaddingValues(start 20, top 16, end 20, bottom 104)（:131）
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, CozyDock.clearance),
                  // 原版 verticalArrangement = Arrangement.spacedBy(16.dp)（:132）
                  separatorBuilder: (BuildContext context, int index) =>
                      const SizedBox(height: 16),
                  itemCount: cards.length,
                  itemBuilder: (BuildContext context, int index) => cards[index],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

/// 原版 `private fun CandyTopBar(onBack: () -> Unit)`（CandyCoinsScreen.kt:166-185）
///
/// `Box(fillMaxWidth + statusBarsPadding + heightIn(min = 64.dp) + padding(h 12, v 8))`：
/// 返回键贴左、标题在**整条**顶栏里居中。这里用 Stack 复刻同样的居中基准
/// （`CozyMainTopBar` 是 72 高 + 滚动发丝线的**主页面**顶栏，几何与语义都不对应）。
class _CandyTopBar extends StatelessWidget {
  const _CandyTopBar({required this.onBack});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      // 原版 heightIn(min = 64.dp)：8 + 48 + 8
      height: 64,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Stack(
          alignment: Alignment.center,
          children: <Widget>[
            Align(
              alignment: Alignment.centerLeft,
              child: IconButton(
                onPressed: onBack,
                tooltip: '返回',
                iconSize: 24,
                icon: const Icon(Icons.arrow_back, color: CozyPalette.primary),
              ),
            ),
            Text(
              '糖糖币管理',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.headlineSmall!.copyWith(
                    color: CozyPalette.primary,
                    fontWeight: FontWeight.w900,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 原版 `private fun CandyHeroCard(balance: Int, isCaretaker: Boolean)`（:188-216）
///
/// 圆角 28 / 内边距 20 / 62 圆形粉底 + 56 糖币图 + 三条文案（间距 4）。
class _CandyHeroCard extends StatelessWidget {
  const _CandyHeroCard({required this.balance, required this.isCaretaker});

  final int balance;
  final bool isCaretaker;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(20),
      child: Row(
        spacing: 14,
        children: <Widget>[
          Container(
            width: 62,
            height: 62,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              // 原版 #FFF3F6 @ 0.80
              color: _kCandyPink.withValues(alpha: 0.80),
            ),
            child: const Center(child: _CandyCoinIcon(size: 56)),
          ),
          Expanded(
            child: Column(
              spacing: 4,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  isCaretaker ? '吃货糖糖币余额' : '我的糖糖币余额',
                  style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
                ),
                Text(
                  '$balance 枚',
                  // 原版 fontSize = 30.sp / lineHeight = 36.sp / FontWeight.Black
                  style: text.bodyLarge!.copyWith(
                    fontSize: 30,
                    height: 36 / 30,
                    fontWeight: FontWeight.w900,
                    color: CozyPalette.onSurface,
                  ),
                ),
                Text(
                  isCaretaker ? '充值后吃货才能继续快乐点菜' : '点菜会真实消耗糖糖币',
                  style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// 原版 `private fun RechargePanel(...)`（:219-260），仅饲养员可见。
///
/// 快捷档位逐字照抄：`listOf(10, 50, 100, 150).chunked(2)` —— 两行两列。
class _RechargePanel extends StatelessWidget {
  const _RechargePanel({
    required this.customAmount,
    required this.controller,
    required this.onCustomAmountChange,
    required this.onSelectAmount,
    required this.onCustomRecharge,
  });

  final String customAmount;
  final TextEditingController controller;
  final ValueChanged<String> onCustomAmountChange;
  final ValueChanged<int> onSelectAmount;
  final VoidCallback onCustomRecharge;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    // 原版 `enabled = customAmount.toIntOrNull()?.let { it > 0 } == true`（:251）
    final bool enabled = (int.tryParse(customAmount) ?? 0) > 0;

    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(18),
      child: Column(
        spacing: 12,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            '给吃货充值',
            style: text.titleMedium!.copyWith(
              fontWeight: FontWeight.w900,
              color: CozyPalette.onSurface,
            ),
          ),
          Row(
            spacing: 10,
            children: <Widget>[
              Expanded(
                child: _RechargeButton(amount: 10, onTap: () => onSelectAmount(10)),
              ),
              Expanded(
                child: _RechargeButton(amount: 50, onTap: () => onSelectAmount(50)),
              ),
            ],
          ),
          Row(
            spacing: 10,
            children: <Widget>[
              Expanded(
                child: _RechargeButton(amount: 100, onTap: () => onSelectAmount(100)),
              ),
              Expanded(
                child: _RechargeButton(amount: 150, onTap: () => onSelectAmount(150)),
              ),
            ],
          ),
          SizedBox(
            width: double.infinity,
            // 原版 OutlinedTextField(singleLine, Number, colors = cozyTextFieldColors())
            child: TextField(
              controller: controller,
              onChanged: onCustomAmountChange,
              keyboardType: TextInputType.number,
              maxLines: 1,
              decoration: cozyInputDecoration(hintText: '自定义金额'),
            ),
          ),
          SizedBox(
            width: double.infinity,
            // 原版 Modifier.fillMaxWidth().height(50.dp)
            height: 50,
            child: FilledButton(
              onPressed: enabled ? onCustomRecharge : null,
              style: FilledButton.styleFrom(
                // 原版 ButtonDefaults.buttonColors(containerColor = CozyRose)，onPrimary = #FFFFFF
                backgroundColor: CozyPalette.primary,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
                // 原版 RoundedCornerShape(999.dp)
              ),
              child: const Text(
                '确认自定义金额',
                style: TextStyle(fontWeight: FontWeight.w900),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 原版 `private fun RechargeButton(amount: Int, modifier: Modifier, onClick: () -> Unit)`（:263-284）
///
/// 高 58 / 圆角 18 / 底色 #FFF3F6 / 描边 1dp CandyLine / 按下缩到 0.97。
class _RechargeButton extends StatefulWidget {
  const _RechargeButton({required this.amount, required this.onTap});

  final int amount;
  final VoidCallback onTap;

  @override
  State<_RechargeButton> createState() => _RechargeButtonState();
}

class _RechargeButtonState extends State<_RechargeButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: (_) => setState(() => _pressed = true),
      onTapUp: (_) => setState(() => _pressed = false),
      onTapCancel: () => setState(() => _pressed = false),
      onTap: widget.onTap,
      child: AnimatedScale(
        scale: _pressed ? 0.97 : 1.0,
        duration: const Duration(milliseconds: 120),
        curve: Curves.easeOut,
        child: Container(
          height: 58,
          decoration: BoxDecoration(
            color: _kCandyPink,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: CozyPalette.outlineVariant, width: 1),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text(
                '+${widget.amount}',
                // 原版 fontSize = 20.sp / FontWeight.Black / CozyRose
                style: text.bodyLarge!.copyWith(
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                  color: CozyPalette.primary,
                ),
              ),
              Row(
                mainAxisSize: MainAxisSize.min,
                spacing: 4,
                children: <Widget>[
                  const _CandyCoinIcon(size: 18),
                  Text(
                    '糖糖币',
                    style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 原版 `private fun ChartControlCard(...)`（:287-319）
///
/// 图标 SsidChart 22 + 标题 + 两组分段（柱状图/折线图、按周/按月）+ 168 高图表。
class _ChartControlCard extends StatelessWidget {
  const _ChartControlCard({
    required this.chartMode,
    required this.period,
    required this.onChartModeChange,
    required this.onPeriodChange,
    required this.chartPoints,
    required this.isCaretaker,
  });

  final _ChartMode chartMode;
  final _ChartPeriod period;
  final ValueChanged<_ChartMode> onChartModeChange;
  final ValueChanged<_ChartPeriod> onPeriodChange;
  final List<_ChartPoint> chartPoints;
  final bool isCaretaker;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(18),
      child: Column(
        spacing: 14,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            spacing: 8,
            children: <Widget>[
              const Icon(Icons.ssid_chart, size: 22, color: CozyPalette.primary),
              Text(
                isCaretaker ? '充值趋势' : '消耗趋势',
                style: text.titleMedium!.copyWith(
                  fontWeight: FontWeight.w900,
                  color: CozyPalette.onSurface,
                ),
              ),
            ],
          ),
          _SegmentedRow(
            labels: _ChartMode.values.map((_ChartMode e) => e.label).toList(growable: false),
            selectedIndex: chartMode.index,
            onSelected: (int index) => onChartModeChange(_ChartMode.values[index]),
          ),
          _SegmentedRow(
            labels: _ChartPeriod.values.map((_ChartPeriod e) => e.label).toList(growable: false),
            selectedIndex: period.index,
            onSelected: (int index) => onPeriodChange(_ChartPeriod.values[index]),
          ),
          SizedBox(
            width: double.infinity,
            // 原版 Modifier.fillMaxWidth().height(168.dp)
            height: 168,
            child: CustomPaint(
              painter: _CandyChartPainter(points: chartPoints, mode: chartMode),
            ),
          ),
        ],
      ),
    );
  }
}

/// 原版 `private fun SegmentedRow(labels, selectedIndex, onSelected)`（:322-343）
///
/// 每片等宽（weight 1f）、间距 8、全圆角、选中 CozyRose，文字上下留白 9。
class _SegmentedRow extends StatelessWidget {
  const _SegmentedRow({
    required this.labels,
    required this.selectedIndex,
    required this.onSelected,
  });

  final List<String> labels;
  final int selectedIndex;
  final ValueChanged<int> onSelected;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return Row(
      spacing: 8,
      children: <Widget>[
        for (int index = 0; index < labels.length; index++)
          Expanded(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => onSelected(index),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 9),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(999),
                  color: selectedIndex == index
                      ? CozyPalette.primary
                      : Colors.white.withValues(alpha: 0.70),
                  border: Border.all(
                    color: selectedIndex == index
                        ? CozyPalette.primary
                        : CozyPalette.outlineVariant,
                    width: 1,
                  ),
                ),
                child: Text(
                  labels[index],
                  textAlign: TextAlign.center,
                  style: text.bodyLarge!.copyWith(
                    fontWeight: FontWeight.w900,
                    color: selectedIndex == index
                        ? CozyPalette.surface
                        : CozyPalette.onSurface,
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}

/// 原版 `private fun CandyChart(points, mode)`（:346-374）的 Canvas 绘制
///
/// 柱状图：CozyRose @ 0.72，宽 = slot * 0.40，左偏移 slot * 0.20
/// 折线图：CozyRose 3dp 圆头折线
/// 两种模式都画 4dp 的 #894C5C 数据点
class _CandyChartPainter extends CustomPainter {
  const _CandyChartPainter({required this.points, required this.mode});

  final List<_ChartPoint> points;
  final _ChartMode mode;

  @override
  void paint(Canvas canvas, Size size) {
    // 原版 `max(points.maxOfOrNull { it.value } ?: 0, 1)`
    final int maxValue = math.max(
      points.fold<int>(0, (int acc, _ChartPoint p) => math.max(acc, p.value)),
      1,
    );
    final double chartHeight = size.height - 32; // size.height - 32.dp.toPx()
    final double bottom = size.height - 18; // size.height - 18.dp.toPx()
    final double slot = size.width / math.max(points.length, 1);

    double yOf(_ChartPoint point) => bottom - (point.value / maxValue) * chartHeight;

    for (int index = 0; index < points.length; index++) {
      final double x = slot * index + slot / 2;
      final double y = yOf(points[index]);
      if (mode == _ChartMode.bar) {
        canvas.drawRect(
          Rect.fromLTWH(x - slot * 0.20, y, slot * 0.40, bottom - y),
          Paint()..color = CozyPalette.primary.withValues(alpha: 0.72),
        );
      }
      canvas.drawCircle(Offset(x, y), 4, Paint()..color = CozyPalette.primary);
    }

    if (mode == _ChartMode.line && points.isNotEmpty) {
      final Path path = Path();
      for (int index = 0; index < points.length; index++) {
        final double x = slot * index + slot / 2;
        final double y = yOf(points[index]);
        if (index == 0) {
          path.moveTo(x, y);
        } else {
          path.lineTo(x, y);
        }
      }
      canvas.drawPath(
        path,
        Paint()
          ..color = CozyPalette.primary
          ..style = PaintingStyle.stroke
          ..strokeWidth = 3
          ..strokeCap = StrokeCap.round,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _CandyChartPainter oldDelegate) =>
      oldDelegate.mode != mode || !listEquals(oldDelegate.points, points);
}

/// 原版 `private fun LedgerListCard(records, isCaretaker)`（:377-401）
///
/// 标题「充值明细 / 消耗明细」+ 最新 12 条流水；空态「还没有糖糖币记录」。
class _LedgerListCard extends StatelessWidget {
  const _LedgerListCard({required this.records, required this.isCaretaker});

  final List<CandyTransaction> records;
  final bool isCaretaker;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    final List<Widget> rows = records.isEmpty
        ? <Widget>[
            Text(
              '还没有糖糖币记录',
              style: text.bodyLarge!.copyWith(color: CozyPalette.onSurfaceVariant),
            ),
          ]
        // 原版 `records.take(12).forEach { ... }`
        : records
            .take(12)
            .map<Widget>((CandyTransaction record) => _LedgerRow(record: record))
            .toList(growable: false);

    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(18),
      child: Column(
        spacing: 12,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            isCaretaker ? '充值明细' : '消耗明细',
            style: text.titleMedium!.copyWith(
              fontWeight: FontWeight.w900,
              color: CozyPalette.onSurface,
            ),
          ),
          ...rows,
        ],
      ),
    );
  }
}

/// 原版 `LedgerListCard` 内联的单行流水（:390-396）
///
/// 左列备注（单行省略）+ yyyy-MM-dd HH:mm:ss；右侧带符号金额，
/// 收入（>= 0）用 CozyRose，支出（< 0）用 CozyTerracotta。
class _LedgerRow extends StatelessWidget {
  const _LedgerRow({required this.record});

  final CandyTransaction record;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    final bool income = record.amount >= 0;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(
                record.description,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: text.bodyLarge!.copyWith(
                  fontWeight: FontWeight.w600,
                  color: CozyPalette.onSurface,
                ),
              ),
              Text(
                _friendlySecondTime(record.createdAt),
                style: text.bodySmall!.copyWith(color: CozyPalette.onSurfaceVariant),
              ),
            ],
          ),
        ),
        Text(
          _signedText(record.amount),
          style: text.bodyLarge!.copyWith(
            fontWeight: FontWeight.w900,
            color: income ? CozyPalette.primary : CozyPalette.tertiary,
          ),
        ),
      ],
    );
  }
}

/// 原版 `private fun RechargeConfirmDialog(amount, onDismiss, onConfirm)`（:404-418）
///
/// AlertDialog 圆角 28 / 底色 CandyCard / 标题与正文居中 / 主按钮全圆角 CozyRose。
class _RechargeConfirmDialog extends StatelessWidget {
  const _RechargeConfirmDialog({
    required this.amount,
    required this.onDismiss,
    required this.onConfirm,
  });

  final int amount;
  final VoidCallback onDismiss;
  final VoidCallback onConfirm;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      title: Text(
        '确认充值',
        textAlign: TextAlign.center,
        style: text.headlineSmall!.copyWith(
          fontWeight: FontWeight.w900,
          color: CozyPalette.onSurface,
        ),
      ),
      content: Text(
        '确定给吃货充值 $amount 枚糖糖币吗？',
        textAlign: TextAlign.center,
        style: text.bodyMedium!.copyWith(color: CozyPalette.onSurfaceVariant),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: onDismiss,
          style: TextButton.styleFrom(foregroundColor: CozyPalette.onSurfaceVariant),
          child: const Text('再想想'),
        ),
        FilledButton(
          onPressed: onConfirm,
          style: FilledButton.styleFrom(
            backgroundColor: CozyPalette.primary,
            foregroundColor: Colors.white,
            shape: const StadiumBorder(),
          ),
          child: const Text('确认充值', style: TextStyle(fontWeight: FontWeight.w900)),
        ),
      ],
    );
  }
}

/// 原版 `uiState.message?.let { AlertDialog(...) }`（:115-124）
///
/// 圆角 26 / 底色 CandyCard / 标题「糖糖币提醒」/ 确认「知道了」用 CozyRose。
class _CandyMessageDialog extends StatelessWidget {
  const _CandyMessageDialog({required this.message, required this.onDismiss});

  final String message;
  final VoidCallback onDismiss;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;
    return AlertDialog(
      backgroundColor: CozyPalette.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26)),
      title: Text(
        '糖糖币提醒',
        style: text.headlineSmall!.copyWith(
          fontWeight: FontWeight.w900,
          color: CozyPalette.onSurface,
        ),
      ),
      content: Text(
        message,
        style: text.bodyMedium!.copyWith(color: CozyPalette.onSurfaceVariant),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: onDismiss,
          style: TextButton.styleFrom(foregroundColor: CozyPalette.primary),
          child: const Text('知道了'),
        ),
      ],
    );
  }
}

/// 原生 `CandyCoinIcon`（`ui/components/CandyCoinIcon.kt`）画的是 `R.drawable.candy_coin` 位图，
/// 已按原图打包为 `assets/images/candy_coin.png`（原 drawable-nodpi 的 473KB PNG）。
class _CandyCoinIcon extends StatelessWidget {
  const _CandyCoinIcon({this.size = 56});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      'assets/images/candy_coin.png',
      width: size,
      height: size,
      fit: BoxFit.contain,
      errorBuilder: (_, _, _) => SizedBox(
        width: size,
        height: size,
        child: FittedBox(
          fit: BoxFit.contain,
          child: Text(
            '🍬',
            style: Theme.of(context).textTheme.bodyLarge!.copyWith(fontSize: size),
          ),
        ),
      ),
    );
  }
}

/// 原版 `private enum class ChartMode(val label: String) { Bar("柱状图"), Line("折线图") }`（:420）
enum _ChartMode {
  bar('柱状图'),
  line('折线图');

  const _ChartMode(this.label);

  final String label;
}

/// 原版 `private enum class ChartPeriod(val label: String) { Week("按周"), Month("按月") }`（:422）
enum _ChartPeriod {
  week('按周'),
  month('按月');

  const _ChartPeriod(this.label);

  final String label;
}

/// 原版 `private data class ChartPoint(val label: String, val value: Int)`（:424）
class _ChartPoint {
  const _ChartPoint(this.label, this.value);

  final String label;
  final int value;

  @override
  bool operator ==(Object other) =>
      other is _ChartPoint && other.label == label && other.value == value;

  @override
  int get hashCode => Object.hash(label, value);
}

/// 原版 `List<CandyCoinRecord>.toChartPoints(period)`（:426-437）
///
/// 按周取最近 7 天、按月取最近 30 天，每天汇总 `abs(amount)`。
List<_ChartPoint> _toChartPoints(List<CandyTransaction> records, _ChartPeriod period) {
  final DateTime now = DateTime.now();
  final int span = period == _ChartPeriod.week ? 7 : 30;
  final List<_ChartPoint> points = <_ChartPoint>[];
  for (int offset = span - 1; offset >= 0; offset--) {
    final DateTime date = DateTime(now.year, now.month, now.day - offset);
    final int total = records
        .where((CandyTransaction r) {
          final DateTime local = r.createdAt.toLocal();
          return local.year == date.year &&
              local.month == date.month &&
              local.day == date.day;
        })
        .fold<int>(0, (int sum, CandyTransaction r) => sum + r.amount.abs());
    points.add(_ChartPoint('${date.month}/${date.day}', total));
  }
  return points;
}

/// 原版 `String.toFriendlySecondTime()`（:443-446）：yyyy-MM-dd HH:mm:ss
String _friendlySecondTime(DateTime value) {
  final DateTime t = value.toLocal();
  String two(int v) => v.toString().padLeft(2, '0');
  return '${t.year}-${two(t.month)}-${two(t.day)} '
      '${two(t.hour)}:${two(t.minute)}:${two(t.second)}';
}

/// 原版 `Int.toSignedText()`（:448）
String _signedText(int value) => value >= 0 ? '+$value' : '$value';
