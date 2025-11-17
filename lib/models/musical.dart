class Musical {
  final int id;
  final String title;
  final String cast;
  final String startDate;
  final String endDate;
  final String runtime;
  final String theater;
  final String posterUrl;
  final String? period;
  final double? interparkRating;
  final double? yes24Rating;
  final String? interparkUrl;
  final String? yes24Url;

  Musical({
    required this.id,
    required this.title,
    required this.cast,
    required this.startDate,
    required this.endDate,
    required this.runtime,
    required this.theater,
    required this.posterUrl,
    this.period,
    this.interparkRating,
    this.yes24Rating,
    this.interparkUrl,
    this.yes24Url,
  });

  factory Musical.fromJson(Map<String, dynamic> json) {
    // 1) JSON 키가 posterUrl 또는 poster_url 중 하나일 수 있으니 둘 다 시도
    final raw = (json['posterUrl'] ?? json['poster_url'] ?? '') as String;
    // 2) 만약 //로 시작하면 https: 스킴 붙여 주기
    final fixedUrl = raw.startsWith('//') ? 'https:$raw' : raw;
    return Musical(
      id: json['id'] as int,
      title: json['title'] as String,
      cast: json['cast'] as String? ?? '',
      startDate: json['startDate'] as String? ?? '',
      endDate: json['endDate'] as String? ?? '',
      runtime: json['runtime'] as String? ?? '',
      theater: json['theater'] as String? ?? '',
      posterUrl: fixedUrl,
      period: json['period'] as String?,
      interparkRating: json['interparkRating'] != null ? (json['interparkRating'] as num).toDouble() : null,
      yes24Rating: json['yes24Rating'] != null ? (json['yes24Rating'] as num).toDouble() : null,
      interparkUrl: json['interparkUrl'] as String?,
      yes24Url: json['yes24Url'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'cast': cast,
    'startDate': startDate,
    'endDate': endDate,
    'runtime': runtime,
    'theater': theater,
    'poster_url': posterUrl,
    'period': period,
    'interparkRating': interparkRating,
    'yes24Rating': yes24Rating,
    'interparkUrl': interparkUrl,
    'yes24Url': yes24Url,
  };
}
