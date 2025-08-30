import 'package:flutter/material.dart';
import 'onboarding_intro_screen.dart';
import 'push_notification_consent_screen.dart';
import 'notification_permission_screen.dart';

class OnboardingFlowScreen extends StatefulWidget {
  const OnboardingFlowScreen({super.key});

  @override
  State<OnboardingFlowScreen> createState() => _OnboardingFlowScreenState();
}

class _OnboardingFlowScreenState extends State<OnboardingFlowScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _nextPage() {
    if (_currentPage < 2) {
      setState(() {
        _currentPage++;
      });
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Column(
        children: [
          // 진행 상황 표시
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(24.0),
              child: Row(
                children: [
                  // 뒤로가기 버튼 (첫 번째 페이지가 아닐 때만 표시)
                  if (_currentPage > 0)
                    IconButton(
                      onPressed: () {
                        if (_currentPage > 0) {
                          setState(() {
                            _currentPage--;
                          });
                          _pageController.previousPage(
                            duration: const Duration(milliseconds: 300),
                            curve: Curves.easeInOut,
                          );
                        }
                      },
                      icon: const Icon(Icons.arrow_back_ios, size: 20),
                      color: const Color(0xFF191F28),
                    )
                  else
                    const SizedBox(width: 48), // 공간 확보
                    
                  const Spacer(),
                  
                  // 페이지 인디케이터
                  Row(
                    children: List.generate(3, (index) {
                      return Container(
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: index == _currentPage
                              ? const Color(0xFF4E7DF5)
                              : const Color(0xFFE5E8EB),
                        ),
                      );
                    }),
                  ),
                  
                  const Spacer(),
                  const SizedBox(width: 48), // 대칭을 위한 공간
                ],
              ),
            ),
          ),
          
          // 온보딩 페이지들
          Expanded(
            child: PageView(
              controller: _pageController,
              onPageChanged: (index) {
                setState(() {
                  _currentPage = index;
                });
              },
              children: [
                OnboardingIntroScreen(onNext: _nextPage),
                PushNotificationConsentScreen(onNext: _nextPage),
                const NotificationPermissionScreen(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}