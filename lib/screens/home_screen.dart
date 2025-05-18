import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,

      // 앱바를 수동으로 구현
      body: Column(
        children: [
          // 커스텀 앱바
          Container(
            height: 90,
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFFE17951),
                  blurRadius: 4,
                  offset: const Offset(5, 0), // 오른쪽 그림자
                  spreadRadius: 0,
                ),
              ],
            ),
            child: const Padding(
              padding: EdgeInsets.only(bottom: 16), // 텍스트를 아래로 내림
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

          // 나머지 콘텐츠
          const Expanded(
            child: Center(
              child: Text('홈 콘텐츠 영역'), // 이 부분 나중에 채워 넣으세요
            ),
          ),
        ],
      ),

      // 하단 내비게이션 바
      bottomNavigationBar: BottomNavBar(currentIndex: 0),
    );
  }
}
