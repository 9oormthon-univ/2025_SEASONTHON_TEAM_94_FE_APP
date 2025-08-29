import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/transaction_provider.dart';
import '../services/notification_service.dart';
import '../models/transaction.dart';
import 'settings_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  bool _hasPermission = false;

  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  Future<void> _checkPermissions() async {
    final hasPermission = await NotificationService.instance.checkPermissions();
    setState(() {
      _hasPermission = hasPermission;
    });
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '금융 출금 추적기',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const SettingsScreen()),
              );
            },
          ),
        ],
      ),
      body: !_hasPermission
          ? _buildPermissionPrompt()
          : _buildTransactionsList(),
      floatingActionButton: _hasPermission
          ? FloatingActionButton(
              onPressed: () {
                context.read<TransactionProvider>().loadTransactions();
              },
              child: const Icon(Icons.refresh),
            )
          : null,
    );
  }

  Widget _buildPermissionPrompt() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.notification_important,
              size: 80,
              color: Theme.of(context).colorScheme.primary,
            ),
            const SizedBox(height: 24),
            const Text(
              '알림 접근 권한이 필요합니다',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            const Text(
              '금융 앱의 알림을 읽어서 출금 내역을 자동으로 추적하려면 알림 접근 권한이 필요합니다.',
              style: TextStyle(fontSize: 16),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            ElevatedButton.icon(
              onPressed: () async {
                final granted = await NotificationService.instance.requestNotificationPermission();
                if (!granted) {
                  await NotificationService.instance.openNotificationListenerSettings();
                }
                await _checkPermissions();
              },
              icon: const Icon(Icons.settings),
              label: const Text('권한 설정하기'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTransactionsList() {
    return Consumer<TransactionProvider>(
      builder: (context, provider, child) {
        if (provider.isLoading) {
          return const Center(child: CircularProgressIndicator());
        }

        if (provider.errorMessage.isNotEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.error, size: 64, color: Colors.red),
                const SizedBox(height: 16),
                Text(provider.errorMessage),
                ElevatedButton(
                  onPressed: () => provider.loadTransactions(),
                  child: const Text('다시 시도'),
                ),
              ],
            ),
          );
        }

        if (provider.transactions.isEmpty) {
          return const Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.inbox, size: 64, color: Colors.grey),
                SizedBox(height: 16),
                Text(
                  '아직 출금 내역이 없습니다',
                  style: TextStyle(fontSize: 18, color: Colors.grey),
                ),
                SizedBox(height: 8),
                Text(
                  '금융 앱에서 출금이 발생하면 자동으로 추적됩니다',
                  style: TextStyle(color: Colors.grey),
                ),
              ],
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: () => provider.loadTransactions(),
          child: ListView.builder(
            itemCount: provider.transactions.length,
            itemBuilder: (context, index) {
              final transaction = provider.transactions[index];
              return _buildTransactionCard(transaction, provider);
            },
          ),
        );
      },
    );
  }

  Widget _buildTransactionCard(Transaction transaction, TransactionProvider provider) {
    final formatter = NumberFormat('#,###');
    
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Colors.red.shade100,
          child: Icon(
            Icons.remove_circle_outline,
            color: Colors.red.shade700,
          ),
        ),
        title: Text(
          transaction.merchant,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(transaction.appName),
            Text(
              DateFormat('yyyy-MM-dd HH:mm').format(transaction.timestamp),
              style: TextStyle(
                color: Colors.grey.shade600,
                fontSize: 12,
              ),
            ),
          ],
        ),
        trailing: Text(
          '-${formatter.format(transaction.amount)}원',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 16,
            color: Colors.red.shade700,
          ),
        ),
        onTap: () => _showTransactionDetails(transaction),
        onLongPress: () => _showDeleteDialog(transaction, provider),
      ),
    );
  }


  void _showTransactionDetails(Transaction transaction) {
    final formatter = NumberFormat('#,###');
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('거래 상세 정보'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildDetailRow('상호명', transaction.merchant),
            _buildDetailRow('금액', '${formatter.format(transaction.amount)}원'),
            _buildDetailRow('앱', transaction.appName),
            _buildDetailRow('일시', DateFormat('yyyy-MM-dd HH:mm:ss').format(transaction.timestamp)),
            _buildDetailRow('원본 텍스트', transaction.rawText),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            value,
            style: const TextStyle(fontSize: 14),
          ),
          const Divider(),
        ],
      ),
    );
  }

  void _showDeleteDialog(Transaction transaction, TransactionProvider provider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('거래 내역 삭제'),
        content: Text('${transaction.merchant} 거래 내역을 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () {
              if (transaction.id != null) {
                provider.deleteTransaction(transaction.id!);
              }
              Navigator.of(context).pop();
            },
            child: const Text('삭제'),
          ),
        ],
      ),
    );
  }
}