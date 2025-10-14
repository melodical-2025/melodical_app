import 'package:flutter/material.dart';

class PostScreen extends StatelessWidget {
  final Map<String, String>? post; // null이면 작성 모드
  final bool isEditing;

  const PostScreen({Key? key, this.post, this.isEditing = false}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final titleController = TextEditingController(text: post?['title'] ?? '');
    final contentController = TextEditingController(text: post?['content'] ?? '');

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditing ? '게시글 작성' : (post?['title'] ?? '게시글')),
      ),
      body: Padding(
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
                  // 작성 후 저장 로직 (서버 연동)
                  Navigator.pop(context);
                },
                child: const Text('저장'),
              ),
          ],
        ),
      ),
    );
  }
}
