import 'package:flutter/services.dart';

class PermissionService {
  static const MethodChannel _channel =
      MethodChannel('kr.klr.stopusing/notification_listener');

  static PermissionService? _instance;
  static PermissionService get instance {
    _instance ??= PermissionService._();
    return _instance!;
  }

  PermissionService._();

  Future<bool> requestNotificationPermission() async {
    try {
      // NotificationListener 권한만 체크하고 설정 화면으로 안내
      return await _isNotificationListenerEnabled();
    } catch (e) {
      return false;
    }
  }

  Future<bool> _isNotificationListenerEnabled() async {
    try {
      final bool isEnabled = await _channel.invokeMethod('isNotificationListenerEnabled');
      return isEnabled;
    } catch (e) {
      return false;
    }
  }

  Future<void> openNotificationListenerSettings() async {
    try {
      await _channel.invokeMethod('openNotificationListenerSettings');
    } catch (e) {
      // Error opening notification listener settings
    }
  }

  Future<bool> checkPermissions() async {
    try {
      // NotificationListener 권한만 체크 (일반 notification 권한은 불필요)
      final isListenerEnabled = await _isNotificationListenerEnabled();
      return isListenerEnabled;
    } catch (e) {
      return false;
    }
  }
}