import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/transaction_provider.dart';
import '../services/notification_service.dart';
import '../services/user_service.dart';
import '../main.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _hasPermission = false;
  bool _isCheckingPermission = false;

  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  Future<void> _checkPermissions() async {
    setState(() {
      _isCheckingPermission = true;
    });
    
    final hasPermission = await NotificationService.instance.checkPermissions();
    
    setState(() {
      _hasPermission = hasPermission;
      _isCheckingPermission = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('설정'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildPermissionSection(),
          const SizedBox(height: 24),
          _buildDataSection(),
          const SizedBox(height: 24),
          _buildDebugSection(),
          const SizedBox(height: 24),
          _buildAboutSection(),
        ],
      ),
    );
  }

  Widget _buildPermissionSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '권한 설정',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            ListTile(
              leading: Icon(
                _hasPermission ? Icons.check_circle : Icons.error,
                color: _hasPermission ? Colors.green : Colors.red,
              ),
              title: const Text('알림 접근 권한'),
              subtitle: Text(
                _hasPermission
                    ? '권한이 허용되었습니다'
                    : '금융 앱 알림을 읽기 위해 권한이 필요합니다',
              ),
              trailing: _isCheckingPermission
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : IconButton(
                      icon: const Icon(Icons.refresh),
                      onPressed: _checkPermissions,
                    ),
              onTap: _hasPermission ? null : _requestPermission,
            ),
            if (!_hasPermission) ...[
              const SizedBox(height: 8),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '권한 설정 방법:',
                      style: TextStyle(fontWeight: FontWeight.w500),
                    ),
                    const SizedBox(height: 4),
                    const Text('1. 아래 버튼을 눌러 설정으로 이동'),
                    const Text('2. "알림 액세스" 또는 "특별한 앱 액세스" 찾기'),
                    const Text('3. "금융 출금 추적기" 앱 활성화'),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton.icon(
                        onPressed: _requestPermission,
                        icon: const Icon(Icons.settings),
                        label: const Text('권한 설정하기'),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildDataSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '데이터 관리',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            ListTile(
              leading: const Icon(Icons.download, color: Colors.blue),
              title: const Text('데이터 내보내기'),
              subtitle: const Text('거래 내역을 CSV 파일로 저장'),
              onTap: _exportData,
            ),
            ListTile(
              leading: const Icon(Icons.delete_forever, color: Colors.red),
              title: const Text('모든 데이터 삭제'),
              subtitle: const Text('저장된 모든 거래 내역을 삭제'),
              onTap: _showDeleteAllDialog,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDebugSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '개발자 옵션',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            ListTile(
              leading: const Icon(Icons.logout, color: Colors.red),
              title: const Text('로그아웃 (테스트용)'),
              subtitle: const Text('로그인 이력을 제거하고 온보딩으로 돌아갑니다'),
              onTap: _showLogoutDialog,
            ),
            ListTile(
              leading: const Icon(Icons.login, color: Colors.green),
              title: const Text('로그인 시뮬레이션'),
              subtitle: const Text('로그인 이력을 생성합니다'),
              onTap: _simulateLogin,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAboutSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '정보',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            const ListTile(
              leading: Icon(Icons.info, color: Colors.blue),
              title: Text('앱 버전'),
              subtitle: Text('1.0.0'),
            ),
            ListTile(
              leading: const Icon(Icons.security, color: Colors.green),
              title: const Text('개인정보 보호'),
              subtitle: const Text('모든 데이터는 기기에만 저장됩니다'),
              onTap: _showPrivacyInfo,
            ),
            ListTile(
              leading: const Icon(Icons.help, color: Colors.orange),
              title: const Text('사용 방법'),
              subtitle: const Text('앱 사용법 보기'),
              onTap: _showUsageInfo,
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _requestPermission() async {
    try {
      final granted = await NotificationService.instance.requestNotificationPermission();
      if (!granted) {
        await NotificationService.instance.openNotificationListenerSettings();
      }
      await _checkPermissions();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('권한 설정 중 오류가 발생했습니다: $e')),
        );
      }
    }
  }

  void _exportData() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('데이터 내보내기'),
        content: const Text(
          '거래 내역을 CSV 형식으로 내보내기 기능은 다음 업데이트에서 추가될 예정입니다.\n\n'
          '현재는 설정에서 "모든 데이터 삭제" 기능만 사용 가능합니다.',
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

  void _showDeleteAllDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('모든 데이터 삭제'),
        content: const Text(
          '저장된 모든 거래 내역이 영구적으로 삭제됩니다. '
          '이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () async {
              final navigator = Navigator.of(context);
              final scaffoldMessenger = ScaffoldMessenger.of(context);
              final provider = context.read<TransactionProvider>();
              
              navigator.pop();
              await provider.deleteAllTransactions();
              scaffoldMessenger.showSnackBar(
                const SnackBar(content: Text('모든 데이터가 삭제되었습니다')),
              );
            },
            child: const Text('삭제', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  void _showPrivacyInfo() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('개인정보 보호'),
        content: const SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '이 앱은 사용자의 개인정보를 보호합니다:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('• 모든 데이터는 기기에만 저장됩니다'),
              Text('• 외부 서버로 데이터를 전송하지 않습니다'),
              Text('• 금융 앱 알림만 읽고 다른 개인 정보는 접근하지 않습니다'),
              Text('• 출금 관련 정보만 추출하고 저장합니다'),
              SizedBox(height: 12),
              Text(
                '권한 사용 목적:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 4),
              Text('• 알림 접근: 금융 앱의 출금 알림만 읽기 위해 사용됩니다'),
              Text('• 저장소 접근: 거래 내역을 기기에 저장하기 위해 사용됩니다'),
            ],
          ),
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

  void _showUsageInfo() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('사용 방법'),
        content: const SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '1. 권한 설정',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              Text('• 알림 접근 권한을 허용해야 합니다'),
              Text('• 설정 > 알림 액세스에서 앱을 활성화하세요'),
              SizedBox(height: 12),
              Text(
                '2. 자동 추적',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              Text('• 지원하는 금융 앱에서 출금이 발생하면 자동으로 기록됩니다'),
              Text('• 카드 결제, 계좌 이체, 현금 인출 등이 추적됩니다'),
              SizedBox(height: 12),
              Text(
                '3. 내역 확인',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              Text('• 메인 화면에서 모든 거래를 실시간으로 확인할 수 있습니다'),
              Text('• 거래 내역을 클릭하면 자세한 정보를 볼 수 있습니다'),
              SizedBox(height: 12),
              Text(
                '지원 앱:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              Text('• 국민, 신한, 우리, 농협, 기업, 하나은행 등'),
              Text('• 토스, 카카오페이 등 간편결제'),
              Text('• 주요 카드사 앱들'),
            ],
          ),
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

  void _showLogoutDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('로그아웃'),
        content: const Text(
          '로그인 이력을 제거하고 온보딩 화면으로 돌아갑니다. '
          '계속하시겠습니까?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () async {
              final navigator = Navigator.of(context);
              await UserService.instance.simulateLogout();
              if (mounted) {
                navigator.pop();
                // 앱을 다시 시작하기 위해 main으로 이동
                navigator.pushAndRemoveUntil(
                  MaterialPageRoute(builder: (context) => const AppRouter()),
                  (route) => false,
                );
              }
            },
            child: const Text('확인', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  void _simulateLogin() async {
    await UserService.instance.simulateLogin('test_user_${DateTime.now().millisecondsSinceEpoch}');
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('로그인 이력이 생성되었습니다')),
      );
    }
  }
}