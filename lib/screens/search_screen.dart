import 'package:flutter/material.dart';
import '../widgets/navigationbar.dart'; // BottomNavBar 위젯 import
import '../widgets/searchbar.dart';

class SearchScreen extends StatelessWidget {
  const SearchScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final TextEditingController _searchController = TextEditingController();

    return Scaffold(
      backgroundColor: Colors.white,
      body: Padding(
        padding: const EdgeInsets.only(top: 70),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start, // 왼쪽 정렬
          children: [
            Center(child: Searchbar(controller: _searchController)), // ✅ controller 전달
            const SizedBox(height: 36),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 44),
              child: Text(
                '인기 작품 랭킹',
                style: TextStyle(
                  color: Color(0xFFEF7B4E),
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            const SizedBox(height: 24),
            // 향후 여기에 검색 결과 위젯을 추가하면 됩니다
          ],
        ),
      ),
      bottomNavigationBar: BottomNavBar(currentIndex: 1),
    );
  }
}
