class Transaction {
  final int? id;
  final String packageName;
  final String appName;
  final int amount;
  final String merchant;
  final String rawText;
  final DateTime timestamp;
  final String? transactionType;

  Transaction({
    this.id,
    required this.packageName,
    required this.appName,
    required this.amount,
    required this.merchant,
    required this.rawText,
    required this.timestamp,
    this.transactionType,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'packageName': packageName,
      'appName': appName,
      'amount': amount,
      'merchant': merchant,
      'rawText': rawText,
      'timestamp': timestamp.millisecondsSinceEpoch,
      'transactionType': transactionType,
    };
  }

  factory Transaction.fromMap(Map<String, dynamic> map) {
    return Transaction(
      id: map['id']?.toInt(),
      packageName: map['packageName'] ?? '',
      appName: map['appName'] ?? '',
      amount: map['amount']?.toInt() ?? 0,
      merchant: map['merchant'] ?? '',
      rawText: map['rawText'] ?? '',
      timestamp: DateTime.fromMillisecondsSinceEpoch(map['timestamp'] ?? 0),
      transactionType: map['transactionType'],
    );
  }

  factory Transaction.fromNotification(Map<String, dynamic> notification) {
    return Transaction(
      packageName: notification['packageName'] ?? '',
      appName: notification['appName'] ?? '',
      amount: notification['amount'] ?? 0,
      merchant: notification['merchant'] ?? '',
      rawText: notification['rawText'] ?? '',
      timestamp: DateTime.fromMillisecondsSinceEpoch(notification['timestamp'] ?? 0),
      transactionType: notification['transactionType'],
    );
  }

  @override
  String toString() {
    return 'Transaction{id: $id, packageName: $packageName, appName: $appName, amount: $amount, merchant: $merchant, transactionType: $transactionType, timestamp: $timestamp}';
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Transaction &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          packageName == other.packageName &&
          appName == other.appName &&
          amount == other.amount &&
          merchant == other.merchant &&
          transactionType == other.transactionType &&
          timestamp == other.timestamp;

  @override
  int get hashCode =>
      id.hashCode ^
      packageName.hashCode ^
      appName.hashCode ^
      amount.hashCode ^
      merchant.hashCode ^
      transactionType.hashCode ^
      timestamp.hashCode;
}