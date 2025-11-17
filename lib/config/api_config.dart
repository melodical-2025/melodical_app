import 'dart:io';

/// API 설정 클래스
/// 환경별 baseUrl 및 API 관련 설정 관리
class ApiConfig {
  // 환경 설정
  static const String _environment = String.fromEnvironment(
    'ENV',
    defaultValue: 'dev', // 기본값은 개발 환경
  );

  // 환경별 Base URL
  static const Map<String, String> _baseUrls = {
    'dev': 'http://localhost:8080',  // macOS/iOS 로컬호스트
    'android': 'http://10.0.2.2:8080',  // Android 에뮬레이터
    'prod': 'https://api.melodical.com',  // 프로덕션 서버 (TODO: 실제 URL로 변경)
  };

  /// 현재 환경의 Base URL 반환
  static String get baseUrl {
    // Android 플랫폼인 경우 자동으로 10.0.2.2 사용
    if (Platform.isAndroid && _environment == 'dev') {
      return _baseUrls['android']!;
    }
    return _baseUrls[_environment] ?? _baseUrls['dev']!;
  }

  /// 현재 환경명 반환
  static String get environment => _environment;

  /// 개발 환경 여부
  static bool get isDev => _environment == 'dev';

  /// 프로덕션 환경 여부
  static bool get isProd => _environment == 'prod';

  // API 엔드포인트 경로
  static const String authPath = '/auth';
  static const String apiPath = '/api';

  // 타임아웃 설정 (초 단위)
  static const int connectTimeout = 60;
  static const int receiveTimeout = 60;
}
