import 'package:flutter/services.dart';

class AppManagementService {
  static const MethodChannel _channel =
      MethodChannel('kr.klr.stopusing/notification_listener');

  Future<List<String>> getAvailableFinancialApps() async {
    try {
      final List<dynamic> apps = await _channel.invokeMethod('getAvailableFinancialApps');
      return List<String>.from(apps);
    } catch (e) {
      return [];
    }
  }

  Future<void> updateMonitoredApps(List<String> packageNames) async {
    try {
      await _channel.invokeMethod('updateMonitoredApps', packageNames);
    } catch (e) {
      // Error updating monitored apps
    }
  }
}