import 'package:flutter/material.dart';
import 'screens/onboarding/korean_onboarding_screen.dart';
import 'screens/webview_screen.dart';
import 'services/notification_service.dart';
import 'services/user_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  // Update userUid in native code on app start
  _updateUserUidOnStart();
  runApp(const StopUsingApp());
}

void _updateUserUidOnStart() async {
  try {
    await NotificationService.instance.updateUserUid();
  } catch (e) {
    // Handle error silently
  }
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
      home: const AppRouter(),
      debugShowCheckedModeBanner: false,
    );
  }
}

/// 로그인 이력 확인 후 적절한 화면으로 라우팅
class AppRouter extends StatefulWidget {
  const AppRouter({super.key});

  @override
  State<AppRouter> createState() => _AppRouterState();
}

class _AppRouterState extends State<AppRouter> {
  bool _isLoading = true;
  bool _hasLoginHistory = false;

  @override
  void initState() {
    super.initState();
    _checkLoginHistory();
  }

  /// 로그인 이력 확인
  Future<void> _checkLoginHistory() async {
    try {
      final hasUserUid = await UserService.instance.hasUserUid();
      setState(() {
        _hasLoginHistory = hasUserUid;
        _isLoading = false;
      });
    } catch (e) {
      // 에러 발생시 온보딩으로 이동
      setState(() {
        _hasLoginHistory = false;
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      // 로딩 화면
      return const Scaffold(
        backgroundColor: Colors.white,
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(
                color: Color(0xFF2E7D32),
              ),
              SizedBox(height: 20),
              Text(
                '그만써!',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF2E7D32),
                ),
              ),
            ],
          ),
        ),
      );
    }

    // 로그인 이력 있으면 홈 웹뷰, 없으면 온보딩
    return _hasLoginHistory 
        ? const WebViewScreen(initialPath: '/home')
        : const KoreanOnboardingScreen();
  }
}

