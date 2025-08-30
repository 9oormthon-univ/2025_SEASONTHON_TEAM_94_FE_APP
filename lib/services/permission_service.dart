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

  Future<bool> requestPostNotificationPermission() async {
    try {
      await _channel.invokeMethod('requestNotificationPermission');
      // 권한 요청 후 상태 확인
      await Future.delayed(const Duration(milliseconds: 500));
      return await _hasNotificationPermission();
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
      // 모든 권한 상태 확인
      final result = await _channel.invokeMethod('checkAllPermissions');
      if (result is Map) {
        return result['allGranted'] ?? false;
      }
      return false;
    } catch (e) {
      return false;
    }
  }


  Future<bool> _hasNotificationPermission() async {
    try {
      final bool hasPermission = await _channel.invokeMethod('hasNotificationPermission');
      return hasPermission;
    } catch (e) {
      return false;
    }
  }

  Future<Map<String, bool>> checkAllPermissions() async {
    try {
      final result = await _channel.invokeMethod('checkAllPermissions');
      if (result is Map) {
        return {
          'listenerEnabled': result['listenerEnabled'] ?? false,
          'notificationPermission': result['notificationPermission'] ?? false,
          'allGranted': result['allGranted'] ?? false,
        };
      }
    } catch (e) {
      // Handle error
    }
    return {
      'listenerEnabled': false,
      'notificationPermission': false,
      'allGranted': false,
    };
  }
}