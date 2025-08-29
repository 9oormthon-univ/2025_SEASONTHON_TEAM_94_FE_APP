import 'dart:math';
import '../models/transaction.dart';
import 'database_service.dart';

class TestService {
  static const List<String> _testMerchants = [
    '스타벅스 강남점',
    '맥도날드 홍대점',
    '편의점 GS25',
    '교보문고',
    '올리브영',
    '네이버페이',
    '쿠팡이츠',
    '배달의민족',
    '카카오택시',
    '지하철 교통카드',
    '커피빈 신사점',
    '이마트 트레이더스',
    '다이소 명동점',
    '롯데시네마',
    'CGV 강남점'
  ];

  static const List<String> _testApps = [
    '토스',
    'KB국민은행',
    '신한은행',
    '카카오페이',
    '우리은행',
    '하나은행',
    '페이코'
  ];

  static const List<String> _testPackages = [
    'viva.republica.toss',
    'com.kbstar.kbbank',
    'com.shinhan.sbanking',
    'com.kakao.talk',
    'com.wooribank.smart.npib',
    'com.kebhana.hanapush',
    'com.nhn.android.payapp'
  ];

  final DatabaseService _databaseService = DatabaseService();
  final Random _random = Random();

  /// 랜덤한 테스트 거래 생성
  Transaction generateRandomTransaction() {
    final merchantIndex = _random.nextInt(_testMerchants.length);
    final appIndex = _random.nextInt(_testApps.length);
    final amount = (_random.nextInt(50) + 1) * 1000; // 1,000원 ~ 50,000원
    final hoursAgo = _random.nextInt(72); // 최근 3일 내
    
    return Transaction(
      packageName: _testPackages[appIndex],
      appName: _testApps[appIndex],
      amount: amount,
      merchant: _testMerchants[merchantIndex],
      rawText: '${_testApps[appIndex]} 결제알림\n${_testMerchants[merchantIndex]} ${amount.toString().replaceAllMapped(RegExp(r'(\d)(?=(\d{3})+(?!\d))'), (Match m) => '${m[1]},')}원 결제완료',
      timestamp: DateTime.now().subtract(Duration(hours: hoursAgo)),
    );
  }

  /// 여러 개의 테스트 거래 생성
  List<Transaction> generateMultipleTransactions(int count) {
    return List.generate(count, (index) => generateRandomTransaction());
  }

  /// 데이터베이스에 테스트 데이터 추가
  Future<void> addTestTransactions(int count) async {
    final transactions = generateMultipleTransactions(count);
    for (final transaction in transactions) {
      await _databaseService.insertTransaction(transaction);
    }
  }

  /// 오늘의 테스트 거래 추가
  Future<void> addTodayTestTransactions() async {
    final todayTransactions = List.generate(5, (index) {
      final merchantIndex = _random.nextInt(_testMerchants.length);
      final appIndex = _random.nextInt(_testApps.length);
      final amount = (_random.nextInt(30) + 1) * 1000; // 1,000원 ~ 30,000원
      final minutesAgo = _random.nextInt(720); // 오늘 12시간 내
      
      return Transaction(
        packageName: _testPackages[appIndex],
        appName: _testApps[appIndex],
        amount: amount,
        merchant: _testMerchants[merchantIndex],
        rawText: '${_testApps[appIndex]} 결제알림\n${_testMerchants[merchantIndex]} ${amount.toString().replaceAllMapped(RegExp(r'(\d)(?=(\d{3})+(?!\d))'), (Match m) => '${m[1]},')}원 결제완료',
        timestamp: DateTime.now().subtract(Duration(minutes: minutesAgo)),
      );
    });

    for (final transaction in todayTransactions) {
      await _databaseService.insertTransaction(transaction);
    }
  }

  /// 실시간 테스트 거래 시뮬레이션
  Future<Transaction> simulateRealTimeTransaction() async {
    final transaction = Transaction(
      packageName: _testPackages[_random.nextInt(_testPackages.length)],
      appName: _testApps[_random.nextInt(_testApps.length)],
      amount: (_random.nextInt(20) + 1) * 1000,
      merchant: _testMerchants[_random.nextInt(_testMerchants.length)],
      rawText: '실시간 테스트 거래',
      timestamp: DateTime.now(),
    );

    await _databaseService.insertTransaction(transaction);
    return transaction;
  }

  /// 특정 앱의 테스트 거래 생성
  Future<Transaction> createTransactionForApp(String appName) async {
    final appIndex = _testApps.indexOf(appName);
    if (appIndex == -1) return generateRandomTransaction();

    final transaction = Transaction(
      packageName: _testPackages[appIndex],
      appName: _testApps[appIndex],
      amount: (_random.nextInt(50) + 1) * 1000,
      merchant: _testMerchants[_random.nextInt(_testMerchants.length)],
      rawText: '$appName 테스트 거래',
      timestamp: DateTime.now(),
    );

    await _databaseService.insertTransaction(transaction);
    return transaction;
  }

  /// 대용량 테스트 데이터 생성 (성능 테스트용)
  Future<void> generateBulkTestData() async {
    // 지난 30일간의 거래 데이터 생성
    for (int day = 0; day < 30; day++) {
      final transactionsPerDay = _random.nextInt(10) + 1; // 1-10개 거래/일
      
      for (int i = 0; i < transactionsPerDay; i++) {
        final merchantIndex = _random.nextInt(_testMerchants.length);
        final appIndex = _random.nextInt(_testApps.length);
        final amount = (_random.nextInt(100) + 1) * 1000;
        final hoursOffset = _random.nextInt(24);
        
        final transaction = Transaction(
          packageName: _testPackages[appIndex],
          appName: _testApps[appIndex],
          amount: amount,
          merchant: _testMerchants[merchantIndex],
          rawText: '${_testApps[appIndex]} 결제알림',
          timestamp: DateTime.now()
              .subtract(Duration(days: day))
              .subtract(Duration(hours: hoursOffset)),
        );

        await _databaseService.insertTransaction(transaction);
      }
    }
  }
}