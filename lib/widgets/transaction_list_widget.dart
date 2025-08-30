import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/transaction_provider.dart';
import '../models/transaction.dart';
import 'transaction_card.dart';

class TransactionListWidget extends StatelessWidget {
  const TransactionListWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<TransactionProvider>(
      builder: (context, provider, child) {
        if (provider.isLoading) {
          return const Center(child: CircularProgressIndicator());
        }

        if (provider.errorMessage.isNotEmpty) {
          return _buildErrorView(provider);
        }

        if (provider.transactions.isEmpty) {
          return _buildEmptyView();
        }

        return _buildTransactionsList(provider);
      },
    );
  }

  Widget _buildErrorView(TransactionProvider provider) {
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

  Widget _buildEmptyView() {
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

  Widget _buildTransactionsList(TransactionProvider provider) {
    return RefreshIndicator(
      onRefresh: () => provider.loadTransactions(),
      child: ListView.builder(
        itemCount: provider.transactions.length,
        itemBuilder: (context, index) {
          final transaction = provider.transactions[index];
          return TransactionCard(
            transaction: transaction,
            onDelete: () => _deleteTransaction(provider, transaction),
          );
        },
      ),
    );
  }

  void _deleteTransaction(TransactionProvider provider, Transaction transaction) {
    if (transaction.id != null) {
      provider.deleteTransaction(transaction.id!);
    }
  }
}