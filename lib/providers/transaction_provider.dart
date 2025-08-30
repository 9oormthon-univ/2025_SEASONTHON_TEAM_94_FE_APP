import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:async';
import '../models/transaction.dart';
import '../data/transaction_repository.dart';
import '../services/notification_service.dart';

class TransactionProvider with ChangeNotifier {
  final List<Transaction> _transactions = [];
  bool _isLoading = false;
  String _errorMessage = '';
  final TransactionRepository _repository = TransactionRepository();
  Timer? _pollTimer;
  int _lastTransactionTime = 0;

  List<Transaction> get transactions => _transactions;
  bool get isLoading => _isLoading;
  String get errorMessage => _errorMessage;

  TransactionProvider() {
    _initProvider();
  }

  void _initProvider() {
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
      
      if (hasNewTransaction && lastTransactionTime > _lastTransactionTime) {
        await loadTransactions();
        _lastTransactionTime = lastTransactionTime;
        await prefs.setBool('new_transaction', false);
      }
    } catch (e) {
      // Error checking for new transactions
    }
  }

  void _addNewTransaction(Transaction transaction) {
    _transactions.insert(0, transaction);
    notifyListeners();
  }

  Future<void> loadTransactions() async {
    _setLoading(true);
    try {
      final transactions = await _repository.getAllTransactions();
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
      await _repository.deleteTransaction(id);
      _transactions.removeWhere((transaction) => transaction.id == id);
      notifyListeners();
    } catch (e) {
      _errorMessage = '거래 내역을 삭제하는 중 오류가 발생했습니다: $e';
      notifyListeners();
    }
  }

  Future<void> deleteAllTransactions() async {
    try {
      await _repository.deleteAllTransactions();
      _transactions.clear();
      notifyListeners();
    } catch (e) {
      _errorMessage = '모든 거래 내역 삭제 중 오류가 발생했습니다: $e';
      notifyListeners();
      rethrow;
    }
  }

  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }
}