import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart'; // BottomNavBar 위젯 import

class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  int _currentIndex = 1; // 하단 내비게이션바 초기 선택 인덱스

  // 탭을 선택했을 때 동작하는 함수
  void _onTabTapped(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // 앱바 없이 배경색을 흰색으로 설정
      appBar: AppBar(
        backgroundColor: Colors.white, // 배경색 흰색
        elevation: 0, // 앱바 그림자 없애기
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // 검색창
            TextField(
              decoration: InputDecoration(
                hintText: '검색어를 입력하세요...',
                prefixIcon: const Icon(Icons.search),
                border: OutlineInputBorder(
                  borderSide: const BorderSide(color: Color(0xFFFFAD75)),
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            ),
          ],
        ),
      ),
      // 내비게이션 바 적용
      bottomNavigationBar: BottomNavBar(
        currentIndex: _currentIndex,
        onTabTapped: _onTabTapped,
      ),
    );
  }
}
