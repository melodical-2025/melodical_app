class Comment {
  final int id;
  final int musicalId;
  final String? musicalTitle;  // 뮤지컬 제목 추가
  final int userId;
  final String username;
  final String content;
  final DateTime createdAt;
  final DateTime? updatedAt;
  
  // 대댓글 관련 필드
  final int? parentId;
  final int depth;
  final bool isDeleted;
  final int likeCount;
  final List<Comment> replies;

  Comment({
    required this.id,
    required this.musicalId,
    this.musicalTitle,
    required this.userId,
    required this.username,
    required this.content,
    required this.createdAt,
    this.updatedAt,
    this.parentId,
    this.depth = 0,
    this.isDeleted = false,
    this.likeCount = 0,
    this.replies = const [],
  });

  factory Comment.fromJson(Map<String, dynamic> json) {
    return Comment(
      id: json['id'],
      musicalId: json['musicalId'],
      musicalTitle: json['musicalTitle'],
      userId: json['userId'],
      username: json['username'] ?? 'Unknown',
      content: json['content'],
      createdAt: DateTime.parse(json['createdAt']),
      updatedAt: json['updatedAt'] != null ? DateTime.parse(json['updatedAt']) : null,
      parentId: json['parentId'],
      depth: json['depth'] ?? 0,
      isDeleted: json['isDeleted'] ?? false,
      likeCount: json['likeCount'] ?? 0,
      replies: json['replies'] != null
          ? (json['replies'] as List).map((r) => Comment.fromJson(r)).toList()
          : [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'musicalId': musicalId,
      'musicalTitle': musicalTitle,
      'userId': userId,
      'username': username,
      'content': content,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
      'parentId': parentId,
      'depth': depth,
      'isDeleted': isDeleted,
      'likeCount': likeCount,
      'replies': replies.map((r) => r.toJson()).toList(),
    };
  }

  // 좋아요 개수 업데이트를 위한 copyWith 메서드
  Comment copyWith({
    int? id,
    int? musicalId,
    String? musicalTitle,
    int? userId,
    String? username,
    String? content,
    DateTime? createdAt,
    DateTime? updatedAt,
    int? parentId,
    int? depth,
    bool? isDeleted,
    int? likeCount,
    List<Comment>? replies,
  }) {
    return Comment(
      id: id ?? this.id,
      musicalId: musicalId ?? this.musicalId,
      musicalTitle: musicalTitle ?? this.musicalTitle,
      userId: userId ?? this.userId,
      username: username ?? this.username,
      content: content ?? this.content,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      parentId: parentId ?? this.parentId,
      depth: depth ?? this.depth,
      isDeleted: isDeleted ?? this.isDeleted,
      likeCount: likeCount ?? this.likeCount,
      replies: replies ?? this.replies,
    );
  }
}

class CommentRequest {
  final int musicalId;
  final String content;
  final int? parentId; // 대댓글인 경우 부모 댓글 ID

  CommentRequest({
    required this.musicalId,
    required this.content,
    this.parentId,
  });

  Map<String, dynamic> toJson() {
    return {
      'musicalId': musicalId,
      'content': content,
      if (parentId != null) 'parentId': parentId,
    };
  }
}
