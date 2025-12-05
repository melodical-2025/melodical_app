import 'package:flutter/material.dart';
import 'dart:math' as math;
import '../models/comment.dart';
import '../repositories/comment_repository.dart';
import '../services/api_service.dart';
import '../config/api_config.dart';
import 'post_screen.dart';

class BoardScreen extends StatefulWidget {
  final int? musicalId;
  
  const BoardScreen({super.key, this.musicalId});

  @override
  State<BoardScreen> createState() => _BoardScreenState();
}

class _BoardScreenState extends State<BoardScreen> {
  final CommentRepository _commentRepository = CommentRepository();
  List<Comment> _comments = [];
  bool _isLoading = true;
  String? _error;
  final TextEditingController _commentController = TextEditingController();
  final TextEditingController _replyController = TextEditingController();
  int? _replyingToCommentId;

  @override
  void initState() {
    super.initState();
    _loadComments();
  }
  
  @override
  void dispose() {
    _commentController.dispose();
    _replyController.dispose();
    super.dispose();
  }

  Future<void> _loadComments() async {
    if (widget.musicalId == null) {
      setState(() {
        _isLoading = false;
        _error = '뮤지컬 정보가 없습니다';
      });
      return;
    }

    try {
      setState(() {
        _isLoading = true;
        _error = null;
      });

      final comments = await _commentRepository.getCommentsByMusical(widget.musicalId!);
      setState(() {
        _comments = comments;
        _isLoading = false;
      });
      print('✅ Loaded ${_comments.length} comments');
    } catch (e) {
      print('❌ Error loading comments: $e');
      setState(() {
        _error = '댓글을 불러오는데 실패했습니다: ${e.toString()}';
        _isLoading = false;
      });
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

      // 서버에 좋아요 토글 요청
      final liked = await ApiService.toggleCommentLike(comment.id);
      
      // 서버 응답에 따라 실제 좋아요 수 업데이트
      final newLikeCount = liked 
          ? originalLikeCount + 1 
          : math.max(0, originalLikeCount - 1);
      
      setState(() {
        _comments[index] = _comments[index].copyWith(likeCount: newLikeCount);
      });
      
      print('✅ Comment ${comment.id} like toggled: $liked (count: $newLikeCount)');
    } catch (e) {
      print('❌ Error toggling comment like: $e');
      // 에러 발생 시 원래 상태로 복구
      _loadComments();
    }
  }
  
