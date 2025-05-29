import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    int likedCount = 5;
    int ratedMusicals = 8;
    int ratedSongs = 12;

    final List<Map<String, dynamic>> recommendedMusicals = [
      {'title': '도리안 그레이', 'image': 'assets/poster.png'},
      {'title': '모리스', 'image': 'assets/poster.png'},
      {'title': '알라딘', 'image': 'assets/poster.png'},
      {'title': '매디슨 카운티의 다리', 'image': 'assets/poster.png'},
    ];

    return Scaffold(
      backgroundColor: Colors.white,
      body: Column(
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
                  spreadRadius: 0,
                ),
              ],
            ),
            child: const Padding(
              padding: EdgeInsets.only(bottom: 16),
              child: Align(
                alignment: Alignment.bottomCenter,
                child: Text(
                  'Melodical',
                  style: TextStyle(
                    color: Color(0xFFE17951),
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
          ),

          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
            child: Text(
              '나의 음악 및 뮤지컬 취향',
              style: TextStyle(
                fontSize: 25,
                fontWeight: FontWeight.bold,
                color: Colors.black,
              ),
            ),
          ),

          const SizedBox(height: 210),

          // 추천 뮤지컬 텍스트 + 슬라이딩 리스트
          Padding(
            padding: const EdgeInsets.only(left: 16.0, bottom: 10.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '당신의 취향에 꼭 맞는 뮤지컬',
                  style: TextStyle(
                    fontSize: 25,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
                const SizedBox(height: 20),
                SizedBox(
                  height: 240,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    itemCount: recommendedMusicals.length,
                    itemBuilder: (context, index) {
                      final musical = recommendedMusicals[index];
                      return Padding(
                        padding: const EdgeInsets.only(right: 18),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(10),
                              child: Image.asset(
                                musical['image'],
                                width: 130,
                                height: 180,
                                fit: BoxFit.cover,
                              ),
                            ),
                            const SizedBox(height: 8),
                            SizedBox(
                              width: 100,
                              child: Text(
                                musical['title'],
                                style: const TextStyle(
                                  fontSize: 15,
                                  fontWeight: FontWeight.bold,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),

          const Spacer(),

          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                buildStatBoxWidget(
                  icon: Icons.favorite,
                  assetPath: null,
                  label1: '찜한',
                  label2: '뮤지컬',
                  count: likedCount,
                  textColor: Colors.black,
                ),
                buildStatBoxWidget(
                  icon: null,
                  assetPath: 'assets/musicalicon.png',
                  label1: '평가한',
                  label2: '뮤지컬',
                  count: ratedMusicals,
                  textColor: Colors.black,
                ),
                buildStatBoxWidget(
                  icon: null,
                  assetPath: 'assets/musicicon.png',
                  label1: '평가한',
                  label2: '음악',
                  count: ratedSongs,
                  textColor: Colors.black,
                ),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: BottomNavBar(currentIndex: 0),
    );
  }

  Widget buildStatBoxWidget({
    String? emoji,
    IconData? icon,
    String? assetPath,
    required String label1,
    required String label2,
    required int count,
    required Color textColor,
  }) {
    return Container(
      width: 120,
      height: 65,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF2DB),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          if (assetPath != null)
            Image.asset(
              assetPath,
              width: 25,
              height: 25,
              fit: BoxFit.contain,
            )
          else if (icon != null)
            Icon(
              icon,
              color: Colors.red,
              size: 25,
            )
          else if (emoji != null)
              Text(
                emoji,
                style: const TextStyle(fontSize: 20),
              ),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              '$label1\n$label2 $count개',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.bold,
                color: textColor,
                height: 1.2,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
