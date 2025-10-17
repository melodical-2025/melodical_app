import 'package:flutter/material.dart';
import '../models/musical.dart';

class PostScreen extends StatelessWidget {
  final Map<String, String>? post;
  final bool isEditing;
  final Musical? musical;

  const PostScreen({Key? key, this.post, this.isEditing = false, this.musical}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final titleController = TextEditingController(text: post?['title'] ?? '');
    final contentController = TextEditingController(text: post?['content'] ?? '');

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
                    onPressed: () {
                      Navigator.pop(context);
                    },
                  ),
                ),
                Align(
                  alignment: Alignment.bottomCenter,
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Text(
                      musical?.title ?? '게시글',
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

          // 본문
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: isEditing
                  ? Column(
                children: [
                  TextField(
                    controller: titleController,
                    decoration: const InputDecoration(
                      hintText: '제목을 입력해주세요.',
                    ),
                    readOnly: !isEditing,
                  ),
                  const SizedBox(height: 16),
                  Expanded(
                    child: TextField(
                      controller: contentController,
                      decoration: const InputDecoration(
                        hintText: '작품에 대해 자유롭게 얘기해보세요.',
                        alignLabelWithHint: true,
                      ),
                      maxLines: null,
                      expands: true,
                      readOnly: !isEditing,
                    ),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: () {
                      Navigator.pop(context);
                    },
                    child: const Text('저장'),
                  ),
                ],
              )
                  : SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 제목
                    Text(
                      post?['title'] ?? '',
                      style: const TextStyle(
                          fontSize: 20, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),

                    // 작성자 • 날짜
                    Text(
                      '${post?['author'] ?? ''} • ${post?['date'] ?? ''}',
                      style:
                      TextStyle(fontSize: 12, color: Colors.grey[600]),
                    ),
                    const SizedBox(height: 16),

                    // 본문 내용
                    Text(
                      post?['content'] ?? '',
                      style: const TextStyle(fontSize: 14, height: 1.5),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
