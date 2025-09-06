import 'package:flutter/services.dart';
import '../models/transaction.dart';
import 'database_service.dart';
import 'user_service.dart';

class TransactionHandler {
  static const MethodChannel _channel =
      MethodChannel('kr.klr.stopusing/notification_listener');

  final DatabaseService _databaseService = DatabaseService();
  Function(Transaction)? onTransactionReceived;

  void setupMethodCallHandler() {
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

  /// Send user UID to Android
  Future<void> updateUserUid() async {
    try {
      final userUid = await UserService.instance.getUserUid();
      await _channel.invokeMethod('updateUserUid', {'userUid': userUid});
    } catch (e) {
      // Handle error silently
    }
  }

  Future<void> _handleWithdrawalDetected(Map<dynamic, dynamic> data) async {
    try {
      final transaction = Transaction.fromNotification(Map<String, dynamic>.from(data));
      
      await _databaseService.insertTransaction(transaction);
      
      onTransactionReceived?.call(transaction);
    } catch (e) {
      // Error processing withdrawal notification
    }
  }

  void dispose() {
    _databaseService.close();
  }
}