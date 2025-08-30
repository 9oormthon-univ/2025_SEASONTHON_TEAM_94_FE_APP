import 'package:flutter/material.dart';

class OnboardingIntroScreen extends StatelessWidget {
  final VoidCallback onNext;
  
  const OnboardingIntroScreen({super.key, required this.onNext});

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
                  '💳',
                  style: TextStyle(fontSize: 120),
                ),
              ),
              
              const SizedBox(height: 60),
              
              // 큰 타이틀
              const Text(
                '지출을 추적하고\n관리해보세요',
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
                '카드 알림과 출금 알림을 자동으로 분석해서\n고정지출과 초과지출을 구분해드려요.',
                style: TextStyle(
                  fontSize: 16,
                  height: 1.5,
                  color: Colors.grey[600],
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
                    '다음',
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