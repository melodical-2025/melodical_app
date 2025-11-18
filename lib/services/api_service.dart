// lib/services/api_service.dart
import 'dart:convert';
import 'dart:async';
import 'package:http/http.dart' as http;
import '../models/musical.dart';
import '../models/song.dart';
import '../models/api_exception.dart';
import '../config/api_config.dart';
import 'token_storage.dart';

class ApiService {
  static final TokenStorage _ts = TokenStorage();

  static Future<String?> getToken() async {
    return await _ts.getToken();
  }

  static Future<void> saveToken(String token) async {
    await _ts.saveToken(token);
  }

  static Future<void> deleteToken() async {
    await _ts.deleteToken();
  }

  /// HTTP 응답 처리 및 에러 변환
  static void _handleResponseErrors(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return; // 성공
    }

    // 에러 응답 처리
    String? errorMessage;
    try {
      final jsonBody = jsonDecode(utf8.decode(response.bodyBytes));
      errorMessage = jsonBody['message'] ?? jsonBody['error'];
    } catch (_) {
      // JSON 파싱 실패시 기본 메시지 사용
    }

    throw ApiException.fromStatusCode(
      response.statusCode,
      customMessage: errorMessage,
    );
  }

  /// 공통 GET
  static Future<http.Response> get(String path, {
    Map<String, String>? queryParameters,
  }) async {
    try {
      final token = await _ts.getToken();
      final uri = Uri.parse('${ApiConfig.baseUrl}$path').replace(
          queryParameters: queryParameters);
      final headers = <String, String>{
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      };
      
      final response = await http
          .get(uri, headers: headers)
          .timeout(Duration(seconds: ApiConfig.connectTimeout));
      
      _handleResponseErrors(response);
      return response;
    } on TimeoutException {
      throw ApiException.timeout();
    } on http.ClientException {
      throw ApiException.networkError();
    } on ApiException {
      rethrow;
    } catch (e) {
      throw ApiException.unknown(e.toString());
    }
  }

  /// 공통 POST
  static Future<http.Response> post(String path,
      Map<String, dynamic> body,) async {
    try {
      final token = await _ts.getToken();
      final uri = Uri.parse('${ApiConfig.baseUrl}$path');
      final headers = {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      };
      
      final response = await http
          .post(uri, headers: headers, body: jsonEncode(body))
          .timeout(Duration(seconds: ApiConfig.connectTimeout));
      
      _handleResponseErrors(response);
      return response;
    } on TimeoutException {
      throw ApiException.timeout();
    } on http.ClientException {
      throw ApiException.networkError();
    } on ApiException {
      rethrow;
    } catch (e) {
      throw ApiException.unknown(e.toString());
    }
  }

  /// 공통 PUT
  static Future<http.Response> put(String path, dynamic body) async {
    try {
      final token = await _ts.getToken();
      final uri = Uri.parse('${ApiConfig.baseUrl}$path');
      final headers = {
        'Content-Type': 'application/json; charset=UTF-8',
        if (token != null) 'Authorization': 'Bearer $token',
      };
      
      final response = await http
          .put(
            uri,
            headers: headers,
            body: body is String ? body : jsonEncode(body),
          )
          .timeout(Duration(seconds: ApiConfig.connectTimeout));
      
      _handleResponseErrors(response);
      return response;
    } on TimeoutException {
      throw ApiException.timeout();
    } on http.ClientException {
      throw ApiException.networkError();
    } on ApiException {
      rethrow;
    } catch (e) {
      throw ApiException.unknown(e.toString());
    }
  }

  /// 공통 DELETE
  static Future<http.Response> delete(String path) async {
    try {
      final token = await _ts.getToken();
      final uri = Uri.parse('${ApiConfig.baseUrl}$path');
      final headers = {
        'Content-Type': 'application/json; charset=UTF-8',
        if (token != null) 'Authorization': 'Bearer $token',
      };
      
      final response = await http
          .delete(uri, headers: headers)
          .timeout(Duration(seconds: ApiConfig.connectTimeout));
      
      _handleResponseErrors(response);
      return response;
    } on TimeoutException {
      throw ApiException.timeout();
    } on http.ClientException {
      throw ApiException.networkError();
    } on ApiException {
      rethrow;
    } catch (e) {
      throw ApiException.unknown(e.toString());
    }
  }
  /// 회원가입
  static Future<http.Response> signup(String email,
      String password,
      String nickname,) {
    return post('/auth/signup', {
      'email': email,
      'password': password,
      'nickname': nickname,
    });
  }

  /// 로그인
  static Future<http.Response> login(String email,
      String password,) {
    return post('/auth/login', {
      'email': email,
      'password': password,
    });
  }

  /// 닉네임 업데이트
  static Future<void> updateNickname(String nickname) async {
    await put('/api/users/nickname', {'nickname': nickname});
  }

  /// 현재 사용자 정보 조회
  static Future<Map<String, dynamic>> getCurrentUser() async {
    final resp = await get('/api/users/me');
    return jsonDecode(utf8.decode(resp.bodyBytes));
  }

  /// 소셜 로그인
  static Future<http.Response> loginWithGoogle(String idToken) {
    return post('/auth/oauth2/google', {'idToken': idToken});
  }

  static Future<http.Response> loginWithKakao(String accessToken) {
    return post('/auth/oauth2/kakao', {'accessToken': accessToken});
  }

  static Future<http.Response> loginWithNaver(String accessToken) {
    return post('/auth/oauth2/naver', {'accessToken': accessToken});
  }

  /// 1) top 차트 + 사용자 평점 함께 가져오기
  ///    (userId 를 getUserId() 로 토큰에서 꺼내옴)
  static Future<List<Song>> fetchTopMusic() async {
    final userId = await _ts.getUserId();
    if (userId == null) {
      throw ApiException(message: '로그인 정보가 없습니다.');
    }

    final resp = await get('/api/music/top', queryParameters: {
      'userId': userId.toString(),
    });

    final utf8body = utf8.decode(resp.bodyBytes);
    final List<dynamic> data = jsonDecode(utf8body);
    return data.map((e) => Song.fromJson(e)).toList();
  }

  static Future<List<Song>> fetchRatedMusicByUser() async {
    final userId = await _ts.getUserId();
    if (userId == null) {
      throw ApiException(message: '로그인 정보가 없습니다.');
    }

    final resp = await get(
      '/api/music/rated',
      queryParameters: {'userId': userId.toString()},
    );

    final utf8body = utf8.decode(resp.bodyBytes);
    final List<dynamic> data = jsonDecode(utf8body);
    return data.map((e) => Song.fromJson(e)).toList();
  }

  /// 2) 평점 저장
  static Future<void> rateMusic(List<Map<String, dynamic>> ratings) async {
    final userId = await _ts.getUserId();
    if (userId == null) {
      throw ApiException(message: '로그인 정보가 없습니다.');
    }

    await post('/api/music/rate', {
      'userId': userId,
      'type': 'music',
      'ratings': ratings,
    });
  }

  static Future<void> rateBatchMusic(
      List<Map<String, dynamic>> ratingsPayload) async {
    final userId = await _ts.getUserId();
    if (userId == null) {
      throw ApiException(message: '로그인 정보가 없습니다.');
    }

    await post('/api/music/rate', {
      'userId': userId,
      'type': 'music',
      'ratings': ratingsPayload,
    });
  }

  /// 전체 뮤지컬 목록 불러오기
  static Future<List<Musical>> fetchAllMusicals() async {
    final resp = await get('/api/musicals/fetch');
    final List<dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
    return data.map((e) => Musical.fromJson(e)).toList();
  }

  static Future<List<Musical>> fetchRatedMusicals() async {
    final userId = await _ts.getUserId();
    if (userId == null) {
      throw ApiException(message: '로그인 정보가 없습니다.');
    }

    final resp = await get('/api/musicals/rated', queryParameters: {
      'userId': userId.toString(),
    });
    final List<dynamic> list = jsonDecode(utf8.decode(resp.bodyBytes));
    return list.map((e) => Musical.fromJson(e)).toList();
  }

  /// 뮤지컬 평점 일괄 저장
  static Future<void> rateBatchMusical(List<Map<String, dynamic>> ratings) async {
    await post('/api/musicals/rate/batch', {'ratings': ratings});
  }
  
  /// 내가 평가한 뮤지컬 평점 목록 조회
  static Future<Map<int, double>> getMyRatings() async {
    final resp = await get('/api/musicals/ratings/my');
    final Map<String, dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
    // String 키를 int로 변환
    return data.map((key, value) => MapEntry(int.parse(key), (value as num).toDouble()));
  }

  /// ========== 찜 관련 API ==========

  /// 찜 추가
  static Future<void> addFavorite(int musicalId) async {
    await post('/api/favorites', {'musicalId': musicalId});
  }

  /// 찜 취소
  static Future<void> removeFavorite(int musicalId) async {
    final resp = await delete('/api/favorites/$musicalId');
  }

  /// 찜 토글 (있으면 취소, 없으면 추가)
  static Future<bool> toggleFavorite(int musicalId) async {
    final resp = await post('/api/favorites/toggle', {'musicalId': musicalId});
    final data = jsonDecode(utf8.decode(resp.bodyBytes));
    return data['isFavorite'] as bool;
  }

  /// 내 찜 목록 조회
  static Future<List<Map<String, dynamic>>> fetchLikedMusicals() async {
    final resp = await get('/api/favorites/my');
    final List<dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
    return data.cast<Map<String, dynamic>>();
  }

  /// 찜 여부 확인
  static Future<bool> checkFavorite(int musicalId) async {
    final resp = await get('/api/favorites/check/$musicalId');
    final data = jsonDecode(utf8.decode(resp.bodyBytes));
    return data['isFavorite'] as bool;
  }
  
  /// 찜 상태 확인 (checkFavorite의 별칭)
  static Future<bool> checkFavoriteStatus(int musicalId) async {
    return checkFavorite(musicalId);
  }

  /// 찜 개수 조회
  static Future<int> getFavoriteCount() async {
    final resp = await get('/api/favorites/count');
    final data = jsonDecode(utf8.decode(resp.bodyBytes));
    return data['count'] as int;
  }

  /// 댓글 좋아요 토글 (있으면 취소, 없으면 추가)
  static Future<bool> toggleCommentLike(int commentId) async {
    final resp = await post('/api/comments/$commentId/like', {});
    return jsonDecode(utf8.decode(resp.bodyBytes)) as bool;
  }

  /// 댓글 좋아요 여부 확인
  static Future<bool> isCommentLiked(int commentId) async {
    final resp = await get('/api/comments/$commentId/liked');
    return jsonDecode(utf8.decode(resp.bodyBytes)) as bool;
  }

  /// 댓글 좋아요 개수 조회
  static Future<int> getCommentLikeCount(int commentId) async {
    final resp = await get('/api/comments/$commentId/like-count');
    return jsonDecode(utf8.decode(resp.bodyBytes)) as int;
  }

  /// ========== 앱용 통합 API ==========

  // 캐싱을 위한 변수들
  static List<Map<String, dynamic>>? _cachedMonthlyMusicals;
  static List<Map<String, dynamic>>? _cachedWeeklyMusicals;
  static DateTime? _lastMonthlyFetch;
  static DateTime? _lastWeeklyFetch;
  static const Duration _cacheExpiration = Duration(minutes: 10);

  /// 월간 인기 뮤지컬 목록 (integrated_monthly_dataset) - 캐싱 적용
  static Future<List<Map<String, dynamic>>> fetchMonthlyMusicals({bool forceRefresh = false}) async {
    // 캐시가 있고 유효하면 캐시 반환
    if (!forceRefresh && 
        _cachedMonthlyMusicals != null && 
        _lastMonthlyFetch != null &&
        DateTime.now().difference(_lastMonthlyFetch!) < _cacheExpiration) {
      print('📦 Using cached monthly musicals (${_cachedMonthlyMusicals!.length} items)');
      return _cachedMonthlyMusicals!;
    }
    
    print('🔄 Fetching fresh monthly musicals from API...');
    final resp = await get('/api/app/musicals/monthly');
    final List<dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
    _cachedMonthlyMusicals = data.cast<Map<String, dynamic>>();
    _lastMonthlyFetch = DateTime.now();
    print('✅ Cached ${_cachedMonthlyMusicals!.length} monthly musicals');
    return _cachedMonthlyMusicals!;
  }

  /// 주간 인기 뮤지컬 목록 (integrated_weekly_dataset) - 캐싱 적용
  static Future<List<Map<String, dynamic>>> fetchWeeklyMusicals({bool forceRefresh = false}) async {
    // 캐시가 있고 유효하면 캐시 반환
    if (!forceRefresh && 
        _cachedWeeklyMusicals != null && 
        _lastWeeklyFetch != null &&
        DateTime.now().difference(_lastWeeklyFetch!) < _cacheExpiration) {
      print('📦 Using cached weekly musicals (${_cachedWeeklyMusicals!.length} items)');
      return _cachedWeeklyMusicals!;
    }
    
    print('🔄 Fetching fresh weekly musicals from API...');
    final resp = await get('/api/app/musicals/weekly');
    final List<dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
    _cachedWeeklyMusicals = data.cast<Map<String, dynamic>>();
    _lastWeeklyFetch = DateTime.now();
    print('✅ Cached ${_cachedWeeklyMusicals!.length} weekly musicals');
    return _cachedWeeklyMusicals!;
  }
  
  /// 캐시 초기화
  static void clearCache() {
    _cachedMonthlyMusicals = null;
    _cachedWeeklyMusicals = null;
    _lastMonthlyFetch = null;
    _lastWeeklyFetch = null;
    print('🗑️ Cache cleared');
  }

  /// 월간 인기 Top N
  static Future<List<Map<String, dynamic>>> fetchTopMonthlyMusicals(int count) async {
    final resp = await get('/api/app/musicals/monthly/top/$count');
    final List<dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
    return data.cast<Map<String, dynamic>>();
  }

  /// 통합 검색 (KOPIS + Crawled Data)
  static Future<List<Map<String, dynamic>>> searchMusicals(String query, {int limit = 50}) async {
    final resp = await get('/api/app/musicals/search', queryParameters: {
      'query': query,
      'limit': limit.toString(),
    });
    final List<dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
    return data.cast<Map<String, dynamic>>();
  }

  /// 사용자 맞춤 추천
  static Future<List<Map<String, dynamic>>> getRecommendations(int userId, {
    String surface = 'home',
    int count = 20,
    String? region,
  }) async {
    print('📡 API: Calling recommendations endpoint for userId=$userId, surface=$surface, count=$count');
    final requestBody = {
      'userId': userId,
      'surface': surface,
      'count': count,
      if (region != null) 'region': region,
    };
    print('📡 API: Request body: $requestBody');
    
    final resp = await post('/api/app/recommendations', requestBody);
    print('📡 API: Response status: ${resp.statusCode}');
    print('📡 API: Response body length: ${resp.bodyBytes.length}');
    
    final List<dynamic> data = jsonDecode(utf8.decode(resp.bodyBytes));
    print('📡 API: Decoded ${data.length} recommendations');
    
    return data.cast<Map<String, dynamic>>();
  }

  /// 뮤지컬 상세 정보 조회 (ID 기준) - URL 정보 포함
  static Future<Map<String, dynamic>> getMusicalDetail(int id) async {
    final resp = await get('/api/app/musicals/$id');
    return jsonDecode(utf8.decode(resp.bodyBytes));
  }

  /// 뮤지컬 상세 정보 조회 (인터파크 ID 기준) - URL 정보 포함
  static Future<Map<String, dynamic>> getMusicalByInterparkId(String interparkId) async {
    final resp = await get('/api/app/musicals/interpark/$interparkId');
    return jsonDecode(utf8.decode(resp.bodyBytes));
  }
}
