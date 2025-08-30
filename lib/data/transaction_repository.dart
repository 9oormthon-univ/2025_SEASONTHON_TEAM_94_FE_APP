import '../models/transaction.dart';
import '../services/database_service.dart';

/// 거래 데이터를 관리하는 Repository
/// 데이터베이스와의 모든 상호작용을 담당
class TransactionRepository {
  final DatabaseService _databaseService = DatabaseService();
  
  /// 모든 거래 내역 조회
  Future<List<Transaction>> getAllTransactions() async {
    try {
      return await _databaseService.getAllTransactions();
    } catch (e) {
      throw Exception('거래 내역을 불러오는데 실패했습니다: $e');
    }
  }
  
  /// 새 거래 추가
  Future<void> addTransaction(Transaction transaction) async {
    try {
      await _databaseService.insertTransaction(transaction);
    } catch (e) {
      throw Exception('거래 추가에 실패했습니다: $e');
    }
  }
  
  /// 거래 삭제
  Future<void> deleteTransaction(int id) async {
    try {
      await _databaseService.deleteTransaction(id);
    } catch (e) {
      throw Exception('거래 삭제에 실패했습니다: $e');
    }
  }
  
  /// 모든 거래 삭제
  Future<void> deleteAllTransactions() async {
    try {
      await _databaseService.deleteAllTransactions();
    } catch (e) {
      throw Exception('모든 거래 삭제에 실패했습니다: $e');
    }
  }
  
  /// 오늘의 거래 조회
  Future<List<Transaction>> getTodayTransactions() async {
    try {
      final allTransactions = await getAllTransactions();
      final today = DateTime.now();
      final startOfDay = DateTime(today.year, today.month, today.day);
      final endOfDay = startOfDay.add(const Duration(days: 1));

      return allTransactions.where((transaction) {
        return transaction.timestamp.isAfter(startOfDay) &&
            transaction.timestamp.isBefore(endOfDay);
      }).toList();
    } catch (e) {
      throw Exception('오늘 거래 내역을 불러오는데 실패했습니다: $e');
    }
  }
}