// 高糖小食 Flutter 端冒烟测试
// 说明：完整业务链路需要真实后端，这里只验证 App 能正常构建出登录页。

import 'package:flutter_test/flutter_test.dart';

import 'package:orderdisk_flutter/main.dart';

void main() {
  testWidgets('App 能正常启动并渲染登录页', (WidgetTester tester) async {
    await tester.pumpWidget(const OrderDiskApp());
    await tester.pump();

    // 登录页标题
    expect(find.text('高糖小食'), findsOneWidget);
    // 演示账号快捷入口
    expect(find.text('吃货 小马'), findsOneWidget);
  });
}
