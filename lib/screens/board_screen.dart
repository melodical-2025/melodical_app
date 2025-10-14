import 'package:flutter/material.dart';
import 'post_screen.dart';

class BoardScreen extends StatelessWidget {
  const BoardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    // 더미 게시글 데이터
    final posts = List.generate(
      10,
          (index) => {
        'title': '게시글 제목 $index',
        'content': '게시글 내용 $index: 여기에 상세 내용이 들어갑니다.',
        'author': '작성자 $index',
        'date': '2025-10-${index + 1}',
      },
    );

    return Scaffold(
      appBar: AppBar(title: const Text('작품 게시판')),
      body: ListView.builder(
        itemCount: posts.length,
        itemBuilder: (context, index) {
          final post = posts[index];
          return ListTile(
            title: Text(post['title']!),
            subtitle: Text('작성자: ${post['author']} | 날짜: ${post['date']}'),
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => PostScreen(post: post),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
