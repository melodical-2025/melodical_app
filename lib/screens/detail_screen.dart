import 'package:flutter/material.dart';
import 'package:flutter_rating_bar/flutter_rating_bar.dart';
import 'package:url_launcher/url_launcher.dart';
import 'dart:math' as math;
import '../models/comment.dart';
import '../repositories/comment_repository.dart';
import '../services/api_service.dart';
import 'board_screen.dart';
import 'post_screen.dart';

class DetailScreen extends StatefulWidget {
  final Map<String, dynamic> musicalData;

  const DetailScreen({
    Key? key,
    required this.musicalData,
  }) : super(key: key);

  @override
  State<DetailScreen> createState() => _DetailScreenState();
}

class _DetailScreenState extends State<DetailScreen> {
  bool isLiked = false;
  double _userRating = 0.0;
  final CommentRepository _commentRepository = CommentRepository();
  List<Comment> _comments = [];
  bool _isLoadingComments = true;
  List<Map<String, dynamic>> _recommendedMusicals = [];
  bool _isLoadingRecommendations = true;

  @override
  void initState() {
    super.initState();
    
    // 디버깅: 받은 데이터 확인
    print('=== DetailScreen initialized ===');
    print('musicalData keys: ${widget.musicalData.keys.toList()}');
    print('All data:');
    widget.musicalData.forEach((key, value) {
      print('  $key: $value (type: ${value.runtimeType})');
    });
    print('recommendationReason: "${widget.musicalData['recommendationReason']}"');
    print('recommendationReason is null? ${widget.musicalData['recommendationReason'] == null}');
    print('recommendationReason is empty? ${widget.musicalData['recommendationReason']?.toString().isEmpty ?? true}');
    print('similarityPercentage: ${widget.musicalData['similarityPercentage']}');
    print('chartRanking: ${widget.musicalData['chartRanking']}');
    print('averageRating: ${widget.musicalData['averageRating']}');
    print('interparkUrl: ${widget.musicalData['interparkUrl']}');
    print('yes24Url: ${widget.musicalData['yes24Url']}');
    print('posterUrl: ${widget.musicalData['posterUrl']}');
    print('================================');
    
    _loadComments();
    _loadRecommendedMusicals();
    _loadUserRating();
    _loadFavoriteStatus();
  }
  
  Future<void> _loadFavoriteStatus() async {
    try {
      final musicalId = widget.musicalData['id'];
      if (musicalId != null) {
        final favoriteStatus = await ApiService.checkFavoriteStatus(musicalId);
        setState(() {
          isLiked = favoriteStatus;
        });
        print('✅ Loaded favorite status for musical $musicalId: $isLiked');
      }
    } catch (e) {
      print('⚠️ Failed to load favorite status: $e');
      // 로그인하지 않았거나 에러가 있는 경우는 무시
    }
  }
  
  Future<void> _loadUserRating() async {
    try {
      final myRatings = await ApiService.getMyRatings();
      final musicalId = widget.musicalData['id'];
      if (musicalId != null && myRatings.containsKey(musicalId)) {
        setState(() {
          _userRating = myRatings[musicalId]!;
        });
        print('✅ Loaded user rating for musical $musicalId: $_userRating');
      }
    } catch (e) {
      print('⚠️ Failed to load user rating: $e');
      // 로그인하지 않았거나 평점이 없는 경우는 무시
    }
  }
  
  Future<void> _loadRecommendedMusicals() async {
    try {
      setState(() {
        _isLoadingRecommendations = true;
      });

      // 월간 인기 뮤지컬 5개를 추천으로 사용
      final musicals = await ApiService.fetchMonthlyMusicals();
      setState(() {
        _recommendedMusicals = musicals.take(5).toList();
        _isLoadingRecommendations = false;
      });
    } catch (e) {
      print('Error loading recommendations: $e');
      setState(() {
        _isLoadingRecommendations = false;
      });
    }
  }

