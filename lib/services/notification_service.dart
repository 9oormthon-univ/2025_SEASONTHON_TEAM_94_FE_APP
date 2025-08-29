import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import '../models/transaction.dart';
import 'database_service.dart';

class NotificationService {
  static const MethodChannel _channel =
      MethodChannel('com.example.stopusing_app/notification_listener');

  static NotificationService? _instance;
  static NotificationService get instance {
    _instance ??= NotificationService._();
    return _instance!;
  }

  NotificationService._() {
    _setupMethodCallHandler();
  }

  final DatabaseService _databaseService = DatabaseService();
  Function(Transaction)? onTransactionReceived;

  void _setupMethodCallHandler() {
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onWithdrawalDetected':
          await _handleWithdrawalDetected(call.arguments);
          break;
        default:
          throw PlatformException(
            code: 'Unimplemented',
            details: 'Method ${call.method} not implemented',
          );
      }
    });
  }

  Future<void> _handleWithdrawalDetected(Map<dynamic, dynamic> data) async {
    try {
      final transaction = Transaction.fromNotification(Map<String, dynamic>.from(data));
      
      // Save to database
      await _databaseService.insertTransaction(transaction);
      
      // Notify UI
      onTransactionReceived?.call(transaction);
      
      // 새로운 출금 내역 저장됨
    } catch (e) {
      // 출금 내역 처리 중 오류 발생: $e
    }
  }

  Future<bool> requestNotificationPermission() async {
    try {
      // Request notification permission
      final status = await Permission.notification.request();
      
      if (status.isGranted) {
        // Check if notification listener service is enabled
        return await _isNotificationListenerEnabled();
      }
      
      return false;
    } catch (e) {
      // 알림 권한 요청 중 오류 발생: $e
      return false;
    }
  }

  Future<bool> _isNotificationListenerEnabled() async {
    try {
      final bool isEnabled = await _channel.invokeMethod('isNotificationListenerEnabled');
      return isEnabled;
    } catch (e) {
      // 알림 리스너 상태 확인 중 오류 발생: $e
      return false;
    }
  }

  Future<void> openNotificationListenerSettings() async {
    try {
      await _channel.invokeMethod('openNotificationListenerSettings');
    } catch (e) {
      // 알림 리스너 설정 열기 중 오류 발생: $e
    }
  }

  Future<bool> checkPermissions() async {
    try {
      // Check notification permission
      final notificationStatus = await Permission.notification.status;
      
      // Check if notification listener is enabled
      final isListenerEnabled = await _isNotificationListenerEnabled();
      
      return notificationStatus.isGranted && isListenerEnabled;
    } catch (e) {
      // 권한 확인 중 오류 발생: $e
      return false;
    }
  }

  Future<List<String>> getAvailableFinancialApps() async {
    try {
      final List<dynamic> apps = await _channel.invokeMethod('getAvailableFinancialApps');
      return List<String>.from(apps);
    } catch (e) {
      // 금융 앱 목록 조회 중 오류 발생: $e
      return [];
    }
  }

  Future<void> updateMonitoredApps(List<String> packageNames) async {
    try {
      await _channel.invokeMethod('updateMonitoredApps', packageNames);
    } catch (e) {
      // 모니터링 앱 업데이트 중 오류 발생: $e
    }
  }

  Future<void> simulateTestNotification(String scenario) async {
    try {
      await _channel.invokeMethod('simulateTestNotification', {'scenario': scenario});
    } catch (e) {
      // 테스트 알림 시뮬레이션 중 오류 발생: $e
    }
  }

  Future<void> testNotificationParsing(String scenario) async {
    try {
      await _channel.invokeMethod('testNotificationParsing', {'scenario': scenario});
    } catch (e) {
      // 알림 파싱 테스트 중 오류 발생: $e
    }
  }

  Future<void> simulateCustomNotification({
    required String appName,
    required String packageName,
    required String transactionType,
    required String merchant,
    required String amount,
  }) async {
    try {
      await _channel.invokeMethod('simulateCustomNotification', {
        'appName': appName,
        'packageName': packageName,
        'transactionType': transactionType,
        'merchant': merchant,
        'amount': amount,
      });
    } catch (e) {
      // 커스텀 알림 시뮬레이션 중 오류 발생: $e
    }
  }

  void dispose() {
    _databaseService.close();
  }
}