import 'dart:async';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:orderdisk_flutter/data/order_errors.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

void main() {
  group('friendlyErrorText —— 服务端拒绝要露出真因', () {
    test('糖币不足：翻成人话，而不是兜底网络文案', () {
      expect(
        friendlyErrorText(
          const PostgrestException(
            message: 'insufficient candy coins',
            code: 'P0001',
            details: null,
            hint: null,
          ),
        ),
        '糖糖币不够啦，找饲养员撒点糖再点菜',
      );
    });

    test('非吃货下单：翻成角色提示', () {
      expect(
        friendlyErrorText(const PostgrestException(message: 'eater role required')),
        '只有吃货可以提交点菜，饲养员负责管理小店和接单',
      );
    });

    test('重复提交：翻成「处理中」', () {
      expect(
        friendlyErrorText(const PostgrestException(message: 'record id conflict')),
        '这笔点菜正在处理中，请稍后重试',
      );
    });

    test('金额不合法：翻成重新选菜', () {
      expect(
        friendlyErrorText(const PostgrestException(message: 'amount must be positive')),
        '这单不需要消耗糖糖币，请重新选菜',
      );
    });

    test('未知的服务端错误：带上原话，方便定位', () {
      expect(
        friendlyErrorText(
          const PostgrestException(
            message: 'new row violates row-level security policy for table "menu_dishes"',
            code: '42501',
            details: 'some detail',
          ),
        ),
        '请求被服务器拒绝：new row violates row-level security policy for table "menu_dishes" some detail',
      );
    });

    test('服务端错误没有内容时仍给兜底文案', () {
      expect(
        friendlyErrorText(const PostgrestException(message: '')),
        '请求失败，请检查网络或稍后重试',
      );
    });
  });

  group('friendlyErrorText —— 网络异常', () {
    test('SocketException 给网络提示', () {
      expect(
        friendlyErrorText(const SocketException('Connection reset by peer')),
        '网络连接失败，请检查网络后重试。',
      );
    });

    test('TimeoutException 给网络提示', () {
      expect(friendlyErrorText(TimeoutException('timeout')), '网络连接失败，请检查网络后重试。');
    });

    test('字符串里带 SocketException 的 ClientException 也给网络提示', () {
      expect(
        friendlyErrorText(
          Exception('ClientException with SocketException: Connection reset by peer'),
        ),
        '网络连接失败，请检查网络后重试。',
      );
    });

    test('其它异常落兜底文案', () {
      expect(friendlyErrorText(Exception('boom')), '请求失败，请检查网络或稍后重试');
    });
  });

  group('orderSubmitBlockedReason —— 提交点菜的前置校验', () {
    test('饲养员不能下单', () {
      expect(
        orderSubmitBlockedReason(isCaretaker: true, candyBalance: 66, candyCost: 2),
        '只有吃货可以提交点菜，饲养员负责管理小店和接单',
      );
    });

    test('糖币不足先拦下，不把请求打到 RPC', () {
      expect(
        orderSubmitBlockedReason(isCaretaker: false, candyBalance: 5, candyCost: 16),
        '糖糖币不够啦，找饲养员撒点糖再点菜',
      );
    });

    test('刚好够可以提交', () {
      expect(
        orderSubmitBlockedReason(isCaretaker: false, candyBalance: 16, candyCost: 16),
        isNull,
      );
    });

    test('糖币富余可以提交', () {
      expect(
        orderSubmitBlockedReason(isCaretaker: false, candyBalance: 66, candyCost: 2),
        isNull,
      );
    });
  });
}
