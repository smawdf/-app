import 'package:flutter/material.dart';

import '../../data/app_state.dart';
import '../../data/models.dart';
import '../theme/cozy_glass.dart';
import '../widgets/cozy_count_up.dart';

/// 糖糖币中心页 —— 1:1 对齐 demo/index.html 中的 `#subpage-candy-coins`
///
/// 结构分区：
/// 1. 顶部返回栏：标题「糖糖币中心」
/// 2. 糖币钱包主卡片（Balance Card）：金橙粉渐变 + 真实余额 + 累计消耗与撒糖
/// 3. 专属撒糖通道（Quick Recharge）：4 档快捷投喂 + 自定义金额输入
/// 4. 糖糖币账本明细卡片（Transaction History Ledger）：真实流水列表 + 温和空态
class CandyCoinsPage extends StatefulWidget {
  const CandyCoinsPage({super.key});

  @override
  State<CandyCoinsPage> createState() => _CandyCoinsPageState();
}

class _CandyCoinsPageState extends State<CandyCoinsPage> {
  final TextEditingController _customAmountCtrl = TextEditingController();
  String _customAmount = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      AppState.instance.refreshTransactions();
    });
  }

  @override
  void dispose() {
    _customAmountCtrl.dispose();
    super.dispose();
  }

  /// 充值确认对话框
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

  /// 执行真实充值
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

  void _handleCustomAmountChange(String value) {
    final String digits = value.replaceAll(RegExp(r'[^0-9]'), '');
    final String next = digits.length > 4 ? digits.substring(0, 4) : digits;
    _customAmountCtrl.value = TextEditingValue(
      text: next,
      selection: TextSelection.collapsed(offset: next.length),
    );
    setState(() => _customAmount = next);
  }

  void _handleCustomRecharge() {
    final int? amount = int.tryParse(_customAmount);
    if (amount == null || amount <= 0) return;
    _showRechargeConfirm(amount);
  }

  @override
  Widget build(BuildContext context) {
    final AppState state = AppState.instance;

    return ListenableBuilder(
      listenable: state.listenFor(const {Domain.candy, Domain.profile}),
      builder: (BuildContext context, Widget? _) {
        final int balance = state.candyBalance;
        final List<CandyTransaction> transactions = state.transactions;

        final List<Widget> cards = <Widget>[
          _CandyBalanceCard(
            balance: balance,
            transactions: transactions,
          ),
          _QuickRechargeCard(
            customAmount: _customAmount,
            controller: _customAmountCtrl,
            onCustomAmountChange: _handleCustomAmountChange,
            onSelectAmount: _showRechargeConfirm,
            onCustomRecharge: _handleCustomRecharge,
          ),
          _LedgerListCard(records: transactions),
        ];

        return CozyPage(
          child: Column(
            children: <Widget>[
              _CandyTopBar(onBack: () => Navigator.of(context).maybePop()),
              Expanded(
                child: ListView.separated(
                  padding: EdgeInsets.fromLTRB(16, 12, 16, CozyDock.clearanceOf(context)),
                  separatorBuilder: (BuildContext context, int index) =>
                      const SizedBox(height: 14),
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

/// 顶部返回栏：标题「糖糖币中心」
class _CandyTopBar extends StatelessWidget {
  const _CandyTopBar({required this.onBack});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
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
              '糖糖币中心',
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

/// 糖币钱包主卡片（Balance Card）
///
/// 暖金/暖橙粉渐变背景 + 大投影 + 胶囊标签 + 主余额 + 累计消耗与撒糖双列统计
class _CandyBalanceCard extends StatelessWidget {
  const _CandyBalanceCard({
    required this.balance,
    required this.transactions,
  });

  final int balance;
  final List<CandyTransaction> transactions;

  @override
  Widget build(BuildContext context) {
    // 从真实 transactions 中统计点餐累计消耗与饲养员累计撒糖
    final int spentCost = transactions
        .where((CandyTransaction t) => t.amount < 0)
        .fold<int>(0, (int sum, CandyTransaction t) => sum + t.amount.abs());
    final int rechargedTotal = transactions
        .where((CandyTransaction t) => t.amount > 0)
        .fold<int>(0, (int sum, CandyTransaction t) => sum + t.amount);

    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(28),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[
            Color(0xFFFBBF24), // amber-400
            Color(0xFFF59E0B), // amber-500
            Color(0xFFFB7185), // rose-400
          ],
        ),
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: const Color(0xFFF59E0B).withValues(alpha: 0.35),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: Stack(
          children: <Widget>[
            // 背景右下角水印装饰
            const Positioned(
              right: -12,
              bottom: -16,
              child: IgnorePointer(
                child: Opacity(
                  opacity: 0.18,
                  child: _CandyCoinIcon(size: 110),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  // 胶囊标签
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: 0.10),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: const Text(
                      'CANDY COINS WALLET',
                      style: TextStyle(
                        fontSize: 10,
                        letterSpacing: 1.2,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFFFEF3C7), // amber-100
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  // 主数值
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.baseline,
                    textBaseline: TextBaseline.alphabetic,
                    children: <Widget>[
                      CozyCountUp(
                        value: balance,
                        style: const TextStyle(
                          fontSize: 38,
                          height: 1.1,
                          fontWeight: FontWeight.w900,
                          color: Colors.white,
                          letterSpacing: -1,
                        ),
                      ),
                      const SizedBox(width: 6),
                      const Text(
                        '糖糖币',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFFFEF3C7),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  // 副标题
                  const Text(
                    '双人小饭桌专属流通货币 · 甜甜蜜蜜每一餐',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: Color(0xE6FEF3C7),
                    ),
                  ),
                  const SizedBox(height: 16),
                  // 底部双列统计
                  Container(
                    padding: const EdgeInsets.only(top: 12),
                    decoration: BoxDecoration(
                      border: Border(
                        top: BorderSide(
                          color: Colors.white.withValues(alpha: 0.20),
                          width: 1,
                        ),
                      ),
                    ),
                    child: Row(
                      children: <Widget>[
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              const Text(
                                '累计点餐消耗',
                                style: TextStyle(
                                  fontSize: 11,
                                  color: Color(0xFFFEF3C7),
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                '🍬 $spentCost 币',
                                style: const TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              const Text(
                                '饲养员累计撒糖',
                                style: TextStyle(
                                  fontSize: 11,
                                  color: Color(0xFFFEF3C7),
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                '🍬 $rechargedTotal 币',
                                style: const TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.bold,
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
          ],
        ),
      ),
    );
  }
}

/// 专属撒糖通道（Quick Recharge）
///
/// 4 档快捷网格按钮 + 自定义充值金额输入
class _QuickRechargeCard extends StatelessWidget {
  const _QuickRechargeCard({
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
    final bool customEnabled = (int.tryParse(customAmount) ?? 0) > 0;

    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          // 标题行与特权胶囊
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              const Text(
                '投喂对方 · 专属撒糖通道',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w900,
                  color: CozyPalette.onSurface,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFFBEB), // amber-50
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(color: const Color(0xFFFDE68A), width: 1), // amber-200
                ),
                child: const Text(
                  '饲养员特权',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFB45309), // amber-700
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          // 提示文案
          const Text(
            '点菜前给吃货补充能量，点选金额立即入账：',
            style: TextStyle(
              fontSize: 11,
              color: CozyPalette.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 12),
          // 2x2 网格按钮
          Row(
            children: <Widget>[
              Expanded(
                child: _QuickRechargeButton(
                  amount: 10,
                  subtitle: '随手投喂小点心',
                  amountColor: const Color(0xFFE11D48), // rose-600
                  onTap: () => onSelectAmount(10),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _QuickRechargeButton(
                  amount: 20,
                  subtitle: '今晚加一份硬菜',
                  amountColor: const Color(0xFFE11D48), // rose-600
                  onTap: () => onSelectAmount(20),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: <Widget>[
              Expanded(
                child: _QuickRechargeButton(
                  amount: 50,
                  subtitle: '周末火锅/大餐基金',
                  amountColor: const Color(0xFFD97706), // amber-600
                  onTap: () => onSelectAmount(50),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _QuickRechargeButton(
                  amount: 100,
                  subtitle: '承包你整周的胃',
                  amountColor: const Color(0xFFD97706), // amber-600
                  onTap: () => onSelectAmount(100),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          // 自定义金额输入与确认充值
          Row(
            children: <Widget>[
              Expanded(
                child: SizedBox(
                  height: 44,
                  child: TextField(
                    controller: controller,
                    onChanged: onCustomAmountChange,
                    keyboardType: TextInputType.number,
                    maxLines: 1,
                    decoration: cozyInputDecoration(hintText: '自定义金额 (枚)'),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              SizedBox(
                height: 44,
                child: FilledButton(
                  onPressed: customEnabled ? onCustomRecharge : null,
                  style: FilledButton.styleFrom(
                    backgroundColor: CozyPalette.primary,
                    foregroundColor: Colors.white,
                    shape: const StadiumBorder(),
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                  ),
                  child: const Text(
                    '确认充值',
                    style: TextStyle(fontWeight: FontWeight.w900),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// 快捷撒糖按钮，带 active 按压触感
class _QuickRechargeButton extends StatefulWidget {
  const _QuickRechargeButton({
    required this.amount,
    required this.subtitle,
    required this.amountColor,
    required this.onTap,
  });

  final int amount;
  final String subtitle;
  final Color amountColor;
  final VoidCallback onTap;

  @override
  State<_QuickRechargeButton> createState() => _QuickRechargeButtonState();
}

class _QuickRechargeButtonState extends State<_QuickRechargeButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: (_) => setState(() => _pressed = true),
      onTapUp: (_) => setState(() => _pressed = false),
      onTapCancel: () => setState(() => _pressed = false),
      onTap: widget.onTap,
      child: AnimatedScale(
        scale: _pressed ? 0.95 : 1.0,
        duration: const Duration(milliseconds: 100),
        curve: Curves.easeOut,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 8),
          decoration: BoxDecoration(
            color: CozyPalette.surfaceVariant.withValues(alpha: 0.35),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: CozyPalette.outlineVariant.withValues(alpha: 0.7),
              width: 1,
            ),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text(
                '+${widget.amount} 币',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w900,
                  color: widget.amountColor,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                widget.subtitle,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 10,
                  fontWeight: FontWeight.w500,
                  color: CozyPalette.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 糖糖币账本明细卡片（Transaction History Ledger）
///
/// 真实流水展示；若为空，保留骨架卡片并居中展示温和引导文案
class _LedgerListCard extends StatelessWidget {
  const _LedgerListCard({required this.records});

  final List<CandyTransaction> records;

  @override
  Widget build(BuildContext context) {
    final TextTheme text = Theme.of(context).textTheme;

    return CozyCard(
      radius: 28,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          const Text(
            '糖糖币账本明细',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w900,
              color: CozyPalette.onSurface,
            ),
          ),
          const SizedBox(height: 12),
          if (records.isEmpty)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 36, horizontal: 16),
              alignment: Alignment.center,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  const Text('🍬', style: TextStyle(fontSize: 28)),
                  const SizedBox(height: 8),
                  Text(
                    '暂无糖糖币明细，开始点餐或撒糖投喂吧~',
                    textAlign: TextAlign.center,
                    style: text.bodySmall!.copyWith(
                      fontSize: 12,
                      color: CozyPalette.onSurfaceVariant,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            )
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: records.length,
              separatorBuilder: (BuildContext context, int index) => Divider(
                height: 18,
                thickness: 1,
                color: CozyPalette.outlineVariant.withValues(alpha: 0.35),
              ),
              itemBuilder: (BuildContext context, int index) {
                final CandyTransaction record = records[index];
                final bool isPositive = record.amount >= 0;
                final String title = record.description.trim().isNotEmpty
                    ? record.description.trim()
                    : record.typeLabel;
                final String amountText = isPositive ? '+${record.amount} 币' : '${record.amount} 币';
                final Color amountColor = isPositive
                    ? const Color(0xFF059669) // emerald-600
                    : const Color(0xFFE11D48); // rose-600

                return Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: CozyPalette.onSurface,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            _friendlyLedgerTime(record.createdAt),
                            style: const TextStyle(
                              fontSize: 11,
                              color: CozyPalette.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      amountText,
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w900,
                        color: amountColor,
                      ),
                    ),
                  ],
                );
              },
            ),
        ],
      ),
    );
  }
}

/// 友好时间转换
String _friendlyLedgerTime(DateTime value) {
  final DateTime now = DateTime.now();
  final DateTime local = value.toLocal();
  final DateTime todayStart = DateTime(now.year, now.month, now.day);
  final DateTime yesterdayStart = todayStart.subtract(const Duration(days: 1));
  String two(int v) => v.toString().padLeft(2, '0');
  final String timeStr = '${two(local.hour)}:${two(local.minute)}';

  if (local.isAfter(todayStart)) {
    return '今天 $timeStr';
  } else if (local.isAfter(yesterdayStart)) {
    return '昨天 $timeStr';
  } else if (local.year == now.year) {
    return '${local.month}月${local.day}日 $timeStr';
  } else {
    return '${local.year}-${two(local.month)}-${two(local.day)} $timeStr';
  }
}

/// 充值确认弹窗
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

/// 提示弹窗
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

/// 糖币图标
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
