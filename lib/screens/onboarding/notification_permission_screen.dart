import 'package:flutter/material.dart';
import '../../services/notification_service.dart';
import '../webview_screen.dart';

class NotificationPermissionScreen extends StatefulWidget {
  const NotificationPermissionScreen({super.key});

  @override
  State<NotificationPermissionScreen> createState() => _NotificationPermissionScreenState();
}

class _NotificationPermissionScreenState extends State<NotificationPermissionScreen> {
  bool _isLoading = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(flex: 2),
              
              // 큰 이모티콘
              const Center(
                child: Text(
                  '🛡️',
                  style: TextStyle(fontSize: 120),
                ),
              ),
              
              const SizedBox(height: 60),
              
              // 큰 타이틀
              const Text(
                '알림 접근 권한이\n필요해요',
                style: TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  height: 1.3,
                  color: Color(0xFF191F28),
                ),
              ),
              
              const SizedBox(height: 20),
              
              // 보조 설명
              Text(
                '은행 앱의 출금 알림을 읽어서\n자동으로 지출을 분석해드려요.',
                style: TextStyle(
                  fontSize: 16,
                  height: 1.5,
                  color: Colors.grey[600],
                ),
              ),
              
              const SizedBox(height: 40),
              
              // 권한 설정 단계 안내
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: const Color(0xFFF8F9FA),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  children: [
                    _buildStepItem('1', '설정 화면에서 그만써! 찾기'),
                    const SizedBox(height: 12),
                    _buildStepItem('2', '알림 접근 허용 체크박스 선택'),
                    const SizedBox(height: 12),
                    _buildStepItem('3', '허용 버튼 터치'),
                  ],
                ),
              ),
              
              const Spacer(flex: 3),
              
              // 권한 설정 버튼
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _requestPermissionAndContinue,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF4E7DF5),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    elevation: 0,
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
                          '설정하러 가기',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                ),
              ),
              
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
  
  Widget _buildStepItem(String number, String text) {
    return Row(
      children: [
        Container(
          width: 24,
          height: 24,
          decoration: BoxDecoration(
            color: const Color(0xFF4E7DF5),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Center(
            child: Text(
              number,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 12,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            text,
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[700],
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ],
    );
  }
  
  Future<void> _requestPermissionAndContinue() async {
    setState(() {
      _isLoading = true;
    });
    
    try {
      // Request notification listener permission
      await NotificationService.instance.requestPermissions();
      
      // Wait a bit for user to potentially grant permission
      await Future.delayed(const Duration(seconds: 1));
      
      // Check if permission was granted
      final hasPermission = await NotificationService.instance.checkPermissions();
      
      if (hasPermission) {
        // Permission granted, navigate to webview
        if (mounted) {
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(builder: (context) => const WebViewScreen()),
          );
        }
      } else {
        // Show retry dialog
        if (mounted) {
          _showPermissionRequiredDialog();
        }
      }
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
  
  void _showPermissionRequiredDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: const Text(
            '권한 설정이 필요해요',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          content: const Text(
            '앱을 정상적으로 사용하려면 알림 접근 권한이 필요해요.\n\n설정에서 그만써! 앱의 알림 접근을 허용해주세요.',
            style: TextStyle(
              fontSize: 14,
              height: 1.4,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                // Navigate to webview anyway (user can set permission later)
                Navigator.of(context).pushReplacement(
                  MaterialPageRoute(builder: (context) => const WebViewScreen()),
                );
              },
              child: Text(
                '나중에 하기',
                style: TextStyle(
                  color: Colors.grey[600],
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
                _requestPermissionAndContinue();
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF4E7DF5),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                elevation: 0,
              ),
              child: const Text(
                '다시 설정하기',
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}