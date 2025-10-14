import 'package:flutter/material.dart';
import '../models/musical.dart'; // dummyMusical 사용을 위해

class PostScreen extends StatelessWidget {
  final Map<String, String>? post; // null이면 작성 모드
  final bool isEditing;
  final Musical? musical; // 작품 제목 표시용

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
                // 뒤로가기 버튼
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
                // 중앙 작품 제목
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
              child: Column(
                children: [
                  TextField(
                    controller: titleController,
                    decoration: const InputDecoration(labelText: '제목'),
                    readOnly: !isEditing,
                  ),
                  const SizedBox(height: 16),
                  Expanded(
                    child: TextField(
                      controller: contentController,
                      decoration: const InputDecoration(labelText: '내용'),
                      maxLines: null,
                      expands: true,
                      readOnly: !isEditing,
                    ),
                  ),
                  if (isEditing)
                    ElevatedButton(
                      onPressed: () {
                        // 작성 후 저장 로직
                        Navigator.pop(context);
                      },
                      child: const Text('저장'),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
