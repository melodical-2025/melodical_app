/// API 에러 처리를 위한 커스텀 예외 클래스
class ApiException implements Exception {
  final int? statusCode;
  final String message;
  final dynamic data;

  ApiException({
    this.statusCode,
    required this.message,
    this.data,
  });

  /// HTTP 상태 코드에 따른 기본 메시지 반환
  factory ApiException.fromStatusCode(int statusCode, {String? customMessage, dynamic data}) {
    String message = customMessage ?? _getDefaultMessage(statusCode);
    
    return ApiException(
      statusCode: statusCode,
      message: message,
      data: data,
    );
  }

  /// 네트워크 에러
  factory ApiException.networkError() {
    return ApiException(
      message: '네트워크 연결을 확인해주세요.',
    );
  }

  /// 타임아웃 에러
  factory ApiException.timeout() {
    return ApiException(
      message: '요청 시간이 초과되었습니다. 다시 시도해주세요.',
    );
  }

  /// 알 수 없는 에러
  factory ApiException.unknown(String? message) {
    return ApiException(
      message: message ?? '알 수 없는 오류가 발생했습니다.',
    );
  }

  /// 상태 코드별 기본 메시지
  static String _getDefaultMessage(int statusCode) {
    switch (statusCode) {
      case 400:
        return '잘못된 요청입니다.';
      case 401:
        return '인증이 필요합니다. 다시 로그인해주세요.';
      case 403:
        return '접근 권한이 없습니다.';
      case 404:
        return '요청한 리소스를 찾을 수 없습니다.';
      case 500:
        return '서버 오류가 발생했습니다.';
      case 502:
        return '서버와의 연결이 원활하지 않습니다.';
      case 503:
        return '서비스를 사용할 수 없습니다. 잠시 후 다시 시도해주세요.';
      default:
        return '오류가 발생했습니다. (코드: $statusCode)';
    }
  }

  /// 사용자에게 표시할 메시지
  String get displayMessage => message;

  /// 인증 에러 여부
  bool get isAuthError => statusCode == 401;

  /// 권한 에러 여부
  bool get isForbiddenError => statusCode == 403;

  /// 서버 에러 여부
  bool get isServerError => statusCode != null && statusCode! >= 500;

  @override
  String toString() {
    if (statusCode != null) {
      return 'ApiException($statusCode): $message';
    }
    return 'ApiException: $message';
  }
}
