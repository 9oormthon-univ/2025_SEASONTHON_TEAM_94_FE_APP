import 'package:flutter/material.dart';
import 'screens/onboarding/simple_onboarding_screen.dart';
import 'screens/onboarding/slide_onboarding_screen.dart';
import 'screens/onboarding/korean_onboarding_screen.dart';

void main() {
  runApp(const StopUsingApp());
}

class StopUsingApp extends StatelessWidget {
  const StopUsingApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '그만써!',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF2E7D32),
          brightness: Brightness.light,
        ),
        useMaterial3: true,
      ),
      home: const KoreanOnboardingScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class TestScreen extends StatelessWidget {
  const TestScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('테스트 화면'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text(
              '앱이 정상적으로 실행되었습니다!',
              style: TextStyle(fontSize: 18),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => const SimpleOnboardingScreen(),
                  ),
                );
              },
              child: const Text('단순 온보딩'),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => const SlideOnboardingScreen(),
                  ),
                );
              },
              child: const Text('슬라이드 온보딩'),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => const KoreanOnboardingScreen(),
                  ),
                );
              },
              child: const Text('한국어 온보딩'),
            ),
          ],
        ),
      ),
    );
  }
}
