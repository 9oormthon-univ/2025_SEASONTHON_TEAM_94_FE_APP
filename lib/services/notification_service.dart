import '../models/transaction.dart';
import 'permission_service.dart';
import 'transaction_handler.dart';
import 'app_management_service.dart';

class NotificationService {
  static NotificationService? _instance;
  static NotificationService get instance {
    _instance ??= NotificationService._();
    return _instance!;
  }

  NotificationService._() {
    _transactionHandler.setupMethodCallHandler();
  }

  final PermissionService _permissionService = PermissionService.instance;
  final TransactionHandler _transactionHandler = TransactionHandler();
  final AppManagementService _appManagementService = AppManagementService();

  Function(Transaction)? get onTransactionReceived => _transactionHandler.onTransactionReceived;
  set onTransactionReceived(Function(Transaction)? callback) {
    _transactionHandler.onTransactionReceived = callback;
  }

  Future<bool> requestNotificationPermission() async {
    return await _permissionService.requestNotificationPermission();
  }

  Future<void> openNotificationListenerSettings() async {
    await _permissionService.openNotificationListenerSettings();
  }

  Future<bool> checkPermissions() async {
    return await _permissionService.checkPermissions();
  }

  Future<List<String>> getAvailableFinancialApps() async {
    return await _appManagementService.getAvailableFinancialApps();
  }

  Future<void> updateMonitoredApps(List<String> packageNames) async {
    await _appManagementService.updateMonitoredApps(packageNames);
  }

  void dispose() {
    _transactionHandler.dispose();
  }
}