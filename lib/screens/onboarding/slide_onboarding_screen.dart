import 'package:flutter/material.dart';

class SlideOnboardingScreen extends StatefulWidget {
  const SlideOnboardingScreen({super.key});

  @override
  State<SlideOnboardingScreen> createState() => _SlideOnboardingScreenState();
}

class _SlideOnboardingScreenState extends State<SlideOnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _nextPage() {
    if (_currentPage < 2) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  void _previousPage() {
    if (_currentPage > 0) {
      _pageController.previousPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('슬라이드 온보딩'),
      ),
      body: SafeArea(
        child: Column(
        children: [
          // 페이지 인디케이터
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(3, (index) {
                return Container(
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: index == _currentPage
                        ? Colors.blue
                        : Colors.grey[300],
                  ),
                );
              }),
            ),
          ),
          
          // 페이지뷰
          Expanded(
            child: PageView(
              controller: _pageController,
              onPageChanged: (index) {
                setState(() {
                  _currentPage = index;
                });
              },
              children: [
                _buildPage(
                  icon: Icons.credit_card,
                  title: '페이지 1',
                  subtitle: '첫 번째 화면',
                  color: Colors.red,
                ),
                _buildPage(
                  icon: Icons.lock,
                  title: '페이지 2', 
                  subtitle: '두 번째 화면',
                  color: Colors.orange,
                ),
                _buildPage(
                  icon: Icons.account_balance_wallet,
                  title: '페이지 3',
                  subtitle: '세 번째 화면',
                  color: Colors.green,
                ),
              ],
            ),
          ),
          
          // 네비게이션 버튼
          Padding(
            padding: const EdgeInsets.fromLTRB(16.0, 16.0, 16.0, 32.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                if (_currentPage > 0)
                  ElevatedButton(
                    onPressed: _previousPage,
                    child: const Text('이전'),
                  )
                else
                  const SizedBox(width: 60),
                  
                ElevatedButton(
                  onPressed: _currentPage < 2 ? _nextPage : () {
                    Navigator.of(context).pop();
                  },
                  child: Text(_currentPage < 2 ? '다음' : '완료'),
                ),
              ],
            ),
          ),
        ],
        ),
      ),
    );
  }
  
  Widget _buildPage({
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
  }) {
    return Padding(
      padding: const EdgeInsets.all(32.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            icon,
            size: 100,
            color: color,
          ),
          const SizedBox(height: 30),
          Text(
            title,
            style: const TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 15),
          Text(
            subtitle,
            style: const TextStyle(
              fontSize: 16,
              color: Colors.grey,
            ),
          ),
        ],
      ),
    );
  }
}