class Song {
  final String id;
  final String title;
  final String artist;
  final String artworkUrl;
  final String genre;
  final double? userRating;

  Song({
    required this.id,
    required this.title,
    required this.artist,
    required this.artworkUrl,
    required this.genre,
    this.userRating,
  });

  factory Song.fromJson(Map<String, dynamic> json) {
    return Song(
      id: json['id'] as String,
      title: json['title'] as String,
      artist: json['artist'] as String,
      artworkUrl: json['artworkUrl'] as String,
      genre: json['genre'] as String,
      userRating: json['userRating'] != null
          ? (json['userRating'] as num).toDouble()
          : null,
    );
  }
}