import 'package:shared_preferences/shared_preferences.dart';

class UserService {
  static UserService? _instance;
  static UserService get instance {
    _instance ??= UserService._();
    return _instance!;
  }

  UserService._();

  static const String _userUidKey = 'userUid';

  /// Save user UID
  Future<void> saveUserUid(String userUid) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_userUidKey, userUid);
    } catch (e) {
      // Handle error silently
    }
  }

  /// Get user UID, returns 'a' as default if not set
  Future<String> getUserUid() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getString(_userUidKey) ?? 'a';
    } catch (e) {
      // Return default if error occurs
      return 'a';
    }
  }

  /// Check if user UID is set
  Future<bool> hasUserUid() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.containsKey(_userUidKey);
    } catch (e) {
      return false;
    }
  }

  /// Clear user UID
  Future<void> clearUserUid() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_userUidKey);
    } catch (e) {
      // Handle error silently
    }
  }
}