import 'package:flutter/material.dart';
import '../services/notification_service.dart';

class PermissionPromptWidget extends StatelessWidget {
  final VoidCallback onPermissionGranted;

  const PermissionPromptWidget({
    super.key,
    required this.onPermissionGranted,
  });

  @override
  Widget build(BuildContext context) {
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
                onPermissionGranted();
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
}