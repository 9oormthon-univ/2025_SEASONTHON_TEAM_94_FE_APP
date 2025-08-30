import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/transaction_provider.dart';
import '../services/notification_service.dart';
import '../widgets/permission_prompt_widget.dart';
import '../widgets/transaction_list_widget.dart';
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
          ? PermissionPromptWidget(onPermissionGranted: _checkPermissions)
          : const TransactionListWidget(),
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
}