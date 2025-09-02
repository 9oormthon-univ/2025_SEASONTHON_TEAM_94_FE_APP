import 'package:flutter/material.dart';
import '../../services/notification_service.dart';
import '../webview_screen.dart';

class KoreanOnboardingScreen extends StatefulWidget {
  const KoreanOnboardingScreen({super.key});

  @override
  State<KoreanOnboardingScreen> createState() => _KoreanOnboardingScreenState();
}

class _KoreanOnboardingScreenState extends State<KoreanOnboardingScreen> with WidgetsBindingObserver {
  final PageController _pageController = PageController();
  int _currentPage = 0;
  bool _isLoading = false;
  bool _isWaitingForPermission = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _pageController.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _isWaitingForPermission && _currentPage == 4) {
      _isWaitingForPermission = false;
      _checkNotificationAccessPermission();
    }
  }

  void _nextPage() {
    if (_currentPage < 4) {
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

  // 푸시 알림 권한 요청
  Future<void> _requestPushNotification() async {
    setState(() {
      _isLoading = true;
    });
    
    try {
      final bool granted = await NotificationService.instance.requestNotificationPermission();
      
      if (mounted) {
        if (granted) {
          _nextPage(); // 권한이 허용되면 다음 페이지로
        } else {
          // 권한이 거부되었을 때 필수 권한 안내
          _showRequiredPermissionDialog();
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('알림 권한 요청 중 오류: $e')),
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

  // 알림 접근 권한 요청
  Future<void> _requestNotificationAccess() async {
    setState(() {
      _isLoading = true;
    });
    
    try {
      _isWaitingForPermission = true;
      await NotificationService.instance.openNotificationListenerSettings();
      
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    } catch (e) {
      _isWaitingForPermission = false;
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('권한 설정 중 오류: $e')),
        );
        setState(() {
          _isLoading = false;
        });
      }
    }
  }
  
  // 화면으로 돌아왔을 때 권한 체크
  void _checkNotificationAccessPermission() async {
    final hasPermission = await NotificationService.instance.checkPermissions();
    
    if (mounted) {
      if (hasPermission) {
        // 권한이 있으면 WebView로 이동
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (context) => const WebViewScreen()),
        );
      } else {
        // 권한이 없으면 안내 다이얼로그
        _showPermissionDialog();
      }
    }
  }

  void _showRequiredPermissionDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('필수 권한 안내'),
        content: const Text('푸시 알림 권한은 앱 사용을 위해 필수입니다.\n설정에서 권한을 허용해주세요.'),
        actions: [
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop();
              _requestPushNotification();
            },
            child: const Text('다시 설정'),
          ),
        ],
      ),
    );
  }

  void _showPermissionDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('필수 권한 안내'),
        content: const Text('알림 접근 권한은 앱 사용을 위해 필수입니다.\n설정에서 그만써! 앱의 알림 접근을 허용해주세요.'),
        actions: [
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop();
              _requestNotificationAccess();
            },
            child: const Text('다시 설정'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Column(
          children: [
            // 상단 네비게이션 (뒤로가기, 페이지 인디케이터)
            Padding(
              padding: const EdgeInsets.all(24.0),
              child: Row(
                children: [
                  // 뒤로가기 버튼 (첫 번째 페이지가 아닐 때만)
                  if (_currentPage > 0)
                    IconButton(
                      onPressed: _previousPage,
                      icon: const Icon(Icons.arrow_back_ios, size: 20),
                      color: const Color(0xFF191F28),
                    )
                  else
                    const SizedBox(width: 48),
                    
                  const Spacer(),
                  
                  // 페이지 인디케이터 (처음 3개 페이지만)
                  if (_currentPage < 3)
                    Row(
                      children: List.generate(3, (index) {
                        return Container(
                          margin: const EdgeInsets.symmetric(horizontal: 4),
                          width: 8,
                          height: 8,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: index == _currentPage
                                ? const Color(0xFF191F28)
                                : const Color(0xFFE5E8EB),
                          ),
                        );
                      }),
                    )
                  else
                    const SizedBox(width: 8 * 3 + 4 * 2 * 3),
                  
                  const Spacer(),
                  const SizedBox(width: 48),
                ],
              ),
            ),
            
            // 페이지뷰
            Expanded(
              child: PageView(
                controller: _pageController,
                physics: _currentPage >= 3 ? const NeverScrollableScrollPhysics() : null,
                onPageChanged: (index) {
                  setState(() {
                    _currentPage = index;
                  });
                },
                children: [
                  _buildExplanationPage(
                    icon: Icons.credit_card,
                    title: '지출 알림이 올 때마다\n직접 관리할 돈인지 구분',
                    subtitle: '지출 알림을 자동으로 분석',
                    color: Colors.blue,
                  ),
                  _buildExplanationPage(
                    icon: Icons.lock,
                    title: '막연한 지출내역을\n내 의지로 온전하게 통제',
                    subtitle: '\'초과지출\'과 \'고정지출\'로 구분',
                    color: Colors.orange,
                  ),
                  _buildExplanationPage(
                    icon: Icons.account_balance_wallet,
                    title: '꼭 필요한 지출만 하도록\n그만써에서 간편하게 관리',
                    subtitle: '체감하지 못했던 진짜 지출을 확실하게',
                    color: Colors.green,
                  ),
                  _buildPermissionPage(
                    icon: Icons.notifications_active,
                    title: '푸시 알림을\n허용해주세요',
                    subtitle: '지출 내역 분석 결과와 중요한 알림을\n놓치지 않도록 도와드려요.',
                    bulletText: '알림에서 내가 통제할 지출로 분류할 수 있어요.',
                    buttonText: '알림 허용하기',
                    color: Colors.red,
                  ),
                  _buildPermissionPage(
                    icon: Icons.security,
                    title: '알림 접근 권한이\n필요해요',
                    subtitle: '은행 앱의 출금 알림을 읽어서\n자동으로 지출을 분석해드려요.',
                    bulletText: '',
                    buttonText: '설정하러 가기',
                    color: Colors.green,
                    showSteps: true,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildExplanationPage({
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32.0),
      child: Column(
        children: [
          const Spacer(flex: 2),
          
          // 아이콘
          Icon(
            icon,
            size: 100,
            color: color,
          ),
          
          const SizedBox(height: 60),
          
          // 제목
          Text(
            title,
            style: const TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              height: 1.3,
              color: Color(0xFF191F28),
            ),
            textAlign: TextAlign.center,
          ),
          
          const SizedBox(height: 16),
          
          // 부제목
          Text(
            subtitle,
            style: TextStyle(
              fontSize: 16,
              color: Colors.grey[600],
              fontWeight: FontWeight.w400,
            ),
            textAlign: TextAlign.center,
          ),
          
          const Spacer(flex: 3),
        ],
      ),
    );
  }
  
  Widget _buildPermissionPage({
    required IconData icon,
    required String title,
    required String subtitle,
    required String bulletText,
    required String buttonText,
    required Color color,
    bool showSteps = false,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32.0),
      child: Column(
        children: [
          const Spacer(flex: 2),
          
          // 아이콘
          Icon(
            icon,
            size: 100,
            color: color,
          ),
          
          const SizedBox(height: 60),
          
          // 제목
          Text(
            title,
            style: const TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              height: 1.3,
              color: Color(0xFF191F28),
            ),
            textAlign: TextAlign.center,
          ),
          
          const SizedBox(height: 16),
          
          // 부제목
          Text(
            subtitle,
            style: TextStyle(
              fontSize: 16,
              color: Colors.grey[600],
              fontWeight: FontWeight.w400,
            ),
            textAlign: TextAlign.center,
          ),
          
          const SizedBox(height: 40),
          
          // 권한 설정 안내
          if (bulletText.isNotEmpty)
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: const Color(0xFFFEF3E8),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: Color(0xFFEA580C),
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      bulletText,
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
          
          // 단계 안내 (알림 접근 권한 페이지)
          if (showSteps)
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
          
          const Spacer(flex: 2),
          
          // 버튼
          SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton(
              onPressed: _isLoading ? null : () {
                if (_currentPage < 3) {
                  _nextPage();
                } else if (_currentPage == 3) {
                  _requestPushNotification();
                } else {
                  _requestNotificationAccess();
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFEA580C),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                elevation: 0,
              ),
              child: _isLoading ? 
                const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.white,
                  ),
                ) :
                Text(
                  buttonText,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
            ),
          ),
          
          const SizedBox(height: 32),
        ],
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
            color: const Color(0xFFEA580C),
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
}