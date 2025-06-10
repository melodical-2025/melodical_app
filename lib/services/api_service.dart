// lib/services/api_service.dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/song.dart';
import 'token_storage.dart';

class ApiService {
  static const _baseUrl = 'http://10.0.2.2:8080';
  static final TokenStorage _ts = TokenStorage();

  /// 공통 GET
  static Future<http.Response> get(
      String path, {
        Map<String, String>? queryParameters,
      }) async {
    final token = await _ts.getToken();
    final uri = Uri.parse('$_baseUrl$path').replace(queryParameters: queryParameters);
    final headers = <String, String>{
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
    return http.get(uri, headers: headers);
  }

  /// 공통 POST
  static Future<http.Response> post(
      String path,
      Map<String, dynamic> body,
      ) async {
    final token = await _ts.getToken();
    final uri = Uri.parse('$_baseUrl$path');
    final headers = {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
    return http.post(uri, headers: headers, body: jsonEncode(body));
  }

  /// 회원가입
  static Future<http.Response> signup(
      String email,
      String password,
      String nickname,
      ) {
    return post('/auth/signup', {
      'email': email,
      'password': password,
      'nickname': nickname,
    });
  }

  /// 로그인
  static Future<http.Response> login(
      String email,
      String password,
      ) {
    return post('/auth/login', {
      'email': email,
      'password': password,
    });
  }

  /// 소셜 로그인
  static Future<http.Response> loginWithGoogle(String idToken) {
    return post('/auth/login/google', {'token': idToken});
  }
  static Future<http.Response> loginWithKakao(String accessToken) {
    return post('/auth/login/kakao', {'token': accessToken});
  }
  static Future<http.Response> loginWithNaver(String accessToken) {
    return post('/auth/login/naver', {'token': accessToken});
  }

  /// 1) top 차트 + 사용자 평점 함께 가져오기
  ///    (userId 를 getUserId() 로 토큰에서 꺼내옴)
  static Future<List<Song>> fetchTopMusic() async {
    final userId = await _ts.getUserId();
    if (userId == null) throw Exception('로그인 정보가 없습니다.');

    final resp = await get('/api/music/top', queryParameters: {
      'userId': userId.toString(),
    });

    if (resp.statusCode == 200) {
      final utf8body = utf8.decode(resp.bodyBytes);
      final List<dynamic> data = jsonDecode(utf8body);
      return data.map((e) => Song.fromJson(e)).toList();
    } else {
      throw Exception('곡 목록 불러오기 실패: ${resp.statusCode}');
    }
  }
  static Future<List<Song>> fetchRatedMusicByUser() async {
    final userId = await _ts.getUserId();
    if (userId == null) throw Exception('로그인 정보가 없습니다.');

    final resp = await get(
      '/api/music/rated',
      queryParameters: {'userId': userId.toString()},
    );

    if (resp.statusCode == 200) {
      final utf8body = utf8.decode(resp.bodyBytes);
      final List<dynamic> data = jsonDecode(utf8body);
      return data.map((e) => Song.fromJson(e)).toList();
    } else {
      throw Exception('평가된 곡 목록 불러오기 실패: ${resp.statusCode}');
    }
  }

  /// 2) 평점 저장
  static Future<void> rateMusic(List<Map<String, dynamic>> ratings)async {
    final userId = await _ts.getUserId();
    if (userId == null) throw Exception('로그인 정보가 없습니다.');

    final resp = await http.post(
      Uri.parse('$_baseUrl/api/music/rate'),
      headers: {
        'Content-Type': 'application/json',
        if (await _ts.getToken() != null)
          'Authorization': 'Bearer ${await _ts.getToken()}',
      },
      body: jsonEncode({
        'userId': userId,
        'type': 'music',
        'ratings': ratings,
      }),
    );
    if (resp.statusCode != 200) {
      throw Exception('평점 저장 실패: ${resp.statusCode}');
    }
  }

  static Future<void> rateBatchMusic(
      List<Map<String, dynamic>> ratingsPayload) async {
    final userId = await _ts.getUserId();
    if (userId == null) throw Exception('로그인 정보가 없습니다.');

    final resp = await post('/api/music/rate', {
      'userId': userId,
      'type': 'music',
      'ratings': ratingsPayload,
    });

    if (resp.statusCode != 200) {
      throw Exception('평점 일괄 저장 실패: ${resp.statusCode}');
    }
  }
}