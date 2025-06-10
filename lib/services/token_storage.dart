// lib/services/token_storage.dart
import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  final _store = const FlutterSecureStorage();
  static const _key = 'jwt_token';

  /// JWT 자체를 읽어서 리턴
  Future<String?> getToken() async {
    return _store.read(key: _key);
  }

  Future<void> save(String token) async {
    await _store.write(key: _key, value: token);
  }

  Future<void> delete() async {
    await _store.delete(key: _key);
  }

  /// JWT 페이로드(=claims) 에서 userId(claim 이름이 "userId" 라고 가정)를 꺼내서 int 로 리턴
  Future<int?> getUserId() async {
    final token = await getToken();
    if (token == null) return null;
    final parts = token.split('.');
    if (parts.length != 3) return null;
    final payload = parts[1];
    final normalized = base64Url.normalize(payload);
    final decoded = utf8.decode(base64Url.decode(normalized));
    final Map<String, dynamic> map = jsonDecode(decoded);
    // 여기서 claim 이름이 실제 서버에서 넣어주는 이름과 일치해야 합니다.
    // 예를 들어 sub 대신 "userId" 로 넣어주셨다면 map['userId']:
    return map['userId'] is int
        ? map['userId']
        : int.tryParse(map['userId'].toString());
  }
}