import 'package:flutter/material.dart';
import '../models/musical.dart';
import 'board_screen.dart';
import 'post_screen.dart';

class DetailScreen extends StatelessWidget {
  const DetailScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final dummyMusical = Musical(
      id: 1,
      title: '엘리자벳',
      cast: '배우 A, 배우 B',
      startDate: '2025-10-01',
      endDate: '2025-12-31',
      runtime: '150분',
      theater: '세종문화회관',
      posterUrl: 'https://picsum.photos/200/300',
    );

    final posts = [
      {'title': '게시글 1', 'content': '내용 일부...', 'author': '작성자1', 'date': '2025-10-14'},
      {'title': '게시글 2', 'content': '내용 일부...', 'author': '작성자2', 'date': '2025-10-13'},
      {'title': '게시글 3', 'content': '내용 일부...', 'author': '작성자3', 'date': '2025-10-12'},
      {'title': '게시글 4', 'content': '내용 일부...', 'author': '작성자4', 'date': '2025-10-11'},
    ];

    return Scaffold(
      backgroundColor: Colors.white,
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
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
                        Navigator.pop(context); // 이전 페이지로 돌아감
                      },
                    ),
                  ),
                  // 중앙 작품 제목
                  Align(
                    alignment: Alignment.bottomCenter,
                    child: Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: Text(
                        dummyMusical.title,
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


            const SizedBox(height: 16),

            // 포스터 + 제목/찜하기/하트
            Center(
              child: Column(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.network(
                      dummyMusical.posterUrl,
                      width: 200,
                      height: 300,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(
                        width: 200,
                        height: 300,
                        color: Colors.grey.shade200,
                        child: const Icon(Icons.broken_image),
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        dummyMusical.title,
                        style: const TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(width: 8),
                      const Text(
                        '찜하기',
                        style: TextStyle(
                          fontSize: 16,
                          color: Colors.red,
                        ),
                      ),
                      const SizedBox(width: 4),
                      const Icon(Icons.favorite, color: Colors.red),
                    ],
                  ),
                  const SizedBox(height: 8),
                  // 출연 / 러닝타임
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text(
                        '출연: ',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      Text(dummyMusical.cast),
                      const SizedBox(width: 16),
                      const Text(
                        '러닝타임: ',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      Text(dummyMusical.runtime),
                    ],
                  ),
                  const SizedBox(height: 4),
                  // 장소 / 기간
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text(
                        '장소: ',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      Text(dummyMusical.theater),
                      const SizedBox(width: 16),
                      const Text(
                        '기간: ',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      Text('${dummyMusical.startDate} ~ ${dummyMusical.endDate}'),
                    ],
                  ),
                  const SizedBox(height: 16),
                ],
              ),
            ),

            Divider(color: Colors.grey.shade400),

            // 예매처 바로가기
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 8),
              child: Text(
                '예매처 바로가기',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () {},
                      child: const Text('인터파크'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () {},
                      child: const Text('예스24'),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 16),
            Divider(color: Colors.grey.shade400),

            // 평점
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 8),
              child: Text(
                '평점',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Row(
                children: const [
                  Icon(Icons.star, color: Colors.orange),
                  SizedBox(width: 4),
                  Text('4.5'),
                  SizedBox(width: 16),
                  Icon(Icons.star, color: Colors.orange),
                  SizedBox(width: 4),
                  Text('4.0'),
                ],
              ),
            ),

            const SizedBox(height: 16),
            Divider(color: Colors.grey.shade400),

            // 평가하기
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 8),
              child: Text(
                '평가하기',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Row(
                children: List.generate(
                  5,
                      (index) => const Icon(Icons.star_border, color: Colors.orange),
                ),
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
                  const Text(
                    '작품게시판',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  GestureDetector(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const PostScreen(isEditing: true),
                        ),
                      );
                    },
                    child: const Text(
                      '작성하기',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Colors.blue,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            Column(
              children: List.generate(3, (index) {
                final dummyPost = posts[index];
                return GestureDetector(
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => PostScreen(post: dummyPost),
                      ),
                    );
                  },
                  child: Container(
                    width: double.infinity,
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      border: Border.all(color: Colors.grey.shade300),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          dummyPost['title']!,
                          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          dummyPost['content']!,
                          style: const TextStyle(fontSize: 12, color: Colors.grey),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${dummyPost['author']} • ${dummyPost['date']}',
                          style: const TextStyle(fontSize: 10, color: Colors.grey),
                        ),
                      ],
                    ),
                  ),
                );
              }),
            ),

            TextButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => BoardScreen()),
                );
              },
              child: const Text('모두보기'),
            ),

            const SizedBox(height: 16),
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
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                children: List.generate(
                  5, // 더미 5개
                      (index) => Padding(
                    padding: const EdgeInsets.only(right: 12.0),
                    child: GestureDetector(
                      onTap: () {
                        // 클릭 시 디테일 화면 이동 (나중에 실제 작품 데이터 연결)
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => const DetailScreen(),
                          ),
                        );
                      },
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: Image.network(
                          'https://picsum.photos/100/150?random=$index',
                          width: 100,
                          height: 150,
                          fit: BoxFit.cover,
                          errorBuilder: (_, __, ___) => Container(
                            width: 100,
                            height: 150,
                            color: Colors.grey.shade200,
                            child: const Icon(Icons.broken_image),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
          ],
        ),
      ),
    );
  }
}
