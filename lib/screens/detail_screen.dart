import 'package:flutter/material.dart';
import '../models/musical.dart';
import 'board_screen.dart';
import 'post_screen.dart';
import 'package:flutter_rating_bar/flutter_rating_bar.dart';


class DetailScreen extends StatefulWidget {
  const DetailScreen({Key? key}) : super(key: key);

  @override
  State<DetailScreen> createState() => _DetailScreenState();
}

class _DetailScreenState extends State<DetailScreen> {
  bool isLiked = false;

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
                      GestureDetector(
                        onTap: () {
                          setState(() {
                            isLiked = !isLiked; // 상태 토글
                          });
                        },
                        child: Icon(
                          isLiked ? Icons.favorite : Icons.favorite_border,
                          color: Colors.red,
                        ),
                      ),
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
              padding: const EdgeInsets.symmetric(horizontal: 50.0),
              child: Row(
                children: [
                  Expanded(
                    child: SizedBox(
                      height: 50,
                      child: ElevatedButton(
                        onPressed: () {
                          // 인터파크 이동 코드
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.white,
                          elevation: 6, // 그림자 높이
                          shadowColor: Colors.grey.withOpacity(0.5), // 그림자 색
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(30),
                          ),
                          padding: EdgeInsets.zero,
                        ),
                        child: Image.asset(
                          'assets/nolinterpark.png',
                          width: 120,
                          height: 30,
                          fit: BoxFit.contain,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 20),
                  Expanded(
                    child: SizedBox(
                      height: 50,
                      child: ElevatedButton(
                        onPressed: () {
                          // 예스24 이동 코드
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.white,
                          elevation: 6,
                          shadowColor: Colors.grey.withOpacity(0.5),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(30),
                          ),
                          padding: EdgeInsets.zero,
                        ),
                        child: Image.asset(
                          'assets/yes.png',
                          width: 110,
                          height: 20,
                          fit: BoxFit.contain,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),


            const SizedBox(height: 16),
            Divider(color: Colors.grey.shade400),

            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 4),
              child: Text(
                '평점',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),

            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Row(
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: Image.asset(
                        'assets/nol.png',
                        width: 48,
                        height: 48,
                        fit: BoxFit.contain,
                      ),
                    ),
                    const SizedBox(width: 8),
                    const Text(
                      '4.3',
                      style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                const SizedBox(width: 70),
                Row(
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: Image.asset(
                        'assets/y24.png',
                        width: 48,
                        height: 48,
                        fit: BoxFit.contain,
                      ),
                    ),
                    const SizedBox(width: 8),
                    const Text(
                      '4.5',
                      style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ],
            ),

            const SizedBox(height: 16),
            Divider(color: Colors.grey.shade400),

            // 평가하기
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16.0, vertical: 4),
              child: Text(
                '평가하기',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
            // 별 아이콘 중앙정렬
            Center(
              child: RatingBar.builder(
                initialRating: 0,
                minRating: 0,
                allowHalfRating: true,
                itemCount: 5,
                itemSize: 40, // 아이콘 크기
                itemPadding: const EdgeInsets.symmetric(horizontal: 4),
                unratedColor: Colors.grey.shade300,
                itemBuilder: (_, __) => const Icon(
                  Icons.star,
                  color: Colors.orange,
                ),
                onRatingUpdate: (rating) {
                  // rating 값 처리
                  print('현재 선택된 평점: $rating');
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
                        color: Colors.orange,
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
                    margin: const EdgeInsets.only(bottom: 0),
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

            Center(
              child: TextButton(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => BoardScreen()),
                  );
                },
                child: const Text('모두보기'),
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
