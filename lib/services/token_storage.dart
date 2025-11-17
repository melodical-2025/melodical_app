import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

class TokenStorage {
  static const String _tokenKey = 'jwt_token';
  static const String _userIdKey = 'user_id';

  /// 토큰 가져오기
  Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  /// 토큰 저장
  Future<void> saveToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);

    // JWT에서 userId 추출 (선택적)
    try {
      final parts = token.split('.');
      if (parts.length == 3) {
        final payload = json.decode(
            utf8.decode(base64Url.decode(base64Url.normalize(parts[1])))
        );
        if (payload['userId'] != null) {
          await prefs.setInt(_userIdKey, payload['userId']);
        }
      }
    } catch (e) {
      print('토큰 파싱 실패: $e');
    }
  }

  /// 토큰 삭제
  Future<void> deleteToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_userIdKey);
  }

  /// 사용자 ID 가져오기
  Future<int?> getUserId() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getInt(_userIdKey);
  }

  /// 사용자 ID 저장
  Future<void> saveUserId(int userId) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_userIdKey, userId);
  }
}
