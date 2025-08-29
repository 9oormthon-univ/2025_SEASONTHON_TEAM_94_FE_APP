import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/transaction_provider.dart';

class StatisticsScreen extends StatelessWidget {
  const StatisticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Consumer<TransactionProvider>(
        builder: (context, provider, child) {
          if (provider.isLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (provider.transactions.isEmpty) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.bar_chart, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text(
                    '통계를 표시할 데이터가 없습니다',
                    style: TextStyle(fontSize: 18, color: Colors.grey),
                  ),
                ],
              ),
            );
          }

          return ListView(
            children: [
              _buildSummaryCards(provider),
              const SizedBox(height: 24),
              _buildAppBreakdown(provider),
              const SizedBox(height: 24),
              _buildPeriodStats(provider),
            ],
          );
        },
      ),
    );
  }

  Widget _buildSummaryCards(TransactionProvider provider) {
    final formatter = NumberFormat('#,###');
    final totalTransactions = provider.transactions.length;
    final totalAmount = provider.transactions.fold<int>(
      0,
      (sum, transaction) => sum + transaction.amount,
    );
    final averageAmount = totalTransactions > 0 ? (totalAmount / totalTransactions).round() : 0;

    final todayTransactions = provider.getTransactionsForToday();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '전체 요약',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _buildStatCard(
                '총 거래 건수',
                '$totalTransactions건',
                Icons.receipt_long,
                Colors.blue,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildStatCard(
                '총 출금액',
                '${formatter.format(totalAmount)}원',
                Icons.account_balance_wallet,
                Colors.red,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _buildStatCard(
                '평균 출금액',
                '${formatter.format(averageAmount)}원',
                Icons.trending_up,
                Colors.orange,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildStatCard(
                '오늘',
                '${todayTransactions.length}건',
                Icons.today,
                Colors.green,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, Color color) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Icon(icon, color: color, size: 32),
            const SizedBox(height: 8),
            Text(
              title,
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBreakdown(TransactionProvider provider) {
    final appStats = <String, Map<String, int>>{};
    
    for (final transaction in provider.transactions) {
      appStats[transaction.appName] = appStats[transaction.appName] ?? {'count': 0, 'amount': 0};
      appStats[transaction.appName]!['count'] = appStats[transaction.appName]!['count']! + 1;
      appStats[transaction.appName]!['amount'] = appStats[transaction.appName]!['amount']! + transaction.amount;
    }

    final sortedApps = appStats.entries.toList()
      ..sort((a, b) => b.value['amount']!.compareTo(a.value['amount']!));

    final formatter = NumberFormat('#,###');

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '앱별 출금 현황',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: sortedApps.map((entry) {
                final appName = entry.key;
                final count = entry.value['count']!;
                final amount = entry.value['amount']!;
                final percentage = (amount / provider.transactions.fold<int>(0, (sum, t) => sum + t.amount) * 100);

                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: Text(
                              appName,
                              style: const TextStyle(fontWeight: FontWeight.w500),
                            ),
                          ),
                          Text(
                            '${formatter.format(amount)}원 ($count건)',
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      LinearProgressIndicator(
                        value: percentage / 100,
                        backgroundColor: Colors.grey.shade200,
                        valueColor: AlwaysStoppedAnimation<Color>(
                          Colors.primaries[sortedApps.indexOf(entry) % Colors.primaries.length],
                        ),
                      ),
                      const SizedBox(height: 4),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Text(
                            '${percentage.toStringAsFixed(1)}%',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey.shade600,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPeriodStats(TransactionProvider provider) {
    final formatter = NumberFormat('#,###');
    
    final todayTransactions = provider.getTransactionsForToday();
    final weekTransactions = provider.getTransactionsForThisWeek();
    final monthTransactions = provider.getTransactionsForThisMonth();
    
    final todayAmount = todayTransactions.fold<int>(0, (sum, t) => sum + t.amount);
    final weekAmount = weekTransactions.fold<int>(0, (sum, t) => sum + t.amount);
    final monthAmount = monthTransactions.fold<int>(0, (sum, t) => sum + t.amount);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '기간별 출금 현황',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                _buildPeriodRow(
                  '오늘',
                  '${todayTransactions.length}건',
                  '${formatter.format(todayAmount)}원',
                  Icons.today,
                  Colors.green,
                ),
                const Divider(),
                _buildPeriodRow(
                  '이번 주',
                  '${weekTransactions.length}건',
                  '${formatter.format(weekAmount)}원',
                  Icons.date_range,
                  Colors.blue,
                ),
                const Divider(),
                _buildPeriodRow(
                  '이번 달',
                  '${monthTransactions.length}건',
                  '${formatter.format(monthAmount)}원',
                  Icons.calendar_month,
                  Colors.orange,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPeriodRow(
    String period,
    String count,
    String amount,
    IconData icon,
    Color color,
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(icon, color: color, size: 24),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  period,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Text(
                  count,
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey.shade600,
                  ),
                ),
              ],
            ),
          ),
          Text(
            amount,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
}