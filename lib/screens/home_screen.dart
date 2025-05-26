import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    int likedCount = 5;
    int ratedMusicals = 8;
    int ratedSongs = 12;

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
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.black,
              ),
            ),
          ),

          const Expanded(
            child: Align(
              alignment: Alignment.centerLeft,
              child: Padding(
                padding: EdgeInsets.symmetric(horizontal: 16),
                child: Text(
                  '당신의 취향에 꼭 맞는 뮤지컬',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
              ),
            ),
          ),

          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                buildStatBoxWidget(
                  icon: Icons.favorite, // Flutter 기본 하트 아이콘
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

  // 하단 통계 박스 위젯
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
      width: 110,
      height: 58,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF2DB),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          if (assetPath != null)
            Image.asset(
              assetPath,
              width: 20,
              height: 20,
              fit: BoxFit.contain,
            )
          else if (icon != null)
            Icon(
              icon,
              color: Colors.red,
              size: 20,
            )
          else if (emoji != null)
              Text(
                emoji,
                style: TextStyle(fontSize: 20),
              ),
          const SizedBox(width: 6),
          Flexible(
            child: Text(
              '$label1\n$label2 $count개',
              style: TextStyle(
                fontSize: 12,
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
