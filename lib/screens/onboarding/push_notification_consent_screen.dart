import 'package:flutter/material.dart';

class PushNotificationConsentScreen extends StatelessWidget {
  final VoidCallback onNext;
  
  const PushNotificationConsentScreen({super.key, required this.onNext});

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
                  '🔔',
                  style: TextStyle(fontSize: 120),
                ),
              ),
              
              const SizedBox(height: 60),
              
              // 큰 타이틀
              const Text(
                '푸시 알림을\n허용해주세요',
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
                '지출 내역 분석 결과와 중요한 알림을\n놓치지 않도록 도와드려요.',
                style: TextStyle(
                  fontSize: 16,
                  height: 1.5,
                  color: Colors.grey[600],
                ),
              ),
              
              const SizedBox(height: 40),
              
              // 알림 설정 안내
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: const Color(0xFFF8F9FA),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: const BoxDecoration(
                        color: Color(0xFF4E7DF5),
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        '지출 분류 결과를 즉시 확인할 수 있어요',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[700],
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              
              const Spacer(flex: 3),
              
              // 다음 버튼
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton(
                  onPressed: onNext,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF4E7DF5),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    elevation: 0,
                  ),
                  child: const Text(
                    '알림 허용하기',
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
}