  Future<void> _addComment() async {
    if (_commentController.text.trim().isEmpty) {
      return;
    }

    try {
      await _commentRepository.addComment(
        musicalId: widget.musicalId!,
        content: _commentController.text.trim(),
      );
      
      _commentController.clear();
      await _loadComments();
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('댓글이 작성되었습니다')),
        );
      }
    } catch (e) {
      print('❌ Error adding comment: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('댓글 작성 실패: ${e.toString()}')),
        );
      }
    }
  }
  
  Future<void> _addReply(int parentId) async {
    if (_replyController.text.trim().isEmpty) {
      return;
    }

    try {
      await _commentRepository.addReply(
        musicalId: widget.musicalId!,
        parentId: parentId,
        content: _replyController.text.trim(),
      );
      
      _replyController.clear();
      setState(() {
        _replyingToCommentId = null;
      });
      await _loadComments();
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('답글이 작성되었습니다')),
        );
      }
    } catch (e) {
      print('❌ Error adding reply: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('답글 작성 실패: ${e.toString()}')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text('작품 게시판', style: TextStyle(color: Colors.black)),
        backgroundColor: Colors.white,
        elevation: 1,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: Column(
        children: [
          Expanded(
            child: _isLoading
                ? const Center(
                    child: CircularProgressIndicator(
                      color: Color(0xFFE17951),
                    ),
                  )
                : _error != null
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              const Icon(
                                Icons.error_outline,
                                size: 64,
                                color: Color(0xFFE17951),
                              ),
                        const SizedBox(height: 16),
                        Text(
                          '댓글을 불러올 수 없습니다',
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          _error!,
                          textAlign: TextAlign.center,
                          style: const TextStyle(color: Colors.grey),
                        ),
                        const SizedBox(height: 24),
                        ElevatedButton(
                          onPressed: _loadComments,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFFE17951),
                          ),
                          child: const Text(
                            '다시 시도',
                            style: TextStyle(color: Colors.white),
                          ),
                        ),
                      ],
                    ),
                  ),
                )
                    : _comments.isEmpty
                        ? const Center(
                            child: Padding(
                              padding: EdgeInsets.all(20),
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(
                                    Icons.chat_bubble_outline,
                                    size: 64,
                                    color: Colors.grey,
                                  ),
                                  SizedBox(height: 16),
                                  Text(
                                    '아직 작성된 댓글이 없습니다',
                                    style: TextStyle(
                                      fontSize: 16,
                                      color: Colors.grey,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.all(12),
                            itemCount: _comments.length,
                            itemBuilder: (context, index) {
                              final comment = _comments[index];
                              // 최상위 댓글만 표시 (대댓글 제외)
                              if (comment.parentId != null) {
                                return const SizedBox.shrink();
                              }
                              return _buildCommentWithReplies(comment);
                            },
                          ),
          ),
          // 하단 댓글 입력 필드
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: Colors.grey.shade300,
                  blurRadius: 4,
                  offset: const Offset(0, -2),
                ),
              ],
            ),
            child: SafeArea(
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _commentController,
                      decoration: const InputDecoration(
                        hintText: '댓글을 입력하세요',
                        border: OutlineInputBorder(),
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 8,
                        ),
                      ),
                      maxLines: 1,
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    icon: const Icon(Icons.send),
                    onPressed: _addComment,
                    color: const Color(0xFFE17951),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildCommentWithReplies(Comment comment) {
    // 해당 댓글의 대댓글들 찾기
    final replies = _comments
        .where((c) => c.parentId == comment.id)
        .toList();
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildCommentCard(comment, isReply: false),
        // 대댓글들 표시
        ...replies.map((reply) => Padding(
              padding: const EdgeInsets.only(left: 24),
              child: _buildCommentCard(reply, isReply: true),
            )),
      ],
    );
  }
  
  Widget _buildCommentCard(Comment comment, {required bool isReply}) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(8),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.shade200,
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  if (isReply)
                    const Padding(
                      padding: EdgeInsets.only(right: 4),
                      child: Icon(
                        Icons.subdirectory_arrow_right,
                        size: 16,
                        color: Colors.grey,
                      ),
                    ),
                  GestureDetector(
                    onTap: () => _showUserProfilePopup(comment.userId),
                    child: CircleAvatar(
                      radius: 20,
                      backgroundColor: Colors.grey[300],
                      backgroundImage: comment.profileImageUrl != null && comment.profileImageUrl!.isNotEmpty
                          ? NetworkImage('${ApiConfig.baseUrl}${comment.profileImageUrl}')
                          : null,
                      child: comment.profileImageUrl == null || comment.profileImageUrl!.isEmpty
                          ? const Icon(Icons.person, size: 16, color: Color(0xFFE17951))
                          : null,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    comment.username,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
              Text(
                '${comment.createdAt.year}-${comment.createdAt.month.toString().padLeft(2, '0')}-${comment.createdAt.day.toString().padLeft(2, '0')}',
                style: const TextStyle(
                  fontSize: 10,
                  color: Colors.grey,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            comment.content,
            style: const TextStyle(
              fontSize: 12,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              // 좋아요 버튼
              GestureDetector(
                onTap: () => _toggleCommentLike(comment),
                child: Row(
                  children: [
                    Icon(
                      Icons.favorite,
                      size: 14,
                      color: comment.likeCount > 0 ? Colors.red : Colors.grey,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      '${comment.likeCount}',
                      style: const TextStyle(
                        fontSize: 11,
                        color: Colors.grey,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              // 답글 버튼 (최상위 댓글에만 표시)
              if (!isReply)
                GestureDetector(
                  onTap: () {
                    setState(() {
                      _replyingToCommentId = comment.id;
                    });
                  },
                  child: const Row(
                    children: [
                      Icon(
                        Icons.reply,
                        size: 14,
                        color: Colors.grey,
                      ),
                      SizedBox(width: 4),
                      Text(
                        '답글',
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.grey,
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
          // 답글 입력 필드
          if (_replyingToCommentId == comment.id)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _replyController,
                      decoration: const InputDecoration(
                        hintText: '답글을 입력하세요',
                        border: OutlineInputBorder(),
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 8,
                        ),
                      ),
                      style: const TextStyle(fontSize: 12),
                      maxLines: 2,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Column(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.send, size: 20),
                        onPressed: () => _addReply(comment.id),
                        color: const Color(0xFFE17951),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close, size: 20),
                        onPressed: () {
                          setState(() {
                            _replyingToCommentId = null;
                            _replyController.clear();
                          });
                        },
                        color: Colors.grey,
                      ),
                    ],
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Future<void> _showUserProfilePopup(int userId) async {
    try {
      final userStats = await ApiService.getUserStats(userId);
      
      if (!mounted) return;
      
      showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        builder: (context) => Container(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              CircleAvatar(
                radius: 50,
                backgroundColor: const Color(0xFFFFD9A3),
                backgroundImage: userStats['profileImageUrl'] != null && userStats['profileImageUrl'].isNotEmpty
                    ? NetworkImage('${ApiConfig.baseUrl}${userStats['profileImageUrl']}')
                    : null,
                child: userStats['profileImageUrl'] == null || userStats['profileImageUrl'].isEmpty
                    ? const Icon(Icons.person, size: 40, color: Color(0xFFE17951))
                    : null,
              ),
              const SizedBox(height: 16),
              Text(
                userStats['nickname'] ?? 'Unknown',
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFFE17951),
                ),
              ),
              const SizedBox(height: 8),
              Text(
                userStats['email'] ?? '',
                style: const TextStyle(
                  fontSize: 14,
                  color: Colors.grey,
                ),
              ),
              const SizedBox(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _buildStatColumn(Icons.favorite, '찜한 개수', '${userStats['favoriteCount'] ?? 0}'),
                  _buildStatColumn(Icons.star, '뮤지컬 평가', '${userStats['ratedMusicalCount'] ?? 0}'),
                  _buildStatColumn(Icons.music_note, '음악 평가', '${userStats['ratedMusicCount'] ?? 0}'),
                ],
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('사용자 정보를 불러올 수 없습니다: $e')),
        );
      }
    }
  }

  Widget _buildStatColumn(IconData icon, String label, String value) {
    return Column(
      children: [
        Icon(icon, color: const Color(0xFFE17951), size: 28),
        const SizedBox(height: 8),
        Text(
          label,
          style: const TextStyle(fontSize: 12, color: Colors.grey),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.black87,
          ),
        ),
      ],
    );
  }
}
