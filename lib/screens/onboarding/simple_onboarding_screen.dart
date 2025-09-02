import 'package:flutter/material.dart';

class SimpleOnboardingScreen extends StatelessWidget {
  const SimpleOnboardingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('온보딩 테스트'),
      ),
      body: const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.check_circle,
              size: 100,
              color: Colors.green,
            ),
            SizedBox(height: 20),
            Text(
              '온보딩 화면 테스트',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
            SizedBox(height: 10),
            Text(
              '이 화면이 표시되면 성공입니다!',
              style: TextStyle(fontSize: 16),
            ),
          ],
        ),
      ),
    );
  }
}