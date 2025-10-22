import 'package:flutter/material.dart';
import '../models/musical.dart';

class PostScreen extends StatefulWidget {
  final Map<String, String>? post; // 게시글 데이터
  final Musical? musical; // 작품 제목 표시용
  final bool isEditing; // 작성 모드 여부

  const PostScreen({Key? key, this.post, this.musical, this.isEditing = false})
      : super(key: key);

  @override
  State<PostScreen> createState() => _PostScreenState();
}

class _PostScreenState extends State<PostScreen> {
  bool isLiked = false;
  int likeCount = 0;

  final titleController = TextEditingController();
  final contentController = TextEditingController();

  final commentController = TextEditingController();
  final List<Map<String, dynamic>> comments = []; // 댓글: {author, content, date, liked, likeCount, replies}

  @override
  void initState() {
    super.initState();
    if (widget.post != null) {
      titleController.text = widget.post!['title'] ?? '';
      contentController.text = widget.post!['content'] ?? '';
    }
  }

  @override
  Widget build(BuildContext context) {
    final post = widget.post ?? {};

    return Scaffold(
      backgroundColor: Colors.white,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 상단바
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
                Positioned(
                  left: 8,
                  bottom: 16,
                  child: IconButton(
                    icon: const Icon(Icons.arrow_back, color: Color(0xFFD55D2E)),
                    onPressed: () => Navigator.pop(context),
                  ),
                ),
                Align(
                  alignment: Alignment.bottomCenter,
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Text(
                      widget.musical?.title ?? '게시글',
                      style: const TextStyle(
                        fontFamily: 'Urbanist',
                        color: Color(0xFFD55D2E),
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),

          // 본문 영역
          Expanded(
            child: widget.isEditing
                ? Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  // 제목
                  TextField(
                    controller: titleController,
                    decoration: const InputDecoration(
                        hintText: '제목을 입력해주세요.'),
                  ),
                  const SizedBox(height: 8),

                  // 내용
                  Expanded(
                    child: TextField(
                      controller: contentController,
                      expands: true,
                      maxLines: null,
                      decoration: const InputDecoration(
                        hintText: '작품에 대해 자유롭게 얘기해보세요.',
                        contentPadding: EdgeInsets.all(12),
                      ),
                    ),
                  ),

                  const SizedBox(height: 8),

                  ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFFFFD9A3),
                      foregroundColor: const Color(0xFFE17951),
                    ),
                    onPressed: () {
                      // 작성 후 저장
                      Navigator.pop(context);
                    },
                    child: const Text('저장'),
                  ),
                ],
              ),
            )
                : ListView(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              children: [
                const SizedBox(height: 16),
                // 제목
                Text(
                  post['title'] ?? '',
                  style: const TextStyle(
                      fontSize: 20, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),

                // 작성자 • 날짜
                Text(
                  '${post['author'] ?? ''} • ${post['date'] ?? ''}',
                  style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                ),
                const SizedBox(height: 16),

                // 내용
                Text(
                  post['content'] ?? '',
                  style: const TextStyle(fontSize: 14, height: 1.5),
                ),
                const SizedBox(height: 16),

                // 분리선
                Container(height: 1, color: Colors.grey.shade300),

                // 좋아요 + 댓글 버튼
                Row(
                  children: [
                    IconButton(
                      icon: Icon(
                        isLiked ? Icons.favorite : Icons.favorite_border,
                        color: isLiked ? Colors.red : Colors.grey,
                      ),
                      onPressed: () {
                        setState(() {
                          isLiked = !isLiked;
                          likeCount += isLiked ? 1 : -1;
                        });
                      },
                    ),
                    Text('$likeCount 좋아요'),
                    const SizedBox(width: 24),
                    IconButton(
                      icon: const Icon(Icons.wechat_rounded,
                          color: Color(0xFFFFAD75)),
                      onPressed: () {
                        final commentFocusNode = FocusNode();
                        showModalBottomSheet(
                          context: context,
                          isScrollControlled: true,
                          builder: (context) {
                            WidgetsBinding.instance
                                .addPostFrameCallback((_) {
                              FocusScope.of(context)
                                  .requestFocus(commentFocusNode);
                            });
                            return Padding(
                              padding: EdgeInsets.only(
                                bottom: MediaQuery.of(context)
                                    .viewInsets
                                    .bottom,
                                left: 16,
                                right: 16,
                                top: 16,
                              ),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: TextField(
                                      focusNode: commentFocusNode,
                                      controller: commentController,
                                      decoration: InputDecoration(
                                        hintText: '댓글을 입력하세요.',
                                        border: OutlineInputBorder(
                                          borderSide: BorderSide(
                                              color: Color(0xFFE17951)),
                                        ),
                                        enabledBorder: OutlineInputBorder(
                                          borderSide: BorderSide(
                                              color: Color(0xFFE17951)),
                                        ),
                                        focusedBorder: OutlineInputBorder(
                                          borderSide: BorderSide(
                                              color: Color(0xFFE17951)),
                                        ),
                                        contentPadding:
                                        const EdgeInsets.symmetric(
                                            horizontal: 12,
                                            vertical: 8),
                                        hintStyle: TextStyle(
                                            color: Color(0xFFE17951)),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  ElevatedButton(
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor:
                                      const Color(0xFFFFD9A3),
                                      foregroundColor:
                                      const Color(0xFFE17951),
                                    ),
                                    onPressed: () {
                                      if (commentController
                                          .text.isNotEmpty) {
                                        setState(() {
                                          comments.add({
                                            'author': '사용자',
                                            'content':
                                            commentController.text,
                                            'date': '방금 전',
                                            'liked': false,
                                            'likeCount': 0,
                                            'replies': [], // 대댓글
                                          });
                                          commentController.clear();
                                        });
                                        Navigator.pop(context);
                                      }
                                    },
                                    child: const Text('등록'),
                                  ),
                                ],
                              ),
                            );
                          },
                        );
                      },
                    ),
                    const Text('댓글'),
                  ],
                ),

                // 버튼과 댓글 목록 사이 분리선
                Container(height: 1, color: Colors.grey.shade300),

                // 댓글 및 대댓글 목록
                ...comments.map((c) {
                  return Container(
                    margin: const EdgeInsets.symmetric(vertical: 8),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey.shade300),
                      borderRadius: BorderRadius.circular(8),
                      color: Colors.grey.shade100,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment:
                          MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              c['author'],
                              style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 12),
                            ),
                            Text(
                              c['date'],
                              style: TextStyle(
                                  fontSize: 10,
                                  color: Colors.grey[600]),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          c['content'],
                          style: const TextStyle(fontSize: 14),
                        ),
                        const SizedBox(height: 4),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.end, // 오른쪽 정렬
                          children: [
                            IconButton(
                              icon: Icon(
                                c['liked']
                                    ? Icons.favorite
                                    : Icons.favorite_border,
                                color: c['liked'] ? Colors.red : Colors.grey,
                                size: 18,
                              ),
                              onPressed: () {
                                setState(() {
                                  c['liked'] = !c['liked'];
                                  c['likeCount'] +=
                                  c['liked'] ? 1 : -1;
                                });
                              },
                            ),
                            Text('${c['likeCount']}'),
                            TextButton(
                              onPressed: () {
                                // 대댓글 입력 모달
                                final replyController =
                                TextEditingController();
                                showModalBottomSheet(
                                  context: context,
                                  isScrollControlled: true,
                                  builder: (context) {
                                    return Padding(
                                      padding: EdgeInsets.only(
                                        bottom: MediaQuery.of(context)
                                            .viewInsets
                                            .bottom,
                                        left: 16,
                                        right: 16,
                                        top: 16,
                                      ),
                                      child: Row(
                                        children: [
                                          Expanded(
                                            child: TextField(
                                              controller: replyController,
                                              decoration: InputDecoration(
                                                hintText: '대댓글을 입력하세요.',
                                                border: OutlineInputBorder(
                                                  borderSide: BorderSide(
                                                      color: Color(
                                                          0xFFE17951)),
                                                ),
                                                enabledBorder:
                                                OutlineInputBorder(
                                                  borderSide: BorderSide(
                                                      color: Color(
                                                          0xFFE17951)),
                                                ),
                                                focusedBorder:
                                                OutlineInputBorder(
                                                  borderSide: BorderSide(
                                                      color: Color(
                                                          0xFFE17951)),
                                                ),
                                                contentPadding:
                                                const EdgeInsets
                                                    .symmetric(
                                                    horizontal: 12,
                                                    vertical: 8),
                                                hintStyle: TextStyle(
                                                    color: Color(
                                                        0xFFE17951)),
                                              ),
                                            ),
                                          ),
                                          const SizedBox(width: 8),
                                          ElevatedButton(
                                            style: ElevatedButton.styleFrom(
                                              backgroundColor:
                                              const Color(0xFFFFD9A3),
                                              foregroundColor:
                                              const Color(0xFFE17951),
                                            ),
                                            onPressed: () {
                                              if (replyController
                                                  .text.isNotEmpty) {
                                                setState(() {
                                                  c['replies'].add({
                                                    'author': '사용자',
                                                    'content':
                                                    replyController
                                                        .text,
                                                    'date': '방금 전',
                                                    'liked': false,
                                                    'likeCount': 0,
                                                  });
                                                });
                                                Navigator.pop(context);
                                              }
                                            },
                                            child: const Text('등록'),
                                          ),
                                        ],
                                      ),
                                    );
                                  },
                                );
                              },
                              child: const Text(
                                '답글',
                                style: TextStyle(
                                  color: Color(0xFFFFAD75), // 글자색 변경
                                  fontWeight: FontWeight.bold, // 굵기 옵션
                                ),
                              ),                            ),
                          ],
                        ),
                        // 대댓글 표시
                        ...c['replies'].map<Widget>((r) {
                          return Container(
                            margin: const EdgeInsets.only(
                                left: 16, top: 8),
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: Colors.grey.shade200,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Column(
                              crossAxisAlignment:
                              CrossAxisAlignment.start,
                              children: [
                                Row(
                                  mainAxisAlignment:
                                  MainAxisAlignment.spaceBetween,
                                  children: [
                                    Text(
                                      r['author'],
                                      style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 12),
                                    ),
                                    Text(
                                      r['date'],
                                      style: TextStyle(
                                          fontSize: 10,
                                          color: Colors.grey[600]),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  r['content'],
                                  style:
                                  const TextStyle(fontSize: 14),
                                ),
                              ],
                            ),
                          );
                        }).toList(),
                      ],
                    ),
                  );
                }).toList(),

                const SizedBox(height: 16),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
