import 'package:flutter/material.dart';
import '../services/notification_service.dart';
import 'webview_screen.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  bool _isLoading = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const Spacer(flex: 2),
              
              // App Icon/Logo placeholder
              Container(
                width: 120,
                height: 120,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primary,
                  borderRadius: BorderRadius.circular(24),
                ),
                child: Icon(
                  Icons.notifications_active,
                  size: 60,
                  color: Theme.of(context).colorScheme.onPrimary,
                ),
              ),
              
              const SizedBox(height: 32),
              
              // Welcome text
              Text(
                '그만써!에 오신 것을 환영합니다',
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
              ),
              
              const SizedBox(height: 16),
              
              Text(
                '알림을 통해 금융 거래를 추적하고 관리할 수 있도록 도와드립니다.',
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  color: Colors.grey[600],
                ),
                textAlign: TextAlign.center,
              ),
              
              const Spacer(flex: 2),
              
              // Permission card
              Card(
                elevation: 2,
                child: Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: Column(
                    children: [
                      Icon(
                        Icons.security,
                        size: 48,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        '알림 권한이 필요합니다',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '앱이 금융 알림을 읽고 분석할 수 있도록 알림 접근 권한을 허용해주세요.',
                        style: Theme.of(context).textTheme.bodyMedium,
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                ),
              ),
              
              const Spacer(),
              
              // Continue button
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _requestPermissionAndContinue,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    foregroundColor: Theme.of(context).colorScheme.onPrimary,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: _isLoading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Text(
                          '권한 설정하고 시작하기',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                ),
              ),
              
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
  
  Future<void> _requestPermissionAndContinue() async {
    setState(() {
      _isLoading = true;
    });
    
    try {
      // 1. Request both permissions (POST_NOTIFICATIONS + NotificationListener)
      await NotificationService.instance.requestPermissions();
      
      // 2. Check all permissions with detailed status
      await _checkAndProcessPermissions();
      
    } catch (e) {
      // Handle error
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('권한 설정 중 오류가 발생했습니다: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _checkAndProcessPermissions() async {
    // 잠시 대기 후 권한 상태 확인 (사용자가 설정 완료할 시간)
    await Future.delayed(const Duration(milliseconds: 1000));
    
    final permissions = await NotificationService.instance.checkAllPermissions();
    
    if (permissions['allGranted'] == true) {
      // 모든 권한 허용됨 - WebView로 이동
      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (context) => const WebViewScreen()),
        );
      }
    } else {
      // 권한 상태에 따른 상세 안내
      if (mounted) {
        _showDetailedPermissionDialog(permissions);
      }
    }
  }
  
  void _showDetailedPermissionDialog(Map<String, bool> permissions) {
    final listenerEnabled = permissions['listenerEnabled'] ?? false;
    final notificationPermission = permissions['notificationPermission'] ?? false;
    
    String title = '권한 설정 필요';
    String message = '';
    
    if (!notificationPermission && !listenerEnabled) {
      message = '다음 두 권한이 모두 필요합니다:\n\n'
          '1. 푸시 알림 권한 (Android 13+)\n'
          '2. 알림 접근 권한\n\n'
          '설정에서 그만써! 앱의 권한을 허용해주세요.';
    } else if (!notificationPermission) {
      message = '푸시 알림 권한이 필요합니다.\n'
          '설정 > 앱 > 그만써! > 권한에서 알림을 허용해주세요.';
    } else if (!listenerEnabled) {
      message = '알림 접근 권한이 필요합니다.\n'
          '설정에서 그만써! 앱의 알림 접근을 허용해주세요.';
    }
    
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: Text(message),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: const Text('취소'),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
                _requestPermissionAndContinue();
              },
              child: const Text('다시 시도'),
            ),
          ],
        );
      },
    );
  }
}