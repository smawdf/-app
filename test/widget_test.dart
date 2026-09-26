// 高糖小食 Flutter 端冒烟测试
// 说明：完整业务链路需要真实后端，这里只验证 App 能正常构建出登录页。

import 'package:flutter_test/flutter_test.dart';

import 'package:orderdisk_flutter/main.dart';

void main() {
  testWidgets('App 能正常启动并渲染登录页', (WidgetTester tester) async {
    await tester.pumpWidget(const OrderDiskApp());
    await tester.pump();

    // 登录页标题与主按钮（改版后的实际文案）
    expect(find.text('欢迎回来'), findsOneWidget);
    expect(find.text('今天也一起好好吃饭吧'), findsOneWidget);
    expect(find.text('去注册'), findsOneWidget);
  });
}