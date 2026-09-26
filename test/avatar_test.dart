import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:orderdisk_flutter/ui/theme/cozy_glass.dart';

/// 头像三种来源的解码契约。
///
/// 背景：这个 Supabase 项目没有任何 storage 桶（`GET /storage/v1/bucket` → `[]`，
/// 建桶要 service_role），所以本地选的头像以 `data:image/jpeg;base64,…` 存进
/// `profiles.avatar_url`。伴侣侧读回来的 `partner_avatar_url` 也是这个形式，
/// 因此 `Image.network` 不够用，必须按前缀分流——这里把分流规则钉死。
void main() {
  // 1 字节的假 JPEG 负载足够验证「编码 → 解码」这条链路
  final Uint8List payload = Uint8List.fromList(<int>[0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10]);
  final String dataUri = 'data:image/jpeg;base64,${base64Encode(payload)}';

  test('data URI 能被识别并解回原始字节', () {
    expect(CozyAvatar.isDataUri(dataUri), isTrue);
    final Uint8List? decoded = CozyAvatar.decodeDataUri(dataUri);
    expect(decoded, isNotNull);
    expect(decoded, equals(payload));
  });

  test('http(s) 地址不走 data URI 分支（交给 Image.network）', () {
    expect(CozyAvatar.isDataUri('https://i2.chuimg.com/a.jpg'), isFalse);
    expect(CozyAvatar.decodeDataUri('https://i2.chuimg.com/a.jpg'), isNull);
  });

  test('空串与坏 data URI 都安全落回 null（调用方显示兜底图标）', () {
    expect(CozyAvatar.decodeDataUri(''), isNull);
    expect(CozyAvatar.decodeDataUri('data:image/jpeg;base64'), isNull);
    expect(CozyAvatar.decodeDataUri('data:image/jpeg;base64,@@@@不是 base64@@@@'), isNull);
  });

  testWidgets('三种来源分别渲染成 Image / 兜底', (WidgetTester tester) async {
    const Key fallbackKey = Key('fallback');
    Widget subject(String url) => MaterialApp(
          home: CozyAvatar(
            url: url,
            size: 40,
            fallback: const SizedBox(key: fallbackKey, width: 40, height: 40),
          ),
        );

    // ① data URI → Image.memory
    await tester.pumpWidget(subject(dataUri));
    expect(find.byType(Image), findsOneWidget);
    expect(find.byKey(fallbackKey), findsNothing);

    // ② 空串 → 兜底
    await tester.pumpWidget(subject(''));
    expect(find.byType(Image), findsNothing);
    expect(find.byKey(fallbackKey), findsOneWidget);
  });
}
