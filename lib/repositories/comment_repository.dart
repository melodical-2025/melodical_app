import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/comment.dart';
import '../services/api_service.dart';
import '../config/api_config.dart';

class CommentRepository {
  String get baseUrl => '${ApiConfig.baseUrl}/api/comments';

  Future<List<Comment>> getCommentsByMusical(int musicalId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/musical/$musicalId'),
      );

      if (response.statusCode == 200) {
        final List<dynamic> jsonList = json.decode(utf8.decode(response.bodyBytes));
        return jsonList.map((json) => Comment.fromJson(json)).toList();
      } else {
        throw Exception('Failed to load comments');
      }
    } catch (e) {
      print('Error fetching comments: $e');
      throw Exception('Failed to load comments: $e');
    }
  }

  Future<List<Comment>> getMyComments() async {
    try {
      final token = await ApiService.getToken();
      if (token == null) {
        throw Exception('로그인이 필요합니다');
      }

      final response = await http.get(
        Uri.parse('$baseUrl/my'),
        headers: {
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> jsonList = json.decode(utf8.decode(response.bodyBytes));
        return jsonList.map((json) => Comment.fromJson(json)).toList();
      } else {
        throw Exception('Failed to load my comments');
      }
    } catch (e) {
      print('Error fetching my comments: $e');
      throw Exception('Failed to load my comments: $e');
    }
  }

  Future<Comment> createComment(CommentRequest request) async {
    try {
      final token = await ApiService.getToken();
      if (token == null) {
        throw Exception('로그인이 필요합니다');
      }

      print('📝 Creating comment: ${request.toJson()}');

      final response = await http.post(
        Uri.parse(baseUrl),
        headers: {
          'Content-Type': 'application/json; charset=UTF-8',
          'Authorization': 'Bearer $token',
        },
        body: json.encode(request.toJson()),
      );

      print('📥 Response status: ${response.statusCode}');
      print('📥 Response body: ${response.body}');

      if (response.statusCode == 200) {
        return Comment.fromJson(json.decode(utf8.decode(response.bodyBytes)));
      } else {
        throw Exception('Failed to create comment: ${response.body}');
      }
    } catch (e) {
      print('❌ Error creating comment: $e');
      throw Exception('Failed to create comment: $e');
    }
  }
  
  // 간편한 댓글 작성 메서드
  Future<Comment> addComment({
    required int musicalId,
    required String content,
  }) async {
    final request = CommentRequest(
      musicalId: musicalId,
      content: content,
    );
    return createComment(request);
  }
  
  // 간편한 답글 작성 메서드
  Future<Comment> addReply({
    required int musicalId,
    required int parentId,
    required String content,
  }) async {
    final request = CommentRequest(
      musicalId: musicalId,
      content: content,
      parentId: parentId,
    );
    return createComment(request);
  }

  Future<Comment> updateComment(int commentId, String content) async {
    try {
      final token = await ApiService.getToken();
      if (token == null) {
        throw Exception('로그인이 필요합니다');
      }

      final response = await http.put(
        Uri.parse('$baseUrl/$commentId'),
        headers: {
          'Content-Type': 'application/json; charset=UTF-8',
          'Authorization': 'Bearer $token',
        },
        body: json.encode(content),
      );

      if (response.statusCode == 200) {
        return Comment.fromJson(json.decode(utf8.decode(response.bodyBytes)));
      } else {
        throw Exception('Failed to update comment');
      }
    } catch (e) {
      print('Error updating comment: $e');
      throw Exception('Failed to update comment: $e');
    }
  }

  Future<void> deleteComment(int commentId) async {
    try {
      final token = await ApiService.getToken();
      if (token == null) {
        throw Exception('로그인이 필요합니다');
      }

      final response = await http.delete(
        Uri.parse('$baseUrl/$commentId'),
        headers: {
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode != 204 && response.statusCode != 200) {
        throw Exception('Failed to delete comment');
      }
    } catch (e) {
      print('Error deleting comment: $e');
      throw Exception('Failed to delete comment: $e');
    }
  }
}
