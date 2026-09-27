import 'dart:async';
import 'dart:io';

import 'package:supabase_flutter/supabase_flutter.dart';

/// 把底层网络/服务端异常翻译成与原生一致的文案。
///
/// 这里以前直接把 `e.toString()` 交给界面，于是注册/登录一旦遇到瞬时网络抖动，
/// 用户看到的是整段
/// `AuthRetryableFetchException(message: ClientException with SocketException:
///  Connection reset by peer (OS Error: Connection reset by peer, errno = 104) ...)`。
/// 原生 `AuthViewModel.kt:164` / `OnboardingViewModel.kt:183` 都只给一句人话。
///
/// 【真机修正】服务端 `PostgrestException` 以前一律落到最后的兜底文案
/// 「请求失败，请检查网络或稍后重试」，于是真机上「提交点菜」被 RPC 拒绝
/// （`insufficient candy coins`）时只看到一句网络提示，根本查不出原因。
/// 这里把已知的服务端原话翻成人话，未知的也把原话带出来。
String friendlyErrorText(Object e) {
  if (e is SocketException || e is HandshakeException || e is TimeoutException) {
    return '网络连接失败，请检查网络后重试。';
  }
  if (e is PostgrestException) {
    final String server = '${e.message} ${e.details ?? ''}'.trim();
    if (server.contains('insufficient candy coins')) {
      return '糖糖币不够啦，找饲养员撒点糖再点菜';
    }
    if (server.contains('eater role required')) {
      return '只有吃货可以提交点菜，饲养员负责管理小店和接单';
    }
    if (server.contains('record id conflict')) {
      return '这笔点菜正在处理中，请稍后重试';
    }
    if (server.contains('amount must be positive')) {
      return '这单不需要消耗糖糖币，请重新选菜';
    }
    if (server.isNotEmpty) return '请求被服务器拒绝：$server';
    return '请求失败，请检查网络或稍后重试';
  }
  final String text = e.toString();
  if (text.contains('SocketException') ||
      text.contains('HandshakeException') ||
      text.contains('Connection reset') ||
      text.contains('Connection terminated') ||
      (text.contains('ClientException') && text.contains('Socket'))) {
    return '网络连接失败，请检查网络后重试。';
  }
  return '请求失败，请检查网络或稍后重试';
}

/// 提交点菜前的前置校验（对应原生 `CheckoutViewModel.kt:87-99`）。
///
/// 返回 `null` 表示可以提交，否则是要展示给用户的原因。
/// 少了糖币这一道，请求会一路打到 `spend_eater_candy_coins` 被 RPC 拒绝
/// （`insufficient candy coins` / `eater role required`），
/// 界面只能看到 [friendlyErrorText] 的兜底文案。
String? orderSubmitBlockedReason({
  required bool isCaretaker,
  required int candyBalance,
  required int candyCost,
}) {
  if (isCaretaker) return '只有吃货可以提交点菜，饲养员负责管理小店和接单';
  if (candyBalance < candyCost) return '糖糖币不够啦，找饲养员撒点糖再点菜';
  return null;
}