  Future<void> _loadComments() async {
    try {
      setState(() {
        _isLoadingComments = true;
      });

      final musicalId = widget.musicalData['id'];
      print('🔍 Loading comments for musicalId: $musicalId (type: ${musicalId.runtimeType})');
      if (musicalId != null) {
        final comments = await _commentRepository.getCommentsByMusical(musicalId);
        print('✅ Loaded ${comments.length} comments');
        setState(() {
          _comments = comments;
          _isLoadingComments = false;
        });
      } else {
        print('❌ musicalId is null!');
        setState(() {
          _isLoadingComments = false;
        });
      }
    } catch (e) {
      print('❌ Error loading comments: $e');
      setState(() {
        _isLoadingComments = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('댓글을 불러오는데 실패했습니다: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Future<void> _createComment(String content, {int? parentId}) async {
    try {
      final musicalId = widget.musicalData['id'];
      if (musicalId == null) {
        throw Exception('Musical ID is null');
      }

      // 낙관적 UI 업데이트 - 댓글을 즉시 목록에 추가
      final tempComment = Comment(
        id: -DateTime.now().millisecondsSinceEpoch, // 임시 ID (음수)
        musicalId: musicalId,
        userId: -1, // 임시 userId
        username: '작성 중...',
        content: content,
        createdAt: DateTime.now(),
        parentId: parentId,
        depth: parentId != null ? 1 : 0,
        likeCount: 0,
        isDeleted: false,
        replies: [],
      );

      setState(() {
        _comments.insert(0, tempComment); // 맨 위에 추가
      });

      final request = CommentRequest(
        musicalId: musicalId,
        content: content,
        parentId: parentId,
      );

      // API 호출
      await _commentRepository.createComment(request);
      
      // 실제 댓글 목록 새로고침
      await _loadComments();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(parentId != null ? '대댓글이 작성되었습니다' : '댓글이 작성되었습니다'),
            backgroundColor: Colors.green,
            duration: const Duration(milliseconds: 1000),
          ),
        );
      }
    } catch (e) {
      print('Error creating comment: $e');
      // 에러 발생 시 임시 댓글 제거하고 새로고침
      await _loadComments();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('댓글 작성에 실패했습니다: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Future<void> _deleteComment(int commentId) async {
    try {
      await _commentRepository.deleteComment(commentId);
      await _loadComments();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('댓글이 삭제되었습니다'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      print('Error deleting comment: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('댓글 삭제에 실패했습니다: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Future<void> _toggleCommentLike(Comment comment) async {
    try {
      final index = _comments.indexWhere((c) => c.id == comment.id);
      if (index == -1) return;

      // 원본 좋아요 수 저장
      final originalLikeCount = comment.likeCount;
      
      // 사용자가 이미 좋아요를 눌렀는지 확인
      bool isAlreadyLiked = false;
      try {
        isAlreadyLiked = await ApiService.isCommentLiked(comment.id);
      } catch (e) {
        print('⚠️ Failed to check like status: $e');
      }
      
      // 낙관적 UI 업데이트 - 현재 상태에 따라 토글
      setState(() {
        final newLikeCount = isAlreadyLiked 
            ? math.max(0, originalLikeCount - 1)  // 이미 좋아요 → 취소
            : originalLikeCount + 1;  // 좋아요 추가
        _comments[index] = _comments[index].copyWith(likeCount: newLikeCount);
      });

      // API 호출 - 실제 토글 수행
      final liked = await ApiService.toggleCommentLike(comment.id);
      
      // API 응답에 따라 정확한 값으로 업데이트
      setState(() {
        final newLikeCount = liked 
            ? originalLikeCount + 1  // 좋아요 추가됨
            : math.max(0, originalLikeCount - 1);  // 좋아요 취소됨
        _comments[index] = _comments[index].copyWith(likeCount: newLikeCount);
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(liked ? '❤️ 좋아요' : '💔 좋아요 취소'),
            duration: const Duration(milliseconds: 500),
            backgroundColor: liked ? Colors.pink : Colors.grey,
          ),
        );
      }
    } catch (e) {
      print('Error toggling comment like: $e');
      // 에러 발생 시 댓글 목록 새로고침
      await _loadComments();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('좋아요 처리에 실패했습니다'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  void _showCommentDialog() {
    final TextEditingController controller = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('댓글 작성'),
          content: TextField(
            controller: controller,
            maxLines: 5,
            decoration: const InputDecoration(
              hintText: '댓글을 입력하세요',
              border: OutlineInputBorder(),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('취소'),
            ),
            ElevatedButton(
              onPressed: () {
                if (controller.text.trim().isNotEmpty) {
                  _createComment(controller.text.trim());
                  Navigator.pop(context);
                }
              },
              child: const Text('작성'),
            ),
          ],
        );
      },
    );
  }

  void _showReplyDialog(Comment parentComment) {
    final TextEditingController controller = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('${parentComment.username}님에게 답글'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  parentComment.content,
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: controller,
                maxLines: 3,
                decoration: const InputDecoration(
                  hintText: '답글을 입력하세요',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('취소'),
            ),
            ElevatedButton(
              onPressed: () {
                if (controller.text.trim().isNotEmpty) {
                  _createComment(
                    controller.text.trim(),
                    parentId: parentComment.id,
                  );
                  Navigator.pop(context);
                }
              },
              child: const Text('완료'),
            ),
          ],
        );
      },
    );
  }

  // 댓글과 대댓글을 함께 표시하는 위젯
  Widget _buildCommentWithReplies(Comment comment) {
    final replies = _comments.where((c) => c.parentId == comment.id).toList();
    
    return Column(
      children: [
        _buildCommentCard(comment, isReply: false),
        // 대댓글 표시
        if (replies.isNotEmpty)
          ...replies.map((reply) => Padding(
            padding: const EdgeInsets.only(left: 24.0),
            child: _buildCommentCard(reply, isReply: true),
          )),
      ],
    );
  }

  // 개별 댓글 카드
  Widget _buildCommentCard(Comment comment, {required bool isReply}) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 0),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: isReply ? Colors.grey.shade50 : Colors.white,
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  if (isReply) const Icon(Icons.subdirectory_arrow_right, size: 14, color: Colors.grey),
                  if (isReply) const SizedBox(width: 4),
                  Text(
                    comment.username,
                    style: TextStyle(
                      fontSize: isReply ? 13 : 14,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
              Row(
                children: [
                  // 좋아요 버튼
                  IconButton(
                    icon: Icon(
                      Icons.favorite,
                      size: 18,
                      color: comment.likeCount > 0 ? Colors.red : Colors.grey.shade400,
                    ),
                    onPressed: () => _toggleCommentLike(comment),
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints(),
                  ),
                  Text(
                    '${comment.likeCount}',
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    icon: const Icon(Icons.delete, size: 18, color: Colors.red),
                    onPressed: () => _deleteComment(comment.id),
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints(),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            comment.content,
            style: TextStyle(fontSize: isReply ? 11 : 12, color: Colors.black87),
            maxLines: 3,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '${comment.createdAt.year}-${comment.createdAt.month.toString().padLeft(2, '0')}-${comment.createdAt.day.toString().padLeft(2, '0')}',
                style: const TextStyle(fontSize: 10, color: Colors.grey),
              ),
              if (!isReply) // 최상위 댓글에만 답글 버튼 표시
                TextButton.icon(
                  onPressed: () => _showReplyDialog(comment),
                  icon: const Icon(Icons.reply, size: 14),
                  label: const Text('답글', style: TextStyle(fontSize: 12)),
                  style: TextButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    minimumSize: Size.zero,
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _launchURL(String? urlString) async {
    print('🔗 Attempting to launch URL: $urlString');
    
    if (urlString == null || urlString.isEmpty) {
      print('❌ URL is null or empty');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('링크 정보가 없습니다'),
            backgroundColor: Colors.red,
          ),
        );
      }
      return;
    }

    try {
      final Uri url = Uri.parse(urlString);
      print('📍 Parsed URI: $url');

      if (await canLaunchUrl(url)) {
        print('✅ Can launch URL, launching...');
        final launched = await launchUrl(
          url,
          mode: LaunchMode.externalApplication,
        );
        print('Launch result: $launched');
      } else {
        print('❌ Cannot launch URL');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('링크를 열 수 없습니다: $urlString'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    } catch (e) {
      print('❌ Error launching URL: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('오류가 발생했습니다: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.musicalData['title'] ?? 'Unknown';
    final posterUrl = widget.musicalData['posterUrl'] ?? '';
    final theater = widget.musicalData['theater'] ?? 'N/A';
    final period = widget.musicalData['period'] ?? 'N/A';
    
    // 평점 정보 - averageRating 우선, 없으면 개별 평점 사용
    final averageRating = widget.musicalData['averageRating'];
    final interparkRating = widget.musicalData['interparkRating'] ?? averageRating;
    final yes24Rating = widget.musicalData['yes24Rating'] ?? averageRating;
    
    final cast = widget.musicalData['cast'] ?? 'N/A';
    final runtime = widget.musicalData['runtime'] ?? 'N/A';
    final interparkUrl = widget.musicalData['interparkUrl'];
    final yes24Url = widget.musicalData['yes24Url'];
    
    // URL 디버깅 로그
    print('🎭 Detail Screen - Musical: $title (ID: ${widget.musicalData['id']})');
    print('🔗 Interpark URL: $interparkUrl (type: ${interparkUrl?.runtimeType}, isEmpty: ${interparkUrl?.toString().isEmpty})');
    print('🔗 Yes24 URL: $yes24Url (type: ${yes24Url?.runtimeType}, isEmpty: ${yes24Url?.toString().isEmpty})');
    print('⭐ Average Rating: $averageRating');
    print('⭐ Interpark Rating: $interparkRating');
    print('⭐ Yes24 Rating: $yes24Rating');
    print('📦 Full musical data keys: ${widget.musicalData.keys.toList()}');

    return Scaffold(
      backgroundColor: Colors.white,
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 상단 헤더
            Container(
              height: 110,
              decoration: BoxDecoration(
                color: Colors.white,
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFFE17951),
                    blurRadius: 4,
                    offset: const Offset(5, 0),
                  ),
                ],
              ),
              child: Stack(
                children: [
                  // 제목 (중앙, 버튼 영역을 피하기 위해 좌우 패딩 추가)
                  Align(
                    alignment: Alignment.bottomCenter,
                    child: Padding(
                      padding: const EdgeInsets.only(
                        bottom: 16,
                        left: 56, // 뒤로가기 버튼 영역 피하기
                        right: 16,
                      ),
                      child: Text(
                        title,
                        style: const TextStyle(
                          fontFamily: 'Urbanist',
                          color: Color(0xFFD55D2E),
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                        textAlign: TextAlign.center,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ),
                  // 뒤로가기 버튼 (가장 위로, z-index 효과)
                  Positioned(
                    left: 8,
                    bottom: 16,
                    child: Material(
                      color: Colors.transparent,
                      child: InkWell(
                        borderRadius: BorderRadius.circular(24),
                        onTap: () => Navigator.pop(context),
                        child: Padding(
                          padding: const EdgeInsets.all(8.0),
                          child: const Icon(
                            Icons.arrow_back,
                            color: Color(0xFFD55D2E),
                            size: 28,
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // 포스터 + 제목/찜하기/하트
            Center(
              child: Column(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: posterUrl.isNotEmpty
                        ? Image.network(
                      posterUrl,
                      width: 200,
                      height: 300,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(
                        width: 200,
                        height: 300,
                        color: Colors.grey.shade200,
                        child: const Icon(Icons.broken_image),
                      ),
                    )
                        : Container(
                      width: 200,
                      height: 300,
                      color: Colors.grey.shade200,
                      child: const Icon(Icons.broken_image),
                    ),
                  ),
                  const SizedBox(height: 8),
                  
                  // 추천 이유 배지 (있는 경우에만 표시)
                  Builder(
                    builder: (context) {
                      final reason = widget.musicalData['recommendationReason'];
                      print('🎯 Building recommendation badge:');
                      print('  - reason: "$reason"');
                      print('  - reason type: ${reason.runtimeType}');
                      print('  - is null: ${reason == null}');
                      print('  - is empty: ${reason?.toString().isEmpty ?? true}');
                      
                      if (reason != null && reason.toString().isNotEmpty) {
                        print('  ✅ SHOWING recommendation badge');
                        return Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                            decoration: BoxDecoration(
                              color: const Color(0xFFFFE4D6),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: const Color(0xFFE17951),
                                width: 1.5,
                              ),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(
                                  widget.musicalData['chartRanking'] != null 
                                      ? Icons.star 
                                      : Icons.favorite,
                                  size: 20,
                                  color: const Color(0xFFE17951),
                                ),
                                const SizedBox(width: 8),
                                Flexible(
                                  child: Text(
                                    reason.toString(),
                                    style: const TextStyle(
                                      fontSize: 13,
                                      color: Color(0xFFE17951),
                                      fontWeight: FontWeight.w600,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      } else {
                        print('  ❌ NOT showing recommendation badge');
                        return const SizedBox.shrink();
                      }
                    },
                  ),
                  
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16.0),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Flexible(
                          child: Text(
                            title,
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                            ),
                            textAlign: TextAlign.center,
                            overflow: TextOverflow.ellipsis,
                            maxLines: 2,
                          ),
                        ),
                        const SizedBox(width: 8),
                        const Text(
                          '관심',
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.red,
                          ),
                        ),
                        const SizedBox(width: 4),
                        GestureDetector(
                          onTap: () async {
                            try {
                              final musicalId = widget.musicalData['id'];
                              if (musicalId != null) {
                                final newIsLiked = await ApiService.toggleFavorite(musicalId);
                                setState(() {
                                  isLiked = newIsLiked;
                                });
                                
                                if (mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(
                                      content: Text(newIsLiked ? '찜 목록에 추가되었습니다' : '찜 목록에서 제거되었습니다'),
                                      duration: const Duration(milliseconds: 1000),
                                      backgroundColor: newIsLiked ? Colors.pink : Colors.grey,
                                    ),
                                  );
                                }
                              }
                            } catch (e) {
                              print('Error toggling favorite: $e');
                              if (mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text('찜 처리에 실패했습니다'),
                                    backgroundColor: Colors.red,
                                  ),
                                );
                              }
                            }
                          },
                          child: Icon(
                            isLiked ? Icons.favorite : Icons.favorite_border,
                            color: Colors.red,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 8),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16.0),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text(
                          '출연: ',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        Flexible(
                          child: Text(
                            cast != 'N/A' ? cast : '정보 없음',
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                          ),
                        ),
                        const SizedBox(width: 16),
                        const Text(
                          '러닝타임: ',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        Text(runtime != 'N/A' ? runtime : '정보 없음'),
                      ],
                    ),
                  ),
                  const SizedBox(height: 4),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16.0),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text(
                          '장소: ',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        Flexible(
                          child: Text(
                            theater,
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                          ),
                        ),
                        const SizedBox(width: 16),
                        const Text(
                          '기간: ',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        Flexible(
                          child: Text(
                            period,
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 8),
                ],
              ),
            ),

            Divider(color: Colors.grey.shade400),

            // 예매처 바로가기
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 4),
              child: Text(
                '예매처 바로가기',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 60.0),
              child: Row(
                children: [
                  Expanded(
                    child: SizedBox(
                      height: 40,
                      child: ElevatedButton(
                        onPressed: interparkUrl != null && interparkUrl.toString().isNotEmpty
                            ? () => _launchURL(interparkUrl.toString())
                            : null,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.white,
                          elevation: 6,
                          shadowColor: Colors.grey.withOpacity(0.5),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(30),
                          ),
                          padding: EdgeInsets.zero,
                          disabledBackgroundColor: Colors.grey.shade300,
                        ),
                        child: Image.asset(
                          'assets/nolinterpark.png',
                          width: 100,
                          height: 30,
                          fit: BoxFit.contain,
                          errorBuilder: (_, __, ___) => const Text(
                            '인터파크',
                            style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
                          ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 50),
                  Expanded(
                    child: SizedBox(
                      height: 40,
                      child: ElevatedButton(
                        onPressed: yes24Url != null && yes24Url.toString().isNotEmpty
                            ? () => _launchURL(yes24Url.toString())
                            : null,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.white,
                          elevation: 6,
                          shadowColor: Colors.grey.withOpacity(0.5),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(30),
                          ),
                          padding: EdgeInsets.zero,
                          disabledBackgroundColor: Colors.grey.shade300,
                        ),
                        child: Image.asset(
                          'assets/yes.png',
                          width: 110,
                          height: 18,
                          fit: BoxFit.contain,
                          errorBuilder: (_, __, ___) => const Text(
                            'Yes24',
                            style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // 평점
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 4),
              child: Text(
                '평점',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
              ),
            ),

            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Row(
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(10),
                      child: Image.asset(
                        'assets/nol.png',
                        width: 40,
                        height: 40,
                        fit: BoxFit.contain,
                        errorBuilder: (_, __, ___) => Container(
                          width: 40,
                          height: 40,
                          decoration: BoxDecoration(
                            color: const Color(0xFFE17951),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Center(
                            child: Text(
                              'IP',
                              style: TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      interparkRating != null
                          ? interparkRating.toStringAsFixed(1)
                          : 'N/A',
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                const SizedBox(width: 80),
                Row(
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(10),
                      child: Image.asset(
                        'assets/y24.png',
                        width: 40,
                        height: 40,
                        fit: BoxFit.contain,
                        errorBuilder: (_, __, ___) => Container(
                          width: 40,
                          height: 40,
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFAD75),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Center(
                            child: Text(
                              'Y24',
                              style: TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 11,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      yes24Rating != null
                          ? yes24Rating.toStringAsFixed(1)
                          : 'N/A',
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ],
            ),

            const SizedBox(height: 16),

            // 평가하기
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 4),
              child: Text(
                '평가하기',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
              ),
            ),
            Center(
              child: RatingBar.builder(
                initialRating: _userRating,
                minRating: 0,
                allowHalfRating: true,
                itemCount: 5,
                itemSize: 40,
                itemPadding: const EdgeInsets.symmetric(horizontal: 4),
                unratedColor: Colors.grey.shade300,
                itemBuilder: (_, __) => const Icon(
                  Icons.star,
                  color: Colors.orange,
                ),
                onRatingUpdate: (rating) async {
                  setState(() {
                    _userRating = rating;
                  });
                  print('💾 평점 저장 시도: $rating');
                  
                  // 평점 자동 저장
                  try {
                    final musicalId = widget.musicalData['id'];
                    if (musicalId != null) {
                      await ApiService.rateBatchMusical([
                        {
                          'musicalId': musicalId,
                          'rating': rating,
                        }
                      ]);
                      
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text('평점 $rating점이 저장되었습니다'),
                            backgroundColor: Colors.green,
                            duration: const Duration(seconds: 1),
                          ),
                        );
                      }
                      print('✅ 평점 저장 성공: $rating');
                    }
                  } catch (e) {
                    print('❌ 평점 저장 실패: $e');
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text('평점 저장에 실패했습니다: $e'),
                          backgroundColor: Colors.red,
                        ),
                      );
                    }
                  }
                },
              ),
            ),

            const SizedBox(height: 16),
            Divider(color: Colors.grey.shade400),

            // 작품게시판 + 작성하기
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  GestureDetector(
                    onTap: () {
                      final musicalId = widget.musicalData['id'];
                      if (musicalId != null) {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => BoardScreen(musicalId: musicalId),
                          ),
                        );
                      }
                    },
                    child: Row(
                      children: [
                        const Text(
                          '작품게시판',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(width: 4),
                        Icon(Icons.arrow_forward_ios, size: 14, color: Colors.grey.shade600),
                      ],
                    ),
                  ),
                  GestureDetector(
                    onTap: _showCommentDialog,
                    child: const Text(
                      '작성하기',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Colors.orange,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            // 댓글 목록
            _isLoadingComments
                ? const Center(
              child: Padding(
                padding: EdgeInsets.all(20.0),
                child: CircularProgressIndicator(),
              ),
            )
                : _comments.isEmpty
                ? const Padding(
              padding: EdgeInsets.all(20.0),
              child: Center(
                child: Text(
                  '작성된 댓글이 없습니다',
                  style: TextStyle(color: Colors.grey),
                ),
              ),
            )
                : Column(
              children: _comments
                  .where((c) => c.parentId == null) // 최상위 댓글만
                  .take(3)
                  .map((comment) => _buildCommentCard(comment, isReply: false))
                  .toList(),
            ),

            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 5.0),
              child: SizedBox(
                width: double.infinity,
                height: 30,
                child: TextButton(
                  style: TextButton.styleFrom(
                    backgroundColor: Colors.grey.shade300,
                    foregroundColor: Colors.black,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => BoardScreen(musicalId: widget.musicalData['id']),
                      ),
                    );
                  },
                  child: const Text(
                    '모두보기',
                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
            ),

            // 연관 작품 추천
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 8),
              child: Text(
                '연관 작품 추천',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
            SizedBox(
              height: 150,
              child: _isLoadingRecommendations
                  ? const Center(child: CircularProgressIndicator())
                  : _recommendedMusicals.isEmpty
                      ? const Center(
                          child: Text(
                            '추천할 작품이 없습니다',
                            style: TextStyle(color: Colors.grey),
                          ),
                        )
                      : ListView.builder(
                          scrollDirection: Axis.horizontal,
                          padding: const EdgeInsets.symmetric(horizontal: 16.0),
                          itemCount: _recommendedMusicals.length,
                          itemBuilder: (context, index) {
                            final musical = _recommendedMusicals[index];
                            final posterUrl = musical['posterUrl'] ?? '';
                            
                            return Padding(
                              padding: const EdgeInsets.only(right: 12.0),
                              child: GestureDetector(
                                onTap: () {
                                  print('🎭 Navigating to musical: ${musical['title']}');
                                  Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (context) => DetailScreen(
                                        musicalData: musical,
                                      ),
                                    ),
                                  );
                                },
                                child: ClipRRect(
                                  borderRadius: BorderRadius.circular(8),
                                  child: posterUrl.isNotEmpty
                                      ? Image.network(
                                          posterUrl,
                                          width: 100,
                                          height: 150,
                                          fit: BoxFit.cover,
                                          errorBuilder: (_, __, ___) => Container(
                                            width: 100,
                                            height: 150,
                                            color: Colors.grey.shade200,
                                            child: const Icon(Icons.broken_image),
                                          ),
                                        )
                                      : Container(
                                          width: 100,
                                          height: 150,
                                          color: Colors.grey.shade200,
                                          child: const Icon(Icons.broken_image),
                                        ),
                                ),
                              ),
                            );
                          },
                        ),
            ),
            const SizedBox(height: 12),
          ],
        ),
      ),
    );
  }
}
