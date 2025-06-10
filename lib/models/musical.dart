class Musical {
  final int id;
  final String title;
  final String cast;
  final String startDate;
  final String endDate;
  final String runtime;
  final String theater;
  final String posterUrl;

  Musical({
    required this.id,
    required this.title,
    required this.cast,
    required this.startDate,
    required this.endDate,
    required this.runtime,
    required this.theater,
    required this.posterUrl,
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
      posterUrl: fixedUrl,  // ← 이 부분이 바뀌었습니다
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
  };
}