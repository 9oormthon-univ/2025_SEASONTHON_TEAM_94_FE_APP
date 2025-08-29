class Transaction {
  final int? id;
  final String packageName;
  final String appName;
  final int amount;
  final String merchant;
  final String rawText;
  final DateTime timestamp;

  Transaction({
    this.id,
    required this.packageName,
    required this.appName,
    required this.amount,
    required this.merchant,
    required this.rawText,
    required this.timestamp,
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
    );
  }

  @override
  String toString() {
    return 'Transaction{id: $id, packageName: $packageName, appName: $appName, amount: $amount, merchant: $merchant, timestamp: $timestamp}';
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
          timestamp == other.timestamp;

  @override
  int get hashCode =>
      id.hashCode ^
      packageName.hashCode ^
      appName.hashCode ^
      amount.hashCode ^
      merchant.hashCode ^
      timestamp.hashCode;
}