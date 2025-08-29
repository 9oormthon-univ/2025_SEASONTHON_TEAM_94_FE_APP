import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/test_service.dart';
import '../services/notification_service.dart';
import '../providers/transaction_provider.dart';

class TestScreen extends StatefulWidget {
  const TestScreen({super.key});

  @override
  State<TestScreen> createState() => _TestScreenState();
}

class _TestScreenState extends State<TestScreen> {
  final TestService _testService = TestService();
  bool _isGenerating = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('테스트 기능'),
        backgroundColor: Colors.orange,
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              color: Colors.blue.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '🔔 실제 알림 시뮬레이션',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    const Text(
                      '실제 금융 앱 알림을 시뮬레이션해서 NotificationListener가 파싱하는지 테스트합니다.',
                      style: TextStyle(color: Colors.grey),
                    ),
                    const SizedBox(height: 20),
                    _buildTestButton(
                      '토스 커피 결제',
                      '토스 앱에서 스타벅스 4,500원 결제 알림',
                      Icons.coffee,
                      Colors.blue,
                      () => _simulateNotification('TOSS_COFFEE'),
                    ),
                    const SizedBox(height: 8),
                    _buildTestButton(
                      '🧪 토스 파싱 테스트',
                      'NotificationListenerService 우회 직접 파싱',
                      Icons.bug_report,
                      Colors.orange,
                      () => _testNotificationParsing('TOSS_COFFEE'),
                    ),
                    const SizedBox(height: 12),
                    _buildTestButton(
                      'KB 식당 결제',
                      'KB국민은행에서 맥도날드 8,900원 결제',
                      Icons.restaurant,
                      Colors.brown,
                      () => _simulateNotification('KB_RESTAURANT'),
                    ),
                    const SizedBox(height: 12),
                    _buildTestButton(
                      '신한 계좌이체',
                      '신한은행에서 김철수에게 50,000원 이체',
                      Icons.account_balance,
                      Colors.blue,
                      () => _simulateNotification('SHINHAN_TRANSFER'),
                    ),
                    const SizedBox(height: 12),
                    _buildTestButton(
                      '카카오페이 배달',
                      '카카오페이로 배달의민족 13,500원 결제',
                      Icons.delivery_dining,
                      Colors.yellow.shade700,
                      () => _simulateNotification('KAKAO_DELIVERY'),
                    ),
                    const SizedBox(height: 12),
                    _buildTestButton(
                      '우리은행 ATM 출금',
                      '우리은행에서 ATM 현금출금 100,000원',
                      Icons.atm,
                      Colors.green,
                      () => _simulateNotification('WOORI_ATM'),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '🧪 테스트 데이터 생성',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    const Text(
                      '직접 데이터베이스에 가짜 거래 데이터를 추가합니다.',
                      style: TextStyle(color: Colors.grey),
                    ),
                    const SizedBox(height: 20),
                    _buildTestButton(
                      '실시간 거래 시뮬레이션',
                      '새로운 거래가 발생한 것처럼 시뮬레이션',
                      Icons.flash_on,
                      Colors.green,
                      _simulateRealTimeTransaction,
                    ),
                    const SizedBox(height: 12),
                    _buildTestButton(
                      '오늘 거래 5개 추가',
                      '오늘 날짜로 랜덤 거래 5개 생성',
                      Icons.today,
                      Colors.blue,
                      _addTodayTransactions,
                    ),
                    const SizedBox(height: 12),
                    _buildTestButton(
                      '랜덤 거래 10개 추가',
                      '최근 3일간의 랜덤 거래 데이터',
                      Icons.shuffle,
                      Colors.purple,
                      () => _addRandomTransactions(10),
                    ),
                    const SizedBox(height: 12),
                    _buildTestButton(
                      '대용량 데이터 생성',
                      '지난 30일간 200+ 거래 데이터',
                      Icons.storage,
                      Colors.orange,
                      _generateBulkData,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '🏦 금융앱별 테스트',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        _buildAppButton('토스', Colors.blue),
                        _buildAppButton('KB국민은행', Colors.brown),
                        _buildAppButton('신한은행', Colors.blue),
                        _buildAppButton('카카오페이', Colors.yellow),
                        _buildAppButton('우리은행', Colors.green),
                        _buildAppButton('하나은행', Colors.green),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
            Card(
              color: Colors.red.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '🗑️ 데이터 관리',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton.icon(
                        onPressed: _clearAllData,
                        icon: const Icon(Icons.delete_forever),
                        label: const Text('모든 테스트 데이터 삭제'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.red,
                          foregroundColor: Colors.white,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTestButton(
    String title,
    String subtitle,
    IconData icon,
    Color color,
    VoidCallback onPressed,
  ) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton.icon(
        onPressed: _isGenerating ? null : onPressed,
        icon: _isGenerating
            ? const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : Icon(icon),
        label: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              title,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            Text(
              subtitle,
              style: const TextStyle(fontSize: 12, color: Colors.white70),
            ),
          ],
        ),
        style: ElevatedButton.styleFrom(
          backgroundColor: color,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
          alignment: Alignment.centerLeft,
        ),
      ),
    );
  }

  Widget _buildAppButton(String appName, Color color) {
    return ElevatedButton(
      onPressed: _isGenerating ? null : () => _addAppTransaction(appName),
      style: ElevatedButton.styleFrom(
        backgroundColor: color,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      ),
      child: Text(appName, style: const TextStyle(fontSize: 12)),
    );
  }

  Future<void> _simulateRealTimeTransaction() async {
    setState(() => _isGenerating = true);
    
    try {
      final transaction = await _testService.simulateRealTimeTransaction();
      
      // Provider를 통해 UI 업데이트
      if (mounted) {
        final provider = context.read<TransactionProvider>();
        provider.onTransactionReceived?.call(transaction);
        await provider.loadTransactions();
        
        _showSuccessSnackBar('실시간 거래가 시뮬레이션되었습니다!\n${transaction.appName}: ${transaction.merchant}');
      }
    } catch (e) {
      _showErrorSnackBar('거래 시뮬레이션 실패: $e');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _addTodayTransactions() async {
    setState(() => _isGenerating = true);
    
    try {
      await _testService.addTodayTestTransactions();
      if (mounted) {
        await context.read<TransactionProvider>().loadTransactions();
        _showSuccessSnackBar('오늘 거래 5개가 추가되었습니다!');
      }
    } catch (e) {
      _showErrorSnackBar('오늘 거래 추가 실패: $e');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _addRandomTransactions(int count) async {
    setState(() => _isGenerating = true);
    
    try {
      await _testService.addTestTransactions(count);
      if (mounted) {
        await context.read<TransactionProvider>().loadTransactions();
        _showSuccessSnackBar('랜덤 거래 $count개가 추가되었습니다!');
      }
    } catch (e) {
      _showErrorSnackBar('랜덤 거래 추가 실패: $e');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _generateBulkData() async {
    setState(() => _isGenerating = true);
    
    try {
      await _testService.generateBulkTestData();
      if (mounted) {
        await context.read<TransactionProvider>().loadTransactions();
        _showSuccessSnackBar('30일간 대용량 테스트 데이터가 생성되었습니다!');
      }
    } catch (e) {
      _showErrorSnackBar('대용량 데이터 생성 실패: $e');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _addAppTransaction(String appName) async {
    setState(() => _isGenerating = true);
    
    try {
      final transaction = await _testService.createTransactionForApp(appName);
      if (mounted) {
        await context.read<TransactionProvider>().loadTransactions();
        _showSuccessSnackBar('$appName 거래가 추가되었습니다!\n${transaction.merchant}');
      }
    } catch (e) {
      _showErrorSnackBar('$appName 거래 추가 실패: $e');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _clearAllData() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('데이터 삭제 확인'),
        content: const Text('모든 거래 내역을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('삭제', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      setState(() => _isGenerating = true);
      
      try {
        await context.read<TransactionProvider>().deleteAllTransactions();
        _showSuccessSnackBar('모든 데이터가 삭제되었습니다!');
      } catch (e) {
        _showErrorSnackBar('데이터 삭제 실패: $e');
      } finally {
        if (mounted) setState(() => _isGenerating = false);
      }
    }
  }

  void _showSuccessSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 3),
      ),
    );
  }

  Future<void> _simulateNotification(String scenario) async {
    setState(() => _isGenerating = true);
    
    try {
      await NotificationService.instance.simulateTestNotification(scenario);
      
      // 잠시 기다린 후 거래 내역 새로고침
      await Future.delayed(const Duration(seconds: 2));
      if (mounted) {
        await context.read<TransactionProvider>().loadTransactions();
        _showSuccessSnackBar('$scenario 알림이 시뮬레이션되었습니다!\n알림이 파싱되어 거래 내역에 추가되는지 확인하세요.');
      }
    } catch (e) {
      _showErrorSnackBar('알림 시뮬레이션 실패: $e');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _testNotificationParsing(String scenario) async {
    setState(() => _isGenerating = true);
    
    try {
      await NotificationService.instance.testNotificationParsing(scenario);
      
      // 잠시 기다린 후 거래 내역 새로고침 (파싱 결과가 있을 경우)
      await Future.delayed(const Duration(seconds: 1));
      if (mounted) {
        await context.read<TransactionProvider>().loadTransactions();
        _showSuccessSnackBar('🧪 $scenario 직접 파싱 테스트 완료!\nLogcat에서 상세 결과를 확인하세요.');
      }
    } catch (e) {
      _showErrorSnackBar('파싱 테스트 실패: $e');
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 3),
      ),
    );
  }
}