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
        // 권한이 있으면 인증 웹뷰로 이동
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (context) => const WebViewScreen(initialPath: '/auth')),
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
            // 상단 페이지 인디케이터
            Padding(
              padding: const EdgeInsets.all(24.0),
              child: Center(
                child: 
                  // 페이지 인디케이터 (처음 3개 페이지만)
                  _currentPage < 3
                    ? Row(
                        mainAxisAlignment: MainAxisAlignment.center,
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
                    : const SizedBox.shrink(),
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
                    title: '지출 알림이 올 때마다\n실질적으로 관리할 돈인지 구분',
                    subtitle: '지출 알림을 자동으로 분석',
                    images: [
                      'assets/images/page1-card-front-color.png',        // 좌상단
                      'assets/images/page1-dollar-front-color.png',      // 우상단
                      'assets/images/page1-dollar-front-color-1.png',    // 좌하단
                      'assets/images/page1-money-front-color.png',       // 우하단
                    ],
                  ),
                  _buildExplanationPage(
                    title: '막연한 지출내역을\n내 의지로 온전하게 통제',
                    subtitle: '\'초과지출\'과 \'고정지출\'로 구분',
                    images: [
                      'assets/images/page2-lock-front-color.png',
                      'assets/images/page2-key-front-color.png',
                    ],
                  ),
                  _buildExplanationPage(
                    title: '꼭 필요한 지출만 하도록\n그만써에서 간편하게 관리',
                    subtitle: '체감하지 못했던 진짜 지출을 확실하게',
                    images: [
                      'assets/images/page3-wallet-front-color.png',
                    ],
                  ),
                  _buildPermissionPage(
                    imagePath: 'assets/images/push-bell-front-color.png',
                    title: '푸시 알림을\n허용해주세요',
                    subtitle: '지출 내역 분석 결과와 중요한 알림을\n놓치지 않도록 도와드려요.',
                    bulletText: '알림에서 내가 통제할 지출로 분류할 수 있어요.',
                    buttonText: '알림 허용하기',
                  ),
                  _buildPermissionPage(
                    imagePath: 'assets/images/push-sheild-front-color.png',
                    title: '알림 접근 권한이\n필요해요',
                    subtitle: '은행 앱의 출금 알림을 읽어서\n자동으로 지출을 분석해드려요.',
                    bulletText: '',
                    buttonText: '알림 허용하기',
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
    required String title,
    required String subtitle,
    required List<String> images,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32.0),
      child: Column(
        children: [
          const SizedBox(height: 60),
          
          // 제목 (상단에 위치)
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
          
          // 부제목 (제목 바로 아래)
          Text(
            subtitle,
            style: TextStyle(
              fontSize: 16,
              color: Colors.grey[600],
              fontWeight: FontWeight.w400,
            ),
            textAlign: TextAlign.center,
          ),
          
          const Spacer(flex: 1),
          
          // 이미지들 조합 (중앙에 위치)
          SizedBox(
            height: 300,
            child: _buildImageComposition(images),
          ),
          
          const Spacer(flex: 1),
          
          // 다음 버튼
          SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton(
              onPressed: () {
                if (_currentPage < 2) {
                  _nextPage();
                } else if (_currentPage == 2) {
                  _nextPage(); // 권한 페이지로 이동
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFF6200),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                elevation: 0,
              ),
              child: Text(
                _currentPage < 2 ? '다음' : '지출 번화가 기대돼요',
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
  
  Widget _buildPermissionPage({
    required String imagePath,
    required String title,
    required String subtitle,
    required String bulletText,
    required String buttonText,
    bool showSteps = false,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32.0),
      child: Column(
        children: [
          const SizedBox(height: 60),
          
          // 이미지 (중앙 위쪽)
          Image.asset(
            imagePath,
            width: 200,
            height: 200,
            fit: BoxFit.contain,
          ),
          
          const SizedBox(height: 80),
          
          // 제목 (좌측 정렬)
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 26,
                fontWeight: FontWeight.bold,
                height: 1.3,
                color: Color(0xFF191F28),
              ),
              textAlign: TextAlign.left,
            ),
          ),
          
          const SizedBox(height: 16),
          
          // 부제목 (좌측 정렬)
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              subtitle,
              style: TextStyle(
                fontSize: 16,
                color: Colors.grey[600],
                fontWeight: FontWeight.w400,
              ),
              textAlign: TextAlign.left,
            ),
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
                        fontSize: 12,  // 14에서 12로 줄임
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
                color: const Color(0xFFFFEDE4),
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
                if (_currentPage < 2) {
                  _nextPage();
                } else if (_currentPage == 2) {
                  // 마지막 설명 페이지에서 권한 페이지로 이동
                  _nextPage();
                } else if (_currentPage == 3) {
                  _requestPushNotification();
                } else {
                  _requestNotificationAccess();
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFFF6200),
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
                  _currentPage < 2 ? '다음' : _currentPage == 2 ? '지출 번화가 기대돼요' : buttonText,
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
  
  Widget _buildImageComposition(List<String> images) {
    if (images.length == 1) {
      // Page 3: 스크린샷과 정확히 일치하는 지갑 크기와 위치
      return Center(
        child: Image.asset(
          images[0], // wallet
          width: 200,
          height: 160,
          fit: BoxFit.contain,
        ),
      );
    } else if (images.length == 2) {
      // Page 2: 자물쇠와 열쇠를 훨씬 멀리 떨어뜨림
      return Stack(
        children: [
          // 좌측: lock-front-color (자물쇠를 더 왼쪽으로)
          Positioned(
            left: -20,
            top: 20,
            child: Image.asset(
              images[0], // lock
              width: 270, // 180 * 1.5
              height: 300, // 200 * 1.5
              fit: BoxFit.contain,
            ),
          ),
          // 우측 하단: key-front-color (열쇠를 훨씬 더 오른쪽으로)
          Positioned(
            right: -30,
            bottom: 10,
            child: Transform.rotate(
              angle: -0.3, // 열쇠가 약간 기울어져 있음
              child: Image.asset(
                images[1], // key
                width: 140,
                height: 140,
                fit: BoxFit.contain,
              ),
            ),
          ),
        ],
      );
    } else if (images.length >= 4) {
      // Page 1: 카드 이미지들 1.5배 크기 증가
      return Stack(
        children: [
          // 좌상단: card-front-color (1.5배 더 큰 빨간 카드 - 더 왼쪽으로)
          Positioned(
            top: 10,
            left: -30,
            child: Transform.rotate(
              angle: -0.15, // 약간 기울어진 각도
              child: Image.asset(
                images[0], // card-front-color
                width: 280, // 조금 줄임
                height: 180, // 조금 줄임
                fit: BoxFit.contain,
              ),
            ),
          ),
          // 우상단: dollar-front-color (달러 심볼)
          Positioned(
            top: 20,
            right: 20,
            child: Image.asset(
              images[1], // dollar-front-color
              width: 80,
              height: 80,
              fit: BoxFit.contain,
            ),
          ),
          // 우하단: money-front-color (1.5배 더 큰 청록색 카드 - 더 오른쪽으로)
          Positioned(
            bottom: 20,
            right: -10,
            child: Transform.rotate(
              angle: 0.1, // 살짝 반대 방향 기울임
              child: Image.asset(
                images[3], // money-front-color
                width: 250, // 조금 줄임
                height: 160, // 조금 줄임
                fit: BoxFit.contain,
              ),
            ),
          ),
          // 좌하단: dollar-front-color-1 (달러 심볼) - 조금만 가까이
          Positioned(
            bottom: 30,
            left: 50, // 20에서 조금만 오른쪽으로
            child: Image.asset(
              images[2], // dollar-front-color-1
              width: 75,
              height: 75,
              fit: BoxFit.contain,
            ),
          ),
        ],
      );
    }
    
    // Fallback
    return Container();
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