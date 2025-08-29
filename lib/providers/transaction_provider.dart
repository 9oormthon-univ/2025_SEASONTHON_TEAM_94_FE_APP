import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:async';
import '../models/transaction.dart';
import '../services/database_service.dart';
import '../services/notification_service.dart';

class TransactionProvider with ChangeNotifier {
  final List<Transaction> _transactions = [];
  bool _isLoading = false;
  String _errorMessage = '';
  final DatabaseService _databaseService = DatabaseService();
  Timer? _pollTimer;
  int _lastTransactionTime = 0;

  List<Transaction> get transactions => _transactions;
  bool get isLoading => _isLoading;
  String get errorMessage => _errorMessage;

  TransactionProvider() {
    _initProvider();
  }

  void _initProvider() {
    // Listen to new transactions from notification service
    NotificationService.instance.onTransactionReceived = (transaction) {
      _addNewTransaction(transaction);
    };
    
    loadTransactions();
    _startPollingForNewTransactions();
  }
  
  void _startPollingForNewTransactions() {
    _pollTimer = Timer.periodic(const Duration(seconds: 2), (timer) async {
      await _checkForNewTransactions();
    });
  }
  
  Future<void> _checkForNewTransactions() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final hasNewTransaction = prefs.getBool('new_transaction') ?? false;
      final lastTransactionTime = prefs.getInt('last_transaction_time') ?? 0;
      
      print('🔍 Checking: new_transaction=$hasNewTransaction, last_time=$lastTransactionTime, current_last=$_lastTransactionTime');
      
      if (hasNewTransaction && lastTransactionTime > _lastTransactionTime) {
        print('🔔 New transaction detected, reloading...');
        await loadTransactions();
        _lastTransactionTime = lastTransactionTime;
        
        // Clear the flag
        await prefs.setBool('new_transaction', false);
        print('✅ Transaction loaded and flag cleared');
      }
    } catch (e) {
      print('Error checking for new transactions: $e');
    }
  }
  
  // Public method for test service access
  Function(Transaction)? get onTransactionReceived => 
      (transaction) => _addNewTransaction(transaction);

  void _addNewTransaction(Transaction transaction) {
    _transactions.insert(0, transaction);
    notifyListeners();
  }

  Future<void> loadTransactions() async {
    _setLoading(true);
    try {
      final transactions = await _databaseService.getAllTransactions();
      _transactions.clear();
      _transactions.addAll(transactions);
      _errorMessage = '';
    } catch (e) {
      _errorMessage = '거래 내역을 불러오는 중 오류가 발생했습니다: $e';
    }
    _setLoading(false);
  }

  Future<void> loadTransactionsByDateRange(DateTime startDate, DateTime endDate) async {
    _setLoading(true);
    try {
      final transactions = await _databaseService.getTransactionsByDateRange(startDate, endDate);
      _transactions.clear();
      _transactions.addAll(transactions);
      _errorMessage = '';
    } catch (e) {
      _errorMessage = '거래 내역을 불러오는 중 오류가 발생했습니다: $e';
    }
    _setLoading(false);
  }

  Future<void> deleteTransaction(int id) async {
    try {
      await _databaseService.deleteTransaction(id);
      _transactions.removeWhere((transaction) => transaction.id == id);
      notifyListeners();
    } catch (e) {
      _errorMessage = '거래 내역을 삭제하는 중 오류가 발생했습니다: $e';
      notifyListeners();
    }
  }

  Future<void> deleteAllTransactions() async {
    _setLoading(true);
    try {
      await _databaseService.deleteAllTransactions();
      _transactions.clear();
      _errorMessage = '';
    } catch (e) {
      _errorMessage = '모든 거래 내역을 삭제하는 중 오류가 발생했습니다: $e';
    }
    _setLoading(false);
  }

  Future<int> getTotalAmount() async {
    try {
      return await _databaseService.getTotalAmount();
    } catch (e) {
      return 0;
    }
  }

  Future<int> getTotalAmountByDateRange(DateTime startDate, DateTime endDate) async {
    try {
      return await _databaseService.getTotalAmountByDateRange(startDate, endDate);
    } catch (e) {
      return 0;
    }
  }

  Future<Map<String, int>> getAmountByApp() async {
    try {
      return await _databaseService.getAmountByApp();
    } catch (e) {
      return {};
    }
  }


  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _databaseService.close();
    super.dispose();
  }
}